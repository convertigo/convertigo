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

	@Test public void refusesDuplicateNamesVersionsAndPortableAliases() throws Exception {
		var app = new ProjectInfo("App", "1", Set.of("Shared"));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of(app, app)));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of(app, new ProjectInfo("App", "2", Set.of()))));
		assertThrows(IOException.class, () -> ProjectBundlePlan.create(List.of(app, new ProjectInfo("app", "1", Set.of()))));
		var cycle = ProjectBundlePlan.create(List.of(app, new ProjectInfo("Shared", "2", Set.of("App"))));
		assertTrue(cycle.groups().get(0).cyclic());
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
			try (var prepared = ProjectBundleImport.prepare(List.of(output), folder.getRoot().toPath(), ProjectBundleZip.Limits.defaults())) {
				assertEquals(plan.projects(), prepared.plan().projects());
				assertTrue(prepared.warnings().isEmpty());
				for (var info : prepared.plan().projects()) {
					try (var canonical = new ZipFile(prepared.archive(info.name()).toFile())) {
						assertEquals(Files.readString(root.resolve(info.name()).resolve("asset.txt")),
								new String(canonical.getInputStream(canonical.getEntry(info.name() + "/asset.txt")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
					}
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
			Files.writeString(root.resolve("Shared/c8oProject.yaml"), "↑convertigo: 8.0.0.m006\n↓Shared [core.Project]: \n  version: 2.3\n  ↓App [references.ProjectSchemaReference]: \n    projectName: App\n");
			ProjectBundle.export(output, List.of("App"), true, ArchiveExportOption.all, catalog::get);
			try (var prepared = ProjectBundleImport.prepare(List.of(output), folder.getRoot().toPath(), ProjectBundleZip.Limits.defaults())) {
				assertTrue(prepared.plan().groups().get(0).cyclic());
				assertEquals(1, prepared.warnings().size());
			}
			assertThrows(IOException.class, () -> ProjectBundle.export(output, List.of("Missing"), true, ArchiveExportOption.all, catalog::get));
			try (var files = Files.list(output.getParent())) {
				assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".project-bundle-")));
			}
		} finally {
			Engine.PROJECTS_PATH = previous;
		}
	}

	@Test public void includesAdditionalProjectsAndExcludesReferencesWithoutEditingDescriptors() throws Exception {
		String previous = Engine.PROJECTS_PATH;
		Engine.logEngine = org.apache.log4j.Logger.getLogger("bundle-selection-test");
		var root = folder.newFolder("selection").toPath();
		Engine.PROJECTS_PATH = root.toString();
		try {
			var app = project(root, "App", "1", "Shared");
			var shared = project(root, "Shared", "2", "Leaf");
			var leaf = project(root, "Leaf", "3", null);
			var extra = project(root, "Extra", "4", null);
			var catalog = Map.of("App", app, "Shared", shared, "Leaf", leaf, "Extra", extra);
			var preview = ProjectBundle.preview(List.of("App"), true, List.of("Extra"), List.of("Shared"), name -> {
				assertNotEquals("An excluded project must not be loaded", "Shared", name);
				return catalog.get(name);
			});
			assertEquals(Set.of("App", "Extra"), preview.roots());
			assertEquals(Set.of("Shared"), preview.excludedProjects());
			assertEquals(List.of("App", "Extra"), preview.plan().projects().stream().map(ProjectInfo::name).toList());
			var output = folder.getRoot().toPath().resolve("selection.zip");
			var plan = ProjectBundle.export(output, List.of("App"), true, List.of("Extra"), List.of("Shared"), ArchiveExportOption.all, catalog::get);
			assertEquals(preview.plan().projects(), plan.projects());
			try (var zip = new ZipFile(output.toFile())) {
				assertNull(zip.getEntry("Shared.car"));assertNull(zip.getEntry("Leaf.car"));assertNotNull(zip.getEntry("Extra.car"));
				var manifest = new JSONObject(new String(zip.getInputStream(zip.getEntry("bundle.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
				assertEquals("Shared", manifest.getJSONArray("excludedProjects").getString(0));
				assertEquals("Shared", manifest.getJSONArray("projects").getJSONObject(0).getJSONArray("dependencies").getString(0));
			}
			try (var prepared = ProjectBundleImport.prepare(List.of(output), folder.getRoot().toPath(), ProjectBundleZip.Limits.defaults())) {
				assertTrue(prepared.warnings().isEmpty());
				var error = assertThrows(IOException.class, () -> prepared.checkDependencies(name -> false));
				assertTrue(error.getMessage().contains("Required project Shared referenced by App"));
				prepared.checkDependencies(name -> name.equals("Shared"));
			}
			ProjectBundle.export(output, List.of("App"), true, List.of("Extra"), List.of(), ArchiveExportOption.all, catalog::get);
			try (var prepared = ProjectBundleImport.prepare(List.of(output), folder.getRoot().toPath(), ProjectBundleZip.Limits.defaults())) {
				assertEquals(4, prepared.plan().projects().size());
				prepared.checkDependencies(name -> false);
			}
			var withoutClosure = ProjectBundle.preview(List.of("App"), false, List.of("Extra"), List.of(), catalog::get);
			assertEquals(List.of("App", "Extra"), withoutClosure.plan().projects().stream().map(ProjectInfo::name).toList());
		} finally { Engine.PROJECTS_PATH = previous; }
	}

	@Test public void refusesExcludedRootsAndInvalidSelectionNamesBeforeResolving() {
		ProjectBundle.ProjectResolver unused = name -> { throw new AssertionError("No project should be resolved"); };
		for (var excluded : List.of("App", "Extra")) {
			var failure = assertThrows(IOException.class, () -> ProjectBundle.preview(List.of("App"), true, List.of("Extra"), List.of(excluded), unused));
			assertTrue(failure.getMessage().contains("cannot be excluded"));
		}
		assertThrows(IOException.class, () -> ProjectBundle.preview(List.of("App"), true, List.of("Extra", "Extra"), List.of(), unused));
		assertThrows(IllegalArgumentException.class, () -> ProjectBundle.preview(List.of("App"), true, List.of(), List.of("../Shared"), unused));
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

	@Test public void checksSourcePathsBeforeReplacingAnExistingBundle() throws Exception {
		String previous = Engine.PROJECTS_PATH;
		Engine.logEngine = org.apache.log4j.Logger.getLogger("bundle-source-test");
		var root = folder.newFolder("source-projects").toPath();
		Engine.PROJECTS_PATH = root.toString();
		try {
			var app = project(root, "App", "1", null);
			var output = folder.getRoot().toPath().resolve("portable.zip");
			ProjectBundle.export(output, List.of("App"), false, ArchiveExportOption.all, name -> app);
			var before = Files.readAllBytes(output);
			if (java.io.File.separatorChar != '\\') {
				Files.writeString(root.resolve("App/unsupported?.txt"), "asset");
				assertThrows(IOException.class, () -> ProjectBundle.export(output, List.of("App"), false, ArchiveExportOption.all, name -> app));
				assertArrayEquals(before, Files.readAllBytes(output));
				Files.delete(root.resolve("App/unsupported?.txt"));
			}
			var sourceDestination = root.resolve("App/application.zip");
			assertThrows(IOException.class, () -> ProjectBundle.export(sourceDestination, List.of("App"), false, ArchiveExportOption.all, name -> app));
			assertFalse(Files.exists(sourceDestination));
			var buildDestination = root.resolve("App/build/application.zip");
			ProjectBundle.export(buildDestination, List.of("App"), false, ArchiveExportOption.all, name -> app);
			try (var prepared = ProjectBundleImport.prepare(List.of(buildDestination), folder.getRoot().toPath(), ProjectBundleZip.Limits.defaults());
					var car = new ZipFile(prepared.archive("App").toFile())) {
				assertFalse(car.stream().anyMatch(entry -> entry.getName().startsWith("App/build/")));
			}
		} finally {
			Engine.PROJECTS_PATH = previous;
		}
	}

	@Test public void ordersDependencyGroupsAndHonorsManifestOrderInsideACycleOnly() throws Exception {
		var app = new ProjectInfo("App", "1", Set.of("Shared", "Base"));
		var shared = new ProjectInfo("Shared", "1", Set.of("App"));
		var base = new ProjectInfo("Base", "1", Set.of());
		var downstream = new ProjectInfo("Viewer", "1", Set.of("App"));
		var selected = List.of(downstream, shared, app, base);
		var plain = ProjectBundlePlan.create(selected);
		assertEquals(List.of("Base", "App", "Shared", "Viewer"), plain.projects().stream().map(ProjectInfo::name).toList());
		assertEquals(3, plain.groups().size());
		assertTrue(plain.groups().get(1).cyclic());
		var manifest = ProjectBundlePlan.create(selected, List.of(List.of("Viewer", "Shared", "App", "Base")));
		assertEquals(List.of("Base", "Shared", "App", "Viewer"), manifest.projects().stream().map(ProjectInfo::name).toList());
		var partial = ProjectBundlePlan.create(selected, List.of(List.of("Shared")));
		assertEquals(plain.projects(), partial.projects());
		assertTrue(ProjectBundlePlan.create(List.of(new ProjectInfo("Self", "1", Set.of("Self")))).groups().get(0).cyclic());
	}

	private static Project project(Path root, String name, String version, String dependency) throws Exception {
		var project = new Project();
		project.setName(name);
		project.setVersion(version);
		var dir = Files.createDirectory(root.resolve(name));
		Files.writeString(dir.resolve("c8oProject.yaml"), "↑convertigo: 8.0.0.m006\n↓" + name + " [core.Project]: \n  version: " + version + "\n"
				+ (dependency == null ? "" : "  ↓Reference [references.ProjectSchemaReference]: \n    projectName: " + dependency + "\n"));
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
