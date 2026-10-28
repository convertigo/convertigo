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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
	/** Effective store: tomcat while Redis is not operational yet or after a tomcat startup fallback. */
	private volatile SessionStoreMode storeMode;
	/** Store requested by session.store.mode. */
	private volatile SessionStoreMode configuredMode;
	private volatile StoreState storeState = StoreState.OK;
	private volatile String storeModeProperty;
	private ScheduledExecutorService redisRetry;
	private int redisRetryAttempts;
	private long redisWaitingSince;

	/** State of the configured session store, as reported by the supervision. */
	public enum StoreState {
		/** The configured store is operational. */
		OK,
		/** Redis was unreachable at startup: retrying, sessions refused meanwhile (session.redis.startup.fallback=none). */
		WAITING,
		/** Redis was unreachable at startup: local sessions until restart (session.redis.startup.fallback=tomcat). */
		DEGRADED
	}

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

	public SessionStoreMode getConfiguredStoreMode() {
		return configuredMode;
	}

	public StoreState getStoreState() {
		return storeState;
	}

	/**
	 * Value of convertigo.session.store in the supervision: OK, KO while Redis is not operational (at startup, or
	 * unreachable for longer than session.redis.readiness.grace once connected), or DEGRADED after a tomcat
	 * startup fallback.
	 */
	public String getSupervisionStatus() {
		if (configuredMode != SessionStoreMode.redis) {
			return "OK";
		}
		if (storeState == StoreState.WAITING) {
			return "KO";
		}
		if (storeState == StoreState.DEGRADED) {
			return "DEGRADED";
		}
		long since = RedisHealth.unreachableSince();
		if (since > 0) {
			long graceSeconds = 60;
			try {
				graceSeconds = Long.parseLong(EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_READINESS_GRACE).trim());
			} catch (Exception e) {
				// keep the default
			}
			if (System.currentTimeMillis() - since > graceSeconds * 1000L) {
				return "KO";
			}
		}
		return "OK";
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
			Engine.logRedis.debug("(ConvertigoSessionManager) tryTerminateSession failed: " + e.getMessage());
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

			var sessionsToSkip = new java.util.HashSet<String>();
			try {
				if (Engine.theApp != null && Engine.theApp.contextManager != null) {
					var allContextIds = new java.util.HashSet<String>(new RedisContextStore(cfg).readLiveContextIds());
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
			Engine.logRedis.debug("(ConvertigoSessionManager) terminateAllSessions failed: " + e.getMessage());
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
			Engine.logRedis.debug("(ConvertigoSessionManager) purgeAllSessions failed: " + e.getMessage());
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
			Engine.logRedis.debug("(ConvertigoSessionManager) flushBuffers failed: " + e.getMessage());
		}
	}

	/**
	 * Writes now, in the session store, what the session of this request changed so far: called before the context lock
	 * of the request is released and before its response is sent, so that the next requests find it. The end of the
	 * request ({@link #flushBuffers()}) writes what changes after. No-op without a buffered session.
	 */
	public void flushRequestSession(HttpServletRequest request) {
		try {
			var current = provider;
			if (current != null) {
				current.flushRequestSession(request);
			}
		} catch (Exception e) {
			Engine.logRedis.debug("(ConvertigoSessionManager) flushRequestSession failed: " + e.getMessage());
		}
	}

	public int estimateCountedSessions() {
		try {
			return provider != null ? provider.estimateCountedSessions() : 0;
		} catch (Exception e) {
			Engine.logRedis.debug("(ConvertigoSessionManager) estimateCountedSessions failed: " + e.getMessage());
			return 0;
		}
	}

	private static final long COUNTED_SESSIONS_CACHE_MILLIS = 10_000L;
	private volatile int cachedCountedSessions = 0;
	private volatile long cachedCountedSessionsTime = 0L;
	private final AtomicBoolean cachedCountedSessionsRefreshing = new AtomicBoolean();

	/**
	 * Last known number of counted sessions in the session store, refreshed in background at most every
	 * 10 seconds. It never waits for the store, so stateless requests keep working when the store is slow
	 * or unreachable: the previous value is kept when a refresh fails.
	 */
	public int cachedCountedSessions() {
		if (System.currentTimeMillis() - cachedCountedSessionsTime > COUNTED_SESSIONS_CACHE_MILLIS
				&& cachedCountedSessionsRefreshing.compareAndSet(false, true)) {
			Engine.execute(() -> {
				try {
					if (provider != null) {
						cachedCountedSessions = provider.countCountedSessions();
					}
				} catch (Exception e) {
					Engine.logRedis.debug("(ConvertigoSessionManager) cachedCountedSessions refresh failed: " + e.getMessage());
				} finally {
					cachedCountedSessionsTime = System.currentTimeMillis();
					cachedCountedSessionsRefreshing.set(false);
				}
			});
		}
		return cachedCountedSessions;
	}

	public int countCountedSessions() {
		try {
			return provider != null ? provider.countCountedSessions() : 0;
		} catch (Exception e) {
			Engine.logRedis.debug("(ConvertigoSessionManager) countCountedSessions failed: " + e.getMessage());
			return 0;
		}
	}

	public Set<String> readCountedSessionIds() {
		try {
			return provider != null ? provider.readCountedSessionIds() : Set.of();
		} catch (Exception e) {
			Engine.logRedis.debug("(ConvertigoSessionManager) readCountedSessionIds failed: " + e.getMessage());
			return Set.of();
		}
	}

	public boolean upsertCountedSessionBillingSnapshot(HttpSession session) {
		try {
			if (provider != null) {
				return provider.upsertCountedSessionBillingSnapshot(session);
			}
		} catch (Exception e) {
			Engine.logRedis.debug("(ConvertigoSessionManager) upsertCountedSessionBillingSnapshot failed: "
					+ e.getMessage());
		}
		return false;
	}

	public void syncCountedSessionBillingAuthenticatedUser(HttpSession session) {
		try {
			if (provider != null) {
				provider.syncCountedSessionBillingAuthenticatedUser(session);
			}
		} catch (Exception e) {
			Engine.logRedis.debug("(ConvertigoSessionManager) syncCountedSessionBillingAuthenticatedUser failed: "
					+ e.getMessage());
		}
	}

	private SessionStoreMode computeStoreMode() {
		var raw = EnginePropertiesManager.getProperty(PropertyName.SESSION_STORE_MODE);
		var result = SessionStoreMode.fromProperty(raw);
		storeModeProperty = raw;
		Engine.logRedis.debug("(ConvertigoSessionManager) Resolved SESSION_STORE_MODE='" + raw + "' => store="
				+ result.name());
		return result;
	}

	private void reload(SessionStoreMode mode) {
		synchronized (mutex) {
			Engine.logRedis.debug("(ConvertigoSessionManager) Reload requested with store=" + mode.name());
			cancelRedisRetry();
			configuredMode = mode;
			if (mode == SessionStoreMode.redis) {
				try {
					Engine.logEngine.info("(ConvertigoSessionManager) Redis session store: "
							+ RedisSessionConfiguration.fromProperties().describe());
					provider = new RedisSessionProvider();
					storeMode = SessionStoreMode.redis;
					storeState = StoreState.OK;
				} catch (Exception e) {
					onRedisStartupFailure(e);
				}
			} else {
				provider = new LegacySessionProvider();
				storeMode = SessionStoreMode.tomcat;
				storeState = StoreState.OK;
			}
			logSelectedMode();
			updateInstanceDiscovery();
		}
		notifyContextManager();
	}

	private void onRedisStartupFailure(Exception e) {
		RedisHealth.failure(e);
		var fallback = RedisStartupFallback.fromProperty(EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_STARTUP_FALLBACK));
		storeMode = SessionStoreMode.tomcat;
		if (fallback == RedisStartupFallback.tomcat) {
			Engine.logEngine.error("(ConvertigoSessionManager) Failed to initialize session store 'redis'. Falling back to legacy mode.", e);
			Engine.logEngine.warn("(ConvertigoSessionManager) Redis is unreachable at startup (" + RedisHealth.describe(e)
					+ ") and session.redis.startup.fallback=tomcat: the HTTP sessions of this instance stay in its memory"
					+ " and are not shared with the other instances until it is restarted.");
			provider = new LegacySessionProvider();
			storeState = StoreState.DEGRADED;
		} else {
			Engine.logEngine.error("(ConvertigoSessionManager) Failed to initialize session store 'redis'. Retrying until Redis is reachable.", e);
			Engine.logEngine.warn("(ConvertigoSessionManager) Redis is unreachable at startup (" + RedisHealth.describe(e)
					+ "): until it is reachable, this instance reports convertigo.session.store=KO to the supervision and"
					+ " answers 503 to the requests that need an HTTP session.");
			provider = new UnavailableSessionProvider();
			storeState = StoreState.WAITING;
			redisRetryAttempts = 0;
			redisWaitingSince = System.currentTimeMillis();
			scheduleRedisRetry();
		}
	}

	private void scheduleRedisRetry() {
		if (redisRetry == null) {
			redisRetry = Executors.newSingleThreadScheduledExecutor(r -> {
				var thread = new Thread(r, "convertigo-redis-session-store-retry");
				thread.setDaemon(true);
				return thread;
			});
		}
		// 1 s, 2 s, 4 s ... up to 30 s between the end of an attempt and the next one
		long delay = Math.min(30_000L, 1000L << Math.min(redisRetryAttempts, 5));
		redisRetry.schedule(this::retryRedis, delay, TimeUnit.MILLISECONDS);
	}

	private void cancelRedisRetry() {
		if (redisRetry != null) {
			redisRetry.shutdownNow();
			redisRetry = null;
		}
	}

	private void retryRedis() {
		SessionProvider redisProvider;
		try {
			redisProvider = new RedisSessionProvider();
		} catch (Exception e) {
			RedisHealth.failure(e);
			Engine.logRedis.debug("(ConvertigoSessionManager) Redis still unreachable: " + RedisHealth.describe(e));
			synchronized (mutex) {
				if (storeState == StoreState.WAITING && configuredMode == SessionStoreMode.redis) {
					redisRetryAttempts++;
					scheduleRedisRetry();
				}
			}
			return;
		}
		synchronized (mutex) {
			if (storeState != StoreState.WAITING || configuredMode != SessionStoreMode.redis) {
				return;
			}
			provider = redisProvider;
			storeMode = SessionStoreMode.redis;
			storeState = StoreState.OK;
			cancelRedisRetry();
			Engine.logEngine.info("(ConvertigoSessionManager) Redis is reachable: the session store is operational after "
					+ (redisRetryAttempts + 1) + " attempt(s) and " + ((System.currentTimeMillis() - redisWaitingSince) / 1000L) + " s");
			logSelectedMode();
			updateInstanceDiscovery();
		}
		notifyContextManager();
	}

	private void updateInstanceDiscovery() {
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

	private void notifyContextManager() {
		try {
			if (Engine.theApp != null && Engine.theApp.contextManager != null) {
				Engine.theApp.contextManager.onSessionStoreChanged();
			}
		} catch (Exception e) {
			Engine.logEngine.warn("(ConvertigoSessionManager) Failed to reconfigure the context manager for the session store", e);
		}
	}

	private void logSelectedMode() {
		var mode = configuredMode != null ? configuredMode : storeMode;
		Engine.logEngine.info("(ConvertigoSessionManager) store=" + mode.name() + ", provider="
				+ provider.getClass().getSimpleName() + ", state=" + storeState.name() + " (session.store.mode=" + storeModeProperty + ")");
		if (storeMode == SessionStoreMode.redis && provider instanceof RedisSessionProvider) {
			RedisHealth.success();
		}
	}

	public String getProviderName() {
		var current = provider;
		return current != null ? current.getClass().getSimpleName() : "";
	}

	@Override
	public void onEvent(PropertyChangeEvent event) {
		if (event == null || event.getKey() == null) {
			return;
		}
		switch (event.getKey()) {
		case SESSION_STORE_MODE:
			Engine.logRedis.debug("(ConvertigoSessionManager) Detected property change for " + event.getKey().name()
					+ ", recomputing session mode");
			var targetMode = computeStoreMode();
			if (targetMode == SessionStoreMode.redis) {
				RedisClients.reload();
			}
			reload(targetMode);
			break;
		case SESSION_REDIS_HOST:
		case SESSION_REDIS_TOPOLOGY:
		case SESSION_REDIS_SENTINEL_MASTER:
		case SESSION_REDIS_SENTINEL_NODES:
		case SESSION_REDIS_SENTINEL_USERNAME:
		case SESSION_REDIS_SENTINEL_PASSWORD:
		case SESSION_REDIS_PORT:
		case SESSION_REDIS_USERNAME:
		case SESSION_REDIS_PASSWORD:
		case SESSION_REDIS_DATABASE:
		case SESSION_REDIS_SSL:
		case SESSION_REDIS_SSL_TRUSTSTORE:
		case SESSION_REDIS_SSL_TRUSTSTORE_PASSWORD:
		case SESSION_REDIS_SSL_KEYSTORE:
		case SESSION_REDIS_SSL_KEYSTORE_PASSWORD:
		case SESSION_REDIS_SSL_KEYSTORE_TYPE:
		case SESSION_REDIS_SSL_VERIFICATION_MODE:
		case SESSION_REDIS_SSL_PROTOCOLS:
		case SESSION_REDIS_TIMEOUT:
		case SESSION_REDIS_CONNECTION_POOL_SIZE:
		case SESSION_REDIS_CONNECTION_MINIMUM_IDLE_SIZE:
		case SESSION_REDIS_PREFIX:
		case SESSION_REDIS_DEFAULT_TTL:
		case SESSION_COOKIE_NAME:
			if (configuredMode == SessionStoreMode.redis) {
				Engine.logRedis.debug("(ConvertigoSessionManager) Detected Redis session property change for "
						+ event.getKey().name() + ", reloading Redis session provider");
				RedisClients.reload();
				reload(SessionStoreMode.redis);
			}
			break;
		default:
			break;
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
			cancelRedisRetry();
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
