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

import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public final class RedisSessionConfiguration {
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
    private final String contextKeyPrefix;

    private RedisSessionConfiguration(String host, int port, String username, String password, int database, boolean ssl, int timeoutMillis, String keyPrefix, int defaultTtlSeconds) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.ssl = ssl;
        this.timeoutMillis = timeoutMillis;
        var normalized = keyPrefix == null ? "" : keyPrefix.replaceAll(":+$", "");
        var ctxPrefix = this.keyPrefix = normalized.isEmpty() ? "" : normalized + ":";
        if (ctxPrefix.endsWith("session:")) {
            ctxPrefix = ctxPrefix.substring(0, ctxPrefix.length() - "session:".length());
        }
        if (!ctxPrefix.isEmpty() && !ctxPrefix.endsWith(":")) {
            ctxPrefix = ctxPrefix + ":";
        }
        this.contextKeyPrefix = ctxPrefix;
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.cookieName = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_COOKIE_NAME);
    }

    public static RedisSessionConfiguration fromProperties() {
        var host = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_HOST);
        var port = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_PORT, 6379);
        var username = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_USERNAME);
        var password = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_PASSWORD);
        var database = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_DATABASE, 0);
        var ssl = EnginePropertiesManager.getPropertyAsBoolean((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL);
        var timeoutMillis = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_TIMEOUT, 5000);
        var prefix = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_PREFIX);
        var ttl = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_DEFAULT_TTL, 1800);
        return new RedisSessionConfiguration(host, port, username, password, database, ssl, timeoutMillis, prefix, ttl);
    }

    private static int parseInt(EnginePropertiesManager.PropertyName property, int defaultValue) {
        try {
            return Integer.parseInt(EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)property));
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    String getAddress() {
        var protocol = this.ssl ? "rediss" : "redis";
        return protocol + "://" + this.host + ":" + this.port;
    }

    String getUsername() {
        return this.username == null || this.username.isBlank() ? null : this.username.trim();
    }

    String getPassword() {
        return this.password == null || this.password.isBlank() ? null : this.password;
    }

    int getDatabase() {
        return this.database;
    }

    int getTimeoutMillis() {
        return this.timeoutMillis;
    }

    String key(String sessionId) {
        return this.keyPrefix + sessionId;
    }

    public String getKeyPrefix() {
        return this.keyPrefix;
    }

    public String getContextKeyPrefix() {
        return this.contextKeyPrefix;
    }

    public String contextKey(String contextId) {
        return this.contextKeyPrefix + "context:" + contextId;
    }

    int getDefaultTtlSeconds() {
        return this.defaultTtlSeconds;
    }

    String getCookieName() {
        return this.cookieName;
    }
}
