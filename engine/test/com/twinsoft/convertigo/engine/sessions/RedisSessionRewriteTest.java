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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

/**
 * A Redis session writes an attribute set again only when its serialized form differs from the one Redis holds, as
 * the session read or wrote it: the same value set again is not sent, a value modified in place and set again with
 * the same reference is.
 */
public class RedisSessionRewriteTest {
	private static final SessionValueCodec CODEC = new SessionValueCodec();
	private static RedisSessionConfiguration configuration;

	@BeforeClass public static void configure() throws Exception {
		Engine.logRedis = org.apache.log4j.Logger.getLogger("redis-session-rewrite-test");
		EnginePropertiesManager.initProperties();
		configuration = RedisSessionConfiguration.fromProperties();
	}

	/** Fields of one session hash; each writeDelta is recorded. */
	private static final class MemoryStore implements SessionStore {
		final Map<String, String> fields = new HashMap<>();
		final List<Map<String, String>> written = new ArrayList<>();
		final List<Set<String>> deleted = new ArrayList<>();

		@Override public SessionStoreMeta readMeta(String sessionId) { return null; }
		@Override public String readAttribute(String sessionId, String name) { return fields.get(name); }
		@Override public Set<String> readAttributeNames(String sessionId) { return new HashSet<>(fields.keySet()); }
		@Override public void writeDelta(String sessionId, Map<String, String> hset, Set<String> hdel, long ttlMillis) {
			var attributes = new HashMap<String, String>();
			hset.forEach((name, value) -> {
				if (!name.startsWith(SessionStoreKeys.META_PREFIX)) attributes.put(name, value);
			});
			written.add(attributes);
			deleted.add(new HashSet<>(hdel));
			fields.putAll(hset);
			hdel.forEach(fields::remove);
		}
		@Override public void delete(String sessionId) { fields.clear(); }
		@Override public void shutdown() {}

		Set<String> lastWritten() { return written.get(written.size() - 1).keySet(); }
		Set<String> lastDeleted() { return deleted.get(deleted.size() - 1); }
	}

	/** One request: a fresh session object on the stored hash, as RedisSessionProvider creates it. */
	private static RedisHttpSession request(MemoryStore store) {
		var now = System.currentTimeMillis();
		return RedisHttpSession.fromMeta(store, null, configuration, "rewrite-test", new SessionStoreMeta(now, now, 1800));
	}

	private static MemoryStore storeWith(String name, Object value) throws Exception {
		var store = new MemoryStore();
		store.fields.put(name, CODEC.serialize(name, value));
		return store;
	}

	@Test public void theSameValueReadThenSetAgainIsNotWritten() throws Exception {
		var store = storeWith("token", "eyJ.same.token");
		var session = request(store);
		assertEquals("eyJ.same.token", session.getAttribute("token"));
		session.setAttribute("token", "eyJ.same.token");
		session.flush();
		assertTrue(store.lastWritten().isEmpty());

		var map = storeWith("entry", Map.of("user_id", "u1", "revs", List.of("1-a", "2-b")));
		var other = request(map);
		other.setAttribute("entry", other.getAttribute("entry"));
		other.flush();
		assertTrue("a map read and set again unchanged", map.lastWritten().isEmpty());
	}

	@Test public void aDifferentValueIsWritten() throws Exception {
		var store = storeWith("token", "eyJ.old");
		var session = request(store);
		session.getAttribute("token");
		session.setAttribute("token", "eyJ.new");
		session.flush();
		assertEquals(Set.of("token"), store.lastWritten());
		assertEquals(CODEC.serialize("token", "eyJ.new"), store.fields.get("token"));
	}

	@SuppressWarnings("unchecked")
	@Test public void aValueModifiedInPlaceAndSetWithTheSameReferenceIsWritten() throws Exception {
		var store = storeWith("entry", Map.of("count", 1));
		var session = request(store);
		var entry = (Map<String, Object>) session.getAttribute("entry");
		entry.put("count", 2);
		session.setAttribute("entry", entry);
		session.flush();
		assertEquals(Set.of("entry"), store.lastWritten());
		assertEquals(2, ((Map<String, Object>) CODEC.deserialize("entry", store.fields.get("entry"))).get("count"));

		// the form written is known: one session object set again unchanged, then modified in place
		var fresh = new MemoryStore();
		var writer = request(fresh);
		var value = new LinkedHashMap<String, Object>();
		value.put("count", 1);
		writer.setAttribute("entry", value);
		writer.flush();
		assertEquals(Set.of("entry"), fresh.lastWritten());
		writer.setAttribute("entry", value);
		writer.flush();
		assertTrue("unchanged since it was written", fresh.lastWritten().isEmpty());
		value.put("count", 3);
		writer.setAttribute("entry", value);
		writer.flush();
		assertEquals(Set.of("entry"), fresh.lastWritten());
		assertEquals(3, ((Map<String, Object>) CODEC.deserialize("entry", fresh.fields.get("entry"))).get("count"));
	}

	@Test public void anAttributeNotReadInTheRequestIsWritten() throws Exception {
		var store = storeWith("token", "eyJ.same.token");
		var session = request(store);
		session.setAttribute("token", "eyJ.same.token");
		session.flush();
		assertEquals("its stored form is unknown to this session", Set.of("token"), store.lastWritten());
	}

	@Test public void theCountedSessionFlagIsWrittenEachTimeItIsMarked() throws Exception {
		// the store keeps the session in its counted sessions index when the flag is written: an unchanged flag is still
		// written, or the removal of the legacy flag done by markCounted would take the session out of that index
		var store = storeWith(SessionAttribute.countedSession.value(), true);
		var session = request(store);
		assertTrue(SessionAttribute.isCounted(session));
		SessionAttribute.markCounted(session);
		session.flush();
		assertEquals(Set.of(SessionAttribute.countedSession.value()), store.lastWritten());
		assertTrue(store.lastDeleted().contains(SessionAttribute.legacyLicensedSession.value()));
	}

	@Test public void removalsAreStillDeletedAndARemovalUndoneKeepsTheValue() throws Exception {
		var store = storeWith("token", "eyJ.kept");
		var session = request(store);
		session.getAttribute("token");
		session.removeAttribute("token");
		session.setAttribute("token", "eyJ.kept");
		session.flush();
		assertTrue(store.lastWritten().isEmpty());
		assertFalse(store.lastDeleted().contains("token"));
		assertEquals(CODEC.serialize("token", "eyJ.kept"), store.fields.get("token"));

		var removal = request(store);
		removal.getAttribute("token");
		removal.removeAttribute("token");
		removal.flush();
		assertTrue(store.lastDeleted().contains("token"));
		assertFalse(store.fields.containsKey("token"));

		// set again after the removal was written: the form is no longer known, the value is written
		removal.setAttribute("token", "eyJ.kept");
		removal.flush();
		assertEquals(Set.of("token"), store.lastWritten());
	}
}
