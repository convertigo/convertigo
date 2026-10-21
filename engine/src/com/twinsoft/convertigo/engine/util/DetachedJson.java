/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeSymbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.xml.XMLObject;

/**
 * Copies an execution result before its JavaScript realm is released. Only JSON
 * containers and immutable Java scalars leave this visitor; getters and toJSON
 * run synchronously in the borrowed realm, never in the response writer.
 * The provider must apply its result/handle policy before this boundary.
 */
public final class DetachedJson {
	private final Context cx;
	private final Scriptable scope;
	private final IdentityHashMap<Object, Boolean> ancestors = new IdentityHashMap<>();

	private DetachedJson(Context cx, Scriptable scope) { this.cx = cx; this.scope = scope; }

	public static JSONObject object(Object value, Context cx, Scriptable scope) throws JSONException {
		var detached = new DetachedJson(cx, scope).copy(value, "");
		if (detached instanceof JSONObject object) return object;
		throw new JSONException("Execution result must be a JSON object");
	}

	private Object copy(Object value, String key) throws JSONException {
		if (value instanceof Scriptable scriptable) {
			var toJson = ScriptableObject.getProperty(scriptable, "toJSON");
			if (toJson instanceof Callable callable) value = callable.call(cx, scope, scriptable, new Object[] {key});
		} else if (value instanceof BigInteger) {
			var boxed = Context.toObject(value, scope);
			var toJson = ScriptableObject.getProperty(boxed, "toJSON");
			if (toJson instanceof Callable callable) value = callable.call(cx, scope, boxed, new Object[] {key});
		}
		if (value instanceof NativeSymbol symbol && symbol.isSymbol() || value instanceof SymbolKey
				|| Undefined.isUndefined(value) || value == Scriptable.NOT_FOUND) return Scriptable.NOT_FOUND;
		// Rhino's boxed scalar classes are package-private. Match their exact Java
		// class, not a user-controlled className or a prototype.
		if (value instanceof Scriptable scriptable) {
			value = switch (value.getClass().getName()) {
			case "org.mozilla.javascript.NativeNumber" -> ScriptRuntime.toNumber(value);
			case "org.mozilla.javascript.NativeString" -> ScriptRuntime.toString(value);
			case "org.mozilla.javascript.NativeBoolean" -> scriptable.getDefaultValue(Boolean.class);
			case "org.mozilla.javascript.NativeBigInt" -> scriptable.getDefaultValue(BigInteger.class);
			default -> value;
			};
		}
		if (value instanceof XMLObject) value = value.toString();
		if (value instanceof NativeJavaObject wrapped) {
			var javaValue = wrapped.unwrap();
			if (javaValue == null || javaValue instanceof String || javaValue instanceof Boolean || javaValue instanceof Number) {
				value = javaValue;
			} else {
				return copyJava(javaValue);
			}
		}
		if (value == null) return JSONObject.EXPLICIT_NULL;
		if (value instanceof Boolean) return value;
		if (value instanceof CharSequence text) return text.toString();
		if (value instanceof BigInteger) throw new JSONException("BigInt cannot be serialized as JSON");
		if (value instanceof Number number) {
			double d = number.doubleValue();
			if (!Double.isFinite(d)) return JSONObject.EXPLICIT_NULL;
			if (d == Math.rint(d) && Math.abs(d) <= 9007199254740991d) return Long.valueOf((long) d);
			// Large integer decimal spellings can differ from a cast of the binary
			// double. Keep the integer that JSON.stringify exposed at this boundary.
			if (d == Math.rint(d) && Math.abs(d) < 1e21) {
				try { return Long.valueOf(ScriptRuntime.toString(number)); }
				catch (NumberFormatException outsideLong) { /* the text parser also used a double */ }
			}
			return Double.valueOf(d);
		}
		if (!(value instanceof Scriptable scriptable) || value instanceof Callable) return Scriptable.NOT_FOUND;
		enter(scriptable);
		try {
			if (scriptable instanceof NativeArray array) {
				var out = new JSONArray();
				long length = array.getLength();
				for (long i = 0; i < length; i++) {
					var item = i <= Integer.MAX_VALUE ? ScriptableObject.getProperty(array, (int) i)
							: ScriptableObject.getProperty(array, Long.toString(i));
					out.put(arrayValue(copy(item, Long.toString(i))));
				}
				return out;
			}
			return copyProperties(scriptable);
		} finally { ancestors.remove(scriptable); }
	}

	private JSONObject copyProperties(Scriptable value) throws JSONException {
		var out = new JSONObject();
		for (var id : value.getIds()) {
			if (id instanceof Symbol) continue;
			var name = id.toString();
			var item = id instanceof Number index ? ScriptableObject.getProperty(value, index.intValue())
					: ScriptableObject.getProperty(value, name);
			var child = copy(item, name);
			if (child != Scriptable.NOT_FOUND) out.put(name, child);
		}
		return out;
	}

	private Object copyJava(Object value) throws JSONException {
		enter(value);
		try {
			if (value instanceof Map<?, ?> map) {
				var object = cx.newObject(scope);
				for (var entry : map.entrySet()) {
					var key = entry.getKey();
					if (key instanceof Symbol) continue;
					var name = key.toString();
					// String keys win over non-string keys with the same spelling.
					if (!(key instanceof String) && map.containsKey(name)) continue;
					ScriptableObject.defineProperty(object, name, Context.javaToJS(entry.getValue(), scope), ScriptableObject.EMPTY);
				}
				return copyProperties(object);
			}
			if (value instanceof Collection<?> || value.getClass().isArray()) {
				var out = new JSONArray();
				if (value instanceof Collection<?> collection) {
					int i = 0;
					for (var item : collection) out.put(arrayValue(copy(Context.javaToJS(item, scope), Integer.toString(i++))));
				} else {
					for (int i = 0; i < Array.getLength(value); i++) out.put(arrayValue(copy(Context.javaToJS(Array.get(value, i), scope), Integer.toString(i))));
				}
				return out;
			}
			return copy(Context.javaToJS(cx.getJavaToJSONConverter().apply(value), scope), "");
		} finally { ancestors.remove(value); }
	}

	private static Object arrayValue(Object value) { return value == Scriptable.NOT_FOUND ? JSONObject.EXPLICIT_NULL : value; }

	private void enter(Object value) throws JSONException {
		if (ancestors.size() >= JSONObject.getGlobalRecursionDepthLimit()) throw new JSONException("JSON result exceeds recursion depth limit");
		if (ancestors.put(value, Boolean.TRUE) != null) throw new JSONException("Cyclic JSON result");
	}
}
