/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.List;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.Before;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.requesters.JsonResponse;
import com.twinsoft.convertigo.engine.util.DetachedJson;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class DetachedResultBoundaryTest {
	@Before public void initializeLogs() {
		Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("detached-test");
	}
	@Test public void directCopyMatchesTextBoundaryForJsonSemanticsAndJavaScalars() throws Exception {
		try (var cx = Context.enter()) {
			cx.setLanguageVersion(Context.VERSION_ES6);
			var scope = cx.initStandardObjects();
			for (var expression : List.of(
					"({result:{nil:null,no:false,empty:'',text:'é☃<>&\\n',missing:undefined,fn:function(){},nan:NaN,inf:Infinity,zero:-0}})",
					"({result:[,undefined,function(){},null,[[],[1]],{x:1}],empty:{},array:[]})",
					"JSON.parse('{\"__proto__\":{\"safe\":true},\"constructor\":\"own\",\"2\":2,\"1\":1}')",
					"({numbers:[1.5,9007199254740991,1000000000000000100,1e21,1e23,1e-7,Number.MIN_VALUE]})",
					"({result:{toJSON:function(key){return {key:key,wrapped:new Number(2),bool:new Boolean(false),str:new String('yes')};}}})",
					"({result:{toJSON:function(){return new java.util.ArrayList(java.util.Arrays.asList('x',new java.lang.Integer(2)));}}})",
					"({result:{toJSON:function(){var m=new java.util.LinkedHashMap();m.put('__proto__','safe');m.put('nil',null);return m;}}})",
					"({result:[Symbol('x'),undefined],omitted:Symbol('y')})")) {
				var value = cx.evaluateString(scope, expression, "result-test", 1, null);
				ScriptableObject.putProperty(scope, "value", value);
				var expected = new JSONObject(Context.toString(cx.evaluateString(scope, "JSON.stringify(value)", "text-test", 1, null)));
				var detached = DetachedJson.object(value, cx, scope);
				assertEquals(expression, expected.toString(), detached.toString());
				assertDetached(detached);
			}
		}
	}

	private static void assertDetached(Object value) throws Exception {
		if (value instanceof JSONObject object) {
			for (var keys = object.keys(); keys.hasNext();) assertDetached(object.get((String) keys.next()));
		} else if (value instanceof org.codehaus.jettison.json.JSONArray array) {
			for (int i = 0; i < array.length(); i++) assertDetached(array.get(i));
		} else assertTrue(value == JSONObject.EXPLICIT_NULL || value instanceof String || value instanceof Boolean
				|| value instanceof Long || value instanceof Double);
	}

	@Test public void gettersRunBeforeReleaseAndAliasesAreIndependent() throws Exception {
		JSONObject detached;
		try (var cx = Context.enter()) {
			var scope = cx.initStandardObjects();
			var value = cx.evaluateString(scope, "var reads=0;var shared={n:1};var value={a:shared,b:shared};"
					+ "Object.defineProperty(value,'computed',{enumerable:true,get:function(){reads++;return {n:reads};}});value", "test", 1, null);
			detached = DetachedJson.object(value, cx, scope);
			assertEquals(1d, Context.toNumber(cx.evaluateString(scope, "reads", "test", 1, null)), 0d);
			cx.evaluateString(scope, "shared.n=42", "test", 1, null);
			assertEquals(1, detached.getJSONObject("a").getInt("n"));
			detached.getJSONObject("a").put("n", 9);
			assertEquals(1, detached.getJSONObject("b").getInt("n"));
		}
		var writer = new StringWriter(); new JsonResponse(detached, "receive").writeTo(writer);
		assertTrue(writer.toString().startsWith("receive(")); assertTrue(writer.toString().endsWith(");"));
		assertEquals(1, detached.getJSONObject("computed").getInt("n"));
	}

	@Test public void detachedObjectsStillSupportLazyXmlAndArrayItemProjection() throws Exception {
		try (var cx = Context.enter()) {
			var scope = cx.initStandardObjects();
			var value = cx.evaluateString(scope, "({result:[{text:'é<&',items:[1,null,true]},{}],xml:{arrayItem:'object'}})", "test", 1, null);
			ScriptableObject.putProperty(scope, "value", value);
			var old = new JSONObject(Context.toString(cx.evaluateString(scope, "JSON.stringify(value)", "test", 1, null)));
			var detached = DetachedJson.object(value, cx, scope);
			assertEquals(xml(old), xml(detached));
			assertEquals(NativeJsonSupport.projectResult(old.get("result"), "object").toString(),
					NativeJsonSupport.projectResult(detached.get("result"), "object").toString());
		}
	}
	private static String xml(JSONObject envelope) throws Exception {
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = document.createElement("document"); document.appendChild(root);
		XMLUtils.jsonToXml(envelope.get("result"), null, root, true, true, false, envelope.getJSONObject("xml").getString("arrayItem"));
		assertEquals(2, root.getChildNodes().getLength());
		assertEquals("object", root.getFirstChild().getNodeName());
		return XMLUtils.XmlToJson(root, true, true, JsonRoot.docChildNodes);
	}

	@Test public void writerFailureAndClientDisconnectCloseTheServletWriter() throws Exception {
		var answer = new JsonResponse(new JSONObject("{items:[1,2,3]}"), null);
		boolean[] closed = {false};
		var broken = new Writer() {
			@Override public void write(char[] chars, int offset, int length) throws IOException { throw new IOException("synthetic disconnect"); }
			@Override public void flush() { }
			@Override public void close() { closed[0] = true; }
		};
		try { answer.writeTo(broken); fail("writer error must propagate"); } catch (IOException expected) { }
		try { answer.writeToResponse(new PrintWriter(broken)); fail("PrintWriter error must propagate"); } catch (IOException expected) { }
		assertTrue(closed[0]);
		closed[0] = false;
		var failingValue = new JSONObject() {
			@Override public Writer write(Writer writer) throws org.codehaus.jettison.json.JSONException {
				throw new org.codehaus.jettison.json.JSONException("synthetic serialization failure");
			}
		};
		try { new JsonResponse(failingValue, null).writeToResponse(new PrintWriter(broken)); fail("serialization error must propagate"); }
		catch (IOException expected) { assertTrue(expected.getCause() instanceof org.codehaus.jettison.json.JSONException); }
		assertTrue(closed[0]);
	}
	@Test public void directWriterMatchesEstablishedEscapingAndContainerOptions() throws Exception {
		var text = new StringBuilder();
		for (int c = 0; c <= Character.MAX_VALUE; c++) text.append((char) c);
		var object = new DetachedJson.ObjectValue();
		var array = new DetachedJson.ArrayValue();
		object.put("key/\"\\\u2028", array);
		array.put(text.toString()).put(JSONObject.EXPLICIT_NULL).put(false).put(1.5).put(1000000000000000100L);
		for (boolean slash : new boolean[] {true, false}) {
			object.setEscapeForwardSlashAlways(slash); array.setEscapeForwardSlashAlways(slash);
			var legacy = new StringWriter(); object.write(legacy);
			var direct = new StringWriter(); new JsonResponse(object, null).writeTo(direct);
			assertEquals(legacy.toString(), direct.toString());
		}
	}

	@Test public void bridgeReturnsScopesOnSuccessDetachmentFailureAndEngineFailure() throws Exception {
		Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("detached-test");
		EnginePropertiesManager.initProperties();
		var previous = Engine.theApp;
		var directory = Files.createTempDirectory("detached-result-test-");
		var engineFile = directory.resolve("_flow/Engine.js"); Files.createDirectories(engineFile.getParent());
		Files.writeString(engineFile, "(function(){var n=0,shared={};return {"
				+ "run:function(){throw new Error('text execution must not be selected');},"
				+ "runObject:function(text,detach){n++;shared.n=n;var r=JSON.parse(text);"
				+ "if(r.fail==='engine')throw new Error('synthetic engine error');"
				+ "if(r.fail==='copy')return detach({toJSON:function(){throw new Error('synthetic copy error');}});"
				+ "return detach({ok:true,result:shared});},"
				+ "analyze:function(){return '{\"ok\":true}';}};})()");
		try {
			Engine.theApp = new Engine();
			var project = new Project() { @Override public String getDirPath() { return directory.toString(); } };
			project.setName("DetachedTest");
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override public Project getOriginalProjectByName(String name, boolean check) { return project; }
				@Override public Project getLoadedProjectByName(String name) { return null; }
			};
			FlowEngineBridge.clearCaches(); var bridge = new FlowEngineBridge();
			var first = bridge.invoke("DetachedTest.Engine", "run", new JSONObject(), null, null, null);
			assertNull(Context.getCurrentContext());
			for (var failure : List.of("copy", "engine")) {
				try { bridge.invoke("DetachedTest.Engine", "run", new JSONObject().put("fail", failure), null, null, null); fail("expected failure"); }
				catch (EngineException expected) { }
				assertNull(Context.getCurrentContext());
			}
			var fourth = bridge.invoke("DetachedTest.Engine", "run", new JSONObject(), null, null, null);
			assertEquals("same runtime is reusable after both failures", 4, fourth.getJSONObject("result").getInt("n"));
			assertEquals("released scope cannot mutate old response", 1, first.getJSONObject("result").getInt("n"));
			assertTrue(bridge.invoke("DetachedTest.Engine", "analyze", new JSONObject(), null, null, null).getBoolean("ok"));
			var writer = new StringWriter(); new JsonResponse(first, null).writeTo(writer);
			assertEquals(1, new JSONObject(writer.toString()).getJSONObject("result").getInt("n"));
			Files.writeString(engineFile, "({run:function(){return '{\"ok\":true,\"legacy\":true}';}})");
			FlowEngineBridge.clearCaches();
			assertTrue("providers without the object protocol still work",
					bridge.invoke("DetachedTest.Engine", "run", new JSONObject(), null, null, null).getBoolean("legacy"));
		} finally {
			FlowEngineBridge.clearCaches(); Engine.theApp = previous;
			try (var paths = Files.walk(directory)) { for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path); }
		}
	}

	@Test public void unsupportedBigIntsCyclesAndDeepResultsFailBeforeWriting() throws Exception {
		try (var cx = Context.enter()) {
			cx.setLanguageVersion(Context.VERSION_ES6);
			var scope = cx.initStandardObjects();
			for (var expression : List.of("({value:BigInt(7)})", "(function(){var a={};a.self=a;return a;})()",
					"(function(){var a=[];a.push(a);return {a:a};})()")) {
				try { DetachedJson.object(cx.evaluateString(scope, expression, "test", 1, null), cx, scope); fail("expected invalid JSON"); }
				catch (org.codehaus.jettison.json.JSONException expected) { }
			}
			var limit = JSONObject.getGlobalRecursionDepthLimit();
			try {
				JSONObject.setGlobalRecursionDepthLimit(2);
				try { DetachedJson.object(cx.evaluateString(scope, "({a:{b:{c:1}}})", "test", 1, null), cx, scope); fail("expected depth limit"); }
				catch (org.codehaus.jettison.json.JSONException expected) { }
			} finally { JSONObject.setGlobalRecursionDepthLimit(limit); }
		}
	}
}
