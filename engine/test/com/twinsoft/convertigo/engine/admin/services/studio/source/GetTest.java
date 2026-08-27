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
import static org.junit.Assert.assertThrows;

import java.nio.file.Files;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
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
				.put("sourceRelativePath", "libs/flow/frontbuilder/svelte/components/DatePicker.flow.svelte")
				.toString());

		var document = Get.sourceDocument(object);

		assertEquals(source.toFile().getCanonicalFile(), document.file());
		assertEquals("libs/flow/frontbuilder/svelte/components/DatePicker.flow.svelte", document.relativePath());
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
}
