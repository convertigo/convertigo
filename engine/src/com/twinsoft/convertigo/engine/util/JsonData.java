/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.util.IdentityHashMap;
import org.codehaus.jettison.json.*;
import org.mozilla.javascript.*;

/**
 * JSON-owned objects, materialized as ordinary JS properties only when
 * observed. Arrays remain real Rhino arrays. A bulk operator can read an
 * unobserved row as Java JSON without allocating a JS object for each cell. Any
 * JS observation or mutation materializes the row and permanently disables its
 * shortcut. No scope, Java host value or mutable JS value is stored in the JSON
 * tree.
 */
public final class JsonData {
	private JsonData() {
	}

	/**
	 * Builders are request-local. wrap() consumes and seals these containers;
	 * retained builder references cannot mutate a published view. Foreign JSON
	 * containers are copied. Already sealed children can be shared safely.
	 */
	public static JSONObject newObject() {
		return new ObjectData();
	}

	public static JSONArray newArray() {
		return new ArrayData();
	}

	private static void mutable(boolean sealed) {
		if (sealed)
			throw new UnsupportedOperationException("Published JSON data is immutable");
	}

	private static final class ObjectData extends JSONObject {
		boolean sealed;
		int height;
		boolean handles;

		@Override
		protected JSONObject doPut(String key, Object value, int depth, boolean copy) throws JSONException {
			mutable(sealed);
			return super.doPut(key, value, depth, copy);
		}

		@Override
		public Object remove(String key) {
			mutable(sealed);
			return super.remove(key);
		}

		@Override
		public java.util.Iterator keys() {
			var source = super.keys();
			return new java.util.Iterator() {
				public boolean hasNext() {
					return source.hasNext();
				}

				public Object next() {
					return source.next();
				}
			};
		}

		@Override
		public void setRecursionDepthLimit(int limit) {
			mutable(sealed);
			super.setRecursionDepthLimit(limit);
		}

		@Override
		public void setEscapeForwardSlashAlways(boolean value) {
			mutable(sealed);
			super.setEscapeForwardSlashAlways(value);
		}
	}

	private static final class ArrayData extends JSONArray {
		boolean sealed;
		int height;
		boolean handles;

		@Override
		public JSONArray put(Object value) {
			mutable(sealed);
			return super.put(value);
		}

		@Override
		public JSONArray put(int index, Object value) throws JSONException {
			mutable(sealed);
			return super.put(index, value);
		}

		@Override
		public JSONArray remove(Object value) {
			mutable(sealed);
			return super.remove(value);
		}

		@Override
		public JSONArray removeAt(int index) {
			mutable(sealed);
			return super.removeAt(index);
		}

		@Override
		public void setEscapeForwardSlashAlways(boolean value) {
			mutable(sealed);
			super.setEscapeForwardSlashAlways(value);
		}
	}

	/**
	 * Copy foreign Java containers; consume owned builders before exposing a view.
	 */
	public static Object wrap(Object json, Scriptable scope) throws JSONException {
		return view(copy(json, new IdentityHashMap<>(), 0), ScriptableObject.getTopLevelScope(scope));
	}

	private static Object view(Object json, Scriptable scope) throws JSONException {
		if (json instanceof JSONObject object)
			return new ObjectView(object, scope);
		if (json instanceof JSONArray array) {
			var values = new Object[array.length()];
			for (int i = 0; i < values.length; i++)
				values[i] = view(array.get(i), scope);
			var result = new NativeArray(values);
			result.setParentScope(scope);
			result.setPrototype(ScriptableObject.getArrayPrototype(scope));
			return result;
		}
		return json == JSONObject.NULL || json == JSONObject.EXPLICIT_NULL ? null : json;
	}

	/**
	 * Internal bulk-operator read-only view. Never mutate or retain the returned
	 * tree.
	 */
	public static Object peek(Object value) {
		return value instanceof ObjectView object && object.pristine() ? object.data : Scriptable.NOT_FOUND;
	}

	/**
	 * A JSON tree cannot hold a runtime handle; a mutated view uses normal checks.
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
		if (value instanceof ObjectView view && view.pristine() && view.noHandles)
			return true;
		if (!(value instanceof ScriptableObject s)
				|| !(s.getClass() == NativeObject.class || s.getClass() == NativeArray.class || isView(s)))
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

	static boolean isView(Object value) {
		return value instanceof ObjectView;
	}

	private static Object copy(Object value, IdentityHashMap<Object, Boolean> ancestors, int depth)
			throws JSONException {
		if (depth > 128)
			throw new JSONException("JSON data exceeds depth limit");
		if (value == null || value == JSONObject.NULL || value == JSONObject.EXPLICIT_NULL)
			return JSONObject.EXPLICIT_NULL;
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
			if (value instanceof JSONObject object) {
				var result = object instanceof ObjectData owned ? owned : new ObjectData();
				for (var keys = object.keys(); keys.hasNext();) {
					var key = (String) keys.next();
					var child = copy(object.get(key), ancestors, depth + 1);
					result.put(key, child);
					result.height = Math.max(result.height, 1 + height(child));
					result.handles |= (key.equals("__flowHandle") && Boolean.TRUE.equals(child)) || hasHandles(child);
				}
				result.height = Math.max(1, result.height);
				result.sealed = true;
				return result;
			}
			if (value instanceof JSONArray array) {
				var result = array instanceof ArrayData owned ? owned : new ArrayData();
				for (int i = 0; i < array.length(); i++) {
					var child = copy(array.get(i), ancestors, depth + 1);
					result.put(i, child);
					result.height = Math.max(result.height, 1 + height(child));
					result.handles |= hasHandles(child);
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

	private static boolean hasHandles(Object value) {
		return value instanceof ObjectData o ? o.handles : value instanceof ArrayData a && a.handles;
	}

	static Iterable<String> keys(JSONObject object) {
		boolean indexed = false;
		for (var it = object.keys(); it.hasNext();) {
			long n = ScriptRuntime.indexFromString((String) it.next());
			if (n >= 0 && n < 4294967295L) {
				indexed = true;
				break;
			}
		}
		if (!indexed)
			return () -> object.keys();
		var indices = new java.util.TreeMap<Long, String>();
		var others = new java.util.ArrayList<String>();
		for (var it = object.keys(); it.hasNext();) {
			var key = (String) it.next();
			long index = ScriptRuntime.indexFromString(key);
			if (index >= 0 && index < 4294967295L)
				indices.put(index, key);
			else
				others.add(key);
		}
		var result = new java.util.ArrayList<String>(indices.values());
		result.addAll(others);
		return result;
	}

	private static final class ObjectView extends MaterializedObject {
		private static final long serialVersionUID = 1L;
		private JSONObject data;
		private final Scriptable originalPrototype;
		private final boolean noHandles;

		ObjectView(JSONObject data, Scriptable scope) {
			originalPrototype = ScriptableObject.getObjectPrototype(scope);
			super.setParentScope(scope);
			super.setPrototype(originalPrototype);
			this.data = data;
			noHandles = !hasHandles(data);
		}

		boolean pristine() {
			return data != null && getPrototype() == originalPrototype
					&& originalPrototype.getClass() == NativeObject.class && originalPrototype.getPrototype() == null
					&& !ScriptableObject.hasProperty(originalPrototype, "__flowHandle")
					&& !ScriptableObject.hasProperty(originalPrototype, "getClass")
					&& !ScriptableObject.hasProperty(originalPrototype, SymbolKey.TO_STRING_TAG);
		}

		@Override
		protected void materialize() {
			var source = data;
			if (source == null)
				return;
			data = null;
			try {
				for (var keys = source.keys(); keys.hasNext();) {
					var key = (String) keys.next();
					super.defineOwnProperty(Context.getCurrentContext(), key,
							new DescriptorInfo(true, true, true, view(source.get(key), getParentScope())), false);
				}
			} catch (JSONException impossible) {
				throw Context.throwAsScriptRuntimeEx(impossible);
			}
		}

		@Override
		public Object get(String name, Scriptable start) {
			if (data != null && !data.has(name))
				return NOT_FOUND;
			materialize();
			return super.get(name, start);
		}

		@Override
		public Object get(int index, Scriptable start) {
			materialize();
			return super.get(index, start);
		}

		@Override
		public Object get(Symbol key, Scriptable start) {
			materialize();
			return super.get(key, start);
		}

		@Override
		public boolean has(String name, Scriptable start) {
			return data != null ? data.has(name) : super.has(name, start);
		}

		@Override
		public boolean has(int index, Scriptable start) {
			materialize();
			return super.has(index, start);
		}

		@Override
		public boolean has(Symbol key, Scriptable start) {
			return data != null ? false : super.has(key, start);
		}

		@Override
		public Object[] getIds() {
			materialize();
			return super.getIds();
		}

		@Override
		public Object[] getAllIds() {
			materialize();
			return super.getAllIds();
		}

		@Override
		public void put(String name, Scriptable start, Object value) {
			materialize();
			super.put(name, start, value);
		}

		@Override
		public void put(int index, Scriptable start, Object value) {
			materialize();
			super.put(index, start, value);
		}

		@Override
		public void put(Symbol key, Scriptable start, Object value) {
			materialize();
			super.put(key, start, value);
		}

		@Override
		public void delete(String name) {
			materialize();
			super.delete(name);
		}

		@Override
		public void delete(int index) {
			materialize();
			super.delete(index);
		}

		@Override
		public void delete(Symbol key) {
			materialize();
			super.delete(key);
		}

		@Override
		public void defineProperty(String name, Object value, int attributes) {
			materialize();
			super.defineProperty(name, value, attributes);
		}

		@Override
		public void defineProperty(Symbol name, Object value, int attributes) {
			materialize();
			super.defineProperty(name, value, attributes);
		}

		@Override
		protected DescriptorInfo getOwnPropertyDescriptor(Context cx, Object id) {
			materialize();
			return super.getOwnPropertyDescriptor(cx, id);
		}

		@Override
		protected boolean defineOwnProperty(Context cx, Object id, DescriptorInfo desc, boolean check) {
			materialize();
			return super.defineOwnProperty(cx, id, desc, check);
		}

		@Override
		public void setGetterOrSetter(Object name, int index, Callable value, boolean setter) {
			materialize();
			super.setGetterOrSetter(name, index, value, setter);
		}

		@Override
		public void setAttributes(String name, int value) {
			materialize();
			super.setAttributes(name, value);
		}

		@Override
		public void setAttributes(int index, int value) {
			materialize();
			super.setAttributes(index, value);
		}

		@Override
		public void setAttributes(Symbol key, int value) {
			materialize();
			super.setAttributes(key, value);
		}

		@Override
		public int getAttributes(String name) {
			materialize();
			return super.getAttributes(name);
		}

		@Override
		public int getAttributes(int index) {
			materialize();
			return super.getAttributes(index);
		}

		@Override
		public int getAttributes(Symbol key) {
			materialize();
			return super.getAttributes(key);
		}

		@Override
		public boolean preventExtensions() {
			materialize();
			return super.preventExtensions();
		}

		@Override
		public void sealObject() {
			materialize();
			super.sealObject();
		}

		@Override
		public void setPrototype(Scriptable value) {
			materialize();
			super.setPrototype(value);
		}
	}
}
