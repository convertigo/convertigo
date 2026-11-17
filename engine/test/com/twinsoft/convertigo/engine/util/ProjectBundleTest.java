package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo;

public class ProjectBundleTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void ordersDeclaredReferencesIndependentlyOfSelectionOrder() throws Exception {
		var app = new ProjectInfo("App", "1", Set.of("Shared", "External"));
		var shared = new ProjectInfo("Shared", "2", Set.of());
		var unused = new ProjectInfo("Another", "3", Set.of());
		var first = ProjectBundlePlan.create(List.of(app, unused, shared));
		var second = ProjectBundlePlan.create(List.of(shared, app, unused));
		assertEquals(first.projects(), second.projects());
		assertEquals(List.of("Another", "Shared", "App"), first.projects().stream().map(ProjectInfo::name).toList());
	}

	@Test public void refusesDuplicateNamesVersionsPortableAliasesAndCycles() throws Exception {
		var app = new ProjectInfo("App", "1", Set.of("Shared"));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of(app, app)));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of(app, new ProjectInfo("App", "2", Set.of()))));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of(app, new ProjectInfo("app", "1", Set.of()))));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of(app, new ProjectInfo("Shared", "2", Set.of("App")))));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of()));
		assertThrows(IllegalArgumentException.class, () -> new ProjectInfo("../App", "1", Set.of()));
		assertThrows(IllegalArgumentException.class, () -> new ProjectInfo("CON", "1", Set.of()));
	}

	@Test public void exportsRealCarsAndExactDependencyClosureWithTheSharedManifest() throws Exception {
		String previous = Engine.PROJECTS_PATH;
		Engine.logEngine = org.apache.log4j.Logger.getLogger("bundle-test");
		Engine.logDatabaseObjectManager = Engine.logEngine;
		var root = folder.newFolder("projects").toPath();
		Engine.PROJECTS_PATH = root.toString();
		try {
			var app = project(root, "App", "1.2", "Shared");
			var shared = project(root, "Shared", "2.3", null);
			var unrelated = project(root, "Unrelated", "3.4", null);
			var catalog = Map.of("App", app, "Shared", shared, "Unrelated", unrelated);
			var output = folder.getRoot().toPath().resolve("all.zip");
			var plan = ProjectBundle.export(output, List.of("App"), true, ArchiveExportOption.all, catalog::get);
			assertEquals(List.of("Shared", "App"), plan.projects().stream().map(ProjectInfo::name).toList());
			try (var zip = new ZipFile(output.toFile())) {
				assertEquals(3, zip.size());
				assertNull(zip.getEntry("Unrelated.car"));
				var json = new JSONObject(new String(zip.getInputStream(zip.getEntry("bundle.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
				assertEquals(1, json.getInt("formatVersion"));
				assertEquals("Shared", json.getJSONArray("order").getString(0));
				assertEquals("2.3", json.getJSONArray("projects").getJSONObject(0).getString("version"));
				var car = folder.getRoot().toPath().resolve("Shared.car");
				Files.copy(zip.getInputStream(zip.getEntry("Shared.car")), car);
				try (var archive = new ZipFile(car.toFile())) {
					assertNotNull(archive.getEntry("Shared/c8oProject.yaml"));
					assertNull(archive.getEntry("Shared/_private/local.txt"));
					assertNotNull(archive.getEntry("Shared/asset.txt"));
				}
			}
			ProjectBundle.export(output, List.of("App"), false, ArchiveExportOption.all, catalog::get);
			try (var zip = new ZipFile(output.toFile())) {
				assertEquals(2, zip.size());
				assertNull(zip.getEntry("Shared.car"));
			}
			var before = Files.readAllBytes(output);
			var cycle = new ProjectSchemaReference();
			cycle.setProjectName("App");
			shared.add(cycle);
			assertThrows(IOException.class, () -> ProjectBundle.export(output, List.of("App"), true, ArchiveExportOption.all, catalog::get));
			assertArrayEquals(before, Files.readAllBytes(output));
			assertThrows(IOException.class, () -> ProjectBundle.export(output, List.of("Missing"), true, ArchiveExportOption.all, catalog::get));
			try (var files = Files.list(output.getParent())) {
				assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".project-bundle-")));
			}
		} finally {
			Engine.PROJECTS_PATH = previous;
		}
	}

	@Test public void serializesConcurrentExportsAndReleasesTheLockAfterFailure() throws Exception {
		var entered = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		var secondEntered = new java.util.concurrent.CountDownLatch(1);
		var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
		try {
			var first = executor.submit(() -> {
				assertThrows(IOException.class, () -> ProjectBundle.export(folder.getRoot().toPath().resolve("first.zip"),
						List.of("First"), false, ArchiveExportOption.all, name -> {
							entered.countDown();
							if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IOException("test timed out");
							return null;
						}));
			});
			assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
			var second = executor.submit(() -> {
				assertThrows(IOException.class, () -> ProjectBundle.export(folder.getRoot().toPath().resolve("second.zip"),
						List.of("Second"), false, ArchiveExportOption.all, name -> {
							secondEntered.countDown();
							return null;
						}));
			});
			assertFalse(secondEntered.await(100, java.util.concurrent.TimeUnit.MILLISECONDS));
			release.countDown();
			first.get(5, java.util.concurrent.TimeUnit.SECONDS);
			second.get(5, java.util.concurrent.TimeUnit.SECONDS);
			assertEquals(0, secondEntered.getCount());
		} finally {
			release.countDown();
			executor.shutdownNow();
		}
	}

	private static Project project(Path root, String name, String version, String dependency) throws Exception {
		var project = new Project();
		project.setName(name);
		project.setVersion(version);
		var dir = Files.createDirectory(root.resolve(name));
		Files.writeString(dir.resolve("c8oProject.yaml"), "project: " + name);
		Files.writeString(dir.resolve("asset.txt"), "asset");
		Files.createDirectory(dir.resolve("_private"));
		Files.writeString(dir.resolve("_private/local.txt"), "local");
		if (dependency != null) {
			var ref = new ProjectSchemaReference();
			ref.setProjectName(dependency);
			project.add(ref);
		}
		return project;
	}
}
