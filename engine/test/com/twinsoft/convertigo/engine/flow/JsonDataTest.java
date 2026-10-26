/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.List;
import org.junit.Test;
import org.mozilla.javascript.*;
import org.codehaus.jettison.json.*;
import com.twinsoft.convertigo.engine.util.*;

public class JsonDataTest {
	private Scriptable scope(Context cx) throws Exception {
		cx.setLanguageVersion(Context.VERSION_ES6);
		var s = cx.initStandardObjects();
		var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		var src = Files.readString(Path.of(root == null ? "../../lib_flow_engine/_flow" : root)
				.resolve("modules/runtime-handle-utils.js"));
		ScriptableObject.putProperty(s, "handles", cx.evaluateString(s, src, "handles", 1, null));
		cx.evaluateString(s,
				"var env={raise:function(c,m){throw Error(c);},NativeJavaObject:Packages.org.mozilla.javascript.NativeJavaObject,JavaObject:java.lang.Object,JavaString:java.lang.String,JavaNumber:java.lang.Number,JavaBoolean:java.lang.Boolean};var policy=function(v){return handles.sanitizeSerializable(v,'result',env);};",
				"policy", 1, null);
		return s;
	}

	private String run(String script, boolean view) throws Exception {
		try (var cx = Context.enter()) {
			var s = scope(cx);
			var data = JsonValues.parse(
					"{\"x\":1,\"nested\":{\"value\":2},\"list\":[{\"a\":3}],\"__proto__\":{\"own\":true}}");
			ScriptableObject.putProperty(s, "data", view ? JsonData.wrap(data, s) : DetachedJson.toScriptable(data, s));
			return Context.toString(cx.evaluateString(s, script, "test", 1, null));
		}
	}

	@Test
	public void untouchedNumbersAndNumericKeysMatchTheOrdinaryBoundary() throws Exception {
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			var json = JsonValues.parse("{\"2\":2,\"1\":1,\"values\":[-0.0,9007199254740991,1000000000000000100,1.5]}");
			var expected = DetachedJson.object(DetachedJson.toScriptable(json, scope), cx, scope);
			var view = JsonData.wrap(json, scope);
			var actual = SanitizedJson.object(view, cx, scope, (c, s, t, a) -> {
				throw new AssertionError("fallback");
			});
			// Numeric property spelling is normalized by the ordinary Rhino projection.
			assertEquals(expected.getJSONArray("values").toString(), actual.getJSONArray("values").toString());
			assertEquals("1", actual.keys().next());
		}
	}

	@Test
	public void ordinaryObjectOperationsHaveIdenticalResults() throws Exception {
		for (var code : List.of("JSON.stringify(data)", "Object.keys(data).join(',')",
				"Object.getOwnPropertyNames(data).join(',')",
				"JSON.stringify(Object.getOwnPropertyDescriptor(data,'x'))", "JSON.stringify(Object.assign({},data))",
				"Reflect.ownKeys(data).join(',')", "var a=[];for(var k in data)a.push(k);a.join(',')",
				"data.nested.value=8;data.list.push({b:4});JSON.stringify(data)",
				"delete data.x;data.y=7;JSON.stringify(data)",
				"Object.defineProperty(data,'x',{value:6,enumerable:false});JSON.stringify(data)",
				"Object.defineProperty(data,'x',{get:function(){return 9;}});JSON.stringify(data)",
				"Object.freeze(data);JSON.stringify(data)+Object.isFrozen(data)",
				"Object.seal(data);JSON.stringify(data)+Object.isSealed(data)",
				"Object.preventExtensions(data);JSON.stringify(data)+Object.isExtensible(data)",
				"Object.setPrototypeOf(data,{other:8});JSON.stringify(data)+data.other"))
			assertEquals(code, run(code, false), run(code, true));
	}

	@Test
	public void numericNamesHaveOrdinaryOwnPropertySemantics() throws Exception {
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			String json = "{\"2\":\"two\",\"1\":\"one\",\"4294967294\":4,\"-1\":5,\"x\":8}";
			ScriptableObject.putProperty(scope, "data", JsonData.wrap(JsonValues.parse(json), scope));
			ScriptableObject.putProperty(scope, "json", json);
			assertEquals(Boolean.TRUE, cx.evaluateString(scope,
					"var ref=JSON.parse(json);JSON.stringify(Object.keys(data).map(function(k){return [k,data[k]];}))===JSON.stringify(Object.keys(ref).map(function(k){return [k,ref[k]];}))",
					"test", 1, null));
		}
	}

	@Test
	public void projectedRowsHonorTheResultDepthLimit() throws Exception {
		int previous = JSONObject.getGlobalRecursionDepthLimit();
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			var data = JsonData.wrap(JsonValues.parse("{\"a\":{\"b\":{\"c\":1}}}"), scope);
			JSONObject.setGlobalRecursionDepthLimit(2);
			try {
				SanitizedJson.object(data, cx, scope, (c, s, t, a) -> {
					throw new AssertionError("fallback");
				});
				fail("depth");
			} catch (JSONException expected) {
			}
		} finally {
			JSONObject.setGlobalRecursionDepthLimit(previous);
		}
	}

	@Test
	public void speculativeHandleWalkNeverExecutesGetters() throws Exception {
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			for (var source : List.of("Object.create({get x(){calls++;return 1;}})",
					"Object.defineProperty({},'x',{enumerable:true,get:function(){calls++;return 1;}})",
					"Object.defineProperty(Array.prototype,'0',{configurable:true,get:function(){calls++;return 1;}});new Array(1)")) {
				var data = cx.evaluateString(scope, "var calls=0;" + source, "test", 1, null);
				assertFalse(JsonData.containsNoHandles(data));
				assertEquals(0, ((Number) ScriptableObject.getProperty(scope, "calls")).intValue());
			}
		}
	}

	@Test
	public void ownedBuildersAreConsumedAndCannotMutateProjections() throws Exception {
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			var object = JsonData.newObject();
			var array = JsonData.newArray();
			array.add(1);
			object.put("array", array);
			var view = JsonData.wrap(object, scope);
			assertSame(object, JsonData.snapshot(object));
			assertEquals(NativeObject.class, view.getClass());
			for (Runnable mutation : List.<Runnable>of(() -> {
				object.put("__flowHandle", true);
			}, () -> object.remove("array"), () -> {
				var keys = object.keySet().iterator();
				keys.next();
				keys.remove();
			}, () -> array.add(2), () -> array.remove(0), () -> {
				array.set(0, 3);
			})) {
				try {
					mutation.run();
					fail("mutable published data");
				} catch (UnsupportedOperationException expected) {
				}
			}
			assertTrue(JsonData.containsNoHandles(view));
			ScriptableObject.putProperty(scope, "data", view);
			cx.evaluateString(scope, "data.array.push(4)", "test", 1, null);
			assertEquals(1, array.size());
		}
	}

	@Test
	public void detachedDataDoesNotRetainExternalJavaMutation() throws Exception {
		try (var cx = Context.enter()) {
			var s = scope(cx);
			var original = JsonValues.parse("{\"rows\":[{\"value\":1}]}");
			var view = JsonData.wrap(original, s);
			JsonValues.map(JsonValues.list(JsonValues.map(original).get("rows")).get(0)).put("value", 9);
			var result = SanitizedJson.object(view, cx, s, (Callable) ScriptableObject.getProperty(s, "policy"));
			assertEquals(1, result.getJSONArray("rows").getJSONObject(0).getInt("value"));
		}
	}

	@Test
	public void ordinaryRowsDetachWithoutASecondPolicyWalk() throws Exception {
		try (var cx = Context.enter()) {
			var s = scope(cx);
			var view = JsonData.wrap(JsonValues.parse("{\"cells\":{\"x\":{\"value\":1}}}"), s);
			assertTrue(JsonData.containsNoHandles(view));
			assertEquals(NativeObject.class, view.getClass());
			var result = SanitizedJson.object(view, cx, s, (c, scope, self, args) -> {
				throw new AssertionError("fallback");
			});
			assertEquals(NativeObject.class, view.getClass());
			assertEquals(1, result.getJSONObject("cells").getJSONObject("x").getInt("value"));
		}
	}

	@Test
	public void mutationsHandlesGettersAndPrototypeHooksNeverBypassPolicy() throws Exception {
		for (var code : List.of("data.nested={__flowHandle:true}",
				"Object.defineProperty(data,'nested',{get:function(){return {__flowHandle:true};}})",
				"Object.prototype.__flowHandle=true")) {
			try (var cx = Context.enter()) {
				var s = scope(cx);
				var view = JsonData.wrap(JsonValues.parse("{\"nested\":{\"value\":1}}"), s);
				ScriptableObject.putProperty(s, "data", view);
				cx.evaluateString(s, code, "test", 1, null);
				assertFalse(JsonData.containsNoHandles(view));
				try {
					SanitizedJson.object(view, cx, s, (Callable) ScriptableObject.getProperty(s, "policy"));
					fail("handle");
				} catch (JavaScriptException expected) {
				}
			}
		}
	}

	@Test
	public void handlesAlreadyInJsonAndHostValuesAreNotTrusted() throws Exception {
		try (var cx = Context.enter()) {
			var s = scope(cx);
			var view = JsonData.wrap(JsonValues.parse("{\"nested\":{\"__flowHandle\":true}}"), s);
			assertFalse(JsonData.containsNoHandles(view));
			try {
				SanitizedJson.object(view, cx, s, (Callable) ScriptableObject.getProperty(s, "policy"));
				fail("handle");
			} catch (JavaScriptException expected) {
			}
			try {
				JsonData.wrap(JsonValues.object("host", new java.io.File("synthetic")), s);
				fail("host");
			} catch (JSONException expected) {
			}
		}
	}

	@Test
	public void mutatedRowsRemainOrdinaryInTheFusedVisitor() throws Exception {
		try (var cx = Context.enter()) {
			var s = scope(cx);
			var view = JsonData.wrap(JsonValues.parse("{\"n\":1}"), s);
			ScriptableObject.putProperty(s, "data", view);
			cx.evaluateString(s, "data.n=2", "test", 1, null);
			assertEquals(NativeObject.class, view.getClass());
			assertEquals(2, SanitizedJson.object(view, cx, s, (c, realm, self, args) -> {
				throw new AssertionError("fallback");
			}).getInt("n"));
		}
	}
}
