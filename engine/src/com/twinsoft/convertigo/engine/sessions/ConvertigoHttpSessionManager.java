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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.AbstractManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;

public final class ConvertigoHttpSessionManager implements PropertyChangeEventListener, AbstractManager {
	private static volatile ConvertigoHttpSessionManager instance;

	private final Object mutex = new Object();
	private volatile SessionProvider provider;
	private volatile SessionStoreMode storeMode;

	private ConvertigoHttpSessionManager() {
	}

	public HttpSession getSession(HttpServletRequest request, boolean create) {
		if (request instanceof HttpServletRequestWrapper wrapper) {
			request = (HttpServletRequest) wrapper.getRequest();
		}
		return provider.getSession(request, create);
	}

	public SessionStoreMode getStoreMode() {
		return storeMode;
	}

	public static boolean isRedisMode() {
		return getInstance().getStoreMode() == SessionStoreMode.redis;
	}

	public static boolean isTomcatMode() {
		return getInstance().getStoreMode() == SessionStoreMode.tomcat;
	}

	public boolean tryTerminateSession(String sessionId, long timeoutMillis) {
		if (sessionId == null || sessionId.isBlank()) {
			return false;
		}
		try {
			if (Engine.theApp != null && Engine.theApp.contextManager != null) {
				var ok = Engine.theApp.contextManager.tryRemoveAll(sessionId, timeoutMillis);
				if (!ok) {
					return false;
				}
			}
		} catch (Exception ignore) {
			// ignore context cleanup failures
		}
		try {
			if (provider == null) {
				return false;
			}
			if (storeMode == SessionStoreMode.redis) {
				return provider.terminateSession(sessionId);
			}
			return provider.terminateSession(sessionId);
		} catch (Exception e) {
			debug("tryTerminateSession failed: " + e.getMessage());
			return false;
		}
	}

	public int terminateAllSessions() {
		return terminateAllSessions(null);
	}

	public int terminateAllSessions(String sessionIdToKeep) {
		try {
			if (provider == null) {
				return 0;
			}
			if (storeMode != SessionStoreMode.redis) {
				return provider.terminateAllSessions();
			}
			var cfg = RedisClients.getConfiguration();
			var client = RedisClients.getClient();
			var contextsIndexKey = cfg.getContextKeyPrefix() + "index:contexts";
			org.redisson.api.RSet<String> contextsIndex = client.getSet(contextsIndexKey,
					org.redisson.client.codec.StringCodec.INSTANCE);

			var sessionsToSkip = new java.util.HashSet<String>();
			try {
				if (Engine.theApp != null && Engine.theApp.contextManager != null) {
					var allContextIds = new java.util.HashSet<String>(contextsIndex.readAll());
					try {
						var inflightKey = cfg.getContextKeyPrefix() + "inflight:contexts";
						org.redisson.api.RMapCache<String, String> inflightContexts = client.getMapCache(inflightKey,
								org.redisson.client.codec.StringCodec.INSTANCE);
						allContextIds.addAll(inflightContexts.readAllKeySet());
					} catch (Exception ignore) {
						// ignore inflight read failures
					}
					for (var contextId : allContextIds) {
						if (contextId == null || contextId.isBlank()) {
							continue;
						}
						var sid = extractSessionId(contextId);
						if (sid == null) {
							continue;
						}
						var lock = Engine.theApp.contextManager.tryLockContext(contextId, 0L);
						if (lock == null) {
							sessionsToSkip.add(sid);
						} else {
							try {
								lock.close();
							} catch (Exception ignore) {
							}
						}
					}
				}
			} catch (Exception ignore) {
				// ignore busy detection failures
			}

			var removed = 0;
			for (var sessionId : readCountedSessionIds()) {
				if (sessionId == null || sessionId.isBlank()) {
					continue;
				}
				if (sessionId.equals(sessionIdToKeep)) {
					continue;
				}
				if (sessionsToSkip.contains(sessionId)) {
					continue;
				}
				try {
					if (provider.terminateSession(sessionId)) {
						removed++;
					}
				} catch (Exception ignore) {
					// ignore
				}
			}
			return removed;
		} catch (Exception e) {
			debug("terminateAllSessions failed: " + e.getMessage());
			return 0;
		}
	}

	public int purgeAllSessions(String sessionIdToKeep) {
		try {
			if (provider == null) {
				return 0;
			}
			if (storeMode != SessionStoreMode.redis) {
				return terminateAllSessions(sessionIdToKeep);
			}
			return provider.purgeAllSessions(sessionIdToKeep);
		} catch (Exception e) {
			debug("purgeAllSessions failed: " + e.getMessage());
			return 0;
		}
	}

	private static String extractSessionId(String contextId) {
		if (contextId == null) {
			return null;
		}
		var idx = contextId.indexOf('_');
		if (idx <= 0) {
			return null;
		}
		return contextId.substring(0, idx);
	}

	public void flushBuffers() {
		try {
			if (provider != null) {
				provider.flushBuffers();
			}
		} catch (Exception e) {
			debug("flushBuffers failed: " + e.getMessage());
		}
	}

	public int estimateCountedSessions() {
		try {
			return provider != null ? provider.estimateCountedSessions() : 0;
		} catch (Exception e) {
			debug("estimateCountedSessions failed: " + e.getMessage());
			return 0;
		}
	}

	public int countCountedSessions() {
		try {
			return provider != null ? provider.countCountedSessions() : 0;
		} catch (Exception e) {
			debug("countCountedSessions failed: " + e.getMessage());
			return 0;
		}
	}

	public Set<String> readCountedSessionIds() {
		try {
			return provider != null ? provider.readCountedSessionIds() : Set.of();
		} catch (Exception e) {
			debug("readCountedSessionIds failed: " + e.getMessage());
			return Set.of();
		}
	}

	public boolean upsertCountedSessionBillingSnapshot(HttpSession session) {
		try {
			if (provider != null) {
				return provider.upsertCountedSessionBillingSnapshot(session);
			}
		} catch (Exception e) {
			debug("upsertCountedSessionBillingSnapshot failed: " + e.getMessage());
		}
		return false;
	}

	public void syncCountedSessionBillingAuthenticatedUser(HttpSession session) {
		try {
			if (provider != null) {
				provider.syncCountedSessionBillingAuthenticatedUser(session);
			}
		} catch (Exception e) {
			debug("syncCountedSessionBillingAuthenticatedUser failed: " + e.getMessage());
		}
	}

	private SessionStoreMode computeStoreMode() {
		var raw = EnginePropertiesManager.getProperty(PropertyName.SESSION_STORE_MODE);
		var result = SessionStoreMode.fromProperty(raw);
		debug("Resolved SESSION_STORE_MODE='" + raw + "' => store=" + result.name());
		return result;
	}

	private SessionProvider buildProvider(SessionStoreMode storeMode) {
		try {
			debug("Building session provider for store=" + storeMode.name());
			return switch (storeMode) {
				case tomcat -> new LegacySessionProvider();
				case redis -> new RedisSessionProvider();
				default -> new LegacySessionProvider();
			};
		} catch (Exception e) {
			logStartupFailure(storeMode.name(), e);
			return new LegacySessionProvider();
		}
	}

	private void reload(SessionStoreMode storeMode) {
		synchronized (mutex) {
			debug("Reload requested with store=" + storeMode.name());
			this.storeMode = storeMode;
			provider = buildProvider(storeMode);
			logSelectedMode();
			try {
				if (storeMode == SessionStoreMode.redis) {
					RedisInstanceDiscovery.start();
				} else {
					RedisInstanceDiscovery.stop();
				}
			} catch (Exception ignore) {
				// ignore discovery failures
			}
		}
	}

	private void logSelectedMode() {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isInfoEnabled()) {
				Engine.logEngine.info("(ConvertigoSessionManager) store=" + storeMode.name() + ", provider="
						+ provider.getClass().getSimpleName());
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	private void logStartupFailure(String mode, Exception e) {
		try {
			if (Engine.logEngine != null) {
				Engine.logEngine.error("(ConvertigoSessionManager) Failed to initialize session store '" + mode
						+ "'. Falling back to legacy mode.", e);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	@Override
	public void onEvent(PropertyChangeEvent event) {
		if (event == null || event.getKey() == null) {
			return;
		}
		switch (event.getKey()) {
		case SESSION_STORE_MODE:
			debug("Detected property change for " + event.getKey().name() + ", recomputing session mode");
			var targetMode = computeStoreMode();
			if (targetMode == SessionStoreMode.redis) {
				RedisClients.reload();
			}
			reload(targetMode);
			break;
		case SESSION_REDIS_HOST:
		case SESSION_REDIS_PORT:
		case SESSION_REDIS_USERNAME:
		case SESSION_REDIS_PASSWORD:
		case SESSION_REDIS_DATABASE:
		case SESSION_REDIS_SSL:
		case SESSION_REDIS_TIMEOUT:
		case SESSION_REDIS_CONNECTION_POOL_SIZE:
		case SESSION_REDIS_CONNECTION_MINIMUM_IDLE_SIZE:
		case SESSION_REDIS_PREFIX:
		case SESSION_REDIS_DEFAULT_TTL:
		case SESSION_COOKIE_NAME:
			if (storeMode == SessionStoreMode.redis) {
				debug("Detected Redis session property change for " + event.getKey().name() + ", reloading Redis session provider");
				RedisClients.reload();
				reload(SessionStoreMode.redis);
			}
			break;
		default:
			break;
		}
	}

	private void debug(String message) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(ConvertigoSessionManager) " + message);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	@Override
	public void init() {
		reload(computeStoreMode());
		try {
			if (Engine.theApp != null && Engine.theApp.eventManager != null) {
				Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
			}
		} catch (Exception ignored) {
			// Engine not fully started yet
		}
	}

	@Override
	public void destroy() {
		try {
			if (Engine.theApp != null && Engine.theApp.eventManager != null) {
				Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
			}
		} catch (Exception ignored) {
			// ignore
		}
		try {
			RedisInstanceDiscovery.stop();
		} catch (Exception ignore) {
			// ignore
		}
		synchronized (mutex) {
			provider = null;
		}
	}

	public static ConvertigoHttpSessionManager getInstance() {
		if (instance == null) {
			synchronized (ConvertigoHttpSessionManager.class) {
				if (instance == null) {
					instance = new ConvertigoHttpSessionManager();
				}
			}
		}
		return instance;
	}
}
