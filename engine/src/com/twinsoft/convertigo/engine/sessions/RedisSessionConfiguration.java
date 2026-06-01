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

import java.util.Arrays;
import java.util.Objects;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public final class RedisSessionConfiguration {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final int database;
    private final boolean ssl;
    private final String sslTruststore;
    private final String sslTruststorePassword;
    private final String sslKeystore;
    private final String sslKeystorePassword;
    private final String sslKeystoreType;
    private final String sslVerificationMode;
    private final String[] sslProtocols;
    private final int timeoutMillis;
    private final int connectionPoolSize;
    private final int connectionMinimumIdleSize;
    private final String keyPrefix;
    private final int defaultTtlSeconds;
    private final String cookieName;
    private final String contextKeyPrefix;

    private RedisSessionConfiguration(String host, int port, String username, String password, int database, boolean ssl,
            String sslTruststore, String sslTruststorePassword, String sslKeystore, String sslKeystorePassword,
            String sslKeystoreType, String sslVerificationMode, String[] sslProtocols, int timeoutMillis,
            int connectionPoolSize, int connectionMinimumIdleSize, String keyPrefix, int defaultTtlSeconds) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.ssl = ssl;
        this.sslTruststore = blankToNull(sslTruststore);
        this.sslTruststorePassword = blankToNull(sslTruststorePassword);
        this.sslKeystore = blankToNull(sslKeystore);
        this.sslKeystorePassword = blankToNull(sslKeystorePassword);
        this.sslKeystoreType = blankToNull(sslKeystoreType);
        this.sslVerificationMode = blankToNull(sslVerificationMode);
        this.sslProtocols = sslProtocols == null ? new String[0] : sslProtocols.clone();
        this.timeoutMillis = timeoutMillis;
        this.connectionPoolSize = Math.max(1, connectionPoolSize);
        this.connectionMinimumIdleSize = Math.max(0, Math.min(connectionMinimumIdleSize, this.connectionPoolSize));
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
        var sslTruststore = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL_TRUSTSTORE);
        var sslTruststorePassword = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL_TRUSTSTORE_PASSWORD);
        var sslKeystore = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL_KEYSTORE);
        var sslKeystorePassword = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL_KEYSTORE_PASSWORD);
        var sslKeystoreType = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL_KEYSTORE_TYPE);
        var sslVerificationMode = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL_VERIFICATION_MODE);
        var sslProtocols = RedisSessionConfiguration.parseList(EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL_PROTOCOLS);
        var timeoutMillis = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_TIMEOUT, 5000);
        var connectionPoolSize = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_CONNECTION_POOL_SIZE, 64);
        var connectionMinimumIdleSize = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_CONNECTION_MINIMUM_IDLE_SIZE, 24);
        var prefix = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_PREFIX);
        var ttl = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_DEFAULT_TTL, 1800);
        return new RedisSessionConfiguration(host, port, username, password, database, ssl, sslTruststore,
                sslTruststorePassword, sslKeystore, sslKeystorePassword, sslKeystoreType, sslVerificationMode,
                sslProtocols, timeoutMillis, connectionPoolSize, connectionMinimumIdleSize, prefix, ttl);
    }

    private static int parseInt(EnginePropertiesManager.PropertyName property, int defaultValue) {
        try {
            return Integer.parseInt(EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)property));
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    private static String[] parseList(EnginePropertiesManager.PropertyName property) {
        var value = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)property);
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(value.split("[,\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toArray(String[]::new);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    boolean isSsl() {
        return this.ssl;
    }

    String getSslTruststore() {
        return this.sslTruststore;
    }

    String getSslTruststorePassword() {
        return this.sslTruststorePassword;
    }

    String getSslKeystore() {
        return this.sslKeystore;
    }

    String getSslKeystorePassword() {
        return this.sslKeystorePassword;
    }

    String getSslKeystoreType() {
        return this.sslKeystoreType;
    }

    String getSslVerificationMode() {
        return this.sslVerificationMode;
    }

    String[] getSslProtocols() {
        return this.sslProtocols.clone();
    }

    int getDatabase() {
        return this.database;
    }

    int getTimeoutMillis() {
        return this.timeoutMillis;
    }

    int getConnectionPoolSize() {
        return this.connectionPoolSize;
    }

    int getConnectionMinimumIdleSize() {
        return this.connectionMinimumIdleSize;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RedisSessionConfiguration other)) {
            return false;
        }
        return port == other.port
                && database == other.database
                && ssl == other.ssl
                && timeoutMillis == other.timeoutMillis
                && connectionPoolSize == other.connectionPoolSize
                && connectionMinimumIdleSize == other.connectionMinimumIdleSize
                && defaultTtlSeconds == other.defaultTtlSeconds
                && Objects.equals(host, other.host)
                && Objects.equals(username, other.username)
                && Objects.equals(password, other.password)
                && Objects.equals(sslTruststore, other.sslTruststore)
                && Objects.equals(sslTruststorePassword, other.sslTruststorePassword)
                && Objects.equals(sslKeystore, other.sslKeystore)
                && Objects.equals(sslKeystorePassword, other.sslKeystorePassword)
                && Objects.equals(sslKeystoreType, other.sslKeystoreType)
                && Objects.equals(sslVerificationMode, other.sslVerificationMode)
                && Arrays.equals(sslProtocols, other.sslProtocols)
                && Objects.equals(keyPrefix, other.keyPrefix)
                && Objects.equals(cookieName, other.cookieName)
                && Objects.equals(contextKeyPrefix, other.contextKeyPrefix);
    }

    @Override
    public int hashCode() {
        var result = Objects.hash(host, port, username, password, database, ssl, sslTruststore,
                sslTruststorePassword, sslKeystore, sslKeystorePassword, sslKeystoreType, sslVerificationMode,
                timeoutMillis, connectionPoolSize, connectionMinimumIdleSize, keyPrefix, defaultTtlSeconds, cookieName,
                contextKeyPrefix);
        return 31 * result + Arrays.hashCode(sslProtocols);
    }
}
