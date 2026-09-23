/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class GetFlowVirtualPropertiesTest {
	@Test
	public void providerInformationOverridesHostInformationWithoutHidingBusinessProperties() throws Exception {
		var project = new com.twinsoft.convertigo.beans.core.Project();
		project.setName("Test");
		var object = new FlowVirtualObject();
		object.setParent(project);
		object.setName("technicalWrapper");
		object.setDefinition("{\"id\":\"equi\",\"props\":{\"name\":\"Business name\"}}");
		object.setVirtualInfo("{\"propertyDefinitions\":{"
				+ "\"$$id\":{\"label\":\"Name\",\"category\":\"Information\",\"definitionPath\":\"id\",\"readOnly\":true},"
				+ "\"name\":{\"label\":\"Business name\",\"category\":\"Base properties\",\"definitionPath\":\"props.name\"},"
				+ "\"hidden\":{\"label\":\"Priority\",\"category\":\"Information\",\"hidden\":true}}}");
		assertEquals(true, object.hasProjectedInformationProperty("Name"));
		assertEquals(false, object.hasProjectedInformationProperty("Business name"));
		assertEquals(false, object.hasProjectedInformationProperty("Priority"));
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = object.toXml(document, ExportOption.bIncludeDisplayName);
		var props = new JSONObject();
		var service = new Get();
		service.addDboProperties(object, props, findProperty(root, "$$id"));
		service.addDboProperties(object, props, findProperty(root, "name"));
		service.addInfosProperties(object, props);
		assertEquals("equi", props.getJSONObject("Name").getString("value"));
		assertEquals("Business name", props.getJSONObject("Business name").getString("value"));
		assertEquals(true, props.has("Priority"));
	}

	@Test
	public void exposesBusinessIdAndEngineIdSeparately() throws Exception {
		var object = new FlowVirtualObject();
		object.setName("item");
		object.setDefinition("{\"id\":\"node\",\"props\":{\"id\":5}}");
		object.setVirtualInfo("{\"propertyDefinitions\":{"
				+ "\"id\":{\"label\":\"Business ID\",\"definitionPath\":\"props.id\",\"type\":\"number\"},"
				+ "\"$$id\":{\"label\":\"Node ID\",\"definitionPath\":\"id\",\"readOnly\":true}}}");
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = object.toXml(document, ExportOption.bIncludeDisplayName, ExportOption.bIncludeEditorClass);
		var properties = new JSONObject();
		new Get().addDboProperties(object, properties, findProperty(root, "id"));
		new Get().addDboProperties(object, properties, findProperty(root, "$$id"));
		assertEquals(5, properties.getJSONObject("Business ID").getInt("value"));
		assertEquals("node", properties.getJSONObject("Node ID").getString("value"));
	}

	@Test
	public void doesNotExposeASecondEditableAliasForEngineMetadata() throws Exception {
		var object = new FlowVirtualObject();
		object.setName("item");
		object.setDefinition("{\"disabled\":true,\"props\":{}}");
		object.setVirtualInfo("{\"propertyDefinitions\":{\"$$disabled\":{"
				+ "\"label\":\"Disabled\",\"definitionPath\":\"disabled\",\"type\":\"boolean\"}}}");
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = object.toXml(document, ExportOption.bIncludeDisplayName);
		findProperty(root, "$$disabled");
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && "property".equals(element.getTagName())) {
				assertEquals("Duplicate metadata alias", false, "disabled".equals(element.getAttribute("name")));
			}
		}
	}

	@Test
	public void exposesClosedAuthoringChoicesThroughThePropertiesServiceContract() throws Exception {
		var object = new FlowVirtualObject();
		object.setName("avatar");
		object.setVirtualKind("frontendWidget");
		object.setDefinition(new JSONObject().put("size", "medium").toString());
		object.setVirtualInfo(new JSONObject()
				.put("propertyDefinitions", new JSONObject().put("size", new JSONObject()
						.put("label", "Size")
						.put("kind", "select")
						.put("type", "string")
						.put("enum", new JSONArray().put("small").put("medium").put("large"))))
				.toString());

		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = object.toXml(document, ExportOption.bIncludeDisplayName,
				ExportOption.bIncludeEditorClass, ExportOption.bIncludeShortDescription);
		var propertyElement = findProperty(root, "size");
		var properties = new JSONObject();
		new Get().addDboProperties(object, properties, propertyElement);
		var property = properties.getJSONObject("Size");

		assertEquals("select", property.getString("flowKind"));
		assertEquals("string", property.getString("flowType"));
		assertEquals("medium", property.getString("value"));
		assertEquals(new JSONArray().put("small").put("medium").put("large").toString(),
				property.getJSONArray("values").toString());
	}

	private static Element findProperty(Element root, String name) {
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && "property".equals(element.getTagName())
					&& name.equals(element.getAttribute("name"))) {
				return element;
			}
		}
		throw new AssertionError("Expected Flow property " + name);
	}
}
