/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.debug;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.GuiCallback;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

/**
 * The JavaScript debugger of the web Studio, as the Rhino debugger of the Eclipse Studio: attached to the
 * engine, it runs the scripts of the objects interpreted, stops them on breakpoints, exceptions or a break
 * request, and lets the Studio step, inspect the frames and evaluate expressions. The thread of a stopped
 * script waits until the Studio resumes it.
 */
class WebDebugger implements GuiCallback {
	private static final int MAX_SOURCES = 200;
	private static final WebDebugger INSTANCE = new WebDebugger();

	private Dim dim;
	private boolean breakOnExceptions = false;
	/** The last compiled source of each script, by url */
	private final Map<String, Dim.SourceInfo> sources = new LinkedHashMap<>() {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Dim.SourceInfo> eldest) {
			return size() > MAX_SOURCES;
		}
	};
	/** The breakpoints set by the Studio, set again on each compilation of their script */
	private final Map<String, Set<Integer>> breakpoints = new ConcurrentHashMap<>();
	private volatile Dim.StackFrame frame;
	private volatile String thread;
	private volatile String alert;
	private volatile long serial;

	static WebDebugger get() {
		return INSTANCE;
	}

	synchronized boolean isAttached() {
		return dim != null;
	}

	synchronized void start() {
		if (dim == null) {
			dim = new Dim();
			dim.setGuiCallback(this);
			dim.setBreakOnExceptions(breakOnExceptions);
			// the scripts of the objects run interpreted, which the debugger follows
			RhinoUtils.debugMode = true;
			RhinoUtils.clearCachedJavascript();
			dim.attachTo(ContextFactory.getGlobal());
			changed();
			Engine.logStudio.info("(WebDebugger) attached");
		}
	}

	synchronized void stop() {
		if (dim != null) {
			dim.clearAllBreakpoints();
			if (frame != null) {
				resume(Dim.GO);
			}
			dim.detach();
			dim = null;
			RhinoUtils.debugMode = false;
			frame = null;
			changed();
			Engine.logStudio.info("(WebDebugger) detached");
		}
	}

	private Dim dim() throws EngineException {
		var current = dim;
		if (current == null) {
			throw new EngineException("The debugger is not started.");
		}
		return current;
	}

	private void changed() {
		serial++;
	}

	@Override
	public void updateSourceText(Dim.SourceInfo sourceInfo) {
		var url = sourceInfo.url();
		if ("utils".equals(url)) {
			// the internal script of the engine, as the Eclipse Studio ignores it
			return;
		}
		synchronized (sources) {
			sources.remove(url);
			sources.put(url, sourceInfo);
		}
		var lines = breakpoints.get(url);
		if (lines != null) {
			for (var line : lines) {
				if (sourceInfo.breakableLine(line)) {
					sourceInfo.breakpoint(line, true);
				}
			}
		}
		changed();
	}

	@Override
	public void enterInterrupt(Dim.StackFrame lastFrame, String threadTitle, String alertMessage) {
		frame = lastFrame;
		thread = threadTitle;
		alert = alertMessage;
		changed();
	}

	@Override
	public boolean isGuiEventThread() {
		// the stopped threads wait for the Studio
		return false;
	}

	@Override
	public void dispatchNextGuiEvent() throws InterruptedException {
		// no GUI thread
	}

	/**
	 * Resumes the stopped script: go, step into, over or out.
	 */
	void resume(int returnValue) {
		var current = dim;
		if (current != null && frame != null) {
			frame = null;
			alert = null;
			changed();
			current.setReturnValue(returnValue);
		}
	}

	void pause() throws EngineException {
		dim().setBreak();
	}

	synchronized void setBreakOnExceptions(boolean value) {
		breakOnExceptions = value;
		if (dim != null) {
			dim.setBreakOnExceptions(value);
		}
		changed();
	}

	/**
	 * @return whether the line of the script can hold a breakpoint
	 */
	boolean breakpoint(String url, int line, boolean set) {
		var lines = breakpoints.computeIfAbsent(url, key -> ConcurrentHashMap.newKeySet());
		var source = source(url);
		if (set && source != null && !source.breakableLine(line)) {
			return false;
		}
		if (set) {
			lines.add(line);
		} else {
			lines.remove(line);
		}
		if (source != null) {
			source.breakpoint(line, set);
		}
		changed();
		return true;
	}

	/**
	 * @return the lines of the breakpoints of a script, set before or after it runs
	 */
	JSONArray breakpointsOf(String url) {
		var list = new JSONArray();
		var lines = breakpoints.get(url);
		if (lines != null) {
			for (var line : new TreeSet<>(lines)) {
				list.put(line);
			}
		}
		return list;
	}

	private Dim.SourceInfo source(String url) {
		synchronized (sources) {
			return sources.get(url);
		}
	}

	JSONObject state() throws Exception {
		var state = new JSONObject();
		state.put("attached", isAttached());
		state.put("breakOnExceptions", breakOnExceptions);
		state.put("serial", serial);
		var list = new JSONArray();
		synchronized (sources) {
			for (var url : sources.keySet()) {
				list.put(url);
			}
		}
		state.put("sources", list);
		var current = frame;
		state.put("stopped", current != null);
		if (current != null) {
			state.put("thread", thread);
			state.put("alert", alert == null ? "" : alert);
			var frames = new JSONArray();
			var data = current.contextData();
			for (int i = 0; i < data.frameCount(); i++) {
				var stackFrame = data.getFrame(i);
				frames.put(new JSONObject()
						.put("url", stackFrame.getUrl())
						.put("line", stackFrame.getLineNumber())
						.put("function", stackFrame.getFunctionName() == null ? "" : stackFrame.getFunctionName()));
			}
			state.put("frames", frames);
		}
		return state;
	}

	JSONObject sourceOf(String url) throws Exception {
		var source = source(url);
		if (source == null) {
			throw new EngineException("The script " + url + " did not run since the debugger started.");
		}
		var text = source.source();
		var lines = new TreeSet<Integer>();
		var breakable = new JSONArray();
		var count = text.split("\n", -1).length;
		for (int line = 1; line <= count; line++) {
			// Rhino refuses to tell the breakpoint of a line that cannot hold one, as an empty line
			if (source.breakableLine(line)) {
				breakable.put(line);
				if (source.breakpoint(line)) {
					lines.add(line);
				}
			}
		}
		return new JSONObject().put("url", url).put("source", text).put("breakpoints", new JSONArray(lines))
				.put("breakable", breakable);
	}

	/**
	 * @return the variables of the scope and the this of a frame of the stopped script
	 */
	JSONObject variables(int index) throws Exception {
		var current = frame;
		var dim = dim();
		if (current == null) {
			throw new EngineException("No script is stopped.");
		}
		var data = current.contextData();
		if (index < 0 || index >= data.frameCount()) {
			throw new EngineException("No frame " + index + ".");
		}
		dim.contextSwitch(index);
		var stackFrame = data.getFrame(index);
		return new JSONObject().put("scope", locals(dim, stackFrame.scope())).put("this",
				properties(dim, stackFrame.thisObj()));
	}

	/**
	 * @return the variables of a scope, evaluated in the frame: the interpreter keeps their values apart from
	 *         the scope object
	 */
	private static JSONArray locals(Dim dim, Object scope) throws Exception {
		var locals = new JSONArray();
		var ids = scope == null ? null : dim.getObjectIds(scope);
		if (ids == null) {
			return locals;
		}
		for (var id : ids) {
			var name = String.valueOf(id);
			if (name.matches("[A-Za-z_$][\\w$]*") && !name.equals("__parent__") && !name.equals("__proto__")) {
				var text = dim.eval(name);
				locals.put(new JSONObject().put("name", name).put("value",
						text.length() > 500 ? text.substring(0, 500) + "…" : text));
			}
		}
		return locals;
	}

	private static JSONArray properties(Dim dim, Object object) throws Exception {
		var properties = new JSONArray();
		if (object == null) {
			return properties;
		}
		var ids = dim.getObjectIds(object);
		if (ids == null) {
			return properties;
		}
		for (var id : ids) {
			var value = dim.getObjectProperty(object, id);
			var text = dim.objectToString(value);
			properties.put(new JSONObject().put("name", String.valueOf(id)).put("value",
					text.length() > 500 ? text.substring(0, 500) + "…" : text));
		}
		return properties;
	}

	String eval(String expression) throws EngineException {
		if (frame == null) {
			throw new EngineException("No script is stopped.");
		}
		return dim().eval(expression);
	}
}
