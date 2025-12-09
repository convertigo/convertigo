/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JavaType
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.twinsoft.convertigo.engine.Engine
 *  com.twinsoft.convertigo.engine.enums.SessionAttribute
 *  org.redisson.Redisson
 *  org.redisson.api.RMap
 *  org.redisson.api.RScript$Mode
 *  org.redisson.api.RScript$ReturnType
 *  org.redisson.api.RedissonClient
 *  org.redisson.client.codec.Codec
 *  org.redisson.client.codec.StringCodec
 *  org.redisson.config.Config
 *  org.redisson.config.SingleServerConfig
 */
package com.twinsoft.convertigo.engine.sessions;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.sessions.RedisSessionConfiguration;
import com.twinsoft.convertigo.engine.sessions.SessionAttributeSanitizer;
import com.twinsoft.convertigo.engine.sessions.SessionData;
import com.twinsoft.convertigo.engine.sessions.SessionStore;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

final class RedisHashSessionStore
implements SessionStore {
    private static final String META_CREATION = "__meta:creationTime";
    private static final String META_LAST_ACCESS = "__meta:lastAccessedTime";
    private static final String META_MAX_INACTIVE = "__meta:maxInactiveInterval";
    private static final String LUA_HGETALL_AND_TOUCH = "local key=KEYS[1]; local ttl=tonumber(ARGV[1])\nlocal res=redis.call('HGETALL', key)\nif ttl and ttl>0 then redis.call('PEXPIRE', key, ttl) end\nreturn res\n";
    private static final String LUA_HSET_DEL_AND_TOUCH = "local key=KEYS[1]; local ttl=tonumber(ARGV[1]); local setCount=tonumber(ARGV[2]);\nlocal idx=3;\nif setCount>0 then redis.call('HSET', key, unpack(ARGV, idx, idx + setCount*2 - 1)); idx = idx + setCount*2; end\nlocal delCount=tonumber(ARGV[idx]); idx = idx + 1;\nif delCount>0 then redis.call('HDEL', key, unpack(ARGV, idx, idx + delCount - 1)); end\nif ttl and ttl>0 then redis.call('PEXPIRE', key, ttl) end\nreturn true\n";
    private final RedissonClient client;
    private final RedisSessionConfiguration configuration;
    private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    RedisHashSessionStore(RedisSessionConfiguration configuration) {
        this.configuration = configuration;
        this.client = this.createClient(configuration);
    }

    private RedissonClient createClient(RedisSessionConfiguration cfg) {
        String password;
        Config config = new Config();
        SingleServerConfig singleServer = (SingleServerConfig)config.useSingleServer().setAddress(cfg.getAddress()).setDatabase(cfg.getDatabase()).setTimeout(cfg.getTimeoutMillis());
        String username = cfg.getUsername();
        if (username != null) {
            singleServer.setUsername(username);
        }
        if ((password = cfg.getPassword()) != null) {
            singleServer.setPassword(password);
        }
        return Redisson.create((Config)config);
    }

    private RMap<String, String> map(String sessionId) {
        return this.client.getMap(this.configuration.key(sessionId), (Codec)StringCodec.INSTANCE);
    }

    @Override
    public SessionData read(String sessionId) {
        try {
            long ttlMillis = this.configuration.getDefaultTtlSeconds() > 0 ? (long)this.configuration.getDefaultTtlSeconds() * 1000L : 0L;
            List entries = (List)this.client.getScript((Codec)StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_HGETALL_AND_TOUCH, RScript.ReturnType.MULTI, Collections.singletonList(this.configuration.key(sessionId)), new Object[]{ttlMillis});
            if (entries == null || entries.isEmpty()) {
                this.debug("MISS " + sessionId);
                return null;
            }
            HashMap<String, String> mapSnapshot = new HashMap<String, String>(entries.size() / 2);
            for (int i = 0; i < entries.size(); i += 2) {
                mapSnapshot.put((String)entries.get(i), (String)entries.get(i + 1));
            }
            long creationTime = this.readLong((String)mapSnapshot.get(META_CREATION));
            long lastAccess = this.readLong((String)mapSnapshot.get(META_LAST_ACCESS));
            int maxInactive = this.readInt((String)mapSnapshot.get(META_MAX_INACTIVE), this.configuration.getDefaultTtlSeconds());
            SessionData data = SessionData.newSession(sessionId, this.configuration.getDefaultTtlSeconds());
            long now = System.currentTimeMillis();
            data.setCreationTime(creationTime > 0L ? creationTime : now);
            data.setLastAccessedTime(lastAccess > 0L ? lastAccess : now);
            data.setMaxInactiveInterval(maxInactive);
            data.setNew(false);
            for (Map.Entry entry : mapSnapshot.entrySet()) {
                String key = (String)entry.getKey();
                if (key.startsWith("__meta:")) continue;
                Object val = this.deserialize(key, (String)entry.getValue());
                if (SessionAttribute.contexts.value().equals(key)) {
                    val = SessionAttributeSanitizer.restoreContexts(val);
                }
                if (val == null) continue;
                data.setAttribute(key, val);
            }
            this.debug("HIT " + sessionId + " hashSize=" + mapSnapshot.size());
            return data;
        }
        catch (Exception e) {
            this.log("(RedisHashSessionStore) Failed to read session " + sessionId, e);
            return null;
        }
    }

    @Override
    public void save(SessionData session) {
        if (session == null) {
            return;
        }
        try {
            if (!session.getAttributes().containsKey(SessionAttribute.contexts.value())) {
                this.log("(RedisHashSessionStore) save(): session " + session.getId() + " has no contexts attribute before sanitize", null);
            }
            SessionData sanitized = SessionAttributeSanitizer.sanitize(session);
            String sessionId = sanitized.getId();
            Pending pendingEntry = this.pending.compute(sessionId, (id, existing) -> {
                if (existing == null) {
                    return new Pending(SessionData.copyOf(sanitized));
                }
                existing.data = SessionData.copyOf(sanitized);
                return existing;
            });
            pendingEntry.dirty = true;
            this.flushPending(sessionId, pendingEntry);
        }
        catch (Exception e) {
            this.log("(RedisHashSessionStore) Failed to save session " + session.getId(), e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void flushPending(String sessionId, Pending p) {
        SessionData data;
        Pending pending = p;
        synchronized (pending) {
            if (!p.dirty) {
                return;
            }
            data = SessionData.copyOf(p.data);
            p.dirty = false;
        }
        try {
            HashMap<String, String> bulk = new HashMap<String, String>();
            HashMap<String, String> meta = new HashMap<String, String>();
            meta.put(META_CREATION, this.writeLong(data.getCreationTime()));
            meta.put(META_LAST_ACCESS, this.writeLong(data.getLastAccessedTime()));
            meta.put(META_MAX_INACTIVE, this.writeInt(data.getMaxInactiveInterval()));
            for (Map.Entry entry : meta.entrySet()) {
                String previous;
                String string = (String)entry.getKey();
                String v = (String)entry.getValue();
                if (v.equals(previous = p.lastMeta.get(string))) continue;
                bulk.put(string, v);
            }
            HashMap<String, String> newAttrsSnapshot = new HashMap<String, String>();
            for (Map.Entry<String, Object> entry : data.getAttributes().entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;
                String serialized = this.serialize(name, value);
                newAttrsSnapshot.put(name, serialized);
                String previous = p.lastAttrs.get(name);
                if (serialized.equals(previous)) continue;
                bulk.put(name, serialized);
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String prevKey : p.lastAttrs.keySet()) {
                if (SessionAttribute.contexts.value().equals(prevKey) || newAttrsSnapshot.containsKey(prevKey)) continue;
                arrayList.add(prevKey);
            }
            if (p.lastAttrs.containsKey(SessionAttribute.contexts.value()) && !newAttrsSnapshot.containsKey(SessionAttribute.contexts.value())) {
                String string = p.lastAttrs.get(SessionAttribute.contexts.value());
                if (string != null) {
                    newAttrsSnapshot.put(SessionAttribute.contexts.value(), string);
                    bulk.put(SessionAttribute.contexts.value(), string);
                    this.log("(RedisHashSessionStore) contexts missing in new snapshot for session " + sessionId + ", reusing previous value to keep contexts", null);
                } else {
                    this.log("(RedisHashSessionStore) contexts attribute missing in new snapshot for session " + sessionId + " and no previous value, skipping delete", null);
                }
            }
            if (!bulk.isEmpty() || !arrayList.isEmpty()) {
                long l = data.getMaxInactiveInterval() > 0 ? (long)data.getMaxInactiveInterval() * 1000L : (long)this.configuration.getDefaultTtlSeconds() * 1000L;
                int setCount = bulk.size();
                int delCount = arrayList.size();
                ArrayList<Object> args = new ArrayList<Object>(2 + setCount * 2 + 1 + delCount);
                args.add(l);
                args.add(setCount);
                for (Map.Entry e : bulk.entrySet()) {
                    args.add(e.getKey());
                    args.add(e.getValue());
                }
                args.add(delCount);
                args.addAll(arrayList);
                this.client.getScript((Codec)StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, LUA_HSET_DEL_AND_TOUCH, RScript.ReturnType.VALUE, Collections.singletonList(this.configuration.key(sessionId)), args.toArray());
                p.lastMeta.clear();
                p.lastMeta.putAll(meta);
                p.lastAttrs.clear();
                p.lastAttrs.putAll(newAttrsSnapshot);
            }
            int n = data.getMaxInactiveInterval() > 0 ? data.getMaxInactiveInterval() : this.configuration.getDefaultTtlSeconds();
            this.debug("SAVE(hash/coalesce) " + sessionId + ", ttl=" + n + ", fields=" + bulk.size());
        }
        catch (Exception e) {
            this.log("(RedisHashSessionStore) Failed to flush session " + sessionId, e);
        }
    }

    @Override
    public void delete(String sessionId) {
        try {
            this.map(sessionId).delete();
            this.debug("DEL(hash) " + sessionId);
        }
        catch (Exception e) {
            this.log("(RedisHashSessionStore) Failed to delete session " + sessionId, e);
        }
    }

    @Override
    public void shutdown() {
        try {
            this.client.shutdown();
        }
        catch (Exception e) {
            this.log("(RedisHashSessionStore) Failed to shutdown Redis client", e);
        }
    }

    private String serialize(String key, Object value) throws Exception {
        SessionAttribute hintAttr = SessionAttribute.fromValue((String)key);
        if (hintAttr != null && hintAttr.expectedClass() != null) {
            return MAPPER.writeValueAsString(value);
        }
        TypedValue wrapper = new TypedValue(value);
        return MAPPER.writeValueAsString((Object)wrapper);
    }

    private Object deserialize(String key, String value) {
        if (value == null) {
            return null;
        }
        try {
            SessionAttribute hintAttr = SessionAttribute.fromValue((String)key);
            if (hintAttr != null && hintAttr.expectedClass() != null) {
                JsonNode tree = MAPPER.readTree(value);
                JsonNode node = tree.has("value") ? tree.get("value") : tree;
                return MAPPER.convertValue((Object)node, MAPPER.getTypeFactory().constructType((Type)hintAttr.expectedClass()));
            }
            JsonNode node = MAPPER.readTree(value);
            JsonNode clazzNode = node.get("clazz");
            JsonNode valNode = node.get("value");
            if (clazzNode == null || valNode == null) {
                return MAPPER.convertValue((Object)node, Object.class);
            }
            String className = clazzNode.asText();
            try {
                Class<?> cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                if (Set.class.isAssignableFrom(cls)) {
                    List list = (List)MAPPER.convertValue((Object)valNode, (JavaType)MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, Object.class));
                    return new HashSet(list);
                }
                return MAPPER.convertValue((Object)valNode, cls);
            }
            catch (ClassNotFoundException e) {
                return MAPPER.convertValue((Object)valNode, Object.class);
            }
        }
        catch (Exception e) {
            this.log("(RedisHashSessionStore) Failed to deserialize attribute", e);
            return null;
        }
    }

    private String writeLong(long v) throws Exception {
        return Long.toString(v);
    }

    private String writeInt(int v) throws Exception {
        return Integer.toString(v);
    }

    private long readLong(String value) {
        Object o = this.deserialize(null, value);
        if (o instanceof Number) {
            Number n = (Number)o;
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o));
        }
        catch (Exception ignore) {
            return 0L;
        }
    }

    private int readInt(String value, int defaultValue) {
        Object o = this.deserialize(null, value);
        if (o instanceof Number) {
            Number n = (Number)o;
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        }
        catch (Exception ignore) {
            return defaultValue;
        }
    }

    private void log(String message, Exception e) {
        try {
            if (Engine.logEngine != null) {
                Engine.logEngine.warn((Object)message, (Throwable)e);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void debug(String message) {
        try {
            if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
                Engine.logEngine.debug((Object)("(RedisHashSessionStore) " + message));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static final class Pending {
        SessionData data;
        boolean dirty;
        final Map<String, String> lastMeta = new HashMap<String, String>();
        final Map<String, String> lastAttrs = new HashMap<String, String>();

        Pending(SessionData data) {
            this.data = data;
            this.dirty = true;
        }
    }

    private static final class TypedValue {
        public String clazz;
        public Object value;

        TypedValue(Object value) {
            this.value = value;
            this.clazz = value != null ? value.getClass().getName() : null;
        }
    }
}
