/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;

public class GetTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void readsTheSourceResolvedByTheVirtualObject() throws Exception {
		var source = temporaryFolder.getRoot().toPath().resolve("DatePicker.flow.svelte");
		Files.writeString(source, "<script>const label = 'Date';</script>\n");
		var object = new FlowVirtualObject();
		object.setVirtualInfo(new JSONObject()
				.put("sourcePath", source.toString())
				.put("sourceRelativePath", "_flow/frontbuilder/svelte/components/DatePicker.flow.svelte")
				.toString());

		var document = Get.sourceDocument(object);

		assertEquals(source.toFile().getCanonicalFile(), document.file());
		assertEquals("_flow/frontbuilder/svelte/components/DatePicker.flow.svelte", document.relativePath());
		assertEquals("<script>const label = 'Date';</script>\n", document.content());
		assertEquals(64, document.revision().length());
	}

	@Test
	public void rejectsObjectsAndFilesThatAreNotFlowCodeSources() throws Exception {
		assertThrows(ServiceException.class, () -> Get.sourceDocument(new Project()));

		var text = temporaryFolder.getRoot().toPath().resolve("notes.txt");
		Files.writeString(text, "not a Flow code source");
		var object = new FlowVirtualObject();
		object.setVirtualInfo(new JSONObject().put("sourcePath", text.toString()).toString());

		assertThrows(ServiceException.class, () -> Get.sourceDocument(object));
	}

	@Test
	public void removedWritableSourceDoesNotFallBackToItsSavedContents() throws Exception {
		var directory = temporaryFolder.getRoot();
		var source = directory.toPath().resolve("source.block.js");
		Files.writeString(source, "saved source");
		var holder = new FlowEngine[1];
		var project = new Project() {
			@Override public java.io.File getDirFile() { return directory; }
			@Override public FlowEngine getFlowEngine() { return holder[0]; }
		};
		var owner = new FlowEngine() { @Override public Project getProject() { return project; } };
		holder[0] = owner;
		var object = new FlowVirtualObject() { @Override public Project getProject() { return project; } };
		object.setVirtualInfo(new JSONObject().put("sourcePath", source.toString()).put("sourceWritable", true).toString());
		assertEquals("saved source", Get.sourceDocument(object).content());
		owner.applySourceChanges(java.util.Map.of(), java.util.List.of(source.toString()));
		assertThrows(ServiceException.class, () -> Get.sourceDocument(object));
		assertEquals("saved source", Files.readString(source));
		Flow.projectUnloaded(project);
	}

	@Test
	public void theSourceOfAFlowIsItsFlowScriptWhoseWorkingCopyWins() throws Exception {
		var directory = temporaryFolder.getRoot();
		var file = directory.toPath().resolve("_flow/flows/Seed.flow.js");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "saved flow");
		var project = new Project() { @Override public java.io.File getDirFile() { return directory; } };
		var flow = new Flow() { @Override public Project getProject() { return project; } };
		flow.setName("Seed");
		assertTrue(Get.hasSourceDocument(flow));
		var document = Get.sourceDocument(flow);
		assertEquals("_flow/flows/Seed.flow.js", document.relativePath());
		assertEquals("saved flow", document.content());
		assertTrue(document.writable());
		flow.setFlowSource("edited flow");
		assertEquals("the working copy until the project Save", "edited flow", Get.sourceDocument(flow).content());
		assertEquals("saved flow", Files.readString(file));
		Flow.projectUnloaded(project);
	}

	@Test
	public void aBlockAndItsCodeImplementationHaveASourceButNotTheItemsOfAFlow() throws Exception {
		var source = temporaryFolder.getRoot().toPath().resolve("attachments.block.js");
		Files.writeString(source, "const _meta = {};\n");
		var info = new JSONObject().put("sourcePath", source.toString()).toString();
		var block = new FlowVirtualObject();
		block.setVirtualKind("block");
		block.setVirtualInfo(info);
		var implementation = new FlowVirtualObject();
		implementation.setVirtualKind("blockImplementation");
		implementation.setVirtualType("rhino");
		implementation.setVirtualInfo(info);
		var item = new FlowVirtualObject();
		item.setVirtualKind("node");
		item.setVirtualInfo(info);

		assertTrue(Get.hasSourceDocument(block));
		assertTrue(Get.hasSourceDocument(implementation));
		assertFalse(Get.hasSourceDocument(item));
		assertFalse(Get.hasSourceDocument(new Project()));
	}

	@Test
	public void theDiagnosticsOfAFlowScriptGetTheLinesOfItsFile() throws Exception {
		var content = "// c8o: header\nconst _flow = {\n  sourceVersion: 2,\n}\n\n// above\nfunction Seed({ input }) {\n  local.a = nope.missing({})\n}\n";
		var diagnostics = new JSONArray()
				.put(new JSONObject().put("line", 2).put("code", "UNKNOWN_BLOCK"))
				.put(new JSONObject().put("code", "NO_LINE"));
		Get.fileLines(diagnostics, content);
		assertEquals(8, diagnostics.getJSONObject(0).getInt("line"));
		assertEquals(2, diagnostics.getJSONObject(0).getInt("flowLine"));
		assertFalse(diagnostics.getJSONObject(1).has("line"));
	}
}
