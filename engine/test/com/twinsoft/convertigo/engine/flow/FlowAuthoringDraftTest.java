package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;

public class FlowAuthoringDraftTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private FlowEngine model(File directory) {
		var project = new Project() {
			@Override
			public File getDirFile() { return directory; }
		};
		return new FlowEngine() {
			@Override
			public Project getProject() { return project; }
		};
	}

	@Test
	public void providerSourceBatchUsesTheOwnerDraftLifecycle() throws Exception {
		var directory = folder.newFolder();
		var current = model(directory);
		var page = directory.toPath().resolve("_flow/new/+page.flow.svelte");
		var marker = directory.toPath().resolve("_flow/new/group/.flow-route.json");
		var provider = new Provider();
		provider.response = new JSONObject().put("ok", true).put("target", "sources")
				.put("sourceChanges", new JSONObject().put(page.toString(), "page").put(marker.toString(), ""));
		provider.authoringMutate(current, new JSONObject().put("dryRun", true));
		assertTrue(current.getSourceDrafts().isEmpty());
		provider.response.put("ok", false);
		provider.authoringMutate(current, new JSONObject());
		assertTrue(current.getSourceDrafts().isEmpty());
		provider.response.put("ok", true);
		provider.authoringMutate(current, new JSONObject().put("write", true));
		assertFalse(provider.request.getBoolean("write"));
		assertEquals("page", current.getSource(page.toString()));
		assertEquals("", current.getSource(marker.toString()));
		assertFalse(Files.exists(page.getParent()));
		current.toXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
		assertEquals("page", Files.readString(page));
		assertEquals("", Files.readString(marker));
		assertTrue(current.getSourceDrafts().isEmpty());
	}

	@Test
	public void invalidProviderBatchPreservesEveryExistingDraft() throws Exception {
		var directory = folder.newFolder();
		var current = model(directory);
		current.setEngineSource("original engine draft");
		var file = directory.toPath().resolve("_flow/data.json");
		current.setSource(file.toString(), "original file draft");
		var provider = new Provider();
		var changes = new JSONObject().put(file.toString(), "changed")
				.put(directory.toPath().resolve("../outside.json").toString(), "invalid");
		provider.response = new JSONObject().put("ok", true).put("target", "engine").put("source", "changed engine")
				.put("sourceChanges", changes);
		assertThrows(EngineException.class, () -> provider.authoringMutate(current, new JSONObject()));
		assertEquals("original engine draft", current.getEngineSource());
		assertEquals("original file draft", current.getSource(file.toString()));
		provider.response.put("sourceChanges", new JSONObject().put(file.toString(), JSONObject.NULL));
		assertThrows(EngineException.class, () -> provider.authoringMutate(current, new JSONObject()));
		assertEquals("original file draft", current.getSource(file.toString()));
		Flow.projectUnloaded(current.getProject());
	}

	@Test
	public void newSourcesIncludingEmptyFilesExistOnlyInTheWorkingCopyUntilSave() throws Exception {
		var directory = folder.newFolder();
		var current = model(directory);
		var page = directory.toPath().resolve("_flow/frontbuilder/svelte/model/Proof/src/routes/new/+page.flow.svelte");
		var empty = directory.toPath().resolve("_flow/frontbuilder/svelte/model/Proof/src/app.flow.css");
		var sources = Map.of(page.toString(), "new page", empty.toString(), "");
		current.setSources(sources);
		assertTrue(current.hasSource(empty.toString()));
		assertTrue(current.isSourceDirty(empty.toString()));
		assertEquals("", current.getSource(empty.toString()));
		assertFalse(Files.exists(page.getParent()));
		assertFalse(Files.exists(empty));
		Flow.projectUnloaded(current.getProject());
		assertFalse(current.hasSource(page.toString()));
		assertFalse(current.hasSource(empty.toString()));
		current.setSources(sources);
		current.toXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
		assertEquals("new page", Files.readString(page));
		assertEquals("", Files.readString(empty));
		assertTrue(current.getSourceDrafts().isEmpty());
		assertEquals("new page", model(directory).getSource(page.toString()));
	}

	@Test
	public void invalidBatchDoesNotPartiallyChangeWorkingCopies() throws Exception {
		var directory = folder.newFolder();
		var current = model(directory);
		var source = directory.toPath().resolve("value.json").toString();
		current.setSource(source, "original draft");
		var changes = new LinkedHashMap<String, String>();
		changes.put(source, "must not apply");
		changes.put(directory.toPath().resolve("../outside.json").toString(), "invalid");
		assertThrows(EngineException.class, () -> current.setSources(changes));
		assertEquals("original draft", current.getSource(source));
		assertEquals(1, current.getSourceDrafts().size());
		changes.clear();
		changes.put(directory.toPath().resolve("new.json").toString(), "parent file");
		changes.put(directory.toPath().resolve("new.json/child.json").toString(), "child");
		assertThrows(EngineException.class, () -> current.setSources(changes));
		assertEquals(1, current.getSourceDrafts().size());
		changes.clear();
		changes.put(source, "one spelling");
		changes.put(directory.toPath().resolve("./value.json").toString(), "other spelling");
		assertThrows(EngineException.class, () -> current.setSources(changes));
		assertEquals("original draft", current.getSource(source));
		var child = directory.toPath().resolve("directory.json/child.json");
		current.setSource(child.toString(), "child draft");
		assertThrows(EngineException.class, () -> current.setSource(child.getParent().toString(), "parent file"));
		assertThrows(EngineException.class, () -> current.setSource(source + "/child.json", "child of draft file"));
		assertEquals("child draft", current.getSource(child.toString()));
		Flow.projectUnloaded(current.getProject());
	}

	@Test
	public void aNewSourceDraftCanBeEditedBeforeItsFirstSave() throws Exception {
		var directory = folder.newFolder();
		var file = directory.toPath().resolve("new.flow.svelte");
		var current = model(directory);
		current.setSource(file.toString(), "created draft");
		var provider = new Provider();
		provider.method = "applySourceMutation";
		provider.response = new JSONObject().put("ok", true).put("source", "edited draft");
		provider.applySourceMutation(current, file.toString(), new JSONObject());
		assertEquals("created draft", provider.request.getString("source"));
		assertEquals("edited draft", current.getSource(file.toString()));
		assertFalse(Files.exists(file));
		Flow.projectUnloaded(current.getProject());
	}

	@Test
	public void reloadDiscardsUnsavedDraftAndExportPersistsIt() throws Exception {
		var directory = folder.newFolder();
		var file = directory.toPath().resolve("_flow/engine.yaml");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "original");
		var current = model(directory);
		var provider = new Provider();
		provider.response = new JSONObject().put("ok", true).put("target", "engine").put("source", "draft");
		provider.authoringMutate(current, new JSONObject());
		assertEquals("draft", current.getEngineSource());
		assertEquals("original", Files.readString(file));
		assertEquals("original", model(directory).getEngineSource());
		current.toXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
		assertEquals("draft", Files.readString(file));
		assertEquals("draft", model(directory).getEngineSource());
	}
	private static class Provider extends FlowEngineBridge {
		String method = "authoringMutate";
		JSONObject request;
		JSONObject response;

		@Override
		JSONObject invoke(String engineQName, String method, JSONObject request, Context context,
				org.mozilla.javascript.Context javascriptContext, Scriptable scope) {
			assertEquals(this.method, method);
			this.request = request;
			return response;
		}
	}

	@Test
	public void frontendClipboardUsesDraftAndOnlyExportPersistsSource() throws Exception {
		var directory = folder.newFolder();
		var file = directory.toPath().resolve("page.flow.svelte");
		Files.writeString(file, "original");
		var current = model(directory);
		current.setFrontendSource(file.toString(), "effective draft");
		var provider = new Provider();
		provider.method = "applySourceMutation";
		provider.response = new JSONObject().put("ok", true).put("source", "pasted draft");
		provider.applySourceMutation(current, file.toString(), new JSONObject().put("op", "paste"));
		assertEquals("effective draft", provider.request.getString("source"));
		assertEquals("pasted draft", current.getFrontendSource(file.toString()));
		assertEquals("original", Files.readString(file));
		Flow.projectUnloaded(current.getProject());
		assertEquals("original", model(directory).getFrontendSource(file.toString()));
		current.setFrontendSource(file.toString(), "pasted draft");
		current.toXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
		assertEquals("pasted draft", Files.readString(file));
		assertEquals("pasted draft", model(directory).getFrontendSource(file.toString()));
	}

	@Test
	public void backendAuthoringMutationAdvancesDraftOnly() throws Exception {
		var flow = new Flow();
		flow.setFlowSource("original");
		var provider = new Provider();
		provider.response = new JSONObject().put("ok", true).put("source", "draft");
		provider.authoringMutate(flow, new JSONObject().put("write", true));
		assertEquals("flow", provider.request.getString("target"));
		assertFalse(provider.request.getBoolean("write"));
		assertFalse(provider.request.getBoolean("persist"));
		assertEquals("original", provider.request.getString("flowSource"));
		assertEquals("draft", flow.getFlowSource());
		provider.response.put("source", "preview");
		provider.authoringMutate(flow, new JSONObject().put("dryRun", true));
		assertEquals("draft", flow.getFlowSource());
		provider.response.put("ok", false);
		provider.authoringMutate(flow, new JSONObject());
		assertEquals("draft", flow.getFlowSource());
	}

	@Test
	public void targetedProjectionStillAdvancesTheCompleteDraft() throws Exception {
		var model = new FlowEngine();
		model.setEngineSource("complete original");
		var provider = new Provider();
		provider.method = "applyMutation";
		provider.response = new JSONObject().put("ok", true).put("source", "complete draft");
		provider.applyMutation(model, new JSONObject(), true, "config.service.value");
		assertEquals("config.service.value", provider.request.getJSONArray("projectionPaths").getString(0));
		assertEquals("complete original", provider.request.getString("engineSource"));
		assertEquals("complete draft", model.getEngineSource());
		provider.applyMutation(model, new JSONObject(), true);
		assertFalse(provider.request.has("projectionPaths"));
	}

	@Test
	public void loadedMutationsAdvanceDraftWithoutAllowingProviderDiskWrites() throws Exception {
		var model = new FlowEngine();
		model.setEngineSource("original");
		var provider = new Provider();
		provider.response = new JSONObject().put("ok", true).put("target", "engine").put("source", "draft1");
		provider.authoringMutate(model, new JSONObject().put("write", true).put("persist", true));
		assertFalse(provider.request.getBoolean("write"));
		assertFalse(provider.request.getBoolean("persist"));
		assertEquals("original", provider.request.getString("engineSource"));
		assertEquals("draft1", model.getEngineSource());
		provider.response.put("source", "draft2");
		provider.authoringMutate(model, new JSONObject());
		assertEquals("draft1", provider.request.getString("engineSource"));
		assertEquals("draft2", model.getEngineSource());
	}

	@Test
	public void previewAndFailureDoNotReplaceDraft() throws Exception {
		var model = new FlowEngine();
		model.setEngineSource("draft");
		var provider = new Provider();
		provider.response = new JSONObject().put("ok", true).put("target", "engine").put("source", "preview");
		provider.authoringMutate(model, new JSONObject().put("dryRun", true));
		assertEquals("draft", model.getEngineSource());
		assertFalse(provider.request.getBoolean("write"));
		provider.response.put("ok", false);
		provider.authoringMutate(model, new JSONObject());
		assertEquals("draft", model.getEngineSource());
	}
}
