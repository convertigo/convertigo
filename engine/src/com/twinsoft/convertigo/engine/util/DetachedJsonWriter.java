/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.io.Writer;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** Writes boundary-owned trees without allocating an escaped String per cell/key. */
public final class DetachedJsonWriter {
	private DetachedJsonWriter() { }
	public static void write(Object value, Writer output) throws IOException, JSONException { write(value, output, true); }
	private static void write(Object value, Writer output, boolean escapeSlash) throws IOException, JSONException {
		if (value instanceof DetachedJson.ObjectValue object) {
			output.write('{'); boolean separator = false;
			for (var keys = object.keys(); keys.hasNext();) {
				var key = (String) keys.next();
				if (separator) output.write(',');
				quote(key, output, object.isEscapeForwardSlashAlways()); output.write(':');
				write(object.get(key), output, object.isEscapeForwardSlashAlways()); separator = true;
			}
			output.write('}');
		} else if (value instanceof DetachedJson.ArrayValue array) {
			output.write('[');
			for (int i = 0; i < array.length(); i++) {
				if (i != 0) output.write(',');
				write(array.get(i), output, array.isEscapeForwardSlashAlways());
			}
			output.write(']');
		} else if (value instanceof JSONObject object) object.write(output);
		else if (value instanceof JSONArray array) array.write(output);
		else if (value instanceof String text) quote(text, output, escapeSlash);
		else if (value == null || value == JSONObject.EXPLICIT_NULL || JSONObject.NULL.equals(value)) output.write("null");
		else if (value instanceof Boolean flag) output.write(flag ? "true" : "false");
		else if (value instanceof Number number) output.write(JSONObject.numberToString(number));
		else throw new JSONException("Unsupported detached JSON scalar");
	}
	public static void quote(String text, Writer output, boolean escapeSlash) throws IOException {
		output.write('"'); int start = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			String escape = switch (c) {
			case '"' -> "\\\"";
			case '\\' -> "\\\\";
			case '/' -> escapeSlash || i > 0 && text.charAt(i - 1) == '<' ? "\\/" : null;
			case '\b' -> "\\b"; case '\t' -> "\\t"; case '\n' -> "\\n";
			case '\f' -> "\\f"; case '\r' -> "\\r";
			case '\u2028' -> "\\u2028"; case '\u2029' -> "\\u2029";
			default -> null;
			};
			if (escape == null && c >= ' ') continue;
			output.write(text, start, i - start);
			if (escape != null) output.write(escape);
			else {
				output.write("\\u00"); output.write("0123456789abcdef".charAt(c >> 4)); output.write("0123456789abcdef".charAt(c & 15));
			}
			start = i + 1;
		}
		output.write(text, start, text.length() - start); output.write('"');
	}
}
