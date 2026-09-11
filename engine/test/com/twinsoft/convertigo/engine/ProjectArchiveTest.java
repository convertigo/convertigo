package com.twinsoft.convertigo.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Regression checks for workspace archive identification and cleanup. */
public class ProjectArchiveTest {
	private static int checks;

	public static void main(String[] args) throws Exception {
		Engine.logEngine = org.apache.log4j.Logger.getLogger("archive-test");
		Engine.logDatabaseObjectManager = Engine.logEngine;
		var previous = Engine.PROJECTS_PATH;
		var root = Files.createTempDirectory("convertigo-archive-test-");
		try {
			var projects = Files.createDirectory(root.resolve("projects"));
			Engine.PROJECTS_PATH = projects.toString();
			var plain = archive(projects, "Demo.car", "Demo/c8oProject.yaml");
			var versioned = archive(projects, "Demo-1.2.3.car", "Demo/c8oProject.yaml");
			archive(projects, "arbitrary.car", "Demo/Demo.xml");
			var other = archive(projects, "Demo-other.car", "Other/c8oProject.yaml");
			var backup = archive(projects, "Demo-backup.zip", "Demo/c8oProject.yaml");
			var broken = Files.writeString(projects.resolve("broken.car"), "not a zip");
			archive(projects, "unrelated.car", "readme.txt");
			var names = DatabaseObjectsManager.findProjectArchives("Demo").stream()
					.map(file -> file.getName()).collect(Collectors.toSet());
			check(names.equals(Set.of("Demo.car", "Demo-1.2.3.car", "arbitrary.car")), "match internal YAML/XML names, not filenames");
			check(DatabaseObjectsManager.findProjectArchives("Dem").isEmpty(), "exact project match only");
			DatabaseObjectsManager.removeDeployedArchive(versioned.toFile());
			check(!Files.exists(versioned), "consume versioned CAR");
			DatabaseObjectsManager.removeDeployedArchive(plain.toFile());
			check(!Files.exists(plain), "consume unversioned CAR");
			DatabaseObjectsManager.removeDeployedArchive(plain.toFile());
			check(!Files.exists(plain), "cleanup is idempotent");
			DatabaseObjectsManager.removeDeployedArchive(backup.toFile());
			check(Files.exists(backup), "retain ZIP backups");
			var external = archive(root, "external.car", "Demo/c8oProject.yaml");
			DatabaseObjectsManager.removeDeployedArchive(external.toFile());
			check(Files.exists(external), "retain archives outside workspace projects");
			var link = projects.resolve("external-link.car");
			Files.createSymbolicLink(link, external);
			DatabaseObjectsManager.removeDeployedArchive(link.toFile());
			check(Files.exists(link) && Files.exists(external), "do not consume external symlink sources");
			check(Files.exists(other) && Files.exists(broken), "retain unrelated and unreadable archives");
			try {
				new DatabaseObjectsManager().deployProject(broken.toString(), null, true);
				throw new AssertionError("invalid archive must fail deployment");
			} catch (EngineException expected) {
				check(Files.exists(broken), "failed deployment preserves input");
			}
			// Exercise the real extraction/deployment paths, isolating only the project importer.
			var manager = new DatabaseObjectsManager() {
				@Override
				public com.twinsoft.convertigo.beans.core.Project importProject(java.io.File file, boolean override)
						throws EngineException {
					if (file.getParentFile().getName().equals("ImportFails")) {
						throw new EngineException("simulated import failure");
					}
					if (file.getParentFile().getName().equals("ImportNull")) return null;
					return new com.twinsoft.convertigo.beans.core.Project();
				}
			};
			var success = archive(projects, "Uploaded-3.1.car", "Uploaded/c8oProject.yaml");
			check(manager.deployProject(success.toString(), null, true) != null && !Files.exists(success), "successful deployment consumes upload");
			var startup = archive(projects, "Startup-2.0.car", "Startup/c8oProject.yaml");
			check(manager.updateProject(startup.toFile()) != null && !Files.exists(startup), "startup path consumes archive");
			var skipped = archive(projects, "Startup-3.0.car", "Startup/c8oProject.yaml");
			check(manager.updateProject(skipped.toFile()) == null && Files.exists(skipped), "skipped deployment retains archive");
			var nullImport = archive(projects, "ImportNull.car", "ImportNull/c8oProject.yaml");
			check(manager.deployProject(nullImport.toString(), null, true) == null && Files.exists(nullImport), "null import retains archive");
			var failedImport = archive(projects, "ImportFails.car", "ImportFails/c8oProject.yaml");
			try {
				manager.deployProject(failedImport.toString(), null, true);
				throw new AssertionError("import must fail");
			} catch (EngineException expected) {
				check(Files.exists(failedImport), "import failure retains extracted archive");
			}
			System.out.println("Project archives: " + checks + " checks passed");
		} finally {
			Engine.PROJECTS_PATH = previous;
			try (var paths = Files.walk(root)) {
				for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(path);
				}
			}
		}
	}

	private static Path archive(Path directory, String filename, String entry) throws Exception {
		var file = directory.resolve(filename);
		try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
			zip.putNextEntry(new ZipEntry(entry));
			zip.write("test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return file;
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
		checks++;
	}
}
