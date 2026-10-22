/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import com.twinsoft.convertigo.engine.util.DetachedJson;
import com.twinsoft.convertigo.engine.util.SanitizedJson;

public class SanitizedResultBoundaryTest {
	private Scriptable scope(Context cx) throws Exception {
		cx.setLanguageVersion(Context.VERSION_ES6);
		var scope = cx.initStandardObjects();
		var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		var source = Files.readString((root == null ? Path.of("../../lib_flow_engine/_flow") : Path.of(root)).resolve("modules/runtime-handle-utils.js"));
		ScriptableObject.putProperty(scope, "handles", cx.evaluateString(scope, source, "runtime-handle-utils.js", 1, null));
		cx.evaluateString(scope, "var reads=0;var env={raise:function(code,message){var e=new Error(message);e.code=code;throw e;},"
				+ "NativeJavaObject:Packages.org.mozilla.javascript.NativeJavaObject,JavaObject:java.lang.Object,"
				+ "JavaString:java.lang.String,JavaBoolean:java.lang.Boolean,JavaNumber:java.lang.Number};"
				+ "var policy=function(value,seen){return handles.sanitizeSerializable(value,'result',env,seen);};", "policy", 1, null);
		return scope;
	}
	private JSONObject copy(String expression, boolean fused) throws Exception {
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			var value = cx.evaluateString(scope, expression, "fixture", 1, null);
			var policy = (Callable) ScriptableObject.getProperty(scope, "policy");
			var out = fused ? SanitizedJson.object(value, cx, scope, policy)
					: DetachedJson.object(policy.call(cx, scope, scope, new Object[] {value}), cx, scope);
			out.put("readsAfter", Context.toNumber(ScriptableObject.getProperty(scope, "reads")));
			return out;
		}
	}
	@Test public void fusedOrdinaryContainersMatchTheProviderPolicy() throws Exception {
		for (var source : List.of(
				"({a:[,undefined,null,NaN,Infinity,-0,1.5,9007199254740991,1000000000000000100],text:'é☃<>&\\n',fn:function(){}})",
				"JSON.parse('{\"__proto__\":{\"safe\":true},\"constructor\":\"own\",\"2\":2,\"1\":1}')",
				"(function(){var shared={n:1};var v={a:shared,b:shared};v.self=v;return v;})()",
				"(function(){var a={};Object.defineProperty(a,'v',{enumerable:true,get:function(){reads++;return {n:reads};}});return a;})()",
				"(function(){var a=[];a.length=3;Object.defineProperty(a,1,{get:function(){reads++;return 'present';}});return {a:a};})()",
				"(function(){var a=[];var o={a:a};a.push(o);return {a:a};})()",
				"({a:{value:new java.lang.Integer(7),bool:new java.lang.Boolean(false),text:new java.lang.String('x')}})",
				"({a:new Number(3),b:new String('abc'),c:new Date(0)})",
				"({a:Object.create(null)})")) assertEquals(source, copy(source, false).toString(), copy(source, true).toString());
	}
	@Test public void ordinaryTreesAreDetachedWithoutBuildingASanitizedRhinoTree() throws Exception {
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			var value = cx.evaluateString(scope, "({items:[{n:1,text:'plain'},null,2],ok:true})", "fixture", 1, null);
			var result = SanitizedJson.object(value, cx, scope, (context, realm, self, args) -> { throw new AssertionError("unexpected fallback"); });
			assertEquals(1, result.getJSONArray("items").getJSONObject(0).getInt("n"));
			var activation = new org.mozilla.javascript.NativeObject(); activation.setParentScope(scope);
			result = SanitizedJson.object(value, cx, activation, (context, realm, self, args) -> { throw new AssertionError("activation scope fallback"); });
			assertEquals(1, result.getJSONArray("items").getJSONObject(0).getInt("n"));
		}
	}
	@Test public void exoticConversionsAndHostValuesUseTheExistingPolicyOnce() throws Exception {
		for (var source : List.of(
				"(function(){var a=[1,2];a.map=function(f){reads++;return Array.prototype.map.call(this,f);};return {a:a};})()",
				"(function(){Object.defineProperty(Array,Symbol.species,{get:function(){reads++;return Array;}});return {a:[1,2],b:[3]};})()",
				"({a:{n:1,toJSON:function(key){reads++;return {n:this.n,key:key};}}})",
				"(function(){var state=0;var a={first:{toJSON:function(){return state;}}};Object.defineProperty(a,'later',{enumerable:true,get:function(){reads++;state=7;return 1;}});return a;})()",
				"(function(){var a={n:1};Object.defineProperty(a,'toJSON',{enumerable:true,get:function(){reads++;return function(){return {n:this.n};};}});return {a:a};})()",
				"(function(){var a={n:1};Object.defineProperty(a,'toJSON',{value:function(){throw Error('ignored');}});return {a:a};})()",
				"(function(){var a={};Object.defineProperty(a,'getClass',{get:function(){reads++;throw Error('ignored');}});return {a:a};})()",
				"({a:{getClass:function(){return {getName:function(){return 'java.lang.String';}};},toString:function(){return 'custom';}}})",
				"(function(){var a={};a.other={toJSON:function(){return {n:1};},back:a};return a;})()",
				"(function(){Object.prototype.toJSON=function(){reads++;return 'replacement';};return {a:1};})()")) {
			// A root toJSON can replace the required envelope; both paths reject it.
			if (source.contains("Object.prototype.toJSON")) {
				for (boolean fused : new boolean[] {false,true}) try { copy(source, fused); fail("invalid envelope"); } catch (org.codehaus.jettison.json.JSONException expected) { }
			} else assertEquals(source, copy(source, false).toString(), copy(source, true).toString());
		}
	}
	@Test public void unsupportedHostObjectsRemainRejectedByThePolicy() throws Exception {
		for (boolean fused : new boolean[] {false,true}) {
			try { copy("({a:new java.io.File('fixture')})", fused); fail("host policy must reject"); }
			catch (org.mozilla.javascript.RhinoException expected) { }
			assertNull(Context.getCurrentContext());
		}
	}
	@Test public void handlesAndThrowingGettersFailInsideTheRealm() throws Exception {
		for (var source : List.of("({a:[{__flowHandle:true,__flowHandleType:'file'}]})",
				"(function(){var a={};Object.defineProperty(a,'__flowHandle',{get:function(){return true;}});return {a:a};})()",
				"({a:Object.create({__flowHandle:true})})")) {
			for (boolean fused : new boolean[] {false,true}) {
				try { copy(source, fused); fail("handle escaped"); }
				catch (JavaScriptException expected) { assertEquals("RUNTIME_HANDLE_IN_RESULT", Context.toString(ScriptableObject.getProperty((Scriptable) expected.getValue(), "code"))); }
				assertNull(Context.getCurrentContext());
			}
		}
		try { copy("(function(){var a={};Object.defineProperty(a,'value',{enumerable:true,get:function(){throw Error('getter');}});return a;})()", true); fail("getter must fail"); }
		catch (JavaScriptException expected) { }
		assertNull(Context.getCurrentContext());
	}
	@Test public void copiesDoNotRetainAliasesOrAnOwningScope() throws Exception {
		JSONObject result;
		try (var cx = Context.enter()) {
			var scope = scope(cx);
			var value = cx.evaluateString(scope, "var shared={n:1};({a:shared,b:shared})", "fixture", 1, null);
			result = SanitizedJson.object(value, cx, scope, (Callable) ScriptableObject.getProperty(scope, "policy"));
			cx.evaluateString(scope, "shared.n=99", "reuse", 1, null);
		}
		result.getJSONObject("a").put("n", 7);
		assertEquals(1, result.getJSONObject("b").getInt("n"));
		assertNull(Context.getCurrentContext());
	}
	@Test public void arrayCyclesAndDepthLimitsAreRejectedBeforeWriting() throws Exception {
		try { copy("(function(){var a=[];a.push(a);return {a:a};})()", true); fail("cycle"); }
		catch (org.codehaus.jettison.json.JSONException expected) { }
		var previous = JSONObject.getGlobalRecursionDepthLimit();
		try {
			JSONObject.setGlobalRecursionDepthLimit(3);
			try { copy("({a:{b:{c:{d:1}}}})", true); fail("depth"); } catch (org.codehaus.jettison.json.JSONException expected) { }
		} finally { JSONObject.setGlobalRecursionDepthLimit(previous); }
	}
}
