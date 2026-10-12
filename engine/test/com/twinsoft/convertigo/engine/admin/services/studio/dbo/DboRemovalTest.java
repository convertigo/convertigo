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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;

/**
 * The deletion the web Studio and the Flow MCP share: what it removes too, then the object; a removed Flow
 * takes its source and its learned schemas with it at the save of the project, not before.
 */
public class DboRemovalTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Engine previousEngine;

	@BeforeClass
	public static void initializeLogging() {
		Engine.logBeans = Logger.getLogger("dbo-removal-test");
		Engine.logEngine = Engine.logStudio = Engine.logAdmin = Engine.logDatabaseObjectManager = Engine.logBeans;
	}

	@Before
	public void installEngine() throws Exception {
		EnginePropertiesManager.initProperties();
		previousEngine = Engine.theApp;
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override
			public Object getCompiledValue(Object propertyObjectValue) {
				return propertyObjectValue;
			}
		};
	}

	@After
	public void restoreEngine() {
		Engine.theApp = previousEngine;
	}

	private Project project(File directory) throws Exception {
		var project = new Project() {
			@Override
			public File getDirFile() {
				return directory;
			}
		};
		project.setName("Lab");
		project.add(new FlowEngine());
		return project;
	}

	@Test
	public void aRemovedFlowTakesItsSourceAndItsSchemasAtTheSaveOfTheProject() throws Exception {
		var directory = folder.newFolder();
		var source = directory.toPath().resolve("_flow/flows/SeedLab.flow.js");
		var schema = directory.toPath().resolve("_flow/schemas/SeedLab/seed.out.schema.json");
		var other = directory.toPath().resolve("_flow/flows/Keep.flow.js");
		Files.createDirectories(schema.getParent());
		Files.createDirectories(source.getParent());
		Files.writeString(source, "saved seed");
		Files.writeString(schema, "{}");
		Files.writeString(other, "saved keep");
		var project = project(directory);
		var seed = new Flow();
		seed.setName("SeedLab");
		project.add(seed);
		var keep = new Flow();
		keep.setName("Keep");
		project.add(keep);

		var check = DboRemoval.check(seed);
		assertEquals(Set.of("_flow/flows/SeedLab.flow.js", "_flow/schemas/SeedLab/seed.out.schema.json"),
				Set.of(check.getJSONArray("sources").getString(0), check.getJSONArray("sources").getString(1)));
		assertEquals(0, check.getJSONArray("folders").length());

		assertTrue(DboRemoval.remove(seed, List.of(), false).getBoolean("done"));
		assertEquals("the Flow left the project", List.of("Keep"),
				project.getSequencesList().stream().map(sequence -> sequence.getName()).toList());
		assertTrue(Files.isRegularFile(source));
		assertTrue("the removal is a working copy until the save", Files.isRegularFile(schema));
		assertEquals(Set.of(source.toFile().getCanonicalPath(), schema.toFile().getCanonicalPath()),
				project.getFlowEngine().getSourceChanges().removals());

		project.getFlowEngine().saveSources();
		assertFalse(Files.exists(source));
		assertFalse(Files.exists(schema));
		assertEquals("saved keep", Files.readString(other));
	}

	@Test
	public void aConnectorTakesTheFoldersChosenAmongItsLinkedOnes() throws Exception {
		var directory = folder.newFolder();
		var project = project(directory);
		var connector = new SqlConnector();
		connector.setName("LabSql");
		project.add(connector);
		var check = DboRemoval.check(connector);
		assertEquals(0, check.getJSONArray("folders").length());
		assertFalse(check.has("sources"));
		assertTrue(DboRemoval.remove(connector, List.of("/not/a/linked/folder"), false).getBoolean("done"));
		assertTrue(project.getConnectorsList().isEmpty());
	}

	@Test
	public void theDeletionsTheStudioRefusesAreRefused() throws Exception {
		try {
			DboRemoval.check(new ThenStep());
			fail("a Then step is not deleted alone");
		} catch (ServiceException e) {
			assertEquals("Cannot delete this step!", e.getMessage());
		}
	}
}
