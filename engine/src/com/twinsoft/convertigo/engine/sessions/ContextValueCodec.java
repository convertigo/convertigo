/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the  Free Software Foundation;  either
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

package com.twinsoft.convertigo.engine.sessions;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.twinsoft.convertigo.engine.util.DomSerializationSupport;

final class ContextValueCodec {
	private static final String MARKER = "__c8o";
	private static final String KIND_DOM = "dom";
	private static final String KIND_FILE = "file";
	private static final String KIND_SET = "set";

	String serialize(Object value) throws Exception {
		if (value == null) {
			return null;
		}
		var encoded = encode(value);
		return encoded != null ? JsonCodec.MAPPER.writeValueAsString(encoded) : null;
	}

	Object deserialize(String raw) throws Exception {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		var node = JsonCodec.MAPPER.readTree(raw);
		return decode(node);
	}

	private static Object encode(Object value) {
		value = unwrap(value);
		if (value == null) {
			return null;
		}
		if (value instanceof String || value instanceof Number || value instanceof Boolean) {
			return value;
		}
		if (value instanceof CharSequence) {
			return value.toString();
		}
		if (value instanceof File file) {
			var obj = new LinkedHashMap<String, Object>();
			obj.put(MARKER, KIND_FILE);
			obj.put("path", file.getPath());
			return obj;
		}
		var dom = DomSerializationSupport.serialize(value);
		if (dom != null) {
			var obj = new LinkedHashMap<String, Object>();
			obj.put(MARKER, KIND_DOM);
			obj.put("type", dom.type().name());
			obj.put("xml", dom.xml());
			return obj;
		}
		if (value instanceof Map<?, ?> map) {
			var obj = new LinkedHashMap<String, Object>();
			for (var entry : map.entrySet()) {
				if (entry.getKey() == null) {
					continue;
				}
				var key = entry.getKey().toString();
				var encoded = encode(entry.getValue());
				if (encoded != null) {
					obj.put(key, encoded);
				}
			}
			return obj;
		}
		if (value instanceof List<?> list) {
			var arr = new ArrayList<Object>(list.size());
			for (var item : list) {
				arr.add(encode(item));
			}
			return arr;
		}
		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			var arr = new ArrayList<Object>(length);
			for (int i = 0; i < length; i++) {
				arr.add(encode(Array.get(value, i)));
			}
			return arr;
		}
		if (value instanceof Set<?> set) {
			var obj = new LinkedHashMap<String, Object>();
			obj.put(MARKER, KIND_SET);
			var arr = new ArrayList<Object>(set.size());
			for (var item : set) {
				arr.add(encode(item));
			}
			obj.put("items", arr);
			return obj;
		}
		return null;
	}

	private static Object decode(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isTextual()) {
			return node.textValue();
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isNumber()) {
			return node.numberValue();
		}
		if (node.isArray()) {
			var arr = new ArrayList<Object>(node.size());
			for (var item : node) {
				arr.add(decode(item));
			}
			return arr;
		}
		if (!node.isObject()) {
			return null;
		}

		var marker = node.get(MARKER);
		if (marker != null && marker.isTextual()) {
			var kind = marker.textValue();
			if (KIND_FILE.equals(kind)) {
				var path = node.path("path").asText(null);
				return path != null ? new File(path) : null;
			}
			if (KIND_DOM.equals(kind)) {
				var typeName = node.path("type").asText(null);
				var xml = node.path("xml").asText(null);
				if (typeName == null || xml == null) {
					return null;
				}
				try {
					var type = DomSerializationSupport.DomType.valueOf(typeName);
					return DomSerializationSupport.deserialize(new DomSerializationSupport.SerializedDom(type, xml));
				} catch (Exception e) {
					return null;
				}
			}
			if (KIND_SET.equals(kind)) {
				var items = node.get("items");
				if (items == null || !items.isArray()) {
					return Set.of();
				}
				var set = new java.util.LinkedHashSet<Object>(items.size());
				for (var item : items) {
					set.add(decode(item));
				}
				return set;
			}
		}

		var obj = new LinkedHashMap<String, Object>();
		var fields = node.fieldNames();
		while (fields.hasNext()) {
			var name = fields.next();
			if (MARKER.equals(name)) {
				continue;
			}
			obj.put(name, decode(node.get(name)));
		}
		return obj;
	}

	private static Object unwrap(Object value) {
		while (value != null) {
			if (value instanceof org.mozilla.javascript.ScriptableObject scriptable) {
				String className;
				try {
					className = scriptable.getClassName();
				} catch (Exception e) {
					className = null;
				}
				if ("String".equals(className)) {
					try {
						return org.mozilla.javascript.Context.toString(value);
					} catch (Exception e) {
						return value.toString();
					}
				}
				if ("Number".equals(className)) {
					try {
						return org.mozilla.javascript.Context.toNumber(value);
					} catch (Exception e) {
						return null;
					}
				}
				if ("Boolean".equals(className)) {
					try {
						return org.mozilla.javascript.Context.toBoolean(value);
					} catch (Exception e) {
						return null;
					}
				}
			}
			if (value instanceof org.mozilla.javascript.Wrapper wrapper) {
				value = wrapper.unwrap();
				continue;
			}
			if (value instanceof org.mozilla.javascript.Undefined || value instanceof org.mozilla.javascript.UniqueTag) {
				return null;
			}
			break;
		}
		return value;
	}
}
