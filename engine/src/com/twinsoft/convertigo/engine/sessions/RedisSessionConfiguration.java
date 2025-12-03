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

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

final class RedisSessionConfiguration {
	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final int database;
	private final boolean ssl;
	private final int timeoutMillis;
	private final String keyPrefix;
	private final int defaultTtlSeconds;
	private final String cookieName;
	private final boolean hashExperimental;

	private RedisSessionConfiguration(String host, int port, String username, String password, int database,
			boolean ssl, int timeoutMillis, String keyPrefix, int defaultTtlSeconds, boolean hashExperimental) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
		this.ssl = ssl;
		this.timeoutMillis = timeoutMillis;
		this.keyPrefix = keyPrefix.endsWith(":") ? keyPrefix : keyPrefix + ':';
		this.defaultTtlSeconds = defaultTtlSeconds;
		this.cookieName = EnginePropertiesManager.getProperty(PropertyName.SESSION_COOKIE_NAME);
		this.hashExperimental = hashExperimental;
	}

	static RedisSessionConfiguration fromProperties() {
		var host = EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_HOST);
		var port = parseInt(PropertyName.SESSION_REDIS_PORT, 6379);
		var username = EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_USERNAME);
		var password = EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_PASSWORD);
		var database = parseInt(PropertyName.SESSION_REDIS_DATABASE, 0);
		var ssl = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.SESSION_REDIS_SSL);
		var timeoutMillis = parseInt(PropertyName.SESSION_REDIS_TIMEOUT, 5000);
		var prefix = EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_PREFIX);
		var ttl = parseInt(PropertyName.SESSION_REDIS_DEFAULT_TTL, 1800);
		var hashExperimental = Boolean.parseBoolean(
				System.getProperty("convertigo.engine.session.redis.hash.experimental", "false"));
		return new RedisSessionConfiguration(host, port, username, password, database, ssl, timeoutMillis, prefix, ttl,
				hashExperimental);
	}

	private static int parseInt(PropertyName property, int defaultValue) {
		try {
			return Integer.parseInt(EnginePropertiesManager.getProperty(property));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	String getAddress() {
		var protocol = ssl ? "rediss" : "redis";
		return protocol + "://" + host + ':' + port;
	}

	String getUsername() {
		return username == null || username.isBlank() ? null : username.trim();
	}

	String getPassword() {
		return password == null || password.isBlank() ? null : password;
	}

	int getDatabase() {
		return database;
	}

	int getTimeoutMillis() {
		return timeoutMillis;
	}

	String key(String sessionId) {
		return keyPrefix + sessionId;
	}

	int getDefaultTtlSeconds() {
		return defaultTtlSeconds;
	}

	String getCookieName() {
		return cookieName;
	}

	boolean isHashExperimental() {
		return hashExperimental;
	}
}
