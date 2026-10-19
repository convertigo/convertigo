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

package com.twinsoft.convertigo.engine.json;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mozilla.javascript.Scriptable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * A JSON path of the steps of a JSON native sequence: <code>$</code> for the value itself, then <code>.key</code>,
 * <code>['key']</code>, <code>[index]</code>, and the wildcards <code>.*</code> and <code>[*]</code>. Without a
 * wildcard it selects one value; with one, the array of the values found. It reads Java maps and lists and
 * JavaScript data, and a JSON text in stream: the parts of the text outside the path are skipped, not built.
 */
public final class JsonPath {
	/** the value of a path that selects nothing */
	public static final Object MISSING = JsonData.UNDEFINED;

	private static final JsonFactory factory = new JsonFactory();

	private final String path;
	/** String for a key, Integer for an index, null for a wildcard */
	private final List<Object> segments;
	private final boolean wildcard;

	private JsonPath(String path, List<Object> segments) {
		this.path = path;
		this.segments = segments;
		this.wildcard = segments.contains(null);
	}

	/** a source or a property is a JSON path when it starts with $ */
	public static boolean isJsonPath(String text) {
		return text != null && text.trim().startsWith("$");
	}

	public static JsonPath parse(String text) {
		String s = text.trim();
		if (!s.startsWith("$")) {
			throw new IllegalArgumentException("A JSON path starts with $: " + text);
		}
		List<Object> segments = new ArrayList<Object>();
		int i = 1;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c == '.') {
				int end = i + 1;
				while (end < s.length() && s.charAt(end) != '.' && s.charAt(end) != '[') {
					end++;
				}
				String key = s.substring(i + 1, end);
				if (key.isEmpty()) {
					throw new IllegalArgumentException("Empty key at " + i + " in the JSON path " + text);
				}
				segments.add("*".equals(key) ? null : key);
				i = end;
			} else if (c == '[') {
				int end = s.indexOf(']', i);
				if (end < 0) {
					throw new IllegalArgumentException("Missing ] in the JSON path " + text);
				}
				String inside = s.substring(i + 1, end).trim();
				if ((inside.startsWith("'") || inside.startsWith("\"")) && inside.length() >= 2) {
					// a quoted key may hold a ]: look for the closing quote
					char quote = inside.charAt(0);
					int close = i + 2;
					StringBuilder key = new StringBuilder();
					for (; close < s.length() && s.charAt(close) != quote; close++) {
						char k = s.charAt(close);
						if (k == '\\' && close + 1 < s.length()) {
							k = s.charAt(++close);
						}
						key.append(k);
					}
					end = s.indexOf(']', close);
					if (close >= s.length() || end < 0) {
						throw new IllegalArgumentException("Unclosed quoted key in the JSON path " + text);
					}
					segments.add(key.toString());
				} else if ("*".equals(inside)) {
					segments.add(null);
				} else {
					try {
						segments.add(Integer.valueOf(inside));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException("Index or quoted key expected in [" + inside + "] in the JSON path " + text);
					}
				}
				i = end + 1;
			} else {
				throw new IllegalArgumentException("Unexpected '" + c + "' at " + i + " in the JSON path " + text);
			}
		}
		return new JsonPath(s, Collections.unmodifiableList(segments));
	}

	public String getPath() {
		return path;
	}

	/** the segments: String for a key, Integer for an index, null for a wildcard */
	public List<Object> getSegments() {
		return segments;
	}

	public boolean isRoot() {
		return segments.isEmpty();
	}

	/** the key of the last segment, null for an index, a wildcard or the root */
	public String getLastKey() {
		return segments.isEmpty() || !(segments.get(segments.size() - 1) instanceof String) ? null : (String) segments.get(segments.size() - 1);
	}

	public boolean hasWildcard() {
		return wildcard;
	}

	@Override
	public String toString() {
		return path;
	}

	/** the value at the path in Java maps and lists or JavaScript data: MISSING, or a list with a wildcard */
	public Object select(Object root) {
		List<Object> current = new ArrayList<Object>(1);
		current.add(JsonData.unwrap(root));
		for (Object segment : segments) {
			List<Object> next = new ArrayList<Object>();
			for (Object value : current) {
				if (segment == null) {
					if (JsonData.isArray(value)) {
						next.addAll(JsonData.list(value));
					} else if (JsonData.isObject(value)) {
						for (String key : JsonData.keys(value)) {
							next.add(get(value, key));
						}
					}
				} else if (segment instanceof Integer) {
					int index = (Integer) segment;
					if (JsonData.isArray(value)) {
						List<Object> list = JsonData.list(value);
						if (index >= 0 && index < list.size()) {
							next.add(JsonData.unwrap(list.get(index)));
						}
					}
				} else {
					Object child = get(value, (String) segment);
					if (child != MISSING) {
						next.add(child);
					}
				}
			}
			current = next;
		}
		if (wildcard) {
			return current;
		}
		return current.isEmpty() ? MISSING : current.get(0);
	}

	private static Object get(Object obj, String key) {
		if (obj instanceof Scriptable && JsonData.isIndexKey(key)) {
			Scriptable s = (Scriptable) obj;
			Object value = s.get(Integer.parseInt(key), s);
			return value == Scriptable.NOT_FOUND ? MISSING : JsonData.unwrap(value);
		}
		return JsonData.unwrap(JsonData.get(obj, key));
	}

	/**
	 * the JSON text of the value at the path in a JSON text, null when the path selects nothing. Without a wildcard
	 * the text is read in stream: only the value selected is copied, the rest is skipped. As JSON.parse, the last of
	 * the keys of the same name wins.
	 */
	public String selectText(String json) throws IOException {
		if (json == null) {
			return null;
		}
		if (wildcard) {
			Object value = select(RhinoUtilsBridge.parse(json));
			return value == MISSING ? null : RhinoUtilsBridge.stringify(value);
		}
		try (JsonParser parser = factory.createParser(json)) {
			if (parser.nextToken() == null) {
				return null;
			}
			return selectText(parser, 0);
		}
	}

	/** the parser is on the first token of a value; the text of the value at the segments from depth */
	private String selectText(JsonParser parser, int depth) throws IOException {
		if (depth == segments.size()) {
			StringWriter writer = new StringWriter();
			try (JsonGenerator generator = factory.createGenerator(writer)) {
				generator.copyCurrentStructure(parser);
			}
			return writer.toString();
		}
		Object segment = segments.get(depth);
		JsonToken token = parser.currentToken();
		if (segment instanceof Integer && token == JsonToken.START_ARRAY) {
			int index = (Integer) segment;
			String found = null;
			for (int i = 0; parser.nextToken() != JsonToken.END_ARRAY; i++) {
				if (i == index) {
					// the parser ends on the last token of this item, the next items are skipped
					found = selectText(parser, depth + 1);
				} else {
					parser.skipChildren();
				}
			}
			return found;
		}
		if (segment instanceof String && token == JsonToken.START_OBJECT) {
			String found = null;
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String name = parser.currentName();
				parser.nextToken();
				if (name.equals(segment)) {
					// the last key of this name wins, as with JSON.parse
					found = selectText(parser, depth + 1);
				} else {
					parser.skipChildren();
				}
			}
			return found;
		}
		// a key on an array, an index on an object or on a value: nothing
		parser.skipChildren();
		return null;
	}

	/** JSON text to JavaScript data and back, through RhinoUtils (kept apart for the parser of this class) */
	private static final class RhinoUtilsBridge {
		static Object parse(String json) {
			return com.twinsoft.convertigo.engine.util.RhinoUtils.jsonParse(json);
		}

		static String stringify(Object value) {
			return com.twinsoft.convertigo.engine.util.RhinoUtils.jsonStringifyData(value);
		}
	}
}
