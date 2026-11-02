/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.CarUtils;

public class EclipseProjectArchiveTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private String previousProjects;
	private Engine previousApp;
	private boolean previousCli;

	@Before public void setup() throws Exception {
		previousProjects = Engine.PROJECTS_PATH;
		previousApp = Engine.theApp;
		previousCli = Engine.bCliMode;
		Engine.bCliMode = true;
		Engine.logEngine = Engine.logBeans = Engine.logDatabaseObjectManager = Logger.getLogger("eclipse-archive-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		Engine.theApp.referencedProjectManager = new ReferencedProjectManager();
		Engine.PROJECTS_PATH = folder.newFolder("projects").getAbsolutePath();
		Engine.theApp.databaseObjectsManager.init();
	}

	@After public void restore() {
		Engine.PROJECTS_PATH = previousProjects;
		Engine.theApp = previousApp;
		Engine.bCliMode = previousCli;
	}

	@Test public void exportsWithAndWithoutTestCasesExcludeRootAndNestedDescriptionsOnly() throws Exception {
		Project project = new Project();
		project.setName("Demo");
		Path directory = Files.createDirectories(Path.of(Engine.projectDir("Demo")));
		Set<String> retained = Set.of("c8oProject.yaml", ".gitignore", ".httpignore", ".settings.json",
				"assets/public.txt", "assets/.project.json", "assets/.project-backup", "nested/.settings-backup");
		for (String path : retained) write(directory, path, "retain");
		for (String path : Set.of(".project", "nested/.project", "deep/more/.project", "nested/.project/child")) {
			// The nested .project directory is tested separately from the file.
			if (!path.equals("nested/.project")) write(directory, path, "Eclipse metadata");
		}
		for (String path : Set.of("_private/local", "_data/local", "build/local", "oas2/local", "oas3/local",
				"nested/.git/config", "nested/node_modules/module.js", ".settings/editor.prefs",
				"nested/.settings/editor.prefs", "deep/more/.settings/editor.prefs")) write(directory, path, "excluded");
		for (boolean includeTests : new boolean[] {true, false}) {
			var options = EnumSet.copyOf(ArchiveExportOption.all);
			if (!includeTests) options.remove(ArchiveExportOption.includeTestCase);
			File archive = CarUtils.makeArchive(folder.newFile("export-" + includeTests + ".car"), project, options);
			try (ZipFile zip = new ZipFile(archive)) {
				Set<String> entries = zip.stream().map(ZipEntry::getName).collect(Collectors.toSet());
				assertFalse(entries.toString(), entries.stream().anyMatch(p -> p.matches(".*(?:^|/)\\.(?:project|settings)(?:/.*)?$")));
				assertEquals(retained.stream().map(p -> "Demo/" + p).collect(Collectors.toSet()), entries);
			}
		}
	}

	@Test public void legacyArchiveStillDeploysAndRenamesItsEclipseDescription() throws Exception {
		for (String target : new String[] {"Legacy", "Renamed"}) {
			Path archive = folder.newFile(target + ".car").toPath();
			Map<String, String> files = Map.of(
					"Legacy/c8oProject.yaml", "↑convertigo: 8.4.0.m006\n↓Legacy [core.Project]: \n",
					"Legacy/.project", "<projectDescription><name>Legacy</name></projectDescription>",
					"Legacy/public.txt", "public",
					"Legacy/.settings/editor.prefs", "legacy preferences",
					"Legacy/nested/.settings/editor.prefs", "nested legacy preferences");
			try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
				for (var entry : files.entrySet()) {
					zip.putNextEntry(new ZipEntry(entry.getKey()));
					zip.write(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
					zip.closeEntry();
				}
			}
			var manager = Engine.theApp.databaseObjectsManager;
			Project deployed = manager.deployProject(archive.toString(), target, true);
			assertEquals(target, deployed.getName());
			Path directory = Path.of(Engine.projectDir(target));
			assertTrue(Files.readString(directory.resolve(".project")).contains("<name>" + target + "</name>"));
			assertEquals("public", Files.readString(directory.resolve("public.txt")));
			assertEquals("legacy preferences", Files.readString(directory.resolve(".settings/editor.prefs")));
			assertEquals("nested legacy preferences", Files.readString(directory.resolve("nested/.settings/editor.prefs")));
		}
	}

	private static void write(Path directory, String name, String content) throws Exception {
		Path target = directory.resolve(name);
		Files.createDirectories(target.getParent());
		Files.writeString(target, content);
	}
}
