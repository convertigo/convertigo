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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/** No public network or installed runtime: exercises the actual resolution and archive extraction. */
public class NodeRuntimeTest {
	private Path temporary;
	private String previousWebapp, previousWorkspace, previousOverride;
	private Engine previousEngine;
	private Logger previousLog;
	private final AtomicInteger downloads = new AtomicInteger();

	@Before
	public void setUp() throws Exception {
		temporary = Files.createTempDirectory("node runtime ").toRealPath();
		previousWebapp = Engine.WEBAPP_PATH;
		previousWorkspace = Engine.USER_WORKSPACE_PATH;
		previousOverride = System.getProperty("convertigo.node.path");
		previousEngine = Engine.theApp;
		previousLog = Engine.logEngine;
		Engine.WEBAPP_PATH = temporary.resolve("webapp").toString();
		Engine.USER_WORKSPACE_PATH = temporary.resolve("workspace").toString();
		Engine.logEngine = Logger.getLogger("node-runtime-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.httpClient4 = request -> { throw new AssertionError("Unexpected download: " + request.getURI()); };
		System.clearProperty("convertigo.node.path");
		ProcessUtils.setDefaultNodeDir(null);
	}

	@After
	public void tearDown() {
		ProcessUtils.setDefaultNodeDir(null);
		if (previousOverride == null) System.clearProperty("convertigo.node.path");
		else System.setProperty("convertigo.node.path", previousOverride);
		EnginePropertiesManager.unload();
		Engine.WEBAPP_PATH = previousWebapp;
		Engine.USER_WORKSPACE_PATH = previousWorkspace;
		Engine.theApp = previousEngine;
		Engine.logEngine = previousLog;
		FileUtils.deleteQuietly(temporary.toFile());
	}

	private File runtime(Path root) throws Exception {
		var bin = Engine.isWindows() ? root : root.resolve("bin");
		Files.createDirectories(bin);
		var executable = bin.resolve(Engine.isWindows() ? "node.exe" : "node").toFile();
		Files.writeString(executable.toPath(), "#!/bin/sh\nprintf '%s\\n' '" + ProcessUtils.getDefaultNodeVersion() + "'\n");
		assertTrue(executable.setExecutable(true));
		return bin.toFile();
	}

	private File bundle(String version) throws Exception {
		var root = Path.of(Engine.WEBAPP_PATH, "WEB-INF", "node");
		var bin = runtime(root);
		Files.writeString(root.resolve(".convertigo-node-version"), version + "\n");
		return bin;
	}

	@Test
	public void defaultVersionComesFromTheGeneratedResource() throws Exception {
		var properties = new Properties();
		try (var input = ProcessUtils.class.getResourceAsStream("/com/twinsoft/convertigo/engine/node-runtime.properties")) {
			assertNotNull(input);
			properties.load(input);
		}
		assertEquals(properties.getProperty("version"), ProcessUtils.getDefaultNodeVersion());
		assertTrue(ProcessUtils.getDefaultNodeVersion().matches("v\\d+\\.\\d+\\.\\d+"));
	}

	@Test
	public void bundleWorksWithoutHttpClientAndWithoutWritableCache() throws Exception {
		var bin = bundle(ProcessUtils.getDefaultNodeVersion());
		Engine.theApp = null;
		assertEquals(bin, ProcessUtils.getDefaultNodeDir());
		assertEquals(bin, ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()));
		assertFalse(Files.exists(Path.of(Engine.USER_WORKSPACE_PATH)));
	}

	@Test
	public void overridePrecedesBundleAndAcceptsRootOrBinWithSpaces() throws Exception {
		bundle(ProcessUtils.getDefaultNodeVersion());
		var root = temporary.resolve("custom runtime");
		var bin = runtime(root);
		System.setProperty("convertigo.node.path", root.toString());
		assertEquals(bin, ProcessUtils.getDefaultNodeDir());
		assertEquals(bin, ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()));
		System.setProperty("convertigo.node.path", bin.toString());
		assertEquals(bin, ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()));
	}

	@Test
	public void overrideCanDirectlyContainAnExecutableWithoutBeingNamedBin() throws Exception {
		var directory = temporary.resolve("custom directory");
		Files.createDirectories(directory);
		var node = directory.resolve(Engine.isWindows() ? "node.exe" : "node").toFile();
		Files.writeString(node.toPath(), "fixture");
		assertTrue(node.setExecutable(true));
		System.setProperty("convertigo.node.path", directory.toString());
		assertEquals(directory.toFile(), ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()));
	}

	@Test
	public void badOverrideDoesNotSilentlyDownloadOrUseAnotherRuntime() throws Exception {
		bundle(ProcessUtils.getDefaultNodeVersion());
		System.setProperty("convertigo.node.path", temporary.resolve("missing").toString());
		var error = assertThrows(IllegalArgumentException.class, () -> ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()));
		assertTrue(error.getMessage().contains("convertigo.node.path"));
	}

	@Test
	public void explicitProjectVersionIgnoresTheDefaultOverrideAndBundle() throws Exception {
		var cache = ProcessUtils.getDefaultNodeDir().toPath();
		var version = "v18.13.0";
		var project = new Project() { @Override public String getDirPath() { return temporary.resolve("project").toString(); } };
		var ionic = Path.of(project.getDirPath(), "_private", "ionic");
		Files.createDirectories(ionic);
		Files.writeString(ionic.resolve("version.json"), "{\"nodeJsVersion\":\"" + version + "\"}");
		var bin = runtime(cache.resolveSibling(cache.getFileName().toString().replace(ProcessUtils.getDefaultNodeVersion(), version)));
		bundle(ProcessUtils.getDefaultNodeVersion());
		System.setProperty("convertigo.node.path", temporary.resolve("invalid override").toString());
		assertEquals(version, ProcessUtils.getNodeVersion(project));
		assertEquals(bin, ProcessUtils.getNodeDir(ProcessUtils.getNodeVersion(project)));
	}

	@Test
	public void portableWarReusesItsDownloadedRuntime() throws Exception {
		var bin = runtime(ProcessUtils.getDefaultNodeDir().toPath());
		assertEquals(bin, ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()));
	}

	@Test
	public void olderBundleDoesNotOverrideAnUpdatedWarDefault() throws Exception {
		var bin = runtime(ProcessUtils.getDefaultNodeDir().toPath());
		bundle("v0.0.1");
		assertEquals(bin, ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()));
	}

	@Test
	public void discoveryBeforeEngineStartupDoesNotHideTheBundleLater() throws Exception {
		var webapp = Engine.WEBAPP_PATH;
		Engine.WEBAPP_PATH = "";
		ProcessUtils.getDefaultNodeDir();
		Engine.WEBAPP_PATH = webapp;
		var bin = bundle(ProcessUtils.getDefaultNodeVersion());
		assertEquals(bin, ProcessUtils.getDefaultNodeDir());
	}

	@Test
	public void studioRegistrationDoesNotChangeTheConfiguredVersionOrBundlePriority() throws Exception {
		var version = ProcessUtils.getDefaultNodeVersion();
		ProcessUtils.setDefaultNodeDir(runtime(temporary.resolve("registered")));
		var bin = bundle(version);
		assertEquals(bin, ProcessUtils.getDefaultNodeDir());
		assertEquals(version, ProcessUtils.getDefaultNodeVersion());
	}

	private static class ArchiveResponse extends BasicHttpResponse implements CloseableHttpResponse {
		ArchiveResponse(byte[] bytes) {
			super(HttpVersion.HTTP_1_1, 200, "OK");
			setEntity(new ByteArrayEntity(bytes));
		}
		@Override public void close() { }
	}

	@Test
	public void portableWarDownloadsOnceEvenWithConcurrentFirstUses() throws Exception {
		org.junit.Assume.assumeFalse("The executable fixture is a POSIX script", Engine.isWindows());
		var expectedRoot = ProcessUtils.getDefaultNodeDir().toPath();
		var name = expectedRoot.getFileName().toString();
		var fixture = temporary.resolve("archive contents");
		var bin = runtime(fixture.resolve(name));
		Files.writeString(bin.toPath().resolve("npm"), "fixture");
		Files.writeString(fixture.resolve(name).resolve("LICENSE"), "fixture");
		byte[] bytes;
		if (Engine.isWindows()) {
			var output = new java.io.ByteArrayOutputStream();
			try (var zip = new ZipOutputStream(output)) {
				zip.putNextEntry(new ZipEntry(name + "/node.exe"));
				zip.write(Files.readAllBytes(bin.toPath().resolve("node.exe")));
				zip.closeEntry();
			}
			bytes = output.toByteArray();
		} else {
			var archive = temporary.resolve("runtime.tar.gz");
			var builder = new ProcessBuilder("tar", "-czf", archive.toString(), "-C", fixture.toString(), name);
			builder.environment().put("COPYFILE_DISABLE", "1");
			assertEquals(0, builder.start().waitFor());
			bytes = Files.readAllBytes(archive);
		}
		String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
		Engine.theApp.httpClient4 = request -> {
			if (request.getURI().toString().endsWith("SHASUMS256.txt")) {
				return new ArchiveResponse((hash + "  " + name + ".tar.gz\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
			assertEquals("https://nodejs.org/dist/" + ProcessUtils.getDefaultNodeVersion() + "/" + name + (Engine.isWindows() ? ".zip" : ".tar.gz"), request.getURI().toString());
			downloads.incrementAndGet();
			return new ArchiveResponse(bytes);
		};
		var expected = (Engine.isWindows() ? expectedRoot : expectedRoot.resolve("bin")).toFile();
		try (var executor = Executors.newFixedThreadPool(4)) {
			var calls = new java.util.ArrayList<java.util.concurrent.Future<File>>();
			for (int i = 0; i < 4; i++) calls.add(executor.submit(() -> ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion())));
			for (var call : calls) assertEquals(expected, call.get(30, java.util.concurrent.TimeUnit.SECONDS));
		}
		assertEquals(1, downloads.get());
	}
}
