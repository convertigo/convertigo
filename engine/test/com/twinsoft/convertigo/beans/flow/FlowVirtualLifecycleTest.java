package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.codehaus.jettison.json.JSONObject;

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

	@Test
	public void presentationSiblingsKeepUniqueQNamesWithoutReservingSourceNames() throws Exception {
		var json = new JSONObject("{name:'collection',path:'collection',children:["
				+ "{name:'default',path:'common',summary:'default'},"
				+ "{name:'default',path:'definitions.default',summary:'default'},"
				+ "{name:'a-b',path:'definitions.a-b'},"
				+ "{name:'a_b',path:'definitions.a_b'}]}");
		var projection = FlowVirtualProjector.projectedObject(null, json, 0);
		var children = projection.getDatabaseObjectChildren();
		assertEquals("default", children.get(0).getName());
		assertNotEquals(children.get(0).getFullQName(), children.get(1).getFullQName());
		assertNotEquals(children.get(2).getFullQName(), children.get(3).getFullQName());
		assertEquals("definitions.default", ((FlowVirtualObject) children.get(1)).getVirtualPath());
		assertEquals("default", children.get(1).toString());
		assertSame(children.get(1), projection.getDatabaseObjectChild(children.get(1).getName()));
		var refreshed = FlowVirtualProjector.projectedObject(null, json, 0).getDatabaseObjectChildren();
		for (int i = 0; i < children.size(); i++) assertEquals(children.get(i).getFullQName(), refreshed.get(i).getFullQName());
		var collision = (FlowVirtualObject) children.get(1);
		var qname = collision.getFullQName();
		assertTrue(collision.replaceProjectedTree(json.getJSONArray("children").getJSONObject(1)));
		assertEquals("Incremental projection must keep its disambiguated QName", qname, collision.getFullQName());
	}
}
