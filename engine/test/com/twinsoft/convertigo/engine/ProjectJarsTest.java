/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.engine.util.FileUtils;

/**
 * The jars a libs folder declares in jars.yaml: downloaded from Maven repositories or plain URLs and checked against
 * their sha256, kept in a cache, taken from the archive of the project when the engine does not download, and given to
 * the class path of the project.
 */
public class ProjectJarsTest {
	private File base;
	private File libs;
	private HttpServer server;
	private String root;
	/** path -> content served */
	private final Map<String, byte[]> served = new ConcurrentHashMap<>();
	private final List<String> requests = new ArrayList<>();
	private final List<String> authorizations = new ArrayList<>();

	@BeforeClass
	public static void logs() {
		if (Engine.logEngine == null) {
			Engine.logEngine = org.apache.log4j.Logger.getLogger("project-jars-test");
		}
	}

	@Before
	public void setUp() throws Exception {
		base = Files.createTempDirectory("project-jars-test").toFile().getCanonicalFile();
		libs = new File(base, "projects/App/libs");
		libs.mkdirs();
		var cache = new File(base, "cache");
		ProjectJars.cache = () -> cache;
		ProjectJars.downloadAllowed = () -> true;
		ProjectJars.retryDelay = TimeUnit.HOURS.toMillis(1);
		ProjectJars.reset();
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			var path = exchange.getRequestURI().getPath();
			synchronized (requests) {
				requests.add(path);
				authorizations.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
			}
			var content = served.get(path);
			if (content == null) {
				exchange.sendResponseHeaders(404, -1);
			} else {
				exchange.sendResponseHeaders(200, content.length);
				exchange.getResponseBody().write(content);
			}
			exchange.close();
		});
		server.start();
		root = "http://127.0.0.1:" + server.getAddress().getPort();
		ProjectLibraries.reset();
		ProjectLibraries.checkPeriod = 0;
		ProjectSources.compileAllowed = () -> true;
	}

	@After
	public void tearDown() {
		server.stop(0);
		ProjectJars.cache = () -> new File(Engine.USER_WORKSPACE_PATH, "libs/.jars");
		ProjectJars.downloadAllowed = ProjectSources::mayCompile;
		ProjectJars.retryDelay = 10 * 60_000;
		ProjectJars.reset();
		ProjectLibraries.reset();
		ProjectLibraries.checkPeriod = 5_000;
		FileUtils.deleteQuietly(base);
	}

	private static byte[] jar(String resource, String text) throws IOException {
		var bytes = new ByteArrayOutputStream();
		try (var jar = new JarOutputStream(bytes)) {
			jar.putNextEntry(new JarEntry(resource));
			jar.write(text.getBytes(StandardCharsets.UTF_8));
			jar.closeEntry();
		}
		return bytes.toByteArray();
	}

	private static String sha256(byte[] content) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
	}

	private void descriptor(String yaml) throws IOException {
		var file = new File(libs, ProjectJars.DESCRIPTOR);
		Files.writeString(file.toPath(), yaml);
		// another content is another descriptor, even in the same second
		file.setLastModified(System.currentTimeMillis() + requests.size() * 1000L);
	}

	private int requestCount() {
		synchronized (requests) {
			return requests.size();
		}
	}

	@Test
	public void aMavenJarComesFromTheFirstRepositoryThatHasItAndThenFromTheCache() throws Exception {
		var content = jar("probe.txt", "querydsl");
		served.put("/second/com/example/probe/1.0/probe-1.0.jar", content);
		descriptor("repositories:\n  - " + root + "/first\n  - " + root + "/second/\njars:\n"
				+ "  - maven: com.example:probe:1.0\n    sha256: " + sha256(content) + "\n");
		var files = ProjectJars.resolve(libs);
		assertEquals(1, files.size());
		assertEquals("probe-1.0.jar", files.get(0).getName());
		assertEquals(List.of("/first/com/example/probe/1.0/probe-1.0.jar", "/second/com/example/probe/1.0/probe-1.0.jar"), requests);
		var count = requestCount();
		assertEquals(files, ProjectJars.resolve(libs));
		assertEquals("cached: not downloaded again", count, requestCount());
	}

	@Test
	public void aPlainUrlWithCredentialsIsDownloadedWithBasicAuthentication() throws Exception {
		var content = jar("probe.txt", "driver");
		served.put("/files/driver-2.jar", content);
		var url = root.replace("http://", "http://reader:s3cr%40t@") + "/files/driver-2.jar";
		descriptor("jars:\n  - url: " + url + "\n    sha256: " + sha256(content) + "\n");
		var files = ProjectJars.resolve(libs);
		assertEquals("driver-2.jar", files.get(0).getName());
		assertEquals("Basic " + Base64.getEncoder().encodeToString("reader:s3cr@t".getBytes(StandardCharsets.UTF_8)), authorizations.get(0));
		assertEquals("http://***@127.0.0.1:1/x.jar", ProjectJars.redacted("http://reader:secret@127.0.0.1:1/x.jar"));
	}

	@Test
	public void aWrongSha256IsRefusedAndNotTriedAgainUntilTheDescriptorChanges() throws Exception {
		var content = jar("probe.txt", "tampered");
		served.put("/files/probe.jar", content);
		descriptor("jars:\n  - url: " + root + "/files/probe.jar\n    sha256: \"" + "0".repeat(64) + "\"\n");
		try {
			ProjectJars.resolve(libs);
			fail("a wrong sha256 must be refused");
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("sha256 " + sha256(content)));
		}
		var count = requestCount();
		try {
			ProjectJars.resolve(libs);
			fail("still refused");
		} catch (IOException e) {
			assertEquals("the failure is kept, nothing downloaded", count, requestCount());
		}
		descriptor("jars:\n  - url: " + root + "/files/probe.jar\n    sha256: " + sha256(content) + "\n");
		assertEquals(1, ProjectJars.resolve(libs).size());
	}

	@Test
	public void aSha256ReadAsANumberIsRefused() throws Exception {
		descriptor("jars:\n  - url: " + root + "/files/probe.jar\n    sha256: " + "1".repeat(64) + "\n");
		try {
			ProjectJars.resolve(libs);
			fail("an unquoted numeric sha256 must be refused");
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("quote the sha256"));
		}
	}

	@Test
	public void aJarWithoutSha256IsRefusedWithTheSha256ToCheck() throws Exception {
		var content = jar("probe.txt", "new");
		served.put("/files/new.jar", content);
		descriptor("jars:\n  - url: " + root + "/files/new.jar\n");
		try {
			ProjectJars.resolve(libs);
			fail("a jar without sha256 must be refused");
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains(sha256(content)));
		}
	}

	@Test
	public void withoutDownloadTheJarsOfTheArchiveAreTakenWhenTheirSha256Match() throws Exception {
		ProjectJars.downloadAllowed = () -> false;
		var content = jar("probe.txt", "embedded");
		var embedded = new File(libs, ProjectJars.EMBEDDED + "/probe-1.0.jar");
		embedded.getParentFile().mkdirs();
		Files.write(embedded.toPath(), content);
		descriptor("jars:\n  - maven: com.example:probe:1.0\n    sha256: " + sha256(content) + "\n");
		assertEquals(List.of(embedded), ProjectJars.resolve(libs));
		assertEquals("never downloaded", 0, requestCount());

		descriptor("jars:\n  - maven: com.example:probe:1.0\n    sha256: \"" + "1".repeat(64) + "\"\n");
		try {
			ProjectJars.resolve(libs);
			fail("another sha256 must not take the jar of the archive");
		} catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("server_build none"));
		}
	}

	@Test
	public void theClassPathOfTheProjectGetsTheDeclaredJars() throws Exception {
		var content = jar("declared.txt", "declared");
		served.put("/maven/com/example/declared/2.0/declared-2.0.jar", content);
		descriptor("repositories:\n  - " + root + "/maven\njars:\n  - maven: com.example:declared:2.0\n    sha256: "
				+ sha256(content) + "\n");
		var loader = ProjectLibraries.classLoader("App", () -> List.of(libs), "1.0", true);
		assertNotNull("a resource of the declared jar", loader.getResource("declared.txt"));

		// a jar that cannot be resolved keeps the previous generation
		descriptor("repositories:\n  - " + root + "/maven\njars:\n  - maven: com.example:missing:1.0\n    sha256: "
				+ "a".repeat(64) + "\n");
		var kept = ProjectLibraries.classLoader("App", () -> List.of(libs), "1.0", true);
		assertTrue("the previous libraries are kept", kept == loader);
		assertNull(kept.getResource("missing.txt"));
	}
}
