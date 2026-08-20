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

package com.twinsoft.convertigo.engine;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;

import com.twinsoft.convertigo.engine.sessions.RedisInstanceDiscovery;

public class ReverseProxyManager {
	private static final Pattern managedRouteKey = Pattern.compile(
			"^[A-Za-z0-9_-]{1,256}\\.[A-Za-z0-9_-]{43}$");
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final Base64.Encoder base64Url = Base64.getUrlEncoder().withoutPadding();

	private final Map<String, HttpHost> reverseProxyHttp = new ConcurrentHashMap<>();
	
	public ReverseProxyManager() {
//		try {
//			addReverseProxyHttp("hello", "http://localhost:5173");
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
	}
	
	public void addReverseProxyHttp(String prefix, String target) throws URISyntaxException {
		var uri = new URI(target);
		if (prefix == null || prefix.isBlank() || uri.getHost() == null || uri.getScheme() == null) {
			throw new URISyntaxException(String.valueOf(target), "A reverse proxy key, scheme and host are required");
		}
		var httpHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
		reverseProxyHttp.put(prefix, httpHost);
	}

	/**
	 * Registers a short-lived capability that can only target a loopback HTTP port.
	 * The instance prefix lets another cluster member forward the capability to its owner.
	 */
	public String registerLoopbackHttp(int port) {
		validatePort(port);
		var key = createManagedRouteKey(RedisInstanceDiscovery.getLocalInstanceId());
		reverseProxyHttp.put(key, new HttpHost("127.0.0.1", port, "http"));
		return key;
	}

	public static String createManagedRouteKey(String instance) {
		if (instance == null || instance.isBlank()) {
			throw new IllegalArgumentException("A route owner instance is required");
		}
		var instancePart = base64Url.encodeToString(instance.getBytes(StandardCharsets.UTF_8));
		if (instancePart.length() > 256) {
			throw new IllegalArgumentException("The route owner instance identifier is too long");
		}
		var random = new byte[32];
		secureRandom.nextBytes(random);
		return instancePart + "." + base64Url.encodeToString(random);
	}

	/** Restores a still-running local dev server after a Flow runtime reload. */
	public boolean restoreLoopbackHttp(String key, int port) {
		validatePort(port);
		var instance = getRouteInstanceId(key);
		if (instance == null || !instance.equals(RedisInstanceDiscovery.getLocalInstanceId())) {
			return false;
		}
		reverseProxyHttp.put(key, new HttpHost("127.0.0.1", port, "http"));
		return true;
	}

	public boolean removeReverseProxyHttp(String key) {
		return key != null && reverseProxyHttp.remove(key) != null;
	}

	public HttpHost getHttpHost(String key) {
		return reverseProxyHttp.get(key);
	}

	public static boolean isManagedRouteKey(String key) {
		return key != null && managedRouteKey.matcher(key).matches();
	}

	public static String getRouteInstanceId(String key) {
		if (!isManagedRouteKey(key)) {
			return null;
		}
		try {
			var separator = key.indexOf('.');
			return new String(Base64.getUrlDecoder().decode(key.substring(0, separator)), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}

	private static void validatePort(int port) {
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("Loopback reverse proxy port must be between 1 and 65535");
		}
	}
}
