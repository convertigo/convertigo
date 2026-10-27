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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

/**
 * A Redis session reads the attributes the engine needs at each request with its metadata, in the same command, and
 * neither reads nor deletes in Redis the attributes that are never stored there.
 */
public class RedisSessionReadTest {
	private static final SessionValueCodec CODEC = new SessionValueCodec();
	private static RedisSessionConfiguration configuration;

	@BeforeClass public static void configure() throws Exception {
		Engine.logRedis = org.apache.log4j.Logger.getLogger("redis-session-read-test");
		EnginePropertiesManager.initProperties();
		configuration = RedisSessionConfiguration.fromProperties();
	}

	/** Fields of one session hash; each read and write is recorded. */
	private static final class MemoryStore implements SessionStore {
		final Map<String, String> fields = new HashMap<>();
		final List<String> reads = new ArrayList<>();
		final List<Set<String>> written = new ArrayList<>();
		final List<Set<String>> deleted = new ArrayList<>();

		@Override public SessionStoreMeta readMeta(String sessionId) { return null; }
		@Override public String readAttribute(String sessionId, String name) {
			reads.add(name);
			return fields.get(name);
		}
		@Override public Set<String> readAttributeNames(String sessionId) { return new HashSet<>(fields.keySet()); }
		@Override public void writeDelta(String sessionId, Map<String, String> hset, Set<String> hdel, long ttlMillis) {
			var attributes = new HashSet<String>(hset.keySet());
			attributes.removeIf(name -> name.startsWith(SessionStoreKeys.META_PREFIX));
			written.add(attributes);
			deleted.add(new HashSet<>(hdel));
			fields.putAll(hset);
			hdel.forEach(fields::remove);
		}
		@Override public void delete(String sessionId) { fields.clear(); }
		@Override public void shutdown() {}
	}

	/** One request on the stored hash, with the attributes the store read with the metadata. */
	private static RedisHttpSession request(MemoryStore store, Map<String, String> prefetched) {
		var now = System.currentTimeMillis();
		return RedisHttpSession.fromMeta(store, null, configuration, "read-test", new SessionStoreMeta(now, now, 1800, prefetched));
	}

	@Test public void prefetchedAttributesAreNotReadAgain() throws Exception {
		var store = new MemoryStore();
		var user = SessionAttribute.authenticatedUser.value();
		var counted = SessionAttribute.countedSession.value();
		store.fields.put(user, CODEC.serialize(user, "someone"));
		store.fields.put("business", CODEC.serialize("business", "value"));
		var prefetched = new HashMap<String, String>();
		prefetched.put(user, store.fields.get(user));
		prefetched.put(counted, null);
		var session = request(store, prefetched);

		assertEquals("someone", session.getAttribute(user));
		assertNull("prefetched as not stored", session.getAttribute(counted));
		assertTrue(store.reads.isEmpty());
		assertEquals("value", session.getAttribute("business"));
		assertEquals(List.of("business"), store.reads);
		assertEquals("someone", session.getAttribute(user));
		assertEquals(List.of("business"), store.reads);
	}

	@Test public void aPrefetchedValueSetAgainUnchangedIsNotWritten() throws Exception {
		var store = new MemoryStore();
		var user = SessionAttribute.authenticatedUser.value();
		store.fields.put(user, CODEC.serialize(user, "someone"));
		var session = request(store, Map.of(user, store.fields.get(user)));
		session.setAttribute(user, session.getAttribute(user));
		session.flush();
		assertTrue(store.written.get(0).isEmpty());
		assertTrue(store.reads.isEmpty());

		session = request(store, Map.of(user, store.fields.get(user)));
		session.setAttribute(user, "someone else");
		session.flush();
		assertEquals(Set.of(user), store.written.get(1));
		assertEquals(CODEC.serialize(user, "someone else"), store.fields.get(user));
	}

	@Test public void neverStoredAttributesAreNeitherReadNorDeleted() throws Exception {
		var store = new MemoryStore();
		store.fields.put("business", CODEC.serialize("business", "value"));
		var session = request(store, Map.of());
		var listener = SessionAttribute.sessionListener.value();
		var clientIP = SessionAttribute.clientIP.value();
		var isNew = SessionAttribute.isNew.value();

		assertNull(session.getAttribute(listener));
		assertNull(session.getAttribute(SessionAttribute.contexts.value()));
		assertNull(session.getAttribute("session:technical"));
		assertTrue("never stored, so never read", store.reads.isEmpty());

		session.setAttribute(clientIP, "127.0.0.1");
		assertEquals("127.0.0.1", session.getAttribute(clientIP));
		session.setAttribute(SessionAttribute.userAgent.value(), "agent");
		session.removeAttribute(isNew);
		session.removeAttribute("business");
		session.flush();
		assertEquals("only the stored attribute is deleted", Set.of("business"), store.deleted.get(0));
		assertFalse(store.fields.containsKey(clientIP));
		assertNull("not readable again from the store", request(store, Map.of()).getAttribute(clientIP));
	}

	@Test public void theCountedSessionsAttributesAreStillReadAndDeleted() throws Exception {
		var store = new MemoryStore();
		var counted = SessionAttribute.countedSession.value();
		var legacy = SessionAttribute.legacyLicensedSession.value();
		store.fields.put(legacy, CODEC.serialize(legacy, true));
		var session = request(store, Map.of());
		assertTrue(SessionAttribute.isCounted(session));
		assertEquals(List.of(counted, legacy), store.reads);
		SessionAttribute.markCounted(session);
		session.flush();
		assertTrue(store.deleted.get(0).contains(legacy));
		assertEquals(CODEC.serialize(counted, true), store.fields.get(counted));
	}
}
