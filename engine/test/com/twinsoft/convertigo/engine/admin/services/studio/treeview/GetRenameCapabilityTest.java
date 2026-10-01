package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import static org.junit.Assert.*;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.steps.SimpleStep;

public class GetRenameCapabilityTest {
	private FlowVirtualObject projected(boolean writable) throws Exception {
		var object = new FlowVirtualObject() {
			@Override public boolean isDefinitionWritable() { return writable; }
		};
		object.setVirtualKind("provider.future-kind");
		object.setName("authoring_technical_id");
		return object;
	}

	@Test public void exposesProviderCapabilityAndEditableNameWithoutKnowingTheKind() throws Exception {
		var object = projected(true);
		object.setVirtualInfo(new JSONObject()
				.put("renameMutation", new JSONObject().put("op", "provider.rename"))
				.put("renameValue", "humanName").toString());
		var node = new JSONObject();
		Get.putRenameCapability(object, node);
		assertTrue(node.getBoolean("canRename"));
		assertEquals("humanName", node.getString("renameValue"));
	}

	@Test public void explicitlyRejectsProjectionWithoutRenameMutation() throws Exception {
		var object = projected(true);
		object.setVirtualInfo("{}");
		var node = new JSONObject();
		Get.putRenameCapability(object, node);
		assertFalse(node.getBoolean("canRename"));
		assertFalse(node.has("renameValue"));
	}

	@Test public void doesNotExposeRenameForReadOnlyProjection() throws Exception {
		var object = projected(false);
		object.setVirtualInfo("{\"renameMutation\":{\"op\":\"provider.rename\"}}");
		var node = new JSONObject();
		Get.putRenameCapability(object, node);
		assertFalse(node.getBoolean("canRename"));
		assertFalse(node.has("renameValue"));
	}

	@Test public void leavesNativeObjectsUnchanged() throws Exception {
		var node = new JSONObject();
		Get.putRenameCapability(new SimpleStep(), node);
		assertFalse(node.has("canRename"));
		assertFalse(node.has("renameValue"));
	}
}
