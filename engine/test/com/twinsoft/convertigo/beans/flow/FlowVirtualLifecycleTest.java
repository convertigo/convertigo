package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

	@Test
	public void engineVirtualChildrenAreFoundByName() throws Exception {
		// Qualified names resolve through getDatabaseObjectChild: the generic walker skips
		// the Flow virtual children of a FlowEngine, which must be found among its children.
		var frontends = new FlowVirtualObject();
		frontends.setName("frontends");
		var engine = new FlowEngine() {
			@Override
			public java.util.List<com.twinsoft.convertigo.beans.core.DatabaseObject> getFlowVirtualChildren() {
				return java.util.List.of(frontends);
			}
		};
		assertSame(frontends, engine.getDatabaseObjectChild("frontends"));
		assertNull(engine.getDatabaseObjectChild("missing"));
	}
}
