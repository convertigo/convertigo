package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Limits;

public class ProjectBundleImportTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void preparesMixedProjectArchivesAndFoldersInDependencyOrder() throws Exception {
		var root = folder.newFolder().toPath();
		var shared = car("Shared", "2", null);
		var app = car("App", "1", "Shared");
		var entries = new LinkedHashMap<String, byte[]>();
		entries.put("App.car", Files.readAllBytes(app));
		entries.put("Shared.zip", Files.readAllBytes(shared));
		entries.put("Folder/c8oProject.yaml", yaml("Folder", "3", "App"));
		entries.put("Folder/asset.zip", new byte[] { 1, 2, 3 });
		entries.put("Folder/_private/local.txt", text("private"));
		entries.put("Folder/_data/db.txt", text("data"));
		entries.put("__MACOSX/metadata", text("metadata"));
		entries.put(".DS_Store", text("metadata"));
		var upload = zip(entries);
		try (var prepared = ProjectBundleImport.prepare(List.of(upload), root, Limits.defaults())) {
			assertEquals(List.of("Shared", "App", "Folder"), prepared.plan().projects().stream().map(ProjectBundlePlan.ProjectInfo::name).toList());
			assertFalse(prepared.warnings().isEmpty());
			try (var archive = new ZipFile(prepared.archive("Folder").toFile())) {
				assertNotNull(archive.getEntry("Folder/asset.zip"));
				assertNull(archive.getEntry("Folder/_private/local.txt"));
				assertNull(archive.getEntry("Folder/_data/db.txt"));
			}
		}
		assertEmpty(root);
	}

	@Test public void keepsDeepSampleDescriptorsInsideTheSingleProject() throws Exception {
		var root = folder.newFolder().toPath();
		var upload = zip(Map.of("App/c8oProject.yaml", yaml("App", "1", null),
				"App/samples/Another/c8oProject.yaml", yaml("Another", "2", null)));
		try (var prepared = ProjectBundleImport.prepare(List.of(upload), root, Limits.defaults())) {
			assertEquals(1, prepared.plan().projects().size());
			try (var car = new ZipFile(prepared.archive("App").toFile())) {
				assertNotNull(car.getEntry("App/samples/Another/c8oProject.yaml"));
			}
		}
		assertEmpty(root);
	}

	@Test public void readsXmlNamesVersionsAndDeclaredReferencesWithoutLoadingBeans() throws Exception {
		var root = folder.newFolder().toPath();
		var xml = "<convertigo><project><property name=\"name\"><java.lang.String value=\"Legacy\"/></property>"
				+ "<property name=\"version\"><java.lang.String value=\"2.1\"/></property>"
				+ "<reference classname=\"com.twinsoft.convertigo.beans.references.ProjectSchemaReference\">"
				+ "<property name=\"projectName\"><java.lang.String value=\"Shared\"/></property></reference></project></convertigo>";
		var legacy = zip(Map.of("Legacy/Legacy.xml", text(xml)));
		try (var prepared = ProjectBundleImport.prepare(List.of(legacy, car("Shared", "1", null)), root, Limits.defaults())) {
			assertEquals(List.of("Shared", "Legacy"), prepared.plan().projects().stream().map(ProjectBundlePlan.ProjectInfo::name).toList());
			assertEquals("2.1", prepared.plan().projects().get(1).version());
		}
		assertEmpty(root);
	}

	@Test public void ignoresManifestPathsAndUsesTheDescriptorsForNamesVersionsAndOrder() throws Exception {
		var root = folder.newFolder().toPath();
		var upload = zip(Map.of("App.car", Files.readAllBytes(car("App", "1", null)), "bundle.json",
				text("{\"formatVersion\":1,\"projects\":[{\"name\":\"Other\",\"version\":\"2\",\"archive\":\"../outside\"}],\"order\":[\"Other\"]}")));
		try (var prepared = ProjectBundleImport.prepare(List.of(upload), root, Limits.defaults())) {
			assertEquals("App", prepared.plan().projects().get(0).name());
			assertEquals("1", prepared.plan().projects().get(0).version());
			assertEquals(1, prepared.warnings().size());
		}
		assertEmpty(root);
	}

	@Test public void refusesDuplicatesCyclesUnknownTopLevelFilesAndNestedBundlesWithoutWorkspaceChanges() throws Exception {
		var root = folder.newFolder().toPath();
		var workspace = folder.newFolder().toPath();
		var sentinel = Files.writeString(workspace.resolve("keep.txt"), "unchanged");
		var app = car("App", "1", null);
		var duplicate = car("App", "2", null);
		var cycleA = car("A", "1", "B");
		var cycleB = car("B", "1", "A");
		var nested = zip(Map.of("inner.zip", Files.readAllBytes(zip(Map.of("App.car", Files.readAllBytes(app))))));
		var unknown = zip(Map.of("App.car", Files.readAllBytes(app), "unexpected.txt", text("unknown")));
		var invalid = Files.writeString(folder.newFile().toPath(), "not a zip");
		for (var uploads : List.of(List.of(app, duplicate), List.of(cycleA, cycleB), List.of(nested), List.of(unknown), List.of(invalid))) {
			assertThrows(IOException.class, () -> ProjectBundleImport.prepare(uploads, root, Limits.defaults()));
			assertEmpty(root);
			assertEquals("unchanged", Files.readString(sentinel));
			try (var files = Files.list(workspace)) { assertEquals(1, files.count()); }
		}
	}

	@Test public void boundsProjectsUploadsAndTheCombinedExpansionBudget() throws Exception {
		var root = folder.newFolder().toPath();
		var a = car("A", "1", null);
		var b = car("B", "1", null);
		assertThrows(IOException.class, () -> ProjectBundleImport.prepare(List.of(a, b), root, new Limits(1, 100, 100_000, 100_000, 200, 100_000, 2)));
		assertThrows(IOException.class, () -> ProjectBundleImport.prepare(List.of(a, b), root, new Limits(2, 100, 100_000, 100_000, 200, 100_000, 1)));
		assertThrows(IOException.class, () -> ProjectBundleImport.prepare(List.of(a), root, new Limits(2, 100, 100_000, 100_000, 200, 1, 2)));
		var outer = zip(Map.of("A.car", Files.readAllBytes(a), "B.car", Files.readAllBytes(b)));
		long outerExpanded = Files.size(a) + Files.size(b);
		assertThrows(IOException.class, () -> ProjectBundleImport.prepare(List.of(outer), root, new Limits(2, 100, outerExpanded + 1, 100_000, 200, 100_000, 2)));
		assertEmpty(root);
	}

	private Path car(String name, String version, String dependency) throws Exception {
		return zip(Map.of(name + "/c8oProject.yaml", yaml(name, version, dependency), name + "/asset.txt", text("asset")));
	}

	private Path zip(Map<String, byte[]> contents) throws Exception {
		var path = folder.newFile().toPath();
		try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
			for (var entry : contents.entrySet()) {
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue());
				zip.closeEntry();
			}
		}
		return path;
	}

	private static byte[] yaml(String name, String version, String dependency) {
		return text("↑convertigo: 8.0.0.m006\n↓" + name + " [core.Project]: \n  version: " + version + "\n"
				+ (dependency == null ? "" : "  ↓Reference [references.ProjectSchemaReference]: \n    projectName: " + dependency + "\n"));
	}

	private static byte[] text(String text) { return text.getBytes(StandardCharsets.UTF_8); }

	private static void assertEmpty(Path root) throws IOException {
		try (var files = Files.list(root)) { assertEquals(0, files.count()); }
	}
}
