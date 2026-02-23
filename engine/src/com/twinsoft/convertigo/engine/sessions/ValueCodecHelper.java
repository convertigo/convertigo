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

package com.twinsoft.convertigo.engine.sessions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class ValueCodecHelper {
	static final String FORMAT_POJO = "pojo";
	private static final ObjectMapper POJO_MAPPER = createPojoMapper();

	private ValueCodecHelper() {
	}

	private static ObjectMapper createPojoMapper() {
		var mapper = JsonCodec.MAPPER.copy();
		mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		return mapper;
	}

	static Object unwrapRhino(Object value) {
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

	static EncodedTypedValue encodeTypedValue(Object value) throws Exception {
		value = unwrapRhino(value);
		if (value == null) {
			return null;
		}
		var clazz = value.getClass().getName();
		try {
			return new EncodedTypedValue(clazz, JsonCodec.MAPPER.valueToTree(value), null);
		} catch (Exception e) {
			return new EncodedTypedValue(clazz, POJO_MAPPER.valueToTree(value), FORMAT_POJO);
		}
	}

	static Object decodeTypedValue(String className, JsonNode valueNode, String format) throws Exception {
		if (valueNode == null || valueNode.isNull()) {
			return null;
		}
		var mapper = FORMAT_POJO.equals(format) ? POJO_MAPPER : JsonCodec.MAPPER;
		if (className != null && !className.isBlank()) {
			try {
				var cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
				if (Set.class.isAssignableFrom(cls)) {
					var list = mapper.<ArrayList<Object>>convertValue(valueNode,
							mapper.getTypeFactory().constructCollectionType(ArrayList.class, Object.class));
					return new HashSet<>(list);
				}
				return mapper.convertValue(valueNode, mapper.getTypeFactory().constructType((Type) cls));
			} catch (ClassNotFoundException e) {
				// fall through
			}
		}
		return mapper.convertValue(valueNode, Object.class);
	}

	static final class EncodedTypedValue {
		final String clazz;
		final JsonNode value;
		final String format;

		EncodedTypedValue(String clazz, JsonNode value, String format) {
			this.clazz = clazz;
			this.value = value;
			this.format = format;
		}
	}
}
