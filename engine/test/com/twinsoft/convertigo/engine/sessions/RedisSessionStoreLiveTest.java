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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

/**
 * The Redis session store against a disposable Redis, opt-in: {@code -PredisSessionTestHost=host:port}. Its keys use a
 * prefix of their own, removed at the end; never run it against a production Redis.
 */
public class RedisSessionStoreLiveTest {
	private static final SessionValueCodec CODEC = new SessionValueCodec();
	private static RedisSessionConfiguration configuration;
	private static RedisSessionStore store;
	private static RedissonClient client;
	private static String prefix;

	@BeforeClass public static void connect() throws Exception {
		var target = System.getProperty("redisSessionTestHost", "");
		Assume.assumeTrue("set -PredisSessionTestHost=host:port to run against a disposable Redis", !target.isBlank());
		Engine.logRedis = org.apache.log4j.Logger.getLogger("redis-session-store-live-test");
		Engine.logEngine = Engine.logRedis;
		EnginePropertiesManager.initProperties();
		var hostPort = target.split(":");
		prefix = "c8o-store-test-" + UUID.randomUUID() + ":";
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_HOST, hostPort[0]);
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_PORT, hostPort.length > 1 ? hostPort[1] : "6379");
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_PREFIX, prefix + "session");
		configuration = RedisSessionConfiguration.fromProperties();
		store = new RedisSessionStore(configuration);
		client = RedisClients.getClient(configuration);
	}

	@AfterClass public static void cleanup() {
		if (client != null) {
			client.getKeys().deleteByPattern(prefix + "*");
			client.shutdown();
		}
	}

	private static boolean inIndex(String index, String sessionId) {
		return client.getSet(prefix + "index:" + index, StringCodec.INSTANCE).contains(sessionId);
	}

	private static String newId() {
		return UUID.randomUUID().toString().replace("-", "").toUpperCase();
	}

	@Test public void theMetadataIsReadWithThePrefetchedAttributes() throws Exception {
		var id = newId();
		var session = RedisHttpSession.newSession(store, null, configuration, id);
		session.setAttribute(SessionAttribute.authenticatedUser.value(), "someone");
		session.setAttribute("business", "value");
		session.flush();

		var meta = store.readMeta(id);
		assertNotNull(meta);
		assertEquals(session.getCreationTime(), meta.creationTime());
		assertEquals(configuration.getDefaultTtlSeconds(), meta.maxInactiveInterval());
		assertEquals(RedisSessionStore.PREFETCHED_ATTRIBUTES, meta.attributes().keySet());
		assertEquals(CODEC.serialize(SessionAttribute.authenticatedUser.value(), "someone"),
				meta.attributes().get(SessionAttribute.authenticatedUser.value()));
		assertNull(meta.attributes().get(SessionAttribute.exception.value()));
		assertFalse(meta.attributes().containsKey("business"));

		var next = RedisHttpSession.fromMeta(store, null, configuration, id, meta);
		assertEquals("someone", next.getAttribute(SessionAttribute.authenticatedUser.value()));
		assertEquals("value", next.getAttribute("business"));

		assertNull("not stored", store.readMeta(newId()));
		client.getMap(configuration.key(id + "-nometa"), StringCodec.INSTANCE).put("business", "\"value\"");
		assertNull("stored without metadata", store.readMeta(id + "-nometa"));
	}

	@Test public void theWriteKeepsTheSessionsIndexes() throws Exception {
		var id = newId();
		client.getSet(prefix + "index:licensedSessions", StringCodec.INSTANCE).add(id);
		var session = RedisHttpSession.newSession(store, null, configuration, id);
		SessionAttribute.markCounted(session);
		session.flush();
		assertTrue(inIndex("sessions", id));
		assertTrue(inIndex("countedSessions", id));
		assertFalse("migrated from the legacy index", inIndex("licensedSessions", id));

		// the next requests mark it again, unchanged: it stays counted
		for (int i = 0; i < 2; i++) {
			var next = RedisHttpSession.fromMeta(store, null, configuration, id, store.readMeta(id));
			assertTrue(SessionAttribute.isCounted(next));
			SessionAttribute.markCounted(next);
			next.flush();
			assertTrue(inIndex("countedSessions", id));
		}

		var cleared = RedisHttpSession.fromMeta(store, null, configuration, id, store.readMeta(id));
		SessionAttribute.clearCounted(cleared);
		cleared.flush();
		assertTrue(inIndex("sessions", id));
		assertFalse(inIndex("countedSessions", id));

		var plain = RedisHttpSession.fromMeta(store, null, configuration, id, store.readMeta(id));
		client.getSet(prefix + "index:countedSessions", StringCodec.INSTANCE).add(id);
		plain.setAttribute("business", "value");
		plain.flush();
		assertTrue("an index the write does not concern is left as is", inIndex("countedSessions", id));
	}

	@Test public void anInvalidatedSessionIsNeitherWrittenNorIndexed() throws Exception {
		var id = newId();
		var session = RedisHttpSession.newSession(store, null, configuration, id);
		SessionAttribute.markCounted(session);
		session.flush();
		var late = RedisHttpSession.fromMeta(store, null, configuration, id, store.readMeta(id));
		store.delete(id);
		assertFalse(inIndex("sessions", id));
		late.setAttribute("business", "late");
		SessionAttribute.markCounted(late);
		late.flush();
		assertNull(store.readMeta(id));
		assertFalse(client.getMap(configuration.key(id), StringCodec.INSTANCE).isExists());
		assertFalse(inIndex("sessions", id));
		assertFalse(inIndex("countedSessions", id));
	}

	@Test public void theCountedSessionsCheckKeepsTheStoredOnesAndDropsTheOthers() throws Exception {
		var counted = client.getSet(prefix + "index:countedSessions", StringCodec.INSTANCE);
		counted.delete();
		var stored = new HashMap<String, Boolean>();
		for (int i = 0; i < 2500; i++) {
			var id = "count-" + i;
			boolean isStored = i % 3 != 0;
			if (isStored) {
				client.getMap(configuration.key(id), StringCodec.INSTANCE).put(SessionStoreKeys.META_LAST_ACCESS, "1");
			}
			stored.put(id, isStored);
		}
		counted.addAll(stored.keySet());
		var expected = stored.values().stream().filter(b -> b).count();
		assertEquals(expected, store.countCountedSessions());
		assertEquals(expected, counted.size());
		for (var entry : stored.entrySet()) {
			assertEquals(entry.getKey(), entry.getValue(), counted.contains(entry.getKey()));
		}
		counted.delete();
	}

	@Test public void theLastAccessTimeOfAStoredContextIsReadAlone() throws Exception {
		if (Engine.theApp == null) {
			Engine.theApp = new Engine();
		}
		var contexts = new RedisContextStore(configuration);
		var id = newId() + "_ctx";
		assertNull("a context not stored", contexts.readLastAccessTime(id));
		var context = new com.twinsoft.convertigo.engine.Context(id);
		context.name = "ctx";
		context.lastAccessTime = 1793125894586L;
		context.set("n", 41);
		contexts.save(context, 60);
		assertEquals(Long.valueOf(1793125894586L), contexts.readLastAccessTime(id));
		var read = contexts.read(id);
		assertEquals(1793125894586L, read.lastAccessTime);
		assertEquals(41, ((Number) read.get("n")).intValue());
		contexts.delete(id);
		assertNull(contexts.readLastAccessTime(id));
	}

	private static void saveContext(RedisContextStore contexts, String id, int ttlSeconds) {
		if (Engine.theApp == null) {
			Engine.theApp = new Engine();
		}
		var context = new com.twinsoft.convertigo.engine.Context(id);
		context.name = id.substring(id.indexOf('_') + 1);
		context.lastAccessTime = System.currentTimeMillis();
		contexts.save(context, ttlSeconds);
	}

	@Test public void theContextsAreIndexedBySessionAndByExpiry() throws Exception {
		var contexts = new RedisContextStore(configuration);
		var byExpiry = client.<String>getScoredSortedSet(prefix + "index:contextsByExpiry", StringCodec.INSTANCE);
		var s1 = newId();
		var s2 = newId();
		saveContext(contexts, s1 + "_a", 60);
		saveContext(contexts, s1 + "_b", 1);
		saveContext(contexts, s2 + "_c", 60);
		var s1Index = client.getSet(prefix + "index:sessionContexts:" + s1, StringCodec.INSTANCE);

		assertEquals(Set.of(s1 + "_a", s1 + "_b"), contexts.readSessionContextIds(s1));
		assertEquals(Set.of(s2 + "_c"), contexts.readSessionContextIds(s2));
		assertTrue("the index of a session lives as long as its contexts", s1Index.remainTimeToLive() > 55_000);
		assertTrue(contexts.readLiveContextIds().containsAll(Set.of(s1 + "_a", s1 + "_b", s2 + "_c")));

		Thread.sleep(1_200);
		var live = contexts.readLiveContextIds();
		assertFalse("expired", live.contains(s1 + "_b"));
		assertTrue(live.contains(s1 + "_a"));
		contexts.maintain();
		assertNull("purged by score", byExpiry.getScore(s1 + "_b"));
		assertNotNull(byExpiry.getScore(s1 + "_a"));

		// a write of the session keeps the index of its contexts as long as the session
		var session = RedisHttpSession.newSession(store, null, configuration, s1);
		session.flush();
		assertTrue(s1Index.remainTimeToLive() > (configuration.getDefaultTtlSeconds() - 5) * 1000L);

		contexts.delete(s1 + "_a");
		assertNull(byExpiry.getScore(s1 + "_a"));
		assertFalse(s1Index.contains(s1 + "_a"));
		assertFalse(client.getMap(configuration.contextKey(s1 + "_a"), StringCodec.INSTANCE).isExists());

		contexts.deleteBySessionPrefix(s2 + "_");
		assertFalse(client.getMap(configuration.contextKey(s2 + "_c"), StringCodec.INSTANCE).isExists());
		assertNull(byExpiry.getScore(s2 + "_c"));
		assertFalse(client.getSet(prefix + "index:sessionContexts:" + s2, StringCodec.INSTANCE).isExists());
		assertFalse("never indexed again", client.getSet(prefix + "index:contexts", StringCodec.INSTANCE).isExists());
	}

	@Test public void theFormerIndexOfTheContextsIsMovedThenRemoved() throws Exception {
		var contexts = new RedisContextStore(configuration);
		var byExpiry = client.<String>getScoredSortedSet(prefix + "index:contextsByExpiry", StringCodec.INSTANCE);
		var former = client.<String>getSet(prefix + "index:contexts", StringCodec.INSTANCE);
		var session = newId();
		var live = session + "_live";
		var gone = session + "_gone";
		saveContext(contexts, live, 60);
		// as saved by a former version: in the former index only
		byExpiry.remove(live);
		client.getSet(prefix + "index:sessionContexts:" + session, StringCodec.INSTANCE).delete();
		former.addAll(Set.of(live, gone));

		assertEquals(1, contexts.moveFormerIndex());
		assertFalse("removed", former.isExists());
		assertNotNull(byExpiry.getScore(live));
		assertNull("not stored any more", byExpiry.getScore(gone));
		assertEquals(Set.of(live), contexts.readSessionContextIds(session));
		assertTrue(client.getSet(prefix + "index:sessionContexts:" + session, StringCodec.INSTANCE).remainTimeToLive() > 0);

		// an instance of a former version writes it again during a rolling update
		former.add(live);
		assertEquals(1, contexts.moveFormerIndex());
		assertFalse(former.isExists());
		assertEquals(Set.of(live), contexts.readSessionContextIds(session));
		contexts.delete(live);
	}
}
