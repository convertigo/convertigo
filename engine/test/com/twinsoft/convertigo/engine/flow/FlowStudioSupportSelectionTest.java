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
	private static final String SOURCE = "libs/flow/frontbuilder/svelte/model/Project/src/routes/+page.flow.svelte";

	@Test
	public void matchesProjectedFrontendNodesByStableMutationMetadata() throws Exception {
		var candidate = candidate("frontends.svelte.routes.home.structure.text", "frontAst.nodes[2]", "text");

		assertTrue(FlowStudioSupport.matchesProjectedSelection(candidate, SOURCE, "frontAst.nodes[2]", "", ""));
		assertTrue(FlowStudioSupport.matchesProjectedSelection(candidate, SOURCE, "", "text", ""));
		assertTrue(FlowStudioSupport.matchesProjectedSelection(candidate, "", "", "",
				"frontends.svelte.routes.home.structure.text"));
	}

	@Test
	public void exposesTheSameStableReferenceUsedByTheGeneratedFrontend() throws Exception {
		var candidate = candidate("frontends.svelte.routes.home.structure.text", "frontAst.nodes[2]", "text");

		var reference = FlowStudioSupport.authoringReference(candidate);

		assertEquals("text", reference.getString("nodeId"));
		assertEquals(SOURCE, reference.getString("sourceRelativePath"));
		assertEquals("frontAst.nodes[2]", reference.getString("sourceMutationPath"));
		assertFalse(reference.has("sourceProject"));
	}

	@Test
	public void exposesFileLevelFrontendSourcesWithoutMutationMetadata() throws Exception {
		var source = candidate("frontends.svelte.appStyles", "", "appStyles");

		assertEquals(SOURCE, FlowStudioSupport.authoringSourceRelativePath(source));
		assertNull(FlowStudioSupport.authoringReference(source));
	}

	@Test
	public void resolvesAFrontendSourceWithoutRequiringMutationMetadata() throws Exception {
		var source = candidate("frontends.svelte.appStyles", "", "appStyles");
		var root = new TestContainer(source);

		assertEquals(source, FlowStudioSupport.findFrontendSource(root, SOURCE));
		assertNull(FlowStudioSupport.findFrontendSource(root,
				"libs/flow/frontbuilder/svelte/model/Project/src/routes/other.flow.svelte"));
	}

	@Test
	public void exposesStudioClientActionsThroughTheSharedMenuDescriptor() throws Exception {
		var candidate = candidate("frontends.svelte.routes.home.structure.text", "frontAst.nodes[2]", "text");
		var menu = new JSONObject()
				.put("ok", true)
				.put("protocol", "flow.studio.menu.v1")
				.put("items", new org.codehaus.jettison.json.JSONArray());

		FlowStudioSupport.appendStudioClientActions(menu, candidate);
		FlowStudioSupport.appendStudioClientActions(menu, candidate);

		var items = menu.getJSONArray("items");
		assertEquals(3, items.length());
		assertEquals("frontend.reveal", items.getJSONObject(0).getString("clientAction"));
		assertEquals("icons/studio/web_color_16x16.png", items.getJSONObject(0).getString("iconFile16"));
		assertEquals("palette.reveal", items.getJSONObject(1).getString("clientAction"));
		assertEquals("definition.reveal", items.getJSONObject(2).getString("clientAction"));
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

	@Test
	public void usesHumanFrontendCategoriesWithoutProviderInternals() throws Exception {
		assertEquals("Svelte / Layout", FlowStudioSupport.frontendBlockCategoryName(new JSONObject()
				.put("provider", "lib_flow_frontbuilder_svelte")
				.put("namespace", "svelte")
				.put("category", "Svelte / Layout")));
		assertEquals("Charts", FlowStudioSupport.frontendBlockCategoryName(new JSONObject()
				.put("provider", "lib_flow_frontend_charts_svelte")));
		assertEquals("Components", FlowStudioSupport.frontendBlockCategoryName(new JSONObject()));
	}

	@Test
	public void acceptsPropsAsTheFrontendPalettePropertyContract() throws Exception {
		var props = new JSONObject().put("value", new JSONObject()
				.put("label", "Value")
				.put("description", "Current value."));
		assertEquals(props.toString(), FlowStudioSupport.frontendPalettePropertyDefinitions(
				new JSONObject().put("props", props)).toString());
		assertEquals(0, FlowStudioSupport.frontendPalettePropertyDefinitions(new JSONObject()).length());
	}

	@Test
	public void exposesOnlyProviderRelativePaletteSources() throws Exception {
		var root = new java.io.File("build/test-provider").getAbsoluteFile();
		var source = new java.io.File(root,
				"libs/flow/frontbuilder/svelte/components/DatePicker.flow.svelte");
		assertEquals("libs/flow/frontbuilder/svelte/components/DatePicker.flow.svelte",
				FlowStudioSupport.relativePaletteSourcePath(root.getPath(), source.getPath()));
		assertEquals("", FlowStudioSupport.relativePaletteSourcePath(root.getPath(),
				new java.io.File(root.getParentFile(), "outside.flow.svelte").getPath()));

		var projectRoots = new java.util.LinkedHashMap<String, String>();
		projectRoots.put("frontbuilder.svelte", new java.io.File(root, "unrelated").getPath());
		projectRoots.put("lib_flow_frontbuilder_svelte", root.getPath());
		var location = FlowStudioSupport.resolvePaletteSourceLocation(projectRoots,
				"frontbuilder.svelte", "", source.getPath());
		assertEquals("lib_flow_frontbuilder_svelte", location.getString("project"));
		assertEquals("libs/flow/frontbuilder/svelte/components/DatePicker.flow.svelte",
				location.getString("relativePath"));

		location = FlowStudioSupport.resolvePaletteSourceLocation(projectRoots,
				"lib_flow_frontbuilder_svelte",
				"libs/flow/frontbuilder/svelte/components/DatePicker.flow.svelte", "");
		assertEquals("lib_flow_frontbuilder_svelte", location.getString("project"));
		assertEquals("libs/flow/frontbuilder/svelte/components/DatePicker.flow.svelte",
				location.getString("relativePath"));
	}

	@Test
	public void exposesPerformancePhasesOnlyForAnExplicitProfile() throws Exception {
		var regular = new JSONObject();
		var regularOwner = FlowStudioSupport.startPerformanceProfile(false, "regular");
		FlowStudioSupport.finishPerformanceProfile(regularOwner, regular);
		assertFalse(regular.has("_profile"));

		var profiled = new JSONObject();
		var profileOwner = FlowStudioSupport.startPerformanceProfile(true, "test.operation");
		assertTrue(profileOwner);
		FlowStudioSupport.performanceProfileMark("test.phase");
		FlowStudioSupport.finishPerformanceProfile(profileOwner, profiled);

		var profile = profiled.getJSONObject("_profile");
		assertEquals("flow.performance.v1", profile.getString("protocol"));
		assertEquals("test.operation", profile.getString("operation"));
		assertTrue(profile.getDouble("totalMs") >= 0);
		assertTrue(profile.getJSONObject("phases").has("test.phase"));
		assertTrue(profile.getJSONObject("phases").has("service.response"));
	}

	private FlowVirtualObject candidate(String virtualPath, String mutationPath, String id) throws Exception {
		var candidate = new FlowVirtualObject();
		candidate.setVirtualPath(virtualPath);
		candidate.setVirtualInfo(new JSONObject()
				.put("sourcePath", SOURCE)
				.put("sourceRelativePath", SOURCE)
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
