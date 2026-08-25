/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;

public class FlowStudioSupportSelectionTest {
	private static final String SOURCE = "model/Project/src/routes/+page.flow.svelte";

	@Test
	public void matchesProjectedFrontendNodesByStableMutationMetadata() throws Exception {
		var candidate = candidate("frontends.svelte.routes.home.structure.text", "frontAst.nodes[2]", "text");

		assertTrue(FlowStudioSupport.matchesProjectedSelection(candidate, SOURCE, "frontAst.nodes[2]", "", ""));
		assertTrue(FlowStudioSupport.matchesProjectedSelection(candidate, SOURCE, "", "text", ""));
		assertTrue(FlowStudioSupport.matchesProjectedSelection(candidate, "", "", "",
				"frontends.svelte.routes.home.structure.text"));
	}

	@Test
	public void rejectsStaleSourceAndMutationMetadata() throws Exception {
		var candidate = candidate("frontends.svelte.routes.home.structure.text", "frontAst.nodes[2]", "text");

		assertFalse(FlowStudioSupport.matchesProjectedSelection(candidate, "model/Other/+page.flow.svelte",
				"frontAst.nodes[2]", "", ""));
		assertFalse(FlowStudioSupport.matchesProjectedSelection(candidate, SOURCE, "frontAst.nodes[3]", "", ""));
		assertFalse(FlowStudioSupport.matchesProjectedSelection(candidate, SOURCE, "", "other", ""));
	}

	@Test
	public void buildsDeleteMutationOnlyForConcreteCollectionEntries() throws Exception {
		var item = candidate("frontends.svelte.routes.home.structure.text", "frontAst.nodes[2]", "text");
		var container = candidate("frontends.svelte.routes.home.structure", "frontAst.nodes", "structure");

		var mutation = FlowStudioSupport.removeNodeMutation(item);
		assertEquals("delete", mutation.getString("op"));
		assertEquals("frontAst.nodes[2]", mutation.getString("path"));
		assertNull(FlowStudioSupport.removeNodeMutation(container));
	}

	@Test
	public void allocatesFrontendIdsAcrossNestedSourceProjection() throws Exception {
		var existing = candidate("frontends.svelte.routes.home.structure.card.text",
				"frontAst.nodes[0].children[0]", "text");
		existing.setVirtualKind("frontendWidget");
		var used = new HashSet<String>();
		FlowStudioSupport.collectFrontendWidgetIds(new TestContainer(existing), used);

		assertEquals("text2", FlowStudioSupport.uniqueFrontendInsertValue(
				new JSONObject().put("id", "text").put("kind", "Text"), used).getString("id"));
		used.add("text2");
		assertEquals("text3", FlowStudioSupport.uniqueFrontendInsertValue(
				new JSONObject().put("id", "text").put("kind", "Text"), used).getString("id"));
	}

	@Test
	public void derivesFrontendContainerCapabilityFromTheProviderContract() throws Exception {
		assertTrue(FlowStudioSupport.frontendBlockCanContainChildren(new JSONObject()
				.put("slots", new JSONObject().put("children", new JSONObject()))));
		assertTrue(FlowStudioSupport.frontendBlockCanContainChildren(new JSONObject()
				.put("traits", new org.codehaus.jettison.json.JSONArray().put("ui.block").put("ui.container"))));
		assertFalse(FlowStudioSupport.frontendBlockCanContainChildren(new JSONObject()
				.put("traits", new org.codehaus.jettison.json.JSONArray().put("ui.block"))));
		var projectedContainer = new FlowVirtualObject();
		projectedContainer.setVirtualKind("frontendContainerBlock");
		assertTrue(FlowStudioSupport.frontendBlockCanContainChildren(projectedContainer));
	}

	private FlowVirtualObject candidate(String virtualPath, String mutationPath, String id) throws Exception {
		var candidate = new FlowVirtualObject();
		candidate.setVirtualPath(virtualPath);
		candidate.setVirtualInfo(new JSONObject()
				.put("sourcePath", SOURCE)
				.put("sourceMutationPath", mutationPath)
				.toString());
		candidate.setDefinition(new JSONObject().put("id", id).toString());
		return candidate;
	}

	private static final class TestContainer extends DatabaseObject {
		private static final long serialVersionUID = 1L;
		private final List<DatabaseObject> children;

		private TestContainer(DatabaseObject... children) {
			this.children = List.of(children);
		}

		@Override
		public List<DatabaseObject> getDatabaseObjectChildren() {
			return children;
		}
	}
}
