/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** A revision owns prepared code and metadata; an invocation owns all execution state.
 * No request, session or value produced by a block belongs in the revision registry.
 * Prepared values are published once, after the provider has frozen them. A replaced
 * revision stays usable by its in-flight callers and is reclaimed after the last call.
 */
public final class FlowRuntimeApi {
    private final ConcurrentHashMap<String, Object> prepared = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> dynamic = new ConcurrentHashMap<>();
    private final Object preparation = new Object();
    private final ThreadLocal<Frame> current = new ThreadLocal<>();
    private final LongAdder hits = new LongAdder(), builds = new LongAdder(), calls = new LongAdder();
    private final RoutingScope shared;
    private final long codeRevision = FlowEngineBridge.cacheGeneration(), dataRevision = FlowEngineBridge.dataGeneration();
    private final Object objectConstructor, arrayConstructor;
    private volatile long nextSourceProbe;
    private String sourceRevision;


    public FlowRuntimeApi(Context cx) {
        shared = new RoutingScope();
        new org.mozilla.javascript.lc.type.impl.factory.WeakReferenceFactory().associate(shared);
        FlowPackages.initializeEngineScope(cx, shared);
        objectConstructor = ScriptableObject.getProperty(shared, "Object");
        arrayConstructor = ScriptableObject.getProperty(shared, "Array");
    }
    public ScriptableObject sharedScope() { return shared; }

    public boolean canonicalData(Object value) {
        return com.twinsoft.convertigo.engine.util.SanitizedJson.canonicalReference(value, Context.getCurrentContext(), shared);
    }
    public Object childResult(Object value, org.mozilla.javascript.Callable policy) throws org.codehaus.jettison.json.JSONException {
        return com.twinsoft.convertigo.engine.util.SanitizedJson.reference(value, Context.getCurrentContext(), shared, policy);
    }

    public String childEligibility(com.twinsoft.convertigo.beans.core.RequestableObject target, Scriptable input) {
        var frame = current.get();
        Object parent = frame == null ? null : ScriptableObject.getProperty(frame.scope, "context");
        if (parent instanceof org.mozilla.javascript.Wrapper wrapper) parent = wrapper.unwrap();
        return childFallback(target, input, parent);
    }
    public void rejectChild(String reason) { com.twinsoft.convertigo.engine.requesters.RequestableInvocation.fallback(reason); }

    /** Acquire only the bounded internal JSON contract. A null result means no
     * user code ran and the caller must keep its managed requester path. */
    public ChildInvocation openChild(com.twinsoft.convertigo.beans.core.RequestableObject target, Scriptable input, boolean authenticationRequired, long startedNanos) throws Exception {
        var frame = current.get();
        Object parent = frame == null ? null : ScriptableObject.getProperty(frame.scope, "context");
        if (parent instanceof org.mozilla.javascript.Wrapper wrapper) parent = wrapper.unwrap();
        String reason = childFallback(target, input, parent);
        if (reason != null) {
            com.twinsoft.convertigo.engine.requesters.RequestableInvocation.fallback(reason);
            return null;
        }
        var flow = (com.twinsoft.convertigo.beans.flow.Flow) target;
        var guard = com.twinsoft.convertigo.beans.core.RequestableObject.currentInvocationGuard();
        var session = parent instanceof com.twinsoft.convertigo.engine.Context c ? c.httpSession : ((ChildInvocation)parent).httpSession;
        try {
            com.twinsoft.convertigo.engine.requesters.RequestableInvocation.authorize(() -> {}, () -> {},
                () -> com.twinsoft.convertigo.engine.requesters.RequestableInvocation.authentication(authenticationRequired, session, false),
                () -> {});
        } catch (com.twinsoft.convertigo.engine.EngineException refusal) {
            // The managed requester owns historical error disclosure/XML policies.
            // No child has started and no ticket has been emitted: delegate once.
            rejectChild("policy-refusal");
            return null;
        }
        var child = new ChildInvocation(flow, parent, guard, authenticationRequired, startedNanos);
        try {
            // Never retry a cancelled parent through a new managed worker. This
            // failed invocation bills once and restores its local state below.
            com.twinsoft.convertigo.engine.requesters.RequestableInvocation.parent(child.isRunning());
            var cx = Context.getCurrentContext();
            var nested = enter(cx);
            child.attach(nested);
            for (Object id : frame.scope.getIds()) {
                if (id instanceof String name && name.startsWith("__flow") && !name.equals("__flowExecutionState"))
                    nested.scope.put(name, nested.scope, frame.scope.get(name, frame.scope));
            }
            FlowPackages.initializeChildScope(nested.scope, shared);
            FlowPackages.bind(cx, nested.scope, flow.getProject().getProjectClassLoader());
            nested.scope.put("context", nested.scope, Context.javaToJS(child, nested.scope));
            com.twinsoft.convertigo.engine.requesters.RequestableInvocation.directCall();
            return child;
        } catch (Exception error) { child.close(); throw error; }
    }

    /** Up to requested lanes running body(lanes, index) on threads of the shared pool (see ParallelLanes). A lane has
     * its own Rhino context and frame: the bindings of the calling frame, its project packages and the project directory
     * of its state, but no Convertigo context, which belongs to the calling thread. Null outside a frame or when no
     * thread is free: the caller then runs the tasks itself. */
    public ParallelLanes openLanes(int requested, org.mozilla.javascript.Function body) {
        var frame = current.get();
        if (frame == null || requested < 1 || body == null) return null;
        var cx = Context.getCurrentContext();
        var bindings = new java.util.LinkedHashMap<String, Object>();
        for (Object id : frame.scope.getIds()) {
            if (id instanceof String name && name.startsWith("__flow") && !name.equals("__flowExecutionState"))
                bindings.put(name, frame.scope.get(name, frame.scope));
        }
        var inherited = new java.util.LinkedHashMap<String, Object>();
        if (frame.state != null) for (var name : new String[] { "projectDirOverride", "activeRequestFallback" }) {
            var value = ScriptableObject.getProperty(frame.state, name);
            if (value != Scriptable.NOT_FOUND && value != null && !org.mozilla.javascript.Undefined.isUndefined(value)) inherited.put(name, value);
        }
        var generation = FlowEngineBridge.currentProjectGeneration();
        int languageVersion = cx.getLanguageVersion();
        boolean interpreted = cx.isInterpretedMode();
        var wrapFactory = cx.getWrapFactory();
        var applicationLoader = cx.getApplicationClassLoader();
        var threadLoader = Thread.currentThread().getContextClassLoader();
        return ParallelLanes.open(Math.min(requested, 64), (lanes, index) -> {
            var thread = Thread.currentThread();
            var previousLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(threadLoader);
            try (var laneCx = Context.enter()) {
                laneCx.setLanguageVersion(languageVersion);
                laneCx.setInterpretedMode(interpreted);
                if (wrapFactory != null) laneCx.setWrapFactory(wrapFactory);
                if (applicationLoader != null) laneCx.setApplicationClassLoader(applicationLoader);
                try (var lane = enter(laneCx)) {
                    bindings.forEach((name, value) -> lane.scope.put(name, lane.scope, value));
                    FlowPackages.initializeChildScope(lane.scope, shared);
                    FlowPackages.bind(laneCx, lane.scope, generation);
                    var state = state();
                    inherited.forEach((name, value) -> state.put(name, state, value));
                    body.call(laneCx, lane.scope, lane.scope, new Object[] { Context.javaToJS(lanes, lane.scope), index });
                }
            } finally {
                thread.setContextClassLoader(previousLoader);
            }
        });
    }

    private String childFallback(com.twinsoft.convertigo.beans.core.RequestableObject target, Scriptable input, Object parent) {
        if (com.twinsoft.convertigo.engine.Engine.isStudioMode() || !Boolean.getBoolean("convertigo.javascript.stateless")
                || "false".equals(System.getProperty("convertigo.javascript.direct_calls"))) return "disabled";
        if (codeRevision != FlowEngineBridge.cacheGeneration() || dataRevision != FlowEngineBridge.dataGeneration()) return "revision";
        if (target == null || target.getClass() != com.twinsoft.convertigo.beans.flow.Flow.class) return "requestable-kind";
        com.twinsoft.convertigo.beans.core.Project project;
        jakarta.servlet.http.HttpSession session;
        jakarta.servlet.http.HttpServletRequest request;
        if (parent instanceof com.twinsoft.convertigo.engine.Context context) {
            if (context.isAsync || context.isStubRequested || context.isXsltRequest || context.removeNamespaces
                    || context.isRequestFromVic || context.isTrustedRequest) return "parent-mode";
            project = context.project; session = context.httpSession; request = context.httpServletRequest;
        } else if (parent instanceof ChildInvocation child) {
            project = child.project; session = child.httpSession; request = child.httpServletRequest;
        } else return "no-invocation";
        // a request runs on its copy of the project: the target must belong to the project loaded when it was copied
        if (project == null || target.getProject() != project.getOriginal()) return "project-generation";
        if (session == null || request == null || request.getSession(false) != session) return "session";
        if (com.twinsoft.convertigo.engine.Engine.theApp.hasRequestableListeners()) return "lifecycle-listeners";
        var plugins = com.twinsoft.convertigo.engine.Engine.theApp.pluginsManager;
        if (plugins != null && plugins.hasPlugins()) return "lifecycle-plugins";
        if (session.getAttribute("customController") != null) return "custom-controller";
        if ("true".equals(com.twinsoft.convertigo.engine.enums.RequestAttribute.system.string(request))) return "system";
        if (((com.twinsoft.convertigo.beans.flow.Flow)target).isIncludeTrace()) return "trace";
        if (target.isBillable() || !target.getResponseExpiryDate().isEmpty() || target.getAddStatistics()) return "legacy-response";
        for (Object id : input.getIds()) {
            if (!(id instanceof String name) || name.startsWith("__") || name.startsWith("convertigo.")) return "reserved-input";
            var variable = ((com.twinsoft.convertigo.beans.flow.Flow)target).getVariable(name);
            if (variable != null && variable.isMultiValued()) return "multi-valued-input";
        }
        if (!com.twinsoft.convertigo.beans.core.RequestableObject.currentInvocationGuard().coversTimeout(target.getResponseTimeout()))
            return "independent-deadline";
        return null;
    }

    /** The values compiled from the global symbols stay valid while this generation does not change. */
    public long symbolsGeneration() {
        var app = com.twinsoft.convertigo.engine.Engine.theApp;
        return app == null || app.databaseObjectsManager == null ? 0 : app.databaseObjectsManager.getSymbolsGeneration();
    }

    /** Typed dispatch avoids Rhino's lazily mutated explicit-overload member table. */
    public String compileSymbols(String text) throws com.twinsoft.convertigo.engine.UndefinedSymbolsException {
        var app = com.twinsoft.convertigo.engine.Engine.theApp;
        return app == null || app.databaseObjectsManager == null ? text : app.databaseObjectsManager.getCompiledValue(text);
    }

    /** Notifications invalidate immediately; this is the fallback for external file edits. */
    boolean revisionMatches(java.util.function.Supplier<String> fingerprint) {
        long now = System.nanoTime();
        if (now < nextSourceProbe) return true;
        synchronized (preparation) {
            if (now < nextSourceProbe) return true;
            String revision = fingerprint.get();
            if (sourceRevision != null && !sourceRevision.equals(revision)) return false;
            sourceRevision = revision;
            nextSourceProbe = now + java.util.concurrent.TimeUnit.SECONDS.toNanos(60);
            return true;
        }
    }

    /** Single-flight compilation, reentrant for module/contract dependencies. Never locks execution. */
    public Object prepare(String key, Callable<Object> builder) throws Exception {
        // Eviction of a dynamic plan must never recreate a module or its environment
        // while existing plans still refer to the original revision's services.
        var registry = key.startsWith("plan:") || key.startsWith("script:") ? dynamic : prepared;
        Object value = registry.get(key);
        if (value != null) { hits.increment(); return value; }
        synchronized (preparation) {
            value = registry.get(key);
            if (value == null) {
                value = builder.call();
                if (value == null) throw new IllegalStateException("A prepared value must be non-null");
                // Bound dynamic expressions without invalidating objects held by active plans.
                if (registry == dynamic && dynamic.size() >= 8192) dynamic.clear();
                registry.put(key, value); builds.increment();
            } else hits.increment();
            return value;
        }
    }
    /** Independent bounded memo table for compiled expressions and metadata. Values never contain request data. */
    public Cache newCache(int limit) { return new Cache(Math.max(1, limit)); }
    public static final class Cache {
        private record Entry(String fingerprint, Object value) { }
        private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
        private final int limit;
        private final LongAdder hits = new LongAdder(), misses = new LongAdder();
        Cache(int limit) { this.limit = limit; }
        public Object read(String key, String fingerprint) {
            var entry = entries.get(key);
            if (entry != null && (fingerprint == null || fingerprint.equals(entry.fingerprint()))) {
                hits.increment(); return entry.value();
            }
            misses.increment(); return null;
        }
        public Object write(String key, String fingerprint, Object value) {
            if (entries.size() >= limit) entries.clear();
            entries.put(key, new Entry(fingerprint, value));
            return value;
        }
        public void clear() { entries.clear(); }
        public String[] keys() { return entries.keySet().toArray(String[]::new); }
        public boolean remove(String key) { return entries.remove(key) != null; }
        public int size() { return entries.size(); }
        public long hits() { return hits.sum(); }
        public long misses() { return misses.sum(); }
    }
    public Object lookup(String key) {
        return key.startsWith("plan:") || key.startsWith("script:") ? dynamic.get(key) : prepared.get(key);
    }
    public Map<String, Object> statistics() {
        return Map.of("prepared", prepared.size(), "dynamic", dynamic.size(), "hits", hits.sum(), "builds", builds.sum(), "calls", calls.sum());
    }
    public Scriptable state() {
        var frame = current.get();
        if (frame == null) throw new IllegalStateException("No execution frame");
        if (frame.state == null) frame.state = Context.getCurrentContext().newObject(frame.scope);
        return frame.state;
    }
    public Scriptable requestScope() {
        var frame = current.get();
        if (frame == null) throw new IllegalStateException("No execution frame");
        return frame.scope;
    }
    Frame enter(Context cx) {
        var scope = new NativeObject();
        scope.setPrototype(shared); scope.setParentScope(null);
        // Result-policy guards require own, ordinary intrinsic bindings. These are
        // references to the sealed constructors, never copies of standard objects.
        scope.defineProperty("Object", objectConstructor, ScriptableObject.READONLY | ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
        scope.defineProperty("Array", arrayConstructor, ScriptableObject.READONLY | ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
        var frame = new Frame(current.get(), scope);
        current.set(frame); calls.increment();
        return frame;
    }
    final class Frame implements AutoCloseable {
        final Frame previous;
        final ScriptableObject scope;
        Scriptable state;
        private final Thread owner = Thread.currentThread();
        private boolean closed;
        Frame(Frame previous, ScriptableObject scope) { this.previous = previous; this.scope = scope; }
        @Override public void close() {
            if (closed) return;
            if (owner != Thread.currentThread() || current.get() != this)
                throw new IllegalStateException("Execution frames must close in order on their owning thread");
            closed = true;
            if (previous == null) current.remove(); else current.set(previous);
            state = null;
        }
    }
    /** Lexical references from shared code resolve request bindings on the calling thread.
     * The prototype owns only standard objects, engine code and fixed bootstrap bindings.
     */
    private final class RoutingScope extends NativeObject {
        private static final long serialVersionUID = 1L;
        @Override public Object get(String name, Scriptable start) {
            var frame = current.get();
            if (frame != null && name.equals("__flowExecutionState")) return state();
            if (frame != null) {
                Object value = frame.scope.get(name, frame.scope);
                if (value != NOT_FOUND || name.equals("context")) return value;
            }
            return super.get(name, start);
        }
        @Override public boolean has(String name, Scriptable start) {
            var frame = current.get();
            if (frame != null && (name.equals("__flowExecutionState") || frame.scope.has(name, frame.scope))) return true;
            return super.has(name, start);
        }
        @Override public void put(String name, Scriptable start, Object value) {
            var frame = current.get();
            if (frame != null && start == this) { frame.scope.put(name, frame.scope, value); return; }
            super.put(name, start, value);
        }
    }
}
