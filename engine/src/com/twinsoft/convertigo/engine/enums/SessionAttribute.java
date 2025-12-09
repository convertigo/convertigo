/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.twinsoft.convertigo.engine.enums.SessionAttribute$KeepMode
 *  com.twinsoft.convertigo.engine.requesters.HttpSessionListener
 *  javax.servlet.http.HttpSession
 */
package com.twinsoft.convertigo.engine.enums;

import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import java.util.HashSet;
import java.util.List;
import javax.servlet.http.HttpSession;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public enum SessionAttribute {
    authenticatedUser(String.class),
    authenticatedUserGrp(String.class),
    authenticatedUserGrpCheck(String.class),
    clientIP(String.class, KeepMode.NEVER),
    contexts(List.class, KeepMode.NEVER),
    deviceUUID(String.class),
    exception(Throwable.class),
    fullSyncRequests(HashSet.class),
    isNew(Boolean.class, KeepMode.NEVER),
    httpClient3("__httpClient3__", null),
    httpClient4("__httpClient4__", null),
    sessionListener(HttpSessionListener.class, KeepMode.NEVER),
    xsrfToken(String.class),
    userAgent(String.class, KeepMode.NEVER);

    String value;
    Class<?> expectedClass;
    KeepMode keepMode;

    private SessionAttribute() {
        this(null, null, KeepMode.ALWAYS, true);
    }

    private SessionAttribute(Class<?> expectedClass) {
        this(null, expectedClass, KeepMode.ALWAYS, true);
    }

    private SessionAttribute(Class<?> expectedClass, KeepMode keepMode) {
        this(null, expectedClass, keepMode, true);
    }

    private SessionAttribute(String value) {
        this(value, null, KeepMode.ALWAYS, false);
    }

    private SessionAttribute(String value, Class<?> expectedClass) {
        this(value, expectedClass, KeepMode.ALWAYS, false);
    }

    private SessionAttribute(String value, Class<?> expectedClass, KeepMode keepMode) {
        this(value, expectedClass, keepMode, false);
    }

    private SessionAttribute(String value, Class<?> expectedClass, KeepMode keepMode, boolean autoPrefix) {
        this.value = autoPrefix ? "__c8o:" + this.name() + "__" : value;
        this.expectedClass = expectedClass;
        this.keepMode = keepMode == null ? KeepMode.ALWAYS : keepMode;
    }

    public String value() {
        return this.value;
    }

    public Class<?> expectedClass() {
        return this.expectedClass;
    }

    public KeepMode keepMode() {
        return this.keepMode;
    }

    public void set(HttpSession session, Object value) {
        if (session != null) {
            session.setAttribute(this.value(), value);
        }
    }

    public <E> E get(HttpSession session) {
        return this.get(session, null);
    }

    public <E> E get(HttpSession session, E defaultValue) {
        Object res = session == null ? null : session.getAttribute(this.value());
        Object value = res == null ? defaultValue : res;
        if (this.expectedClass != null && value != null && !this.expectedClass.isInstance(value)) {
            return defaultValue;
        }
        return GenericUtils.cast(value);
    }

    public boolean has(HttpSession session) {
        if (session != null) {
            return session.getAttribute(this.value()) != null;
        }
        return false;
    }

    public void remove(HttpSession session) {
        if (session != null) {
            session.removeAttribute(this.value());
        }
    }

    public String string(HttpSession session) {
        return this.string(session, null);
    }

    public String string(HttpSession session, String defaultValue) {
        Object res = session == null ? null : session.getAttribute(this.value());
        return res == null ? defaultValue : res.toString();
    }

    public static SessionAttribute fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (SessionAttribute attr : SessionAttribute.values()) {
            if (!attr.value.equals(value)) continue;
            return attr;
        }
        return null;
    }

    public enum KeepMode {
        ALWAYS, NEVER, ON_DEMAND;

        public boolean shouldKeep(boolean onDemandEnabled) {
            return this == ALWAYS || (this == ON_DEMAND && onDemandEnabled);
        }
    }
}
