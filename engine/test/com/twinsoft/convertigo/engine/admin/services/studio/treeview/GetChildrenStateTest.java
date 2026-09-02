package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.steps.SimpleStep;

public class GetChildrenStateTest {
	@Test
	public void distinguishesEmptyContainersFromLeaves() throws Exception {
		var container = new FlowVirtualObject();
		container.setVirtualKind("frontendContainerBlock");
		var leaf = new FlowVirtualObject();
		leaf.setVirtualKind("frontendWidget");

		assertEquals(0, Get.lazyChildrenState(container, false));
		assertEquals(0, Get.loadedChildrenState(container, new JSONArray(), false));
		assertFalse((Boolean) Get.lazyChildrenState(leaf, false));
		assertFalse((Boolean) Get.loadedChildrenState(leaf, new JSONArray(), false));
	}

	@Test
	public void preservesLoadedChildrenForContainers() throws Exception {
		var container = new FlowVirtualObject();
		container.setVirtualKind("frontendContainerBlock");
		var children = new JSONArray().put("child");

		assertSame(children, Get.loadedChildrenState(container, children, false));
	}

	@Test
	public void exposesDescriptorIconsAsIconifyIdentifiers() throws Exception {
		var avatar = new FlowVirtualObject();
		avatar.setDefinition(new org.codehaus.jettison.json.JSONObject()
				.put("icon", "mdi:account-circle-outline")
				.toString());
		assertEquals("mdi:account-circle-outline", Get.iconifyIcon(avatar));

		var image = new FlowVirtualObject();
		image.setDefinition(new org.codehaus.jettison.json.JSONObject()
				.put("icon", "/images/image.png")
				.toString());
		assertEquals("", Get.iconifyIcon(image));
	}

	@Test
	public void exposesFlowAndBackendEnabledState() throws Exception {
		var enabledFlow = new FlowVirtualObject();
		assertEquals(Boolean.TRUE, Get.enabledState(enabledFlow));

		var disabledDefinition = new FlowVirtualObject();
		disabledDefinition.setDefinition(new org.codehaus.jettison.json.JSONObject()
				.put("disabled", true)
				.toString());
		assertEquals(Boolean.FALSE, Get.enabledState(disabledDefinition));

		var disabledInfo = new FlowVirtualObject();
		disabledInfo.setVirtualInfo(new org.codehaus.jettison.json.JSONObject()
				.put("disabled", true)
				.toString());
		assertEquals(Boolean.FALSE, Get.enabledState(disabledInfo));

		var backendStep = new SimpleStep();
		backendStep.setEnabled(false);
		assertEquals(Boolean.FALSE, Get.enabledState(backendStep));
	}
}
