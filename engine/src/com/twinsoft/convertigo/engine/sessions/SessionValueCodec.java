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
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.Engine;

final class SessionValueCodec {
	String serialize(String name, Object value) throws Exception {
		value = ValueCodecHelper.unwrapRhino(value);
		if (value == null) {
			return null;
		}
		var hint = SessionAttribute.fromValue(name);
		if (hint != null && hint.expectedClass() != null) {
			return JsonCodec.MAPPER.writeValueAsString(value);
		}
		if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Map
				|| value instanceof List) {
			return JsonCodec.MAPPER.writeValueAsString(value);
		}
		var typed = ValueCodecHelper.encodeTypedValue(value);
		return JsonCodec.MAPPER.writeValueAsString(new TypedValue(typed.clazz, typed.value, typed.format));
	}

	Object deserialize(String name, String raw) throws Exception {
		if (raw == null) {
			return null;
		}
		var hint = SessionAttribute.fromValue(name);
		var node = JsonCodec.MAPPER.readTree(raw);
		var isWrapped = node != null && node.has("clazz") && node.has("value");
		var valueNode = isWrapped ? node.get("value") : node;

		if (hint != null && hint.expectedClass() != null) {
			return deserializeExpectedValue(hint.expectedClass(), valueNode);
		}
		if (isWrapped) {
			var formatNode = node.get("format");
			var format = formatNode != null ? formatNode.asText(null) : null;
			var clazzNode = node.get("clazz");
			var className = clazzNode != null ? clazzNode.asText(null) : null;
			return ValueCodecHelper.decodeTypedValue(className, valueNode, format);
		}
		return JsonCodec.MAPPER.convertValue(valueNode, Object.class);
	}

	private Object deserializeExpectedValue(Class<?> expectedClass, com.fasterxml.jackson.databind.JsonNode valueNode)
			throws Exception {
		try {
			return JsonCodec.MAPPER.convertValue(valueNode,
					JsonCodec.MAPPER.getTypeFactory().constructType((Type) expectedClass));
		} catch (IllegalArgumentException e) {
			if (expectedClass.isMemberClass() && !Modifier.isStatic(expectedClass.getModifiers())) {
				var enclosingClass = expectedClass.getEnclosingClass();
				var outerInstance = resolveEnclosingInstance(enclosingClass);
				if (outerInstance != null) {
					var constructor = expectedClass.getDeclaredConstructor(enclosingClass);
					constructor.setAccessible(true);
					var instance = constructor.newInstance(outerInstance);
					var parser = valueNode.traverse(JsonCodec.MAPPER);
					parser.nextToken();
					return JsonCodec.MAPPER.readerForUpdating(instance).readValue(parser);
				}
			}
			throw e;
		}
	}

	private Object resolveEnclosingInstance(Class<?> enclosingClass) {
		if (enclosingClass != null && Engine.theApp != null && Engine.theApp.couchDbManager != null
				&& enclosingClass.isInstance(Engine.theApp.couchDbManager)) {
			return Engine.theApp.couchDbManager;
		}
		return null;
	}

	boolean canSerialize(String name, Object value) {
		try {
			serialize(name, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static final class TypedValue {
		@SuppressWarnings("unused")
		public final String clazz;
		@SuppressWarnings("unused")
		public final Object value;
		@SuppressWarnings("unused")
		public final String format;

		TypedValue(String clazz, Object value, String format) {
			this.clazz = clazz;
			this.value = value;
			this.format = format;
		}
	}
}
