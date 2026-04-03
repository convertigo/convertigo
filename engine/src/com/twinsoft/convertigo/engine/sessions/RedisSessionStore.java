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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

final class RedisSessionStore implements SessionStore {
	private static final String INDEX_SESSIONS = "index:sessions";
	private static final String INDEX_COUNTED_SESSIONS = "index:countedSessions";
	private static final String INDEX_LEGACY_LICENSED_SESSIONS = "index:licensedSessions";
	private static final String INDEX_COUNTED_SESSIONS_BILLING = "index:countedSessionsBilling";
	private static final String TOMBSTONE_SESSION_PREFIX = "tombstone:session:";
	private static final String LUA_HSET_DEL_AND_TOUCH = ""
			+ "local key=KEYS[1]; local tomb=KEYS[2];\n"
			+ "if redis.call('EXISTS', tomb) == 1 then return 0 end\n"
			+ "local ttl=tonumber(ARGV[1]); local setCount=tonumber(ARGV[2]);\n"
			+ "local idx=3;\n"
			+ "if setCount>0 then redis.call('HSET', key, unpack(ARGV, idx, idx + setCount*2 - 1)); idx = idx + setCount*2; end\n"
			+ "local delCount=tonumber(ARGV[idx]); idx = idx + 1;\n"
			+ "if delCount>0 then redis.call('HDEL', key, unpack(ARGV, idx, idx + delCount - 1)); end\n"
			+ "if ttl and ttl>0 then redis.call('PEXPIRE', key, ttl) end\n"
			+ "return 1\n";

	private final RedissonClient client;
	private final RedisSessionConfiguration configuration;
	private final RSet<String> sessionsIndex;
	private final RSet<String> countedSessionsIndex;
	private final RSet<String> legacyLicensedSessionsIndex;
	private final RMap<String, String> countedSessionsBillingIndex;

	RedisSessionStore(RedisSessionConfiguration configuration) {
		this.configuration = configuration;
		this.client = RedisClients.getClient(configuration);
		this.sessionsIndex = this.client.getSet(configuration.getContextKeyPrefix() + INDEX_SESSIONS, StringCodec.INSTANCE);
		this.countedSessionsIndex = this.client.getSet(configuration.getContextKeyPrefix() + INDEX_COUNTED_SESSIONS,
				StringCodec.INSTANCE);
		this.legacyLicensedSessionsIndex = this.client
				.getSet(configuration.getContextKeyPrefix() + INDEX_LEGACY_LICENSED_SESSIONS,
				StringCodec.INSTANCE);
		this.countedSessionsBillingIndex = this.client
				.getMap(configuration.getContextKeyPrefix() + INDEX_COUNTED_SESSIONS_BILLING, StringCodec.INSTANCE);
	}

	private RMap<String, String> map(String sessionId) {
		return this.client.getMap(this.configuration.key(sessionId), StringCodec.INSTANCE);
	}

	Set<String> readSessionIds() {
		try {
			return sessionsIndex.readAll();
		} catch (Exception e) {
			return Set.of();
		}
	}

	int estimateCountedSessions() {
		return readCountedSessionIds().size();
	}

	int countCountedSessions() {
		return collectCountedSessionIds().size();
	}

	Set<String> readCountedSessionIds() {
		return collectCountedSessionIds();
	}

	private Set<String> collectCountedSessionIds() {
		var staleSessionIds = new ArrayList<String>();
		var validSessionIds = new HashSet<String>();
		for (var sessionId : readCountedSessionIdsIndex()) {
			if (sessionId == null || sessionId.isBlank()) {
				continue;
			}
			if (readMeta(sessionId) == null) {
				staleSessionIds.add(sessionId);
				continue;
			}
			validSessionIds.add(sessionId);
		}
		if (!staleSessionIds.isEmpty()) {
			int score = validSessionIds.size() + staleSessionIds.size();
			for (var sessionId : staleSessionIds) {
				emitStopBilling(sessionId, score--);
			}
			try {
				countedSessionsIndex.removeAll(staleSessionIds);
				legacyLicensedSessionsIndex.removeAll(staleSessionIds);
			} catch (Exception e) {
				debug("Failed to cleanup counted sessions index: " + e.getMessage());
			}
		}
		return validSessionIds;
	}

	boolean upsertCountedSessionBillingSnapshot(HttpSession session) {
		if (session == null || !SessionAttribute.isCounted(session)) {
			return false;
		}
		if (Engine.theApp == null || Engine.theApp.billingManager == null || !Engine.theApp.billingManager.hasActiveManagers()) {
			return false;
		}
		try {
			String raw = buildBillingSnapshot(session);
			String previous = countedSessionsBillingIndex.putIfAbsent(session.getId(), raw);
			if (previous == null) {
				return true;
			}
			countedSessionsBillingIndex.fastPut(session.getId(), raw);
		} catch (Exception e) {
			debug("Failed to update counted session billing snapshot for " + session.getId() + ": " + e.getMessage());
		}
		return false;
	}

	void syncCountedSessionBillingAuthenticatedUser(HttpSession session) {
		if (session == null || !SessionAttribute.isCounted(session)) {
			return;
		}
		if (Engine.theApp == null || Engine.theApp.billingManager == null || !Engine.theApp.billingManager.hasActiveManagers()) {
			return;
		}
		try {
			String authenticatedUser = SessionAttribute.authenticatedUser.get(session, "");
			if (authenticatedUser == null || authenticatedUser.isBlank()) {
				return;
			}
			String raw = countedSessionsBillingIndex.get(session.getId());
			if (raw == null || raw.isBlank()) {
				return;
			}
			var json = new JSONObject(raw);
			if (!authenticatedUser.equals(json.optString("authenticatedUser"))) {
				json.put("authenticatedUser", authenticatedUser);
				countedSessionsBillingIndex.fastPut(session.getId(), json.toString());
			}
		} catch (Exception e) {
			debug("Failed to sync counted session billing authenticated user for " + session.getId() + ": " + e.getMessage());
		}
	}

	private String buildBillingSnapshot(HttpSession session) throws Exception {
		var json = new JSONObject();
		json.put("creationTime", session.getCreationTime());
		json.put("clientIP", SessionAttribute.clientIP.get(session, ""));
		json.put("userAgent", SessionAttribute.userAgent.get(session, ""));
		json.put("authenticatedUser", SessionAttribute.authenticatedUser.get(session, ""));
		json.put("deviceUUID", SessionAttribute.deviceUUID.get(session, ""));
		return json.toString();
	}

	private void emitStopBilling(String sessionId, int score) {
		try {
			if (sessionId == null || sessionId.isBlank()) {
				return;
			}
			String raw = countedSessionsBillingIndex.remove(sessionId);
			if (raw == null || raw.isBlank()) {
				return;
			}
			if (Engine.theApp == null || Engine.theApp.billingManager == null || !Engine.theApp.billingManager.hasActiveManagers()) {
				return;
			}
			var json = new JSONObject(raw);
			Engine.theApp.billingManager.insertBillingSession("stop", json.optLong("creationTime"),
					sessionId, json.optString("clientIP"), json.optString("userAgent"),
					json.optString("authenticatedUser"), json.optString("deviceUUID"), Math.max(score, 0));
		} catch (Exception e) {
			debug("Failed to emit stop billing for " + sessionId + ": " + e.getMessage());
		}
	}

	private Set<String> readCountedSessionIdsIndex() {
		try {
			var sessionIds = new HashSet<String>(countedSessionsIndex.readAll());
			sessionIds.addAll(legacyLicensedSessionsIndex.readAll());
			return sessionIds;
		} catch (Exception e) {
			try {
				return legacyLicensedSessionsIndex.readAll();
			} catch (Exception ignore) {
				return Set.of();
			}
		}
	}

	@Override
	public SessionStoreMeta readMeta(String sessionId) {
		try {
			var rmap = map(sessionId);
			if (!rmap.isExists()) {
				debug("MISS " + sessionId);
				return null;
			}
			var meta = rmap.getAll(Set.of(SessionStoreKeys.META_CREATION, SessionStoreKeys.META_LAST_ACCESS,
					SessionStoreKeys.META_MAX_INACTIVE));
			if (meta == null || meta.isEmpty()) {
				debug("MISS(meta) " + sessionId);
				return null;
			}
			var now = System.currentTimeMillis();
			var creationTime = parseLong(meta.get(SessionStoreKeys.META_CREATION), now);
			var lastAccessedTime = parseLong(meta.get(SessionStoreKeys.META_LAST_ACCESS), creationTime);
			var maxInactive = parseInt(meta.get(SessionStoreKeys.META_MAX_INACTIVE), configuration.getDefaultTtlSeconds());
			debug("HIT(meta) " + sessionId);
			return new SessionStoreMeta(creationTime, lastAccessedTime, maxInactive);
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to read session meta " + sessionId, e);
			return null;
		}
	}

	@Override
	public String readAttribute(String sessionId, String name) {
		if (name == null) {
			return null;
		}
		try {
			return map(sessionId).get(name);
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to read attribute '" + name + "' for session " + sessionId, e);
			return null;
		}
	}

	@Override
	public Set<String> readAttributeNames(String sessionId) {
		try {
			var keys = map(sessionId).readAllKeySet();
			if (keys == null || keys.isEmpty()) {
				return Set.of();
			}
			var names = new HashSet<String>();
			for (var key : keys) {
				if (key != null && !key.startsWith(SessionStoreKeys.META_PREFIX)) {
					names.add(key);
				}
			}
			return names;
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to read attribute names for session " + sessionId, e);
			return Set.of();
		}
	}

	@Override
	public void writeDelta(String sessionId, Map<String, String> hset, Set<String> hdel, long ttlMillis) {
		try {
			boolean keepCountedSession = hset != null && hset.containsKey(SessionAttribute.countedSession.value());
			boolean dropCountedSession = hdel != null
					&& (hdel.contains(SessionAttribute.countedSession.value())
							|| hdel.contains(SessionAttribute.legacyLicensedSession.value()));
			var setCount = hset != null ? hset.size() : 0;
			var delCount = hdel != null ? hdel.size() : 0;
			var args = new ArrayList<Object>(2 + setCount * 2 + 1 + delCount);
			args.add(ttlMillis);
			args.add(setCount);
			if (setCount > 0) {
				for (var entry : hset.entrySet()) {
					args.add(entry.getKey());
					args.add(entry.getValue());
				}
			}
			args.add(delCount);
			if (delCount > 0) {
				args.addAll(hdel);
			}

			String key = configuration.key(sessionId);
			String tombstone = configuration.getContextKeyPrefix() + TOMBSTONE_SESSION_PREFIX + sessionId;
			Number written = client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_HSET_DEL_AND_TOUCH,
					RScript.ReturnType.LONG, java.util.List.of(key, tombstone), args.toArray());
			try {
				if (written != null && written.longValue() == 1L) {
					sessionsIndex.add(sessionId);
					if (keepCountedSession) {
						countedSessionsIndex.add(sessionId);
						legacyLicensedSessionsIndex.remove(sessionId);
					} else if (dropCountedSession) {
						countedSessionsIndex.remove(sessionId);
						legacyLicensedSessionsIndex.remove(sessionId);
					}
				}
			} catch (Exception e) {
				debug("Failed to update sessions index for " + sessionId + ": " + e.getMessage());
			}
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				debug("FLUSH " + sessionId + " set=" + setCount + " del=" + delCount + " ttlMillis=" + ttlMillis);
			}
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to flush delta for session " + sessionId, e);
		}
	}

	@Override
	public void delete(String sessionId) {
		try {
			int countedScore = 0;
			try {
				var counted = map(sessionId).getAll(Set.of(SessionAttribute.countedSession.value(),
						SessionAttribute.legacyLicensedSession.value()));
				if ("true".equals(counted.get(SessionAttribute.countedSession.value()))
						|| "true".equals(counted.get(SessionAttribute.legacyLicensedSession.value()))) {
					countedScore = countCountedSessions();
				}
			} catch (Exception ignore) {
				// ignore billing pre-read failures
			}
			if (countedScore > 0) {
				emitStopBilling(sessionId, countedScore);
			} else {
				countedSessionsBillingIndex.remove(sessionId);
			}
			try {
				int ttlSeconds = configuration.getDefaultTtlSeconds();
				try {
					var raw = map(sessionId).get(SessionStoreKeys.META_MAX_INACTIVE);
					if (raw != null) {
						ttlSeconds = parseInt(raw, ttlSeconds);
					}
				} catch (Exception ignore) {
					// ignore
				}
				ttlSeconds = Math.max(ttlSeconds, 60);
				String tombstoneKey = configuration.getContextKeyPrefix() + TOMBSTONE_SESSION_PREFIX + sessionId;
				RBucket<String> bucket = client.getBucket(tombstoneKey, StringCodec.INSTANCE);
				bucket.set("1", Duration.ofSeconds(ttlSeconds));
			} catch (Exception ignore) {
				// ignore tombstone failures
			}
			map(sessionId).delete();
			try {
				sessionsIndex.remove(sessionId);
				countedSessionsIndex.remove(sessionId);
				legacyLicensedSessionsIndex.remove(sessionId);
				countedSessionsBillingIndex.remove(sessionId);
			} catch (Exception e) {
				debug("Failed to remove " + sessionId + " from sessions index: " + e.getMessage());
			}
			debug("DEL(hash) " + sessionId);
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to delete session " + sessionId, e);
		}
	}

	@Override
	public void shutdown() {
		// Shared JVM-wide Redis client is shut down by RedisClients.
	}

	private void log(String message, Exception e) {
		try {
			if (Engine.logEngine != null) {
				Engine.logEngine.warn(message, e);
			}
		} catch (Exception ignore) {
			// ignore
		}
	}

	private void debug(String message) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(RedisSessionStore) " + message);
			}
		} catch (Exception ignore) {
			// ignore
		}
	}

	private static long parseLong(String value, long defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value);
		} catch (Exception ignore) {
			return defaultValue;
		}
	}

	private static int parseInt(String value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (Exception ignore) {
			return defaultValue;
		}
	}
}
