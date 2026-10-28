/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sessions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.Engine;

/**
 * Contexts stored in Redis hashes, with two indexes kept by the scripts that write and delete them: the contexts of each
 * session (a set that lives at least as long as the session and as its contexts), and all the contexts scored by their
 * expiry time (a sorted set: the live ones are read by score, the expired ones purged by score). The former set of all
 * the contexts, which kept the expired ones, is moved to these indexes and removed.
 */
public final class RedisContextStore implements ContextStore {
	private static final String INDEX_CONTEXTS_FORMER = "index:contexts";
	private static final String INDEX_CONTEXTS_BY_EXPIRY = "index:contextsByExpiry";
	private static final String INDEX_SESSION_CONTEXTS = "index:sessionContexts:";
	private static final String DATA_PREFIX = "__data:";
	private static final int FORMER_INDEX_BATCH = 500;
	private static final long PURGE_EVERY_MILLIS = 60_000L;
	private static final long FORMER_INDEX_CHECK_EVERY_MILLIS = 300_000L;

	// Adds a context to the index of its session, which lives at least as long as each of its contexts (ttl in
	// milliseconds, 0 for a context that never expires)
	private static final String LUA_INDEX_SESSION = ""
			+ "local function indexSession(sessionIndex, id, ttl)\n"
			+ "local current=redis.call('PTTL', sessionIndex); redis.call('SADD', sessionIndex, id)\n"
			+ "if ttl>0 then if current==-2 or (current>=0 and current<ttl) then redis.call('PEXPIRE', sessionIndex, ttl) end\n"
			+ "else redis.call('PERSIST', sessionIndex) end\n"
			+ "end\n";

	// KEYS = context hash, contexts by expiry, index of the session (if any);
	// ARGV = ttl, set count, fields and values, delete count, fields, context id, expiry score.
	private static final String LUA_HSET_DEL_AND_TOUCH = LUA_INDEX_SESSION
			+ "local key=KEYS[1]; local ttl=tonumber(ARGV[1]); local setCount=tonumber(ARGV[2]);\n"
			+ "local idx=3;\n"
			+ "if setCount>0 then redis.call('HSET', key, unpack(ARGV, idx, idx + setCount*2 - 1)); idx = idx + setCount*2; end\n"
			+ "local delCount=tonumber(ARGV[idx]); idx = idx + 1;\n"
			+ "if delCount>0 then redis.call('HDEL', key, unpack(ARGV, idx, idx + delCount - 1)); idx = idx + delCount; end\n"
			+ "if ttl and ttl>0 then redis.call('PEXPIRE', key, ttl) end\n"
			+ "local id=ARGV[idx]; redis.call('ZADD', KEYS[2], ARGV[idx + 1], id)\n"
			+ "if KEYS[3] then indexSession(KEYS[3], id, ttl or 0) end\n"
			+ "return true\n";

	// KEYS = contexts by expiry, index of the session (if any), context hashes; ARGV = context ids.
	private static final String LUA_DELETE = ""
			+ "local first=#KEYS - #ARGV + 1\n"
			+ "for i=1,#ARGV do redis.call('DEL', KEYS[first + i - 1]) end\n"
			+ "redis.call('ZREM', KEYS[1], unpack(ARGV))\n"
			+ "if first==3 then redis.call('SREM', KEYS[2], unpack(ARGV)) end\n"
			+ "return #ARGV\n";

	// KEYS = contexts by expiry, then for each context its hash and the index of its session (its hash again without
	// session); ARGV = now, then for each context its id and 1 if it has a session. The ones still stored are indexed.
	private static final String LUA_INDEX_FORMER = LUA_INDEX_SESSION
			+ "local now=tonumber(ARGV[1]); local moved=0\n"
			+ "for i=1,(#ARGV - 1) / 2 do\n"
			+ "local id=ARGV[2*i]; local pttl=redis.call('PTTL', KEYS[2*i])\n"
			+ "if pttl~=-2 then moved=moved + 1\n"
			+ "if pttl>0 then redis.call('ZADD', KEYS[1], string.format('%.0f', now + pttl), id) else redis.call('ZADD', KEYS[1], '+inf', id) end\n"
			+ "if ARGV[2*i + 1]=='1' then indexSession(KEYS[2*i + 1], id, pttl>0 and pttl or 0) end\n"
			+ "end\n"
			+ "end\n"
			+ "return moved\n";

	private final RedissonClient client;
	private final RedisSessionConfiguration configuration;
	private final StoreSerializer serializer = new StoreSerializer();
	private final RScoredSortedSet<String> contextsByExpiry;
	private volatile long nextPurge = 0L;
	private volatile long nextFormerIndexCheck = 0L;

	public RedisContextStore(RedisSessionConfiguration configuration) {
		this.configuration = configuration;
		this.client = RedisClients.getClient(configuration);
		this.contextsByExpiry = this.client.getScoredSortedSet(configuration.getContextKeyPrefix() + INDEX_CONTEXTS_BY_EXPIRY,
				StringCodec.INSTANCE);
	}

	/**
	 * The session of a stored context: the start of its id, before the first '_'. None for the contexts of the pools and
	 * of the Studio.
	 */
	static String sessionIdOf(String contextId) {
		if (contextId == null || contextId.startsWith(ContextManager.POOL_CONTEXT_ID_PREFIX)
				|| contextId.startsWith(ContextManager.STUDIO_CONTEXT_PREFIX)) {
			return null;
		}
		int idx = contextId.indexOf('_');
		return idx > 0 ? contextId.substring(0, idx) : null;
	}

	/** Key of the index of the stored contexts of a session. */
	static String sessionContextsKey(RedisSessionConfiguration configuration, String sessionId) {
		return configuration.getContextKeyPrefix() + INDEX_SESSION_CONTEXTS + sessionId;
	}

	private String sessionContextsKey(String sessionId) {
		return sessionContextsKey(configuration, sessionId);
	}

	private RMap<String, String> map(String contextId) {
		return this.client.getMap(this.configuration.contextKey(contextId), StringCodec.INSTANCE);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Context read(String contextId) {
		try {
			var rmap = map(contextId);
			var snapshot = rmap.readAllMap();
			if (snapshot == null || snapshot.isEmpty()) {
				return null;
			}
			var ctx = new Context(contextId);
			serializer.populate(ctx, snapshot);
			restoreContextData(ctx, snapshot);
			var rawHttpState = snapshot.get("__meta:httpState");
			if (rawHttpState != null && !rawHttpState.isBlank()) {
				try {
					var root = JsonCodec.MAPPER.readTree(rawHttpState);

					Integer cookiePolicy = null;
					var cookiePolicyNode = root.get("cookiePolicy");
					if (cookiePolicyNode != null && cookiePolicyNode.isNumber()) {
						cookiePolicy = cookiePolicyNode.intValue();
					}
					if (cookiePolicy != null && cookiePolicy < 0) {
						cookiePolicy = null;
					}

					var cookies = new ArrayList<Cookie>();
					var cookiesNode = root.get("cookies");
					if (cookiesNode != null && cookiesNode.isArray()) {
						for (var cookieNode : cookiesNode) {
							var domain = cookieNode.path("domain").asText(null);
							var name = cookieNode.path("name").asText(null);
							var value = cookieNode.path("value").asText(null);
							if (name == null || domain == null) {
								continue;
							}

							try {
								var cookie = new Cookie(domain, name, value);

								var path = cookieNode.path("path").asText(null);
								if (path != null) {
									cookie.setPath(path);
								}

								var expiryNode = cookieNode.get("expiry");
								if (expiryNode != null && expiryNode.isNumber()) {
									cookie.setExpiryDate(new java.util.Date(expiryNode.longValue()));
								}

								var secureNode = cookieNode.get("secure");
								if (secureNode != null && secureNode.isBoolean()) {
									cookie.setSecure(secureNode.booleanValue());
								}

								var versionNode = cookieNode.get("version");
								if (versionNode != null && versionNode.isNumber()) {
									cookie.setVersion(versionNode.intValue());
								}

								var comment = cookieNode.path("comment").asText(null);
								if (comment != null) {
									cookie.setComment(comment);
								}

								cookies.add(cookie);
							} catch (Exception e) {
								// ignore invalid cookie
							}
						}
					}

					if (!cookies.isEmpty() || cookiePolicy != null) {
						var httpState = new HttpState();
						if (cookiePolicy != null) {
							try {
								httpState.setCookiePolicy(cookiePolicy);
							} catch (Exception e) {
								// ignore
							}
						}
						for (var cookie : cookies) {
							httpState.addCookie(cookie);
						}
						ctx.httpState = httpState;
					}
				} catch (Exception e) {
					Engine.logRedis.debug("(RedisContextStore) Failed to restore httpState for context " + contextId, e);
				}
			}
			return ctx;
		} catch (Exception e) {
			Engine.logRedis.warn("(RedisContextStore) Failed to read context " + contextId, e);
			return null;
		}
	}
	
	@Override
	public Long readLastAccessTime(String contextId) {
		var raw = map(contextId).get("lastAccessTime");
		if (raw == null) {
			return null;
		}
		raw = raw.trim();
		if (raw.length() >= 2 && raw.charAt(0) == '"' && raw.charAt(raw.length() - 1) == '"') {
			raw = raw.substring(1, raw.length() - 1);
		}
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void save(Context context, int ttlSeconds) {
		if (context == null) {
			return;
		}
		try {
			var snapshot = serializer.serialize(context);
			addAdminMeta(context, snapshot);
			persistContextData(context, snapshot);
			var httpState = context.httpState;
			if (httpState != null) {
				try {
					var cookies = new ArrayList<java.util.Map<String, Object>>();
					for (Cookie cookie : httpState.getCookies()) {
						var cookieMap = new java.util.LinkedHashMap<String, Object>();
						cookieMap.put("name", cookie.getName());
						cookieMap.put("value", cookie.getValue());
						cookieMap.put("domain", cookie.getDomain());
						var path = cookie.getPath();
						if (path != null) {
							cookieMap.put("path", path);
						}
						var expiryDate = cookie.getExpiryDate();
						if (expiryDate != null) {
							cookieMap.put("expiry", expiryDate.getTime());
						}
						if (cookie.getSecure()) {
							cookieMap.put("secure", true);
						}
						if (cookie.getVersion() != 0) {
							cookieMap.put("version", cookie.getVersion());
						}
						if (cookie.getComment() != null) {
							cookieMap.put("comment", cookie.getComment());
						}
						cookies.add(cookieMap);
					}

					Integer cookiePolicy = null;
					try {
						cookiePolicy = httpState.getCookiePolicy();
					} catch (Exception e) {
						// ignore
					}
					if (cookiePolicy != null && cookiePolicy < 0) {
						cookiePolicy = null;
					}

					if (!cookies.isEmpty() || cookiePolicy != null) {
						var stateMap = new java.util.LinkedHashMap<String, Object>();
						if (cookiePolicy != null) {
							stateMap.put("cookiePolicy", cookiePolicy);
						}
						if (!cookies.isEmpty()) {
							stateMap.put("cookies", cookies);
						}
						snapshot.put("__meta:httpState", JsonCodec.MAPPER.writeValueAsString(stateMap));
					}
				} catch (Exception e) {
					Engine.logRedis.debug("(RedisContextStore) Failed to serialize httpState for context " + context.contextID, e);
				}
			}
			var rmap = map(context.contextID);
			var existingKeys = new HashSet<>(rmap.readAllKeySet());
			var toRemove = new ArrayList<String>();
			for (var key : existingKeys) {
				if (!snapshot.containsKey(key)) {
					toRemove.add(key);
				}
			}
			int ttl = ttlSeconds > 0 ? ttlSeconds : configuration.getDefaultTtlSeconds();
			var ttlMillis = ttl > 0 ? ttl * 1000L : 0L;

			var setCount = snapshot.size();
			var delCount = toRemove.size();
			var args = new ArrayList<Object>(2 + setCount * 2 + 1 + delCount);
			args.add(ttlMillis);
			args.add(setCount);
			for (var entry : snapshot.entrySet()) {
				args.add(entry.getKey());
				args.add(entry.getValue());
			}
			args.add(delCount);
			args.addAll(toRemove);

			args.add(context.contextID);
			args.add(ttlMillis > 0 ? Long.toString(System.currentTimeMillis() + ttlMillis) : "+inf");

			var keys = new ArrayList<Object>(3);
			keys.add(configuration.contextKey(context.contextID));
			keys.add(contextsByExpiry.getName());
			var sessionId = sessionIdOf(context.contextID);
			if (sessionId != null) {
				keys.add(sessionContextsKey(sessionId));
			}
			client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_HSET_DEL_AND_TOUCH, RScript.ReturnType.VALUE,
					keys, args.toArray());
		} catch (Exception e) {
			Engine.logRedis.warn("(RedisContextStore) Failed to save context " + context.contextID, e);
		}
	}

	@Override
	public void delete(String contextId) {
		if (contextId == null) {
			return;
		}
		try {
			delete(sessionIdOf(contextId), List.of(contextId));
		} catch (Exception e) {
			Engine.logRedis.warn("(RedisContextStore) Failed to delete context " + contextId, e);
		}
	}

	/** Deletes contexts of one session (null for none) and drops them from the indexes, in one call. */
	private void delete(String sessionId, Collection<String> contextIds) {
		if (contextIds.isEmpty()) {
			return;
		}
		var keys = new ArrayList<Object>(contextIds.size() + 2);
		keys.add(contextsByExpiry.getName());
		if (sessionId != null) {
			keys.add(sessionContextsKey(sessionId));
		}
		for (var contextId : contextIds) {
			keys.add(configuration.contextKey(contextId));
		}
		client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_DELETE, RScript.ReturnType.LONG, keys,
				contextIds.toArray());
	}

	/**
	 * Deletes the stored contexts of a session ("&lt;session id&gt;_") from the index of the session, or the contexts of
	 * a pool from the live contexts: never a scan of the keys.
	 */
	@Override
	public void deleteBySessionPrefix(String sessionIdPrefix) {
		if (sessionIdPrefix == null || sessionIdPrefix.isEmpty()) {
			return;
		}
		try {
			var sessionId = sessionIdPrefix.endsWith("_") ? sessionIdOf(sessionIdPrefix) : null;
			if (sessionId != null && sessionIdPrefix.length() == sessionId.length() + 1) {
				delete(sessionId, readSessionContextIds(sessionId));
			} else {
				var contextIds = new ArrayList<String>();
				for (var contextId : readLiveContextIds()) {
					if (contextId.startsWith(sessionIdPrefix)) {
						contextIds.add(contextId);
					}
				}
				for (var contextId : contextIds) {
					delete(contextId);
				}
			}
		} catch (Exception e) {
			Engine.logRedis.warn("(RedisContextStore) Failed to delete contexts by prefix " + sessionIdPrefix, e);
		}
	}

	@Override
	public Set<String> readSessionContextIds(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return Set.of();
		}
		RSet<String> sessionContexts = client.getSet(sessionContextsKey(sessionId), StringCodec.INSTANCE);
		return sessionContexts.readAll();
	}

	@Override
	public Set<String> readLiveContextIds() {
		return new HashSet<>(contextsByExpiry.valueRange(System.currentTimeMillis(), false, Double.POSITIVE_INFINITY, true));
	}

	@Override
	public void forget(Collection<String> contextIds) {
		if (contextIds != null && !contextIds.isEmpty()) {
			contextsByExpiry.removeAll(contextIds);
		}
	}

	/**
	 * Called by the vulture of each instance: purges the expired contexts from the index by expiry, and moves the former
	 * index of all the contexts to the new ones at the first call, then again if an instance of a former version wrote it
	 * meanwhile (rolling update).
	 */
	@Override
	public void maintain() {
		long now = System.currentTimeMillis();
		if (now >= nextFormerIndexCheck) {
			nextFormerIndexCheck = now + FORMER_INDEX_CHECK_EVERY_MILLIS;
			try {
				moveFormerIndex();
			} catch (Exception e) {
				Engine.logRedis.warn("(RedisContextStore) Failed to move the former index of the contexts", e);
			}
		}
		if (now >= nextPurge) {
			nextPurge = now + PURGE_EVERY_MILLIS;
			try {
				int purged = contextsByExpiry.removeRangeByScore(Double.NEGATIVE_INFINITY, true, now, true);
				if (purged > 0) {
					Engine.logRedis.debug("(RedisContextStore) " + purged + " expired contexts purged from the index");
				}
			} catch (Exception e) {
				Engine.logRedis.debug("(RedisContextStore) Failed to purge the expired contexts from the index", e);
			}
		}
	}

	/** Moves the contexts of the former index that are still stored to the new indexes, and removes it. */
	int moveFormerIndex() {
		RSet<String> former = client.getSet(configuration.getContextKeyPrefix() + INDEX_CONTEXTS_FORMER, StringCodec.INSTANCE);
		int read = 0;
		int moved = 0;
		Set<String> batch;
		while (!(batch = former.removeRandom(FORMER_INDEX_BATCH)).isEmpty()) {
			read += batch.size();
			var keys = new ArrayList<Object>(1 + batch.size() * 2);
			var args = new ArrayList<Object>(1 + batch.size() * 2);
			keys.add(contextsByExpiry.getName());
			args.add(Long.toString(System.currentTimeMillis()));
			for (var contextId : batch) {
				var contextKey = configuration.contextKey(contextId);
				var sessionId = sessionIdOf(contextId);
				keys.add(contextKey);
				keys.add(sessionId != null ? sessionContextsKey(sessionId) : contextKey);
				args.add(contextId);
				args.add(sessionId != null ? "1" : "0");
			}
			Number count = client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_INDEX_FORMER,
					RScript.ReturnType.LONG, keys, args.toArray());
			moved += count == null ? 0 : count.intValue();
		}
		if (read > 0) {
			Engine.logRedis.info("(RedisContextStore) Former index of the contexts removed: " + read + " entries read, "
					+ moved + " contexts still stored moved to the new indexes");
		}
		return moved;
	}

	@Override
	public void shutdown() {
		// Shared JVM-wide Redis client is shut down by RedisClients.
	}

	private void restoreContextData(Context context, java.util.Map<String, String> snapshot) {
		if (context == null || snapshot == null || snapshot.isEmpty()) {
			return;
		}
		var codec = new ContextValueCodec();
		for (var entry : snapshot.entrySet()) {
			var key = entry.getKey();
			if (key == null || !key.startsWith(DATA_PREFIX)) {
				continue;
			}
			var ctxKey = key.substring(DATA_PREFIX.length());
			if (ctxKey.isEmpty()) {
				continue;
			}
			try {
				var value = codec.deserialize(entry.getValue());
				if (value != null) {
					context.set(ctxKey, value);
				}
			} catch (Exception e) {
				Engine.logRedis.debug("(RedisContextStore) Failed to deserialize context value '" + ctxKey + "' for context " + context.contextID, e);
			}
		}
	}

	private void persistContextData(Context context, java.util.Map<String, String> snapshot) {
		if (context == null || snapshot == null) {
			return;
		}
		java.util.Set<String> keys;
		try {
			keys = new java.util.HashSet<>(context.keys());
		} catch (Exception e) {
			return;
		}
		if (keys.isEmpty()) {
			return;
		}
		var codec = new ContextValueCodec();
		for (var key : keys) {
			if (key == null || key.isBlank()) {
				continue;
			}
			try {
				var value = context.get(key);
				if (value == null) {
					continue;
				}
				var raw = codec.serialize(value);
				if (raw != null) {
					snapshot.put(DATA_PREFIX + key, raw);
				}
			} catch (Exception e) {
				Engine.logRedis.debug("(RedisContextStore) Skip context value '" + key + "' (serialization failure) for context " + context.contextID, e);
			}
		}
	}

	private static void addAdminMeta(Context context, java.util.Map<String, String> snapshot) {
		if (context == null || snapshot == null) {
			return;
		}
		try {
			putMeta(snapshot, "projectName", context.projectName);
			putMeta(snapshot, "connectorName", context.connectorName);
			putMeta(snapshot, "sequenceName", context.sequenceName);
			putMeta(snapshot, "transactionName", context.transactionName);
			putMeta(snapshot, "remoteHost", context.remoteHost);
			putMeta(snapshot, "remoteAddr", context.remoteAddr);
			putMeta(snapshot, "userAgent", context.userAgent);
			putMeta(snapshot, "waitingRequests", context.waitingRequests);
			String requested = context.transactionName != null ? context.transactionName : context.sequenceName;
			putMeta(snapshot, "requested", requested);
		} catch (Exception e) {
			// ignore meta enrichment failures
		}
	}

	private static void putMeta(java.util.Map<String, String> snapshot, String name, Object value) throws Exception {
		if (name == null || name.isBlank() || value == null) {
			return;
		}
		snapshot.put("__meta:" + name, JsonCodec.MAPPER.writeValueAsString(value));
	}

}
