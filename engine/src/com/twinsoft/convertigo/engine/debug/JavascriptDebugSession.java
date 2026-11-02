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
	private final Map<String, Set<Integer>> breakpoints = new LinkedHashMap<>();
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
	}
	public JavascriptDebugSession() { this(JavascriptDebugger.pauseTimeoutMillis()); }
	JavascriptDebugSession(long pauseTimeoutMillis) { this.pauseTimeoutMillis = pauseTimeoutMillis; }
	public void unsupportedRuntime() { synchronized (lock) { notice = UNSUPPORTED_RUNTIME; event("unsupportedRuntime"); } }
	private final List<JSONObject> events = new ArrayList<>();
	private long serial, pauseId, seen = System.currentTimeMillis();
	private int sourceChars, stopDepth = -1;
	private boolean paused, breakNext, breakOnExceptions, breakOnEnter, breakOnReturn, closed, inspecting;

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
			failJob(); frames = List.of(); pausedWorker = null; stepWorker = null; sources.clear(); breakpoints.clear(); sourceChars = 0;
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
		synchronized (lock) { return closed ? null : new Frame(script, (Worker) cx.getDebuggerContextData()); }
	}

	private final class Frame implements DebugFrame {
		final DebuggableScript script;
		final Worker worker;
		Scriptable scope, thisObject;
		int line;
		Frame(DebuggableScript script, Worker worker) { this.script = script; this.worker = worker; }
		String url() { return script.getSourceName() == null ? "<script>" : script.getSourceName(); }
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
	private void pause(Context cx, String reason) {
		synchronized (lock) {
			if (closed || paused) return;
			pausedWorker = (Worker) cx.getDebuggerContextData(); frames = pausedWorker.frames;
			pauseDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(pauseTimeoutMillis);
			paused = true; breakNext = false; stopDepth = -1; pauseId++; event(reason);
		}
		while (true) {
			Job pending;
			synchronized (lock) {
				if (!paused || closed) return;
				if (System.nanoTime() >= pauseDeadline) {
					paused = false; breakNext = false; stepWorker = null; failJob();
					notice = "The maximum pause duration was reached. The request resumed automatically.";
					event("autoResumed"); lock.notifyAll(); return;
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

	public void resume(String action, long expectedPause) {
		if (!Set.of("go", "stepInto", "stepOver", "stepOut").contains(action)) throw new IllegalArgumentException("Invalid resume action.");
		synchronized (lock) {
			checkPause(expectedPause);
			if (job != null || inspecting) throw new IllegalStateException("An inspection is pending.");
			stopDepth = switch (action) { case "stepOver" -> frames.size(); case "stepOut" -> frames.size() - 1; default -> -1; };
			stepWorker = "go".equals(action) ? null : pausedWorker;
			breakNext = !"go".equals(action); paused = false; event("resumed"); lock.notifyAll();
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
			var frame = frame(index); var debugger = cx.getDebugger(); var data = cx.getDebuggerContextData();
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
			catch (RuntimeException e) { return json("result", "Evaluation failed: " + e.getClass().getSimpleName(), "evaluationError", true); }
			finally {
				((Worker) data).evaluationDeadline = 0; cx.setDebugger(debugger, data); cx.setInterpretedMode(interpreted); cx.setGeneratingDebug(generating);
			}
		});
	}

	public JSONObject variables(int index, long expected) throws Exception {
		return inspect(expected, cx -> {
			var frame = frame(index); var scopes = new JSONArray(); var scope = frame.scope;
			var tree = new Inspection();
			if (!frame.script.isFunction() && scope != null && scope.getParentScope() != null) {
				var lexical = tree.properties(scope, 0);
				var global = tree.properties(scope.getParentScope(), 0);
				var actualNames = new java.util.HashSet<String>();
				for (int i = 0; i < global.length(); i++) actualNames.add(global.optJSONObject(i).optString("name"));
				var bindings = new JSONArray();
				for (int i = 0; i < lexical.length(); i++) {
					var value = lexical.optJSONObject(i);
					if (!actualNames.contains(value.optString("name"))) bindings.put(value);
				}
				for (int i = 0; i < global.length(); i++) bindings.put(global.optJSONObject(i));
				scopes.put(json("name", "Local", "variables", bindings));
				return json("scope", bindings, "scopes", scopes, "this", tree.properties(frame.thisObject, 0));
			}
			var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Scriptable, Boolean>());
			JSONArray locals = new JSONArray();
			for (int level = 0; scope != null && level < 8 && visited.add(scope); level++, scope = scope.getParentScope()) {
				var variables = tree.properties(scope, 0); if (level == 0) locals = variables;
				scopes.put(json("name", level == 0 ? "Local" : "Parent " + level, "variables", variables));
			}
			return json("scope", locals, "scopes", scopes, "this", tree.properties(frame.thisObject, 0));
		});
	}

	/** Bounded snapshots, with no user getters, methods, proxies or Java string conversion. */
	private static final class Inspection {
		private int remaining = 1000;
		private final Set<Object> path = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		private static boolean ordinary(Scriptable value) {
			return value.getClass() == NativeObject.class || value instanceof NativeCall
					|| value.getClass().getName().equals("org.mozilla.javascript.Interpreter$DebugScope");
		}
		JSONArray properties(Object object, int depth) {
			var result = new JSONArray();
			if (object == null || depth > 3 || remaining <= 0 || !path.add(object)) return result;
			Object identity = object;
			try {
				if (object instanceof org.mozilla.javascript.NativeJavaObject host) object = host.unwrap();
				if (object != null && object.getClass().isArray()) {
					int count = java.lang.reflect.Array.getLength(object);
					for (int i = 0; i < count && i < 100 && remaining > 0; i++) result.put(node(Integer.toString(i), java.lang.reflect.Array.get(object, i), depth));
				} else if (object instanceof NativeArray array) {
					for (int i = 0; i < array.getLength() && i < 100 && remaining > 0; i++) result.put(property(array, i, depth));
				} else if (object instanceof Scriptable scope && ordinary(scope)) {
					for (var id : scope.getIds()) {
						if (result.length() >= 100 || remaining <= 0) break;
						if (id instanceof String || id instanceof Number) result.put(property(scope, id, depth));
					}
				} else if (object != null && !(object instanceof Scriptable) && !primitive(object)) {
					for (var field : object.getClass().getFields()) {
						if (result.length() >= 100 || remaining <= 0) break;
						if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
						try { result.put(node(field.getName(), field.get(object), depth)); }
						catch (IllegalAccessException | RuntimeException e) { result.put(node(field.getName(), "<inaccessible>", depth)); }
					}
				}
			} finally { path.remove(identity); }
			return result;
		}
		private JSONObject property(Scriptable object, Object id, int depth) {
			boolean numeric = id instanceof Number; int index = numeric ? ((Number) id).intValue() : 0;
			String name = numeric ? null : (String) id;
			if (object instanceof ScriptableObject nativeObject) {
				var getter = nativeObject.getGetterOrSetter(name, index, false);
				if (getter != null && getter != Undefined.instance) return node(String.valueOf(id), "<accessor>", depth);
			}
			return node(String.valueOf(id), numeric ? object.get(index, object) : object.get(name, object), depth);
		}
		private JSONObject node(String name, Object value, int depth) {
			remaining--;
			boolean cycle = path.contains(value);
			var children = !cycle && depth < 3 && remaining > 0 ? properties(value, depth + 1) : new JSONArray();
			return json("name", name.length() > 500 ? name.substring(0, 500) : name, "value", preview(value),
					"children", children, "limited", cycle || depth >= 3 || remaining <= 0 || children.length() >= 100);
		}
	}
	private static boolean primitive(Object value) {
		return value instanceof String || value instanceof org.mozilla.javascript.ConsString || value instanceof Boolean
				|| value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
				|| value instanceof Float || value instanceof Double || value instanceof java.math.BigInteger || value instanceof java.math.BigDecimal
				|| value instanceof Character;
	}
	private static String preview(Object value) {
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

	public boolean breakpoint(String url, int line, boolean set) {
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
			event("breakpoints"); return true;
		}
	}
	public JSONArray breakpoints(String url) { synchronized (lock) { return array(breakpoints.getOrDefault(url, Set.of())); } }
	public JSONObject source(String url) {
		synchronized (lock) {
			var source = sources.get(url); if (source == null) throw new IllegalArgumentException("Source unavailable. Run the debug request first.");
			return json("url", url, "source", source.text, "breakable", array(source.lines), "breakpoints", breakpoints(url));
		}
	}
	public JSONObject state() {
		synchronized (lock) {
			var stack = new JSONArray();
			if (paused) for (int i = frames.size() - 1; i >= 0; i--) {
				var frame = frames.get(i); stack.put(json("url", frame.url(), "line", frame.line, "function", frame.script.getFunctionName()));
			}
			return json("attached", !closed, "sessionId", id, "serial", serial, "pauseId", pauseId,
					"running", !workers.isEmpty(), "workers", workers.size(), "notice", notice, "pauseTimeoutMillis", pauseTimeoutMillis, "stopped", paused, "breakOnExceptions", breakOnExceptions,
					"breakOnEnter", breakOnEnter, "breakOnReturn", breakOnReturn,
					"studioRequests", JavascriptDebugger.studioSession() == this,
					"sources", array(sources.keySet()), "frames", stack, "events", array(events));
		}
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
