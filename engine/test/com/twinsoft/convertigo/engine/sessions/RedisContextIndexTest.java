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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.redisson.api.RMapCache;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * With the Redis session store, the contexts of a session are read from the index of the session, never from the
 * contexts of all the sessions: to end the session, to interrupt its contexts, at the logout, and when the session is
 * invalidated, which removes its contexts as the in-memory store does. Removing all the contexts reads the live ones.
 */
public class RedisContextIndexTest {
	private static RedisSessionConfiguration configuration;

	private MemoryContextStore store;
	private TestContextManager manager;

	@BeforeClass public static void configure() throws Exception {
		var logger = Logger.getLogger("redis-context-index-test");
		logger.setLevel(Level.ERROR);
		Engine.logRedis = Engine.logEngine = Engine.logContext = Engine.logContextManager = Engine.logStatistics
				= Engine.logUsageMonitor = Engine.logBeans = logger;
		EnginePropertiesManager.initProperties();
		configuration = RedisSessionConfiguration.fromProperties();
		Engine.theApp = new Engine();
	}

	@Before public void redisStore() throws Exception {
		store = new MemoryContextStore();
		manager = new TestContextManager(store);
		Engine.theApp.contextManager = manager;
		use(SessionStoreMode.redis);
		for (var contextId : List.of("S1_a", "S1_b", "S2_c")) {
			var context = new Context(contextId);
			context.name = contextId.substring(3);
			store.save(context, 300);
		}
	}

	@After public void reset() throws Exception {
		use(null);
	}

	private static void use(SessionStoreMode mode) throws Exception {
		var storeMode = ConvertigoHttpSessionManager.class.getDeclaredField("storeMode");
		storeMode.setAccessible(true);
		storeMode.set(ConvertigoHttpSessionManager.getInstance(), mode);
	}

	/** The stored contexts, and what was read to find them. */
	private static final class MemoryContextStore implements ContextStore {
		final Map<String, Context> data = new ConcurrentHashMap<>();
		final List<String> sessionReads = new CopyOnWriteArrayList<>();
		final AtomicInteger liveReads = new AtomicInteger();

		@Override public Context read(String contextId) {
			return data.get(contextId);
		}
		@Override public void save(Context context, int ttlSeconds) {
			data.put(context.contextID, context);
		}
		@Override public void delete(String contextId) {
			data.remove(contextId);
		}
		@Override public void deleteBySessionPrefix(String sessionIdPrefix) {
			data.keySet().removeIf(id -> id.startsWith(sessionIdPrefix));
		}
		@Override public void shutdown() {}

		@Override public Set<String> readSessionContextIds(String sessionId) {
			sessionReads.add(sessionId);
			var ids = new TreeSet<String>();
			for (var id : data.keySet()) {
				if (id.startsWith(sessionId + "_")) {
					ids.add(id);
				}
			}
			return ids;
		}
		@Override public Set<String> readLiveContextIds() {
			liveReads.incrementAndGet();
			return new TreeSet<>(data.keySet());
		}

		Set<String> stored() {
			return new TreeSet<>(data.keySet());
		}
	}

	/** The real context manager, with local locks only and the aborts recorded. */
	private static final class TestContextManager extends ContextManager {
		final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
		final List<String> aborted = new CopyOnWriteArrayList<>();

		TestContextManager(ContextStore store) throws Exception {
			set("contexts", new ConcurrentHashMap<String, Context>());
			set("contextStore", store);
			set("inflightContexts", Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RMapCache.class },
					(proxy, method, args) -> "readAllKeySet".equals(method.getName()) ? Set.of() : null));
		}

		private void set(String name, Object value) throws Exception {
			var field = ContextManager.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(this, value);
		}

		private AutoCloseable lock(String contextID) {
			var lock = locks.computeIfAbsent(contextID, id -> new ReentrantLock());
			lock.lock();
			return lock::unlock;
		}

		@Override public AutoCloseable lockContext(Context context) {
			return lock(context.contextID);
		}
		@Override public AutoCloseable lockContext(String contextID) {
			return lock(contextID);
		}
		@Override public AutoCloseable tryLockContext(Context context, long timeoutMillis) {
			return lock(context.contextID);
		}
		@Override public AutoCloseable tryLockContext(String contextID, long timeoutMillis) {
			return lock(contextID);
		}
		@Override public boolean requestAbort(String contextID) {
			aborted.add(contextID);
			return true;
		}
	}

	@Test public void endingASessionReadsOnlyTheIndexOfTheSession() throws Exception {
		assertTrue(manager.tryRemoveAll("S1", 0L));

		assertEquals(Set.of("S2_c"), store.stored());
		assertEquals(List.of("S1"), store.sessionReads);
		assertEquals("the contexts of all the sessions are not read", 0, store.liveReads.get());
	}

	@Test public void interruptingTheContextsOfASessionReadsOnlyTheIndexOfTheSession() throws Exception {
		assertTrue(manager.requestAbortAll("S1"));

		assertEquals(List.of("S1_a", "S1_b", "S1_default"), manager.aborted);
		assertEquals(List.of("S1"), store.sessionReads);
		assertEquals(0, store.liveReads.get());
	}

	@Test public void theLogoutRemovesTheStoredContextsOfTheSession() throws Exception {
		assertEquals(3, manager.tryRemoveAllBestEffort("S1", 0L));

		assertEquals(Set.of("S2_c"), store.stored());
		assertEquals(0, store.liveReads.get());
	}

	@Test public void invalidatingASessionRemovesItsContexts() throws Exception {
		var sessions = new RedisContextIndexTest.Sessions();
		var session = RedisHttpSession.newSession(sessions, null, configuration, "S1");

		session.invalidate();

		assertEquals(List.of("S1"), sessions.deleted);
		assertEquals("as with the in-memory store", Set.of("S2_c"), store.stored());
		assertEquals(0, store.liveReads.get());
	}

	@Test public void removingAllTheContextsReadsTheLiveOnes() throws Exception {
		manager.removeAll();

		assertEquals(Set.of(), store.stored());
		assertEquals(1, store.liveReads.get());
	}

	@Test public void aSessionWithStoredContextsIsNotEmpty() throws Exception {
		var sessions = new RedisContextIndexTest.Sessions();

		assertFalse("the contexts of another request are stored, not kept with the session",
				manager.isSessionEmpty(RedisHttpSession.newSession(sessions, null, configuration, "S1")));
		assertTrue(manager.isSessionEmpty(RedisHttpSession.newSession(sessions, null, configuration, "S3")));
		assertEquals(List.of("S1", "S3"), store.sessionReads);
		assertEquals(0, store.liveReads.get());
	}

	/** A session store that only records the deletions. */
	private static final class Sessions implements SessionStore {
		final List<String> deleted = new CopyOnWriteArrayList<>();

		@Override public SessionStoreMeta readMeta(String sessionId) { return null; }
		@Override public String readAttribute(String sessionId, String name) { return null; }
		@Override public Set<String> readAttributeNames(String sessionId) { return Set.of(); }
		@Override public void writeDelta(String sessionId, Map<String, String> hset, Set<String> hdel, long ttlMillis) {}
		@Override public void delete(String sessionId) { deleted.add(sessionId); }
		@Override public void shutdown() {}
	}
}
