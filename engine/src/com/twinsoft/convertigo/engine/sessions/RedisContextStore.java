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
import java.util.HashSet;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public final class RedisContextStore implements ContextStore {
	private static final String INDEX_CONTEXTS = "index:contexts";
	private static final String DATA_PREFIX = "__data:";

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
	private final StoreSerializer serializer = new StoreSerializer();
	private final RSet<String> contextsIndex;

	public RedisContextStore(RedisSessionConfiguration configuration) {
		this.configuration = configuration;
		this.client = createClient(configuration);
		this.contextsIndex = this.client.getSet(configuration.getContextKeyPrefix() + INDEX_CONTEXTS, StringCodec.INSTANCE);
	}

	private RMap<String, String> map(String contextId) {
		return this.client.getMap(this.configuration.contextKey(contextId), StringCodec.INSTANCE);
	}

	private static RedissonClient createClient(RedisSessionConfiguration cfg) {
		var config = new Config();
		if (cfg.getUsername() != null) {
			config.setUsername(cfg.getUsername());
		}
		if (cfg.getPassword() != null) {
			config.setPassword(cfg.getPassword());
		}
		config.useSingleServer().setAddress(cfg.getAddress())
				.setDatabase(cfg.getDatabase()).setTimeout(cfg.getTimeoutMillis());
		return Redisson.create(config);
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
					Engine.logEngine.debug("(RedisContextStore) Failed to restore httpState for context " + contextId, e);
				}
			}
			return ctx;
		} catch (Exception e) {
			log("(RedisContextStore) Failed to read context " + contextId, e);
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
					Engine.logEngine.debug("(RedisContextStore) Failed to serialize httpState for context " + context.contextID, e);
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

			String ctxKey = configuration.contextKey(context.contextID);
			client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_HSET_DEL_AND_TOUCH, RScript.ReturnType.VALUE,
					java.util.Collections.singletonList(ctxKey), args.toArray());
			try {
				contextsIndex.add(context.contextID);
			} catch (Exception e) {
				Engine.logEngine.debug("(RedisContextStore) Failed to update contexts index for " + context.contextID, e);
			}
		} catch (Exception e) {
			log("(RedisContextStore) Failed to save context " + context.contextID, e);
		}
	}

	@Override
	public void delete(String contextId) {
		try {
			map(contextId).delete();
			try {
				contextsIndex.remove(contextId);
			} catch (Exception e) {
				Engine.logEngine.debug("(RedisContextStore) Failed to remove " + contextId + " from contexts index", e);
			}
		} catch (Exception e) {
			log("(RedisContextStore) Failed to delete context " + contextId, e);
		}
	}

	@Override
	public void deleteBySessionPrefix(String sessionIdPrefix) {
		try {
			var prefix = this.configuration.getContextKeyPrefix() + "context:" + sessionIdPrefix;
			var options = KeysScanOptions.defaults().pattern(prefix + "*");
			var ctxKeyPrefix = this.configuration.getContextKeyPrefix() + "context:";
			for (var key : this.client.getKeys().getKeys(options)) {
				this.client.getMap(key, StringCodec.INSTANCE).delete();
				try {
					if (key != null && key.startsWith(ctxKeyPrefix)) {
						contextsIndex.remove(key.substring(ctxKeyPrefix.length()));
					}
				} catch (Exception e) {
					// ignore index cleanup failures
				}
			}
		} catch (Exception e) {
			log("(RedisContextStore) Failed to delete contexts by prefix " + sessionIdPrefix, e);
		}
	}

	@Override
	public void shutdown() {
		try {
			this.client.shutdown();
		} catch (Exception e) {
			log("(RedisContextStore) Failed to shutdown", e);
		}
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
				Engine.logEngine.debug("(RedisContextStore) Failed to deserialize context value '" + ctxKey + "' for context " + context.contextID, e);
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
				Engine.logEngine.debug("(RedisContextStore) Skip context value '" + key + "' (serialization failure) for context " + context.contextID, e);
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

	private void log(String message, Exception e) {
		try {
			if (Engine.logEngine != null) {
				if (e == null) {
					Engine.logEngine.warn(message);
				} else {
					Engine.logEngine.warn(message, e);
				}
			}
		} catch (Exception ignore) {
			// ignore
		}
	}
}
