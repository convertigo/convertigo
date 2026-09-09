package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlowVirtualLifecycleTest {
	@Test
	public void deletionUsesProjectedCapabilityNotVirtualKind() {
		var object = new FlowVirtualObject() {
			@Override
			public boolean isDefinitionWritable() {
				return true;
			}
		};
		object.setVirtualKind("arbitraryProviderKind");
		assertFalse(object.isDefinitionDeletable());
		object.setVirtualInfo("{\"deletable\":true}");
		assertTrue(object.isDefinitionDeletable());
		object.setVirtualInfo("{\"deletable\":false}");
		assertFalse(object.isDefinitionDeletable());
	}

	@Test
	public void deletionStillRequiresWritableDefinition() {
		var object = new FlowVirtualObject();
		object.setVirtualInfo("{\"deletable\":true}");
		assertFalse(object.isDefinitionDeletable());
	}
}
