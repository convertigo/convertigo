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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.beans.core.Project;

public class PathAccessorsTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private String previousProjects, previousWorkspace;
	private DatabaseObjectsManager.StudioProjects previousRegistry;
	private final Map<String, File> registered = new java.util.HashMap<>();

	@Before public void setup() throws Exception {
		previousProjects = Engine.PROJECTS_PATH;
		previousWorkspace = Engine.USER_WORKSPACE_PATH;
		previousRegistry = DatabaseObjectsManager.studioProjects;
		Engine.USER_WORKSPACE_PATH = folder.getRoot().getCanonicalPath();
		Engine.PROJECTS_PATH = Files.createDirectory(folder.getRoot().toPath().resolve("projects")).toString();
		DatabaseObjectsManager.studioProjects = new DatabaseObjectsManager.StudioProjects() {
			@Override public File getProject(String name) { return registered.get(name); }
			@Override public void reloadProject(String name) { }
			@Override public void renameProject(String oldName, String newName) { }
		};
	}

	@After public void restore() {
		Engine.PROJECTS_PATH = previousProjects;
		Engine.USER_WORKSPACE_PATH = previousWorkspace;
		DatabaseObjectsManager.studioProjects = previousRegistry;
	}

	private Path projectPath(String method, String name) throws Exception {
		return (Path) Engine.class.getMethod(method, String.class).invoke(null, name);
	}

	@Test public void projectAccessorsKeepXmlYamlAndMissingProjectResolution() throws Exception {
		for (String name : new String[] {"missing", "xml", "yaml"}) {
			if (!name.equals("missing")) {
				var directory = Files.createDirectory(Path.of(Engine.PROJECTS_PATH, name));
				Files.createFile(directory.resolve(name.equals("xml") ? "xml.xml" : "c8oProject.yaml"));
			}
			assertEquals(Engine.projectFile(name).toPath(), projectPath("projectPath", name));
			assertEquals(Engine.projectYamlFile(name).toPath(), projectPath("projectYamlPath", name));
		}
	}

	@Test public void linkedAndRegisteredProjectsKeepTheirExistingResolution() throws Exception {
		var external = Files.createDirectory(folder.getRoot().toPath().resolve("external-é"));
		Files.createFile(external.resolve("linked.xml"));
		Files.writeString(Path.of(Engine.PROJECTS_PATH, "linked"), external.toString());
		assertEquals(external.resolve("linked.xml"), projectPath("projectPath", "linked"));
		assertEquals(external.resolve("c8oProject.yaml"), projectPath("projectYamlPath", "linked"));
		registered.put("registered", external.resolve("registered.xml").toFile());
		Files.createFile(external.resolve("registered.xml"));
		assertEquals(Engine.projectFile("registered").toPath(), projectPath("projectPath", "registered"));
		assertEquals(Engine.projectYamlFile("registered").toPath(), projectPath("projectYamlPath", "registered"));
		var input = Path.of(Engine.PROJECTS_PATH, "linked", "resource.bin");
		assertEquals(Engine.resolveProjectPath(input.toFile()).toPath(),
				Engine.class.getMethod("resolveProjectNioPath", Path.class).invoke(null, input));
	}

	@Test public void directoryAndFilePropertiesKeepRelativeAbsoluteAndWindowsSpelling() throws Exception {
		var project = new Project(); project.setName("demo");
		assertEquals(project.getDirFile().toPath(), Project.class.getMethod("getDirectoryPath").invoke(project));
		var manager = new FilePropertyManager();
		for (String value : new String[] {"bare.txt", ".//nested/file", "./projects/demo/file", "./workspace.txt",
				folder.getRoot().toPath().resolve("absolute.txt").toString(), "C:\\work\\file.txt"}) {
			assertEquals(value, manager.getFileFromProperty(value, "demo").toPath(),
					FilePropertyManager.class.getMethod("getPathFromProperty", String.class, String.class).invoke(manager, value, "demo"));
		}
	}

	@Test public void nullArgumentsKeepTheirLegacyFailureAndDoNotAddOverloads() throws Exception {
		assertThrows(NullPointerException.class, () -> Engine.projectFile(null));
		var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
				() -> Engine.class.getMethod("projectPath", String.class).invoke(null, new Object[] {null}));
		assertTrue(failure.getCause() instanceof NullPointerException);
		assertEquals(2, java.util.Arrays.stream(Engine.class.getMethods())
				.filter(method -> method.getName().equals("resolveProjectPath")).count());
	}

	@Test public void rhinoKeepsFileCallsAndCanChoosePathNamesExplicitly() throws Exception {
		var project = new Project(); project.setName("demo");
		try (var context = org.mozilla.javascript.Context.enter()) {
			var scope = context.initStandardObjects();
			ScriptableObject.putProperty(scope, "project", org.mozilla.javascript.Context.javaToJS(project, scope));
			Object result = context.evaluateString(scope, "var Engine = Packages.com.twinsoft.convertigo.engine.Engine;"
					+ "Engine.projectFile('demo') instanceof java.io.File && "
					+ "Engine.projectYamlFile('demo') instanceof java.io.File && "
					+ "Engine.resolveProjectPath(Engine.projectFile('demo')) instanceof java.io.File && "
					+ "Engine.projectPath('demo') instanceof java.nio.file.Path && "
					+ "Engine.projectYamlPath('demo') instanceof java.nio.file.Path && "
					+ "project.getDirFile() instanceof java.io.File && "
					+ "String(project.getDirPath()) === String(project.getDirectoryPath())",
					"path-accessors.js", 1, null);
			assertEquals(Boolean.TRUE, result);
		}
		assertEquals(File.class, Engine.class.getMethod("projectFile", String.class).getReturnType());
		assertEquals(File.class, Engine.class.getMethod("projectYamlFile", String.class).getReturnType());
		assertEquals(File.class, Project.class.getMethod("getDirFile").getReturnType());
		assertEquals(File.class, FilePropertyManager.class.getMethod("getFileFromProperty", String.class, String.class).getReturnType());
	}
}
