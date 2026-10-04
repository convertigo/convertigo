package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import com.twinsoft.convertigo.beans.flow.FlowWorkingCopies;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;

/** The shared bridge used by both Studios, with the actual Rhino descriptor provider. */
public class FlowNamedConfigurationsTest {

	@Rule public TemporaryFolder folder = new TemporaryFolder();

	private static final String SAVED = """
			{"version":1,"config":{"common":true},"configs":{"B1":{"api":{"host":"saved"}}}}
			""";

	private static class Model {
		final Project project;
		final FlowEngine owner;
		Model(File directory) throws Exception {
			project = new Project() {
				@Override public File getDirFile() { return directory; }
				@Override public String getDirPath() { return directory.getAbsolutePath(); }
				@Override public FlowEngine getFlowEngine() { return owner; }
				@Override public List<Sequence> getSequencesList() { return List.of(); }
			};
			project.setName("ConfigProof");
			owner = new FlowEngine() { @Override public Project getProject() { return project; } };
			owner.setName("FlowEngine");
		}
	}

	private static class Provider extends FlowEngineBridge {
		@Override JSONObject invoke(String engineQName, String method, JSONObject request, Context context,
				org.mozilla.javascript.Context javascriptContext, Scriptable ignored) throws EngineException {
			var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
			Assume.assumeTrue("Set FLOW_ENGINE_RESOURCE_ROOT for the real provider contract", root != null);
			var cx = org.mozilla.javascript.Context.enter();
			try {
				cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
				var scope = cx.initStandardObjects();
				var file = Path.of(root, "Engine.js");
				initializeSourceScope(scope, file.toFile());
				ScriptableObject.putProperty(scope, "__flowProjectDir", request.optString("projectDir"));
				var engine = (Scriptable) cx.evaluateString(scope, Files.readString(file), file.toString(), 1, null);
				var call = (Function) ScriptableObject.getProperty(engine, method);
				return new JSONObject(org.mozilla.javascript.Context.toString(call.call(cx, scope, engine,
						new Object[] { request.toString() })));
			} catch (Exception e) {
				throw new EngineException("Named configuration provider failed", e);
			} finally { org.mozilla.javascript.Context.exit(); }
		}
	}

	private static JSONObject find(JSONObject tree, String path) throws Exception {
		if (path.equals(tree.optString("path"))) return tree;
		var children = tree.optJSONArray("children");
		if (children != null) for (int i = 0; i < children.length(); i++) {
			var found = find(children.getJSONObject(i), path);
			if (found != null) return found;
		}
		return null;
	}

	private Model savedModel() throws Exception {
		var directory = folder.newFolder();
		Files.createDirectories(directory.toPath().resolve("_flow"));
		Files.writeString(directory.toPath().resolve("_flow/engine.yaml"), SAVED);
		return new Model(directory);
	}

	@Test public void defaultAndNamedConfigurationsShareOneProjectionWithTheirOriginalSourcePaths() throws Exception {
		var model = savedModel();
		var provider = new Provider();
		try {
			var tree = provider.describeTree(model.owner);
			var collection = find(tree, "configs");
			assertEquals("Configs", collection.getString("summary"));
			assertEquals("default", collection.getJSONArray("children").getJSONObject(0).getString("name"));
			assertEquals("config", collection.getJSONArray("children").getJSONObject(0).getString("path"));
			assertNotNull(find(collection, "configs.B1.api.host"));
			var edited = provider.applyMutation(model.owner,
					new JSONObject().put("op", "replace").put("path", "config.common").put("value", false), true, "configs");
			assertTrue(edited.toString(), edited.getBoolean("ok"));
			assertEquals("false", find(edited, "config.common").getString("definition"));
			assertNotNull(find(edited, "configs.B1.api.host"));
			assertEquals(SAVED, Files.readString(model.project.getDirFile().toPath().resolve("_flow/engine.yaml")));
		} finally { Flow.projectUnloaded(model.project); }
	}

	@Test public void namedConfigurationEditsUseTheExistingDraftSaveReloadLifecycle() throws Exception {
		var model = savedModel();
		var provider = new Provider();
		var file = model.project.getDirFile().toPath().resolve("_flow/engine.yaml");
		try {
			assertNotNull(find(provider.describeTree(model.owner), "configs.B1.api.host"));
			var changed = provider.applyMutation(model.owner,
					new JSONObject().put("op", "replace").put("path", "configs.B1.api.host").put("value", "draft"), true, "configs");
			assertTrue(changed.toString(), changed.getBoolean("ok"));
			assertEquals("\"draft\"", find(changed, "configs.B1.api.host").getString("definition"));
			assertTrue(model.owner.isEngineSourceDirty());
			assertEquals(SAVED, Files.readString(file));
			assertEquals(model.owner.getEngineSource(), FlowWorkingCopies.of(model.project).get("engine"));

			Flow.projectUnloaded(model.project);
			model = new Model(file.getParent().getParent().toFile());
			assertEquals("\"saved\"", find(provider.describeTree(model.owner), "configs.B1.api.host").getString("definition"));
			provider.applyMutation(model.owner, new JSONObject().put("op", "renameKey").put("path", "configs.B1").put("value", "ServerOne"));
			model.owner.saveSources();
			assertFalse(model.owner.isEngineSourceDirty());
			Flow.projectUnloaded(model.project);
			model = new Model(file.getParent().getParent().toFile());
			assertNotNull(find(provider.describeTree(model.owner), "configs.ServerOne.api.host"));
			assertTrue(FlowWorkingCopies.of(model.project).isEmpty());
		} finally { Flow.projectUnloaded(model.project); }
	}

	@Test public void paletteCreationAndInvalidEditsStayInTheSameGenericEngineContract() throws Exception {
		var model = savedModel();
		var provider = new Provider();
		try {
			var palette = provider.authoringPalette(model.owner,
					new JSONObject().put("surface", "virtual").put("focusPath", "configs").put("applyFallback", false));
			assertTrue(palette.toString(), palette.getBoolean("ok"));
			assertEquals(1, palette.getJSONArray("items").length());
			var item = palette.getJSONArray("items").getJSONObject(0);
			var created = provider.authoringMutate(model.owner, new JSONObject().put("surface", "virtual").put("includeTree", true)
					.put("action", new JSONObject().put("id", item.getString("id")).put("targetPath", "configs").put("position", "inside")));
			assertTrue(created.toString(), created.getBoolean("ok"));
			assertEquals("configs.configuration", created.getString("selectionVirtualPath"));
			assertNotNull(find(created, "configs.configuration"));
			assertTrue(model.owner.isEngineSourceDirty());
			assertEquals(SAVED, Files.readString(model.project.getDirFile().toPath().resolve("_flow/engine.yaml")));
			var before = model.owner.getEngineSource();
			var invalid = provider.applyMutation(model.owner,
					new JSONObject().put("op", "replace").put("path", "configs.B1").put("value", false), false);
			assertFalse(invalid.getBoolean("ok"));
			assertEquals("FLOW_CONFIG_DEFINITION_OBJECT_REQUIRED", invalid.getJSONObject("error").getString("code"));
			assertEquals(before, model.owner.getEngineSource());
		} finally { Flow.projectUnloaded(model.project); }
	}
}
