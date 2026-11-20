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

/** Registry of administrator-owned debug sessions; no context factory listeners or work when detached. */
public final class JavascriptDebugger {
	public static final String PARAMETER = "__javascriptDebug";
	private static final String CLAIM = JavascriptDebugger.class.getName();
	private static final Map<String, JavascriptDebugSession> sessions = new HashMap<>();
	private static ScheduledExecutorService expiry;
	private static volatile boolean active;
	/** Cached by compiled providers: the detached block path reads this field only. */
	public static final class Signal extends org.mozilla.javascript.ScriptableObject {
		private static final long serialVersionUID = 1L;
		public volatile boolean active;
		private Signal() { sealObject(); }
		@Override public String getClassName() { return "DebuggerSignal"; }
		@Override public Object get(String name, org.mozilla.javascript.Scriptable start) {
			return "active".equals(name) ? active : NOT_FOUND;
		}
		@Override public boolean has(String name, org.mozilla.javascript.Scriptable start) { return "active".equals(name); }
		@Override public Object[] getIds() { return new Object[] { "active" }; }
	}
	public static final Signal signal = new Signal();
	private static final Map<String, Execution> executions = new java.util.LinkedHashMap<>();
	private static final ThreadLocal<Execution> execution = new ThreadLocal<>();
	private static final ThreadLocal<String> participantEditorOwner = new ThreadLocal<>();
	private static final class Execution {
		final String id = java.util.UUID.randomUUID().toString(), owner, label;
		final Thread thread = Thread.currentThread();
		volatile JavascriptDebugSession session;
		Object worker;
		AutoCloseable lease, cancelWake;
		boolean construction;
		com.twinsoft.convertigo.beans.core.RequestableObject.InvocationGuard guard = com.twinsoft.convertigo.beans.core.RequestableObject.currentInvocationGuard();
		Execution(String owner, String label, JavascriptDebugSession session) {
			this.owner = owner; this.label = label; this.session = session;
		}
	}
	/** Versioned, additive participant contract; no provider-specific concepts cross this boundary. */
	public static org.codehaus.jettison.json.JSONObject participantContract() {
		return JavascriptDebugSession.json("version", 2, "capabilities", JavascriptDebugSession.json(
				"explicitParents", true, "workerStepping", true, "singlePause", true, "cancellation", true,
				"activationMetadata", true, "ancestorScopes", false, "waitBudgets", true, "cancellationNotifications", true, "boundedInspection", true, "propertyCursors", true, "parsedPathEvaluation", true));
	}
	public static final class ParentLink {
		private final JavascriptDebugSession session;
		private final JavascriptDebugSession.Parent parent;
		private final String owner, label;
		private final boolean construction;
		private final com.twinsoft.convertigo.beans.core.RequestableObject.InvocationGuard guard;
		private ParentLink(Execution current, JavascriptDebugSession.Parent parent, boolean construction) {
			this.session = current.session; this.parent = parent; this.owner = current.owner; this.label = current.label;
			this.construction = construction; this.guard = current.guard;
		}
	}
	/** Capture on the caller, before handing work to another thread. No inheritable thread-local state. */
	public static ParentLink captureParent() {
		if (!signal.active) return null;
		var current = execution.get();
		if (current == null || current.session == null || current.session.expired(System.currentTimeMillis())) return null;
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		Object worker = current.worker;
		if (worker == null && cx != null && cx.getDebugger() == current.session) worker = cx.getDebuggerContextData();
		if (worker == null && cx != null && cx.getThreadLocal(PROVIDER_BINDING) instanceof ProviderBinding binding) worker = binding.worker;
		if (worker == null) return null;
		return new ParentLink(current, current.session.parent(worker), current.construction || cx != null && cx.getThreadLocal(PROVIDER_CONSTRUCTION) != null);
	}
	/** Bind the captured parent only for this child lifetime; pooled threads are clean after close. */
	public static AutoCloseable bindChild(ParentLink link) { return bindChildWithBudget(link, null); }
    /** An optional wait-domain owns the budget; parent identity is independent of that wait. */
    public static AutoCloseable bindChildWithBudget(ParentLink link, java.util.function.Supplier<DebugSuspension> budget) {
		if (link == null || execution.get() != null) return null;
		var current = new Execution(link.owner, link.label, link.construction ? null : link.session);
		current.construction = link.construction;
		if (!current.guard.isBound()) current.guard = link.guard;
		if (link.parent == null) current.session = null;
		if (current.session != null && link.parent != null) {
			current.worker = current.session.joinChild(link.parent,
					() -> current.guard.isBound() && !current.guard.isRunning() || link.guard.isBound() && !link.guard.isRunning(),
					budget != null ? budget : () -> {
						var local = com.twinsoft.convertigo.beans.core.RequestableObject.pauseCurrentRequestable();
						var caller = link.guard.pauseDebugger();
						return () -> { try { if (local != null) local.close(); } finally { if (caller != null) caller.close(); } };
					});
			if (current.worker == null) current.session = null;
            else current.cancelWake = current.guard.onCancellation(current.session::cancellationChanged);
		}
		execution.set(current);
		return () -> { try { if (current.cancelWake != null) current.cancelWake.close(); } finally { try { if (current.worker != null) link.session.finishParticipantWorker(); } finally { execution.remove(); } } };
	}
	private static final String STUDIO_REQUEST = CLAIM + ".studio";
	private static volatile JavascriptDebugSession studioSession;
	private JavascriptDebugger() { }

	public static boolean enabled() {
		return Engine.isStudioMode() || EnginePropertiesManager.getPropertyAsBoolean(PropertyName.JAVASCRIPT_DEBUGGER);
	}
	/** Editing-capable Studios only; production servers can expose participants without Rhino. */
	public static boolean rhinoEnabled() { return enabled() && Engine.isWebStudioAllowed(); }
	/** Authoritative mode, independent of request parameters. */
	public static boolean freeEvaluation() { return Engine.isWebStudioAllowed(); }
	public static boolean active() { return active; }
	public static long pauseTimeoutMillis() {
		try {
			long seconds = Long.parseLong(EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_DEBUGGER_PAUSE_TIMEOUT));
			return Math.max(1, Math.min(3600, seconds)) * 1000;
		} catch (RuntimeException e) { return 120_000; }
	}
	/** A scoped suspension leaves the request worker and its legacy frames intact. */
	public interface DebugSuspension extends AutoCloseable {
		@Override void close();
	}
	private static final Object PROVIDER_BINDING = new Object();
	private static final Object PROVIDER_CONSTRUCTION = new Object();
	private record ProviderBinding(JavascriptDebugSession session, Object worker) { }
	public static DebugSuspension suspendForFlow() {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		if (cx == null || !(cx.getDebugger() instanceof JavascriptDebugSession session)) return null;
		var data = cx.getDebuggerContextData();
		var previous = cx.getThreadLocal(PROVIDER_BINDING);
		var interpreted = cx.isInterpretedMode();
		var generating = cx.isGeneratingDebug();
		if (previous == null) session.flowWithoutDebugging();
		cx.putThreadLocal(PROVIDER_BINDING, new ProviderBinding(session, data));
		cx.setDebugger(null, null);
		cx.setGeneratingDebug(false);
		cx.setInterpretedMode(false);
		return () -> {
			if (previous == null) cx.removeThreadLocal(PROVIDER_BINDING); else cx.putThreadLocal(PROVIDER_BINDING, previous);
			cx.setInterpretedMode(interpreted);
			cx.setGeneratingDebug(generating);
			cx.setDebugger(session, data);
		};
	}
	/** Shared builders cannot attach a private implementation, even through nested callbacks. */
	public static DebugSuspension providerConstruction() {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		if (cx == null) return null;
		var previous = cx.getThreadLocal(PROVIDER_CONSTRUCTION);
		var suspension = suspendForFlow();
		cx.putThreadLocal(PROVIDER_CONSTRUCTION, Boolean.TRUE);
		return () -> {
			if (previous == null) cx.removeThreadLocal(PROVIDER_CONSTRUCTION); else cx.putThreadLocal(PROVIDER_CONSTRUCTION, previous);
			if (suspension != null) suspension.close();
		};
	}
	public static boolean providerDebugging() {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		return cx != null && cx.getThreadLocal(PROVIDER_BINDING) instanceof ProviderBinding binding
				&& cx.getThreadLocal(PROVIDER_CONSTRUCTION) == null && !binding.session.expired(System.currentTimeMillis());
	}
	/** Compile and initialize invocation-owned code, never a cache entry. Initializers cannot pause. */
	public static org.mozilla.javascript.Scriptable compileProviderImplementation(String source, String url,
			org.mozilla.javascript.Scriptable scope) {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		var binding = cx == null ? null : (ProviderBinding) cx.getThreadLocal(PROVIDER_BINDING);
		if (binding == null || cx.getThreadLocal(PROVIDER_CONSTRUCTION) != null)
			throw new IllegalStateException("Private code cannot be compiled during shared preparation.");
		var debugger = cx.getDebugger(); var data = cx.getDebuggerContextData();
		boolean interpreted = cx.isInterpretedMode(), generating = cx.isGeneratingDebug();
		try {
			cx.setInterpretedMode(true); cx.setGeneratingDebug(true);
			cx.setDebugger(binding.session, binding.worker);
			binding.session.registerProviderSource(url);
			binding.session.privateBoundary(binding.worker, url, scope);
			var script = cx.compileString(com.twinsoft.convertigo.engine.util.CopilotHelper.addInstruction(null, source), url, 1, null);
			cx.setDebugger(null, null);
			try (var construction = providerConstruction()) {
				return (org.mozilla.javascript.Scriptable) script.exec(cx, scope);
			}
		} finally {
			cx.setDebugger(debugger, data); cx.setInterpretedMode(interpreted); cx.setGeneratingDebug(generating);
		}
	}
	/** Reattach only around a private function; parent provider globals stay outside inspected scopes. */
	public static Object callProviderFunction(org.mozilla.javascript.Function function, org.mozilla.javascript.Scriptable self,
			Object[] args, org.mozilla.javascript.Scriptable boundary) {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		var binding = (ProviderBinding) cx.getThreadLocal(PROVIDER_BINDING);
		if (binding == null || cx.getThreadLocal(PROVIDER_CONSTRUCTION) != null)
			throw new IllegalStateException("Private code cannot execute during shared preparation.");
		var debugger = cx.getDebugger(); var data = cx.getDebuggerContextData();
		boolean interpreted = cx.isInterpretedMode(), generating = cx.isGeneratingDebug();
		try (var scopes = binding.session.providerScopes(binding.worker, boundary)) {
			cx.setInterpretedMode(true); cx.setGeneratingDebug(true); cx.setDebugger(binding.session, binding.worker);
			binding.session.providerDebuggable();
			return function.call(cx, boundary, self == null ? boundary : self, args);
		} finally {
			cx.setDebugger(debugger, data); cx.setInterpretedMode(interpreted); cx.setGeneratingDebug(generating);
		}
	}
	/** Bounded scalar display for plugin inspections, without invoking arbitrary host toString methods. */
	public static String preview(Object value) { return JavascriptDebugSession.preview(value); }
	/** Editor metadata can update only its authenticated administrator's session; it never joins a worker. */
	public static DebugSuspension participantEditor(HttpServletRequest request) {
		var previous = participantEditorOwner.get();
		var httpSession = active && request != null ? request.getSession(false) : null;
		if (httpSession != null && Engine.authenticatedSessionManager.hasRole(httpSession, Role.WEB_ADMIN))
			participantEditorOwner.set(httpSession.getId());
		else participantEditorOwner.remove();
		return () -> { if (previous == null) participantEditorOwner.remove(); else participantEditorOwner.set(previous); };
	}
	/** Synchronize opaque participant identities for this administrator's selected request or Studio view. */
	public static void participantSources(String kind, String manifest) throws Exception {
		if (!participantAllowed()) return;
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		JavascriptDebugSession session = null;
		if (cx.getThreadLocal(PROVIDER_BINDING) instanceof ProviderBinding binding) session = binding.session;
		var current = execution.get();
		if (session == null && current != null) session = current.session != null ? current.session : current.owner.isEmpty() ? null : session(current.owner,false);
		if (session == null && participantEditorOwner.get() != null) session = session(participantEditorOwner.get(),false);
		if (session == null) session = studioSession();
		if (session != null && !session.expired(System.currentTimeMillis())) session.participantSources(kind, manifest);
	}
	/** Register an invocation-owned plugin. The transport knows no provider model or values. */
	public static JavascriptDebugSession.Participant participant(String kind, org.mozilla.javascript.Function inspect) {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		if (cx == null || cx.getThreadLocal(PROVIDER_CONSTRUCTION) != null) return null;
		if (cx.getThreadLocal(PROVIDER_BINDING) instanceof ProviderBinding binding)
			return binding.session.expired(System.currentTimeMillis()) ? null : binding.session.participant(kind, binding.worker, inspect);
		var current = execution.get();
		if (current == null || current.session == null || current.session.expired(System.currentTimeMillis())) return null;
		if (current.lease == null) {
			current.worker = current.session.joinParticipantWorker();
			current.lease = current.session::finishParticipantWorker;
		}
		return current.session.participant(kind, current.worker, inspect);
	}
	static boolean participantAllowed() {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		return cx != null && cx.getThreadLocal(PROVIDER_CONSTRUCTION) == null;
	}
	public static boolean participantSelected() {
		if (!participantAllowed()) return false;
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		var binding = cx.getThreadLocal(PROVIDER_BINDING);
		if (binding instanceof ProviderBinding selected) return !selected.session.expired(System.currentTimeMillis());
		var current = execution.get();
		return current != null && current.session != null && !current.session.expired(System.currentTimeMillis());
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
			if (sessions.size() >= 16) throw new IllegalStateException("Too many debug sessions.");
			session = new JavascriptDebugSession(); sessions.put(owner, session);
			active = signal.active = true;
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
		if (studioSession != null && !sessions.containsValue(studioSession)) studioSession = null;
		if (active && sessions.isEmpty()) {
			active = signal.active = false;
			if (expiry != null) { expiry.shutdown(); expiry = null; }
		}
	}

	/** One administrator explicitly owns the local Eclipse launch bridge. Never available on servers. */
	public static synchronized void studioRequests(String owner, boolean value) {
		if (!Engine.isStudioMode()) throw new IllegalStateException("Eclipse request debugging requires Studio mode.");
		var session = session(owner, false);
		if (session == null) throw new IllegalStateException("Start the debugger first.");
		if (value && studioSession != null && studioSession != session)
			throw new IllegalStateException("Another administrator is already debugging Eclipse requests.");
		if (value) studioSession = session;
		else if (studioSession == session) studioSession = null;
		session.settingsChanged();
	}
	public static JavascriptDebugSession studioSession() {
		return Engine.isStudioMode() ? studioSession : null;
	}
	public static synchronized void stopStudioSession() {
		var session = studioSession();
		if (session != null) {
			sessions.values().removeIf(candidate -> candidate == session);
			session.close();
			stopWhenEmpty();
		}
	}
	/** Called only by Eclipse's launch boundary, not by InternalRequester or an HTTP parameter. */
	public static void selectStudioRequest(com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest request) {
		var session = studioSession();
		if (session != null) request.setAttribute(STUDIO_REQUEST, session);
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
		var studio = studioSession();
		if (studio != null && request instanceof com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest
				&& request.getAttribute(STUDIO_REQUEST) == studio) return studio;
		var httpSession = request.getSession(false);
		if (httpSession == null || !Engine.authenticatedSessionManager.hasRole(httpSession, Role.WEB_ADMIN)) return null;
		String id = request.getParameter(PARAMETER);
		if (id == null || id.isEmpty()) return null;
		var session = session(httpSession.getId(), false);
		if (session == null || !session.id().equals(id)) throw new IllegalArgumentException("Unknown debug session for this administrator.");
		return session;
	}
	public static AutoCloseable bind(HttpServletRequest request) { return bind(request, "Request"); }
	/** Register administrator-owned work even before Start, so attachment can happen at a later boundary. */
	public static AutoCloseable bind(HttpServletRequest request, String label) {
		if (request == null || !enabled()) return null;
		var session = selectedSession(request);
		var parent = execution.get();
		if (parent != null) return null;
		var httpSession = request.getSession(false);
		boolean administrator = httpSession != null && Engine.authenticatedSessionManager.hasRole(httpSession, Role.WEB_ADMIN);
		if (session == null && !administrator) return null;
		var current = new Execution(administrator ? httpSession.getId() : "", label, session);
		synchronized (JavascriptDebugger.class) {
			if (executions.size() >= 128) {
				if (session != null) throw new IllegalStateException("Too many debugger request targets.");
				return null;
			}
			executions.put(current.id, current);
		}
		execution.set(current);
		try {
			if (session != null && rhinoEnabled() && session.hasRhinoBreakpoints()) current.lease = bindRhino(request, session);
			else if (session != null) { current.worker = session.joinParticipantWorker(); current.lease = session::finishParticipantWorker; }
			if (session != null) {
				if (current.worker == null) current.worker = org.mozilla.javascript.Context.getCurrentContext().getDebuggerContextData();
				session.identifyRoot(current.worker, current.id, () -> current.guard.isBound() && !current.guard.isRunning());
            current.cancelWake = current.guard.onCancellation(session::cancellationChanged);
			}
		} catch (RuntimeException | Error e) { execution.remove(); synchronized (JavascriptDebugger.class) { executions.remove(current.id); } throw e; }
		return () -> {
			// Remove under the same monitor as attach before reading the lease: finishing cannot race a reservation.
			synchronized (JavascriptDebugger.class) { executions.remove(current.id); }
			try { if (current.lease != null) current.lease.close(); }
			finally { try { if (current.cancelWake != null) current.cancelWake.close(); } finally { execution.remove(); } }
		};
	}
	public static synchronized org.codehaus.jettison.json.JSONArray targets(String owner) {
		var result = new org.codehaus.jettison.json.JSONArray();
		for (var current : executions.values()) if (current.owner.equals(owner))
			result.put(JavascriptDebugSession.json("id", current.id, "label", current.label, "selected", current.session != null));
		return result;
	}
	public static synchronized void attach(String owner, String target) {
		var session = session(owner, false);
		if (session == null) throw new IllegalStateException("Start the debugger first.");
		var current = executions.get(target);
		if (current == null || !current.owner.equals(owner)) throw new IllegalArgumentException("Request target unavailable.");
		if (current.session != null) throw new IllegalStateException("This request is already selected.");
		// Interpreted compilation cannot be retrofitted into running code. Late attachment is participant-only.
		if (session.hasRhinoBreakpoints()) throw new IllegalStateException("Remove script breakpoints before attaching to a running request.");
		for (var other : executions.values()) if (other.session == session) throw new IllegalStateException("Another request is already selected.");
		current.worker = session.joinParticipantWorker(current.thread);
		current.lease = session::finishParticipantWorker;
		current.session = session;
		session.identifyRoot(current.worker, current.id, () -> current.guard.isBound() && !current.guard.isRunning());
            current.cancelWake = current.guard.onCancellation(session::cancellationChanged);
		session.settingsChanged();
	}
	private static AutoCloseable bindRhino(HttpServletRequest request, JavascriptDebugSession session) {
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
