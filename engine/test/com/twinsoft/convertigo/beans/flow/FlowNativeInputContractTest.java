/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class FlowNativeInputContractTest {
	private static Engine previous;

	// a text default is compiled for its global symbols, as setDefaultIfChanged does in the engine
	@BeforeClass
	public static void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("flow-native-input-contract-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			{
				symbolsProperties = new Properties();
			}
		};
	}

	@AfterClass
	public static void restore() {
		Engine.theApp = previous;
	}

	private void sync(Flow flow, JSONObject definitions) throws Throwable {
		var method = Flow.class.getDeclaredMethod("syncFlowInputDefinitions", JSONObject.class);
		method.setAccessible(true);
		try { method.invoke(flow, definitions); }
		catch (InvocationTargetException e) { throw e.getCause(); }
	}

	@Test public void rejectsLateInvalidDefinitionBeforeRemovingOrUpdatingVariables() throws Throwable {
		for (var property : List.of("required", "multi", "multiValued", "isFileUpload", "nullable")) {
			for (Object value : List.of("abc", " 60 ", "Bogus", "private", "yes", " TRUE ", 60, JSONObject.NULL)) {
				var flow = new Flow();
				var existing = new RequestableVariable(); existing.setName("existing"); existing.setDescription("keep"); flow.addVariable(existing);
				var good = new JSONObject().put("type", "integer").put("required", true);
				var bad = new JSONObject().put("type", "string").put(property, value);
				try { sync(flow, new JSONObject().put("first", good).put("last", bad)); fail(property + "=" + value); }
				catch (EngineException e) {
					assertTrue(e.getMessage(), e.getMessage().contains("_flow.inputs.last." + property));
					assertTrue(e.getMessage(), e.getMessage().contains("takes true or false"));
				}
				assertSame(existing, flow.getVariable("existing"));
				assertEquals("keep", existing.getDescription());
				assertNull(flow.getVariable("first"));
			}
		}
	}

	@Test public void rejectsUnknownTypesVisibilityAndObjectDescriptions() throws Exception {
		for (Object value : List.of("abc", " 60 ", "Bogus", "private", "yes", 16, -2, 1.5, true, JSONObject.NULL)) {
			var bad = new JSONObject().put("visibility", value);
			try { Flow.validateFlowInputDefinitions(new JSONObject().put("records", bad)); fail(String.valueOf(value)); }
			catch (EngineException e) { assertTrue(e.getMessage().contains("_flow.inputs.records.visibility")); }
		}
		for (var name : List.of("$records", "cost$value", "__reserved", "bad-name")) {
			try { Flow.validateFlowInputDefinitions(new JSONObject().put(name, new JSONObject().put("type", "string"))); fail(name); }
			catch (EngineException e) { assertTrue(e.getMessage().contains("_flow.inputs." + name)); }
		}
		for (var bad : List.of(new JSONObject().put("type", "Bogus"), new JSONObject().put("description", new JSONObject()),
				new JSONObject().put("type", new JSONArray()), new JSONObject().put("type", "integer"))) {
			if ("integer".equals(bad.opt("type"))) { Flow.validateFlowInputDefinitions(new JSONObject().put("records", bad)); continue; }
			try { Flow.validateFlowInputDefinitions(new JSONObject().put("records", bad)); fail(bad.toString()); }
			catch (EngineException e) { assertTrue(e.getMessage().startsWith("FLOW_NATIVE_PROPERTY_INVALID:")); }
		}
	}

	@Test public void preflightsWholeResponseIncludingAccessibilityAndAuthentication() throws Exception {
		for (var response : List.of(new JSONObject().put("accessibility", "Bogus"), new JSONObject().put("accessibility", "private"),
				new JSONObject().put("flowMetadata", new JSONObject().put("authenticatedContextRequired", "yes")),
				new JSONObject().put("accessibility", "Private").put("inputDefinitions", new JSONObject()
						.put("first", new JSONObject().put("required", true)).put("last", new JSONObject().put("required", "yes"))),
				new JSONObject().put("inputDefinitions", new JSONArray()))) {
			try { Flow.validateFlowInputResponseMetadata(response); fail(response.toString()); }
			catch (EngineException e) { assertTrue(e.getMessage().contains("takes")); }
		}
		Flow.validateFlowInputResponseMetadata(new JSONObject().put("accessibility", "Private")
				.put("flowMetadata", new JSONObject().put("authenticatedContextRequired", true)));
	}

	@Test public void replaysCanonicalFlagsAndKeepsJsonDefaultsWithoutCoercion() throws Throwable {
		var flow = new Flow();
		var definitions = new JSONObject().put("records", new JSONObject().put("type", "integer").put("required", false).put("visibility", -1).put("description", ""))
				.put("upload", new JSONObject().put("type", "array").put("isFileUpload", true).put("default", new JSONArray().put(new JSONObject().put("id", 1))))
				.put("object", new JSONObject().put("type", "object").put("default", new JSONObject().put("name", "l'équipe")));
		sync(flow, definitions);
		assertFalse(flow.getVariable("records").isRequired()); assertEquals(-1, flow.getVariable("records").getVisibility());
		assertEquals("", flow.getVariable("records").getDescription());
		assertTrue(flow.getVariable("upload").isMultiValued()); assertTrue(flow.getVariable("upload").getIsFileUpload());
		assertEquals("{\"name\":\"l'équipe\"}", flow.getVariable("object").getValueOrNull());
		Flow.validateFlowInputDefinitions(new JSONObject().put("symbol", new JSONObject().put("type", "integer").put("default", "${count=60}")));
		definitions.getJSONObject("records").remove("visibility");
		definitions.getJSONObject("upload").remove("isFileUpload");
		sync(flow, definitions);
		assertEquals(-1, flow.getVariable("records").getVisibility()); assertTrue(flow.getVariable("upload").getIsFileUpload());
		definitions.getJSONObject("records").put("visibility", 0).put("required", true);
		definitions.getJSONObject("upload").put("isFileUpload", false);
		sync(flow, definitions);
		assertEquals(0, flow.getVariable("records").getVisibility()); assertTrue(flow.getVariable("records").isRequired()); assertFalse(flow.getVariable("upload").getIsFileUpload());
	}

	// JSON null is literal data: in an object, in an array or as the whole default, as the JS contract answers it
	@Test public void acceptsNullInsideJsonDefaults() throws Throwable {
		var definitions = new JSONObject("{\"filter\":{\"type\":\"object\",\"default\":{\"field\":null,\"values\":[null,1]}},"
				+ "\"from\":{\"type\":\"string\",\"default\":null},\"ids\":{\"type\":\"array\",\"default\":[null]}}");
		Flow.validateFlowInputDefinitions(definitions);
		var flow = new Flow();
		sync(flow, definitions);
		assertEquals("{\"field\":null,\"values\":[null,1]}", flow.getVariable("filter").getValueOrNull());
		assertNull(flow.getVariable("from").getValueOrNull());
		assertNull(Flow.flowInputDefaultValue(definitions.getJSONObject("from").get("default"), false));
		assertEquals(java.util.List.of(), Flow.flowInputDefaultValue(definitions.getJSONObject("ids").get("default"), true));
		var detached = new JSONObject().put("filter", new JSONObject().put("type", "object")
				.put("default", new JSONObject().put("field", JSONObject.NULL).put("values", new JSONArray().put(JSONObject.NULL))));
		Flow.validateFlowInputDefinitions(detached);
	}

	@Test public void rejectsNonJsonDefaultsBeforePersistence() throws Exception {
		var definitions = new JSONObject().put("options", new JSONObject().put("type", "object").put("default", new java.util.Date()));
		try { Flow.validateFlowInputDefinitions(definitions); fail("A Java object must not become a serialized native variable default"); }
		catch (EngineException e) { assertTrue(e.getMessage().contains("_flow.inputs.options.default")); }
	}
}
