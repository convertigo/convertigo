/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.math.BigInteger;
import java.util.IdentityHashMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;

/**
 * One policy/detachment walk for ordinary data trees, without an intermediate
 * Rhino tree. An exotic value abandons the speculative copy BEFORE invoking
 * user code: the entire provider policy then runs once, in its original order.
 * This matters when a later getter changes what an earlier toJSON will see.
 */
public final class SanitizedJson {
	private static final class UsePolicy extends RuntimeException {
		private static final long serialVersionUID = 1L;
		UsePolicy(String reason) { super(reason, null, false, false); }
	}
	private final Scriptable objectPrototype, arrayPrototype;
	private final DetachedJson detacher;
	private final IdentityHashMap<Object, Boolean> objects = new IdentityHashMap<>();
	private final IdentityHashMap<Object, Boolean> arrays = new IdentityHashMap<>();

	private SanitizedJson(Context cx, Scriptable scope) {
		detacher = new DetachedJson(cx, scope);
		objectPrototype = ScriptableObject.getObjectPrototype(scope);
		arrayPrototype = ScriptableObject.getClassPrototype(scope, "Array");
		if (objectPrototype.getClass() != NativeObject.class || objectPrototype.getPrototype() != null
				|| !(arrayPrototype instanceof NativeArray) || arrayPrototype.getPrototype() != objectPrototype) throw new UsePolicy("prototype");
		checkAccessor((ScriptableObject) arrayPrototype, "map");
		checkArraySpecies(cx, scope);
		for (var prototype : new Scriptable[] {objectPrototype, arrayPrototype}) {
			for (var name : new String[] {"toJSON", "getClass", "__flowHandle"}) {
				if (ScriptableObject.hasProperty(prototype, name)) throw new UsePolicy("prototype-hook");
			}
			if (ScriptableObject.hasProperty(prototype, SymbolKey.TO_STRING_TAG)) throw new UsePolicy("prototype-tag");
		}
	}

	public static JSONObject object(Object value, Context cx, Scriptable scope, Callable policy) throws JSONException {
		Object copy;
		try { copy = new SanitizedJson(cx, scope).copy(value, ""); }
		catch (UsePolicy fallback) {
			try (var trace = AllocationTrace.open("boundary", "sanitize-fallback", fallback.getMessage())) { copy = new DetachedJson(cx, scope).copy(policy.call(cx, scope, scope, new Object[] {value}), ""); }
			catch (StackOverflowError cycle) { throw new JSONException("Cyclic or excessively nested JSON result"); }
		}
		if (copy instanceof JSONObject object) return object;
		throw new JSONException("Execution result must be a JSON object");
	}

	private Object copy(Object value, String key) throws JSONException {
		if (!(value instanceof Scriptable s)) {
			if (value instanceof BigInteger) throw new UsePolicy("bigint"); // BigInt.prototype.toJSON can run code.
			return detacher.copy(value, key);
		}
		if (!(s.getClass() == NativeObject.class && s.getPrototype() == objectPrototype)
				&& !(s.getClass() == NativeArray.class && s.getPrototype() == arrayPrototype)) throw new UsePolicy("exotic-container");
		if (ScriptableObject.hasProperty(s, "getClass") || ScriptableObject.hasProperty(s, "__flowHandle")
				|| ScriptableObject.hasProperty(s, SymbolKey.TO_STRING_TAG)) throw new UsePolicy("handle-or-tag");
		if (objects.containsKey(s)) return "[Circular]";
		if (objects.size() + arrays.size() >= JSONObject.getGlobalRecursionDepthLimit()) throw new JSONException("JSON result exceeds recursion depth limit");
		if (s.getClass() == NativeObject.class && s.getPrototype() == objectPrototype) {
			objects.put(s, Boolean.TRUE);
			try {
				var out = new DetachedJson.ObjectValue();
				for (var id : s.getIds()) {
					if (id instanceof Symbol) continue;
					var name = id.toString();
					if (name.equals("toJSON") || !name.equals("__proto__") && ScriptableObject.hasProperty(objectPrototype, name)) throw new UsePolicy("special-key");
					checkAccessor((ScriptableObject) s, id);
					var item = id instanceof Number index ? ScriptableObject.getProperty(s, index.intValue()) : ScriptableObject.getProperty(s, name);
					var child = copy(item, name);
					if (child != Scriptable.NOT_FOUND) out.put(name, child);
				}
				return out;
			} finally { objects.remove(s); }
		}
		if (s instanceof NativeArray array && s.getPrototype() == arrayPrototype
				&& !s.has("map", s) && !s.has("constructor", s)
				&& ordinaryMap(ScriptableObject.getProperty(arrayPrototype, "map"))) {
			if (arrays.put(s, Boolean.TRUE) != null) throw new UsePolicy("array-cycle");
			try {
				var out = new DetachedJson.ArrayValue();
				for (long i = 0, length = array.getLength(); i < length; i++) {
					var name = Long.toString(i);
					if (i > Integer.MAX_VALUE || ScriptableObject.hasProperty(arrayPrototype, (int) i)) throw new UsePolicy("array-index");
					checkAccessor(array, (int) i);
					var item = ScriptableObject.getProperty(s, (int) i);
					var child = item == Scriptable.NOT_FOUND ? item : copy(item, name);
					out.put(child == Scriptable.NOT_FOUND ? JSONObject.EXPLICIT_NULL : child);
				}
				return out;
			} finally { arrays.remove(s); }
		}
		throw new UsePolicy("array-map");
	}
	@SuppressWarnings("deprecation")
	private static void checkAccessor(ScriptableObject value, Object id) {
		var getter = id instanceof Number n ? value.getGetterOrSetter(null, n.intValue(), false)
				: value.getGetterOrSetter(id.toString(), 0, false);
		if (getter != null && !Undefined.isUndefined(getter)) throw new UsePolicy("accessor");
	}
	private static boolean ordinaryMap(Object map) {
		return map instanceof IdFunctionObject id && id.getFunctionName().equals("map")
				|| map instanceof LambdaFunction lambda && lambda.getFunctionName().equals("map");
	}
	private void checkArraySpecies(Context cx, Scriptable scope) {
		checkAccessor((ScriptableObject) arrayPrototype, "constructor");
		var constructor = ScriptableObject.getProperty(arrayPrototype, "constructor");
		if (!(constructor instanceof org.mozilla.javascript.BaseFunction array) || !nativeFunction(array, "Array")) throw new UsePolicy("array-constructor");
		// Inspect the species descriptor with the intrinsic, without calling a
		// user-supplied species getter. A customized constructor uses the policy.
		scope = ScriptableObject.getTopLevelScope(scope);
		if (!(scope instanceof ScriptableObject global)) throw new UsePolicy("global-scope");
		checkAccessor(global, "Object");
		var object = ScriptableObject.getProperty(scope, "Object");
		if (!(object instanceof org.mozilla.javascript.BaseFunction objectCtor) || !nativeFunction(objectCtor, "Object")) throw new UsePolicy("object-constructor");
		checkAccessor(objectCtor, "getOwnPropertyDescriptor");
		var descriptorFunction = ScriptableObject.getProperty(objectCtor, "getOwnPropertyDescriptor");
		if (!(descriptorFunction instanceof org.mozilla.javascript.BaseFunction descriptor) || !nativeFunction(descriptor, "getOwnPropertyDescriptor")) throw new UsePolicy("descriptor-intrinsic");
		var description = descriptor.call(cx, scope, objectCtor, new Object[] {array, SymbolKey.SPECIES});
		if (!(description instanceof Scriptable d)
				|| !(ScriptableObject.getProperty(d, "get") instanceof LambdaFunction getter)
				|| !getter.getFunctionName().equals("get [Symbol.species]")) throw new UsePolicy("array-species");
	}
	private static boolean nativeFunction(org.mozilla.javascript.BaseFunction value, String name) {
		return (value instanceof IdFunctionObject || value instanceof LambdaFunction) && value.getFunctionName().equals(name);
	}
}
