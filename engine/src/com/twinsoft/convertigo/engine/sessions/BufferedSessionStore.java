/*
 * Decompiled with CFR 0.152.
 */
package com.twinsoft.convertigo.engine.sessions;

import java.util.HashMap;
import java.util.Map;

final class BufferedSessionStore
implements SessionStore {
    private final SessionStore delegate;
    private final ThreadLocal<Map<String, Buffer>> local = ThreadLocal.withInitial(HashMap::new);

    BufferedSessionStore(SessionStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public SessionData read(String sessionId) {
        Map<String, Buffer> map = this.local.get();
        Buffer buf = map.get(sessionId);
        if (buf != null) {
            return buf.data;
        }
        SessionData data = this.delegate.read(sessionId);
        if (data != null) {
            map.put(sessionId, new Buffer(data));
        }
        return data;
    }

    @Override
    public void save(SessionData session) {
        if (session == null) {
            return;
        }
        Map<String, Buffer> map = this.local.get();
        Buffer existing = map.get(session.getId());
        if (existing != null) {
            existing.data = session;
            existing.dirty = true;
        } else {
            map.put(session.getId(), new Buffer(session, true));
        }
    }

    @Override
    public void delete(String sessionId) {
        this.local.get().remove(sessionId);
        this.delegate.delete(sessionId);
    }

    @Override
    public void shutdown() {
        this.delegate.shutdown();
    }

    void flushThread() {
        Map<String, Buffer> map = this.local.get();
        if (map.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Buffer> entry : map.entrySet()) {
            Buffer buf = entry.getValue();
            if (!buf.dirty) continue;
            this.delegate.save(buf.data);
        }
        map.clear();
    }

    private static final class Buffer {
        SessionData data;
        boolean dirty;

        Buffer(SessionData data) {
            this(data, false);
        }

        Buffer(SessionData data, boolean dirty) {
            this.data = data;
            this.dirty = dirty;
        }
    }
}
