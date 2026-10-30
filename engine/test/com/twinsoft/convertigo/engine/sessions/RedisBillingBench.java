/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.sessions;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNodes;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

/**
 * Redis commands of the billing for the requests of counted sessions with an authenticated user, against a disposable
 * Redis (not a test): {@code java ... RedisBillingBench host:port [sessions] [requests]}. Its keys use a prefix of their
 * own, removed at the end.
 */
public class RedisBillingBench {
	public static void main(String[] args) throws Exception {
		var hostPort = args[0].split(":");
		int sessions = args.length > 1 ? Integer.parseInt(args[1]) : 20;
		int requests = args.length > 2 ? Integer.parseInt(args[2]) : 100;
		com.twinsoft.convertigo.engine.BillingBench.setUpEngine();
		Engine.logRedis = org.apache.log4j.Logger.getLogger("redis-billing-bench");
		var prefix = "c8o-billing-bench-" + UUID.randomUUID() + ":";
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_HOST, hostPort[0]);
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_PORT, hostPort[1]);
		EnginePropertiesManager.setProperty(PropertyName.SESSION_REDIS_PREFIX, prefix + "session");
		var configuration = RedisSessionConfiguration.fromProperties();
		var store = new RedisSessionStore(configuration);
		var client = RedisClients.getClient(configuration);
		var billing = com.twinsoft.convertigo.engine.BillingBench.start(List.of(new ITicketManager() {
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
		try {
			var ids = new String[sessions];
			for (int s = 0; s < sessions; s++) {
				ids[s] = UUID.randomUUID().toString().replace("-", "").toUpperCase();
				var session = RedisHttpSession.newSession(store, null, configuration, ids[s]);
				SessionAttribute.markCounted(session);
				store.upsertCountedSessionBillingSnapshot(session);
				SessionAttribute.authenticatedUser.set(session, "user" + s);
				session.flush();
				// the first request after the sign in gives the user to the snapshot
				session = RedisHttpSession.fromMeta(store, null, configuration, ids[s], store.readMeta(ids[s]));
				store.syncCountedSessionBillingAuthenticatedUser(session);
				session.flush();
			}
			var node = client.getRedisNodes(RedisNodes.SINGLE).getInstance();
			long hget0 = calls(node, "cmdstat_hget");
			long all0 = calls(node, null);
			long nanos = 0;
			for (int r = 0; r < requests; r++) {
				for (int s = 0; s < sessions; s++) {
					var session = RedisHttpSession.fromMeta(store, null, configuration, ids[s], store.readMeta(ids[s]));
					long t0 = System.nanoTime();
					store.syncCountedSessionBillingAuthenticatedUser(session);
					nanos += System.nanoTime() - t0;
					session.flush();
				}
			}
			long n = (long) sessions * requests;
			long info = 2; // the two INFO of the measure
			System.out.println(String.format(Locale.ROOT,
					"requests=%d HGET/request=%.2f commands/request (read, billing, write)=%.2f billing sync=%.1f us/request",
					n, (calls(node, "cmdstat_hget") - hget0) / (double) n, (calls(node, null) - all0 - info) / (double) n,
					nanos / 1e3 / n));
		} finally {
			billing.destroy();
			client.getKeys().deleteByPattern(prefix + "*");
			client.shutdown();
		}
		System.exit(0);
	}

	/** calls of a command, or of all of them */
	private static long calls(RedisNode node, String command) {
		long total = 0;
		for (var entry : node.info(RedisNode.InfoSection.COMMANDSTATS).entrySet()) {
			if (command != null && !command.equals(entry.getKey())) {
				continue;
			}
			for (var part : entry.getValue().split(",")) {
				if (part.startsWith("calls=")) {
					total += Long.parseLong(part.substring("calls=".length()));
				}
			}
		}
		return total;
	}
}
