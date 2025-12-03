package com.twinsoft.convertigo.engine.sessions;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.RScript;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

/**
 * Experimental per-attribute session store backed by a Redis hash. Enables
 * incremental attribute writes instead of rewriting the entire SessionData
 * blob. Enabled with system property
 * convertigo.engine.session.redis.hash.experimental=true
 */
final class RedisHashSessionStore implements SessionStore {
	private static final String META_CREATION = "__meta:creationTime";
	private static final String META_LAST_ACCESS = "__meta:lastAccessedTime";
	private static final String META_MAX_INACTIVE = "__meta:maxInactiveInterval";
	private static final String LUA_HGETALL_AND_TOUCH = ""
			+ "local key=KEYS[1]; local ttl=tonumber(ARGV[1])\n"
			+ "local res=redis.call('HGETALL', key)\n"
			+ "if ttl and ttl>0 then redis.call('PEXPIRE', key, ttl) end\n"
			+ "return res\n";
	private static final String LUA_HSET_DEL_AND_TOUCH = ""
			+ "local key=KEYS[1]; local ttl=tonumber(ARGV[1]); local setCount=tonumber(ARGV[2]);\n"
			+ "local idx=3;\n"
			+ "if setCount>0 then redis.call('HSET', key, unpack(ARGV, idx, idx + setCount*2 - 1)); idx = idx + setCount*2; end\n"
			+ "local delCount=tonumber(ARGV[idx]); idx = idx + 1;\n"
			+ "if delCount>0 then redis.call('HDEL', key, unpack(ARGV, idx, idx + delCount - 1)); end\n"
			+ "if ttl and ttl>0 then redis.call('PEXPIRE', key, ttl) end\n"
			+ "return true\n";

	private final RedissonClient client;
	private final RedisSessionConfiguration configuration;
	private final ScheduledExecutorService scheduler;
	private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	RedisHashSessionStore(RedisSessionConfiguration configuration) {
		this.configuration = configuration;
		this.client = createClient(configuration);
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			var t = new Thread(r, "redis-hash-flush");
			t.setDaemon(true);
			return t;
		});
	}

	private RedissonClient createClient(RedisSessionConfiguration cfg) {
		var config = new Config();
		var singleServer = config.useSingleServer()
				.setAddress(cfg.getAddress())
				.setDatabase(cfg.getDatabase())
				.setTimeout(cfg.getTimeoutMillis());
		var username = cfg.getUsername();
		if (username != null) {
			singleServer.setUsername(username);
		}
		var password = cfg.getPassword();
		if (password != null) {
			singleServer.setPassword(password);
		}
		return Redisson.create(config);
	}

	private RMap<String, String> map(String sessionId) {
		return client.getMap(configuration.key(sessionId), StringCodec.INSTANCE);
	}

	@Override
	public SessionData read(String sessionId) {
		try {
			var ttlMillis = configuration.getDefaultTtlSeconds() > 0 ? configuration.getDefaultTtlSeconds() * 1000L : 0L;
			@SuppressWarnings("unchecked")
			var entries = (java.util.List<Object>) client.getScript(StringCodec.INSTANCE)
					.eval(RScript.Mode.READ_WRITE, LUA_HGETALL_AND_TOUCH, RScript.ReturnType.MULTI,
							java.util.Collections.singletonList(configuration.key(sessionId)), ttlMillis);
			if (entries == null || entries.isEmpty()) {
				debug("MISS " + sessionId);
				return null;
			}

			var mapSnapshot = new java.util.HashMap<String, String>(entries.size() / 2);
			for (int i = 0; i < entries.size(); i += 2) {
				mapSnapshot.put((String) entries.get(i), (String) entries.get(i + 1));
			}

			var creationTime = readLong(mapSnapshot.get(META_CREATION));
			var lastAccess = readLong(mapSnapshot.get(META_LAST_ACCESS));
			var maxInactive = readInt(mapSnapshot.get(META_MAX_INACTIVE), configuration.getDefaultTtlSeconds());

			var data = SessionData.newSession(sessionId, configuration.getDefaultTtlSeconds());
			var now = System.currentTimeMillis();
			data.setCreationTime(creationTime > 0 ? creationTime : now);
			data.setLastAccessedTime(lastAccess > 0 ? lastAccess : now);
			data.setMaxInactiveInterval(maxInactive);
			data.setNew(false);

			for (var entry : mapSnapshot.entrySet()) {
				var key = entry.getKey();
				if (key.startsWith("__meta:")) {
					continue;
				}
				var val = deserialize(key, entry.getValue());
				if (val != null) {
					data.setAttribute(key, val);
				}
			}
			debug("HIT " + sessionId + " hashSize=" + mapSnapshot.size());
			return data;
		} catch (Exception e) {
			log("(RedisHashSessionStore) Failed to read session " + sessionId, e);
			return null;
		}
	}

	@Override
	public void save(SessionData session) {
		if (session == null) {
			return;
		}
		try {
			var sanitized = SessionAttributeSanitizer.sanitize(session);
			var sessionId = sanitized.getId();
			var pendingEntry = pending.compute(sessionId, (id, existing) -> {
				if (existing == null) {
					return new Pending(SessionData.copyOf(sanitized));
				}
				existing.data = SessionData.copyOf(sanitized);
				return existing;
			});
			pendingEntry.dirty = true;
			scheduleFlush(sessionId, pendingEntry);
		} catch (Exception e) {
			log("(RedisHashSessionStore) Failed to save session " + session.getId(), e);
		}
	}

	private void scheduleFlush(String sessionId, Pending p) {
		synchronized (p) {
			if (p.future != null && !p.future.isDone()) {
				p.future.cancel(false);
			}
			p.future = scheduler.schedule(() -> flushPending(sessionId, p), 20, TimeUnit.MILLISECONDS);
		}
	}

	private void flushPending(String sessionId, Pending p) {
		SessionData data;
		synchronized (p) {
			if (!p.dirty) {
				return;
			}
			data = SessionData.copyOf(p.data);
			p.dirty = false;
			p.future = null;
		}
		try {
			var bulk = new java.util.HashMap<String, String>();
			var meta = new java.util.HashMap<String, String>();
			meta.put(META_CREATION, writeLong(data.getCreationTime()));
			meta.put(META_LAST_ACCESS, writeLong(data.getLastAccessedTime()));
			meta.put(META_MAX_INACTIVE, writeInt(data.getMaxInactiveInterval()));

			// Only push meta keys that changed since last flush
			for (var entry : meta.entrySet()) {
				var k = entry.getKey();
				var v = entry.getValue();
				var previous = p.lastMeta.get(k);
				if (!v.equals(previous)) {
					bulk.put(k, v);
				}
			}

			var newAttrsSnapshot = new java.util.HashMap<String, String>();
			for (Map.Entry<String, Object> entry : data.getAttributes().entrySet()) {
				var name = entry.getKey();
				var value = entry.getValue();
				if (value != null) {
					var serialized = serialize(name, value);
					newAttrsSnapshot.put(name, serialized);
					var previous = p.lastAttrs.get(name);
					if (!serialized.equals(previous)) {
						bulk.put(name, serialized);
					}
				}
			}

			// detect deletions (keys previously stored but now absent)
			var deletes = new java.util.ArrayList<String>();
			for (var prevKey : p.lastAttrs.keySet()) {
				if (!newAttrsSnapshot.containsKey(prevKey)) {
					deletes.add(prevKey);
				}
			}

			// Only write fields that changed vs existing hash to avoid full HMSET
			if (!bulk.isEmpty() || !deletes.isEmpty()) {
				var ttlMillis = data.getMaxInactiveInterval() > 0 ? data.getMaxInactiveInterval() * 1000L
						: configuration.getDefaultTtlSeconds() * 1000L;
				var setCount = bulk.size();
				var delCount = deletes.size();
				var args = new java.util.ArrayList<Object>(2 + setCount * 2 + 1 + delCount);
				args.add(ttlMillis);
				args.add(setCount);
				for (var e : bulk.entrySet()) {
					args.add(e.getKey());
					args.add(e.getValue());
				}
				args.add(delCount);
				args.addAll(deletes);

				client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_HSET_DEL_AND_TOUCH,
						RScript.ReturnType.VALUE, java.util.Collections.singletonList(configuration.key(sessionId)),
						args.toArray());
				// remember latest meta snapshot
				p.lastMeta.clear();
				p.lastMeta.putAll(meta);
				// remember latest attrs snapshot
				p.lastAttrs.clear();
				p.lastAttrs.putAll(newAttrsSnapshot);
			}

			var ttlSeconds = data.getMaxInactiveInterval() > 0 ? data.getMaxInactiveInterval()
					: configuration.getDefaultTtlSeconds();
			debug("SAVE(hash/coalesce) " + sessionId + ", ttl=" + ttlSeconds + ", fields=" + bulk.size());
		} catch (Exception e) {
			log("(RedisHashSessionStore) Failed to flush session " + sessionId, e);
		}
	}

	private static final class Pending {
		SessionData data;
		boolean dirty;
		ScheduledFuture<?> future;
		final java.util.Map<String, String> lastMeta = new java.util.HashMap<>();
		final java.util.Map<String, String> lastAttrs = new java.util.HashMap<>();

		Pending(SessionData data) {
			this.data = data;
			this.dirty = true;
		}
	}

	@Override
	public void delete(String sessionId) {
		try {
			map(sessionId).delete();
			debug("DEL(hash) " + sessionId);
		} catch (Exception e) {
			log("(RedisHashSessionStore) Failed to delete session " + sessionId, e);
		}
	}

	@Override
	public void shutdown() {
		try {
			client.shutdown();
		} catch (Exception e) {
			log("(RedisHashSessionStore) Failed to shutdown Redis client", e);
		}
	}

	private String serialize(String key, Object value) throws Exception {
		var wrapper = new TypedValue(value);
		return MAPPER.writeValueAsString(wrapper);
	}

	private Object deserialize(String key, String value) {
		if (value == null) {
			return null;
		}
		try {
			var hintAttr = SessionAttribute.fromValue(key);
			if (hintAttr != null && hintAttr.expectedClass() != null) {
				return MAPPER.readValue(value, MAPPER.getTypeFactory().constructType(hintAttr.expectedClass()));
			}
			var node = MAPPER.readTree(value);
			var clazzNode = node.get("clazz");
			var valNode = node.get("value");
			if (clazzNode == null || valNode == null) {
				return MAPPER.convertValue(node, Object.class);
			}
			var className = clazzNode.asText();
			try {
				var cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
				return MAPPER.convertValue(valNode, cls);
			} catch (ClassNotFoundException e) {
				return MAPPER.convertValue(valNode, Object.class);
			}
		} catch (Exception e) {
			log("(RedisHashSessionStore) Failed to deserialize attribute", e);
			return null;
		}
	}

	private String writeLong(long v) throws Exception {
		return Long.toString(v);
	}

	private String writeInt(int v) throws Exception {
		return Integer.toString(v);
	}

	private long readLong(String value) {
		var o = deserialize(null, value);
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o));
		} catch (Exception ignore) {
			return 0L;
		}
	}

	private int readInt(String value, int defaultValue) {
		var o = deserialize(null, value);
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o));
		} catch (Exception ignore) {
			return defaultValue;
		}
	}

	private void log(String message, Exception e) {
		try {
			if (Engine.logEngine != null) {
				Engine.logEngine.warn(message, e);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	private void debug(String message) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(RedisHashSessionStore) " + message);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	private static final class TypedValue {
		public String clazz;
		public Object value;

		TypedValue(Object value) {
			this.value = value;
			this.clazz = value != null ? value.getClass().getName() : null;
		}
	}
}
