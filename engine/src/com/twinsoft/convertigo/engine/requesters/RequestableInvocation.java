/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.requesters;

import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import jakarta.servlet.http.HttpSession;

/** Policy shared by managed requesters and lightweight internal invocations.
 * Internal calls deliberately retain InternalRequester's accessibility/transport
 * policy; they must not silently acquire ServletRequester's external policy.
 */
public final class RequestableInvocation {
    private RequestableInvocation() { }
    private static final java.util.concurrent.atomic.LongAdder direct = new java.util.concurrent.atomic.LongAdder();
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.LongAdder> fallback = new java.util.concurrent.ConcurrentHashMap<>();
    public static void directCall() { direct.increment(); }
    public static void fallback(String reason) { fallback.computeIfAbsent(reason, ignored -> new java.util.concurrent.atomic.LongAdder()).increment(); }
    public static java.util.Map<String, Object> statistics() {
        var reasons = new java.util.TreeMap<String, Long>();
        fallback.forEach((key, count) -> reasons.put(key, count.sum()));
        return java.util.Map.of("direct", direct.sum(), "fallback", reasons.values().stream().mapToLong(Long::longValue).sum(), "reasons", reasons);
    }

    @FunctionalInterface public interface Check { void run() throws EngineException; }

    public static void authorize(Check accessibility, Check securedConnection, Check authentication, Check parent)
            throws EngineException {
        accessibility.run();
        securedConnection.run();
        authentication.run();
        parent.run();
    }

    public static void authorize(Requester requester) throws EngineException {
        authorize(requester::checkAccessibility, requester::checkSecuredConnection,
                requester::checkAuthenticatedContext, requester::checkParentContext);
    }

    /** Only an already-authorized Studio/test-platform stub may bypass authentication. */
    public static void authentication(RequestableObject target, HttpSession session, boolean trustedStub) throws EngineException {
        if (!trustedStub && SessionAttribute.authenticatedUser.string(session) == null && target.getAuthenticatedContextRequired())
            throw new EngineException("Authentication required");
    }
    public static void authentication(boolean required, HttpSession session, boolean trustedStub) throws EngineException {
        if (!trustedStub && SessionAttribute.authenticatedUser.string(session) == null && required)
            throw new EngineException("Authentication required");
    }

    public static void parent(boolean running) throws EngineException {
        if (!running) throw new EngineException("Parent context stopped");
    }

    /** No Context/session is retained by the asynchronous billing queue. */
    public record Billing(String clientIp, String userName, String projectName, String connectorName,
            String connectorType, String requestableName, String requestableType, long responseTime,
            long score, String sessionId, String userAgent, String uiId, String deviceUuid) { }

    public static void bill(com.twinsoft.convertigo.engine.Context context) {
        if (context.requestedObject == null) return;
        try { Engine.theApp.billingManager.insertBilling(context); }
        catch (Exception e) { billingFailure(e); }
    }

    public static void bill(Billing value) {
        try { Engine.theApp.billingManager.insertBilling(value); }
        catch (Exception e) { billingFailure(e); }
    }

    public static void billChild(java.util.concurrent.Callable<Billing> value) {
        try { Engine.theApp.billingManager.insertBilling(value.call()); }
        catch (Exception e) { billingFailure(e); }
    }

    private static void billingFailure(Exception error) {
        // Match the legacy failure policy: billing cannot replace the business outcome.
        Engine.logContext.warn("Unable to insert billing ticket (the billing is thus ignored): ["
                + error.getClass().getName() + "] " + error.getMessage());
    }
}
