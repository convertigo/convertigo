/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.sessions;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.client.codec.StringCodec;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/** Opt-in probe against a disposable Docker Redis topology; never run against production. */
public final class RedisSentinelRuntimeProbe {
	public static void main(String[] args) throws Exception {
		Engine.logRedis = org.apache.log4j.Logger.getLogger("sentinel-probe");
		Engine.logEngine = Engine.logRedis;
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		var cfg = RedisSessionConfiguration.fromProperties();
		var client = RedisClients.getClient(cfg);
		var other = Redisson.create(RedisClients.createConfig(cfg));
		var id = "sentinel-probe-" + UUID.randomUUID();
		var sessionStore = new RedisSessionStore(cfg);
		var contextStore = new RedisContextStore(cfg);
		var session = RedisHttpSession.newSession(sessionStore, null, cfg, id);
		var context = new Context(id);
		context.name = "sentinel-probe";
		context.set("sentinel-value", "before");
		var inflight = client.<String, String>getMapCache(cfg.getContextKeyPrefix() + "inflight:contexts", StringCodec.INSTANCE);
		var topic = other.getTopic(cfg.getContextKeyPrefix() + id + ":topic", StringCodec.INSTANCE);
		var messages = new LinkedBlockingQueue<String>();
		topic.addListener(String.class, (channel, message) -> messages.add(message));
		try {
			session.setAttribute("sentinel-value", "before");
			session.flush();
			contextStore.save(context, 180);
			inflight.fastPut(id, "before", 180, TimeUnit.SECONDS);
			assertStored(sessionStore, contextStore, cfg, id, "before");
			check("before".equals(inflight.get(id)), "inflight marker before failover");
			checkLockAndPubSub(client, other, messages, cfg, id);
			var initialMaster = client.getRedisNodes(RedisNodes.SENTINEL_MASTER_SLAVE).getMaster().getAddr();
			if (args.length > 0 && "--failover".equals(args[0])) {
				// A separate controller must wait for replication before stopping the master.
				System.out.println("READY_FOR_FAILOVER " + initialMaster);
				System.out.flush();
				var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
				while (initialMaster.equals(client.getRedisNodes(RedisNodes.SENTINEL_MASTER_SLAVE).getMaster().getAddr())
						|| initialMaster.equals(other.getRedisNodes(RedisNodes.SENTINEL_MASTER_SLAVE).getMaster().getAddr())) {
					if (System.nanoTime() > deadline) {
						throw new AssertionError("Sentinel did not switch master within 90 seconds");
					}
					Thread.sleep(200);
				}
				// WAIT guarantees must be established by the test controller, not by production code.
				assertStored(sessionStore, contextStore, cfg, id, "before");
				check("before".equals(inflight.get(id)), "replicated inflight marker after failover");
			}
			session.setAttribute("sentinel-value", "after");
			session.flush();
			context.set("sentinel-value", "after");
			contextStore.save(context, 180);
			assertStored(sessionStore, contextStore, cfg, id, "after");
			inflight.fastPut(id, "after", 180, TimeUnit.SECONDS);
			check("after".equals(inflight.get(id)), "inflight read after write");
			checkLockAndPubSub(client, other, messages, cfg, id);
			System.out.println("PASS sessions, contexts, inflight, locks, pubsub; master="
					+ client.getRedisNodes(RedisNodes.SENTINEL_MASTER_SLAVE).getMaster().getAddr());
		} finally {
			try {
				sessionStore.delete(id);
				contextStore.delete(id);
				inflight.fastRemove(id);
			} catch (RuntimeException e) {
				System.err.println("Probe cleanup failed: " + RedisHealth.describe(e));
			} finally {
				other.shutdown();
				client.shutdown();
			}
		}
	}

	private static void assertStored(RedisSessionStore sessions, RedisContextStore contexts,
			RedisSessionConfiguration cfg, String id, String value) {
		var meta = sessions.readMeta(id);
		check(meta != null, "session metadata exists");
		var restored = RedisHttpSession.fromMeta(sessions, null, cfg, id, meta);
		check(value.equals(restored.getAttribute("sentinel-value")), "session round trip: " + value);
		var context = contexts.read(id);
		check(context != null && value.equals(context.get("sentinel-value")), "context round trip: " + value);
	}

	private static void checkLockAndPubSub(org.redisson.api.RedissonClient client,
			org.redisson.api.RedissonClient other, LinkedBlockingQueue<String> messages,
			RedisSessionConfiguration cfg, String id) throws Exception {
		var lock = client.getLock(cfg.getContextKeyPrefix() + "lock:context:" + id);
		var competitor = other.getLock(lock.getName());
		lock.lock(10, TimeUnit.SECONDS);
		try {
			check(!competitor.tryLock(), "cross-client lock exclusion");
		} finally {
			lock.unlock();
		}
		check(competitor.tryLock(5, 10, TimeUnit.SECONDS), "lock can be acquired after release");
		competitor.unlock();
		var message = UUID.randomUUID().toString();
		client.getTopic(cfg.getContextKeyPrefix() + id + ":topic", StringCodec.INSTANCE).publish(message);
		check(message.equals(messages.poll(10, TimeUnit.SECONDS)), "existing pubsub subscription delivery");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
