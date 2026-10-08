/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;

/** Saving the drafts a virtual tree was projected from keeps that tree, instead of projecting it again. */
public class FlowEngineSaveProjectionTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private FlowEngine engine(File directory) {
		var project = new Project() {
			@Override
			public File getDirFile() {
				return directory;
			}
		};
		return new FlowEngine() {
			@Override
			public Project getProject() {
				return project;
			}
		};
	}

	private static FlowVirtualObject builder(FlowEngine engine) {
		return new FlowVirtualObject(engine, "svelte", "frontendBuilder", "svelte", "frontends.svelte", "Svelte builder",
				"{\"modelVersion\":\"2\",\"dirty\":true}");
	}

	@Test
	public void theTreeProjectedFromTheSavedDraftsIsKept() throws Exception {
		var directory = folder.newFolder();
		var engine = engine(directory);
		var page = new File(directory, "_flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte");
		engine.setSource(page.getPath(), "<FlowComponent />");
		var builder = builder(engine);
		engine.restoreFlowVirtualChildrenCache(List.<DatabaseObject> of(builder));

		engine.saveSources();
		assertEquals("<FlowComponent />", Files.readString(page.toPath()));
		var kept = engine.snapshotFlowVirtualChildrenCache();
		assertSame("the projected tree is kept", builder, kept.get(0));
		assertFalse("its model is no longer unsaved", new JSONObject(builder.getDefinition()).getBoolean("dirty"));
		assertEquals("2", new JSONObject(builder.getDefinition()).getString("modelVersion"));
	}

	@Test
	public void aTreeProjectedFromOtherDraftsIsNotKept() throws Exception {
		var directory = folder.newFolder();
		var engine = engine(directory);
		var page = new File(directory, "_flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte");
		engine.setSource(page.getPath(), "<FlowComponent />");
		engine.restoreFlowVirtualChildrenCache(List.<DatabaseObject> of(builder(engine)));
		// a draft changed after the projection, without projecting again
		engine.setSource(new File(directory, "_flow/other.json").getPath(), "{}");

		engine.saveSources();
		assertNull("the tree is projected again", engine.snapshotFlowVirtualChildrenCache());
	}
}
