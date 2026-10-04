package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;

public class FlowAuthoringContextTest {

	@Rule public TemporaryFolder folder = new TemporaryFolder();

	private static final String BLOCK = """
			const _meta = {
			  sourceVersion: 2, runtime: "flow", targets: ["backend"],
			  private: %s, properties: {}, outputs: { out: { type: "object" } }
			};
			function example({ input }) { return { valid: true }; }
			""";

	private static final String SOURCE = """
			function proof() {
			  local.header = custom.header({});
			  local.page = shared.page({});
			  return local.page;
			}
			""";

	private class Fixture {
		final File directory;
		final Project project;
		final FlowEngine owner;
		final Flow flow;
		final Path flowFile;

		Fixture(String source) throws Exception {
			directory = folder.newFolder("Consumer");
			project = new Project() {
				@Override public File getDirFile() { return directory; }
				@Override public String getDirPath() { return directory.getAbsolutePath(); }
				@Override public FlowEngine getFlowEngine() { return owner; }
				@Override public List<Sequence> getSequencesList() { return List.of(flow); }
			};
			project.setName("Consumer");
			owner = new FlowEngine() {
				@Override public Project getProject() { return project; }
			};
			flow = new Flow() {
				@Override public Project getProject() { return project; }
			};
			flow.setParent(project);
			flow.setName("proof");
			flowFile = flow.getFlowSourceFile().toPath();
			write(flowFile, source);
			write(directory.toPath().resolve("c8oProject.yaml"), """
					↓Consumer [core.Project]:
					  ↓Shared [references.ProjectSchemaReference]:
					    projectName: Shared
					""");
			write(directory.toPath().resolve("_flow/blocks/custom/header.block.js"), BLOCK.formatted(true));
			write(directory.toPath().resolve("_flow/blocks/custom/public.block.js"), BLOCK.formatted(false));
			var shared = folder.newFolder("Shared").toPath();
			write(shared.resolve("c8oProject.yaml"), "↓Shared [core.Project]:\n");
			write(shared.resolve("_flow/blocks/shared/page.block.js"), BLOCK.formatted(false));
			write(shared.resolve("_flow/blocks/shared/internal.block.js"), BLOCK.formatted(true));
		}
	}

	private static void write(Path file, String source) throws Exception {
		Files.createDirectories(file.getParent());
		Files.writeString(file, source);
	}

	private static class Provider extends FlowEngineBridge {
		JSONObject request;
		@Override JSONObject invoke(String engineQName, String method, JSONObject request, Context context,
				org.mozilla.javascript.Context javascriptContext, Scriptable scope) {
			this.request = request;
			try { return new JSONObject().put("ok", true); }
			catch (Exception e) { throw new AssertionError(e); }
		}
	}

	/** Runs the actual provider against the request assembled by the Java bridge, without executing a Flow. */
	private static class RhinoProvider extends FlowEngineBridge {
		@Override JSONObject invoke(String engineQName, String method, JSONObject request, Context context,
				org.mozilla.javascript.Context javascriptContext, Scriptable ignored) throws EngineException {
			var resourceRoot = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
			Assume.assumeTrue("Set FLOW_ENGINE_RESOURCE_ROOT for the real provider contract", resourceRoot != null);
			var cx = org.mozilla.javascript.Context.enter();
			try {
				cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
				var scope = cx.initStandardObjects();
				var engineFile = Path.of(resourceRoot, "Engine.js");
				initializeSourceScope(scope, engineFile.toFile());
				ScriptableObject.putProperty(scope, "__flowProjectDir", request.optString("projectDir"));
				var engine = (Scriptable) cx.evaluateString(scope, Files.readString(engineFile), engineFile.toString(), 1, null);
				var call = (Function) ScriptableObject.getProperty(engine, method);
				return new JSONObject(org.mozilla.javascript.Context.toString(call.call(cx, scope, engine, new Object[] { request.toString() })));
			} catch (Exception e) {
				throw new EngineException("Provider contract failed", e);
			} finally {
				org.mozilla.javascript.Context.exit();
			}
		}
	}

	private static JSONObject options() throws Exception {
		return new JSONObject().put("surface", "virtual").put("detail", "compact").put("applyFallback", false);
	}

	private static JSONObject item(JSONObject palette, String block) throws Exception {
		var items = palette.getJSONArray("items");
		for (int i = 0; i < items.length(); i++) {
			var current = items.getJSONObject(i);
			var insert = current.optJSONObject("insert");
			if (insert != null && block.equals(insert.optString("block"))) return current;
		}
		return null;
	}

	@Test public void treePaletteAndBothMutationPathsCarryTheSameOwnerAndEffectiveSources() throws Exception {
		var f = new Fixture(SOURCE);
		try {
			var draft = f.directory.toPath().resolve("_flow/blocks/custom/draft.block.js").toFile().getCanonicalPath();
			var removed = f.directory.toPath().resolve("_flow/blocks/custom/header.block.js").toFile().getCanonicalPath();
			f.owner.setSource(draft, BLOCK.formatted(true));
			f.owner.applySourceChanges(java.util.Map.of(), List.of(removed));
			var provider = new Provider();
			for (String method : List.of("describeTree", "authoringPalette", "authoringMutate", "applyMutation")) {
				switch (method) {
				case "describeTree" -> provider.describeTree(f.flow);
				case "authoringPalette" -> provider.authoringPalette(f.flow, options());
				case "authoringMutate" -> provider.authoringMutate(f.flow, options());
				case "applyMutation" -> provider.applyMutation(f.flow, new JSONObject());
				}
				assertEquals(method, f.directory.getAbsolutePath(), provider.request.getString("projectDir"));
				assertEquals("proof", provider.request.getString("flowName"));
				assertEquals(f.flow.getQName(), provider.request.getString("flowQName"));
				assertEquals(f.flowFile.toString(), provider.request.getString("sourceFile"));
				assertEquals(SOURCE, provider.request.getString("flowSource"));
				assertEquals(BLOCK.formatted(true), provider.request.getJSONObject("frontendSourceDrafts").getString(draft));
				assertEquals(removed, provider.request.getJSONArray("sourceRemovals").getString(0));
			}
		} finally { Flow.projectUnloaded(f.project); }
	}

	@Test public void anUnattachedFlowKeepsItsEmptyProjectContext() throws Exception {
		var provider = new Provider();
		provider.authoringPalette(new Flow(), options());
		assertEquals("", provider.request.getString("projectDir"));
		assertFalse(provider.request.has("sourceFile"));
		assertEquals(0, provider.request.getJSONObject("frontendSourceDrafts").length());
	}

	@Test public void presentationOptionsCannotReplaceTheLoadedOwnerContext() throws Exception {
		var f = new Fixture(SOURCE);
		try {
			var provider = new Provider();
			var options = options().put("projectDir", "/wrong-owner").put("sourceFile", "/wrong-file")
					.put("flowName", "wrong-name").put("target", "engine")
					.put("frontendSourceDrafts", new JSONObject().put("/wrong-file", "wrong draft"));
			provider.authoringPalette(f.flow, options);
			assertEquals(f.directory.getAbsolutePath(), provider.request.getString("projectDir"));
			assertEquals(f.flowFile.toString(), provider.request.getString("sourceFile"));
			assertEquals("proof", provider.request.getString("flowName"));
			assertEquals("flow", provider.request.getString("target"));
			assertEquals(0, provider.request.getJSONObject("frontendSourceDrafts").length());
		} finally { Flow.projectUnloaded(f.project); }
	}

	@Test public void paletteResolvesLocalPrivateAndReferencedPublicBlocks() throws Exception {
		var f = new Fixture(SOURCE);
		try {
			var provider = new RhinoProvider();
			var tree = provider.describeTree(f.flow);
			assertTrue(tree.toString(), tree.getBoolean("ok"));
			var palette = provider.authoringPalette(f.flow, options());
			assertTrue(palette.toString(), palette.getBoolean("ok"));
			assertTrue("Local public block is offered for insertion", item(palette, "custom.public") != null);
			// The existing Flow resolves its private helper; private helpers are not public insertion candidates.
			assertTrue(item(palette, "custom.header") == null);
			assertTrue("Public library block is available to its consumer", item(palette, "shared.page") != null);
			assertTrue("Private library block must not leak to the consumer palette", item(palette, "shared.internal") == null);
		} finally { Flow.projectUnloaded(f.project); }
	}

	@Test public void insertionUsesThatSameCatalogAndOnlyChangesTheFlowDraft() throws Exception {
		var f = new Fixture(SOURCE);
		try {
			var provider = new RhinoProvider();
			var palette = provider.authoringPalette(f.flow, options());
			assertTrue(palette.toString(), palette.getBoolean("ok"));
			var action = item(palette, "shared.page");
			assertTrue(action != null);
			var request = options().put("action", action).put("write", true).put("persist", true);
			var preview = provider.authoringMutate(f.flow, new JSONObject(request.toString()).put("dryRun", true));
			assertTrue(preview.toString(), preview.getBoolean("ok"));
			assertEquals(SOURCE, f.flow.getFlowSource());
			var result = provider.authoringMutate(f.flow, request);
			assertTrue(result.toString(), result.getBoolean("ok"));
			assertTrue(f.flow.isFlowSourceDirty());
			assertFalse(SOURCE.equals(f.flow.getFlowSource()));
			assertEquals(SOURCE, Files.readString(f.flowFile));
			assertTrue(provider.describeTree(f.flow).getBoolean("ok"));
			assertTrue(provider.authoringPalette(f.flow, options()).getBoolean("ok"));
			f.flow.discardFlowSource();
			assertEquals(SOURCE, f.flow.getFlowSource());
		} finally { Flow.projectUnloaded(f.project); }
	}

	@Test public void draftBlockDiscoveryAndRemovalAgreeWithTheTreeWithoutSaving() throws Exception {
		var f = new Fixture("function proof() { local.value = custom.draft({}); return local.value; }\n");
		try {
			var block = f.directory.toPath().resolve("_flow/blocks/custom/draft.block.js");
			f.owner.setSource(block.toString(), BLOCK.formatted(false));
			var provider = new RhinoProvider();
			assertTrue(provider.describeTree(f.flow).getBoolean("ok"));
			var palette = provider.authoringPalette(f.flow, options());
			assertTrue(palette.toString(), palette.getBoolean("ok"));
			assertTrue(item(palette, "custom.draft") != null);
			assertFalse(Files.exists(block));
			f.owner.applySourceChanges(java.util.Map.of(), List.of(block.toString()));
			var removed = provider.authoringPalette(f.flow, options());
			assertFalse(removed.getBoolean("ok"));
			assertEquals("FLOWSCRIPT_CANONICAL_INVALID", removed.getJSONObject("error").getString("code"));
			assertFalse(Files.exists(block));
		} finally { Flow.projectUnloaded(f.project); }
	}

	@Test public void aGenuinelyMissingBlockStillFailsWithoutCreatingAMock() throws Exception {
		var f = new Fixture("function proof() { local.value = absent.block({}); return local.value; }\n");
		try {
			var palette = new RhinoProvider().authoringPalette(f.flow, options());
			assertFalse(palette.getBoolean("ok"));
			assertEquals("FLOWSCRIPT_CANONICAL_INVALID", palette.getJSONObject("error").getString("code"));
			assertFalse(Files.exists(f.directory.toPath().resolve("_flow/blocks/absent")));
		} finally { Flow.projectUnloaded(f.project); }
	}
}
