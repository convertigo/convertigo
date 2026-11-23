/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.log4j.Logger;
import org.apache.tools.tar.TarConstants;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.LocalWorkDirectory;

public class NodeArchiveTest {

	private Path temporary, nodes, destination;
	private String root, name, url, version, previousWebapp, previousWorkspace, previousOverride;
	private Engine previousEngine;
	private Logger previousLog;
	private final List<String> messages = new ArrayList<>();
	private final List<String> requests = new ArrayList<>();

	@Before public void setUp() throws Exception {
		temporary = Files.createTempDirectory("manual node ").toRealPath();
		previousWebapp = Engine.WEBAPP_PATH;
		previousWorkspace = Engine.USER_WORKSPACE_PATH;
		previousOverride = System.getProperty("convertigo.node.path");
		previousEngine = Engine.theApp;
		previousLog = Engine.logEngine;
		Engine.WEBAPP_PATH = temporary.resolve("webapp").toString();
		Engine.USER_WORKSPACE_PATH = temporary.resolve("workspace").toString();
		Engine.logEngine = Logger.getLogger("node-archive-test");
		EnginePropertiesManager.initProperties();
		LocalWorkDirectory.release();
		Engine.theApp = new Engine();
		Engine.theApp.httpClient4 = request -> {
			requests.add(request.getURI().toString());
			throw new IOException("Connection blocked by the test fixture");
		};
		System.clearProperty("convertigo.node.path");
		ProcessUtils.setDefaultNodeDir(null);
		version = ProcessUtils.getDefaultNodeVersion();
		destination = ProcessUtils.getDefaultNodeDir().toPath();
		nodes = destination.getParent();
		Files.createDirectories(nodes);
		root = destination.getFileName().toString();
		name = root + (Engine.isWindows() ? ".zip" : ".tar.gz");
		url = "https://nodejs.org/dist/" + version + "/" + name;
	}

	@After public void tearDown() {
		ProcessUtils.setDefaultNodeDir(null);
		LocalWorkDirectory.release();
		EnginePropertiesManager.unload();
		Engine.WEBAPP_PATH = previousWebapp;
		Engine.USER_WORKSPACE_PATH = previousWorkspace;
		Engine.theApp = previousEngine;
		Engine.logEngine = previousLog;
		if (previousOverride == null) System.clearProperty("convertigo.node.path");
		else System.setProperty("convertigo.node.path", previousOverride);
		FileUtils.deleteQuietly(temporary.toFile());
	}

	private Path install() throws Exception {
		return ProcessUtils.getNodeDir(version, null, messages::add).toPath();
	}

	private byte[] tar(String directory, String executableVersion, String link) throws Exception {
		var bytes = new ByteArrayOutputStream();
		try (var gzip = new GZIPOutputStream(bytes); var tar = new TarOutputStream(gzip)) {
			add(tar, directory + "/bin/node", "#!/bin/sh\nprintf '%s\\n' '" + executableVersion + "'\n", 0755);
			add(tar, directory + "/lib/npm-cli.js", "fixture", 0644);
			add(tar, directory + "/LICENSE", "fixture", 0644);
			var entry = new TarEntry(directory + "/bin/npm", TarConstants.LF_SYMLINK);
			entry.setLinkName(link);
			tar.putNextEntry(entry);
			tar.closeEntry();
		}
		return bytes.toByteArray();
	}

	private void add(TarOutputStream tar, String name, String value, int mode) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		var entry = new TarEntry(name);
		entry.setSize(bytes.length);
		entry.setMode(mode);
		tar.putNextEntry(entry);
		tar.write(bytes);
		tar.closeEntry();
	}

	private Path validArchive(boolean checksum) throws Exception {
		org.junit.Assume.assumeFalse("The executable fixture is a POSIX script; ZIP extraction is tested separately", Engine.isWindows());
		var archive = nodes.resolve(name);
		Files.write(archive, tar(root, version, "../lib/npm-cli.js"));
		if (checksum) manifest(archive);
		return archive;
	}

	private String hash(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	private void manifest(Path archive) throws Exception {
		Files.writeString(archive.getParent().resolve("SHASUMS256.txt"), hash(Files.readAllBytes(archive)) + "  " + name + "\n");
	}

	@Test public void manualArchiveAndManifestInstallOfflineAndKeepNpmLink() throws Exception {
		var archive = validArchive(true);
		byte[] before = Files.readAllBytes(archive);
		assertEquals(destination.resolve("bin"), install());
		assertTrue(requests.isEmpty());
		assertArrayEquals(before, Files.readAllBytes(archive));
		assertTrue(Files.isSymbolicLink(destination.resolve("bin/npm")));
		assertTrue(messages.stream().anyMatch(s -> s.startsWith("Using local")));
		assertTrue(messages.stream().anyMatch(s -> s.startsWith("SHA-256 verified")));
		assertTrue(messages.getLast().contains("is ready"));
	}

	@Test public void unavailableChecksumDoesNotBlockTheManualRecovery() throws Exception {
		validArchive(false);
		install();
		assertEquals(List.of("https://nodejs.org/dist/" + version + "/SHASUMS256.txt"), requests);
		assertTrue(messages.stream().anyMatch(s -> s.startsWith("SHA-256 is unavailable")));
		assertFalse(messages.stream().anyMatch(s -> s.startsWith("SHA-256 verified")));
	}

	@Test public void proxyBlockPageForTheChecksumIsReportedAsUnavailable() throws Exception {
		validArchive(false);
		Engine.theApp.httpClient4 = request -> new Response(200, "<html>Download not allowed</html>".getBytes(StandardCharsets.UTF_8));
		install();
		assertTrue(messages.stream().anyMatch(s -> s.startsWith("SHA-256 is unavailable")));
	}

	@Test public void onlineChecksumIsUsedWithoutDownloadingTheLocalArchiveAgain() throws Exception {
		var archive = validArchive(false);
		byte[] checksum = (hash(Files.readAllBytes(archive)) + "  " + name).getBytes(StandardCharsets.UTF_8);
		Engine.theApp.httpClient4 = request -> {
			assertTrue(request.getURI().toString().endsWith("/SHASUMS256.txt"));
			return new Response(200, checksum);
		};
		install();
		assertTrue(messages.stream().anyMatch(s -> s.startsWith("SHA-256 verified")));
	}

	@Test public void wrongChecksumKeepsTheArchiveAndNeverInstallsIt() throws Exception {
		var archive = validArchive(false);
		Files.writeString(nodes.resolve("SHASUMS256.txt"), "0".repeat(64) + "  " + name);
		var failure = assertThrows(IOException.class, this::install);
		assertTrue(failure.getMessage().contains("SHA-256 mismatch"));
		assertTrue(Files.isRegularFile(archive));
		assertFalse(Files.exists(destination));
		assertTrue(requests.isEmpty());
	}

	@Test public void aRenamedArchiveWithAnotherVersionIsRejected() throws Exception {
		var archive = validArchive(false);
		Files.write(archive, tar("node-v0.0.1-test", version, "../lib/npm-cli.js"));
		manifest(archive);
		assertTrue(assertThrows(IOException.class, this::install).getMessage().contains("expected Node.js directory"));
		assertFalse(Files.exists(destination));
		assertTrue(Files.exists(archive));
	}

	@Test public void theExecutableVersionMustMatchEvenWhenTheDirectoryWasRenamed() throws Exception {
		var archive = validArchive(false);
		Files.write(archive, tar(root, "v0.0.1", "../lib/npm-cli.js"));
		manifest(archive);
		assertTrue(assertThrows(IOException.class, this::install).getMessage().contains("does not report " + version));
		assertFalse(Files.exists(destination));
	}

	@Test public void aTruncatedTarNeverPublishesItsPartiallyExtractedExecutable() throws Exception {
		var archive = validArchive(false);
		byte[] bytes = Files.readAllBytes(archive);
		Files.write(archive, java.util.Arrays.copyOf(bytes, bytes.length - 5));
		manifest(archive);
		assertThrows(IOException.class, this::install);
		assertFalse(Files.exists(destination));
		try (var files = Files.list(nodes)) { assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".node-install-"))); }
	}

	@Test public void aTarLinkCannotPointOutsideTheDistribution() throws Exception {
		var archive = validArchive(false);
		Files.write(archive, tar(root, version, "../../outside"));
		manifest(archive);
		assertThrows(IOException.class, this::install);
		assertFalse(Files.exists(destination));
		assertFalse(Files.exists(nodes.resolve("outside")));
	}

	@Test public void failedDownloadGivesExactUrlDirectoryAndKeepsTheNetworkCause() throws Exception {
		var cause = new IOException("Connection refused by the fixture");
		Engine.theApp.httpClient4 = request -> { throw cause; };
		var failure = assertThrows(IOException.class, this::install);
		assertSame(cause, failure.getCause());
		assertTrue(failure.getMessage().contains(url));
		assertTrue(failure.getMessage().contains(nodes.toString()));
		assertTrue(failure.getMessage().contains("Do not unpack it"));
		assertEquals(failure.getMessage(), messages.getLast());
		assertFalse(Files.exists(destination));
	}

	@Test public void httpFailureDoesNotWriteAnErrorPageAsAnArchive() throws Exception {
		Engine.theApp.httpClient4 = request -> new Response(403, "denied".getBytes(StandardCharsets.UTF_8));
		var failure = assertThrows(IOException.class, this::install);
		assertTrue(failure.getMessage().contains("HTTP 403"));
		assertTrue(failure.getMessage().contains(url));
		try (var files = Files.list(nodes)) { assertEquals(0, files.count()); }
	}

	@Test public void interruptedDownloadLeavesNeitherFinalArchiveNorPartialInstallation() throws Exception {
		Engine.theApp.httpClient4 = request -> {
			var response = new Response(200, new byte[0]);
			response.setEntity(new InputStreamEntity(new ByteArrayInputStream(new byte[] {1, 2, 3}), 100));
			return response;
		};
		assertTrue(assertThrows(IOException.class, this::install).getMessage().contains("Incomplete download"));
		try (var files = Files.list(nodes)) { assertEquals(0, files.count()); }
	}

	@Test public void otherVersionsAndPlatformsAreLeftUntouched() throws Exception {
		var other = nodes.resolve("node-v0.0.1-other.zip");
		Files.writeString(other, "another archive");
		var failure = assertThrows(IOException.class, this::install);
		assertEquals(List.of(url), requests);
		assertTrue(failure.getMessage().contains(name));
		assertEquals("another archive", Files.readString(other));
	}

	@Test public void workspaceArchiveIsUsedWhenTheRuntimeLivesInLocalWork() throws Exception {
		var archive = validArchive(true);
		EnginePropertiesManager.setProperty(PropertyName.LOCAL_WORK_DIRECTORY, temporary.resolve("local work").toString());
		LocalWorkDirectory.init();
		var bin = install();
		assertTrue(bin.startsWith(temporary.resolve("local work")));
		assertTrue(Files.exists(archive));
		assertTrue(requests.isEmpty());
	}

	@Test public void anIncompleteDirectoryIsKeptAsideOnlyAfterSuccessfulVerification() throws Exception {
		validArchive(true);
		Files.createDirectories(destination);
		Files.writeString(destination.resolve("unfinished"), "keep");
		install();
		assertFalse(Files.exists(destination.resolve("unfinished")));
		try (var paths = Files.list(nodes)) {
			var previous = paths.filter(p -> p.getFileName().toString().startsWith(root + ".incomplete-")).findFirst().orElseThrow();
			assertEquals("keep", Files.readString(previous.resolve("unfinished")));
		}
	}

	@Test public void anArchiveInLocalWorkIsAcceptedAndTheErrorNamesThatDirectory() throws Exception {
		var archive = validArchive(true);
		EnginePropertiesManager.setProperty(PropertyName.LOCAL_WORK_DIRECTORY, temporary.resolve("local work").toString());
		LocalWorkDirectory.init();
		Path localNodes = ProcessUtils.getDefaultNodeDir().toPath().getParent();
		Files.move(archive, localNodes.resolve(name));
		Files.writeString(localNodes.resolve("SHASUMS256.txt"), "0".repeat(64) + "  " + name);
		var failure = assertThrows(IOException.class, this::install);
		assertTrue(failure.getMessage().contains("unchanged in " + localNodes));
		Files.copy(nodes.resolve("SHASUMS256.txt"), localNodes.resolve("SHASUMS256.txt"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		assertEquals(localNodes.resolve(root).resolve("bin"), install());
		assertTrue(requests.isEmpty());
	}

	@Test public void aSuccessfulDownloadIsRetainedForTheNextLocalInstallation() throws Exception {
		var archive = validArchive(false);
		byte[] bytes = Files.readAllBytes(archive);
		Files.delete(archive);
		byte[] checksums = (hash(bytes) + "  " + name).getBytes(StandardCharsets.UTF_8);
		Engine.theApp.httpClient4 = request -> {
			requests.add(request.getURI().toString());
			return new Response(200, request.getURI().toString().equals(url) ? bytes : checksums);
		};
		install();
		assertArrayEquals(bytes, Files.readAllBytes(archive));
		assertEquals(2, requests.size());
		FileUtils.deleteDirectory(destination.toFile());
		requests.clear();
		install();
		assertEquals(List.of("https://nodejs.org/dist/" + version + "/SHASUMS256.txt"), requests);
	}

	@Test public void olderTemplatesOnAppleSiliconRequestTheExistingIntelDistribution() throws Exception {
		org.junit.Assume.assumeTrue(Engine.isMac() && "aarch64".equals(System.getProperty("os.arch")));
		var failure = assertThrows(IOException.class, () -> ProcessUtils.getNodeDir("v8.9.1"));
		assertTrue(failure.getMessage().contains("https://nodejs.org/dist/v8.9.1/node-v8.9.1-darwin-x64.tar.gz"));
		assertEquals(1, requests.size());
	}

	private Path zip(String... names) throws Exception {
		var file = temporary.resolve("fixture.zip");
		try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
			for (String name : names) {
				zip.putNextEntry(new ZipEntry(name));
				zip.write("fixture".getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return file;
	}

	@Test public void windowsZipLayoutIsExtractedWithoutAnExternalTool() throws Exception {
		String expected = "node-v26.13.1-win-x64";
		var archive = zip(expected + "/node.exe", expected + "/npm.cmd", expected + "/LICENSE");
		var extracted = NodeRuntimeInstaller.extract(archive, temporary.resolve("extract"), expected, true);
		assertEquals("fixture", Files.readString(extracted.resolve("node.exe")));
		assertTrue(Files.isRegularFile(extracted.resolve("npm.cmd")));
	}

	@Test public void windowsZipRejectsAnotherVersionOrAnEntryOutsideItsRoot() throws Exception {
		String expected = "node-v26.13.1-win-x64";
		var wrong = zip("node-v0.0.1-win-x64/node.exe");
		assertThrows(IOException.class, () -> NodeRuntimeInstaller.extract(wrong, temporary.resolve("wrong"), expected, true));
		var outside = zip(expected + "/node.exe", expected + "/../outside.txt");
		assertThrows(IOException.class, () -> NodeRuntimeInstaller.extract(outside, temporary.resolve("escape"), expected, true));
		assertFalse(Files.exists(temporary.resolve("escape/outside.txt")));
	}

	@Test public void windowsZipMustHaveItsCentralDirectory() throws Exception {
		String expected = "node-v26.13.1-win-x64";
		var archive = zip(expected + "/node.exe");
		byte[] bytes = Files.readAllBytes(archive);
		Files.write(archive, java.util.Arrays.copyOf(bytes, bytes.length - 30));
		assertThrows(IOException.class, () -> NodeRuntimeInstaller.extract(archive, temporary.resolve("cut"), expected, true));
	}

	@Test public void aZipWithOnlyTheExecutableIsNotACompleteDistribution() throws Exception {
		String expected = "node-v26.13.1-win-x64";
		var archive = zip(expected + "/node.exe");
		var error = assertThrows(IOException.class, () -> NodeRuntimeInstaller.extract(archive, temporary.resolve("incomplete"), expected, true));
		assertTrue(error.getMessage().contains("npm or LICENSE is missing"));
	}

	private static class Response extends BasicHttpResponse implements CloseableHttpResponse {
		Response(int status, byte[] bytes) {
			super(HttpVersion.HTTP_1_1, status, "fixture");
			setEntity(new ByteArrayEntity(bytes));
		}
		@Override public void close() { }
	}
}
