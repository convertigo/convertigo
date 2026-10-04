/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.*;

import org.junit.Test;

/** Ownership is declared by the AST provider, independently of its paths and kinds. */
public class FlowVirtualWritableContractTest {

	private FlowVirtualObject projection(FlowEngine owner, String path, String info) {
		var object = new FlowVirtualObject(owner, "value", "futureKind", "futureType", path, "", "1");
		object.setVirtualInfo(info);
		return object;
	}

	@Test public void providerOwnershipDoesNotDependOnKnownJavaPaths() {
		for (var path : new String[] { "configs.B1.api.host", "future.any.value", "catalog.localDefinition" }) {
			var object = projection(new FlowEngine(), path, "{\"sourceWritable\":true}");
			assertTrue(path, object.isDefinitionWritable());
		}
		assertFalse(projection(new FlowEngine(), "config.value", "{}").isDefinitionWritable());
	}

	@Test public void explicitRestrictionsWinOverDefinitionValues() throws Exception {
		var parent = projection(new FlowEngine(), "future", "{\"sourceWritable\":true}");
		var child = new FlowVirtualObject(parent, "value", "field", "futureType", "future.value", "", "{\"sourceWritable\":true}");
		assertTrue(child.isDefinitionWritable());
		for (var info : new String[] { "{\"sourceWritable\":false}", "{\"sourceWritable\":true,\"readOnly\":true}",
				"{\"sourceWritable\":true,\"readOnlyReference\":true}" }) {
			child.setVirtualInfo(info);
			assertFalse(info, child.isDefinitionWritable());
			assertFalse(child.isDeletable());
		}
		var undeclared = new FlowVirtualObject(parent, "other", "field", "futureType", "future.other", "", "1");
		assertFalse(undeclared.isDefinitionWritable());
	}

	@Test public void sourceOwnershipAppliesToBackendInstancesToo() {
		var parent = new Flow();
		var child = new FlowVirtualObject(parent, "node", "node", "anything", "nodes[0]", "", "{}");
		assertFalse(child.isDefinitionWritable());
		child.setVirtualInfo("{\"sourceWritable\":true}");
		assertTrue(child.isDefinitionWritable());
		child.setVirtualInfo("{\"sourceWritable\":false}");
		assertFalse(child.isDefinitionWritable());
	}
}
