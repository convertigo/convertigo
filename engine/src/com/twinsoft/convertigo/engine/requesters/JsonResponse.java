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

/** JSON response serialized to the servlet writer without a complete String and byte[] copy. */
public final class JsonResponse {
	private final JSONObject value;
	private final String callback;
	public JsonResponse(JSONObject value, String callback) { this.value = value; this.callback = callback; }
	/** Own the servlet writer and detect errors swallowed by PrintWriter. */
	public void writeToResponse(PrintWriter output) throws IOException {
		try (Writer writer = new BufferedWriter(output)) {
			writeTo(writer);
			writer.flush();
			if (output.checkError()) throw new IOException("JSON response connection failed");
		}
	}
	public void writeTo(Writer writer) throws IOException {
		try {
			if (callback != null) { writer.write(callback); writer.write('('); }
			value.write(writer);
			if (callback != null) writer.write(");");
		} catch (JSONException e) { throw new IOException("Unable to serialize JSON response", e); }
	}
}
