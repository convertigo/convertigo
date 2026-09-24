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

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class FlowVirtualPropertyPathTest {
	private FlowVirtualObject object() throws Exception {
		var object = new FlowVirtualObject() {
			@Override public boolean isDefinitionWritable() { return true; }
			@Override protected Object convertEditedValue(String name, String text) {
				// Isolate projection routing from the provider's separately tested codec.
				return name.equals("id") || name.equals("$$$id") ? Integer.valueOf(text) : text;
			}
		};
		object.setVirtualKind("node");
		object.setDefinition("{\"id\":\"node\",\"comment\":\"engine\",\"disabled\":false,"
				+ "\"props\":{\"id\":5,\"disabled\":true,\"comment\":\"business\",\"$$id\":6}}");
		var definitions = new JSONObject();
		for (var key : new String[] { "id", "disabled", "comment" }) {
			definitions.put(key, new JSONObject().put("definitionPath", "props." + key));
			definitions.put("$$" + key, new JSONObject().put("definitionPath", key));
		}
		definitions.put("$$$id", new JSONObject().put("definitionPath", "props.$$id"));
		object.setVirtualInfo(new JSONObject().put("propertyDefinitions", definitions).toString());
		return object;
	}

	@Test public void readsIndependentMetadataAndBusinessValues() throws Exception {
		var object = object();
		assertEquals(5, object.getDefinitionProperty("id"));
		assertEquals("node", object.getDefinitionProperty("$$id"));
		assertEquals(6, object.getDefinitionProperty("$$$id"));
		assertEquals(true, object.getDefinitionProperty("disabled"));
		assertEquals(false, object.getDefinitionProperty("$$disabled"));
		assertEquals("engine", object.getComment());
	}

	@Test public void rejectsUndeclaredAndReadOnlyPropertiesBeforeCallingTheProvider() throws Exception {
		var object = object();
		var before = object.getDefinition();
		assertFalse(object.setDynamicProperty("props", "{}"));
		assertFalse(object.setDynamicProperty("notDeclared", "value"));
		var info = object.getVirtualInfoObject();
		info.getJSONObject("propertyDefinitions").getJSONObject("$$id").put("readOnly", true);
		info.getJSONObject("propertyDefinitions").getJSONObject("$$disabled").put("hidden", true);
		object.setVirtualInfo(info.toString());
		assertThrows(com.twinsoft.convertigo.engine.EngineException.class, () -> object.setDynamicProperty("$$id", "changed"));
		assertThrows(com.twinsoft.convertigo.engine.EngineException.class, () -> object.setDynamicProperty("$$disabled", "true"));
		assertEquals(before, object.getDefinition());
	}

	@Test public void editsBusinessValuesWithoutFlatteningTheCache() throws Exception {
		var object = object();
		var before = object.getDefinitionObject();
		assertTrue(object.setDynamicProperty("#flow_property:id", "7"));
		assertTrue(object.setDynamicProperty("comment", "new business"));
		assertTrue(object.setDynamicProperty("$$$id", "8"));
		assertEquals("node", object.getDefinitionObject().getString("id"));
		assertEquals(7, object.getDefinitionObject().getJSONObject("props").getInt("id"));
		assertEquals(8, object.getDefinitionProperty("$$$id"));
		assertEquals("engine", object.getComment());
		assertEquals("new business", object.getDefinitionProperty("comment"));
		assertEquals(5, before.getJSONObject("props").getInt("id"));
	}

	@Test public void editsMetadataIndependentlyAndPreservesFalse() throws Exception {
		var object = object();
		object.setDefinitionProperty("$$disabled", true);
		object.setDefinitionProperty("disabled", false);
		object.setComment("new engine");
		assertEquals(true, object.getDefinitionObject().getBoolean("disabled"));
		assertEquals(false, object.getDefinitionObject().getJSONObject("props").getBoolean("disabled"));
		assertEquals("new engine", object.getComment());
		assertEquals("business", object.getDefinitionProperty("comment"));
	}

	@Test public void preservesLegacyNestedPropertyOnEdit() throws Exception {
		var object = new FlowVirtualObject();
		object.setDefinition("{\"id\":\"node\",\"props\":{\"value\":5}}");
		object.setDefinitionProperty("value", 6);
		assertFalse(object.getDefinitionObject().has("value"));
		assertEquals(6, object.getDefinitionProperty("value"));
	}

	@Test public void rejectsInvalidPathWithoutChangingCachedValue() throws Exception {
		var object = object();
		object.setVirtualInfo("{\"propertyDefinitions\":{\"id\":{\"definitionPath\":\"props..id\"}}}");
		var before = object.getDefinition();
		assertThrows(IllegalArgumentException.class, () -> object.setDefinitionProperty("id", 9));
		assertEquals(before, object.getDefinition());
	}

	@Test public void propertyEditsNeverInferDisplayFromBusinessKeys() throws Exception {
		var object = object();
		object.setSummary("Provider display, not node identity");
		object.setComment("new comment");
		assertEquals("Provider display, not node identity", object.getSummary());
		object.setDefinitionProperty("label", "Business label");
		assertEquals("Provider display, not node identity", object.getSummary());
	}
}
