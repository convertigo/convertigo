package com.twinsoft.convertigo.engine.sessions;

import com.twinsoft.convertigo.engine.Context;

public interface ContextStore {
    Context read(String contextId);
    void save(Context context, int ttlSeconds);
    void delete(String contextId);
    void deleteBySessionPrefix(String sessionIdPrefix);
    void shutdown();
}
