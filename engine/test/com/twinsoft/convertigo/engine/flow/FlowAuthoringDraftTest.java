package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.file.Files;

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
	public void reloadDiscardsUnsavedDraftAndExportPersistsIt() throws Exception {
		var directory = folder.newFolder();
		var file = directory.toPath().resolve("libs/flow/engine.yaml");
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
