/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.twinsoft.convertigo.engine.EnginePropertiesManager
 *  com.twinsoft.convertigo.engine.EnginePropertiesManager$PropertyName
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
        String normalized = keyPrefix == null ? "" : keyPrefix.replaceAll(":+$", "");
        String ctxPrefix = this.keyPrefix = normalized.isEmpty() ? "" : normalized + ":";
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
        String host = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_HOST);
        int port = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_PORT, 6379);
        String username = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_USERNAME);
        String password = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_PASSWORD);
        int database = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_DATABASE, 0);
        boolean ssl = EnginePropertiesManager.getPropertyAsBoolean((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_SSL);
        int timeoutMillis = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_TIMEOUT, 5000);
        String prefix = EnginePropertiesManager.getProperty((EnginePropertiesManager.PropertyName)EnginePropertiesManager.PropertyName.SESSION_REDIS_PREFIX);
        int ttl = RedisSessionConfiguration.parseInt(EnginePropertiesManager.PropertyName.SESSION_REDIS_DEFAULT_TTL, 1800);
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
        String protocol = this.ssl ? "rediss" : "redis";
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
