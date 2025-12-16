/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the  Free Software Foundation;  either
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import com.twinsoft.convertigo.engine.Engine;

final class RedisSessionStore implements SessionStore {
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

	RedisSessionStore(RedisSessionConfiguration configuration) {
		this.configuration = configuration;
		this.client = this.createClient(configuration);
	}

	private RedissonClient createClient(RedisSessionConfiguration cfg) {
		var config = new Config();
		var singleServer = (SingleServerConfig) config.useSingleServer().setAddress(cfg.getAddress())
				.setDatabase(cfg.getDatabase()).setTimeout(cfg.getTimeoutMillis());
		if (cfg.getUsername() != null) {
			singleServer.setUsername(cfg.getUsername());
		}
		if (cfg.getPassword() != null) {
			singleServer.setPassword(cfg.getPassword());
		}
		return Redisson.create(config);
	}

	private RMap<String, String> map(String sessionId) {
		return this.client.getMap(this.configuration.key(sessionId), StringCodec.INSTANCE);
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

			client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_HSET_DEL_AND_TOUCH,
					RScript.ReturnType.VALUE, Collections.singletonList(configuration.key(sessionId)), args.toArray());
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
			map(sessionId).delete();
			debug("DEL(hash) " + sessionId);
		} catch (Exception e) {
			log("(RedisSessionStore) Failed to delete session " + sessionId, e);
		}
	}

	@Override
	public void shutdown() {
		try {
			this.client.shutdown();
		} catch (Exception e) {
			this.log("(RedisSessionStore) Failed to shutdown Redis client", e);
		}
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
