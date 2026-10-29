package com.twinsoft.convertigo.engine.admin.services.studio.flowpicker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class GetPickerInfoTest {
	@Test
	public void keepsOnlyTheRequestedPropertyInEmbeddedPickerState() throws Exception {
		var info = new JSONObject()
				.put("propertyDefinitions", new JSONObject()
						.put("size", new JSONObject().put("kind", "size"))
						.put("classes", new JSONObject().put("kind", "binding")))
				.put("propertyOrder", new org.codehaus.jettison.json.JSONArray().put("size").put("classes"))
				.put("sourceWritable", true);
		var definitions = new JSONObject().put("size", info.getJSONObject("propertyDefinitions").getJSONObject("size"));

		var filtered = Get.pickerInfoForProperty(info, "size", definitions);

		assertEquals(1, filtered.getJSONObject("propertyDefinitions").length());
		assertTrue(filtered.getJSONObject("propertyDefinitions").has("size"));
		assertFalse(filtered.getJSONObject("propertyDefinitions").has("classes"));
		assertEquals(1, filtered.getJSONArray("propertyOrder").length());
		assertEquals("size", filtered.getJSONArray("propertyOrder").getString(0));
		assertTrue(filtered.getBoolean("sourceWritable"));
		assertEquals(2, info.getJSONObject("propertyDefinitions").length());
	}

	@Test
	public void omitsOnlyAnEditorThePageAlreadyHolds() {
		assertTrue(Get.knownRevision("a1, b2", "b2"));
		assertFalse(Get.knownRevision("a1,b2", "c3"));
		assertFalse(Get.knownRevision("", "a1"));
		assertFalse(Get.knownRevision(null, "a1"));
		assertFalse(Get.knownRevision("a1", ""));
	}

	@Test
	public void answersABindingSourceRequestWithTheSourcesOfTheProperty() throws Exception {
		var sources = new org.codehaus.jettison.json.JSONArray()
				.put(new JSONObject().put("id", "copy").put("pathsDeferred", true));
		var info = new JSONObject().put("propertyDefinitions", new JSONObject()
				.put("text", new JSONObject().put("kind", "binding").put("bindingSources", sources)));

		assertEquals(1, Get.bindingSources(info, "text").length());
		assertEquals("copy", Get.bindingSources(info, "text").getJSONObject(0).getString("id"));
		assertEquals(0, Get.bindingSources(info, "label").length());
		assertEquals(0, Get.bindingSources(null, "text").length());
	}

	@Test
	public void projectsTheFrontendTreeOnlyForContextualProperties() throws Exception {
		var info = new JSONObject().put("propertyDefinitions", new JSONObject()
				.put("size", new JSONObject().put("kind", "size"))
				.put("classes", new JSONObject().put("kind", "binding")));

		assertFalse(Get.requiresFrontendProjection(info, "size"));
		assertTrue(Get.requiresFrontendProjection(info, "classes"));
	}
}
