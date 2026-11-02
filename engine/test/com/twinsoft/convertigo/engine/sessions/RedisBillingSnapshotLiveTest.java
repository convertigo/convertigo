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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.client.codec.StringCodec;

import com.twinsoft.convertigo.engine.BillingManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

/**
 * The billing snapshot of a counted session against a disposable Redis, opt-in: {@code -PredisSessionTestHost=host:port}.
 * It is read at a request only when the authenticated user of the session changed since the snapshot got it, and is
 * still given the new user then. Its keys use a prefix of their own, removed at the end; never run it against a
 * production Redis.
 */
public class RedisBillingSnapshotLiveTest {
	private static RedisSessionConfiguration configuration;
	private static RedisSessionStore store;
	private static RedissonClient client;
	private static String prefix;
	private static BillingManager billing;

	@BeforeClass public static void connect() throws Exception {
		var target = System.getProperty("redisSessionTestHost", "");
		Assume.assumeTrue("set -PredisSessionTestHost=host:port to run against a disposable Redis", !target.isBlank());
		com.twinsoft.convertigo.engine.BillingBench.setUpEngine();
		Engine.logRedis = org.apache.log4j.Logger.getLogger("redis-billing-snapshot-live-test");
		var hostPort = target.split(":");
		prefix = "c8o-billing-test-" + UUID.randomUUID() + ":";
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_HOST, hostPort[0]);
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_PORT, hostPort.length > 1 ? hostPort[1] : "6379");
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_PREFIX, prefix + "session");
		configuration = RedisSessionConfiguration.fromProperties();
		store = new RedisSessionStore(configuration);
		client = RedisClients.getClient(configuration);
		// a ticket manager is active: the snapshots are kept
		billing = com.twinsoft.convertigo.engine.BillingBench.start(List.of(new ITicketManager() {
			@Override public void addTicket(Ticket ticket) {
			}

			@Override public void removeTicket(Ticket ticket) {
			}

			@Override public Ticket newTicket() {
				return new Ticket();
			}

			@Override public void destroy() {
			}
		}));
	}

	@AfterClass public static void cleanup() throws Exception {
		if (billing != null) {
			billing.destroy();
		}
		if (client != null) {
			client.getKeys().deleteByPattern(prefix + "*");
			client.shutdown();
		}
	}

	private static String snapshotUser(String id) throws Exception {
		var raw = client.getMap(prefix + "index:countedSessionsBilling", StringCodec.INSTANCE).get(id);
		return new JSONObject((String) raw).optString("authenticatedUser");
	}

	private static void setSnapshotUser(String id, String user) throws Exception {
		var map = client.<String, String>getMap(prefix + "index:countedSessionsBilling", StringCodec.INSTANCE);
		var json = new JSONObject(map.get(id));
		json.put("authenticatedUser", user);
		map.fastPut(id, json.toString());
	}

	private static long hgetCalls() {
		var stats = client.getRedisNodes(RedisNodes.SINGLE).getInstance().info(RedisNode.InfoSection.COMMANDSTATS);
		var hget = stats.get("cmdstat_hget");
		if (hget == null) {
			return 0;
		}
		for (var part : hget.split(",")) {
			if (part.startsWith("calls=")) {
				return Long.parseLong(part.substring("calls=".length()));
			}
		}
		return 0;
	}

	/** the next request of the session: read from Redis as a new request reads it */
	private static RedisHttpSession next(String id) {
		return RedisHttpSession.fromMeta(store, null, configuration, id, store.readMeta(id));
	}

	@Test public void theSnapshotIsReadOnlyWhenTheUserChanged() throws Exception {
		var id = UUID.randomUUID().toString().replace("-", "").toUpperCase();
		var session = RedisHttpSession.newSession(store, null, configuration, id);
		SessionAttribute.markCounted(session);
		assertTrue("the snapshot is made", store.upsertCountedSessionBillingSnapshot(session));
		session.flush();
		assertEquals("", snapshotUser(id));

		// the user signs in: the next request gives it to the snapshot
		session = next(id);
		SessionAttribute.authenticatedUser.set(session, "alice");
		session.flush();
		session = next(id);
		store.syncCountedSessionBillingAuthenticatedUser(session);
		session.flush();
		assertEquals("alice", snapshotUser(id));

		// the same user: the snapshot is no more read
		long before = hgetCalls();
		for (int i = 0; i < 20; i++) {
			session = next(id);
			store.syncCountedSessionBillingAuthenticatedUser(session);
			session.flush();
		}
		assertEquals("no read of the snapshot", 0, hgetCalls() - before);

		// another user, even set on another node: given to the snapshot at the next request
		session = next(id);
		SessionAttribute.authenticatedUser.set(session, "bob");
		session.flush();
		session = next(id);
		store.syncCountedSessionBillingAuthenticatedUser(session);
		session.flush();
		assertEquals("bob", snapshotUser(id));

		// the snapshot of a node of the previous version, changed without the mark: still given the user then
		session = next(id);
		SessionAttribute.authenticatedUser.set(session, "carol");
		session.flush();
		setSnapshotUser(id, "carol");
		session = next(id);
		store.syncCountedSessionBillingAuthenticatedUser(session);
		session.flush();
		assertEquals("carol", snapshotUser(id));
		assertEquals("carol", SessionAttribute.billingUser.get(next(id)));
	}

	@Test public void aSessionCountedWithTheUserHasItsSnapshotMarked() throws Exception {
		var id = UUID.randomUUID().toString().replace("-", "").toUpperCase();
		var session = RedisHttpSession.newSession(store, null, configuration, id);
		SessionAttribute.authenticatedUser.set(session, "dave");
		SessionAttribute.markCounted(session);
		assertTrue(store.upsertCountedSessionBillingSnapshot(session));
		session.flush();
		assertEquals("dave", snapshotUser(id));
		long before = hgetCalls();
		session = next(id);
		store.syncCountedSessionBillingAuthenticatedUser(session);
		session.flush();
		assertEquals("no read of the snapshot", 0, hgetCalls() - before);
	}
}
