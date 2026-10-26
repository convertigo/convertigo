/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.RequestableInvocation;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Invocation-local view for the JSON execution contract. No managed Context,
 * context-manager entry, worker, session or document is created for this view.
 * Legacy connectors, XML/attachment consumers and custom controllers keep a requester.
 */
public final class ChildInvocation implements AutoCloseable {
    public final Project project;
    public final Flow requestedObject;
    public final String projectName, sequenceName, name, contextID;
    public final String connectorName = null, transactionName = null;
    public final Object parentContext = null;
    public final HttpSession httpSession;
    public final HttpServletRequest httpServletRequest;
    public final String remoteAddr, userAgent;
    private final Object parent;
    private final Boolean authenticationRequired;
    private final RequestableObject.InvocationGuard guard;
    private final long started;
    private Map<String, Object> attributes;
    private boolean cancelled, closed;
    private AutoCloseable scope;
    private final Log4jHelper.MdcSnapshot previousLog = Log4jHelper.mdcSnapshot();
    private final ClassLoader previousLoader = Thread.currentThread().getContextClassLoader();

    ChildInvocation(Flow target, Object parent, RequestableObject.InvocationGuard guard) {
        this(target, parent, guard, null, System.nanoTime());
    }
    ChildInvocation(Flow target, Object parent, RequestableObject.InvocationGuard guard, Boolean authenticationRequired, long startedNanos) {
        started = startedNanos;
        this.parent = parent;
        this.authenticationRequired = authenticationRequired;
        this.guard = guard;
        requestedObject = target; project = target.getProject();
        projectName = project.getName(); sequenceName = target.getName();
        httpSession = parent instanceof Context c ? c.httpSession : ((ChildInvocation) parent).httpSession;
        httpServletRequest = parent instanceof Context c ? c.httpServletRequest : ((ChildInvocation) parent).httpServletRequest;
        remoteAddr = httpServletRequest.getRemoteAddr(); userAgent = httpServletRequest.getHeader("User-Agent");
        name = "flow-" + UUID.randomUUID(); contextID = httpSession.getId() + "_" + name;
        Log4jHelper.mdcPut(Log4jHelper.mdcKeys.ContextID, contextID);
        Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Project, projectName);
        Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Sequence, sequenceName);
        updateUserInLog();
    }

    void attach(AutoCloseable scope) { this.scope = scope; }
    void authorize() throws EngineException {
        // InternalRequester deliberately has no external accessibility or TLS restriction.
        RequestableInvocation.authorize(() -> {}, () -> {},
                () -> RequestableInvocation.authentication(authenticationRequired == null ? requestedObject.getAuthenticatedContextRequired() : authenticationRequired, httpSession, false),
                () -> RequestableInvocation.parent(isRunning()));
    }
    public boolean isRunning() {
        return !closed && !cancelled && guard.isRunning()
                && (!(parent instanceof ChildInvocation child) || child.isRunning());
    }
    public void abortRequestable() { cancelled = true; }
    public String getAuthenticatedUser() { return SessionAttribute.authenticatedUser.string(httpSession); }
    public void setAuthenticatedUser(String user) {
        if (user != null) SessionAttribute.authenticatedUser.set(httpSession, user);
        updateUserInLog();
    }
    public void removeAuthenticatedUser() { SessionAttribute.authenticatedUser.remove(httpSession); updateUserInLog(); }
    public void updateUserInLog() {
        var user = getAuthenticatedUser();
        Log4jHelper.mdcPut(Log4jHelper.mdcKeys.User, user == null ? "(anonymous)" : "'" + user + "'");
    }
    public String getProjectName() { return projectName; }
    public String getProjectDirectory() { return project.getDirPath(); }
    public String getConvertigoUrl() { return HttpUtils.convertigoRequestURL(httpServletRequest) + '/'; }
    public String getProjectUrl() { return HttpUtils.projectRequestURL(httpServletRequest) + '/'; }
    public String getAbsoluteRequestedUrl() { return HttpUtils.originalRequestURL(httpServletRequest); }
    public Object get(String key) { return attributes == null ? null : attributes.get(key); }
    public void set(String key, Object value) { if (attributes == null) attributes = new HashMap<>(); attributes.put(key, value); }
    public void remove(String key) { if (attributes != null) attributes.remove(key); }
    public void setResponseHeader(String name, String value) {
        Map<String, String> headers = RequestAttribute.responseHeader.get(httpServletRequest);
        if (headers == null) RequestAttribute.responseHeader.set(httpServletRequest, headers = new HashMap<>());
        headers.put(name, value);
    }
    public void setResponseStatus(Integer code, String text) {
        Map<Integer, String> status = RequestAttribute.responseStatus.get(httpServletRequest);
        if (status == null) RequestAttribute.responseStatus.set(httpServletRequest, status = new HashMap<>());
        status.clear(); status.put(code, text);
    }
    private RequestableInvocation.Billing billing() {
        var user = getAuthenticatedUser();
        if (user == null) user = (String) get("username");
        return new RequestableInvocation.Billing(remoteAddr, user == null ? "user" : user,
                projectName, "", "", sequenceName, requestedObject.getClass().getSimpleName(),
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                requestedObject.getScore(), httpSession.getId(), userAgent,
                (String) httpSession.getAttribute(Parameter.UIid.name()),
                (String) httpSession.getAttribute(Parameter.DeviceUUID.name()));
    }
    @Override public void close() throws Exception {
        if (closed) return;
        closed = true;
        try { RequestableInvocation.billChild(this::billing); }
        finally {
            try { if (scope != null) scope.close(); }
            finally { previousLog.install(); Thread.currentThread().setContextClassLoader(previousLoader); }
        }
    }
}
