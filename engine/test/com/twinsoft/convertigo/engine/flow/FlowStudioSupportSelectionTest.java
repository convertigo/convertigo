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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;

public class FlowStudioSupportSelectionTest {
	private static final String SOURCE = "libs/flow/frontbuilder/svelte/model/Project/src/routes/+page.flow.svelte";

	@Test
	public void backendPaletteActionKeepsProviderProjectionAndSelectionOnOwnerOrSlot() throws Exception {
		var flow = new Flow() {
			@Override public List<DatabaseObject> getDatabaseObjectChildren() { return List.of(); }
		};
		flow.setName("backend");
		var slot = new FlowVirtualObject();
		slot.setParent(flow);
		slot.setVirtualPath("nodes");
		var data = new JSONObject().put("authoringAction", new JSONObject().put("id", "provider.action"))
				.put("acceptedPositions", new org.codehaus.jettison.json.JSONArray().put("inside"))
				.put("targetSlot", new JSONObject().put("id", "nodes"));
		var transfer = new JSONObject().put("data", data);
		for (var target : List.of(flow, slot)) {
			assertTrue(FlowStudioSupport.canAddFromPalette(target, "inside", transfer));
			var calls = new int[1];
			var bridge = new FlowEngineBridge() {
				@Override public JSONObject authoringMutate(Flow owner, JSONObject options) {
					assertSame(flow, owner);
					assertEquals(target == flow ? "" : "nodes", options.optJSONObject("action").optString("targetPath"));
					assertEquals("provider.action", options.optJSONObject("action").optString("id"));
					assertEquals("nodes", options.optJSONObject("action").optString("targetSlotId"));
					calls[0]++;
					try {
						var child = new JSONObject().put("name", "created").put("path", "nodes[0]")
								.put("kind", "node").put("type", "provider.block")
								.put("info", new JSONObject().put("sourceMutationPath", "nodes[0]").toString());
						var tree = new JSONObject().put("name", "flow").put("path", "nodes")
								.put("children", new org.codehaus.jettison.json.JSONArray().put(child));
						return new JSONObject().put("ok", true).put("selectionMutationPath", "nodes[0]")
								.put("children", new org.codehaus.jettison.json.JSONArray().put(tree));
					} catch (Exception e) { throw new AssertionError(e); }
				}
			};
			var result = FlowStudioSupport.addFromPalette(target, "inside", transfer, bridge);
			assertEquals(1, calls[0]);
			assertTrue(result.getBoolean("done"));
			assertTrue(result.getBoolean("projected"));
			assertEquals("nodes", result.getString("projectedRootPath"));
			assertEquals("nodes[0]", result.getString("selectionVirtualPath"));
			assertNotEquals(flow.getFullQName(), result.getString("id"));
		}
	}

	@Test
	public void webRemovalUsesTheSameVirtualDeleteAsEclipse() throws Exception {
		var calls = new int[1];
		var parent = new FlowEngine();
		var object = new FlowVirtualObject() {
			@Override public boolean isDeletable() { return true; }
			@Override public void delete() { calls[0]++; }
		};
		object.setParent(parent);
		object.setVirtualPath("config.namedGroup");
		var result = FlowStudioSupport.removeNode(object);
		assertEquals(1, calls[0]);
		assertTrue(result.getBoolean("done"));
		assertTrue(parent.hasChanged);
		assertEquals(parent.getFullQName(), result.getString("parentId"));
	}

	@Test
	public void sourceClipboardUsesEffectiveSnapshotAndSourceMutationAdapter() throws Exception {
		var engine = new FlowEngine() {
			@Override public String getSource(String path) {
				assertEquals(SOURCE, path);
				return "effective source at copy time";
			}
		};
		var target = candidate("projected.component.card", "frontAst.children[0]", "card");
		target.setParent(engine);
		var info = target.getVirtualInfoObject().put("sourceWritable", true);
		target.setVirtualInfo(info.toString());
		var clipboard = FlowStudioSupport.virtualClipboard(target);
		assertEquals("effective source at copy time", clipboard.getString("sourceSnapshot"));
		assertEquals("frontAst.children[0]", clipboard.getString("sourceMutationPath"));
		var calls = new int[1];
		var bridge = new FlowEngineBridge() {
			@Override public JSONObject applySourceMutation(FlowEngine owner, String path, JSONObject mutation, String root) {
				calls[0]++;
				assertSame(engine, owner);
				assertEquals(SOURCE, path);
				assertEquals("paste", mutation.optString("op"));
				assertEquals("frontAst.children[0]", mutation.optString("from"));
				assertEquals("effective source at copy time", mutation.optString("sourceSnapshot"));
				try { return new JSONObject().put("ok", false).put("message", "Test rejection"); }
				catch (Exception e) { throw new AssertionError(e); }
			}
			@Override public JSONObject authoringMutate(FlowEngine owner, JSONObject options) {
				throw new AssertionError("Source clipboard must never mutate engine.yaml");
			}
		};
		assertFalse(FlowStudioSupport.pasteVirtualClipboard(target, clipboard.toString(), bridge).getBoolean("done"));
		assertEquals(1, calls[0]);
		clipboard.remove("sourceSnapshot");
		assertFalse(FlowStudioSupport.pasteVirtualClipboard(target, clipboard.toString(), bridge).getBoolean("done"));
		assertEquals(1, calls[0]);
	}

	@Test
	public void sourcePasteSelectsTheNewObjectAfterProjectionDetachesItsDestination() throws Exception {
		var engine = new FlowEngine() {
			@Override public String getSource(String path) { return "snapshot"; }
			@Override public List<DatabaseObject> getDatabaseObjectChildren() { return List.of(); }
		};
		var root = candidate("projected.page", "frontAst", "page");
		root.setParent(engine);
		var targetInfo = new JSONObject().put("sourcePath", SOURCE).put("sourceWritable", true)
				.put("sourceMutationPath", "frontAst.children[0]");
		var tree = new JSONObject().put("path", "projected.page").put("name", "page")
				.put("info", root.getVirtualInfo()).put("children", new org.codehaus.jettison.json.JSONArray()
						.put(new JSONObject().put("path", "projected.page.card").put("name", "card")
								.put("info", targetInfo.toString())));
		assertTrue(root.replaceProjectedTree(tree));
		var target = (FlowVirtualObject) root.getDatabaseObjectChildren().get(0);
		var clipboard = FlowStudioSupport.virtualClipboard(target);
		var bridge = new FlowEngineBridge() {
			@Override public JSONObject applySourceMutation(FlowEngine owner, String path, JSONObject mutation, String rootPath) {
				try {
					return new JSONObject().put("ok", true).put("selectionMutationPath", "frontAst.children[0]")
							.put("authoringTree", new JSONObject().put("ok", true)
									.put("children", new org.codehaus.jettison.json.JSONArray().put(tree)));
				} catch (Exception e) { throw new AssertionError(e); }
			}
		};
		// No running Studio/dev server in this projection-only fixture.
		var previousLogger = com.twinsoft.convertigo.engine.Engine.logStudio;
		JSONObject result;
		try {
			com.twinsoft.convertigo.engine.Engine.logStudio = org.apache.log4j.Logger.getLogger(getClass());
			result = FlowStudioSupport.pasteVirtualClipboard(target, clipboard.toString(), bridge);
		} finally {
			com.twinsoft.convertigo.engine.Engine.logStudio = previousLogger;
		}
		assertNull(target.getParent());
		assertTrue(result.getBoolean("done"));
		assertEquals("projected.page.card", result.getString("selectionVirtualPath"));
		assertSame(tree, result.getJSONObject("projectedTree"));
		assertEquals("projected.page", result.getString("projectedRootPath"));
		assertEquals(root.getDatabaseObjectChildren().get(0).getFullQName(), result.getString("id"));
	}

	@Test
	public void backendClipboardRoutesLeafFolderAndOwnerToTheSameProvider() throws Exception {
		var owner = new Flow();
		var folder = new FlowVirtualObject();
		folder.setParent(owner);
		folder.setVirtualPath("nodes");
		var leaf = new FlowVirtualObject();
		leaf.setParent(folder);
		leaf.setVirtualPath("nodes[0]");
		var provider = new FlowEngineBridge() {
			@Override
			public JSONObject authoringMutate(Flow flow, JSONObject options) {
				assertSame(owner, flow);
				try {
					return new JSONObject().put("ok", true).put("selectionMutationPath", "nodes[1]")
							.put("children", new org.codehaus.jettison.json.JSONArray().put(new JSONObject().put("path", "nodes")));
				} catch (Exception e) { throw new AssertionError(e); }
			}
		};
		for (DatabaseObject target : List.of(leaf, folder, owner)) {
			var result = FlowStudioSupport.pasteVirtualClipboard(target, FlowStudioSupport.virtualClipboard(leaf).toString(), provider);
			assertTrue(result.getBoolean("done"));
			assertTrue(result.getBoolean("projected"));
			assertEquals("nodes", result.getString("projectedRootPath"));
			assertEquals("nodes[1]", result.getString("selectionMutationPath"));
		}
	}

	@Test
	public void valueDisplayUsesProviderProjectionWithoutKindOrPropertyHeuristics() throws Exception {
		var object = new FlowVirtualObject();
		object.setVirtualPath("arbitrary.entry");
		object.setSummary("old display");
		var node = new JSONObject().put("path", "arbitrary.entry").put("name", "entry")
				.put("kind", "provider.custom").put("type", "provider.type")
				.put("summary", "formatted new value").put("definition", "\"new value\"");
		var tree = new JSONObject().put("children", new org.codehaus.jettison.json.JSONArray().put(node));
		assertTrue(FlowStudioSupport.refreshVirtualObjectFromTree(object, tree));
		assertEquals("formatted new value", object.getSummary());
		assertEquals("new value", object.getDefinitionValue());
		assertFalse(FlowStudioSupport.refreshVirtualObjectFromTree(object, new JSONObject()));
		assertEquals("formatted new value", object.getSummary());
	}

	@Test
	public void pasteUsesReturnedProjectionWithoutMaterializingTheWholeEngine() throws Exception {
		var root = new FlowVirtualObject();
		root.setVirtualPath("config");
		var reads = new int[1];
		var engine = new FlowEngine() {
			@Override
			public List<DatabaseObject> getDatabaseObjectChildren() {
				reads[0]++;
				return List.of(root);
			}
		};
		var target = new FlowVirtualObject();
		target.setParent(engine);
		target.setVirtualPath("config.group");
		var bridge = new FlowEngineBridge() {
			@Override
			public JSONObject authoringMutate(FlowEngine owner, JSONObject options) {
				assertTrue(options.optBoolean("includeTree", false));
				try {
					assertEquals("config", options.getJSONArray("projectionPaths").getString(0));
					return new JSONObject().put("ok", true).put("selectionVirtualPath", "config.group.copy")
							.put("children", new org.codehaus.jettison.json.JSONArray().put(new JSONObject()
									.put("path", "config").put("name", "Config")
									.put("children", new org.codehaus.jettison.json.JSONArray().put(new JSONObject()
											.put("path", "config.group.copy").put("name", "copy")))));
				} catch (Exception e) { throw new AssertionError(e); }
			}
		};
		var result = FlowStudioSupport.pasteVirtualClipboard(target, FlowStudioSupport.virtualClipboard(target).toString(), bridge);
		assertTrue(result.getBoolean("done"));
		assertTrue(result.getBoolean("projected"));
		assertEquals("config.group.copy", result.getString("selectionVirtualPath"));
		assertEquals("config", result.getJSONObject("projectedTree").getString("path"));
		assertNotEquals(engine.getFullQName(), result.getString("id"));
		assertTrue(result.getString("id").endsWith(".copy"));
		assertTrue(result.getString("parentId").endsWith(".Config"));
		assertEquals(0, reads[0]);
	}

	@Test
	public void renameMaterializesOneProjectionWithoutRequestingAnUnusedTree() throws Exception {
		var root = new FlowVirtualObject();
		root.setVirtualPath("config");
		assertTrue(root.replaceProjectedTree(new JSONObject().put("path", "config").put("name", "Config")
				.put("children", new org.codehaus.jettison.json.JSONArray().put(new JSONObject()
						.put("path", "config.after").put("name", "after")
						.put("info", "{\"sourceMutationPath\":\"config.after\"}")))));
		var reads = new int[1];
		var engine = new FlowEngine() {
			@Override
			public List<DatabaseObject> getDatabaseObjectChildren() {
				reads[0]++;
				return List.of(root);
			}
		};
		var object = new FlowVirtualObject() {
			@Override
			public boolean isDefinitionWritable() { return true; }
		};
		object.setParent(engine);
		object.setVirtualPath("config.before");
		object.setVirtualInfo("{\"renameMutationOp\":\"rename\",\"sourceMutationPath\":\"config.before\"}");
		var bridge = new FlowEngineBridge() {
			@Override
			public JSONObject applyMutation(FlowEngine owner, JSONObject mutation, boolean includeTree) {
				assertSame(engine, owner);
				assertFalse(includeTree);
				assertEquals("config.before", mutation.optString("path"));
				assertEquals("after", mutation.optString("value"));
				try {
					return new JSONObject().put("ok", true).put("selectionMutationPath", "config.after");
				} catch (Exception e) { throw new AssertionError(e); }
			}
		};
		var result = FlowStudioSupport.renameVirtualObject(object, "after", bridge);
		assertTrue(result.getBoolean("projected"));
		assertEquals("config", result.getString("projectedRootPath"));
		assertEquals("config.after", result.getString("selectionMutationPath"));
		assertEquals("config.after", result.getString("selectionVirtualPath"));
		assertEquals(root.getDatabaseObjectChildren().get(0).getFullQName(), result.getString("id"));
		assertEquals(1, reads[0]);
	}

	@Test
	public void clipboardTransportsProviderTraitsWithoutInterpretingTheKind() throws Exception {
		var source = new FlowVirtualObject();
		source.setVirtualKind("arbitraryProviderKind");
		source.setVirtualInfo("{\"traits\":[\"example.entry\",\"example.named\"]}");
		var clipboard = FlowStudioSupport.virtualClipboard(source);
		assertEquals("example.entry", clipboard.getJSONArray("traits").getString(0));
		assertEquals("example.named", clipboard.getJSONArray("traits").getString(1));
		assertTrue(FlowStudioSupport.isVirtualClipboard(clipboard.toString()));
	}

	@Test
	public void distinguishesNativePaletteEntriesFromVirtualDescriptors() throws Exception {
		for (var type : List.of("Dbo", "Ion")) {
			var data = new JSONObject().put("type", type).put("id", "any.native.component");
			assertFalse(FlowStudioSupport.isFlowPaletteData(
					new JSONObject().put("type", "paletteData").put("data", data)));
		}
		var data = new JSONObject().put("id", "opaque.virtual.prototype");
		assertTrue(FlowStudioSupport.isFlowPaletteData(
				new JSONObject().put("type", "paletteData").put("data", data)));
		assertFalse(FlowStudioSupport.isFlowPaletteData(
				new JSONObject().put("type", "treeData").put("data", data)));
		assertFalse(FlowStudioSupport.isFlowPaletteData(null));
	}

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
	public void webRemovalRequiresTheVirtualDeleteCapability() throws Exception {
		var item = candidate("frontends.svelte.routes.home.structure.text", "frontAst.nodes[2]", "text");
		item.setParent(new FlowEngine());
		assertFalse(FlowStudioSupport.canRemoveNode(item));
		assertFalse(FlowStudioSupport.removeNode(item).getBoolean("done"));
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

	@Test
	public void preservesAProjectedTreeWhenOnlyCatalogDataIsInvalidated() throws Exception {
		var engine = new FlowEngine();
		var projected = candidate("frontends.svelte.routes.home", "frontAst", "home");
		projected.setParent(engine);
		engine.restoreFlowVirtualChildrenCache(List.of(projected));

		FlowStudioSupport.clearCatalogCache(engine, false);

		assertSame(projected, engine.snapshotFlowVirtualChildrenCache().get(0));
		assertSame(projected, engine.getFlowVirtualChildren().get(0));

		FlowStudioSupport.clearCatalogCache(engine);
		assertNull(engine.snapshotFlowVirtualChildrenCache());
	}

	@Test
	public void resolvesCurrentProjectionRatherThanDetachedViewerBean() throws Exception {
		for (var path : List.of("config", "custom.scope", "frontends.svelte.routes.home")) {
			var previous = candidate(path, "", "old");
			var current = candidate(path, "", "new");
			var owner = new TestContainer(current);
			previous.setParent(owner);
			current.setParent(owner);
			assertSame(current, FlowStudioSupport.currentProjectionRoot(previous.getParent(), SOURCE, path));
			assertNull(FlowStudioSupport.currentProjectionRoot(owner, "another-source", path));
			assertNull(FlowStudioSupport.currentProjectionRoot(owner, SOURCE, "missing"));
		}
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
