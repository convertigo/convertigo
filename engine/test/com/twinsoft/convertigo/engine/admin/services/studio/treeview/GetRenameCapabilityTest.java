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

	@Test public void namesARenamableFlowItemByItsIdentityAndOthersByTheirSummary() throws Exception {
		var item = projected(true);
		item.setSummary("bronn = false");
		item.setVirtualInfo(new JSONObject()
				.put("renameMutation", new JSONObject().put("op", "provider.rename"))
				.put("renameValue", "bronn").toString());
		assertEquals("bronn = false", item.toString());
		assertEquals("bronn", Get.objectName(item));

		var folder = projected(false);
		folder.setSummary("Variables");
		folder.setVirtualInfo("{}");
		assertEquals("Variables", Get.objectName(folder));
	}

	@Test public void namesANativeObjectByItsName() throws Exception {
		var step = new SimpleStep();
		step.setName("checkLogin");
		assertEquals("checkLogin", Get.objectName(step));
	}

	@Test public void leavesNativeObjectsUnchanged() throws Exception {
		var node = new JSONObject();
		Get.putRenameCapability(new SimpleStep(), node);
		assertFalse(node.has("canRename"));
		assertFalse(node.has("renameValue"));
	}
}
