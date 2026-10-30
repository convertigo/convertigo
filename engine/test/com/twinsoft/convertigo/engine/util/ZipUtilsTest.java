package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;

public class ZipUtilsTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void flushesBinaryEntriesAcrossOutputBufferBoundaries() throws Exception {
		Engine.logEngine = Logger.getLogger("zip-test");
		var archive = folder.newFile("binary.car").toPath();
		var target = folder.newFolder("binary-target").toPath();
		var content = new byte[150_001];
		new java.util.Random(42).nextBytes(content);
		try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			zip.putNextEntry(new ZipEntry("Demo/content.bin"));
			zip.write(content);
			zip.closeEntry();
			zip.putNextEntry(new ZipEntry("Demo/empty.bin"));
			zip.closeEntry();
		}
		ZipUtils.expandZip(archive.toString(), target.toString(), "Demo");
		assertArrayEquals(content, Files.readAllBytes(target.resolve("content.bin")));
		assertEquals(0, Files.size(target.resolve("empty.bin")));
	}

	@Test
	public void extractsSharedDirectoriesAndReplacesExistingFiles() throws Exception {
		Engine.logEngine = Logger.getLogger("zip-test");
		var archive = folder.newFile("project.car").toPath();
		var target = folder.newFolder("target").toPath();
		try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			for (String name : new String[] { "a.txt", "b.txt" }) {
				zip.putNextEntry(new ZipEntry("Demo/nested/" + name));
				zip.write(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		for (int pass = 0; pass < 2; pass++) {
			ZipUtils.expandZip(archive.toString(), target.toString(), "Demo");
			assertEquals("a.txt", Files.readString(target.resolve("nested/a.txt")));
			assertEquals("b.txt", Files.readString(target.resolve("nested/b.txt")));
			Files.writeString(target.resolve("nested/a.txt"), "old content");
		}
	}

	@Test
	public void skipsEntriesThatClimbOutOfTheTargetDirectory() throws Exception {
		var target = folder.newFolder("climb", "target").toPath();
		expand(target, "Demo/kept.txt", "Demo/../escaped.txt", "Demo/nested/../../../escaped-deep.txt");
		assertEquals("Demo/kept.txt", Files.readString(target.resolve("kept.txt")));
		assertFalse(Files.exists(target.resolveSibling("escaped.txt")));
		assertFalse(Files.exists(target.getParent().resolveSibling("escaped-deep.txt")));
	}

	@Test
	public void keepsAnAbsoluteEntryNameInsideTheTargetDirectory() throws Exception {
		var outside = folder.newFolder("absolute-outside").toPath().resolve("absolute.txt");
		var target = folder.newFolder("absolute-target").toPath();
		expand(target, "Demo/kept.txt", "Demo/" + outside);
		assertEquals("Demo/kept.txt", Files.readString(target.resolve("kept.txt")));
		assertFalse(Files.exists(outside));
		assertTrue(Files.isRegularFile(Path.of(target + "/" + outside)));
	}

	@Test
	public void skipsEntriesWrittenThroughALinkToAnOutsideDirectory() throws Exception {
		var outside = folder.newFolder("link-outside").toPath();
		var target = folder.newFolder("link-target").toPath();
		Files.createSymbolicLink(target.resolve("linked"), outside);
		expand(target, "Demo/kept.txt", "Demo/linked/escaped.txt");
		assertEquals("Demo/kept.txt", Files.readString(target.resolve("kept.txt")));
		assertFalse(Files.exists(outside.resolve("escaped.txt")));
	}

	private void expand(Path target, String... entries) throws Exception {
		Engine.logEngine = Logger.getLogger("zip-test");
		var archive = Files.createTempFile(folder.getRoot().toPath(), "traversal-", ".car");
		try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			for (String entry : entries) {
				zip.putNextEntry(new ZipEntry(entry));
				zip.write(entry.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		ZipUtils.expandZip(archive.toString(), target.toString(), "Demo");
	}

	@Test
	public void findsYamlDescriptorAfterUnrelatedEntries() throws Exception {
		checkArchive("MyProject", "other/readme.txt", "MyProject/assets/payload.bin", "MyProject/c8oProject.yaml");
	}

	@Test
	public void preservesLegacyXmlDescriptorAndArchiveOrder() throws Exception {
		checkArchive("Legacy", "wrong/another.xml", "Legacy/Legacy.xml", "Later/c8oProject.yaml");
	}

	@Test
	public void prefersTheRootDescriptorToTheOnesOfInnerFolders() throws Exception {
		// a project shipping a sample project, listed before its own descriptor (as '_' sorts before 'c')
		checkArchive("MyLib", "MyLib/_flow/resources/samples/Sample/c8oProject.yaml", "MyLib/_flow/engine.yaml",
				"MyLib/c8oProject.yaml", "MyLib/resources/Other/Other.xml");
	}

	@Test
	public void keepsAnInnerDescriptorWhenItIsTheOnlyOne() throws Exception {
		checkArchive("repo/projects/Inner", "repo/readme.md", "repo/projects/Inner/c8oProject.yaml");
	}

	@Test
	public void ignoresArchiveFilenameAndAcceptsUnicodeProjectNames() throws Exception {
		checkArchive("Démo", "Démo/c8oProject.yaml");
	}

	@Test
	public void reportsMissingDescriptorAsAnIoException() throws Exception {
		checkArchive(null, "project/readme.txt");
		checkArchive(null);
	}

	private void checkArchive(String expected, String... entries) throws Exception {
		Engine.logEngine = Logger.getLogger("zip-test");
		Path file = Files.createTempFile("unrelated-name-", ".car");
		try {
			try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
				for (String entry : entries) {
					zip.putNextEntry(new ZipEntry(entry));
					zip.write(new byte[8192]);
					zip.closeEntry();
				}
			}
			if (expected == null) {
				assertThrows(IOException.class, () -> ZipUtils.getProjectName(file.toString()));
			} else {
				assertEquals(expected, ZipUtils.getProjectName(file.toString()));
			}
		} finally {
			Files.deleteIfExists(file);
		}
	}
}
