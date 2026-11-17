package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Budget;
import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Limits;

public class ProjectBundleZipTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void validatesAllNamesBeforeWritingEntries() throws Exception {
		for (var name : List.of("../x", "a/../../x", "/x", "C:\\x", "C:x", "\\\\srv\\x", "//srv/x", "a\\..\\x",
				"a//x", "a/./x", "x\u0001", "x\u0085", "x.", "x ", "CON.txt", "a/aux.log", "a?.txt", "x".repeat(256))) {
			var archive = archive("good.txt", "ok", name, "bad");
			var target = folder.newFolder().toPath();
			assertThrows(name, IOException.class, () -> prepare(archive, target, new Budget(Limits.defaults())));
			try (var files = Files.list(target)) { assertEquals(name, 0, files.count()); }
		}
	}

	@Test public void refusesPortableCollisionsAndFileDirectoryConflicts() throws Exception {
		for (var names : List.of(List.of("Proj/a", "proj/A"), List.of("é/a", "e\u0301/b"), List.of("a", "a/b"), List.of("a/b", "a"))) {
			var archive = archive(names.get(0), "one", names.get(1), "two");
			assertThrows(IOException.class, () -> ProjectBundleZip.inspect(archive, new Budget(Limits.defaults())));
		}
		var archive = archive("a.txt", "one", "b.txt", "two");
		var bytes = Files.readAllBytes(archive);
		for (int i = 0; i + 5 <= bytes.length; i++) {
			if (bytes[i] == 'b' && bytes[i + 1] == '.' && bytes[i + 2] == 't' && bytes[i + 3] == 'x' && bytes[i + 4] == 't') bytes[i] = 'a';
		}
		Files.write(archive, bytes);
		assertThrows(IOException.class, () -> ProjectBundleZip.inspect(archive, new Budget(Limits.defaults())));
	}

	@Test public void checksLocalNamesFlagsAndEntryAttributes() throws Exception {
		for (int change = 0; change < 4; change++) {
			var archive = archive("a.txt", "body");
			var bytes = Files.readAllBytes(archive);
			int central = signature(bytes, 0x02014b50);
			switch (change) {
			case 0: bytes[30] = 'b'; break;
			case 1: bytes[6] ^= 8; break;
			case 2: bytes[central + 5] = 3; put32(bytes, central + 38, 0120777 << 16); break;
			case 3: bytes[central + 8] |= 1; bytes[6] |= 1; break;
			}
			Files.write(archive, bytes);
			assertThrows(IOException.class, () -> ProjectBundleZip.inspect(archive, new Budget(Limits.defaults())));
		}
	}

	@Test public void countsActualEntriesEvenWhenTheDirectoryCountIsSmaller() throws Exception {
		var archive = archive("a", "one", "b", "two");
		var bytes = Files.readAllBytes(archive);
		int end = signature(bytes, 0x06054b50);
		bytes[end + 8] = 1;
		bytes[end + 10] = 1;
		Files.write(archive, bytes);
		var budget = new Budget(new Limits(2, 1, 1024, 1024, 200, 10_000, 2));
		assertThrows(IOException.class, () -> ProjectBundleZip.inspect(archive, budget));
	}

	@Test public void sharesActualExpandedBytesAcrossArchives() throws Exception {
		var first = archive("one.txt", "a".repeat(80));
		var second = archive("two.txt", "b".repeat(80));
		var budget = new Budget(new Limits(2, 20, 100, 100, 200, 10_000, 2));
		prepare(first, folder.newFolder().toPath(), budget);
		assertEquals(80, budget.expandedBytes());
		assertThrows(IOException.class, () -> prepare(second, folder.newFolder().toPath(), budget));
	}

	@Test public void rejectsUnexpectedExpandedSizesAndChecksums() throws Exception {
		for (int change = 0; change < 2; change++) {
			var archive = archive("a.txt", "real content");
			var bytes = Files.readAllBytes(archive);
			int central = signature(bytes, 0x02014b50);
			put32(bytes, central + (change == 0 ? 24 : 16), 1);
			Files.write(archive, bytes);
			assertThrows(IOException.class, () -> prepare(archive, folder.newFolder().toPath(), new Budget(Limits.defaults())));
		}
	}

	@Test public void boundsExpansionRatiosAndIndividualEntries() throws Exception {
		var large = archive("large.txt", "0".repeat(2 * 1024 * 1024));
		assertThrows(IOException.class, () -> prepare(large, folder.newFolder().toPath(), new Budget(Limits.defaults())));
		var smallLimit = new Budget(new Limits(2, 20, 100, 10, 200, 10_000, 2));
		assertThrows(IOException.class, () -> ProjectBundleZip.inspect(archive("a", "x".repeat(11)), smallLimit));
	}

	@Test public void extractsOrdinaryFilesAndKeepsNestedArchivesAsFiles() throws Exception {
		var archive = archive("Project/", "", "Project/c8oProject.yaml", "project", "Project/lib.zip", "ordinary bytes",
				"Project/_private/local.txt", "private", "Project/_data/db.txt", "data");
		var target = folder.newFolder().toPath();
		var budget = new Budget(Limits.defaults());
		var entries = ProjectBundleZip.inspect(archive, budget);
		ProjectBundleZip.extract(archive, target, entries, budget, name -> name.startsWith("Project/_private/") || name.startsWith("Project/_data/"));
		assertEquals("ordinary bytes", Files.readString(target.resolve("Project/lib.zip")));
		assertFalse(Files.exists(target.resolve("Project/_private")));
		assertFalse(Files.exists(target.resolve("Project/_data")));
		assertThrows(IOException.class, () -> ProjectBundleZip.extract(archive, target, entries, budget, name -> false));
	}

	@Test public void acceptsZip64DirectoryAndEntrySizes() throws Exception {
		byte[] name = "a.txt".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		byte[] content = "ZIP64 content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		var checksum = new java.util.zip.CRC32();
		checksum.update(content);
		var bytes = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
		bytes.putInt(0x04034b50).putShort((short) 45).putShort((short) 2048).putShort((short) 0);
		bytes.putInt(0).putInt((int) checksum.getValue()).putInt(-1).putInt(-1);
		bytes.putShort((short) name.length).putShort((short) 20).put(name);
		bytes.putShort((short) 1).putShort((short) 16).putLong(content.length).putLong(content.length).put(content);
		int central = bytes.position();
		bytes.putInt(0x02014b50).putShort((short) 45).putShort((short) 45).putShort((short) 2048).putShort((short) 0);
		bytes.putInt(0).putInt((int) checksum.getValue()).putInt(-1).putInt(-1);
		bytes.putShort((short) name.length).putShort((short) 28).putShort((short) 0).putShort((short) 0).putShort((short) 0);
		bytes.putInt(0).putInt(-1).put(name).putShort((short) 1).putShort((short) 24);
		bytes.putLong(content.length).putLong(content.length).putLong(0);
		int centralSize = bytes.position() - central;
		int zip64 = bytes.position();
		bytes.putInt(0x06064b50).putLong(44).putShort((short) 45).putShort((short) 45).putInt(0).putInt(0);
		bytes.putLong(1).putLong(1).putLong(centralSize).putLong(central);
		bytes.putInt(0x07064b50).putInt(0).putLong(zip64).putInt(1);
		bytes.putInt(0x06054b50).putShort((short) 0).putShort((short) 0).putShort((short) -1).putShort((short) -1);
		bytes.putInt(-1).putInt(-1).putShort((short) 0);
		var archive = folder.newFile().toPath();
		Files.write(archive, java.util.Arrays.copyOf(bytes.array(), bytes.position()));
		var target = folder.newFolder().toPath();
		prepare(archive, target, new Budget(Limits.defaults()));
		assertArrayEquals(content, Files.readAllBytes(target.resolve("a.txt")));
	}

	@Test public void acceptsWindowsEntryAttributesWithoutTreatingThemAsUnixModes() throws Exception {
		var archive = archive("a.txt", "Windows file");
		var bytes = Files.readAllBytes(archive);
		int central = signature(bytes, 0x02014b50);
		bytes[central + 5] = 0;
		put32(bytes, central + 38, (0120777 << 16) | 32);
		Files.write(archive, bytes);
		var target = folder.newFolder().toPath();
		prepare(archive, target, new Budget(Limits.defaults()));
		assertEquals("Windows file", Files.readString(target.resolve("a.txt")));
		assertFalse(Files.isSymbolicLink(target.resolve("a.txt")));
	}

	@Test public void projectsCanonicalEntriesOntoTheProjectDirectoryAndStopsOnWriteFailure() throws Exception {
		var archive = archive("Project/", "", "Project/c8oProject.yaml", "descriptor", "Project/assets/data.zip", "asset");
		var target = folder.newFolder().toPath();
		Files.writeString(target.resolve(".project"), "existing metadata");
		var budget = new Budget(Limits.defaults());
		var entries = ProjectBundleZip.inspect(archive, budget);
		ProjectBundleZip.extractProject(archive, target, entries, budget, "Project");
		assertEquals("descriptor", Files.readString(target.resolve("c8oProject.yaml")));
		assertEquals("asset", Files.readString(target.resolve("assets/data.zip")));
		assertEquals("existing metadata", Files.readString(target.resolve(".project")));
		assertFalse(Files.exists(target.resolve("Project")));
		assertThrows(IOException.class, () -> ProjectBundleZip.extractProject(archive, folder.newFolder().toPath(), entries, new Budget(Limits.defaults()), "Another"));
		var blocked = folder.newFolder().toPath();
		Files.createDirectory(blocked.resolve("c8oProject.yaml"));
		assertThrows(IOException.class, () -> ProjectBundleZip.extractProject(archive, blocked, entries, new Budget(Limits.defaults()), "Project"));
		assertFalse(Files.exists(blocked.resolve("assets/data.zip")));
	}

	private static void prepare(Path archive, Path target, Budget budget) throws IOException {
		var entries = ProjectBundleZip.inspect(archive, budget);
		ProjectBundleZip.extract(archive, target, entries, budget, name -> false);
	}

	private Path archive(String... pairs) throws Exception {
		var path = folder.newFile().toPath();
		try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
			for (int i = 0; i < pairs.length; i += 2) {
				zip.putNextEntry(new ZipEntry(pairs[i]));
				zip.write(pairs[i + 1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return path;
	}

	private static int signature(byte[] bytes, int signature) {
		for (int i = 0; i + 4 <= bytes.length; i++) if (ByteBuffer.wrap(bytes, i, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() == signature) return i;
		throw new AssertionError("signature missing");
	}

	private static void put32(byte[] bytes, int offset, int value) {
		ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
	}
}
