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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.engine.enums.SessionAttribute;

final class SessionValueCodec {
	String serialize(String name, Object value) throws Exception {
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
		return JsonCodec.MAPPER.writeValueAsString(new TypedValue(value));
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
			return JsonCodec.MAPPER.convertValue(valueNode, JsonCodec.MAPPER.getTypeFactory().constructType((Type) hint.expectedClass()));
		}
		if (isWrapped) {
			var clazzNode = node.get("clazz");
			if (clazzNode != null) {
				var className = clazzNode.asText();
				try {
					var cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
					if (Set.class.isAssignableFrom(cls)) {
						var list = JsonCodec.MAPPER.<List<Object>>convertValue(valueNode, JsonCodec.MAPPER.getTypeFactory()
								.constructCollectionType(ArrayList.class, Object.class));
						return new HashSet<>(list);
					}
					return JsonCodec.MAPPER.convertValue(valueNode, JsonCodec.MAPPER.getTypeFactory().constructType((Type) cls));
				} catch (ClassNotFoundException e) {
					// fall through
				}
			}
		}
		return JsonCodec.MAPPER.convertValue(valueNode, Object.class);
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

		TypedValue(Object value) {
			this.value = value;
			this.clazz = value != null ? value.getClass().getName() : null;
		}
	}
}
