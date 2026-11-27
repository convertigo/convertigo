/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.engine.debug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeCall;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

/** A bounded, single-request debugger. All access to live JavaScript values stays on its worker. */
public final class JavascriptDebugSession implements Debugger, AutoCloseable {
	public static final long IDLE_MILLIS = 60_000;
	private static final int MAX_SOURCES = 64, MAX_SOURCE_CHARS = 262_144, MAX_TOTAL_CHARS = 2_097_152;
	private final String id = UUID.randomUUID().toString();
	private final Object lock = new Object();
	private final Map<String, Source> sources = new LinkedHashMap<>();
	private final Map<String, JSONObject> locations = new LinkedHashMap<>();
	private final Inspection inspection = new Inspection();
	private final Map<String, Set<Integer>> breakpoints = new LinkedHashMap<>();
	private record ParticipantTarget(String url, int line, String key, boolean unique) { }
	private record ParticipantSource(String revision, List<ParticipantTarget> targets) { }
	private final Map<String, ParticipantSource> participantSources = new LinkedHashMap<>();
	private final List<JSONObject> breakpointChanges = new ArrayList<>();
	private final Set<String> rhinoSources = new java.util.HashSet<>();
	private final Map<Thread, Worker> workers = new LinkedHashMap<>();
	private List<Frame> frames = List.of();
	private Worker pausedWorker, stepWorker;
	private final long pauseTimeoutMillis;
	private long pauseDeadline;
	private boolean pauseFreeEvaluation;
	private String notice = "";
	public static final String UNSUPPORTED_RUNTIME = "Prepared provider runtimes and shared compiled modules cannot be debugged. Run this request with the debugger detached.";
	private static final class Worker {
		final String id = UUID.randomUUID().toString();
		String root = UUID.randomUUID().toString();
		Worker parent;
		List<Frame> ancestors = List.of();
		java.util.function.BooleanSupplier cancelled = () -> false;
		java.util.function.Supplier<JavascriptDebugger.DebugSuspension> budgetPause = com.twinsoft.convertigo.beans.core.RequestableObject::pauseCurrentRequestable;
		final List<Frame> frames = new ArrayList<>();
		final boolean rhino;
		Worker(boolean rhino) { this.rhino = rhino; }
		volatile long evaluationDeadline;
		Scriptable providerBoundary;
		final Map<String, Scriptable> privateBoundaries = new LinkedHashMap<>();
	}
	/** A server-only continuation, captured by its owner before scheduling; never a public attachment token. */
	static final class Parent {
		final Worker worker;
		final List<Frame> frames;
		Parent(Worker worker) { this.worker = worker; this.frames = stack(worker); }
	}
	private static List<Frame> stack(Worker worker) {
		var frames = new ArrayList<>(worker.ancestors); frames.addAll(worker.frames); return frames;
	}
	Parent parent(Object data) {
		synchronized (lock) {
			var worker = (Worker) data;
			if (closed || workers.get(Thread.currentThread()) != worker) return null;
			return new Parent(worker);
		}
	}
	Object joinChild(Parent parent, java.util.function.BooleanSupplier cancelled,
			java.util.function.Supplier<JavascriptDebugger.DebugSuspension> budgetPause) {
		synchronized (lock) {
			if (closed || !workers.containsValue(parent.worker)) return null;
			if (parent.frames.size() >= 128) throw new IllegalStateException("Participant ancestry limit.");
			if (workers.containsKey(Thread.currentThread()) || workers.size() >= 64) throw new IllegalStateException("Invalid child worker.");
			var worker = new Worker(false); worker.parent = parent.worker; worker.root = parent.worker.root;
			worker.ancestors = parent.frames; worker.cancelled = () -> parent.worker.cancelled.getAsBoolean() || cancelled.getAsBoolean(); worker.budgetPause = budgetPause;
			workers.put(Thread.currentThread(), worker); event("workerStarted"); return worker;
		}
	}
	/**
	 * A requestable called by a source-debugged request, such as a sub-sequence on its own thread, gets the same
	 * interpreted context: its breakpoints stop it. Returns null when the parent is not source-debugged.
	 */
	AutoCloseable joinRhinoChild(Parent parent, java.util.function.BooleanSupplier cancelled,
			java.util.function.Supplier<JavascriptDebugger.DebugSuspension> budgetPause) {
		var worker = new Worker(true);
		synchronized (lock) {
			if (closed || !parent.worker.rhino || !workers.containsValue(parent.worker) || Context.getCurrentContext() != null) return null;
			if (parent.frames.size() >= 128) throw new IllegalStateException("Participant ancestry limit.");
			if (workers.containsKey(Thread.currentThread()) || workers.size() >= 64) throw new IllegalStateException("Invalid child worker.");
			worker.parent = parent.worker; worker.root = parent.worker.root;
			worker.ancestors = parent.frames; worker.cancelled = () -> parent.worker.cancelled.getAsBoolean() || cancelled.getAsBoolean(); worker.budgetPause = budgetPause;
			workers.put(Thread.currentThread(), worker); event("workerStarted");
		}
		return enterContext(worker);
	}
	void identifyRoot(Object data, String root, java.util.function.BooleanSupplier cancelled) {
		synchronized (lock) { var worker = (Worker) data; worker.root = root; worker.cancelled = cancelled; }
	}
	private static boolean descendant(Worker worker, Worker parent) {
		for (; worker != null; worker = worker.parent) if (worker == parent) return true;
		return false;
	}
	private static boolean cancelled(Worker worker) { return Thread.currentThread().isInterrupted() || worker.cancelled.getAsBoolean(); }
	private static void checkpoint(Worker worker) {
		if (cancelled(worker)) throw new java.util.concurrent.CancellationException("Request owner cancelled.");
	}
	public JavascriptDebugSession() { this(JavascriptDebugger.pauseTimeoutMillis()); }
	JavascriptDebugSession(long pauseTimeoutMillis) { this.pauseTimeoutMillis = pauseTimeoutMillis; JavascriptDebugProperties.prepare(); JavascriptDebugRedaction.text(""); JavascriptDebugResponse.prepare(); }
	public void flowWithoutDebugging() { synchronized (lock) { notice = "Flow orchestration runs without line debugging; no private block implementation has executed yet."; event("flowWithoutDebugging"); } }
	void providerDebuggable() { synchronized (lock) { notice = "Private block implementations, libraries and their module dependencies are debuggable; orchestration stays detached."; } }
	void registerProviderSource(String url) {
		synchronized (lock) {
			if (closed) return;
			if (locations.size() >= MAX_SOURCES && !locations.containsKey(url)) locations.remove(locations.keySet().iterator().next());
			locations.put(url, json("id", url, "file", true, "label", url,
					"offset", com.twinsoft.convertigo.engine.util.CopilotHelper.addInstruction(null, "").split("\n", -1).length - 1));
		}
	}
	void privateBoundary(Object data, String url, Scriptable boundary) {
		((Worker) data).privateBoundaries.put(url, boundary);
	}
	JavascriptDebugger.DebugSuspension providerScopes(Object data, Scriptable boundary) {
		var worker = (Worker) data;
		var previous = worker.providerBoundary;
		worker.providerBoundary = boundary;
		return () -> worker.providerBoundary = previous;
	}
	public void unsupportedRuntime() { synchronized (lock) { notice = UNSUPPORTED_RUNTIME; event("unsupportedRuntime"); } }
	private final List<JSONObject> events = new ArrayList<>();
	private long serial, pauseId, seen = System.currentTimeMillis();
	private int sourceChars, stopDepth = -1;
	private boolean paused, breakNext, breakOnExceptions, breakOnEnter, breakOnReturn, closed, inspecting, stepAfterAllowed, stepReturnAllowed;

	private Throwable lastException;
	private Job job, activeJob;


	private record Source(String text, Set<Integer> lines) { }
	private record Job(Function<Context, JSONObject> operation, CompletableFuture<JSONObject> result) { }
	private static final class EvaluationTimeout extends Error {
		private static final long serialVersionUID = 1L;
	}

	public String id() { return id; }
	public void touch() { synchronized (lock) { seen = System.currentTimeMillis(); } }
    void cancellationChanged() {
        synchronized (lock) {
            if (paused && pausedWorker != null && pausedWorker.cancelled.getAsBoolean()) {
                paused = false; breakNext = false; stepWorker = null; inspection.clear(); failJob(); event("cancelled");
            }
            lock.notifyAll();
        }
    }
	public boolean expired(long now) { synchronized (lock) { return closed || now - seen >= IDLE_MILLIS; } }

	/** Enter before the requestable creates its ordinary context; nested Context.enter calls reuse this one. */
	public AutoCloseable enter() { return enter(false); }
	public AutoCloseable enterChild() { return enter(true); }
	/** Join the existing compiled context. No factory, debugger or compilation mode is changed. */
	Object joinParticipantWorker() { return joinParticipantWorker(Thread.currentThread()); }
	Object joinParticipantWorker(Thread owner) {
		synchronized (lock) {
			if (closed) throw new IllegalStateException("The debug session has ended.");
			var worker = workers.get(owner);
			if (worker != null) return worker;
			if (!workers.isEmpty()) throw new IllegalStateException("Another request is already running in this debug session.");
			worker = new Worker(false); workers.put(owner, worker); event("started");
			return worker;
		}
	}
	void finishParticipantWorker() { finish(); }
	private AutoCloseable enter(boolean child) {
		var worker = new Worker(true);
		synchronized (lock) {
			if (closed) { if (child) return null; throw new IllegalStateException("The debug session has ended."); }
			if (!child && !workers.isEmpty()) {
				notice = "Another Eclipse or web request was refused: this debug session already has a running request. Continue it or stop the debugger before running again.";
				event("requestRejected");
				throw new IllegalStateException(notice);
			}
			if (child && workers.isEmpty()) return null;
			if (workers.size() >= 64) throw new IllegalStateException("Too many debug workers.");
			if (Context.getCurrentContext() != null) throw new IllegalStateException("A JavaScript context is already active.");
			workers.put(Thread.currentThread(), worker);
			lastException = null;
			event("started");
		}
		return enterContext(worker);
	}
	private AutoCloseable enterContext(Worker worker) {
		try {
			new ContextFactory() {
				@Override protected Context makeContext() {
					var cx = super.makeContext();
					cx.setLanguageVersion(Context.VERSION_ES6);
					cx.setInterpretedMode(true);
					cx.setGeneratingDebug(true);
					cx.setInstructionObserverThreshold(10_000);
					cx.setDebugger(JavascriptDebugSession.this, worker);
					return cx;
				}
				@Override protected void observeInstructionCount(Context cx, int count) {
					if (worker.evaluationDeadline != 0 && System.nanoTime() > worker.evaluationDeadline) throw new EvaluationTimeout();
				}
			}.enterContext();
		} catch (RuntimeException | Error e) {
			finish();
			throw e;
		}
		return () -> { try { Context.exit(); } finally { finish(); } };
	}

	private void finish() {
		synchronized (lock) {
			var worker = workers.remove(Thread.currentThread());
			if (worker != null && worker.parent != null) {
				if (!worker.ancestors.isEmpty()) worker.ancestors.get(worker.ancestors.size() - 1).childReturned = true;
				if (stepWorker == worker) stepWorker = worker.parent;
			}
			if (pausedWorker == worker) { paused = false; pausedWorker = null; frames = List.of(); failJob(); }
			if (workers.isEmpty()) { lastException = null; stepWorker = null; event("finished"); }
			lock.notifyAll();
		}
	}

	@Override public void close() {
		synchronized (lock) {
			closed = true; paused = false; breakNext = false;
			failJob(); frames = List.of(); pausedWorker = null; stepWorker = null; sources.clear(); locations.clear(); inspection.clear(); breakpoints.clear(); participantSources.clear(); breakpointChanges.clear(); rhinoSources.clear(); sourceChars = 0;
			event("detached"); lock.notifyAll();
		}
	}

	private void failJob() {
        if (activeJob != null) activeJob.result.completeExceptionally(new IllegalStateException("The pause has ended."));
		if (job != null) { job.result.completeExceptionally(new IllegalStateException("The pause has ended.")); job = null; }
	}

	private void event(String type) {
		events.add(json("serial", ++serial, "type", type, "pauseId", pauseId));
		if (events.size() > 64) events.remove(0);
	}

	@Override public void handleCompilationDone(Context cx, DebuggableScript script, String text) {
		if (!script.isTopLevel() || text == null || text.length() > MAX_SOURCE_CHARS) return;
		synchronized (lock) {
			if (closed) return;
			var lines = new TreeSet<Integer>(); collectLines(script, lines);
			String url = script.getSourceName();
			if (url == null) url = "<script>";
			var previous = sources.remove(url);
			if (previous != null) {
				sourceChars -= previous.text.length();
				if (!previous.text.equals(text) && breakpoints.containsKey(url)) {
					var remapped = remapBreakpoints(previous.text, text, breakpoints.get(url), lines);
					breakpoints.put(url, remapped);
					notice = "Source changed. Breakpoints were remapped; removed or ambiguous lines were cleared.";
					event("breakpointsRemapped");
				}
			}
			while (!sources.isEmpty() && (sources.size() >= MAX_SOURCES || sourceChars + text.length() > MAX_TOTAL_CHARS)) {
				var first = sources.keySet().iterator().next(); sourceChars -= sources.remove(first).text.length();
			}
			sources.put(url, new Source(text, lines)); sourceChars += text.length(); event("source");
		}
	}

	/** Keep unchanged prefix/suffix and unique surviving lines; never move a removed line silently. */
	static Set<Integer> remapBreakpoints(String before, String after, Set<Integer> points, Set<Integer> executable) {
		String[] oldLines = before.split("\\n", -1), newLines = after.split("\\n", -1);
		int prefix = 0, suffix = 0;
		while (prefix < Math.min(oldLines.length, newLines.length) && oldLines[prefix].equals(newLines[prefix])) prefix++;
		while (suffix < Math.min(oldLines.length, newLines.length) - prefix
				&& oldLines[oldLines.length - 1 - suffix].equals(newLines[newLines.length - 1 - suffix])) suffix++;
		var unique = new java.util.HashMap<String, Integer>();
		for (int i = prefix; i < newLines.length - suffix; i++) unique.merge(newLines[i], i + 1, (a, b) -> -1);
		var result = new TreeSet<Integer>();
		for (int line : points) {
			if (line < 1 || line > oldLines.length) continue;
			int next = line <= prefix ? line : line > oldLines.length - suffix ? line + newLines.length - oldLines.length
					: unique.getOrDefault(oldLines[line - 1], -1);
			if (next > 0 && executable.contains(next)) result.add(next);
		}
		return result;
	}

	private static void collectLines(DebuggableScript script, Set<Integer> lines) {
		for (int line : script.getLineNumbers()) if (line > 0) lines.add(line);
		for (int i = 0; i < script.getFunctionCount(); i++) collectLines(script.getFunction(i), lines);
	}

	@Override public DebugFrame getFrame(Context cx, DebuggableScript script) {
		synchronized (lock) {
			var worker = (Worker) cx.getDebuggerContextData();
			// Provider orchestration is never an inspectable frame, even if Rhino invokes
			// an engine callback while an interpreted private function is attached.
			if (closed || worker.providerBoundary != null && !worker.privateBoundaries.containsKey(script.getSourceName())) return null;
			return new Frame(script, worker);
		}
	}

	/** Opaque participant frames share the authenticated session, pause queue and stepping depth. */
	/** A tree/editor can set a point before the first invocation, including from a cached projection. */
	public boolean participantBreakpoint(String url, int line, boolean set, String identity) throws Exception {
		if (identity == null || identity.isEmpty()) return breakpoint(url,line,set,false);
		if (identity.length() > 4096) throw new IllegalArgumentException("Participant identity limit.");
		var info = new JSONObject(identity);
		var kind = info.getString("kind"); var sourceId = info.getString("sourceId");
		var revision = info.getString("revision"); var key = info.getString("key");
		if (kind.isEmpty() || kind.length()>128 || sourceId.isEmpty() || sourceId.length()>2048 || revision.isEmpty()
				|| revision.length()>128 || key.isEmpty() || key.length()>128) throw new IllegalArgumentException("Invalid participant identity.");
		synchronized (lock) {
			var id = kind + ":" + sourceId;
			var previous = participantSources.get(id);
			if (set && previous != null && !previous.revision.equals(revision)) {
				notice = "This breakpoint belongs to an older source revision. Refresh the source and set a new breakpoint.";
				event("participantBreakpointStale"); return false;
			}
			if (set && previous == null && participantSources.size() >= MAX_SOURCES)
				throw new IllegalArgumentException("Too many participant sources.");
			var accepted = breakpoint(url,line,set,false);
			if (accepted && set) {
				var rows = new ArrayList<ParticipantTarget>(previous == null ? List.of() : previous.targets);
				if (rows.stream().noneMatch(target -> target.url.equals(url))) rows.add(new ParticipantTarget(url,line,key,info.getBoolean("unique")));
				participantSources.put(id,new ParticipantSource(revision,List.copyOf(rows)));
			}
			return accepted;
		}
	}

	/** Providers publish opaque revision/key pairs. No source syntax or model paths are interpreted here. */
	void participantSources(String kind, String manifest) throws Exception {
		if (kind == null || kind.length() > 128 || manifest.length() > MAX_TOTAL_CHARS)
			throw new IllegalArgumentException("Participant source limit.");
		var input = new JSONObject(manifest);
		var sourceId = input.getString("id"); var revision = input.getString("revision");
		var rows = input.getJSONArray("targets");
		if (sourceId.isEmpty() || sourceId.length() > 2048 || revision.isEmpty() || revision.length() > 128 || rows.length() > 4096)
			throw new IllegalArgumentException("Invalid participant source.");
		var targets = new ArrayList<ParticipantTarget>(); var urls = new java.util.HashSet<String>();
		for (int i = 0; i < rows.length(); i++) {
			var row = rows.getJSONObject(i);
			var url = row.getString("url"); var line = row.getInt("line"); var key = row.getString("key");
			if (url.isEmpty() || url.length() > 1024 || line < 1 || line > 1_000_000 || key.isEmpty() || key.length() > 128 || !urls.add(url))
				throw new IllegalArgumentException("Invalid participant target.");
			targets.add(new ParticipantTarget(url, line, key, row.optBoolean("unique", true)));
		}
		synchronized (lock) {
			if (closed) return;
			var id = kind + ":" + sourceId;
			var previousId = input.optString("previousId", sourceId);
			if (previousId.length() > 2048) throw new IllegalArgumentException("Invalid previous participant source.");
			var previousKey = kind + ":" + previousId;
			var previous = participantSources.get(previousKey);
			if (previous != null && previousKey.equals(id) && previous.revision.equals(revision)) return;
			if (previous != null) {
				var oldCounts = new java.util.HashMap<String, Integer>();
				var newCounts = new java.util.HashMap<String, Integer>();
				var byKey = new java.util.HashMap<String, ParticipantTarget>();
				for (var target : previous.targets) oldCounts.merge(target.key, 1, Integer::sum);
				for (var target : targets) { newCounts.merge(target.key, 1, Integer::sum); byKey.put(target.key, target); }
				int kept = 0, invalidated = 0, ambiguous = 0;
				for (var target : previous.targets) {
					var points = breakpoints.remove(target.url);
					if (points == null || points.isEmpty()) continue;
					rhinoSources.remove(target.url);
					var replacement = byKey.get(target.key);
					boolean unique = target.unique && oldCounts.get(target.key) == 1 && newCounts.getOrDefault(target.key, 0) == 1;
					if (replacement != null && unique) {
						breakpoints.computeIfAbsent(replacement.url, unused -> new TreeSet<>()).add(replacement.line);
						kept++;
						breakpointChanges.add(json("url", target.url, "line", target.line, "status", "preserved",
							"replacementUrl", replacement.url, "replacementLine", replacement.line,
							"reason", "Source changed; the unchanged target is unique in both revisions."));
					} else {
						invalidated++;
						if (replacement != null) ambiguous++;
						breakpointChanges.add(json("url", target.url, "line", target.line, "status", "invalidated",
							"reason", replacement == null ? "Source changed; the target was edited or removed. Set a new breakpoint."
								: "Source changed; duplicate targets make this breakpoint ambiguous. It was cleared, never moved. Set a new breakpoint."));
					}
				}
				while (breakpointChanges.size() > 64) breakpointChanges.remove(0);
				if (kept + invalidated > 0) {
					notice = "Source changed: " + kept + " breakpoint(s) preserved on unique unchanged targets. "
						+ (ambiguous > 0 ? ambiguous + " invalidated: duplicate targets are ambiguous. " : "")
						+ (invalidated > ambiguous ? (invalidated-ambiguous) + " invalidated: the target was edited or removed. " : "")
						+ (invalidated > 0 ? "Invalidated breakpoints were cleared, never moved; set a new breakpoint." : "");
					event("participantBreakpointsChanged");
				}
			}
			if (participantSources.size() >= MAX_SOURCES && !participantSources.containsKey(id)) {
				// Losing a reconciliation baseline must never leave active points with an unknown revision.
				var first = participantSources.keySet().iterator().next();
				for (var target : participantSources.remove(first).targets) breakpoints.remove(target.url);
			}
			if (!previousKey.equals(id)) participantSources.remove(previousKey);
			participantSources.put(id, new ParticipantSource(revision, List.copyOf(targets)));
		}
	}

	public final class Participant {
		private final String kind;
		private final Worker worker;
		private final org.mozilla.javascript.Function inspect;
		private final String invocation = UUID.randomUUID().toString();
        private boolean parsedPathEvaluation;
        /** Declare support for the parsed-path action; no legacy evaluation fallback. */
        public void pathEvaluation() { owner(); parsedPathEvaluation = true; }
        public String previewParsedPath(Object value, JavascriptDebugPath path) {
            owner();
            if (!inspecting || !paused || pausedWorker != worker || closed) throw JavascriptDebugPath.refused();
            try { return preview(path.resolve(value, () -> JavascriptDebugSession.this.inspection.work(true))); }
            catch (JavascriptDebugResponse.Limit e) { throw e; }
            catch (Exception e) { return "<opaque>"; }
        }
		private Participant(String kind, Worker worker, org.mozilla.javascript.Function inspect) {
			this.kind = kind; this.worker = worker; this.inspect = inspect;
		}
		private void owner() {
			synchronized (lock) {
				if (workers.get(Thread.currentThread()) != worker) throw new IllegalStateException("Participant used by another worker.");
			}
		}
		public void enter(String frameId, String descriptor) throws Exception {
			owner();
			if (frameId == null || frameId.length() > 128 || descriptor.length() > MAX_SOURCE_CHARS) throw new IllegalArgumentException("Participant frame limit.");
			var info = new JSONObject(descriptor);
			var url = info.getString("url"); var line = info.getInt("line");
			if (url.length() > 1024 || line < 1 || line > 1_000_000) throw new IllegalArgumentException("Invalid participant location.");
			synchronized (lock) {
				if (closed) return;
				if (worker.frames.size() >= 128) throw new IllegalStateException("Participant stack limit.");
				var frame = new Frame(this, frameId, info, worker);
				var chain = stack(worker);
				frame.parentActivation = chain.isEmpty() ? "" : chain.get(chain.size() - 1).activation;
				frame.depth = chain.size();
				worker.frames.add(frame);
				var text = info.optString("source", "");
				if (!text.isEmpty() && text.length() <= MAX_SOURCE_CHARS) {
					var previous = sources.remove(url); if (previous != null) sourceChars -= previous.text.length();
					while (!sources.isEmpty() && (sources.size() >= MAX_SOURCES || sourceChars + text.length() > MAX_TOTAL_CHARS)) {
						var first = sources.keySet().iterator().next(); sourceChars -= sources.remove(first).text.length();
					}
					sources.put(url, new Source(text, Set.of(line))); sourceChars += text.length();
				}
				if (info.optJSONObject("location") != null) {
					if (locations.size() >= MAX_SOURCES && !locations.containsKey(url)) locations.remove(locations.keySet().iterator().next());
					locations.put(url, info.getJSONObject("location"));
				}
			}
		}
		private Frame top(String frameId) {
			owner();
			if (worker.frames.isEmpty()) return null;
			var frame = worker.frames.get(worker.frames.size() - 1);
			if (frame.participant != this || !frame.frameId.equals(frameId)) throw new IllegalStateException("Participant frames must be nested.");
			return frame;
		}
		public void at(String frameId, String phase) {
			// A previously registered token cannot bypass a shared-construction barrier.
			if (!JavascriptDebugger.participantAllowed()) return;
			if (!Set.of("before", "after", "return", "error").contains(phase)) throw new IllegalArgumentException("Invalid participant phase.");
			boolean stop;
			synchronized (lock) {
				checkpoint(worker);
				if (closed) return;
				var frame = top(frameId); if (frame == null) return;
				if ("after".equals(phase) && frame.childReturned) phase = "return";
				frame.phase = phase;
				stop = !inspecting && !paused && ("error".equals(phase)
					|| "before".equals(phase) && breakpoints.getOrDefault(frame.url(), Set.of()).contains(frame.line)
					|| breakNext && ("before".equals(phase) || stepReturnAllowed && "return".equals(phase) || stepAfterAllowed && "after".equals(phase))
						&& (stepWorker == null || descendant(worker, stepWorker)) && (stopDepth < 0 || stack(worker).size() <= stopDepth));
			}
			if (stop) pause(Context.getCurrentContext(), "participant", worker);
			checkpoint(worker);
		}
		public void leave(String frameId) {
			synchronized (lock) { if (closed) return; var frame = top(frameId); if (frame != null) worker.frames.remove(frame); }
		}
		/** Reuse the ordinary opaque references and pages; only the paused worker can export a scope. */
		public String scope(String name, Object value) throws Exception {
			owner();
			synchronized (lock) {
				if (!paused || pausedWorker != worker || !inspecting || closed)
					throw new IllegalStateException("Scopes can only be described during participant inspection.");
			}
			if (name == null || name.length() > 500) throw new IllegalArgumentException("Invalid scope name.");
			var registry = JavascriptDebugSession.this.inspection;
			return JavascriptDebugResponse.prepare(registry.page(value, 0, 20).put("name", name).put("ref", registry.reference(value)), registry::work).toString();
		}
        /** Read a supplied root through qualified own-data descriptors only, never host getters. */
        public String previewPath(Object value, String path) {
            owner();
            if (!inspecting || !paused || pausedWorker != worker || closed) throw new IllegalStateException("Inspection has ended.");
            if (path == null || path.length() > 4096 || !path.isEmpty() && !path.matches("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*"))
                throw new IllegalArgumentException("Invalid inspection path.");
            try {
                if (!path.isEmpty()) for (String key : path.split("\\.")) {
                    if (!JavascriptDebugSession.this.inspection.budget.candidate()) return "<inspection limited>";
                    value = JavascriptDebugProperties.read(value, key);
                    if (!JavascriptDebugSession.this.inspection.budget.check()) return "<inspection limited>";
                }
                return preview(value);
            } catch (Exception e) { return "<opaque>"; }
        }
		private JSONObject inspection(Context cx, Frame frame, String action, Object expression) {
			var debugger = cx.getDebugger(); var data = cx.getDebuggerContextData();
			cx.setDebugger(null, null); worker.evaluationDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
			try {
				var scope = inspect.getParentScope();
				JavascriptDebugSession.this.inspection.work(false);
				Object value; long evaluation = System.nanoTime();
				try { value = inspect.call(cx, scope, scope, new Object[] { frame.frameId, action, expression instanceof JavascriptDebugPath ? Context.javaToJS(expression, scope) : expression }); }
				finally {
					// An administrator evaluation has its own deadline, as a Rhino expression: its run is not inspection work.
					if ("evaluate".equals(action) || "path".equals(action)) JavascriptDebugSession.this.inspection.exclude(System.nanoTime() - evaluation);
				}
				if (!(value instanceof String || value instanceof org.mozilla.javascript.ConsString)) throw new IllegalArgumentException("Participant inspection must return text.");
                if (!JavascriptDebugSession.this.inspection.budget.check()) return json("result", "Inspection work limit reached.", "partial", true, "reason", JavascriptDebugSession.this.inspection.budget.reason);
                // A rope can flatten arbitrary work in toString: only a materialized, bounded reply is accepted.
                if (!(value instanceof String text) || text.length() > 32768)
                    return JavascriptDebugSession.this.inspection.refused("participant-response-size");
                try { return JavascriptDebugResponse.decode(text, JavascriptDebugSession.this.inspection::work); }
                catch (JavascriptDebugResponse.Limit e) { return JavascriptDebugSession.this.inspection.refused(e.reason); }
			} catch (EvaluationTimeout e) { return json("result", "Evaluation timed out.", "evaluationError", true); }
			catch (Exception e) {
                Throwable cause = e instanceof org.mozilla.javascript.WrappedException wrapped ? wrapped.getWrappedException() : e;
                if (cause instanceof JavascriptDebugResponse.Limit limit) return JavascriptDebugSession.this.inspection.refused(limit.reason);
                throw new IllegalStateException("Participant inspection failed.");
            }
			finally { worker.evaluationDeadline = 0; cx.setDebugger(debugger, data); }
		}
	}
	Participant participant(String kind, Object data, org.mozilla.javascript.Function inspect) {
		if (kind == null || !kind.matches("[a-zA-Z][a-zA-Z0-9.-]{0,63}") || inspect == null) throw new IllegalArgumentException("Invalid participant.");
		return new Participant(kind, (Worker) data, inspect);
	}

	private final class Frame implements DebugFrame {
		final DebuggableScript script;
		final Participant participant;
		final String frameId;
		final JSONObject descriptor;
		final String activation = UUID.randomUUID().toString();
		String parentActivation = "";
		int depth;
		boolean childReturned;
		String phase = "";
		final Worker worker;
		final Scriptable providerBoundary;
		Scriptable scope, thisObject;
		int line;
		Frame(DebuggableScript script, Worker worker) { this.participant = null; this.frameId = ""; this.descriptor = null; this.script = script; this.worker = worker; this.providerBoundary = worker.privateBoundaries.getOrDefault(script.getSourceName(), worker.providerBoundary); }
		Frame(Participant participant, String frameId, JSONObject descriptor, Worker worker) {
			this.participant = participant; this.frameId = frameId; this.descriptor = descriptor;
			this.worker = worker; this.script = null; this.providerBoundary = null; this.line = descriptor.optInt("line", 1);
		}
		String url() { if (participant != null) return descriptor.optString("url"); return script.getSourceName() == null ? "<script>" : script.getSourceName(); }
		@Override public void onEnter(Context cx, Scriptable scope, Scriptable self, Object[] args) {
			synchronized (lock) {
				var chain = stack(worker); depth = chain.size(); parentActivation = chain.isEmpty() ? "" : chain.get(chain.size() - 1).activation;
				this.scope = scope; thisObject = self; worker.frames.add(this);
			}
			if (script.isFunction()) {
				synchronized (lock) { if (!breakOnEnter || closed || inspecting) return; }
				var lines = script.getLineNumbers();
				line = lines.length == 0 ? 1 : java.util.Arrays.stream(lines).min().orElse(1);
				pause(cx, "functionEnter");
			}
		}
		@Override public void onLineChange(Context cx, int number) {
			synchronized (lock) {
				line = number;
				if (closed || paused) return;
				boolean breakpoint = breakpoints.getOrDefault(url(), Set.of()).contains(number);
				if (!breakpoint && !(breakNext && (stepWorker == null || stepWorker == worker) && (stopDepth < 0 || worker.frames.size() <= stopDepth))) return;
			}
			pause(cx, "pause");
		}
		@Override public void onExceptionThrown(Context cx, Throwable exception) {
			synchronized (lock) {
				if (closed || !breakOnExceptions || lastException == exception) return;
				lastException = exception;
			}
			pause(cx, "exception");
		}
		@Override public void onExit(Context cx, boolean byThrow, Object result) {
			boolean stop;
			synchronized (lock) { stop = script.isFunction() && !byThrow && breakOnReturn && !closed && !inspecting; }
			if (stop) pause(cx, "functionReturn");
			synchronized (lock) { worker.frames.remove(this); scope = null; thisObject = null; }
		}
		@Override public void onDebuggerStatement(Context cx) { pause(cx, "pause"); }
	}

	/** The worker services commands while paused. No monitor is held during user evaluation. */
	private void pause(Context cx, String reason) {
        if (!JavascriptDebugger.freeEvaluation()) { evaluationFree(); return; }
        pause(cx, reason, (Worker) cx.getDebuggerContextData());
    }
	private void pause(Context cx, String reason, Worker worker) {
		try (var requestPause = worker.budgetPause.get()) {
			synchronized (lock) {
				if (closed || paused) return;
				pausedWorker = worker; frames = stack(worker);
				pauseDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(pauseTimeoutMillis);
				inspection.clear();
				pauseFreeEvaluation = JavascriptDebugger.freeEvaluation();
				paused = true; breakNext = false; stopDepth = -1; pauseId++; event(reason);
			}
			while (true) {
				Job pending;
				synchronized (lock) {
					if (!paused || closed) { inspection.clear(); return; }
					if (cancelled(worker)) {
						paused = false; breakNext = false; stepWorker = null; failJob();
						inspection.clear(); event("cancelled"); lock.notifyAll(); return;
					}
					if (System.nanoTime() >= pauseDeadline) {
						paused = false; breakNext = false; stepWorker = null; failJob();
						notice = "The maximum pause duration was reached. The request resumed automatically.";
						inspection.clear(); event("autoResumed"); lock.notifyAll(); return;
					}
					if (System.currentTimeMillis() - seen >= IDLE_MILLIS) { close(); return; }
					if (job == null) {
						try { lock.wait(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
						continue;
					}
					pending = job; job = null; activeJob = pending; inspecting = true;
				}
				JSONObject result = null; Throwable failure = null;
				try { inspection.begin(); result = inspection.finish(pending.operation.apply(cx)); }
				catch (RuntimeException | Error e) { failure = new IllegalStateException("Inspection failed."); }
				finally { synchronized (lock) { activeJob = null; inspecting = false; lock.notifyAll(); } }
				if (failure == null) pending.result.complete(result); else pending.result.completeExceptionally(failure);
			}
		}
	}

	public void resume(String action, long expectedPause) {
		if (!Set.of("go", "stepInto", "stepOver", "stepOut").contains(action)) throw new IllegalArgumentException("Invalid resume action.");
		synchronized (lock) {
			checkPause(expectedPause);
			if (job != null || inspecting) throw new IllegalStateException("An inspection is pending.");
			stopDepth = switch (action) { case "stepOver" -> frames.size(); case "stepOut" -> frames.size() - 1; default -> -1; };
			// Into follows a child first, or exposes a leaf's output. Over/out skip logical return boundaries.
			stepAfterAllowed = "stepInto".equals(action) || !frames.isEmpty() && frames.get(frames.size() - 1).participant == null;
			stepReturnAllowed = "stepInto".equals(action) || "stepOut".equals(action);
			stepWorker = "go".equals(action) ? null : pausedWorker;
			inspection.clear(); breakNext = !"go".equals(action); paused = false; event("resumed"); lock.notifyAll();
		}
	}
	public void requestPause() { synchronized (lock) { breakNext = true; stepWorker = null; stopDepth = -1; event("breakRequested"); } }
	public void breakOnExceptions(boolean value) { synchronized (lock) { breakOnExceptions = value; event("settings"); } }
	public void breakOnEnter(boolean value) { synchronized (lock) { breakOnEnter = value; event("settings"); } }
	public void breakOnReturn(boolean value) { synchronized (lock) { breakOnReturn = value; event("settings"); } }
	void settingsChanged() { synchronized (lock) { event("settings"); } }

	private void checkPause(long expected) {
		if (!paused || closed || expected != pauseId || System.nanoTime() >= pauseDeadline || pausedWorker == null || cancelled(pausedWorker)) throw new IllegalStateException("The pause has changed. Refresh the debugger.");
	}
	private JSONObject inspect(long expected, Function<Context, JSONObject> operation) throws Exception {
		var result = new CompletableFuture<JSONObject>();
		synchronized (lock) {
			checkPause(expected);
			if (job != null || inspecting) throw new IllegalStateException("An inspection is pending.");
			job = new Job(operation, result); lock.notifyAll();
		}
		try { return result.get(3, TimeUnit.SECONDS); }
		finally { synchronized (lock) { if (job != null && job.result == result) job = null; } }
	}
	private Frame frame(int index) {
		if (index < 0 || index >= frames.size()) throw new IllegalArgumentException("Invalid frame.");
		var frame = frames.get(frames.size() - index - 1);
		if (frame.worker != pausedWorker) throw new IllegalStateException("Ancestor frame belongs to a running or waiting worker; its live scopes are not inspected across threads.");
		return frame;
	}

    private boolean evaluationFree() {
        synchronized (lock) {
            boolean current = JavascriptDebugger.freeEvaluation();
            if (!current && !rhinoSources.isEmpty()) {
                for (String url : rhinoSources) breakpoints.remove(url);
                rhinoSources.clear();
                notice = "Script breakpoints were cleared because editing-enabled Studio mode is unavailable.";
                event("scriptBreakpointsCleared");
            }
            return pauseFreeEvaluation && current;
        }
    }

	public JSONObject evaluate(int index, long expected, String expression) throws Exception {
        final JavascriptDebugPath path;
        synchronized (lock) {
            checkPause(expected); var selected = frame(index);
            boolean free = evaluationFree();
            if (com.twinsoft.convertigo.engine.Engine.logAudit != null)
                com.twinsoft.convertigo.engine.Engine.logAudit.info("JavaScript debugger administrator evaluation requested mode=" + (free ? "free" : "path") + " frame=" + (selected.participant == null ? "rhino" : "participant"));
            if (expression == null || expression.length() > 4096) throw JavascriptDebugPath.refused();
            path = free ? null : JavascriptDebugPath.parse(expression);
            if (path != null && selected.participant != null && !selected.participant.parsedPathEvaluation) throw JavascriptDebugPath.refused();
        }
		synchronized (lock) { checkPause(expected); frame(index); }
		return inspect(expected, cx -> {
            inspection.work(false);
			var frame = frame(index);
			// A queued command rechecks a mode that may have become more restrictive.
            var parsed = evaluationFree() ? path : path == null ? JavascriptDebugPath.parse(expression) : path;
            if (frame.participant != null) {
                if (parsed != null && !frame.participant.parsedPathEvaluation) throw JavascriptDebugPath.refused();
                return frame.participant.inspection(cx, frame, parsed == null ? "evaluate" : "path", parsed == null ? expression : parsed);
            }
            if (parsed != null) {
                try {
                    Object value = JavascriptDebugProperties.binding(frame.scope, parsed.root(), () -> inspection.work(true));
                    if (value == Undefined.instance && !frame.script.isFunction() && frame.scope.getParentScope() != null)
                        value = JavascriptDebugProperties.binding(frame.scope.getParentScope(), parsed.root(), () -> inspection.work(true));
                    return json("result", preview(parsed.resolve(value, () -> inspection.work(true))));
                } catch (JavascriptDebugResponse.Limit e) { throw e; }
                catch (Exception e) { return json("result", "<opaque>"); }
            }
			var debugger = cx.getDebugger(); var data = cx.getDebuggerContextData();
			boolean interpreted = cx.isInterpretedMode(), generating = cx.isGeneratingDebug();
			cx.setDebugger(null, null); cx.setInterpretedMode(true); cx.setGeneratingDebug(false);
			((Worker) data).evaluationDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
			try {
				var scope = frame.scope;
				// Top-level lexical bindings live in the enclosing scope (as in Rhino Dim.do_eval).
				if (!frame.script.isFunction() && scope.getParentScope() != null) scope = scope.getParentScope();
				Object value; long evaluation = System.nanoTime();
				try { value = cx.compileString(expression, "<debug-expression>", 1, null).exec(cx, scope, frame.thisObject); }
				finally { inspection.exclude(System.nanoTime() - evaluation); }
                inspection.work(false);
				return json("result", preview(value));
			} catch (EvaluationTimeout e) { return json("result", "Evaluation timed out.", "evaluationError", true); }
			catch (org.mozilla.javascript.RhinoException e) { return json("result", e.details(), "evaluationError", true); }
			catch (RuntimeException e) { return json("result", "Evaluation failed: " + e.getClass().getSimpleName(), "evaluationError", true); }
			finally {
				((Worker) data).evaluationDeadline = 0; cx.setDebugger(debugger, data); cx.setInterpretedMode(interpreted); cx.setGeneratingDebug(generating);
			}
		});
	}

	public JSONObject variables(int index, long expected) throws Exception {
		synchronized (lock) { checkPause(expected); frame(index); }
		return inspect(expected, cx -> {
			var frame = frame(index);
			if (frame.participant != null) return frame.participant.inspection(cx, frame, "variables", "");
            var scopes = new JSONArray(); var scope = frame.scope;
            var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Scriptable, Boolean>());
            JSONObject local = json("variables", new JSONArray());
            // Prioritize effective top-level bindings once, rather than spending the quota on duplicate scopes.
            if (!frame.script.isFunction() && scope != null && scope.getParentScope() != null && scope.getParentScope() != frame.providerBoundary) {
                var bindings = new LexicalScope(scope, scope.getParentScope());
                local = inspection.page(bindings, 0);
                try { local.put("name", "Local").put("ref", inspection.reference(bindings)); }
                catch (Exception e) { throw new IllegalStateException("Scope unavailable."); }
                scopes.put(local);
                scope = scope.getParentScope().getParentScope();
            }
            for (int level = scopes.length(); scope != null && scope != frame.providerBoundary && level < 8 && visited.add(scope); level++, scope = scope.getParentScope()) {
                var page = inspection.page(scope, 0);
                try { page.put("name", level == 0 ? "Local" : "Parent " + level).put("ref", inspection.reference(scope)); }
                catch (Exception e) { throw new IllegalStateException("Scope unavailable."); }
                if (level == 0) local = page;
                scopes.put(page);
            }

			var receiverObject = frame.thisObject == frame.providerBoundary ? null : frame.thisObject;
			var receiver = inspection.page(receiverObject, 0);
			return json("scope", local.optJSONArray("variables"), "scopeRef", local.optString("ref"),
					"scopeNextOffset", local.optInt("nextOffset", -1), "scopeReason", local.optString("reason"), "scopes", scopes,
					"this", receiver.optJSONArray("variables"), "thisRef", inspection.reference(receiverObject),
					"thisNextOffset", receiver.optInt("nextOffset", -1), "thisReason", receiver.optString("reason"));
		});
	}

	public JSONObject properties(long expected, String reference, int offset) throws Exception {
		if (offset < 0) throw new IllegalArgumentException("Invalid property page.");
		return inspect(expected, cx -> inspection.page(inspection.value(reference), offset));
	}
	public JSONObject value(long expected, String reference) throws Exception {
		return inspect(expected, cx -> {
			var value = scalarValue(inspection.value(reference));
			if (!primitive(value)) throw new IllegalArgumentException("Not a primitive value.");
			var text = JavascriptDebugRedaction.text(String.valueOf(value));
			return json("value", text.substring(0, Math.min(text.length(), 65_536)), "truncated", text.length() > 65_536);
		});
	}

	private record LexicalScope(Scriptable lexical, Scriptable global) { }
    /** One quota for the whole worker command, including all scopes, skipped keys and previews. */
    private final class Inspection {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final Map<Object, String> references = new java.util.IdentityHashMap<>();
        private final Map<Integer, Continuation> cursors = new LinkedHashMap<>();
        private volatile long generation;
        private int scalarChars, cursorSerial = 1_000_000;
        private Budget budget;
        private final java.util.List<Page> pages = new java.util.ArrayList<>();
        private final class Page {
            final JSONObject result;
            final Continuation continuation;
            final java.util.ArrayDeque<JavascriptDebugProperties.Item> exported;
            Page(JSONObject result, Continuation continuation, java.util.ArrayDeque<JavascriptDebugProperties.Item> exported) {
                this.result = result; this.continuation = continuation; this.exported = exported;
            }
        }
        private final class Budget {
            final long epoch = generation;
            long start = System.nanoTime();
            int examined;
            String reason = "";
            boolean check() {
                if (epoch != generation || closed || !paused || pausedWorker == null || pausedWorker.cancelled.getAsBoolean()) reason = "expired";
                else if (System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(50)) reason = "time";
                else if (examined >= 1000) reason = "candidates";
                return reason.isEmpty();
            }
            boolean candidate() { if (!check()) return false; examined++; return true; }
        }
        private final class Continuation {
            final JavascriptDebugProperties.Cursor iterator;
            final String reference;
            JavascriptDebugProperties.Item pending;
            final java.util.ArrayDeque<java.util.ArrayDeque<JavascriptDebugProperties.Item>> replay = new java.util.ArrayDeque<>();
            JavascriptDebugProperties.Item next() throws Exception {
                if (!replay.isEmpty()) {
                    var head = replay.peekFirst(); var item = head.removeFirst();
                    if (head.isEmpty()) replay.removeFirst();
                    return item;
                }
                if (pending != null) { var item = pending; pending = null; return item; }
                return iterator.next();
            }

            Continuation(String reference, JavascriptDebugProperties.Cursor iterator) { this.reference = reference; this.iterator = iterator; }
        }
        synchronized void begin() { pages.clear(); budget = new Budget(); }
        /** An administrator expression has its own evaluation deadline: its run is not inspection work. */
        void exclude(long nanos) { if (budget != null) budget.start += nanos; }
        void work(boolean candidate) {
            if (budget == null || !(candidate ? budget.candidate() : budget.check()))
                throw new JavascriptDebugResponse.Limit(budget == null ? "expired" : budget.reason);
        }
        JSONObject refused(String reason) {
            return json("refused", true, "partial", true, "reason", reason, "scopes", new JSONArray().put(json("name", "Inspection limited", "variables", new JSONArray(), "limited", true, "reason", reason)), "result", "Inspection response refused; request a smaller qualified page.");
        }
        JSONObject finish(JSONObject result) {
            JSONObject output;
            try {
                if (result != null && result.optBoolean("refused", false)) {
                    String reason = result.optString("reason");
                    if (!Set.of("time", "candidates", "expired", "participant-response-size", "participant-response-shape", "scope-capacity").contains(reason)) reason = "participant-refused";
                    output = retryPages(reason, result);
                } else output = JavascriptDebugResponse.prepare(result == null ? json() : result, this::work);
            }
            catch (JavascriptDebugResponse.Limit e) {
                output = retryPages(e.reason, result);
                try { if (result != null && result.optBoolean("evaluationError", false)) output.put("evaluationError", true); }
                catch (Exception ignored) { }
            }
            try {
                output.put("inspectionBudget", json("candidates", budget.examined,
                    "elapsedNanos", System.nanoTime() - budget.start, "limitCandidates", 1000,
                    "limitMillis", 50, "reason", budget.reason, "cpuNanos", JSONObject.NULL));
                // Fixed-size accounting trailer; user data has already been masked and formatted under the quota.
                if (output instanceof JavascriptDebugResponse.Prepared prepared) prepared.seal();
            } catch (org.codehaus.jettison.json.JSONException e) { throw new IllegalStateException("Inspection result unavailable."); }
            return output;
        }
        /** A response refused during formatting must not skip the rows which were never delivered.
         * Replay whole already-owned batches by linking them, without examining their values again here. */
        private synchronized JSONObject retryPages(String reason, JSONObject original) {
            if (budget.epoch != generation || closed || !paused) return refused("expired");
            if (pages.isEmpty()) return refused(reason);
            var groups = new JSONArray(); JSONObject direct = null;
            try {
                int index = 0;
                for (var page : pages) {
                    if (!page.exported.isEmpty()) page.continuation.replay.addFirst(page.exported);
                    int next = page.result.optInt("nextOffset", -1);
                    if (next < 0 && !page.continuation.replay.isEmpty() && cursors.size() < 256 && cursorSerial < Integer.MAX_VALUE) {
                        next = ++cursorSerial; cursors.put(next, page.continuation);
                    }
                    String label = page.result.optString("name");
                    if (!Set.of("Inputs", "Arguments", "Arguments (source)", "Output", "Local", "Result", "@", "Error", "this").contains(label)) label = "Scope " + (++index);
                    var group = json("name", label, "ref", page.continuation.reference, "variables", new JSONArray(),
                        "partial", true, "reason", reason, "nextOffset", next, "cursor", next < 0 ? "" : Integer.toString(next));
                    groups.put(group);
                    if (page.result == original) direct = group;
                }
                var result = direct == null ? refused(reason).put("scopes", groups) : direct.put("refused", true);
                return result;
            } catch (Exception e) { return refused("response-unavailable"); }
        }
        synchronized void clear() { generation++; scalarChars = 0; values.clear(); references.clear(); cursors.clear(); pages.clear(); }
        synchronized String reference(Object value) {
            if (value == null) return "";
            var existing = references.get(value);
            if (existing != null) return existing;
            if (values.size() >= 4096) return "";
            if (value instanceof String text) { if (scalarChars + text.length() > 262144) return ""; scalarChars += text.length(); }
            var ref = UUID.randomUUID().toString();
            values.put(ref, value); references.put(value, ref); return ref;
        }
        synchronized Object value(String reference) {
            if (!values.containsKey(reference)) throw new IllegalArgumentException("The variable reference has expired. Refresh the variables.");
            return values.get(reference);
        }
        private Object unwrap(Object value) {
            return value instanceof org.mozilla.javascript.NativeJavaObject host && trustedWrapper(value) ? host.unwrap() : value;
        }
        private boolean expandable(Object value) {
            return value instanceof LexicalScope || JavascriptDebugProperties.supported(unwrap(value));
        }
        JSONObject page(Object object, int offset) { return page(object, offset, 100); }
        JSONObject page(Object object, int offset, int limit) {
            synchronized (this) { if (pages.size() >= 20) throw new JavascriptDebugResponse.Limit("scope-capacity"); }
            var result = new JSONArray(); object = unwrap(object);
            String ref = reference(object);
            Continuation cursor;
            try {
                synchronized (this) {
                    cursor = offset == 0 ? null : cursors.get(offset);
                    if (offset != 0 && (cursor == null || !cursor.reference.equals(ref)))
                        return json("variables", result, "nextOffset", -1, "limited", true, "reason", "expired-cursor");
                    if (offset != 0) cursors.remove(offset); // a page token is consumed exactly once
                }
                if (offset == 0) {
                    JavascriptDebugProperties.Cursor adapter;
                    if (object instanceof LexicalScope lexical) {
                        var first = JavascriptDebugProperties.open(lexical.global);
                        var second = JavascriptDebugProperties.open(lexical.lexical);
                        adapter = new JavascriptDebugProperties.Cursor() {
                            boolean parent;
                            final Set<String> seen = new java.util.HashSet<>();
                            public JavascriptDebugProperties.Item next() throws Exception {
                                var item = parent || first == null ? null : first.next();
                                if (item == null) { parent = true; item = second == null ? null : second.next(); }
                                if (seen.size() >= 1000) throw new IllegalStateException("Lexical continuation capacity reached.");
                                if (item != null && !seen.add(item.name())) return new JavascriptDebugProperties.Item(item.name(), null, false, false);
                                return item;
                            }
                        };
                    } else adapter = JavascriptDebugProperties.open(object);
                    if (adapter == null) return json("variables", result, "nextOffset", -1, "limited", object != null,
                        "reason", "opaque", "view", "non-atomic");
                    cursor = new Continuation(ref, adapter);
                }
                var exported = new java.util.ArrayDeque<JavascriptDebugProperties.Item>();
                int rows = 0; boolean end = false;
                while (rows < limit && budget.candidate()) {
                    var item = cursor.next();
                    if (!budget.check()) { cursor.pending = item; break; }
                    if (item == null) { end = true; break; }
                    if (!item.visible()) continue;
                    result.put(item.accessor() ? json("name", boundedName(item.name()), "value", "<accessor>",
                        "type", "accessor", "expandable", false) : node(item.name(), item.value()));
                    exported.addLast(item); rows++;
                    if (!budget.check()) break;
                }
                int next = -1;
                synchronized (this) {
                    if (!end && budget.epoch == generation && cursors.size() < 256) {
                        if (cursorSerial < Integer.MAX_VALUE) {
                            next = ++cursorSerial;
                            cursors.put(next, cursor);
                        }
                    }
                }
                // nextOffset is retained as a wire-compatible name; its value is an opaque, single-use page token.
                var page = json("variables", result, "nextOffset", next, "cursor", next < 0 ? "" : Integer.toString(next), "partial", !end,
                    "reason", budget.reason.isEmpty() ? !end ? next < 0 ? "cursor-capacity" : "page" : "" : budget.reason,
                    "view", "non-atomic");
                synchronized (this) { if (budget.epoch == generation) pages.add(new Page(page, cursor, exported)); }
                return page;
            } catch (IllegalArgumentException e) { throw e; }
            catch (Exception e) {
                synchronized (this) { cursors.remove(offset); }
                return json("variables", result, "nextOffset", -1, "partial", true, "reason", "changed-or-unsupported", "view", "non-atomic");
            }
        }
        private JSONObject node(String name, Object value) {
            if (JavascriptDebugRedaction.sensitive(name)) return json("name", boundedName(name), "value", "[redacted]", "type", "string", "ref", "", "expandable", false, "fullValue", false, "limited", false);
            var scalar = scalarValue(value);
            boolean large = largeScalar(scalar);
            boolean absent = scalar == null || scalar == Undefined.instance || scalar == Scriptable.NOT_FOUND;
            boolean expand = expandable(value), full = !large && primitive(scalar) && String.valueOf(scalar).length() > 500;
            String ref = expand || full ? reference(full ? scalar : value) : "";
            return json("name", boundedName(name), "value", preview(value), "type", type(value), "ref", ref,
                "expandable", expand && !ref.isEmpty(), "fullValue", full && !ref.isEmpty(),
                "limited", !absent && (large || !primitive(scalar) && !expand || (expand || full) && ref.isEmpty()));
        }
    }
	private static String boundedName(String name) {
        if (name.length() > 65536) return "<long property name: inspection limited>";
        name = JavascriptDebugRedaction.text(name);
        return name.length() > 500 ? name.substring(0, 500) + "…" : name;
    }
    static boolean trustedWrapper(Object value) {
        return value != null && (value.getClass() == org.mozilla.javascript.NativeJavaObject.class
            || value.getClass() == org.mozilla.javascript.NativeJavaArray.class
            || value.getClass() == org.mozilla.javascript.NativeJavaMap.class
            || value.getClass() == org.mozilla.javascript.NativeJavaList.class);
    }
	private static String type(Object value) {
		if (value == null) return "null";
		if (value == Undefined.instance || value == Scriptable.NOT_FOUND) return "undefined";
		if (value instanceof org.mozilla.javascript.NativeJavaObject host && trustedWrapper(value)) {
			var java = host.unwrap(); return java == null ? "null" : java.getClass().getName();
		}
		if (value instanceof NativeArray || value.getClass().isArray()) return "array";
		if (value instanceof org.mozilla.javascript.Function) return "function";
		if (value instanceof Number) return "number";
		if (value instanceof Boolean) return "boolean";
		if (primitive(value)) return "string";
		if (value instanceof Scriptable) return "object";
		return value.getClass().getName();
	}

	private static boolean primitive(Object value) {
		return value instanceof String || value instanceof org.mozilla.javascript.ConsString || value instanceof Boolean
				|| value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
				|| value instanceof Float || value instanceof Double
				|| value != null && (value.getClass() == java.math.BigInteger.class || value.getClass() == java.math.BigDecimal.class)
				|| value instanceof Character;
	}
	/** Unwrap only trusted scalar values; never call an arbitrary Java object's toString. */
	private static Object scalarValue(Object value) {
		if (value instanceof org.mozilla.javascript.NativeJavaObject host && trustedWrapper(value)) {
			var java = host.unwrap();
			if (java == null || primitive(java)) return java;
		}
		return value;
	}
	private static boolean largeScalar(Object value) {
        if (value instanceof CharSequence text && (value instanceof String || value instanceof org.mozilla.javascript.ConsString)) return text.length() > 65536;
        if (value != null && value.getClass() == java.math.BigInteger.class) return ((java.math.BigInteger)value).bitLength() > 4096;
        if (value != null && value.getClass() == java.math.BigDecimal.class) return ((java.math.BigDecimal)value).unscaledValue().bitLength() > 4096;
        return false;
    }
    static String preview(Object value) {
		value = scalarValue(value);
        if (largeScalar(value)) return "<large scalar: inspection limited>";
		if (value == null) return "null";
		if (value == Undefined.instance || value == Scriptable.NOT_FOUND) return "undefined";
		if (value instanceof Double || value instanceof Float) return JavascriptDebugRedaction.text(org.mozilla.javascript.ScriptRuntime.numberToString(((Number) value).doubleValue(), 10));
		if (primitive(value)) {
			String text = JavascriptDebugRedaction.text(String.valueOf(value)); return text.length() > 500 ? text.substring(0, 500) + "…" : text;
		}
		if (value instanceof org.mozilla.javascript.Function) return "[Function]";
		if (value instanceof NativeArray) return "[Array]";
		if (value instanceof NativeObject) return "[Object]";
		if (value instanceof org.mozilla.javascript.NativeJavaObject host && trustedWrapper(value)) {
			var java = host.unwrap(); return java == null ? "null" : "[Java " + java.getClass().getName() + "]";
		}
		return "[Host object]";
	}

	public void registerSource(com.twinsoft.convertigo.beans.core.DatabaseObject dbo, String scriptName) {
		String url = dbo.getShortQName() + "-" + scriptName;
		String property = scriptName.endsWith("-smartType") ? scriptName.substring(0, scriptName.length() - 10) : scriptName;
		if (dbo instanceof com.twinsoft.convertigo.beans.core.Transaction && scriptName.equals(dbo.getName())) property = "handlers";
		else if (dbo instanceof com.twinsoft.convertigo.beans.steps.LogStep && scriptName.equals("LogStep")
				|| dbo instanceof com.twinsoft.convertigo.beans.steps.ExceptionStep && scriptName.equals("message")) property = "expression";
		else if (scriptName.equals("ctxName")) property = "contextName";
		int offset = com.twinsoft.convertigo.engine.util.CopilotHelper.addInstruction(dbo, "").split("\n", -1).length - 1;
		synchronized (lock) {
			if (closed) return;
			if (locations.size() >= MAX_SOURCES && !locations.containsKey(url)) locations.remove(locations.keySet().iterator().next());
			locations.put(url, json("id", dbo.getFullQName(), "property", property, "offset", offset,
					"label", dbo.getQName().replace(".", " › ") + " › " + property));
		}
	}
	private JSONObject location(String url) {
		var known = locations.get(url);
		if (known != null) return known;
		return url.contains("/") ? json("label", url.replaceFirst("/", " › "), "id", url, "file", true, "offset", com.twinsoft.convertigo.engine.util.CopilotHelper.addInstruction(null, "").split("\n", -1).length - 1) : json("label", url);
	}
	public boolean breakpoint(String url, int line, boolean set) {
		return breakpoint(url, line, set, true);
	}
	public boolean hasRhinoBreakpoints() { synchronized (lock) { return !rhinoSources.isEmpty(); } }
	public boolean breakpoint(String url, int line, boolean set, boolean rhino) {
		if (url == null || url.length() > 1024 || line < 1 || line > 1_000_000) throw new IllegalArgumentException("Invalid breakpoint.");
		synchronized (lock) {
			var source = sources.get(url);
			if (set && source != null && !source.lines.contains(line)) return false;
			if (set) {
				if (breakpoints.size() >= 64 && !breakpoints.containsKey(url)) throw new IllegalArgumentException("Too many breakpoint sources.");
				var lines = breakpoints.computeIfAbsent(url, key -> new TreeSet<>());
				if (lines.size() >= 256) throw new IllegalArgumentException("Too many breakpoints.");
				lines.add(line);
			} else if (breakpoints.containsKey(url)) { breakpoints.get(url).remove(line); if (breakpoints.get(url).isEmpty()) breakpoints.remove(url); }
			if (!breakpoints.containsKey(url)) rhinoSources.remove(url);
			else if (rhino) rhinoSources.add(url);
			event("breakpoints"); return true;
		}
	}
	public JSONArray breakpoints(String url) { synchronized (lock) { return array(breakpoints.getOrDefault(url, Set.of())); } }
	public JSONObject source(String url) {
		synchronized (lock) {
			var source = sources.get(url); if (source == null) throw new IllegalArgumentException("Source unavailable. Run the debug request first.");
			return JavascriptDebugRedaction.output(json("url", url, "source", source.text, "breakable", array(source.lines), "breakpoints", breakpoints(url), "location", location(url)));
		}
	}
	public JSONObject state() {
		synchronized (lock) {
			evaluationFree();
			var stack = new JSONArray();
			if (paused) for (int i = frames.size() - 1; i >= 0; i--) {
				var frame = frames.get(i);
				stack.put(json("url", frame.url(), "line", frame.line,
					"kind", frame.participant == null ? "rhino" : frame.participant.kind,
					"frameId", frame.frameId, "phase", frame.phase,
					"inspectable", frame.worker == pausedWorker,
					"activation", json("id", frame.activation, "parent", frame.parentActivation, "rootRequest", frame.worker.root,
						"worker", frame.worker.id, "invocation", frame.participant == null ? frame.activation : frame.participant.invocation,
						"depth", frame.depth, "iteration", frame.descriptor == null ? 0 : frame.descriptor.optLong("iteration", 0)),
					"function", frame.participant == null ? frame.script.getFunctionName() : frame.descriptor.optString("label", frame.participant.kind),
					"location", location(frame.url()), "span", frame.descriptor == null ? JSONObject.NULL : frame.descriptor.opt("span"),
					"origins", frame.descriptor == null ? JSONObject.NULL : frame.descriptor.opt("origins")));
			}
			return JavascriptDebugRedaction.output(json("attached", !closed, "sessionId", id, "serial", serial, "pauseId", pauseId,
                    "transport", JavascriptDebugger.participantContract(),
                    "currentLevel", closed || workers.isEmpty() ? JSONObject.NULL : workers.values().iterator().next().rhino ? 2 : 1,
                    "breakpoints", breakpointLocations(),
                    "running", !workers.isEmpty(), "workers", workers.size(), "notice", notice, "breakpointChanges", array(breakpointChanges), "pauseTimeoutMillis", pauseTimeoutMillis, "stopped", paused, "breakOnExceptions", breakOnExceptions,
					"breakOnEnter", breakOnEnter, "breakOnReturn", breakOnReturn,
					"studioRequests", JavascriptDebugger.studioSession() == this,
					"sources", array(sources.keySet()), "sourceLocations", sourceLocations(), "frames", stack, "events", array(events)));
		}
	}
	/** Snapshot the existing breakpoint map; views never keep their own registry. */
	private JSONObject breakpointLocations() {
		var result = new JSONObject();
		try { for (var entry : breakpoints.entrySet()) result.put(entry.getKey(), array(entry.getValue())); }
		catch (Exception e) { throw new IllegalStateException("Cannot describe debugger breakpoints."); }
		return result;
	}
	private JSONObject sourceLocations() {
		var result = new JSONObject();
		try { for (var url : sources.keySet()) result.put(url, location(url)); }
		catch (Exception e) { throw new IllegalStateException("Cannot describe debugger sources."); }
		return result;
	}
	private static JSONArray array(Iterable<?> values) {
		var result = new JSONArray(); for (var value : values) result.put(value); return result;
	}
	static JSONObject json(Object... pairs) {
		var json = new JSONObject();
		try { for (int i = 0; i < pairs.length; i += 2) json.put((String) pairs[i], pairs[i + 1] instanceof String text && !JavascriptDebugRedaction.handle((String)pairs[i]) ? JavascriptDebugRedaction.text(text) : pairs[i + 1]); }
		catch (Exception e) { throw new IllegalStateException("Cannot build debugger response."); }
		return json;
	}
}
