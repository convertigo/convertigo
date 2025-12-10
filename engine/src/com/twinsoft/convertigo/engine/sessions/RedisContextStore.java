/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.twinsoft.convertigo.engine.Context
 *  com.twinsoft.convertigo.engine.Engine
 *  org.redisson.Redisson
 *  org.redisson.api.RBucket
 *  org.redisson.api.RedissonClient
 *  org.redisson.api.options.KeysScanOptions
 *  org.redisson.config.Config
 *  org.redisson.config.SingleServerConfig
 */
package com.twinsoft.convertigo.engine.sessions;

import java.time.Duration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public final class RedisContextStore
implements ContextStore {
    private final RedissonClient client;
    private final RedisSessionConfiguration configuration;
    private final ObjectMapper mapper = new ObjectMapper();

    public RedisContextStore(RedisSessionConfiguration configuration) {
        this.configuration = configuration;
        this.client = this.createClient(configuration);
    }

    private RedissonClient createClient(RedisSessionConfiguration cfg) {
        var config = new Config();
        var singleServer = (SingleServerConfig)config.useSingleServer().setAddress(cfg.getAddress()).setDatabase(cfg.getDatabase()).setTimeout(cfg.getTimeoutMillis());
        if (cfg.getUsername() != null) {
            singleServer.setUsername(cfg.getUsername());
        }
        if (cfg.getPassword() != null) {
            singleServer.setPassword(cfg.getPassword());
        }
        return Redisson.create((Config)config);
    }

    private String key(String contextId) {
        return this.configuration.contextKey(contextId);
    }

	@Override
	public Context read(String contextId) {
		try {
			var bucket = this.client.<String>getBucket(this.key(contextId));
			var json = bucket.get();
			if (json == null) {
				return null;
			}
			var snap = (SessionAttributeSanitizer.ContextSnapshot)this.mapper.readValue(json, SessionAttributeSanitizer.ContextSnapshot.class);
			return snap.toContext();
        }
        catch (Exception e) {
            this.log("(RedisContextStore) Failed to read context " + contextId, e);
            return null;
        }
    }

    @Override
    public void save(Context context, int ttlSeconds) {
        if (context == null) {
            return;
        }
        try {
			var snap = SessionAttributeSanitizer.toSnapshot(context);
			if (snap == null) {
				return;
			}
			var json = this.mapper.writeValueAsString((Object)snap);
			var bucket = this.client.<String>getBucket(this.key(context.contextID));
			var ttl = ttlSeconds > 0 ? ttlSeconds : this.configuration.getDefaultTtlSeconds();
			if (ttl > 0) {
				bucket.set(json, Duration.ofSeconds(ttl));
			} else {
				bucket.set(json);
			}
        }
        catch (Exception e) {
            this.log("(RedisContextStore) Failed to save context " + context.contextID, e);
        }
    }

    @Override
    public void delete(String contextId) {
        try {
            this.client.getBucket(this.key(contextId)).delete();
        }
        catch (Exception e) {
            this.log("(RedisContextStore) Failed to delete context " + contextId, e);
        }
    }

    @Override
    public void deleteBySessionPrefix(String sessionIdPrefix) {
        try {
            var prefix = this.configuration.getContextKeyPrefix() + "context:" + sessionIdPrefix;
            var options = KeysScanOptions.defaults().pattern(prefix + "*");
            for (var key : this.client.getKeys().getKeys(options)) {
                this.client.getBucket(key).delete();
            }
        }
        catch (Exception e) {
            this.log("(RedisContextStore) Failed to delete contexts by prefix " + sessionIdPrefix, e);
        }
    }

    @Override
    public void shutdown() {
        try {
            this.client.shutdown();
        }
        catch (Exception e) {
            this.log("(RedisContextStore) Failed to shutdown", e);
        }
    }

    private void log(String message, Exception e) {
        try {
            if (Engine.logEngine != null) {
                if (e == null) {
                    Engine.logEngine.warn((Object)message);
                } else {
                    Engine.logEngine.warn((Object)message, (Throwable)e);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}
