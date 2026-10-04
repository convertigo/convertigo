package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.tags.TagDocument;
import com.twinsoft.convertigo.engine.tags.TagManager;
import com.twinsoft.convertigo.engine.tags.TagManager.Scope;

/** The MCP and Studio host boundary captures the same working context, not caller-supplied tags. */
public class FlowProjectRequestContextTest {

	@Rule public TemporaryFolder folder = new TemporaryFolder();

	private final class Fixture implements AutoCloseable {
		final Engine previousEngine = Engine.theApp;
		final File directory = folder.newFolder("Proof");
		final Project project;
		final FlowEngine owner;
		final Flow flow;
		final TagManager manager;
		final java.lang.reflect.Field managerField, managerOwner;
		final Object previousManager, previousManagerOwner;
		final String first, second;
		int projectLookups;

		Fixture() throws Exception {
			Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("flow-context-test");
			EnginePropertiesManager.initProperties();
			project = new Project() {
				@Override public File getDirFile() { return directory; }
				@Override public String getDirPath() { return directory.getAbsolutePath(); }
				@Override public FlowEngine getFlowEngine() { return owner; }
				@Override public List<Sequence> getSequencesList() { return List.of(flow); }
			};
			project.setName("Proof");
			owner = new FlowEngine() { @Override public Project getProject() { return project; } };
			owner.setName("FlowEngine");
			flow = new Flow() { @Override public Project getProject() { return project; } };
			flow.setName("Sample"); flow.setParent(project);
			write("_flow/engine.yaml", "{\"version\":1,\"configs\":{\"B1\":{\"api\":{\"host\":\"first\"}},\"B2\":{\"api\":{\"host\":\"second\"}}}}");
			write("_flow/flows/Sample.flow.js", "function Sample({ config }) { return config; }\n");
			write("_flow/removed.flow.js", "function removed() { return true; }\n");
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override public Project getLoadedProjectByName(String name) { return "Proof".equals(name) ? project : null; }
				@Override public List<String> getAllProjectNamesList(boolean checkOpenable) { return List.of("Proof"); }
				@Override public Project getOriginalProjectByName(String name, boolean checkOpenable) {
					projectLookups++;
					assertEquals("Proof", name); return project;
				}
			};
			manager = new TagManager(folder.getRoot().toPath(), name -> "Proof".equals(name) ? project : null,
					p -> directory.toPath().resolve("_c8oProject/tags.json"), () -> List.of("Proof"));
			manager.contributions().register("flow", TagDocument.parseObject("""
					{"label":"Flow","fields":{"configs":{"label":"Configurations","type":"array","uniqueItems":true,
					"items":{"type":"string","enum":["B1","B2"]}}}}
					"""));
			manager.projectOpened(project);
			managerField = TagManager.class.getDeclaredField("instance"); managerField.setAccessible(true);
			managerOwner = TagManager.class.getDeclaredField("owner"); managerOwner.setAccessible(true);
			previousManager = managerField.get(null); previousManagerOwner = managerOwner.get(null);
			managerField.set(null, manager); managerOwner.set(null, Engine.theApp);
			first = create("B1"); second = create("B2");
			mutate("assign", "{\"targets\":[\"Proof.sq:Sample\"],\"tagIds\":[\"" + first + "\",\"" + second + "\"]}");
		}

		void write(String path, String content) throws Exception {
			var file = directory.toPath().resolve(path); Files.createDirectories(file.getParent()); Files.writeString(file, content);
		}

		String create(String name) throws Exception {
			return mutate("create", "{\"definition\":{\"label\":\"" + name + "\",\"metadata\":{\"flow\":{\"configs\":[\"" + name + "\"]}}}}")
					.path("id").asText();
		}

		com.fasterxml.jackson.databind.node.ObjectNode mutate(String action, String input) throws Exception {
			return manager.mutate(Scope.projectObjects, "Proof", manager.read(Scope.projectObjects, "Proof").path("revision").asText(),
					action, TagDocument.parseObject(input));
		}

		JSONObject request() throws Exception {
			return new JSONObject().put("project", "Proof").put("projectDir", directory.getCanonicalPath()).put("flowQName", "Proof.Sample");
		}

		@Override public void close() throws Exception {
			Flow.projectUnloaded(project);
			Engine.theApp = previousEngine; managerField.set(null, previousManager); managerOwner.set(null, previousManagerOwner);
		}
	}

	@Test public void capturesOrderedTagAndSourceDraftsWithoutImportingOrPersisting() throws Exception {
		try (var fixture = new Fixture()) {
			var source = new File(fixture.directory, "_flow/engine.yaml").getCanonicalPath();
			var removal = new File(fixture.directory, "_flow/removed.flow.js").getCanonicalPath();
			fixture.owner.setEngineSource("{\"version\":1,\"configs\":{}}");
			fixture.owner.applySourceChanges(java.util.Map.of(), List.of(removal));
			var request = fixture.request().put("tagContext", new JSONObject().put("project", "Forged")).put("includeTrace", true);
			assertSame(request, FlowEngineBridge.prepareProjectRequest(request));
			assertEquals("Proof", request.getJSONObject("tagContext").getString("project"));
			var memberships = request.getJSONObject("tagContext").getJSONObject("assignments").getJSONArray("Proof.sq:Sample");
			assertEquals(fixture.first, memberships.getString(0)); assertEquals(fixture.second, memberships.getString(1));
			assertEquals("Proof.sq:Sample", request.getJSONObject("tagContext").getJSONObject("aliases").getString("Proof.Sample"));
			assertEquals(fixture.owner.getEngineSource(), request.getJSONObject("frontendSourceDrafts").getString(source));
			fixture.flow.setFlowSource("function Sample() { return { draft: true }; }\n");
			var flowDraft = FlowEngineBridge.prepareProjectRequest(fixture.request());
			assertEquals(fixture.flow.getFlowSource(), flowDraft.getJSONObject("frontendSourceDrafts").getString(fixture.flow.getFlowSourceFile().getCanonicalPath()));
			assertEquals(removal, request.getJSONArray("sourceRemovals").getString(0));
			assertTrue(request.getBoolean("includeTrace"));
			fixture.mutate("reorder", "{\"targets\":[\"Proof.sq:Sample\"],\"tagIds\":[\"" + fixture.second + "\",\"" + fixture.first + "\"]}");
			var next = FlowEngineBridge.prepareProjectRequest(fixture.request());
			assertEquals(fixture.second, next.getJSONObject("tagContext").getJSONObject("assignments").getJSONArray("Proof.sq:Sample").getString(0));
			assertEquals(fixture.first, memberships.getString(0));
			assertEquals(0, fixture.projectLookups);
			assertFalse(Files.exists(fixture.directory.toPath().resolve("_c8oProject/tags.json")));
		}
	}

	@Test public void mismatchedOrUnloadedOwnersCannotReceiveAnotherProjectContext() throws Exception {
		try (var fixture = new Fixture()) {
			for (var request : List.of(fixture.request().put("projectDir", folder.newFolder("Other").getCanonicalPath()),
					new JSONObject().put("project", "Unloaded").put("tagContext", new JSONObject()))) {
				FlowEngineBridge.prepareProjectRequest(request);
				assertFalse(request.has("tagContext")); assertFalse(request.has("frontendSourceDrafts"));
			}
		}
	}

	@Test public void standaloneCallsDoNotAcceptForgedTagContext() throws Exception {
		var previous = Engine.theApp;
		try {
			Engine.theApp = null;
			var request = new JSONObject().put("project", "Proof").put("tagContext", new JSONObject());
			FlowEngineBridge.prepareProjectRequest(request);
			assertFalse(request.has("tagContext")); assertEquals("Proof", request.getString("project"));
		} finally { Engine.theApp = previous; }
	}

	@Test public void contradictoryFileAndBeanDraftsAreRefusedWithoutPublication() throws Exception {
		try (var fixture = new Fixture()) {
			fixture.owner.setEngineSource("{\"version\":1,\"config\":{\"value\":\"bean\"}}");
			fixture.owner.setSource(new File(fixture.directory, "_flow/engine.yaml").getCanonicalPath(),
					"{\"version\":1,\"config\":{\"value\":\"file\"}}");
			var error = assertThrows(com.twinsoft.convertigo.engine.EngineException.class,
					() -> FlowEngineBridge.prepareProjectRequest(fixture.request()));
			assertTrue(error.getCause().getMessage().contains("Conflicting Flow working copies"));
			assertFalse(Files.readString(fixture.directory.toPath().resolve("_flow/engine.yaml")).contains("bean"));
			var reference = new com.twinsoft.convertigo.beans.references.ProjectSchemaReference();
			reference.setProjectName("Proof");
			var consumer = new Project() {
				@Override public List<com.twinsoft.convertigo.beans.core.Reference> getReferenceList() { return List.of(reference); }
				@Override public List<Sequence> getSequencesList() { return List.of(); }
				@Override public FlowEngine getFlowEngine() { return null; }
			};
			consumer.setName("Consumer");
			var consumerOwner = new FlowEngine() { @Override public Project getProject() { return consumer; } };
			var referencedError = assertThrows(com.twinsoft.convertigo.engine.EngineException.class,
					() -> FlowEngineBridge.sourceWorkingCopies(consumerOwner));
			assertTrue(referencedError.getMessage().contains("Conflicting Flow working copies"));
			assertFalse(Files.readString(fixture.directory.toPath().resolve("_flow/engine.yaml")).contains("bean"));
		}
	}

	@Test public void actualMcpPreparationAndRhinoExecutionUseTheHostTagDraft() throws Exception {
		String engineRoot = System.getenv("FLOW_ENGINE_RESOURCE_ROOT"), mcpRoot = System.getenv("FLOW_MCP_RESOURCE_ROOT");
		Assume.assumeTrue("Set both Flow resource roots for the real MCP preparation contract", engineRoot != null && mcpRoot != null);
		try (var fixture = new Fixture()) {
			var cx = org.mozilla.javascript.Context.enter();
			try {
				cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
				var scope = cx.initStandardObjects();
				var engineFile = Path.of(engineRoot, "Engine.js");
				FlowEngineBridge.initializeSourceScope(scope, engineFile.toFile());
				ScriptableObject.putProperty(scope, "__flowProjectDir", fixture.directory.getAbsolutePath());
				ScriptableObject.putProperty(scope, "engine", cx.evaluateString(scope, Files.readString(engineFile), engineFile.toString(), 1, null));
				var mcpFile = Path.of(mcpRoot, "lib/mcp.js");
				ScriptableObject.putProperty(scope, "mcp", cx.evaluateString(scope, Files.readString(mcpFile), mcpFile.toString(), 1, null));
				ScriptableObject.putProperty(scope, "requestJson", new JSONObject().put("params", new JSONObject().put("name", "code-run")
						.put("arguments", fixture.request().put("qname", "Proof.Sample").put("tagContext", new JSONObject().put("project", "Forged")))).toString());
				var script = "var args = mcp.prepareToolArguments(null, JSON.parse(requestJson), {resolveProject:false}); engine.flowCodeRun(JSON.stringify(args));";
				var result = new JSONObject(org.mozilla.javascript.Context.toString(cx.evaluateString(scope, script, "mcp-context-proof", 1, null)));
				assertTrue(result.toString(), result.getBoolean("ok"));
				assertEquals("second", result.getJSONObject("result").getJSONObject("api").getString("host"));
				fixture.mutate("reorder", "{\"targets\":[\"Proof.sq:Sample\"],\"tagIds\":[\"" + fixture.second + "\",\"" + fixture.first + "\"]}");
				result = new JSONObject(org.mozilla.javascript.Context.toString(cx.evaluateString(scope, script, "mcp-context-reordered", 1, null)));
				assertTrue(result.toString(), result.getBoolean("ok"));
				assertEquals("first", result.getJSONObject("result").getJSONObject("api").getString("host"));
				fixture.owner.setEngineSource("{\"version\":1,\"configs\":{\"B1\":{\"api\":{\"host\":\"draft\"}},\"B2\":{\"api\":{\"host\":\"second\"}}}}");
				result = new JSONObject(org.mozilla.javascript.Context.toString(cx.evaluateString(scope, script, "mcp-context-config-draft", 1, null)));
				assertTrue(result.toString(), result.getBoolean("ok"));
				assertEquals("draft", result.getJSONObject("result").getJSONObject("api").getString("host"));
				assertFalse(Files.readString(fixture.directory.toPath().resolve("_flow/engine.yaml")).contains("draft"));
				assertFalse(Files.exists(fixture.directory.toPath().resolve("_c8oProject/tags.json")));
			} finally { org.mozilla.javascript.Context.exit(); }
		}
	}
}
