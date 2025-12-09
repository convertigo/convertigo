/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.twinsoft.convertigo.engine.Context
 *  com.twinsoft.convertigo.engine.Engine
 *  com.twinsoft.convertigo.engine.enums.SessionAttribute
 *  com.twinsoft.convertigo.engine.enums.SessionAttribute$KeepMode
 *  org.apache.commons.httpclient.Cookie
 *  org.apache.commons.httpclient.HttpState
 */
package com.twinsoft.convertigo.engine.sessions;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.SessionData;
import com.twinsoft.convertigo.engine.sessions.SessionStoreMode;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.w3c.dom.Document;

final class SessionAttributeSanitizer {
    private static final Set<String> TECHNICAL_PREFIXES = Set.of("session:");
    private static final boolean ENABLE_ON_DEMAND = Boolean.parseBoolean(System.getProperty("convertigo.engine.session.keep.on_demand", "false"));

    private SessionAttributeSanitizer() {
    }

    static SessionData sanitize(SessionData session) {
        if (session == null) {
            return null;
        }
        try {
            SessionData copy = SessionData.copyOf(session);
            Map<String, Object> attributes = copy.getAttributes();
            attributes.keySet().removeIf(SessionAttributeSanitizer::isTechnicalAttribute);
            attributes.keySet().removeIf(name -> !SessionAttributeSanitizer.shouldKeep(name));
            if (ConvertigoHttpSessionManager.getInstance().getStoreMode() == SessionStoreMode.redis) {
                Object ctxValue = attributes.get(SessionAttribute.contexts.value());
                if (ctxValue instanceof List<?>) {
                    try {
                        int sessionTtl = session.getMaxInactiveInterval(); // httpSessionTimeout (tomcat mode)
                        int projectContextTtl = 0;
                        List<?> list = (List<?>) ctxValue;
                        if (!list.isEmpty()) {
                            Object first = list.get(0);
                            String projectName = null;
                            if (first instanceof ContextSnapshot cs) {
                                projectName = cs.projectName;
                            } else if (first instanceof Context ctx) {
                                projectName = ctx.projectName;
                                if (ctx.project != null) {
                                    projectContextTtl = ctx.project.getContextTimeout();
                                }
                            }
                            if (projectName != null && projectContextTtl <= 0) {
                                try {
                                    var project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
                                    if (project != null) {
                                        projectContextTtl = project.getContextTimeout();
                                    }
                                } catch (Exception ignore) {
                                }
                            }
                        }
                        int ttl = sessionTtl > 0 && projectContextTtl > 0 ? Math.min(sessionTtl, projectContextTtl)
                                : (sessionTtl > 0 ? sessionTtl : projectContextTtl);
                        Engine.theApp.contextManager.saveContexts(GenericUtils.cast((List<?>)ctxValue), ttl);
                    } catch (Exception e) {
                        Engine.logEngine.warn("(SessionAttributeSanitizer) Failed to persist contexts for redis store", e);
                    }
                }
                attributes.remove(SessionAttribute.contexts.value());
            } else {
                SessionAttributeSanitizer.sanitizeContexts(attributes);
            }
            SessionAttributeSanitizer.sanitizeDom(attributes);
            attributes.remove("__c8o:sessionListener__");
            attributes.remove("__c8o:clientIP__");
            attributes.remove("__c8o:userAgent__");
            attributes.remove("__c8o:isNew__");
            return copy;
        }
        catch (Exception e) {
            if (Engine.logEngine != null) {
                Engine.logEngine.debug((Object)("(SessionAttributeSanitizer) Failed to sanitize session " + session.getId()), (Throwable)e);
            }
            return session;
        }
    }

    static boolean isTechnicalAttribute(String name) {
        if (name == null) {
            return false;
        }
        for (String prefix : TECHNICAL_PREFIXES) {
            if (!name.startsWith(prefix)) continue;
            return true;
        }
        return false;
    }

    private static void sanitizeContexts(Map<String, Object> attributes) {
        Object ctxValue = attributes.get("__c8o:contexts__");
        if (ctxValue instanceof List) {
            List list = (List)ctxValue;
            ArrayList<ContextSnapshot> lightContexts = new ArrayList<ContextSnapshot>(list.size());
            for (Object item : list) {
                ContextSnapshot snap = SessionAttributeSanitizer.toSnapshot(item);
                if (snap == null) continue;
                lightContexts.add(snap);
            }
            lightContexts.sort(Comparator.comparing(s -> s.contextId, Comparator.nullsLast(String::compareTo)));
            attributes.put("__c8o:contexts__", lightContexts);
        }
    }

    static Object restoreContexts(Object value) {
        if (!(value instanceof List)) {
            return value;
        }
        List<?> list = (List<?>) value;
        ArrayList<Context> contexts = list.stream()
                .map(SessionAttributeSanitizer::toSnapshot)
                .filter(snap -> snap != null && snap.contextId != null)
                .map(ContextSnapshot::toContext)
                .collect(Collectors.toCollection(ArrayList::new));
        return contexts;
    }

    public static ContextSnapshot toSnapshot(Object item) {
        if (item instanceof Context) {
            Context ctx = (Context)item;
            ContextSnapshot light = new ContextSnapshot();
            light.contextId = ctx.contextID;
            light.name = ctx.name;
            light.projectName = ctx.projectName;
            light.connectorName = ctx.connectorName;
            light.transactionName = ctx.transactionName;
            light.sequenceName = ctx.sequenceName;
            light.isDestroying = false;
            light.isErrorDocument = ctx.isErrorDocument;
            light.isNewSession = ctx.isNewSession;
            if (ctx.httpState != null) {
                light.httpStateCookiePolicy = ctx.httpState.getCookiePolicy();
                Cookie[] cookies = ctx.httpState.getCookies();
                int n = light.httpStateCookies = cookies != null ? cookies.length : 0;
                if (cookies != null && cookies.length > 0) {
                    light.cookies = new ArrayList<CookieSnapshot>(cookies.length);
                    for (Cookie c : cookies) {
                        CookieSnapshot snap = new CookieSnapshot();
                        snap.name = c.getName();
                        snap.value = c.getValue();
                        snap.domain = c.getDomain();
                        snap.path = c.getPath();
                        snap.secure = c.getSecure();
                        snap.version = c.getVersion();
                        Date exp = c.getExpiryDate();
                        snap.expiry = exp != null ? exp.getTime() : -1L;
                        light.cookies.add(snap);
                    }
                }
            }
            light.trimNulls();
            return light;
        }
        if (item instanceof ContextSnapshot) {
            ContextSnapshot snapshot = (ContextSnapshot)item;
            return snapshot;
        }
        if (item instanceof Map) {
            Map map = (Map)item;
            ContextSnapshot snap = new ContextSnapshot();
            snap.contextId = SessionAttributeSanitizer.string(map.get("contextId"));
            snap.name = SessionAttributeSanitizer.string(map.get("name"));
            snap.projectName = SessionAttributeSanitizer.string(map.get("projectName"));
            snap.connectorName = SessionAttributeSanitizer.string(map.get("connectorName"));
            snap.transactionName = SessionAttributeSanitizer.string(map.get("transactionName"));
            snap.sequenceName = SessionAttributeSanitizer.string(map.get("sequenceName"));
            snap.isDestroying = SessionAttributeSanitizer.bool(map.get("isDestroying"));
            snap.isErrorDocument = SessionAttributeSanitizer.bool(map.get("isErrorDocument"));
            snap.isNewSession = SessionAttributeSanitizer.bool(map.get("isNewSession"));
            snap.httpStateCookiePolicy = SessionAttributeSanitizer.number(map.get("httpStateCookiePolicy"));
            snap.httpStateCookies = SessionAttributeSanitizer.number(map.get("httpStateCookies"));
            snap.trimNulls();
            return snap;
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean) {
            Boolean b = (Boolean)value;
            return b;
        }
        if (value instanceof Number) {
            Number n = (Number)value;
            return n.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static int number(Object value) {
        if (value instanceof Number) {
            Number n = (Number)value;
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        }
        catch (Exception e) {
            return 0;
        }
    }

    private static void sanitizeDom(Map<String, Object> attributes) {
        attributes.entrySet().removeIf(entry -> entry.getValue() instanceof Document);
    }

    private static boolean shouldKeep(String name) {
        SessionAttribute attr = SessionAttribute.fromValue((String)name);
        if (attr == null) {
            return true;
        }
        SessionAttribute.KeepMode mode = attr.keepMode();
        return mode == null || mode.shouldKeep(ENABLE_ON_DEMAND);
    }

    static final class ContextSnapshot
    implements Serializable {
        private static final long serialVersionUID = 1L;
        public String contextId;
        public String name;
        public String projectName;
        public String connectorName;
        public String transactionName;
        public String sequenceName;
        public boolean isDestroying;
        public boolean isErrorDocument;
        public boolean isNewSession;
        public int httpStateCookiePolicy;
        public int httpStateCookies;
        public List<CookieSnapshot> cookies;

        ContextSnapshot() {
        }

        void trimNulls() {
            if (this.connectorName != null && this.connectorName.isEmpty()) {
                this.connectorName = null;
            }
            if (this.transactionName != null && this.transactionName.isEmpty()) {
                this.transactionName = null;
            }
            if (this.sequenceName != null && this.sequenceName.isEmpty()) {
                this.sequenceName = null;
            }
            if (this.name != null && this.name.isEmpty()) {
                this.name = null;
            }
            if (this.projectName != null && this.projectName.isEmpty()) {
                this.projectName = null;
            }
        }

        Context toContext() {
            Context ctx = new Context(this.contextId);
            ctx.name = this.name;
            ctx.projectName = this.projectName;
            ctx.connectorName = this.connectorName;
            ctx.transactionName = this.transactionName;
            ctx.sequenceName = this.sequenceName;
            ctx.isDestroying = this.isDestroying;
            ctx.isErrorDocument = this.isErrorDocument;
            ctx.isNewSession = this.isNewSession;
            if (this.httpStateCookiePolicy != 0 || this.httpStateCookies != 0 || this.cookies != null && !this.cookies.isEmpty()) {
                HttpState state = new HttpState();
                state.setCookiePolicy(this.httpStateCookiePolicy);
                if (this.cookies != null) {
                    for (CookieSnapshot c : this.cookies) {
                        try {
                            Cookie cookie = new Cookie(c.domain, c.name, c.value, c.path, c.expiry > 0L ? new Date(c.expiry) : null, c.secure);
                            cookie.setVersion(c.version);
                            state.addCookie(cookie);
                        }
                        catch (Exception exception) {}
                    }
                }
                ctx.httpState = state;
            }
            return ctx;
        }
    }

    static final class CookieSnapshot
    implements Serializable {
        private static final long serialVersionUID = 1L;
        public String name;
        public String value;
        public String domain;
        public String path;
        public boolean secure;
        public int version;
        public long expiry = -1L;

        CookieSnapshot() {
        }
    }
}
