/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.engine.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

/** Registry of administrator-owned debug sessions; no context factory listeners or work when detached. */
public final class JavascriptDebugger {
	public static final String PARAMETER = "__javascriptDebug";
	private static final String CLAIM = JavascriptDebugger.class.getName();
	private static final Map<String, JavascriptDebugSession> sessions = new HashMap<>();
	private static ScheduledExecutorService expiry;
	private static volatile boolean active;
	private JavascriptDebugger() { }

	public static boolean enabled() {
		return Engine.isStudioMode() || Engine.isWebStudioAllowed()
				&& EnginePropertiesManager.getPropertyAsBoolean(PropertyName.JAVASCRIPT_DEBUGGER);
	}
	public static boolean active() { return active; }
	public static long pauseTimeoutMillis() {
		try {
			long seconds = Long.parseLong(EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_DEBUGGER_PAUSE_TIMEOUT));
			return Math.max(1, Math.min(3600, seconds)) * 1000;
		} catch (RuntimeException e) { return 120_000; }
	}
	/** Refuse shared/prepared code before accessing caches; expose a value-free reason to the UI. */
	public static void requireUnsharedRuntime(org.mozilla.javascript.Context cx) {
		if (cx != null && cx.getDebugger() instanceof JavascriptDebugSession session) {
			session.unsupportedRuntime();
			throw new IllegalStateException(JavascriptDebugSession.UNSUPPORTED_RUNTIME);
		}
	}
	public static synchronized JavascriptDebugSession session(String owner, boolean create) {
		if (owner == null || owner.isEmpty()) throw new IllegalArgumentException("Administrator session required.");
		reap();
		var session = sessions.get(owner);
		if (session == null && create) {
			if (!enabled()) throw new IllegalStateException("JavaScript debugging is disabled on this server.");
			if (!active && RhinoUtils.debugMode) throw new IllegalStateException("Another JavaScript debugger is already attached.");
			if (sessions.size() >= 16) throw new IllegalStateException("Too many debug sessions.");
			session = new JavascriptDebugSession(); sessions.put(owner, session);
			active = true;
			if (expiry == null) {
				expiry = Executors.newSingleThreadScheduledExecutor(runnable -> {
					var thread = new Thread(runnable, "JavascriptDebugger-expiry");
					thread.setDaemon(true); thread.setContextClassLoader(JavascriptDebugger.class.getClassLoader()); return thread;
				});
				expiry.scheduleWithFixedDelay(JavascriptDebugger::reap, 1, 1, TimeUnit.SECONDS);
			}
		}
		if (session != null) session.touch();
		return session;
	}
	public static synchronized void stop(String owner) {
		var session = sessions.remove(owner); if (session != null) session.close();
		stopWhenEmpty();
	}
	private static synchronized void reap() {
		long now = System.currentTimeMillis();
		sessions.values().removeIf(session -> { if (!enabled() || session.expired(now)) { session.close(); return true; } return false; });
		stopWhenEmpty();
	}
	private static void stopWhenEmpty() {
		if (active && sessions.isEmpty()) {
			active = false;
			if (expiry != null) { expiry.shutdown(); expiry = null; }
		}
	}

	/** Capture only at explicit child scheduling boundaries, never by inheritable thread state. */
	public static JavascriptDebugSession currentSession() {
		if (!active) return null;
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		return cx != null && cx.getDebugger() instanceof JavascriptDebugSession session ? session : null;
	}
	/** Bind only an explicitly marked, authenticated request; nested requestables keep the root claim. */
	public static boolean selects(HttpServletRequest request) { return selectedSession(request) != null; }
	private static JavascriptDebugSession selectedSession(HttpServletRequest request) {
		if (!active || request == null || !enabled()) return null;
		var httpSession = request.getSession(false);
		if (httpSession == null || !Engine.authenticatedSessionManager.hasRole(httpSession, Role.WEB_ADMIN)) return null;
		String id = request.getParameter(PARAMETER);
		if (id == null || id.isEmpty()) return null;
		var session = session(httpSession.getId(), false);
		if (session == null || !session.id().equals(id)) throw new IllegalArgumentException("Unknown debug session for this administrator.");
		return session;
	}
	public static AutoCloseable bind(HttpServletRequest request) {
		var session = selectedSession(request);
		if (session == null) return null;
		synchronized (request) {
			if (request.getAttribute(CLAIM) == session) {
				var cx = org.mozilla.javascript.Context.getCurrentContext();
				return cx != null && cx.getDebugger() == session ? null : session.enterChild();
			}
			request.setAttribute(CLAIM, session);
		}
		try {
			var lease = session.enter();
			return () -> { try { lease.close(); } finally { request.removeAttribute(CLAIM); } };
		} catch (RuntimeException | Error e) { request.removeAttribute(CLAIM); throw e; }
	}
}
