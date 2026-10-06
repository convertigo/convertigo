/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.util.FileUtils;

/** The local working directory, on a temporary folder: no engine is started. */
public class LocalWorkDirectoryTest {
	private static final String IONIC = "_private/ionic";

	private File base;
	private File root;
	private File project;
	private File displayObjects;

	@Before
	public void setUp() throws Exception {
		base = Files.createTempDirectory("local-work-test").toFile().getCanonicalFile();
		root = new File(base, "work");
		project = new File(base, "projects/App");
		displayObjects = new File(project, "DisplayObjects/mobile");
		displayObjects.mkdirs();
		new File(project, "Flashupdate").mkdirs();
		Files.writeString(new File(project, "c8oProject.yaml").toPath(), "project");
		Files.writeString(new File(displayObjects, "index.html").toPath(), "built");
	}

	@After
	public void tearDown() {
		LocalWorkDirectory.use(null);
		FileUtils.deleteQuietly(base);
	}

	private LocalWorkDirectory open(String version, String... projects) {
		var existing = Set.of(projects.length == 0 ? new String[] { "App" } : projects);
		var local = LocalWorkDirectory.open(root, version, existing::contains);
		assertNotNull("the directory is usable", local);
		return local;
	}

	private File local(String path) {
		return new File(root, "projects/App/" + path);
	}

	/** Removals go to the background deletions of the engine when another test started its logs. */
	private static void assertGone(String message, File file) throws Exception {
		for (int i = 0; i < 100 && Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS); i++) {
			Thread.sleep(100);
		}
		assertFalse(message, Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS));
	}

	private void assertProjectIntact() throws Exception {
		assertEquals("the project keeps its files", "built", Files.readString(new File(displayObjects, "index.html").toPath()));
		assertTrue(new File(project, "Flashupdate").isDirectory());
		assertTrue(new File(project, "c8oProject.yaml").isFile());
	}

	@Test
	public void withoutDirectoryFoldersStayInTheProject() throws Exception {
		LocalWorkDirectory.use(null);
		var ionic = new File(project, IONIC);
		ionic.mkdirs();
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertTrue("a folder of the project is kept", ionic.isDirectory() && !Files.isSymbolicLink(ionic.toPath()));
		assertNull(LocalWorkDirectory.getProjectDirectory("App"));
	}

	@Test
	public void withoutDirectoryALinkLeftByAPreviousConfigurationIsRemoved() throws Exception {
		LocalWorkDirectory.use(null);
		var elsewhere = new File(base, "elsewhere");
		elsewhere.mkdir();
		var ionic = new File(project, IONIC);
		ionic.getParentFile().mkdirs();
		Files.createSymbolicLink(ionic.toPath(), elsewhere.toPath());
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertFalse("the link is removed, the folder is rebuilt in the project", Files.exists(ionic.toPath(), LinkOption.NOFOLLOW_LINKS));
		assertTrue("its former target is not touched", elsewhere.isDirectory());
	}

	@Test
	public void theFolderIsLinkedAndItsRelativePathsReachTheProject() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var ionic = LocalWorkDirectory.relocate("App", project, IONIC);
		assertTrue(Files.isSymbolicLink(ionic.toPath()));
		assertEquals(local(IONIC).toPath(), Files.readSymbolicLink(ionic.toPath()));
		assertTrue("the folder is usable at once", ionic.isDirectory());

		Files.writeString(new File(ionic, "angular.json").toPath(), "{}");
		assertTrue("its content is written locally", local(IONIC + "/angular.json").isFile());

		// the template writes its build in ../../DisplayObjects/mobile and reads ../../Flashupdate
		assertEquals(displayObjects.getCanonicalFile(), new File(local(IONIC), "../../DisplayObjects/mobile").getCanonicalFile());
		assertEquals(new File(project, "Flashupdate").getCanonicalFile(), new File(local(IONIC), "../../Flashupdate").getCanonicalFile());
		assertFalse("the folder holding the relocated one is not mirrored", Files.isSymbolicLink(local("_private").toPath()));
	}

	@Test
	public void theMirrorFollowsTheProject() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		LocalWorkDirectory.relocate("App", project, IONIC);
		new File(project, "Flashupdate").delete();
		new File(project, "xsd").mkdir();
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertFalse("a link to an entry no longer in the project goes", Files.exists(local("Flashupdate").toPath(), LinkOption.NOFOLLOW_LINKS));
		assertTrue("a new entry of the project is mirrored", Files.isSymbolicLink(local("xsd").toPath()));
	}

	@Test
	public void aFolderWrittenInTheProjectIsReplaced() throws Exception {
		var ionic = new File(project, IONIC);
		new File(ionic, "node_modules/old").mkdirs();
		LocalWorkDirectory.use(open("v1"));
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertTrue(Files.isSymbolicLink(ionic.toPath()));
		assertFalse("the former content is not taken", new File(ionic, "node_modules").exists());
		var left = ionic.getParentFile().listFiles((dir, name) -> name.startsWith("ionic.moved-"));
		for (var aside : left) {
			assertGone("the former folder is removed", aside);
		}
	}

	@Test
	public void aLinkToAnotherPlaceIsRepointed() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var ionic = new File(project, IONIC);
		ionic.getParentFile().mkdirs();
		Files.createSymbolicLink(ionic.toPath(), new File(base, "old-place").toPath());
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertEquals(local(IONIC).toPath(), Files.readSymbolicLink(ionic.toPath()));
	}

	@Test
	public void anInstallationIsMarkedUntilItSucceeds() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var nodeModules = new File(LocalWorkDirectory.relocate("App", project, IONIC), "node_modules");
		LocalWorkDirectory.beginWork(nodeModules);
		var marker = local(".c8o-working/_private%2Fionic%2Fnode_modules");
		assertTrue("the installation is marked in progress", marker.isFile());
		LocalWorkDirectory.endWork(nodeModules);
		assertFalse("a completed installation is no longer marked", marker.exists());
	}

	@Test
	public void aFolderElsewhereIsNotMarked() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var elsewhere = new File(base, "elsewhere/node_modules");
		elsewhere.getParentFile().mkdirs();
		LocalWorkDirectory.beginWork(elsewhere);
		assertFalse(new File(root, "projects").exists() && local(".c8o-working").exists());
	}

	@Test
	public void contentIsReusedAfterARestartOfTheSameVersion() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var nodeModules = new File(LocalWorkDirectory.relocate("App", project, IONIC), "node_modules");
		LocalWorkDirectory.beginWork(nodeModules);
		new File(nodeModules, "pkg").mkdirs();
		LocalWorkDirectory.endWork(nodeModules);
		LocalWorkDirectory.use(null);

		LocalWorkDirectory.use(open("v1"));
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertTrue("installed packages are reused", new File(nodeModules, "pkg").isDirectory());
	}

	@Test
	public void anInterruptedInstallationIsRemovedAtStartup() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var nodeModules = new File(LocalWorkDirectory.relocate("App", project, IONIC), "node_modules");
		LocalWorkDirectory.beginWork(nodeModules);
		new File(nodeModules, "half").mkdirs();
		LocalWorkDirectory.use(null);

		LocalWorkDirectory.use(open("v1"));
		assertFalse("half installed packages are not reused", local(IONIC + "/node_modules").exists());
		assertTrue("the rest of the folder is kept", local(IONIC).isDirectory());
		assertFalse(local(".c8o-working/_private%2Fionic%2Fnode_modules").exists());
		assertProjectIntact();
	}

	@Test
	public void anotherVersionRemovesItsDataOnly() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		LocalWorkDirectory.relocate("App", project, IONIC);
		LocalWorkDirectory.use(null);
		var foreign = new File(root, "not-ours.txt");
		Files.writeString(foreign.toPath(), "kept");

		LocalWorkDirectory.use(open("v2"));
		assertFalse("data of another version is removed", local(IONIC).exists());
		assertEquals("v2", Files.readString(new File(root, ".convertigo-version").toPath(), StandardCharsets.UTF_8));
		assertTrue("other content of the directory is kept", foreign.isFile());
		assertProjectIntact();
	}

	@Test
	public void dataOfRemovedProjectsIsRemovedWithoutTheirFolders() throws Exception {
		LocalWorkDirectory.use(open("v1", "App", "Other"));
		LocalWorkDirectory.relocate("App", project, IONIC);
		var other = new File(base, "projects/Other");
		new File(other, "DisplayObjects").mkdirs();
		LocalWorkDirectory.relocate("Other", other, IONIC);
		LocalWorkDirectory.use(null);

		LocalWorkDirectory.use(open("v1", "Other"));
		assertFalse("the data of a project no longer in the workspace is removed", new File(root, "projects/App").exists());
		assertTrue(new File(root, "projects/Other/" + IONIC).isDirectory());
		assertProjectIntact();

		LocalWorkDirectory.projectRemoved("Other");
		assertGone("a deleted project takes its data along", new File(root, "projects/Other"));
		assertTrue("but not its folders", new File(other, "DisplayObjects").isDirectory());
	}

	@Test
	public void twoEnginesDoNotShareTheDirectory() throws Exception {
		var first = open("v1");
		var second = LocalWorkDirectory.open(root, "v1", name -> true);
		try {
			assertNotNull(second);
			var instance = new File(root, "instance-" + ProcessHandle.current().pid());
			assertEquals("the second engine works in its own sub-directory", instance, second.getRoot());
			assertTrue(new File(instance, ".convertigo-version").isFile());
		} finally {
			second.close();
			first.close();
		}
		LocalWorkDirectory.use(open("v1"));
		assertFalse("the sub-directory of a stopped engine is removed",
				new File(root, "instance-" + ProcessHandle.current().pid()).exists());
	}

	@Test
	public void projectsKeepTheirFoldersWhenTheirDataIsRemoved() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertTrue(Files.isSymbolicLink(local("DisplayObjects").toPath()));
		LocalWorkDirectory.projectRemoved("App");
		assertGone("the data of the project is removed", local(""));
		assertProjectIntact();
		assertTrue("the link left in the project is removed with the project folder", Files.isSymbolicLink(new File(project, IONIC).toPath()));
	}

	@Test
	public void engineFoldersAreKeptAcrossVersions() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var nodes = LocalWorkDirectory.getDirectory("nodes");
		assertEquals(new File(root, "nodes"), nodes);
		assertTrue("the folder is created", nodes.isDirectory());
		new File(nodes, "node-v22-linux-x64/bin").mkdirs();
		LocalWorkDirectory.relocate("App", project, IONIC);
		assertNull("projects are not an engine folder", LocalWorkDirectory.getDirectory("projects"));
		LocalWorkDirectory.use(null);
		assertNull("no engine folder without local working directory", LocalWorkDirectory.getDirectory("nodes"));

		LocalWorkDirectory.use(open("v2"));
		assertTrue("Node.js distributions do not depend on the Convertigo version", new File(root, "nodes/node-v22-linux-x64/bin").isDirectory());
		assertFalse("project data does", local(IONIC).exists());
	}

	@Test
	public void anInterruptedExtractionIsRemovedAtStartup() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var nodes = LocalWorkDirectory.getDirectory("nodes");
		var interrupted = new File(nodes, "node-v22-linux-arm64");
		var completed = new File(nodes, "node-v20-linux-arm64");
		LocalWorkDirectory.beginWork(interrupted);
		new File(interrupted, "bin").mkdirs();
		LocalWorkDirectory.beginWork(completed);
		new File(completed, "bin").mkdirs();
		LocalWorkDirectory.endWork(completed);
		assertTrue(new File(root, ".c8o-working/nodes%2Fnode-v22-linux-arm64").isFile());
		LocalWorkDirectory.use(null);

		LocalWorkDirectory.use(open("v1"));
		assertFalse("a half extracted distribution is not reused", interrupted.exists());
		assertTrue("a completed one is", new File(completed, "bin").isDirectory());
		assertFalse(new File(root, ".c8o-working/nodes%2Fnode-v22-linux-arm64").exists());
	}

	@Test
	public void relativePathsResolveFromTheLink() throws Exception {
		LocalWorkDirectory.use(open("v1"));
		var ionic = LocalWorkDirectory.relocate("App", project, IONIC);
		// processes started in the folder work in its real place, where ../../ is the mirror of the project
		var real = ionic.toPath().toRealPath();
		assertEquals(displayObjects.toPath().toRealPath(), real.resolve("../../DisplayObjects/mobile").toRealPath());
		assertTrue(Path.of(root.getPath()).toRealPath().resolve("projects/App").equals(real.getParent().getParent()));
	}
}
