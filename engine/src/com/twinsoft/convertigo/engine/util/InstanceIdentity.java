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

package com.twinsoft.convertigo.engine.util;

import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class InstanceIdentity {
	private static final int DEFAULT_PORT = 28080;

	private static volatile String localInstanceId;

	private InstanceIdentity() {
	}

	public static String getLocalInstanceId() {
		if (localInstanceId != null) {
			return localInstanceId;
		}
		synchronized (InstanceIdentity.class) {
			if (localInstanceId == null) {
				localInstanceId = resolveLocalInstanceId();
			}
			return localInstanceId;
		}
	}

	private static String resolveLocalInstanceId() {
		var configuredUrl = normalizeUrl(EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL));
		var defaultUrl = normalizeUrl(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL.getDefaultValue());
		var hasExplicitUrl = configuredUrl != null && !configuredUrl.equals(defaultUrl);

		if (hasExplicitUrl) {
			return computeInstanceIdFromBaseUrl(configuredUrl);
		}

		var hostName = resolveHostName();
		Integer port = null;

		if (configuredUrl != null) {
			try {
				var uri = URI.create(configuredUrl);
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

		return computeInstanceId(hostName != null ? hostName : "instance", port);
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

	private static Integer firstIntEnv(String... names) {
		if (names == null) {
			return null;
		}
		for (var name : names) {
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

	private static String trimToNull(String s) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		return s.isEmpty() ? null : s;
	}
}
