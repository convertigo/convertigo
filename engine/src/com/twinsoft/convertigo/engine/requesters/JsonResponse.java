/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.requesters;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.Writer;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.flow.rows.ResultDocument;
import com.twinsoft.convertigo.engine.flow.rows.RowStreamException;

/** JSON response serialized without a complete String and byte[] copy. */
public final class JsonResponse implements AutoCloseable {
	private Object value;
	private final String callback;
	private ResultDocument rows;
	private boolean prepared;
	// a text found in the cache, or the cache entry this answer fills once written complete
	private String text;
	private com.twinsoft.convertigo.engine.cache.NativeJsonCache.Pending cache;
	private int status;
	private boolean complete = true;
	public JsonResponse(Object value, String callback) { this(value, callback, null); }
	public JsonResponse(Object value, String callback, ResultDocument rows) { this.value=value; this.callback=callback; this.rows=rows; }
	/** The native JSON answer of a request: its cached text, or its value and rows, kept in the cache when it should. */
	public static JsonResponse of(com.twinsoft.convertigo.engine.Context context, String callback) {
		var response = new JsonResponse(context.nativeJsonResponse, callback, context.takeNativeRows());
		response.text = context.nativeJsonCached;
		// a JSONP answer carries its callback: the cache keeps JSON answers only
		if (callback == null && context.isCacheEnabled) response.cache = context.nativeJsonCache;
		context.nativeJsonCached = null;
		context.nativeJsonCache = null;
		return response;
	}
	/** Before getWriter/JSONP prefix: preserve the historical early error/status. */
	public int prepare() throws IOException {
		if (prepared) return 0;
		prepared = true;
		if (rows != null) {
			try { rows.prepare(); }
			catch (RowStreamException error) {
				rows = null;
				value = error.response();
				return status = error.status();
			}
		}
		return 0;
	}
	/** Detect PrintWriter's swallowed errors at each producer flush, not after another page. */
	public void writeToResponse(PrintWriter output) throws IOException {
		try (var allocation = com.twinsoft.convertigo.engine.util.AllocationTrace.open("boundary", "write", "");
				Writer writer = new BufferedWriter(new Writer() {
					@Override public void write(char[] c,int off,int len) { output.write(c,off,len); }
					@Override public void flush() throws IOException {
						output.flush(); if (output.checkError()) throw new IOException("JSON response connection failed");
					}
					@Override public void close() throws IOException { try { flush(); } finally { output.close(); } }
				})) {
			writeTo(writer); writer.flush();
		} finally { close(); }
	}
	public void writeTo(Writer output) throws IOException {
		prepare();
		var tee = cache == null || text != null ? null : new com.twinsoft.convertigo.engine.cache.NativeJsonCache.Tee(output, cache);
		var writer = tee == null ? output : tee;
		try {
			if (callback != null) { writer.write(callback); writer.write('('); }
			if (text != null) writer.write(text);
			else if (rows == null) {
				com.twinsoft.convertigo.engine.util.DetachedJsonWriter.write(value, writer);
				complete = !(value instanceof java.util.Map<?, ?> map && map.containsKey("error"));
			} else {
				rows.writeTo(writer, callback == null ? "" : ");");
				complete = rows.isComplete();
			}
			if (rows == null && callback != null) writer.write(");");
			if (tee != null) {
				if (complete && status == 0) tee.store();
				else tee.discard();
			}
		} catch (JSONException e) { throw new IOException("Unable to serialize JSON response", e); }
		finally { close(); }
	}
	@Override public void close() throws IOException { if (rows != null) { var owned=rows; rows=null; owned.close(); } }
}
