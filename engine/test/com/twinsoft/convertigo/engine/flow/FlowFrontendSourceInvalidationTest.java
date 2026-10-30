/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * A page an agent writes through the Flow MCP is read by its content (provider documents, catalogs, trees) and by no
 * Flow runtime: its write drops the authoring answers of its project, not the data revision of the project. Bumped at
 * each page, the revision refreshed every pooled runtime and the block catalog of the project, and the Flows of the
 * project checked or promoted meanwhile started cold. A block, a type or a Flow still changes the data revision.
 */
public class FlowFrontendSourceInvalidationTest {
	private File project;

	@Before
	public void setUp() throws Exception {
		Engine.logEngine = Engine.logBeans = org.apache.log4j.Logger.getLogger("frontend-source-invalidation-test");
		EnginePropertiesManager.initProperties();
		project = Files.createTempDirectory("flow-frontend-invalidation").toFile();
	}

	@After
	public void tearDown() throws Exception {
		org.apache.commons.io.FileUtils.deleteDirectory(project);
	}

	@Test
	public void aWrittenPageKeepsTheDataRevisionOfItsProject() {
		var generation = FlowEngineBridge.dataGeneration();
		var revision = FlowEngineBridge.metadataSourceRevision(project.getPath());
		for (var page : new String[] {
				"_flow/frontbuilder/svelte/model/App/src/routes/+page.flow.svelte",
				new File(project, "_flow/frontbuilder/svelte/model/App/src/routes/items/+page.flow.svelte").getPath(),
				"_flow/frontbuilder/svelte/model/App/src/app.flow.css",
				"_flow/frontbuilder/svelte/components/Badge.flow.svelte" }) {
			FlowEngineBridge.notifySourceMutation(project.getPath(), page, true);
		}
		assertEquals("a page refreshed the pooled runtimes", generation, FlowEngineBridge.dataGeneration());
		assertEquals("a page changed the data revision of its project", revision,
				FlowEngineBridge.metadataSourceRevision(project.getPath()));
	}

	@Test
	public void aWrittenBlockStillChangesTheDataRevisionOfItsProject() {
		var generation = FlowEngineBridge.dataGeneration();
		var revision = FlowEngineBridge.metadataSourceRevision(project.getPath());
		FlowEngineBridge.notifySourceMutation(project.getPath(), "_flow/blocks/items/list.block.js", false);
		assertNotEquals(generation, FlowEngineBridge.dataGeneration());
		assertNotEquals(revision, FlowEngineBridge.metadataSourceRevision(project.getPath()));
	}
}
