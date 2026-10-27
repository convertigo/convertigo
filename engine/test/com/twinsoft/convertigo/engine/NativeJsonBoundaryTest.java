/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.engine.cache.FileCacheManager;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.flow.NativeJsonSupport;
import com.twinsoft.convertigo.engine.requesters.*;
import com.twinsoft.convertigo.engine.util.TimingTrace;
import com.twinsoft.convertigo.engine.util.JsonValues;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class NativeJsonBoundaryTest {
	private Engine previous;
	private Project project;
	private Probe flow;
	private Context context;
	private int tickets;
	private int checks;

	static class Probe extends Flow {
		long thread;
		boolean fail;
		@Override public void fireRequestableEvent(String event) { }
		@Override public void runCore() throws EngineException {
			thread = Thread.currentThread().getId();
			assertNull("Pooled runtime must not build the unused sequence scope", scope);
			if (fail) { MDC.put("test-marker", "child"); throw new EngineException("synthetic failure"); }
			try { context.nativeJsonResponse = JsonValues.object("rows", List.of(1)); }
			catch (Exception e) { throw new EngineException("test", e); }
		}
	}

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logBeans = Engine.logContext = Engine.logCacheManager = Engine.logDatabaseObjectManager = Logger.getLogger("native-test");
		EnginePropertiesManager.initProperties();
		previous = Engine.theApp; Engine.theApp = new Engine();
		Engine.theApp.billingManager = new BillingManager() { @Override public void insertBilling(Context context) { tickets++; } };
		Engine.theApp.cacheManager = new FileCacheManager();
		project = new Project() { @Override public ClassLoader getProjectClassLoader() { return NativeJsonBoundaryTest.class.getClassLoader(); } };
		project.setName("Test"); project.setJsonOutput(JsonOutput.useType); project.setJsonRoot(JsonRoot.docChildNodes);
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override public Project getOriginalProjectByName(String name, boolean check) { return project; }
		};
		flow = new Probe(); flow.setName("Echo");
		context = new Context("test_context") { @Override public void loadSequence() { requestedObject = flow; } };
		context.project = project; context.projectName = "Test"; context.sequenceName = "Echo"; context.name = "context";
		context.requestedObject = flow;
	}
	@After public void cleanup() { Engine.theApp = previous; MDC.clear(); }

	private JsonServletRequester requester(Map<String, Object> input) {
		var requester = new JsonServletRequester() {
			@Override public void checkAccessibility() { checks++; }
			@Override public void checkSecuredConnection() { checks++; }
			@Override public void checkAuthenticatedContext() { checks++; }
			@Override public void checkParentContext() { checks++; }
		};
		requester.context = context; requester.inputData = input;
		return requester;
	}

	@Test public void nativeExecutionKeepsFourChecksAndOneBillingTicketWithoutXml() throws Exception {
		var requester = requester(Map.of("plain", new String[] {"one", "two"}, "__nocache", "true"));
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE_ALLOCATIONS, "true");
		EnginePropertiesManager.setProperty(PropertyName.XML_COUNT_DOCUMENTS, "true");
		com.twinsoft.convertigo.engine.util.XmlUseCounter.begin();
		var span = TimingTrace.open("test", "native");
		try {
			assertNull(Engine.theApp.getDocument(requester, context));
			assertEquals(4, checks); assertEquals(1, tickets);
			assertNull(context.inputDocument); assertNull(context.outputDocument);
			assertEquals("two", flow.variables.get("plain")); assertFalse(flow.variables.containsKey("__nocache"));
			var writer = new java.io.StringWriter();
			((JsonResponse) requester.postGetNativeJson()).writeTo(writer);
			assertEquals(1, new JSONObject(writer.toString()).getJSONArray("rows").getInt(0));
			assertEquals("false", context.nativeJsonAttributes.get("fromcache"));
		} finally { span.close(); com.twinsoft.convertigo.engine.util.XmlUseCounter.end(); EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE_ALLOCATIONS, "false"); EnginePropertiesManager.setProperty(PropertyName.XML_COUNT_DOCUMENTS, "false"); }
		assertEquals(0, span.documents());
	}

	@Test public void anUntrustedServerStubStillRequiresAuthentication() throws Exception {
        assertFalse(Engine.isStudioMode());
        var sequence = new com.twinsoft.convertigo.beans.sequences.GenericSequence();
        sequence.setAuthenticatedContextRequired(true);
        context.requestedObject = sequence;
        context.isStubRequested = true;
        var requester = new JsonServletRequester();
        requester.context = context;
        assertThrows(EngineException.class, requester::checkAuthenticatedContext);
        context.isStubRequested = false;
        assertThrows(EngineException.class, requester::checkAuthenticatedContext);
    }

	@Test public void ordinaryMultivaluesAndBodyMatchTheXmlInputPath() throws Exception {
		var multi = new RequestableMultiValuedVariable(); multi.setName("values"); flow.addVariable(multi);
		var scalar = new RequestableVariable(); scalar.setName("scalar"); flow.addVariable(scalar);
		var input = new LinkedHashMap<String, Object>();
		input.put("values", new String[] {"a<&", "b"}); input.put("scalar", new String[] {"first", "last"});
		input.put("__body", "{\"fromBody\":42}"); input.put("__project", "Test");
		var requester = requester(input);
		assertTrue(NativeJsonSupport.prepare(requester, context)); flow.parseInputDocument(context);
		var actual = flow.getFlowInput();
		context.nativeInput = null; context.nativeJson = false;
		context.inputDocument = XMLUtils.getDefaultDocumentBuilder().newDocument();
		new com.twinsoft.convertigo.engine.translators.DefaultInternalTranslator().buildInputDocument(context, input);
		flow.parseInputDocument(context);
		assertEquals(flow.getFlowInput().toString(), actual.toString());
	}

	@Test public void unsupportedInputsOptionsAndConsumersFallBack() throws Exception {
		var requester = requester(Map.of("value", new String[0]));
		assertFalse(NativeJsonSupport.prepare(requester, context));
		for (var value : List.of("_empty_array_", XMLUtils.getDefaultDocumentBuilder().newDocument())) {
			requester.inputData = Map.of("value", value); assertFalse(NativeJsonSupport.prepare(requester, context));
		}
		requester.inputData = Map.of("__testcase", "case"); assertFalse(NativeJsonSupport.prepare(requester, context));
		requester.inputData = Map.of("value", "valid");
		project.setJsonRoot(JsonRoot.docNode); assertFalse(NativeJsonSupport.prepare(requester, context));
		project.setJsonRoot(JsonRoot.docChildNodes); project.setJsonOutput(JsonOutput.verbose);
		assertFalse(NativeJsonSupport.prepare(requester, context)); project.setJsonOutput(JsonOutput.useType);
		context.isAsync = true; assertFalse(NativeJsonSupport.prepare(requester, context)); context.isAsync = false;
		context.isStubRequested = true; assertFalse(NativeJsonSupport.prepare(requester, context)); context.isStubRequested = false;
		// a cached Flow answers a JSON request in native JSON, kept by the native JSON cache; the other requesters
		// keep the XML path and its cache
		flow.setResponseExpiryDate("absolute,9999999999999");
		requester.inputData = Map.of("value", "x"); assertTrue(NativeJsonSupport.prepare(requester, context));
		context.nativeInput = null; context.nativeJson = false;
		var mapper = new com.twinsoft.convertigo.engine.requesters.UrlMapperRequester(Map.of(), null,
				com.twinsoft.convertigo.engine.requesters.UrlMapperRequester.ResponseMode.json);
		mapper.context = context; mapper.inputData = Map.of("value", "x");
		assertFalse(NativeJsonSupport.prepare(mapper, context));
		flow.setResponseExpiryDate("");
		flow.setBillable(true); assertFalse(NativeJsonSupport.prepare(requester, context)); flow.setBillable(false);
		EnginePropertiesManager.setProperty(PropertyName.REQUESTABLE_NATIVE_JSON, "false"); assertFalse(NativeJsonSupport.prepare(requester, context));
	}

	@Test public void directResultShapesHaveExactLegacyJsonValues() throws Exception {
		for (var encoded : List.of("{\"value\":\"<>&é /\\n\",\"empty\":null,\"yes\":true,\"n\":1.5}",
				"{\"rows\":[{\"\":1,\"a b\":2,\"é\":3,\"__proto__\":4}],\"objects\":{},\"list\":[]}",
				"{\"nested\":[[],[1],[[2]]],\"large\":9007199254740991}")) {
			var value = new JSONObject(encoded); assertNotNull(NativeJsonSupport.projectResult(JsonValues.fromLegacy(value), "object"));
			var doc = XMLUtils.getDefaultDocumentBuilder().newDocument(); var root = doc.createElement("document"); doc.appendChild(root);
			XMLUtils.jsonToXml(value, null, root, true, true, false, "object");
			var legacy = new JSONObject(XMLUtils.XmlToJson(root, true, true, JsonRoot.docChildNodes));
			assertEquals(value.toString(), legacy.toString());
		}
		for (var value : List.of(new JSONArray("[1]"), new JSONArray("[1,2]"),
				new JSONArray("[[1],[],{a:1}]"), "scalar", true, 12, new JSONObject("{attr:1}"), new JSONObject("{attr:1,text:'content'}"))) {
			var doc = XMLUtils.getDefaultDocumentBuilder().newDocument(); var root = doc.createElement("document"); doc.appendChild(root);
			root.setAttribute("project", "Test");
			XMLUtils.jsonToXml(JSONObject.NULL.equals(value) ? new JSONObject() : value, null, root, true, true, false, "object");
			var legacy = new JSONObject(XMLUtils.XmlToJson(root, true, true, JsonRoot.docChildNodes));
			assertEquals(value.toString(), legacy.toString(), JsonValues.toLegacy(NativeJsonSupport.projectResult(JsonValues.fromLegacy(value), "object")).toString());
		}
		assertNull(NativeJsonSupport.projectResult(JsonValues.parse("{\"nested\":{\"_attachments\":{}}}"), "object"));
	}

	@Test public void emptyNativeResultsHaveNoSyntheticTextButExplicitTextAndXmlStayIntact() throws Exception {
		for (var value : new Object[] {new JSONObject(), new JSONArray(), JSONObject.NULL, null}) {
			context.nativeJsonResponse = NativeJsonSupport.projectResult(JsonValues.fromLegacy(value), "object");
			assertEquals("{}", JsonValues.toLegacy(context.nativeJsonResponse).toString());
			var json = requester(Map.of());
			var jsonp = new JsonPServletRequester(); jsonp.context = context; context.set("jsonpCallback", "receive");
			var rest = new UrlMapperRequester(Map.of("__project", "Test"), null, UrlMapperRequester.ResponseMode.json);
			rest.context = context;
			for (var boundary : List.of(json, jsonp, rest)) {
				var writer = new java.io.StringWriter();
				((JsonResponse) boundary.postGetNativeJson()).writeTo(writer);
				assertEquals(boundary == jsonp ? "receive({});" : "{}", writer.toString());
			}
		}
		var explicit = JsonValues.map(JsonValues.fromLegacy(new JSONObject("{text:'',nested:{},items:[]}")));
		assertSame(explicit, NativeJsonSupport.projectResult(explicit, "object"));
		var doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = doc.createElement("document"); doc.appendChild(root); root.setAttribute("project", "Test");
		XMLUtils.jsonToXml(new JSONObject(), null, root, true, true, false, "object");
		assertEquals("{\"text\":\"\"}", new JSONObject(XMLUtils.XmlToJson(root, true, true, JsonRoot.docChildNodes)).toString());
	}

	@Test public void inlineIsOptInAndRetainsThreadStateAndExceptionBehavior() throws Exception {
		var requester = new NativeJsonRequester(Map.of("__project", "Test"), null); requester.context = context;
		context.nativeJson = true; context.nativeInput = Map.of();
		flow.setResponseTimeout(0);
		EnginePropertiesManager.setProperty(PropertyName.NATIVE_INLINE_CALLS, "true");
		var thread = Thread.currentThread(); var loader = thread.getContextClassLoader();
		MDC.put("test-marker", "parent");
		flow.run(requester, context); assertEquals(thread.getId(), flow.thread);
		flow.fail = true;
		try { flow.run(requester, context); fail("Expected failure"); }
		catch (EngineException expected) { assertEquals("synthetic failure", expected.getMessage()); }
		assertSame(loader, thread.getContextClassLoader()); assertEquals("parent", MDC.get("test-marker"));
		assertNull(flow.runningThread);
		flow.fail = false; flow.setResponseTimeout(60);
		flow.run(requester, context); assertNotEquals(thread.getId(), flow.thread);
	}
	@Test public void nestedInlineKeepsAnEarlierCallerDeadlineAndCancellation() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.NATIVE_INLINE_CALLS, "true");
		var child = new Probe(); child.setName("Child"); child.setResponseTimeout(60);
		var childContext = new Context("child"); childContext.project = project;
		childContext.nativeJson = true; childContext.nativeInput = Map.of();
		var childRequester = new NativeJsonRequester(Map.of(), null); childRequester.context = childContext;
		var parent = new Probe() {
			@Override public void runCore() throws EngineException {
				long caller = Thread.currentThread().getId();
				var loader = Thread.currentThread().getContextClassLoader();
				MDC.put("test-marker", "parent");
				child.run(childRequester, childContext);
				assertEquals(caller, child.thread);
				child.fail = true;
				try { child.run(childRequester, childContext); fail("Expected failure"); }
				catch (EngineException expected) { assertEquals("synthetic failure", expected.getMessage()); }
				assertSame(loader, Thread.currentThread().getContextClassLoader());
				assertEquals("parent", MDC.get("test-marker"));
				child.fail = false; child.setResponseTimeout(1);
				child.run(childRequester, childContext);
				assertNotEquals("shorter child timeout keeps its waiter", caller, child.thread);
				var owner = this;
				var cancelled = new Probe() {
					@Override public void runCore() {
						assertTrue(isRunning());
						owner.runningThread.bContinue = false;
						assertFalse("caller timeout cancels inline child", isRunning());
					}
				};
				cancelled.setName("Cancelled"); cancelled.setResponseTimeout(60);
				try { cancelled.run(childRequester, childContext); } finally { owner.runningThread.bContinue = true; }
			}
		};
		parent.setName("Parent"); parent.setResponseTimeout(60);
		context.requestedObject = parent;
		parent.run(requester(Map.of()), context);
		assertNull(org.mozilla.javascript.Context.getCurrentContext());
		// Outside a running parent, an independent timeout still requires a worker.
		child.setResponseTimeout(60); child.run(childRequester, childContext);
		assertNotEquals(Thread.currentThread().getId(), child.thread);
	}

	@Test public void callerTimeoutStillStopsAnInlineChildCooperatively() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.NATIVE_INLINE_CALLS, "true");
		EnginePropertiesManager.setProperty(PropertyName.DOCUMENT_THREADING_USE_STOP_METHOD, "false");
		Engine.theApp.pluginsManager = new PluginsManager();
		var finished = new java.util.concurrent.CountDownLatch(1);
		var child = new Probe() {
			@Override public void runCore() throws EngineException {
				long limit = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
				try {
					while (isRunning() && System.nanoTime() < limit) Thread.sleep(5);
					assertFalse("parent deadline must propagate", isRunning());
				} catch (InterruptedException e) { throw new EngineException("interrupted", e); }
			}
		};
		child.setName("SlowChild"); child.setResponseTimeout(60);
		var childContext = new Context("child"); childContext.project = project;
		childContext.nativeJson = true; childContext.nativeInput = Map.of();
		var childRequester = new NativeJsonRequester(Map.of(), null); childRequester.context = childContext;
		var parent = new Probe() {
			@Override public void runCore() throws EngineException {
				try { child.run(childRequester, childContext); } finally { finished.countDown(); }
			}
		};
		parent.setName("TimedParent"); parent.setResponseTimeout(1);
		assertThrows(com.twinsoft.convertigo.beans.core.TransactionTimeoutException.class,
				() -> parent.run(requester(Map.of()), context));
		assertTrue("worker exits after the waiter times out", finished.await(3, java.util.concurrent.TimeUnit.SECONDS));
	}

	@Test(timeout = 5000) public void explicitCallerAbortCancelsItsInlineChildAndWakesTheWaiter() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.NATIVE_INLINE_CALLS, "true");
		var parent = new Probe() {
			@Override public void runCore() throws EngineException {
				var owner = this;
				var child = new Probe() {
					@Override public void runCore() {
						assertTrue(isRunning()); owner.abort();
						assertFalse(isRunning());
					}
				};
				child.setName("Child"); child.setResponseTimeout(60);
				var childContext = new Context("child"); childContext.project = project;
				childContext.nativeJson = true; childContext.nativeInput = Map.of();
				var childRequester = new NativeJsonRequester(Map.of(), null); childRequester.context = childContext;
				child.run(childRequester, childContext);
			}
		};
		parent.setName("Parent"); parent.setResponseTimeout(60);
		parent.run(requester(Map.of()), context);
	}

	@Test public void inlineNestedExecutionStillChecksAndBillsBothRequestables() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.NATIVE_INLINE_CALLS, "true");
		var child = new Probe(); child.setName("Child"); child.setResponseTimeout(60);
		var childContext = new Context("child") { @Override public void loadSequence() { requestedObject = child; } };
		childContext.project = project; childContext.projectName = "Test";
		childContext.sequenceName = "Child"; childContext.name = "child";
		var childRequester = new NativeJsonRequester(Map.of(), null) {
			@Override public void checkAccessibility() { checks++; }
			@Override public void checkSecuredConnection() { checks++; }
			@Override public void checkAuthenticatedContext() { checks++; }
			@Override public void checkParentContext() { checks++; }
		};
		childRequester.context = childContext;
		flow = new Probe() {
			@Override public void runCore() throws EngineException {
				Engine.theApp.getDocument(childRequester, childContext);
				assertEquals(Thread.currentThread().getId(), child.thread);
				super.runCore();
			}
		};
		flow.setName("Parent"); flow.setResponseTimeout(60);
		Engine.theApp.getDocument(requester(Map.of()), context);
		assertEquals(8, checks); assertEquals(2, tickets);
		assertNull(childContext.inputDocument); assertNull(childContext.outputDocument);
	}

	@Test public void jsonpAndRestUseTheSameStreamedValue() throws Exception {
		context.nativeJsonResponse = JsonValues.map(JsonValues.fromLegacy(new JSONObject("{text:'é<&',items:[true,null,1]}")));
		var jsonp = new JsonPServletRequester(); jsonp.context = context; context.set("jsonpCallback", "receive");
		var writer = new java.io.StringWriter(); ((JsonResponse) jsonp.postGetNativeJson()).writeTo(writer);
		assertTrue(writer.toString().startsWith("receive(")); assertTrue(writer.toString().endsWith(");"));
		assertEquals(JsonValues.toLegacy(context.nativeJsonResponse).toString(), new JSONObject(writer.toString().substring(8, writer.toString().length()-2)).toString());
		var rest = new UrlMapperRequester(Map.of("__project", "Test"), null, UrlMapperRequester.ResponseMode.json);
		rest.context = context; assertTrue(rest.acceptNativeJson());
		var expected = JsonValues.toLegacy(context.nativeJsonResponse).toString();
		var captured = (JsonResponse) rest.postGetNativeJson();
		// A later request may reset the shared context before REST writes this response.
		context.reset();
		var restWriter = new java.io.StringWriter(); captured.writeTo(restWriter);
		assertEquals(expected, restWriter.toString());
		assertNull(rest.getResponseDocument());
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		document.appendChild(document.createElement("document"));
		rest.postGetDocument(document);
		context.outputDocument = XMLUtils.getDefaultDocumentBuilder().newDocument();
		assertSame(document, rest.getResponseDocument());
		var xml = new UrlMapperRequester(Map.of("__project", "Test"), null, UrlMapperRequester.ResponseMode.defaultContent);
		assertFalse(xml.acceptNativeJson());
		context.reset(); assertNull(context.nativeJsonResponse); assertNull(context.nativeInput); assertFalse(context.nativeJson);
	}

	@Test public void internalJsonObjectsBelongToTheCallerAndPreserveOwnKeys() throws Exception {
		var value = new JSONObject("{items:[null,true,1.5,{text:'é<&',__proto__:{safe:true}}],empty:{},list:[]}");
		var requester = new NativeJsonRequester(Map.of("__project", "Test"), null) {
			@Override public Object processRequest() { return value; }
		};
		try (var javascript = org.mozilla.javascript.Context.enter()) {
			var first = javascript.initStandardObjects();
			var second = javascript.initStandardObjects();
			var object = (org.mozilla.javascript.Scriptable) requester.processRequestAsObject(first);
			assertSame(org.mozilla.javascript.ScriptableObject.getObjectPrototype(first), object.getPrototype());
			org.mozilla.javascript.ScriptableObject.putProperty(first, "result", object);
			assertEquals(Boolean.TRUE, javascript.evaluateString(first,
					"Array.isArray(result.items) && result.items[0] === null && result.items[1] === true && "
					+ "result.items[2] === 1.5 && result.items[3].text === 'é<&' && "
					+ "Object.prototype.hasOwnProperty.call(result.items[3], '__proto__') && "
					+ "result.items[3].__proto__.safe === true && Object.getPrototypeOf(result.items[3]) === Object.prototype && "
					+ "Object.keys(result.empty).length === 0 && Array.isArray(result.list)", "native-json-test", 1, null));
			javascript.evaluateString(first, "result.items[3].text = 'changed'", "native-json-test", 1, null);
			assertEquals("é<&", value.getJSONArray("items").getJSONObject(3).getString("text"));
			var other = (org.mozilla.javascript.Scriptable) requester.processRequestAsObject(second);
			assertSame(org.mozilla.javascript.ScriptableObject.getObjectPrototype(second), other.getPrototype());
			assertNotSame(object.getPrototype(), other.getPrototype());
		}
	}

	@Test public void authorizationFailureNeverEntersTheNativeRuntime() throws Exception {
		var denied = new JsonServletRequester() {
			@Override public void checkAccessibility() throws EngineException { throw new EngineException("denied"); }
		};
		denied.context = context; denied.inputData = Map.of();
		try { Engine.theApp.getDocument(denied, context); fail("Expected denial"); }
		catch (EngineException expected) { assertEquals("denied", expected.getMessage()); }
		assertEquals(0, flow.thread); assertNull(context.nativeJsonResponse); assertFalse(context.nativeJson);
	}

	@Test public void restSelectionPreservesSchemaAndXpathConsumers() throws Exception {
		var operation = new com.twinsoft.convertigo.beans.rest.GetOperation() {
			@Override public Project getProject() { return project; }
		};
		var select = com.twinsoft.convertigo.beans.rest.AbstractRestOperation.class.getDeclaredMethod(
				"createUrlMapperRequester", Map.class, jakarta.servlet.http.HttpServletRequest.class,
				com.twinsoft.convertigo.beans.rest.AbstractRestOperation.OutputContent.class);
		select.setAccessible(true);
		String[] servletPath = {"/api"};
		var request = (jakarta.servlet.http.HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[]{jakarta.servlet.http.HttpServletRequest.class},
				(proxy, method, args) -> "getServletPath".equals(method.getName()) ? servletPath[0] : null);
		var output = com.twinsoft.convertigo.beans.rest.AbstractRestOperation.OutputContent.toJson;
		var input = Map.of("__project", "Test");
		assertTrue(((UrlMapperRequester) select.invoke(operation,input,request,output)).acceptNativeJson());
		servletPath[0] = "/openapi";
		assertFalse(((UrlMapperRequester) select.invoke(operation,input,request,output)).acceptNativeJson());
		servletPath[0] = "/api";
		project.setJsonRoot(JsonRoot.docNode);
		assertFalse(((UrlMapperRequester) select.invoke(operation,input,request,output)).acceptNativeJson());
		project.setJsonRoot(JsonRoot.docChildNodes);
		var response = new com.twinsoft.convertigo.beans.rest.OperationResponse();
		response.setXpath("//error"); operation.add(response);
		assertFalse(((UrlMapperRequester) select.invoke(operation,input,request,output)).acceptNativeJson());
	}

}
