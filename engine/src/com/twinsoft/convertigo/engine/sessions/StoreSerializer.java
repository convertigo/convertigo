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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

/**
 * Reflection-based serializer used by session and context stores.
 */
final class StoreSerializer {
	Map<String, String> serialize(Object bean) {
		return serialize(bean, Map.of(), null);
	}

	Map<String, String> serialize(Object bean, Map<String, Object> overrides) {
		return serialize(bean, overrides, null);
	}

	Map<String, String> serialize(Object bean, Map<String, Object> overrides,
			BiFunction<Field, Object, Object> transformer) {
		var snapshot = new LinkedHashMap<String, String>();
		if (bean == null) {
			return snapshot;
		}
		for (var field : fields(bean.getClass())) {
			if (skip(field)) {
				continue;
			}
			var name = resolveName(field);
			Object value = overrides.containsKey(name) ? overrides.get(name) : readField(bean, field);
			if (transformer != null) {
				value = transformer.apply(field, value);
			}
			if (value == null) {
				continue;
			}
			try {
				snapshot.put(name, JsonCodec.MAPPER.writeValueAsString(value));
			} catch (Exception e) {
				debug("(StoreSerializer) Skip field '" + name + "' (" + field.getType().getName() + "): " + e);
			}
		}
		return snapshot;
	}

	boolean canSerializeValue(Object value) {
		try {
			JsonCodec.MAPPER.writeValueAsString(value);
			return true;
		} catch (Exception e) {
			debug("(StoreSerializer) value not serializable: " + e.getMessage());
			return false;
		}
	}

	void populate(Object target, Map<String, String> snapshot) {
		if (target == null || snapshot == null || snapshot.isEmpty()) {
			return;
		}
		for (var field : fields(target.getClass())) {
			if (skip(field)) {
				continue;
			}
			var name = resolveName(field);
			var raw = snapshot.get(name);
			if (raw == null) {
				continue;
			}
			try {
				Type targetType = field.getGenericType();
				var value = JsonCodec.MAPPER.readValue(raw, JsonCodec.MAPPER.getTypeFactory().constructType(targetType));
				if (value == null && field.getType().isPrimitive()) {
					continue;
				}
				if (value != null) {
					if (Map.class.isAssignableFrom(field.getType()) && !(value instanceof ConcurrentHashMap)) {
						value = new ConcurrentHashMap<>(GenericUtils.cast(value));
					} else if (Set.class.isAssignableFrom(field.getType()) && value instanceof List) {
						value = Set.copyOf(GenericUtils.cast(value));
					}
				}
				field.setAccessible(true);
				field.set(target, value);
			} catch (Exception e) {
				debug("(StoreSerializer) Failed to set field '" + name + "': " + e.getMessage());
			}
		}
	}

	private static List<Field> fields(Class<?> type) {
		var fields = new ArrayList<Field>();
		var current = type;
		while (current != null && current != Object.class) {
			for (var f : current.getDeclaredFields()) {
				fields.add(f);
			}
			current = current.getSuperclass();
		}
		return fields;
	}

	private static boolean skip(Field field) {
		var mods = field.getModifiers();
		return Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isAnnotationPresent(StoreIgnore.class);
	}

	private static String resolveName(Field field) {
		var ann = field.getAnnotation(StoreName.class);
		return ann != null ? ann.value() : field.getName();
	}

	private static Object readField(Object bean, Field field) {
		try {
			field.setAccessible(true);
			return field.get(bean);
		} catch (Exception e) {
			debug("(StoreSerializer) Failed to read field '" + field.getName() + "': " + e.getMessage());
			return null;
		}
	}

	private static void debug(String message) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug(message);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}
}
