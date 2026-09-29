package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
	public void findsYamlDescriptorAfterUnrelatedEntries() throws Exception {
		checkArchive("MyProject", "other/readme.txt", "MyProject/assets/payload.bin", "MyProject/c8oProject.yaml");
	}

	@Test
	public void preservesLegacyXmlDescriptorAndArchiveOrder() throws Exception {
		checkArchive("Legacy", "wrong/another.xml", "Legacy/Legacy.xml", "Later/c8oProject.yaml");
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
