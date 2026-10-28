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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.requesters.GenericRequester;
import com.twinsoft.convertigo.engine.servlets.SessionSerializationFilter;
import com.twinsoft.convertigo.engine.translators.Translator;

/**
 * With the Redis session store, a context changed by a request is in the context store when its lock is released, and
 * is not saved again after: requests run through {@link SessionSerializationFilter} and the real
 * {@link GenericRequester} and {@link ContextManager}, on session and context stores kept in memory, with a lock per
 * context shared by all the requests as the Redis lock of the instances is.
 */
public class RedisContextSavedBeforeUnlockTest {
	private static final String COOKIE = "JSESSIONID";
	private static final String PROJECT = "ContextTest";
	private static RedisSessionConfiguration configuration;

	private MemoryContextStore contexts;
	private MemorySessionStore sessions;
	private TestContextManager manager;
	private String sessionId;

	@BeforeClass public static void configure() throws Exception {
		var logger = Logger.getLogger("redis-context-before-unlock-test");
		logger.setLevel(Level.ERROR);
		Engine.logRedis = Engine.logEngine = Engine.logContext = Engine.logContextManager = Engine.logStatistics
				= Engine.logUsageMonitor = Engine.logBeans = logger;
		EnginePropertiesManager.initProperties();
		configuration = RedisSessionConfiguration.fromProperties();
		Engine.isEngineMode();
		Engine.theApp = new Engine();
	}

	@Before public void redisStores() throws Exception {
		contexts = new MemoryContextStore();
		sessions = new MemorySessionStore();
		manager = new TestContextManager(contexts);
		manager.store(contexts);
		Engine.theApp.contextManager = manager;
		use(SessionStoreMode.redis, new RedisSessionProvider(sessions, configuration));
		sessionId = "session-" + System.nanoTime();
		sessions.create(sessionId);
	}

	@After public void reset() throws Exception {
		use(null, null);
	}

	private static void use(SessionStoreMode mode, SessionProvider provider) throws Exception {
		var sessionManager = ConvertigoHttpSessionManager.getInstance();
		var storeMode = ConvertigoHttpSessionManager.class.getDeclaredField("storeMode");
		storeMode.setAccessible(true);
		storeMode.set(sessionManager, mode);
		var providerField = ConvertigoHttpSessionManager.class.getDeclaredField("provider");
		providerField.setAccessible(true);
		providerField.set(sessionManager, provider);
	}

	/** Each saved state of a context, and the order of the saves and of the lock releases. */
	private static final class MemoryContextStore implements ContextStore {
		final Map<String, Map<String, Object>> data = new ConcurrentHashMap<>();
		final List<String> events = new CopyOnWriteArrayList<>();
		final List<Integer> ttls = new CopyOnWriteArrayList<>();

		@Override public Context read(String contextId) {
			var stored = data.get(contextId);
			if (stored == null) {
				return null;
			}
			var context = new Context(contextId);
			context.name = (String) stored.get("name");
			context.lastAccessTime = (Long) stored.get("lastAccessTime");
			context.creationTime = (Long) stored.get("creationTime");
			if (stored.get("n") != null) {
				context.set("n", stored.get("n"));
			}
			return context;
		}

		@Override public synchronized void save(Context context, int ttlSeconds) {
			var stored = new HashMap<String, Object>();
			stored.put("name", context.name);
			stored.put("lastAccessTime", context.lastAccessTime);
			stored.put("creationTime", context.creationTime);
			stored.put("n", context.get("n"));
			data.put(context.contextID, stored);
			ttls.add(ttlSeconds);
			events.add("save " + context.contextID + " " + context.get("n"));
		}

		@Override public void delete(String contextId) {
			data.remove(contextId);
			events.add("delete " + contextId);
		}

		@Override public void deleteBySessionPrefix(String sessionIdPrefix) {}
		@Override public void shutdown() {}

		synchronized void released(String contextId) {
			var stored = data.get(contextId);
			events.add("release " + contextId + " " + (stored == null ? null : stored.get("n")));
		}

		Object stored(String contextId, String name) {
			var stored = data.get(contextId);
			return stored == null ? null : stored.get(name);
		}

		long saves() {
			return events.stream().filter(e -> e.startsWith("save ")).count();
		}

		long savesAfterTheLastRelease() {
			int last = -1;
			for (int i = 0; i < events.size(); i++) {
				if (events.get(i).startsWith("release ")) {
					last = i;
				}
			}
			return events.subList(last + 1, events.size()).stream().filter(e -> e.startsWith("save ")).count();
		}
	}

	/** The real context manager, with a lock per context shared by all the requests, as the Redis lock is. */
	private static final class TestContextManager extends ContextManager {
		final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
		final MemoryContextStore store;

		TestContextManager(MemoryContextStore store) {
			this.store = store;
		}

		void store(ContextStore contextStore) throws Exception {
			var contextsField = ContextManager.class.getDeclaredField("contexts");
			contextsField.setAccessible(true);
			contextsField.set(this, new ConcurrentHashMap<String, Context>());
			var storeField = ContextManager.class.getDeclaredField("contextStore");
			storeField.setAccessible(true);
			storeField.set(this, contextStore);
		}

		@Override public AutoCloseable lockContext(Context context) {
			return lock(context.contextID);
		}

		@Override public AutoCloseable lockContext(String contextID) {
			return lock(contextID);
		}

		private AutoCloseable lock(String contextID) {
			var lock = locks.computeIfAbsent(contextID, id -> new ReentrantLock());
			lock.lock();
			return () -> {
				if (lock.getHoldCount() == 1 && store != null) {
					store.released(contextID);
				}
				lock.unlock();
			};
		}

		@Override public boolean isLastHoldOfContextLock(String contextID) {
			var lock = locks.get(contextID);
			return lock == null || lock.getHoldCount() <= 1;
		}
	}

	/** The session hashes, each write recorded. */
	private static final class MemorySessionStore implements SessionStore {
		final Map<String, Map<String, String>> sessions = new ConcurrentHashMap<>();

		void create(String sessionId) {
			var now = Long.toString(System.currentTimeMillis());
			var fields = new ConcurrentHashMap<String, String>();
			fields.put(SessionStoreKeys.META_CREATION, now);
			fields.put(SessionStoreKeys.META_LAST_ACCESS, now);
			fields.put(SessionStoreKeys.META_MAX_INACTIVE, "1800");
			sessions.put(sessionId, fields);
		}

		@Override public SessionStoreMeta readMeta(String sessionId) {
			var fields = sessions.get(sessionId);
			if (fields == null) {
				return null;
			}
			var attributes = new HashMap<String, String>();
			for (var name : RedisSessionStore.PREFETCHED_ATTRIBUTES) {
				attributes.put(name, fields.get(name));
			}
			var now = System.currentTimeMillis();
			return new SessionStoreMeta(now, now, Integer.parseInt(fields.get(SessionStoreKeys.META_MAX_INACTIVE)), attributes);
		}
		@Override public String readAttribute(String sessionId, String name) {
			var fields = sessions.get(sessionId);
			return fields == null ? null : fields.get(name);
		}
		@Override public Set<String> readAttributeNames(String sessionId) {
			var fields = sessions.get(sessionId);
			return fields == null ? Set.of() : new HashSet<>(fields.keySet());
		}
		@Override public void writeDelta(String sessionId, Map<String, String> hset, Set<String> hdel, long ttlMillis) {
			var fields = sessions.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>());
			fields.putAll(hset);
			hdel.forEach(fields::remove);
		}
		@Override public void delete(String sessionId) { sessions.remove(sessionId); }
		@Override public void shutdown() {}
	}

	/** What a sequence does with its context locked, before it increments n. */
	private interface WhileLocked {
		void run(IncrRequester requester) throws Exception;
	}

	/** A sequence that increments the context variable n, as a requester of the engine runs it. */
	private static final class IncrRequester extends GenericRequester {
		final HttpServletRequest request;
		final String contextName;
		final boolean requestKnownBeforeTheLock;
		final WhileLocked whileLocked;

		/**
		 * @param requestKnownBeforeTheLock as the internal requests (REST operations, calls of a sequence): the context
		 * has its HTTP request before its lock is taken, the context cache of the request is used; otherwise as the
		 * servlet requests, which set it in initContext
		 */
		IncrRequester(HttpServletRequest request, String contextName, boolean requestKnownBeforeTheLock, WhileLocked whileLocked) {
			this.request = request;
			this.contextName = contextName;
			this.requestKnownBeforeTheLock = requestKnownBeforeTheLock;
			this.whileLocked = whileLocked;
			this.projectName = PROJECT;
		}

		@Override public String getName() { return "IncrRequester"; }
		@Override public Context getContext() throws Exception {
			var session = request.getSession(true);
			var context = Engine.theApp.contextManager.get(this, contextName, session.getId(), null, PROJECT, null, "Incr");
			if (requestKnownBeforeTheLock) {
				context.setRequest(request);
			}
			return context;
		}
		@Override public void initContext(Context context) throws Exception {
			context.setRequest(request);
			super.initContext(context);
		}
		@Override protected void initInternalVariables() {}
		@Override public void preGetDocument() {}
		@Override public void setStyleSheet(Document document) {}
		@Override protected Object addStatisticsAsText(String stats, Object result) { return result; }
		@Override protected Object addStatisticsAsData(Object result) { return result; }
		@Override protected Object coreProcessRequest() throws Exception {
			if (whileLocked != null) {
				whileLocked.run(this);
			}
			var n = context.get("n");
			int next = (n == null ? 0 : ((Number) n).intValue()) + 1;
			context.set("n", next);
			return next;
		}
		@Override public Translator getTranslator() {
			return new Translator() {
				public void buildInputDocument(Context context, Object input) {}
				public Object buildOutputData(Context context, Object output) { return output; }
				public String getContextName(byte[] data) { return null; }
				public String getProjectName(byte[] data) { return PROJECT; }
			};
		}
	}

	private HttpServletRequest request(Map<String, String> parameters) {
		var attributes = new ConcurrentHashMap<String, Object>();
		var cookies = new Cookie[] { new Cookie(COOKIE, sessionId) };
		return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletRequest.class },
				(proxy, method, args) -> switch (method.getName()) {
					case "getAttribute" -> attributes.get((String) args[0]);
					case "setAttribute" -> { if (args[1] == null) attributes.remove(args[0]); else attributes.put((String) args[0], args[1]); yield null; }
					case "removeAttribute" -> { attributes.remove((String) args[0]); yield null; }
					case "getAttributeNames" -> Collections.enumeration(new ArrayList<>(attributes.keySet()));
					case "getParameter" -> parameters.get((String) args[0]);
					case "getCookies" -> cookies;
					case "getContextPath" -> "/convertigo";
					case "getRequestURI" -> "/convertigo/projects/" + PROJECT + "/.json";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "test request";
					default -> defaultValue(method.getReturnType());
				});
	}

	private static HttpServletResponse response() {
		var writer = new PrintWriter(new StringWriter());
		return (HttpServletResponse) Proxy.newProxyInstance(RedisContextSavedBeforeUnlockTest.class.getClassLoader(),
				new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getWriter" -> writer;
					case "getCharacterEncoding" -> "UTF-8";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "test response";
					default -> defaultValue(method.getReturnType());
				});
	}

	private static Object defaultValue(Class<?> type) {
		if (type == boolean.class) return false;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		return null;
	}

	/** What the servlet does after its requester (another request meanwhile, end of the context...). */
	private interface AfterRequester {
		void run(HttpServletRequest request, Context context) throws Exception;
	}

	/** One request of the named context, through the filter, as GenericServlet runs it. */
	private Context incr(String contextName, boolean requestKnownBeforeTheLock, Map<String, String> parameters,
			WhileLocked whileLocked, AfterRequester after) throws Exception {
		Context[] context = new Context[1];
		FilterChain chain = (req, resp) -> {
			req.setAttribute("response", resp);
			try {
				var requester = new IncrRequester((HttpServletRequest) req, contextName, requestKnownBeforeTheLock, whileLocked);
				requester.processRequest(req);
				context[0] = requester.context;
				if (after != null) {
					after.run((HttpServletRequest) req, requester.context);
				}
				resp.getWriter().write("ok");
				resp.getWriter().flush();
			} catch (IOException | RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		new SessionSerializationFilter().doFilter(request(parameters), response(), chain);
		return context[0];
	}

	private Context incr(String contextName, boolean requestKnownBeforeTheLock) throws Exception {
		return incr(contextName, requestKnownBeforeTheLock, Map.of(), null, null);
	}

	private String contextId(String contextName) {
		return sessionId + "_" + contextName;
	}

	private static final boolean[] BOTH_KINDS = { false, true };

	@Test public void aContextIsInTheStoreWhenItsLockIsReleased() throws Exception {
		for (var requestKnownBeforeTheLock : BOTH_KINDS) {
			var name = "shared-" + requestKnownBeforeTheLock;
			incr(name, requestKnownBeforeTheLock);
			incr(name, requestKnownBeforeTheLock);
			var releases = contexts.events.stream().filter(e -> e.startsWith("release " + contextId(name))).toList();
			assertEquals(List.of("release " + contextId(name) + " 1", "release " + contextId(name) + " 2"), releases);
			assertEquals(2, contexts.stored(contextId(name), "n"));
		}
	}

	@Test public void aContextIsNotSavedAgainAfterItsLockIsReleased() throws Exception {
		for (var requestKnownBeforeTheLock : BOTH_KINDS) {
			contexts.events.clear();
			var name = "once-" + requestKnownBeforeTheLock;
			incr(name, requestKnownBeforeTheLock);
			assertEquals("a single save, before the lock is released (" + contexts.events + ")", 1, contexts.saves());
			assertEquals(0, contexts.savesAfterTheLastRelease());
			assertNull("evicted at the end of the request", manager.get(contextId(name)));
		}
	}

	@Test public void aRequestThatTookTheLockMeanwhileKeepsItsChange() throws Exception {
		for (var requestKnownBeforeTheLock : BOTH_KINDS) {
			var name = "meanwhile-" + requestKnownBeforeTheLock;
			incr(name, requestKnownBeforeTheLock, Map.of(), null, (request, context) -> {
				// the lock of the context is released, the response is not sent yet: the next request runs entirely
				var next = new Thread(() -> {
					try {
						incr(name, requestKnownBeforeTheLock);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				next.start();
				next.join();
			});
			assertEquals("the end of the first request does not overwrite the second one (" + contexts.events + ")", 2,
					contexts.stored(contextId(name), "n"));
		}
	}

	@Test public void concurrentIncrementsOfAContextVariableAreAllKept() throws Exception {
		var name = "concurrent";
		var threads = new ArrayList<Thread>();
		var failures = new CopyOnWriteArrayList<Throwable>();
		for (int t = 0; t < 8; t++) {
			int index = t;
			var thread = new Thread(() -> {
				for (int i = 0; i < 25; i++) {
					try {
						incr(name, (index + i) % 2 == 0);
					} catch (Throwable e) {
						failures.add(e);
					}
				}
			});
			threads.add(thread);
			thread.start();
		}
		for (var thread : threads) {
			thread.join();
		}
		assertTrue(failures.toString(), failures.isEmpty());
		assertEquals(200, contexts.stored(contextId(name), "n"));
	}

	@Test public void aContextRemovedAtTheEndOfTheRequestIsNotSaved() throws Exception {
		for (var requestKnownBeforeTheLock : BOTH_KINDS) {
			contexts.events.clear();
			var name = "removed-" + requestKnownBeforeTheLock;
			incr(name, requestKnownBeforeTheLock, Map.of("__removeContext", "true"), null,
					(request, context) -> manager.remove(context, request.getSession(false)));
			assertEquals("no save for a context that the end of the request removes (" + contexts.events + ")", 0,
					contexts.saves());
			assertNull(contexts.data.get(contextId(name)));
		}
	}

	@Test public void aContextKeptWithRemoveContextFalseIsSaved() throws Exception {
		for (var requestKnownBeforeTheLock : BOTH_KINDS) {
			contexts.events.clear();
			var name = "kept-" + requestKnownBeforeTheLock;
			incr(name, requestKnownBeforeTheLock, Map.of("__removeContext", "false"), null, null);
			assertEquals(1, contexts.stored(contextId(name), "n"));
			assertEquals(0, contexts.savesAfterTheLastRelease());
		}
	}

	@Test public void theNextRequesterOfTheSameRequestReadsAContextChangedMeanwhile() throws Exception {
		// as a REST operation after its authentication sequence: two requesters of the same context in one request
		var name = "two-requesters";
		var id = contextId(name);
		incr(name, true, Map.of(), null, (request, context) -> {
			// between the two, a request on another instance changes the context
			var other = contexts.read(id);
			other.set("n", 100);
			other.lastAccessTime = context.lastAccessTime + 5;
			contexts.save(other, 300);
			var second = new IncrRequester(request, name, true, null);
			assertEquals(101, second.processRequest(request));
		});
		assertEquals(101, contexts.stored(id, "n"));
	}

	@Test public void aRequestEndingWhileAnotherOneUsesTheContextLeavesItToIt() throws Exception {
		var name = "in-use";
		var id = contextId(name);
		incr(name, true);
		var inside = new java.util.concurrent.CountDownLatch(1);
		var firstOver = new java.util.concurrent.CountDownLatch(1);
		var failures = new CopyOnWriteArrayList<Throwable>();
		var second = new Thread(() -> {
			try {
				incr(name, true, Map.of(), requester -> {
					inside.countDown();
					firstOver.await();
					// a sequence called in the same context, after the end of the other request
					new IncrRequester(requester.request, name, true, null).processRequest(requester.request);
				}, null);
			} catch (Throwable e) {
				failures.add(e);
			}
		});
		incr(name, true, Map.of(), null, (request, context) -> {
			// the lock is released: the next request takes it, then this one ends
			second.start();
			inside.await();
		});
		firstOver.countDown();
		second.join();
		assertTrue(failures.toString(), failures.isEmpty());
		assertEquals("the called sequence and its caller worked on the same context", 4, contexts.stored(id, "n"));
	}

	@Test public void aContextCreatedByARequestIsReadAgainIfAnotherOneStoredItMeanwhile() throws Exception {
		var name = "created";
		var id = contextId(name);
		// a request on another instance holds the lock of the context
		var held = manager.lockContext(id);
		var failures = new CopyOnWriteArrayList<Throwable>();
		var waiting = new Thread(() -> {
			try {
				incr(name, false);
			} catch (Throwable e) {
				failures.add(e);
			}
		});
		waiting.start();
		// this request did not find the context in the store: it created it, and waits for its lock
		var lock = manager.locks.get(id);
		while (manager.get(id) == null || !lock.hasQueuedThreads()) {
			Thread.sleep(1);
		}
		var created = manager.get(id);
		// the other request stores the context, with the millisecond of its creation here as last access time
		var other = new Context(id);
		other.name = name;
		other.creationTime = created.creationTime;
		other.lastAccessTime = created.lastAccessTime;
		other.set("n", 5);
		contexts.save(other, 300);
		held.close();
		waiting.join();
		assertTrue(failures.toString(), failures.isEmpty());
		assertEquals("the request read the stored context before it changed it", 6, contexts.stored(id, "n"));
	}

	@Test public void eachSavedStateOfAContextHasItsOwnLastAccessTime() throws Exception {
		var name = "stamp";
		var id = contextId(name);
		incr(name, false);
		// another instance whose clock is ahead saved the context last
		var other = contexts.read(id);
		long ahead = System.currentTimeMillis() + 60_000;
		other.lastAccessTime = ahead;
		contexts.save(other, 300);
		incr(name, false);
		assertTrue("a newer state never has the stamp of an older one", (Long) contexts.stored(id, "lastAccessTime") > ahead);
		assertEquals(2, contexts.stored(id, "n"));
	}

	@Test public void aContextIsStoredForTheContextTimeoutWithinTheSessionOne() throws Exception {
		var context = new Context(contextId("ttl"));
		var session = new RequestScopedHttpSession(null, sessionId);
		session.setMaxInactiveInterval(1800);
		context.contextListSession = session;
		var project = new com.twinsoft.convertigo.beans.core.Project();
		context.project = project;
		assertEquals("the context timeout of the project", project.getContextTimeout(), invokeStoreTtl(context));
		session.setMaxInactiveInterval(60);
		assertEquals("within the session timeout", 60, invokeStoreTtl(context));
		context.contextListSession = null;
		assertEquals("without session: the default of the store", -1, invokeStoreTtl(context));
		incr("ttl-request", false);
		incr("ttl-request", true);
		assertEquals("a request saves its context for the timeout of its session, its project being unknown here",
				List.of(1800, 1800), contexts.ttls);
	}

	private int invokeStoreTtl(Context context) throws Exception {
		var method = ContextManager.class.getDeclaredMethod("storeTtlSeconds", Context.class);
		method.setAccessible(true);
		return (Integer) method.invoke(manager, context);
	}

	@Test public void theLockOfAContextIsReleasedByItsFirstHolder() throws Exception {
		use(SessionStoreMode.tomcat, new LegacySessionProvider());
		var tomcat = new ContextManager();
		var contextsField = ContextManager.class.getDeclaredField("contexts");
		contextsField.setAccessible(true);
		contextsField.set(tomcat, new ConcurrentHashMap<String, Context>());
		try (var outer = tomcat.lockContext("s_held")) {
			assertTrue(tomcat.isLastHoldOfContextLock("s_held"));
			try (var inner = tomcat.lockContext("s_held")) {
				assertFalse("a nested requester of the same context does not release it", tomcat.isLastHoldOfContextLock("s_held"));
			}
			assertTrue(tomcat.isLastHoldOfContextLock("s_held"));
		}
	}

	@Test public void theTomcatStoreKeepsItsContextsInMemory() throws Exception {
		HttpSession session = new RequestScopedHttpSession(null, sessionId);
		use(SessionStoreMode.tomcat, new SessionProvider() {
			@Override public HttpSession getSession(HttpServletRequest request, boolean create) {
				return session;
			}
		});
		var tomcat = new TestContextManager(null);
		tomcat.store(null);
		Engine.theApp.contextManager = tomcat;
		var first = incr("tomcat", false);
		var second = incr("tomcat", false);
		assertSame("the same context object for the requests of the context", first, second);
		assertEquals(2, second.get("n"));
		assertTrue(tomcat.isHeld(second));
		assertTrue("no context store in Tomcat mode", contexts.events.isEmpty());
		assertNotNull(tomcat.get(contextId("tomcat")));
	}
}
