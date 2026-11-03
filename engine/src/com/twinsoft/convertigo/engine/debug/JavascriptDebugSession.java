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
	private final Set<String> rhinoSources = new java.util.HashSet<>();
	private final Map<Thread, Worker> workers = new LinkedHashMap<>();
	private List<Frame> frames = List.of();
	private Worker pausedWorker, stepWorker;
	private final long pauseTimeoutMillis;
	private long pauseDeadline;
	private String notice = "";
	public static final String UNSUPPORTED_RUNTIME = "Prepared provider runtimes and shared compiled modules cannot be debugged. Run this request with the debugger detached.";
	private static final class Worker {
		final List<Frame> frames = new ArrayList<>();
		volatile long evaluationDeadline;
		Scriptable providerBoundary;
		final Map<String, Scriptable> privateBoundaries = new LinkedHashMap<>();
	}
	public JavascriptDebugSession() { this(JavascriptDebugger.pauseTimeoutMillis()); }
	JavascriptDebugSession(long pauseTimeoutMillis) { this.pauseTimeoutMillis = pauseTimeoutMillis; }
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
	private boolean paused, breakNext, breakOnExceptions, breakOnEnter, breakOnReturn, closed, inspecting, stepAfterAllowed;

	private Throwable lastException;
	private Job job;


	private record Source(String text, Set<Integer> lines) { }
	private record Job(Function<Context, JSONObject> operation, CompletableFuture<JSONObject> result) { }
	private static final class EvaluationTimeout extends Error {
		private static final long serialVersionUID = 1L;
	}

	public String id() { return id; }
	public void touch() { synchronized (lock) { seen = System.currentTimeMillis(); } }
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
			worker = new Worker(); workers.put(owner, worker); event("started");
			return worker;
		}
	}
	void finishParticipantWorker() { finish(); }
	private AutoCloseable enter(boolean child) {
		var worker = new Worker();
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
			if (pausedWorker == worker) { paused = false; pausedWorker = null; frames = List.of(); failJob(); }
			if (workers.isEmpty()) { lastException = null; stepWorker = null; event("finished"); }
			lock.notifyAll();
		}
	}

	@Override public void close() {
		synchronized (lock) {
			closed = true; paused = false; breakNext = false;
			failJob(); frames = List.of(); pausedWorker = null; stepWorker = null; sources.clear(); locations.clear(); inspection.clear(); breakpoints.clear(); rhinoSources.clear(); sourceChars = 0;
			event("detached"); lock.notifyAll();
		}
	}

	private void failJob() {
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
	public final class Participant {
		private final String kind;
		private final Worker worker;
		private final org.mozilla.javascript.Function inspect;
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
				worker.frames.add(new Frame(this, frameId, info, worker));
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
			if (!Set.of("before", "after", "error").contains(phase)) throw new IllegalArgumentException("Invalid participant phase.");
			boolean stop;
			synchronized (lock) {
				if (closed) return;
				var frame = top(frameId); if (frame == null) return;
				frame.phase = phase;
				stop = !inspecting && !paused && ("error".equals(phase)
					|| "before".equals(phase) && breakpoints.getOrDefault(frame.url(), Set.of()).contains(frame.line)
					|| breakNext && ("before".equals(phase) || stepAfterAllowed && "after".equals(phase))
						&& (stepWorker == null || stepWorker == worker) && (stopDepth < 0 || worker.frames.size() <= stopDepth));
			}
			if (stop) pause(Context.getCurrentContext(), "participant", worker);
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
			return registry.page(value, 0, 20).put("name", name).put("ref", registry.reference(value)).toString();
		}
		private JSONObject inspection(Context cx, Frame frame, String action, String expression) {
			var debugger = cx.getDebugger(); var data = cx.getDebuggerContextData();
			cx.setDebugger(null, null); worker.evaluationDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
			try {
				var scope = inspect.getParentScope();
				var value = inspect.call(cx, scope, scope, new Object[] { frame.frameId, action, expression });
				var text = Context.toString(value);
				if (text.length() > MAX_SOURCE_CHARS) throw new IllegalArgumentException("Participant inspection limit.");
				return new JSONObject(text);
			} catch (EvaluationTimeout e) { return json("result", "Evaluation timed out.", "evaluationError", true); }
			catch (Exception e) { throw new IllegalStateException("Participant inspection failed."); }
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
			synchronized (lock) { this.scope = scope; thisObject = self; worker.frames.add(this); }
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
	private void pause(Context cx, String reason) { pause(cx, reason, (Worker) cx.getDebuggerContextData()); }
	private void pause(Context cx, String reason, Worker worker) {
		try (var requestPause = com.twinsoft.convertigo.beans.core.RequestableObject.pauseCurrentRequestable()) {
			synchronized (lock) {
				if (closed || paused) return;
				pausedWorker = worker; frames = pausedWorker.frames;
				pauseDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(pauseTimeoutMillis);
				inspection.clear();
				paused = true; breakNext = false; stopDepth = -1; pauseId++; event(reason);
			}
			while (true) {
				Job pending;
				synchronized (lock) {
					if (!paused || closed) { inspection.clear(); return; }
					if (System.nanoTime() >= pauseDeadline) {
						paused = false; breakNext = false; stepWorker = null; failJob();
						notice = "The maximum pause duration was reached. The request resumed automatically.";
						inspection.clear(); event("autoResumed"); lock.notifyAll(); return;
					}
					if (System.currentTimeMillis() - seen >= IDLE_MILLIS) { close(); return; }
					if (job == null) {
						try { lock.wait(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); close(); }
						continue;
					}
					pending = job; job = null; inspecting = true;
				}
				JSONObject result = null; Throwable failure = null;
				try { result = pending.operation.apply(cx); }
				catch (RuntimeException | Error e) { failure = new IllegalStateException("Inspection failed."); }
				finally { synchronized (lock) { inspecting = false; lock.notifyAll(); } }
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
		if (!paused || closed || expected != pauseId) throw new IllegalStateException("The pause has changed. Refresh the debugger.");
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
		return frames.get(frames.size() - index - 1);
	}

	public JSONObject evaluate(int index, long expected, String expression) throws Exception {
		if (expression == null || expression.length() > 4096) throw new IllegalArgumentException("Expression limit: 4096 characters.");
		return inspect(expected, cx -> {
			var frame = frame(index);
			if (frame.participant != null) return frame.participant.inspection(cx, frame, "evaluate", expression);
			var debugger = cx.getDebugger(); var data = cx.getDebuggerContextData();
			boolean interpreted = cx.isInterpretedMode(), generating = cx.isGeneratingDebug();
			cx.setDebugger(null, null); cx.setInterpretedMode(true); cx.setGeneratingDebug(false);
			((Worker) data).evaluationDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
			try {
				var scope = frame.scope;
				// Top-level lexical bindings live in the enclosing scope (as in Rhino Dim.do_eval).
				if (!frame.script.isFunction() && scope.getParentScope() != null) scope = scope.getParentScope();
				var script = cx.compileString(expression, "<debug-expression>", 1, null);
				return json("result", preview(script.exec(cx, scope, frame.thisObject)));
			} catch (EvaluationTimeout e) { return json("result", "Evaluation timed out.", "evaluationError", true); }
			catch (org.mozilla.javascript.RhinoException e) { return json("result", e.details(), "evaluationError", true); }
			catch (RuntimeException e) { return json("result", "Evaluation failed: " + e.getClass().getSimpleName(), "evaluationError", true); }
			finally {
				((Worker) data).evaluationDeadline = 0; cx.setDebugger(debugger, data); cx.setInterpretedMode(interpreted); cx.setGeneratingDebug(generating);
			}
		});
	}

	public JSONObject variables(int index, long expected) throws Exception {
		return inspect(expected, cx -> {
			var frame = frame(index);
			if (frame.participant != null) return frame.participant.inspection(cx, frame, "variables", "");
			var scopes = new JSONArray(); var scope = frame.scope;
			var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Scriptable, Boolean>());
			JSONObject local = json("variables", new JSONArray());
			for (int level = 0; scope != null && scope != frame.providerBoundary && level < 8 && visited.add(scope); level++, scope = scope.getParentScope()) {
				var page = inspection.page(scope, 0);
				page = json("name", level == 0 ? "Local" : "Parent " + level, "ref", inspection.reference(scope),
						"variables", page.optJSONArray("variables"), "nextOffset", page.optInt("nextOffset", -1));
				if (level == 0) local = page;
				scopes.put(page);
			}
			// Rhino's top-level lexical and global bindings live in adjacent scopes. Keep both visible.
			if (!frame.script.isFunction() && scopes.length() > 1) {
				var bindings = new LexicalScope(frame.scope, frame.scope.getParentScope());
				var page = inspection.page(bindings, 0);
				local = json("variables", page.optJSONArray("variables"), "ref", inspection.reference(bindings),
						"nextOffset", page.optInt("nextOffset", -1));
			}
			var receiverObject = frame.thisObject == frame.providerBoundary ? null : frame.thisObject;
			var receiver = inspection.page(receiverObject, 0);
			return json("scope", local.optJSONArray("variables"), "scopeRef", local.optString("ref"),
					"scopeNextOffset", local.optInt("nextOffset", -1), "scopes", scopes,
					"this", receiver.optJSONArray("variables"), "thisRef", inspection.reference(receiverObject),
					"thisNextOffset", receiver.optInt("nextOffset", -1));
		});
	}

	public JSONObject properties(long expected, String reference, int offset) throws Exception {
		if (offset < 0 || offset > 1_000_000) throw new IllegalArgumentException("Invalid property page.");
		return inspect(expected, cx -> inspection.page(inspection.value(reference), offset));
	}
	public JSONObject value(long expected, String reference) throws Exception {
		return inspect(expected, cx -> {
			var value = scalarValue(inspection.value(reference));
			if (!primitive(value)) throw new IllegalArgumentException("Not a primitive value.");
			var text = String.valueOf(value);
			return json("value", text.substring(0, Math.min(text.length(), 65_536)), "truncated", text.length() > 65_536);
		});
	}

	private record LexicalScope(Scriptable lexical, Scriptable global) { }
	private record Binding(Scriptable owner, Object id) { }

	/** Shallow pages only. References are opaque and valid only during the current pause. */
	private static final class Inspection {
		private final Map<String, Object> values = new LinkedHashMap<>();
		private final Map<Object, String> references = new java.util.IdentityHashMap<>();
		private long serial;
		synchronized void clear() { values.clear(); references.clear(); }
		synchronized String reference(Object value) {
			if (value == null) return "";
			var existing = references.get(value);
			if (existing != null) return existing;
			if (values.size() >= 4096) return "";
			var ref = Long.toString(++serial);
			values.put(ref, value); references.put(value, ref); return ref;
		}
		synchronized Object value(String reference) {
			if (!values.containsKey(reference)) throw new IllegalArgumentException("The variable reference has expired. Refresh the variables.");
			return values.get(reference);
		}
		private static boolean ordinary(Scriptable value) {
			return value.getClass() == NativeObject.class || value instanceof NativeCall
					|| value.getClass().getName().equals("org.mozilla.javascript.Interpreter$DebugScope");
		}
		private static Object unwrap(Object value) {
			return value instanceof org.mozilla.javascript.NativeJavaObject host ? host.unwrap() : value;
		}
		private static boolean expandable(Object value) {
			value = unwrap(value);
			return value != null && (value.getClass().isArray() || value instanceof NativeArray
					|| value instanceof Scriptable script && ordinary(script)
					|| !(value instanceof Scriptable) && !primitive(value) && value.getClass().getFields().length > 0);
		}
		synchronized JSONObject page(Object object, int offset) { return page(object, offset, 100); }
		synchronized JSONObject page(Object object, int offset, int limit) {
			var result = new JSONArray(); object = unwrap(object);
			int count = 0;
			if (object instanceof LexicalScope scope) {
				var bindings = new LinkedHashMap<String, Binding>();
				for (var owner : new Scriptable[] { scope.lexical, scope.global }) {
					if (!ordinary(owner)) continue;
					for (var id : owner.getIds()) if (id instanceof String || id instanceof Number)
						bindings.put(String.valueOf(id), new Binding(owner, id));
				}
				count = bindings.size(); int index = 0;
				for (var binding : bindings.values()) {
					if (index >= offset + limit) break;
					if (index++ >= offset) result.put(property(binding.owner, binding.id));
				}
			} else if (object != null && object.getClass().isArray()) {
				count = java.lang.reflect.Array.getLength(object);
				for (int i = offset; i < count && i < offset + limit; i++) result.put(node(Integer.toString(i), java.lang.reflect.Array.get(object, i)));
			} else if (object instanceof NativeArray array) {
				count = (int)Math.min(array.getLength(), 1_000_000);
				for (int i = offset; i < count && i < offset + limit; i++) result.put(property(array, i));
			} else if (object instanceof Scriptable scope && ordinary(scope)) {
				var ids = scope.getIds(); count = ids.length;
				for (int i = offset; i < count && i < offset + limit; i++) {
					var id = ids[i];
					if (id instanceof String || id instanceof Number) result.put(property(scope, id));
				}
			} else if (object != null && !(object instanceof Scriptable) && !primitive(object)) {
				var fields = java.util.Arrays.stream(object.getClass().getFields())
						.filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers())).toList();
				count = fields.size();
				for (int i = offset; i < count && i < offset + limit; i++) {
					var field = fields.get(i);
					try { result.put(node(field.getName(), field.get(object))); }
					catch (IllegalAccessException | RuntimeException e) { result.put(node(field.getName(), "<inaccessible>")); }
				}
			}
			return json("variables", result, "nextOffset", offset + limit < count ? offset + limit : -1);
		}
		private JSONObject property(Scriptable object, Object id) {
			boolean numeric = id instanceof Number; int index = numeric ? ((Number) id).intValue() : 0;
			String name = numeric ? null : (String) id;
			if (object instanceof ScriptableObject nativeObject) {
				var getter = nativeObject.getGetterOrSetter(name, index, false);
				var setter = nativeObject.getGetterOrSetter(name, index, true);
				if (getter != null && getter != Undefined.instance || setter != null && setter != Undefined.instance)
					return json("name", boundedName(String.valueOf(id)), "value", "<accessor>", "type", "accessor", "expandable", false);
			}
			return node(String.valueOf(id), numeric ? object.get(index, object) : object.get(name, object));
		}
		private JSONObject node(String name, Object value) {
			var scalar = scalarValue(value);
			boolean expand = expandable(value), full = primitive(scalar) && String.valueOf(scalar).length() > 500;
			String ref = expand || full ? reference(full ? scalar : value) : "";
			return json("name", boundedName(name), "value", preview(value), "type", type(value), "ref", ref,
					"expandable", expand && !ref.isEmpty(), "fullValue", full && !ref.isEmpty(), "limited", (expand || full) && ref.isEmpty());
		}
	}
	private static String boundedName(String name) { return name.length() > 500 ? name.substring(0, 500) + "…" : name; }
	private static String type(Object value) {
		if (value == null) return "null";
		if (value == Undefined.instance || value == Scriptable.NOT_FOUND) return "undefined";
		if (value instanceof org.mozilla.javascript.NativeJavaObject host) {
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
		if (value instanceof org.mozilla.javascript.NativeJavaObject host) {
			var java = host.unwrap();
			if (java == null || primitive(java)) return java;
		}
		return value;
	}
	static String preview(Object value) {
		value = scalarValue(value);
		if (value == null) return "null";
		if (value == Undefined.instance || value == Scriptable.NOT_FOUND) return "undefined";
		if (value instanceof Double || value instanceof Float) return org.mozilla.javascript.ScriptRuntime.numberToString(((Number) value).doubleValue(), 10);
		if (primitive(value)) {
			String text = String.valueOf(value); return text.length() > 500 ? text.substring(0, 500) + "…" : text;
		}
		if (value instanceof org.mozilla.javascript.Function) return "[Function]";
		if (value instanceof NativeArray) return "[Array]";
		if (value instanceof NativeObject) return "[Object]";
		if (value instanceof org.mozilla.javascript.NativeJavaObject host) {
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
			return json("url", url, "source", source.text, "breakable", array(source.lines), "breakpoints", breakpoints(url), "location", location(url));
		}
	}
	public JSONObject state() {
		synchronized (lock) {
			var stack = new JSONArray();
			if (paused) for (int i = frames.size() - 1; i >= 0; i--) {
				var frame = frames.get(i);
				stack.put(json("url", frame.url(), "line", frame.line,
					"kind", frame.participant == null ? "rhino" : frame.participant.kind,
					"frameId", frame.frameId, "phase", frame.phase,
					"function", frame.participant == null ? frame.script.getFunctionName() : frame.descriptor.optString("label", frame.participant.kind),
					"location", location(frame.url())));
			}
			return json("attached", !closed, "sessionId", id, "serial", serial, "pauseId", pauseId,
					"running", !workers.isEmpty(), "workers", workers.size(), "notice", notice, "pauseTimeoutMillis", pauseTimeoutMillis, "stopped", paused, "breakOnExceptions", breakOnExceptions,
					"breakOnEnter", breakOnEnter, "breakOnReturn", breakOnReturn,
					"studioRequests", JavascriptDebugger.studioSession() == this,
					"sources", array(sources.keySet()), "sourceLocations", sourceLocations(), "frames", stack, "events", array(events));
		}
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
		try { for (int i = 0; i < pairs.length; i += 2) json.put((String) pairs[i], pairs[i + 1]); }
		catch (Exception e) { throw new IllegalStateException("Cannot build debugger response."); }
		return json;
	}
}
