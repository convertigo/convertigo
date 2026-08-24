/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class DboUtilsTest {
	@Test
	public void copiesFlowMutationMetadataWithoutDroppingSelection() throws Exception {
		var source = new JSONObject()
				.put("done", true)
				.put("id", "Project.Engine.frontends.svelte.routes.home.structure.text")
				.put("parentId", "Project.Engine.frontends.svelte.routes.home.structure")
				.put("projected", true)
				.put("selectionSourcePath", "model/Project/src/routes/+page.flow.svelte")
				.put("selectionMutationPath", "frontAst.nodes[2]")
				.put("selectionId", "text");
		var target = new JSONObject();

		DboUtils.copyResult(source, target);

		assertTrue(target.getBoolean("done"));
		assertEquals(source.length(), target.length());
		for (var keys = source.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			assertEquals(source.get(key), target.get(key));
		}
	}
}
