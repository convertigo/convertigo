package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;

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
}
