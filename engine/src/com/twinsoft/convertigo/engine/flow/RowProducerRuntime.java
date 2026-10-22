/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import com.twinsoft.convertigo.engine.flow.rows.RowSource;
import com.twinsoft.convertigo.engine.flow.rows.RowStreamException;
import com.twinsoft.convertigo.engine.util.DetachedJson;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.SanitizedJson;

/**
 * A response-owned, bounded pool of execution permits. Each lease has a fresh
 * realm: project code may modify intrinsic prototypes, so returning that realm
 * to another request would not be safe. Compiled immutable scripts are shared
 * by RhinoUtils; input, state, functions and classloader bindings never are.
 */
public final class RowProducerRuntime implements RowSource {

	private static final int CAPACITY = 32;
	private static final Semaphore permits = new Semaphore(CAPACITY);
	public static final class Plan {
		private final String bootstrap, entry, finish;
		private final Map<String,String> blocks;
		private final JSONObject input;
		private final boolean collect;
		private final long collectRows;
		private final int collectBytes;
		public Plan(String bootstrap, Map<String,String> blocks, JSONObject input, String entry, String finish) {
			this(bootstrap,blocks,input,entry,finish,false,0,0);
		}
		public Plan(String bootstrap, Map<String,String> blocks, JSONObject input, String entry, String finish,
				boolean collect, long collectRows, int collectBytes) {
			this.bootstrap=bootstrap; this.blocks=Map.copyOf(blocks); this.entry=entry; this.finish=finish;
			this.collect=collect; this.collectRows=collectRows; this.collectBytes=collectBytes;
			if (collect && (collectRows<1 || collectRows>1_000_000 || collectBytes<1 || collectBytes>256*1024*1024))
				throw new IllegalArgumentException("Invalid bounded row collection");
			try { this.input=(JSONObject) copyInput(input,0,new int[]{0}); }
			catch (Exception invalid) { throw new IllegalArgumentException("Invalid detached row input"); }
		}
		private static Object copyInput(Object value,int depth,int[] nodes) throws Exception {
			if(depth>64||++nodes[0]>100_000)throw new IllegalArgumentException("Row input limit");
			if(value instanceof JSONObject object){var copy=new DetachedJson.ObjectValue();for(var keys=object.keys();keys.hasNext();){var key=(String)keys.next();copy.put(key,copyInput(object.get(key),depth+1,nodes));}return copy;}
			if(value instanceof JSONArray array){var copy=new DetachedJson.ArrayValue();for(int i=0;i<array.length();i++)copy.put(copyInput(array.get(i),depth+1,nodes));return copy;}
			if(value==null||value==JSONObject.NULL||value==JSONObject.EXPLICIT_NULL)return JSONObject.EXPLICIT_NULL;
			if(value instanceof String||value instanceof Boolean||value instanceof Number)return value;
			throw new IllegalArgumentException("Row input is not JSON");
		}
	}

	private final RowSource input;
	private final boolean collect;
	private final long collectRows;
	private final int collectBytes;
	private JSONArray batchRows;
	private JSONObject batchMetadata;
	private int batchIndex;
	private Plan plan;
	private ClassLoader generation;
	private Scriptable scope, producer;
	private Thread owner;
	private volatile boolean closed;
	private boolean borrowed, inputClosed;
	private final Object realmLock = new Object();

	public RowProducerRuntime(RowSource input, Plan plan, ClassLoader generation) {
		this.input = input; this.plan = plan; this.generation = generation;
		this.collect = plan.collect; this.collectRows = plan.collectRows; this.collectBytes = plan.collectBytes;
	}
	static int activeLeases() { return CAPACITY - permits.availablePermits(); }
	@FunctionalInterface private interface Work { JSONObject run(Context cx) throws Exception; }
	private JSONObject inRealm(Work work) throws IOException {
		synchronized (realmLock) { return inOwnedRealm(work); }
	}
	private JSONObject inOwnedRealm(Work work) throws IOException {
		if (closed) throw new RowStreamException("ROW_STREAM_CANCELLED", 499);
		var thread = Thread.currentThread();
		if (owner != null && owner != thread) throw new RowStreamException("ROW_STREAM_OWNER", 409);
		owner = thread;
		var previousLoader = thread.getContextClassLoader();
		var mdc = Log4jHelper.mdcSnapshot();
		var cx = Context.enter();
		var previousLanguage = cx.getLanguageVersion();
		cx.setLanguageVersion(Context.VERSION_ES6);
		try {
			// No inherited request/session/MDC objects in the detached execution.
			Log4jHelper.mdcClear(); org.apache.log4j.MDC.put("nolog",true);
			if (generation != null) thread.setContextClassLoader(generation);
			if (!borrowed) {
				if (!permits.tryAcquire()) throw new RowStreamException("ROW_STREAM_POOL_BUSY", 503);
				borrowed = true;
				scope = FlowPackages.createScope(cx);
			}
			FlowPackages.bind(cx, scope, generation);
			if (producer == null) {
				var blocks = cx.newObject(scope);
				for (var entry : plan.blocks.entrySet()) {
					var block = RhinoUtils.evalCachedJavascript(cx, scope, entry.getValue(), "row-block:" + entry.getKey(), 1, null);
					if (!(block instanceof Scriptable)) throw new RowStreamException("ROW_STREAM_BLOCK", 500);
					ScriptableObject.defineProperty(blocks, entry.getKey(), block, ScriptableObject.EMPTY);
				}
				var factory = (Function) RhinoUtils.evalCachedJavascript(cx, scope, plan.bootstrap, "row-producer", 1, null);
				producer = (Scriptable) factory.call(cx, scope, scope, new Object[] {blocks,
						DetachedJson.toScriptable(plan.input, scope), plan.entry, plan.finish});
				plan = null; // Source strings/config belong to this realm only until close.
			}
			return work.run(cx);
		} catch (Exception error) {
			throw RowStreamException.safe(error);
		} finally {
			if (scope != null) FlowPackages.restore(scope, generation);
			cx.setLanguageVersion(previousLanguage); Context.exit(); thread.setContextClassLoader(previousLoader); mdc.install();
		}
	}
	private JSONObject call(Context cx, String method, Object... args) throws Exception {
		var fn = ScriptableObject.getProperty(producer, method);
		var value = ((Function) fn).call(cx, scope, producer, args);
		// Row plans emit plain JSON. Exotic objects, handles and accessors
		// are not a cursor protocol and cannot escape to the servlet writer.
		return SanitizedJson.object(value, cx, scope, (c,s,t,a) -> {
			throw Context.reportRuntimeError("Row producers must emit plain JSON data");
		});
	}
	@Override public JSONObject next() throws IOException {
		try (var logging = Log4jHelper.mdcScope()) {
			Log4jHelper.mdcClear(); org.apache.log4j.MDC.put("nolog",true);
			synchronized (realmLock) {
				if (closed) throw new RowStreamException("ROW_STREAM_CANCELLED",499);
				if (owner != null && owner != Thread.currentThread()) throw new RowStreamException("ROW_STREAM_OWNER",409);
				owner = Thread.currentThread();
			}
			if (collect) return nextBatch();
			while (true) {
				if (closed) throw new RowStreamException("ROW_STREAM_CANCELLED",499);
				if (owner != null && owner != Thread.currentThread()) throw new RowStreamException("ROW_STREAM_OWNER",409);
				var row = input.next(); if (row == null) return null;
				var value = inRealm(cx -> call(cx, "next", DetachedJson.toScriptable(row, scope)));
				if (value.optBoolean("skip", false)) continue;
				var item = value.optJSONObject("row");
				if (item == null) throw new RowStreamException("ROW_STREAM_TRANSFORM", 502);
				return item;
			}
		} catch (Exception error) { close(); throw RowStreamException.safe(error); }
	}
	private JSONObject nextBatch() throws Exception {
		if (closed) throw new RowStreamException("ROW_STREAM_CANCELLED",499);
		if (batchRows == null) {
			var values = new DetachedJson.ArrayValue(); long bytes = 0;
			while (true) {
				var row = input.next(); if (row == null) break;
				if (values.length() >= collectRows) throw new RowStreamException("ROW_STREAM_COLLECT_ROWS",413);
				bytes += com.twinsoft.convertigo.engine.flow.rows.ResultDocument.encodedBytes(row,collectBytes);
				if (bytes > collectBytes) throw new RowStreamException("ROW_STREAM_COLLECT_BYTES",413);
				values.put(row);
			}
			var result = inRealm(cx -> call(cx,"batch",DetachedJson.toScriptable(values,scope)));
			com.twinsoft.convertigo.engine.flow.rows.ResultDocument.encodedBytes(result,collectBytes);
			if (result.has("failure")) throw RowStreamException.response(result.getJSONObject("failure"),result.optInt("status",400));
			batchRows = result.getJSONArray("rows"); batchMetadata = result.getJSONObject("metadata");
			// The batch is now detached. Release its realm before serializing it.
			releaseRealm();
		}
		if (batchIndex >= batchRows.length()) return null;
		var row = batchRows.getJSONObject(batchIndex); batchRows.put(batchIndex++,JSONObject.EXPLICIT_NULL); return row;
	}
	@Override public JSONObject metadata() throws IOException { return collect ? batchMetadata : inRealm(cx -> call(cx, "finish")); }
	private void releaseRealm() {
		synchronized (realmLock) {
			producer = scope = null; plan = null; generation = null;
			if (borrowed) { borrowed = false; permits.release(); }
		}
	}
	@Override public void close() throws IOException {
		closed = true;
		try (var logging = Log4jHelper.mdcScope()) {
			Log4jHelper.mdcClear(); org.apache.log4j.MDC.put("nolog",true);
			synchronized (input) { if (!inputClosed) { inputClosed = true; input.close(); } }
		} finally {
			// Discard all functions, operator state and project generation references.
			releaseRealm(); batchRows=null; batchMetadata=null; owner=null;
		}
	}
}
