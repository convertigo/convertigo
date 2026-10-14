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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * Asking the state of a plain file of the project (a .md guide, a .java source) answers instead of refusing: it has no
 * working copy, so no unsaved change, and exists when the disk says so. A Flow source keeps its working copy.
 */
public class FlowEngineSourceStateTest {
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

	@Test
	public void aPlainFileHasNoWorkingCopyAndExistsOnDisk() throws Exception {
		var directory = folder.newFolder();
		var engine = engine(directory);
		var guide = new File(directory, "_flow/resources/guide/authoring.md");
		assertFalse("a plain file is never dirty", engine.isSourceDirty(guide.getPath()));
		assertFalse("a missing plain file does not exist", engine.hasSource(guide.getPath()));
		guide.getParentFile().mkdirs();
		Files.writeString(guide.toPath(), "# Guide");
		assertTrue("the disk tells whether a plain file exists", engine.hasSource(guide.getPath()));
		assertFalse(engine.isSourceDirty(new File(directory, "libs/src/com/acme/Rows.java").getPath()));
		try {
			engine.getSource(guide.getPath());
			fail("a plain file is not read as a Flow source");
		} catch (EngineException expected) {
		}
	}

	@Test
	public void aFlowSourceKeepsItsWorkingCopy() throws Exception {
		var directory = folder.newFolder();
		var engine = engine(directory);
		var page = new File(directory, "_flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte");
		engine.setSource(page.getPath(), "<FlowComponent />");
		assertTrue("a Flow source has its working copy", engine.isSourceDirty(page.getPath()));
		assertTrue(engine.hasSource(page.getPath()));
	}

	@Test
	public void theLastDraftTakesTheChangeFlagItRaised() throws Exception {
		var directory = folder.newFolder();
		var engine = engine(directory);
		var flow = new File(directory, "_flow/flows/Orders.flow.js");
		var page = new File(directory, "_flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte");
		engine.setSource(flow.getPath(), "function Orders() {}");
		engine.setSource(page.getPath(), "<FlowComponent />");
		assertTrue("a draft marks the engine", engine.hasChanged);
		engine.applySourceChanges(java.util.Map.of(), java.util.List.of(flow.getPath()));
		assertTrue("another draft remains", engine.hasChanged);
		engine.discardSource(page.getPath());
		assertFalse("the last draft gone, nothing remains to save", engine.hasChanged);

		engine.setSource(flow.getPath(), "function Orders() {}");
		engine.discardSource(flow.getPath());
		assertFalse("a discarded draft takes its flag too", engine.hasChanged);
	}

	@Test
	public void anotherChangeKeepsTheFlagAfterTheDrafts() throws Exception {
		var directory = folder.newFolder();
		var engine = engine(directory);
		var flow = new File(directory, "_flow/flows/Orders.flow.js");
		engine.setSource(flow.getPath(), "function Orders() {}");
		engine.changed();
		engine.discardSource(flow.getPath());
		assertTrue("a change besides the drafts is still to save", engine.hasChanged);

		engine.hasChanged = false;
		engine.changed();
		engine.setSource(flow.getPath(), "function Orders() {}");
		engine.discardSource(flow.getPath());
		assertTrue("a change before the drafts is still to save", engine.hasChanged);
	}
}
