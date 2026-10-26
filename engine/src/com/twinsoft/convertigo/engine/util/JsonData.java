/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import org.codehaus.jettison.json.JSONException;
import java.util.*;
import org.mozilla.javascript.*;

/**
 * Request-owned Map/List snapshots and their ordinary Rhino projections.
 * Java containers never retain a realm or a mutable JavaScript value. Projection
 * uses public Rhino APIs; JavaScript writes affect only the projected objects.
 */
public final class JsonData {
	private JsonData() {
	}

	/**
	 * Builders are request-local. wrap() consumes and seals these containers;
	 * retained builder references cannot mutate a published snapshot. Foreign JSON
	 * containers are copied. Already sealed children can be shared safely.
	 */
	public static Map<String, Object> newObject() {
		return new ObjectData();
	}

	public static List<Object> newArray() {
		return new ArrayData();
	}

	private static void mutable(boolean sealed) {
		if (sealed)
			throw new UnsupportedOperationException("Published JSON data is immutable");
	}

	private static final class ObjectData extends AbstractMap<String, Object> {
		final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
		boolean sealed;
		int height;
		@Override public Object put(String key, Object value) { mutable(sealed); return values.put(Objects.requireNonNull(key), value); }
		@Override public Object remove(Object key) { mutable(sealed); return values.remove(key); }
		@Override public void clear() { mutable(sealed); values.clear(); }
		@Override public Object get(Object key) { return values.get(key); }
		@Override public boolean containsKey(Object key) { return values.containsKey(key); }
		@Override public int size() { return values.size(); }
		@Override public Set<Entry<String, Object>> entrySet() { return Collections.unmodifiableMap(values).entrySet(); }
	}

	private static final class ArrayData extends AbstractList<Object> {
		final ArrayList<Object> values = new ArrayList<>();
		boolean sealed;
		int height;
		@Override public Object get(int index) { return values.get(index); }
		@Override public int size() { return values.size(); }
		@Override public Object set(int index, Object value) { mutable(sealed); return values.set(index, value); }
		@Override public void add(int index, Object value) { mutable(sealed); values.add(index, value); modCount++; }
		@Override public Object remove(int index) { mutable(sealed); modCount++; return values.remove(index); }
	}

	/**
	 * Copy foreign Java containers; consume owned builders before projecting into Rhino.
	 */
	public static Object wrap(Object json, Scriptable scope) throws JSONException {
		return DetachedJson.toScriptable(snapshot(json), ScriptableObject.getTopLevelScope(scope));
	}

	/** Consume owned builders and copy foreign containers into an immutable snapshot. */
	public static Object snapshot(Object json) throws JSONException {
		return copy(json, new IdentityHashMap<>(), 0);
	}

	/**
	 * Check ordinary JSON projections without executing getters.
	 */
	public static boolean containsNoHandles(Object value) {
		return noHandles(value, new IdentityHashMap<>(), 0);
	}

	private static boolean noHandles(Object value, IdentityHashMap<Object, Boolean> seen, int depth) {
		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
				|| Undefined.isUndefined(value))
			return true;
		if (depth > 128)
			return false;
		if (!(value instanceof ScriptableObject s)
				|| !(s.getClass() == NativeObject.class || s.getClass() == NativeArray.class))
			return false;
		var expected = s instanceof NativeArray ? ScriptableObject.getArrayPrototype(s)
				: ScriptableObject.getObjectPrototype(s);
		if (s.getPrototype() != expected || expected
				.getPrototype() != (s instanceof NativeArray ? ScriptableObject.getObjectPrototype(s) : null))
			return false;
		for (var name : new String[] { "getClass", "__flowHandle" })
			if (ScriptableObject.hasProperty(s, name))
				return false;
		if (ScriptableObject.hasProperty(s, SymbolKey.TO_STRING_TAG))
			return false;
		if (seen.put(s, true) != null)
			return true;
		if (s instanceof NativeArray array) {
			for (int i = 0; i < array.getLength(); i++) {
				if (!s.has(i, s))
					return false;
				var getter = s.getGetterOrSetter(null, i, false);
				if (getter != null && !Undefined.isUndefined(getter))
					return false;
				if (!noHandles(ScriptableObject.getProperty(s, i), seen, depth + 1))
					return false;
			}
		} else
			for (var id : s.getIds()) {
				if (id instanceof Symbol)
					continue;
				var getter = id instanceof Number n ? s.getGetterOrSetter(null, n.intValue(), false)
						: s.getGetterOrSetter(id.toString(), 0, false);
				if (getter != null && !Undefined.isUndefined(getter))
					return false;
				var child = id instanceof Number n ? ScriptableObject.getProperty(s, n.intValue())
						: ScriptableObject.getProperty(s, id.toString());
				if (!noHandles(child, seen, depth + 1))
					return false;
			}
		return true;
	}

	private static Object copy(Object value, IdentityHashMap<Object, Boolean> ancestors, int depth)
			throws JSONException {
		if (depth > 128)
			throw new JSONException("JSON data exceeds depth limit");
		if (value == null)
			return null;
		if (value instanceof String || value instanceof Boolean)
			return value;
		if (value instanceof Number number) {
			if (!Double.isFinite(number.doubleValue()))
				throw new JSONException("JSON data requires finite numbers");
			return number.doubleValue();
		}
		if (value instanceof ObjectData object && object.sealed) {
			if (depth + object.height > 129)
				throw new JSONException("JSON data exceeds depth limit");
			return value;
		}
		if (value instanceof ArrayData array && array.sealed) {
			if (depth + array.height > 129)
				throw new JSONException("JSON data exceeds depth limit");
			return value;
		}
		if (ancestors.put(value, true) != null)
			throw new JSONException("JSON data contains a cycle");
		try {
			if (value instanceof Map<?, ?> object) {
				var result = object instanceof ObjectData owned ? owned : new ObjectData();
				for (var keys = object.keySet().iterator(); keys.hasNext();) {
					var key = (String) keys.next();
					var child = copy(object.get(key), ancestors, depth + 1);
					result.put(key, child);
					result.height = Math.max(result.height, 1 + height(child));
				}
				result.height = Math.max(1, result.height);
				result.sealed = true;
				return result;
			}
			if (value instanceof List<?> array) {
				var result = array instanceof ArrayData owned ? owned : new ArrayData();
				for (int i = 0; i < array.size(); i++) {
					var child = copy(array.get(i), ancestors, depth + 1);
					if (i == result.size()) result.add(child); else result.set(i, child);
					result.height = Math.max(result.height, 1 + height(child));
				}
				result.height = Math.max(1, result.height);
				result.sealed = true;
				return result;
			}
			throw new JSONException("JSON data contains a host value");
		} finally {
			ancestors.remove(value);
		}
	}

	private static int height(Object value) {
		return value instanceof ObjectData o ? o.height : value instanceof ArrayData a ? a.height : 0;
	}

}
