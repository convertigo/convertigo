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
	private JSONObject value;
	private final String callback;
	private ResultDocument rows;
	private boolean prepared;
	public JsonResponse(JSONObject value, String callback) { this(value, callback, null); }
	public JsonResponse(JSONObject value, String callback, ResultDocument rows) { this.value=value; this.callback=callback; this.rows=rows; }
	/** Before getWriter/JSONP prefix: preserve the historical early error/status. */
	public int prepare() throws IOException {
		if (prepared) return 0;
		prepared = true;
		if (rows != null) {
			try { rows.prepare(); }
			catch (RowStreamException error) {
				rows = null;
				value = error.response();
				return error.status();
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
	public void writeTo(Writer writer) throws IOException {
		prepare();
		try {
			if (callback != null) { writer.write(callback); writer.write('('); }
			if (rows == null) com.twinsoft.convertigo.engine.util.DetachedJsonWriter.write(value, writer);
			else rows.writeTo(writer, callback == null ? "" : ");");
			if (rows == null && callback != null) writer.write(");");
		} catch (JSONException e) { throw new IOException("Unable to serialize JSON response", e); }
		finally { close(); }
	}
	@Override public void close() throws IOException { if (rows != null) { var owned=rows; rows=null; owned.close(); } }
}
