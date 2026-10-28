/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.sessions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EventManager;

/**
 * With a shared context store, the expiry check and the stop of the context manager of an instance only drop the copies
 * of the stored contexts it keeps in memory: other instances may still use the contexts, which expire in the store by
 * their own time to live. Without a store, the contexts are removed as before.
 */
public class RedisContextExpiredCopyTest {
	private MemoryContextStore store;
	private TestContextManager manager;
	private Project project;

	@BeforeClass public static void configure() throws Exception {
		var logger = Logger.getLogger("redis-context-expired-copy-test");
		logger.setLevel(Level.ERROR);
		Engine.logRedis = Engine.logEngine = Engine.logContext = Engine.logContextManager = Engine.logStatistics
				= Engine.logUsageMonitor = Engine.logBeans = logger;
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.eventManager = new EventManager();
	}

	@Before public void manager() throws Exception {
		store = new MemoryContextStore();
		manager = new TestContextManager();
		Engine.theApp.contextManager = manager;
		project = new Project();
		project.setContextTimeout(1);
	}

	private static final class MemoryContextStore implements ContextStore {
		final Map<String, Context> data = new ConcurrentHashMap<>();
		final List<String> deleted = new CopyOnWriteArrayList<>();

		@Override public Context read(String contextId) {
			return data.get(contextId);
		}
		@Override public void save(Context context, int ttlSeconds) {
			data.put(context.contextID, context);
		}
		@Override public void delete(String contextId) {
			data.remove(contextId);
			deleted.add(contextId);
		}
		@Override public void deleteBySessionPrefix(String sessionIdPrefix) {}
		@Override public void shutdown() {}
	}

	private static final class TestContextManager extends ContextManager {
		final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
		final Map<String, Context> contexts = new ConcurrentHashMap<>();

		TestContextManager() throws Exception {
			var contextsField = ContextManager.class.getDeclaredField("contexts");
			contextsField.setAccessible(true);
			contextsField.set(this, contexts);
		}

		void store(ContextStore contextStore) throws Exception {
			var storeField = ContextManager.class.getDeclaredField("contextStore");
			storeField.setAccessible(true);
			storeField.set(this, contextStore);
		}

		@Override public AutoCloseable lockContext(Context context) {
			return lockContext(context.contextID);
		}

		@Override public AutoCloseable lockContext(String contextID) {
			var lock = locks.computeIfAbsent(contextID, id -> new ReentrantLock());
			lock.lock();
			return lock::unlock;
		}

		/** One pass of the vulture over the contexts kept in memory. */
		void expire() throws Exception {
			var method = ContextManager.class.getDeclaredMethod("removeExpiredContexts");
			method.setAccessible(true);
			method.invoke(this);
		}
	}

	/** A context last used here long ago, stored as well. */
	private Context expiredHere(String contextID) {
		var context = new Context(contextID);
		context.name = contextID.substring(contextID.indexOf('_') + 1);
		context.project = project;
		context.projectName = "ContextTest";
		context.creationTime = System.currentTimeMillis() - 60_000;
		context.lastAccessTime = System.currentTimeMillis() - 30_000;
		manager.contexts.put(contextID, context);
		store.save(context, 300);
		return context;
	}

	@Test public void theExpiredCopyOfAStoredContextIsDroppedButTheStoredContextIsKept() throws Exception {
		manager.store(store);
		var context = expiredHere("SESSION1_named");

		manager.expire();

		assertFalse("the copy of this instance is dropped", manager.contexts.containsKey(context.contextID));
		assertTrue("the context is not deleted from the store", store.deleted.isEmpty());
		assertSame("other instances still find the stored context", context, store.read(context.contextID));
		assertFalse("the context is not removed", context.isDestroying);
	}

	@Test public void theCopyOfAStoredContextInUseByARequestOfThisInstanceIsKept() throws Exception {
		manager.store(store);
		var context = expiredHere("SESSION2_named");
		context.httpSession = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { HttpSession.class }, (proxy, method, args) -> null);

		manager.expire();

		assertSame("the request keeps its copy, it evicts it at its end", context, manager.contexts.get(context.contextID));
		assertTrue(store.deleted.isEmpty());
	}

	@Test public void anExpiredContextOfAPoolIsStillRemoved() throws Exception {
		manager.store(store);
		var context = expiredHere("/ContextTest/connector/pool_1");
		context.lockPooledContext = true;

		manager.expire();

		assertFalse(manager.contexts.containsKey(context.contextID));
		assertEquals("a pool context belongs to its instance", List.of(context.contextID), store.deleted);
	}

	@Test public void withoutContextStoreAnExpiredContextIsRemoved() throws Exception {
		var context = expiredHere("SESSION3_named");

		manager.expire();

		assertFalse(manager.contexts.containsKey(context.contextID));
		assertTrue("the context is removed", context.isDestroying);
	}

	@Test public void theStopOfAnInstanceDropsItsCopiesOfTheStoredContexts() throws Exception {
		manager.store(store);
		var stored = expiredHere("SESSION4_named");
		stored.lastAccessTime = System.currentTimeMillis();
		var container = expiredHere("SESSION4_Container-1");
		container.name = "Container-1";
		store.data.remove(container.contextID);

		manager.destroy();

		assertTrue("the stored context stays for the other instances", store.deleted.isEmpty());
		assertSame(stored, store.read(stored.contextID));
		assertFalse(stored.isDestroying);
		assertTrue("a context that only exists here is removed", container.isDestroying);
	}

	@Test public void withoutContextStoreTheStopOfAnInstanceRemovesItsContexts() throws Exception {
		var context = expiredHere("SESSION5_named");
		context.lastAccessTime = System.currentTimeMillis();

		manager.destroy();

		assertTrue(context.isDestroying);
	}
}
