package com.convertigo.gradle;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;

public class ProjectBundleTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void taskExportsIncludedProjectsAndOmitsExcludedDependencies() throws Exception {
		String previousPath = Engine.PROJECTS_PATH;
		Engine previousApp = Engine.theApp;
		var previousLog = Engine.logEngine;
		var previousManagerLog = Engine.logDatabaseObjectManager;
		try {
			Engine.PROJECTS_PATH = folder.newFolder("projects").toString();
			Engine.logEngine = Engine.logDatabaseObjectManager = org.apache.log4j.Logger.getLogger("bundle-gradle-test");
			var app = project("App", "Remote");var extra = project("Extra", null);var remote = project("Remote", null);
			var catalog = Map.of("App", app, "Extra", extra, "Remote", remote);
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override public Project getOriginalProjectByName(String name, boolean checkOpenable) { return catalog.get(name); }
			};
			var build = ProjectBuilder.builder().withProjectDir(folder.newFolder("build-project")).build();
			var plugin = new ConvertigoPlugin();plugin.apply(build);
			var loaded = ProjectLoad.class.getDeclaredField("convertigoProject");loaded.setAccessible(true);loaded.set(plugin.load, app);
			var task = (ProjectBundle)build.getTasks().getByName("projectBundle");
			task.setIncludeDependencies(true);task.setIncludeProjects(List.of("Extra"));task.setExcludeProjects(List.of("Remote"));
			var output = folder.getRoot().toPath().resolve("selection.zip");task.setDestinationFile(output.toFile());
			task.taskAction();
			try (var zip = new ZipFile(output.toFile())) {
				assertNotNull(zip.getEntry("App.car"));assertNotNull(zip.getEntry("Extra.car"));assertNull(zip.getEntry("Remote.car"));
				var manifest = new org.codehaus.jettison.json.JSONObject(new String(zip.getInputStream(zip.getEntry("bundle.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
				assertEquals("Remote", manifest.getJSONArray("excludedProjects").getString(0));
			}
			task.setExcludeProjects(List.of());task.taskAction();
			try (var zip = new ZipFile(output.toFile())) { assertNotNull(zip.getEntry("Remote.car")); }
			task.setExcludeProjects(List.of("Extra"));
			assertTrue(assertThrows(java.io.IOException.class, task::taskAction).getMessage().contains("cannot be excluded"));
		} finally {
			Engine.PROJECTS_PATH = previousPath;Engine.theApp = previousApp;
			Engine.logEngine = previousLog;Engine.logDatabaseObjectManager = previousManagerLog;
		}
	}

	private Project project(String name, String dependency) throws Exception {
		var project = new Project();project.setName(name);project.setVersion("1");
		var dir = Files.createDirectories(Path.of(Engine.PROJECTS_PATH, name));
		Files.writeString(dir.resolve("c8oProject.yaml"), "↑convertigo: 8.0.0.m006\n↓" + name + " [core.Project]: \n  version: 1\n"
				+ (dependency == null ? "" : "  ↓Reference [references.ProjectSchemaReference]: \n    projectName: " + dependency + "\n"));
		if (dependency != null) { var reference = new ProjectSchemaReference();reference.setProjectName(dependency);project.add(reference); }
		return project;
	}
}
