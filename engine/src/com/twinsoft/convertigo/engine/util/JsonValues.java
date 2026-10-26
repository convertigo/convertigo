/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.util.*;
import com.fasterxml.jackson.core.*;
import org.codehaus.jettison.json.*;

/** Plain JSON data. Containers belong to one request; legacy conversion occurs only at adapters. */
public final class JsonValues {
	private JsonValues() { }
	public static Map<String, Object> object(Object... pairs) {
		var result = new LinkedHashMap<String, Object>();
		for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
		return result;
	}
	@SuppressWarnings("unchecked")
	public static Map<String, Object> map(Object value) {
		if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("Expected JSON object");
		return (Map<String, Object>) value;
	}
	@SuppressWarnings("unchecked")
	public static List<Object> list(Object value) {
		if (!(value instanceof List<?>)) throw new IllegalArgumentException("Expected JSON array");
		return (List<Object>) value;
	}
	public static String text(Map<?, ?> value, String key, String fallback) { var v=value.get(key); return v instanceof String s ? s : fallback; }
	public static long number(Map<?, ?> value, String key, long fallback) { var v=value.get(key); return v instanceof Number n ? n.longValue() : fallback; }
	public static boolean bool(Map<?, ?> value, String key, boolean fallback) { var v=value.get(key); return v instanceof Boolean b ? b : fallback; }
	public static Object parse(String text) throws IOException {
		try (var parser = new JsonFactory().createParser(text)) {
			var value = read(parser, parser.nextToken(), 0);
			if (parser.nextToken() != null) throw new IOException("Trailing JSON");
			return value;
		}
	}
	private static Object read(JsonParser parser, JsonToken token, int depth) throws IOException {
		if (token == null || depth > 128) throw new IOException("Invalid or excessively nested JSON");
		return switch (token) {
		case START_OBJECT -> {
			var value = object();
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				if (parser.currentToken() != JsonToken.FIELD_NAME) throw new IOException("Invalid JSON object");
				var key = parser.currentName(); value.put(key, read(parser, parser.nextToken(), depth + 1));
			}
			yield value;
		}
		case START_ARRAY -> {
			var value = new ArrayList<>(); JsonToken item;
			while ((item = parser.nextToken()) != JsonToken.END_ARRAY) value.add(read(parser, item, depth + 1));
			yield value;
		}
		case VALUE_STRING -> parser.getText();
		case VALUE_TRUE -> true;
		case VALUE_FALSE -> false;
		case VALUE_NULL -> null;
		case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> parser.getNumberValue();
		default -> throw new IOException("Invalid JSON token");
		};
	}
	/** Copy a legacy result into request-owned data, preserving explicit nulls. */
	public static Object fromLegacy(Object value) throws JSONException {
		if (value instanceof JSONObject object) {
			var result = object();
			for (var keys=object.keys();keys.hasNext();) { var key=(String)keys.next(); result.put(key,fromLegacy(object.get(key))); }
			return result;
		}
		if (value instanceof JSONArray array) {
			var result = new ArrayList<>();
			for (int i=0;i<array.length();i++) result.add(fromLegacy(array.get(i)));
			return result;
		}
		return value == JSONObject.NULL || value == JSONObject.EXPLICIT_NULL ? null : value;
	}
	/** Explicit legacy boundary. Never used as the storage of runtime data. */
	public static Object toLegacy(Object value) throws JSONException {
		if (value instanceof Map<?, ?> object) {
			var result = new JSONObject();
			for (var entry : object.entrySet()) result.put((String)entry.getKey(),toLegacy(entry.getValue()));
			return result;
		}
		if (value instanceof List<?> array) {
			var result = new JSONArray();
			for (var item : array) result.put(toLegacy(item));
			return result;
		}
		return value == null ? JSONObject.EXPLICIT_NULL : value;
	}
}
