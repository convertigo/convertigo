/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.redisson.api.RMapCache;
import org.redisson.client.codec.StringCodec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * Lightweight Redis-based instance discovery for clustered admin usage.
 * <p>
 * Each instance periodically updates its entry with a TTL so stale instances
 * disappear automatically on crash.
 */
public final class RedisInstanceDiscovery {
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Object MUTEX = new Object();

	private static final String INDEX_INSTANCES = "index:instances";
	private static final int DEFAULT_PORT = 28080;
	private static final int HEARTBEAT_SECONDS = 10;
	private static final int TTL_SECONDS = 30;

	private static volatile boolean started;
	private static volatile String localInstanceId;
	private static volatile String localBaseUrl;
	private static volatile long startedAt;
	private static volatile ScheduledExecutorService scheduler;

	private RedisInstanceDiscovery() {
	}

	public static final class InstanceInfo {
		public final String instanceId;
		public final String baseUrl;
		public final long startedAt;
		public final long lastHeartbeat;

		private InstanceInfo(String instanceId, String baseUrl, long startedAt, long lastHeartbeat) {
			this.instanceId = instanceId;
			this.baseUrl = baseUrl;
			this.startedAt = startedAt;
			this.lastHeartbeat = lastHeartbeat;
		}
	}

	public static String getLocalInstanceId() {
		ensureLocalResolved();
		return localInstanceId;
	}

	public static String getLocalBaseUrl() {
		ensureLocalResolved();
		return localBaseUrl;
	}

	public static void start() {
		if (!ConvertigoHttpSessionManager.isRedisMode()) {
			return;
		}
		synchronized (MUTEX) {
			if (started) {
				return;
			}
			ensureLocalResolved();
			started = true;
			startedAt = System.currentTimeMillis();
			scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
				var t = new Thread(r, "convertigo-redis-instance-heartbeat");
				t.setDaemon(true);
				return t;
			});
			try {
				scheduler.scheduleAtFixedRate(RedisInstanceDiscovery::heartbeat, 0L, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
			} catch (Exception e) {
				safeLogWarn("(RedisInstanceDiscovery) Failed to schedule heartbeat", e);
			}
		}
	}

	public static void updateFromRequest(HttpServletRequest request) {
		if (!ConvertigoHttpSessionManager.isRedisMode() || request == null) {
			return;
		}
		try {
			ensureLocalResolved();

			var contextPath = request.getContextPath();
			var port = request.getLocalPort();
			if (port <= 0) {
				port = request.getServerPort();
			}
			var scheme = trimToNull(request.getScheme());
			if (scheme == null) {
				scheme = "http";
			}
			var hostName = resolveHostName();
			if (hostName == null || port <= 0) {
				return;
			}

			var addrForUrl = trimToNull(request.getLocalAddr());
			if (isLoopbackOrUnspecified(addrForUrl)) {
				addrForUrl = null;
			}
			if (addrForUrl == null) {
				try {
					var uri = URI.create(localBaseUrl);
					addrForUrl = trimToNull(uri.getHost());
				} catch (Exception ignore) {
					// ignore
				}
			}
			if (addrForUrl == null) {
				addrForUrl = hostName;
			}

			var instanceId = computeInstanceId(hostName, port);
			var baseUrl = normalizeUrl(
					scheme + "://" + hostForUrl(addrForUrl) + ":" + port + (contextPath == null ? "" : contextPath));

			var changed = false;
			synchronized (MUTEX) {
				if (startedAt <= 0) {
					startedAt = System.currentTimeMillis();
				}
				if (!instanceId.equals(localInstanceId)) {
					localInstanceId = instanceId;
					changed = true;
				}
				if (baseUrl != null && !baseUrl.equals(localBaseUrl)) {
					localBaseUrl = baseUrl;
					changed = true;
				}
			}
			if (changed) {
				heartbeat();
			}
		} catch (Exception ignore) {
			// ignore
		}
	}

	public static void stop() {
		synchronized (MUTEX) {
			started = false;
			try {
				if (scheduler != null) {
					scheduler.shutdownNow();
				}
			} catch (Exception ignore) {
			} finally {
				scheduler = null;
			}
			try {
				removeLocal();
			} catch (Exception ignore) {
			}
		}
	}

	public static List<InstanceInfo> listInstances() {
		try {
			var map = instancesMap();
			if (map == null) {
				return Collections.emptyList();
			}
			var snapshot = map.readAllMap();
			if (snapshot == null || snapshot.isEmpty()) {
				return Collections.emptyList();
			}
			var list = new ArrayList<InstanceInfo>(snapshot.size());
			for (var entry : snapshot.entrySet()) {
				var info = parseInfo(entry.getKey(), entry.getValue());
				if (info != null && !isHiddenInstance(info)) {
					list.add(info);
				}
			}
			list.sort(Comparator.comparing(a -> a.instanceId));
			return list;
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private static boolean isHiddenInstance(InstanceInfo info) {
		try {
			var id = trimToNull(info.instanceId);
			if (id == null) {
				return false;
			}
			var lower = id.toLowerCase();
			if (lower.startsWith("localhost") || lower.startsWith("127.")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public static String resolveBaseUrl(String instanceId) {
		if (instanceId == null || instanceId.isBlank()) {
			return null;
		}
		try {
			var map = instancesMap();
			if (map == null) {
				return null;
			}
			var raw = map.get(instanceId);
			if (raw == null || raw.isBlank()) {
				return null;
			}
			var info = parseInfo(instanceId, raw);
			return info != null ? info.baseUrl : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static void ensureLocalResolved() {
		if (localInstanceId != null && localBaseUrl != null) {
			return;
		}
		synchronized (MUTEX) {
			if (localInstanceId != null && localBaseUrl != null) {
				return;
			}

			var configuredUrl = normalizeUrl(EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL));
			var defaultUrl = normalizeUrl(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL.getDefaultValue());
			var hasExplicitUrl = configuredUrl != null && !configuredUrl.equals(defaultUrl);

			if (hasExplicitUrl) {
				localBaseUrl = configuredUrl;
				localInstanceId = computeInstanceIdFromBaseUrl(localBaseUrl);
				return;
			}

			var hostName = resolveHostName();

			var localAddr = trimToNull(System.getenv("POD_IP"));
			if (localAddr == null) {
				try {
					localAddr = trimToNull(InetAddress.getLocalHost().getHostAddress());
				} catch (Exception ignore) {
					// ignore
				}
			}
			if (localAddr == null) {
				localAddr = hostName;
			}

			var scheme = "http";
			var contextPath = "/convertigo";
			Integer port = null;

			if (configuredUrl != null) {
				try {
					var uri = URI.create(configuredUrl);
					if (uri.getScheme() != null) {
						scheme = uri.getScheme();
					}
					if (uri.getPath() != null && !uri.getPath().isBlank()) {
						contextPath = uri.getPath();
					}
					if (hasExplicitUrl && uri.getPort() > 0) {
						port = uri.getPort();
					}
				} catch (Exception ignore) {
					// ignore
				}
			}

			if (port == null) {
				port = firstIntEnv("C8O_PORT", "CONVERTIGO_PORT", "SERVER_PORT", "PORT");
			}
			if (port == null || port <= 0) {
				port = resolveTomcatHttpPort();
			}
			if (port == null || port <= 0) {
				port = DEFAULT_PORT;
			}

			var resolvedHost = hostName != null ? hostName : "instance";
			localInstanceId = computeInstanceId(resolvedHost, port);
			localBaseUrl = normalizeUrl(scheme + "://" + hostForUrl(localAddr) + ":" + port + contextPath);
		}
	}

	private static Integer resolveTomcatHttpPort() {
		var base = trimToNull(System.getProperty("catalina.base"));
		if (base == null) {
			base = trimToNull(System.getProperty("catalina.home"));
		}
		if (base == null) {
			return null;
		}

		try {
			var serverXml = Path.of(base, "conf", "server.xml");
			if (!Files.isRegularFile(serverXml)) {
				return null;
			}

			var xml = Files.readString(serverXml, StandardCharsets.UTF_8);
			var matcher = Pattern.compile("<Connector\\b[^>]*\\bport\\s*=\\s*\"(\\d+)\"[^>]*>", Pattern.CASE_INSENSITIVE)
					.matcher(xml);
			while (matcher.find()) {
				var tag = matcher.group(0).toLowerCase();
				if (tag.contains("protocol=\"ajp") || tag.contains("protocol='ajp") || tag.contains("ajp/")) {
					continue;
				}
				try {
					var port = Integer.parseInt(matcher.group(1));
					if (port > 0) {
						return port;
					}
				} catch (Exception ignore) {
					// ignore
				}
			}
		} catch (Exception ignore) {
			// ignore
		}
		return null;
	}

	private static String resolveHostName() {
		var env = sanitizeInstanceId(trimToNull(System.getenv("HOSTNAME")));
		String sys = null;
		try {
			sys = sanitizeInstanceId(trimToNull(InetAddress.getLocalHost().getHostName()));
		} catch (Exception ignore) {
			// ignore
		}

		if (env == null) {
			return sys;
		}
		if ("localhost".equalsIgnoreCase(env)) {
			return sys != null && !"localhost".equalsIgnoreCase(sys) ? sys : env;
		}
		if (sys != null && sys.contains(".") && !env.contains(".") && sys.startsWith(env)) {
			return sys;
		}
		return env;
	}

	private static String computeInstanceId(String host, int port) {
		var safeHost = sanitizeInstanceId(trimToNull(host));
		if (safeHost == null) {
			safeHost = "instance";
		}
		return port == DEFAULT_PORT ? safeHost : safeHost + ":" + port;
	}

	private static boolean isLoopbackOrUnspecified(String host) {
		host = trimToNull(host);
		if (host == null) {
			return false;
		}
		if (host.startsWith("[") && host.endsWith("]")) {
			host = host.substring(1, host.length() - 1);
		}
		host = host.toLowerCase();
		if ("localhost".equals(host)) {
			return true;
		}
		if ("0.0.0.0".equals(host) || "::".equals(host) || "0:0:0:0:0:0:0:0".equals(host)) {
			return true;
		}
		if ("::1".equals(host) || "0:0:0:0:0:0:0:1".equals(host)) {
			return true;
		}
		return "127.0.0.1".equals(host) || host.startsWith("127.");
	}

	private static String computeInstanceIdFromBaseUrl(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			return null;
		}
		try {
			var uri = URI.create(baseUrl);
			var host = uri.getHost();
			var port = uri.getPort();
			if (host == null || host.isBlank()) {
				return sanitizeInstanceId(baseUrl);
			}
			if (port <= 0 || port == DEFAULT_PORT) {
				return host;
			}
			return host + ":" + port;
		} catch (Exception e) {
			return sanitizeInstanceId(baseUrl);
		}
	}

	private static String sanitizeInstanceId(String raw) {
		if (raw == null) {
			return null;
		}
		var s = raw.trim();
		if (s.isEmpty()) {
			return null;
		}
		s = s.replace("://", "_");
		s = s.replace('/', '_');
		s = s.replace('?', '_');
		return s;
	}

	private static String normalizeUrl(String url) {
		if (url == null) {
			return null;
		}
		url = url.trim();
		if (url.isEmpty()) {
			return null;
		}
		while (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	private static String hostForUrl(String host) {
		host = trimToNull(host);
		if (host == null) {
			return null;
		}
		if (host.startsWith("[") && host.endsWith("]")) {
			return host;
		}
		return host.indexOf(':') >= 0 ? "[" + host + "]" : host;
	}

	private static Integer firstIntEnv(String... names) {
		if (names == null) {
			return null;
		}
		for (String name : names) {
			if (name == null || name.isBlank()) {
				continue;
			}
			var raw = trimToNull(System.getenv(name));
			if (raw == null) {
				continue;
			}
			try {
				return Integer.parseInt(raw);
			} catch (Exception ignore) {
				// ignore
			}
		}
		return null;
	}

	private static RMapCache<String, String> instancesMap() {
		if (!ConvertigoHttpSessionManager.isRedisMode()) {
			return null;
		}
		try {
			var cfg = RedisClients.getConfiguration();
			var key = cfg.getContextKeyPrefix() + INDEX_INSTANCES;
			return RedisClients.getClient().getMapCache(key, StringCodec.INSTANCE);
		} catch (Exception e) {
			return null;
		}
	}

	private static void heartbeat() {
		try {
			if (!ConvertigoHttpSessionManager.isRedisMode()) {
				return;
			}
			ensureLocalResolved();
			var map = instancesMap();
			if (map == null) {
				return;
			}
			String id = localInstanceId;
			String url = localBaseUrl;
			if (id == null || id.isBlank() || url == null || url.isBlank()) {
				return;
			}

			var now = System.currentTimeMillis();
			var payload = new java.util.HashMap<String, Object>(8);
			payload.put("instanceId", id);
			payload.put("baseUrl", url);
			payload.put("startedAt", startedAt > 0 ? startedAt : now);
			payload.put("lastHeartbeat", now);

			String json = JSON.writeValueAsString(payload);
			map.fastPut(id, json, TTL_SECONDS, TimeUnit.SECONDS);
		} catch (Exception e) {
			safeLogDebug("(RedisInstanceDiscovery) heartbeat failed", e);
		}
	}

	private static void removeLocal() {
		if (!ConvertigoHttpSessionManager.isRedisMode()) {
			return;
		}
		try {
			ensureLocalResolved();
			var map = instancesMap();
			if (map != null && localInstanceId != null) {
				map.fastRemove(localInstanceId);
			}
		} catch (Exception ignore) {
			// ignore
		}
	}

	private static InstanceInfo parseInfo(String instanceId, String rawJson) {
		if (instanceId == null || instanceId.isBlank() || rawJson == null || rawJson.isBlank()) {
			return null;
		}
		try {
			var root = JSON.readTree(rawJson);
			if (root == null || root.isNull()) {
				return null;
			}
			var baseUrl = root.path("baseUrl").asText(null);
			var startedAt = root.path("startedAt").asLong(0L);
			var lastHeartbeat = root.path("lastHeartbeat").asLong(0L);
			if (baseUrl != null) {
				baseUrl = normalizeUrl(baseUrl);
			}
			return new InstanceInfo(instanceId, baseUrl, startedAt, lastHeartbeat);
		} catch (Exception ignore) {
			return new InstanceInfo(instanceId, null, 0L, 0L);
		}
	}

	private static String trimToNull(String s) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

	private static void safeLogWarn(String message, Exception e) {
		try {
			if (Engine.logEngine != null) {
				Engine.logEngine.warn(message, e);
			}
		} catch (Exception ignore) {
		}
	}

	private static void safeLogDebug(String message, Exception e) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug(message, e);
			}
		} catch (Exception ignore) {
		}
	}
}
