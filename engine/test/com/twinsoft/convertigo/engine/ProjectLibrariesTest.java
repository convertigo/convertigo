/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.ToolProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.util.FileUtils;

/** The class paths of the projects, on libraries compiled for the test: no engine is started. */
public class ProjectLibrariesTest {
	private File base;
	private File libs;
	private File local;

	@BeforeClass
	public static void logs() {
		if (Engine.logEngine == null) {
			Engine.logEngine = org.apache.log4j.Logger.getLogger("project-libraries-test");
		}
	}

	@Before
	public void setUp() throws Exception {
		base = Files.createTempDirectory("project-libraries-test").toFile().getCanonicalFile();
		libs = new File(base, "projects/App/libs");
		libs.mkdirs();
		local = new File(base, "local");
		LocalWorkDirectory.use(LocalWorkDirectory.open(local, "test", name -> true));
		ProjectLibraries.reset();
		ProjectLibraries.checkPeriod = 0;
		ProjectLibraries.releaseGrace = 0;
		ProjectLibraries.leakDelay = 0;
		ProjectLibraries.removedDelay = 0;
		// the tests sweep themselves
		ProjectLibraries.sweepPeriod = TimeUnit.HOURS.toMillis(1);
		ProjectSources.compileAllowed = () -> true;
	}

	@After
	public void tearDown() {
		ProjectLibraries.reset();
		ProjectLibraries.checkPeriod = 5_000;
		ProjectLibraries.releaseGrace = 30_000;
		ProjectLibraries.leakDelay = 600_000;
		ProjectLibraries.removedDelay = 30_000;
		ProjectLibraries.sweepPeriod = 15_000;
		ProjectLibraries.beforeSwap = null;
		ProjectLibraries.reporter = null;
		ProjectSources.compileAllowed = () -> true;
		LocalWorkDirectory.use(null);
		FileUtils.deleteQuietly(base);
	}

	private ProjectClassLoader load() {
		return ProjectLibraries.classLoader("App", () -> List.of(libs), "1.0", true);
	}

	/** Compiles sources (by class name) and adds resources (by path) to a jar, replacing it. */
	private File jar(File jar, Map<String, String> sources, Map<String, String> resources) throws Exception {
		var classes = Files.createTempDirectory(base.toPath(), "classes").toFile();
		compile(sources, classes);
		var temporary = new File(jar.getPath() + ".tmp");
		try (var output = new JarOutputStream(new FileOutputStream(temporary))) {
			for (var file : FileUtils.listFiles(classes, null, true)) {
				output.putNextEntry(new JarEntry(classes.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')));
				output.write(Files.readAllBytes(file.toPath()));
				output.closeEntry();
			}
			for (var resource : resources.entrySet()) {
				output.putNextEntry(new JarEntry(resource.getKey()));
				output.write(resource.getValue().getBytes(StandardCharsets.UTF_8));
				output.closeEntry();
			}
		}
		Files.move(temporary.toPath(), jar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		return jar;
	}

	private File jar(File jar, Map<String, String> sources) throws Exception {
		return jar(jar, sources, Map.of());
	}

	private void compile(Map<String, String> sources, File output) throws Exception {
		var sourceDir = Files.createTempDirectory(base.toPath(), "sources").toFile();
		var files = new ArrayList<File>();
		for (var source : sources.entrySet()) {
			var file = new File(sourceDir, source.getKey().replace('.', '/') + ".java");
			file.getParentFile().mkdirs();
			Files.writeString(file.toPath(), source.getValue());
			files.add(file);
		}
		var compiler = ToolProvider.getSystemJavaCompiler();
		try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
			assertTrue("the test sources compile", compiler.getTask(null, fileManager, null, List.of("-d", output.getPath()), null,
					fileManager.getJavaFileObjectsFromFiles(files)).call());
		}
	}

	private static Map<String, String> hello(String version) {
		return Map.of("lib.Hello", "package lib; public class Hello { public static String version() { return \"" + version + "\"; } }");
	}

	private static String call(Class<?> cls, String method) throws Exception {
		return (String) cls.getMethod(method).invoke(null);
	}

	private static String version(ClassLoader loader) throws Exception {
		return call(loader.loadClass("lib.Hello"), "version");
	}

	@Test
	public void aReplacedJarAppliesToTheNextRequestsWhileARunningOneKeepsItsClasses() throws Exception {
		var jar = jar(new File(libs, "hello.jar"), hello("v1"));
		var running = load();
		var helloOfTheRunningRequest = running.loadClass("lib.Hello");
		assertTrue("the libraries are copied in the local working directory",
				running.getSnapshot().toPath().startsWith(local.toPath()));

		jar(jar, hello("v2-replaced"));
		var next = load();
		assertNotSame("a new generation", running, next);
		assertEquals("v2-replaced", version(next));
		assertEquals("the running request keeps its classes", "v1", call(helloOfTheRunningRequest, "version"));
		assertEquals("and loads new ones from its copy", "v1", version(running));
		assertEquals(1, ProjectLibraries.retiredCount());
	}

	@Test
	public void aProjectLoadedAgainWithTheSameLibrariesKeepsItsGeneration() throws Exception {
		var jar = jar(new File(libs, "hello.jar"), hello("v1"));
		var first = load();
		ProjectLibraries.checkAtNextUse("App");
		assertSame("the same libraries keep their generation", first, load());
		assertTrue(jar.setLastModified(jar.lastModified() + 10_000));
		assertSame("a file touched without change too", first, load());
		assertEquals(0, ProjectLibraries.retiredCount());
	}

	@Test
	public void aChangeInASubFolderOfTheClassesIsDetected() throws Exception {
		var classes = new File(libs, "classes");
		compile(Map.of("deep.inside.Value", "package deep.inside; public class Value { public static String get() { return \"one\"; } }"), classes);
		var first = load();
		assertEquals("one", call(first.loadClass("deep.inside.Value"), "get"));

		compile(Map.of("deep.inside.Value", "package deep.inside; public class Value { public static String get() { return \"second\"; } }"), classes);
		var second = load();
		assertNotSame(first, second);
		assertEquals("second", call(second.loadClass("deep.inside.Value"), "get"));
	}

	@Test
	public void theEngineKeepsItsProtectedPackages() throws Exception {
		var sources = new LinkedHashMap<String, String>(hello("v1"));
		sources.put("org.mozilla.javascript.ContextFactory", "package org.mozilla.javascript; public class ContextFactory { }");
		sources.put("com.twinsoft.convertigo.engine.OnlyInAProject", "package com.twinsoft.convertigo.engine; public class OnlyInAProject { }");
		jar(new File(libs, "embedded.jar"), sources);
		var generation = load();
		assertSame("a protected class comes from the engine", org.mozilla.javascript.ContextFactory.class,
				generation.loadClass("org.mozilla.javascript.ContextFactory"));
		assertSame("a protected package class missing in the engine comes from the project", generation,
				generation.loadClass("com.twinsoft.convertigo.engine.OnlyInAProject").getClassLoader());
		assertSame(generation, generation.loadClass("lib.Hello").getClassLoader());

		var unprotected = new ProjectClassLoader(generation.getURLs(), getClass().getClassLoader(), "App", "1.0", "x", null, List.of());
		assertSame("without protection, the copy of the project comes first, as before", unprotected,
				unprotected.loadClass("org.mozilla.javascript.ContextFactory").getClassLoader());
	}

	@Test
	public void resourcesOfTheProjectComeBeforeThoseOfTheEngine() throws Exception {
		var name = "META-INF/convertigo-test.txt";
		var jar = jar(new File(libs, "resources.jar"), hello("v1"), Map.of(name, "project"));
		var engineDir = new File(base, "engine");
		Files.createDirectories(new File(engineDir, "META-INF").toPath());
		Files.writeString(new File(engineDir, name).toPath(), "engine");
		try (var engine = new URLClassLoader(new URL[] { engineDir.toURI().toURL() }, null)) {
			var generation = new ProjectClassLoader(new URL[] { jar.toURI().toURL() }, engine, "App", "1.0", "x", null, List.of());
			var contents = new ArrayList<String>();
			for (var url : Collections.list(generation.getResources(name))) {
				try (var input = url.openStream()) {
					contents.add(new String(input.readAllBytes(), StandardCharsets.UTF_8));
				}
			}
			assertEquals(List.of("project", "engine"), contents);
			try (var input = generation.getResource(name).openStream()) {
				assertEquals("project", new String(input.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
	}

	@Test
	public void requestsDoNotWaitForANewGeneration() throws Exception {
		var jar = jar(new File(libs, "hello.jar"), hello("v1"));
		var dirs = List.of(libs);
		var first = ProjectLibraries.classLoader("App", () -> dirs, "1.0", false);
		var copied = new CountDownLatch(1);
		var proceed = new CountDownLatch(1);
		ProjectLibraries.beforeSwap = () -> {
			copied.countDown();
			try {
				proceed.await(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		jar(jar, hello("v2-replaced"));
		assertSame("the request that notices the change keeps the current generation", first,
				ProjectLibraries.classLoader("App", () -> dirs, "1.0", false));
		assertTrue("the next generation is built in the background", copied.await(30, TimeUnit.SECONDS));
		var started = System.nanoTime();
		assertSame(first, ProjectLibraries.classLoader("App", () -> dirs, "1.0", false));
		assertTrue("requests answer during the build", System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1));
		proceed.countDown();
		ProjectClassLoader next = first;
		for (int i = 0; i < 300 && next == first; i++) {
			Thread.sleep(100);
			next = ProjectLibraries.classLoader("App", () -> dirs, "1.0", false);
		}
		assertNotSame("the next generation replaces the current one once ready", first, next);
		assertEquals("v2-replaced", version(next));
	}

	@Test
	public void replacedGenerationsAreCollectedAndOneKeptIsReported() throws Exception {
		var reports = new ArrayList<String>();
		ProjectLibraries.reporter = reports::add;
		var jar = jar(new File(libs, "hello.jar"), hello("v1"));
		var first = new WeakReference<>(load());
		var firstSnapshot = first.get().getSnapshot();

		jar(jar, hello("v2-kept"));
		var second = load();
		var kept = second.loadClass("lib.Hello");
		var keptId = second.getId();
		second = null;

		jar(jar, hello("v3-current"));
		assertEquals("v3-current", version(load()));
		assertEquals(2, ProjectLibraries.retiredCount());

		ProjectLibraries.sweep();
		for (int i = 0; i < 100 && first.get() != null; i++) {
			System.gc();
			Thread.sleep(50);
		}
		assertNull("a replaced generation nothing uses is collected", first.get());
		ProjectLibraries.sweep();
		assertFalse("its copy is deleted", firstSnapshot.exists());
		assertEquals("the kept generation is reported", 1, reports.size());
		assertTrue(reports.get(0), reports.get(0).contains(keptId));
		assertEquals(1, ProjectLibraries.retiredCount());
		assertEquals("v2-kept", call(kept, "version"));
	}

	@Test
	public void aRemovedProjectRetiresItsGenerationUnlessItComesBack() throws Exception {
		jar(new File(libs, "hello.jar"), hello("v1"));
		var generation = load();
		ProjectLibraries.projectRemoved("App");
		assertSame("a project deployed again keeps its generation", generation, load());
		ProjectLibraries.sweep();
		assertEquals(0, ProjectLibraries.retiredCount());

		ProjectLibraries.projectRemoved("App");
		ProjectLibraries.sweep();
		assertEquals("a project removed for good retires its generation", 1, ProjectLibraries.retiredCount());
		assertNotSame(generation, load());
	}

	@Test
	public void eachProjectConnectsWithItsOwnDriverVersionAndAReplacedOneIsReleased() throws Exception {
		var libsOther = new File(base, "projects/Other/libs");
		libsOther.mkdirs();
		jar(new File(libs, "driver.jar"), driver("v1"));
		jar(new File(libsOther, "driver.jar"), driver("v2"));
		var app = load();
		var other = ProjectLibraries.classLoader("Other", () -> List.of(libsOther), "1.0", true);
		var jdbc = new JdbcConnectionManager();
		jdbc.init();
		try {
			var drivers = DriverManager.drivers().count();
			assertEquals("v1", connect(jdbc, app));
			assertEquals("v2", connect(jdbc, other));
			assertEquals("the drivers of the projects are registered for their code", drivers + 2, DriverManager.drivers().count());

			jar(new File(libs, "driver.jar"), driver("v3-replaced"));
			var next = load();
			assertEquals("v3-replaced", connect(jdbc, next));
			assertFalse(jdbc.inUse(app));
			var replaced = new WeakReference<>(app);
			app = null;
			ProjectLibraries.sweep();
			assertEquals("the driver of the replaced generation is deregistered", drivers + 2, DriverManager.drivers().count());
			assertEquals("the other project keeps its own", "v2", connect(jdbc, other));
			for (int i = 0; i < 100 && replaced.get() != null; i++) {
				System.gc();
				Thread.sleep(50);
			}
			assertNull("no driver keeps the replaced generation, even the one it registered itself", replaced.get());
		} finally {
			jdbc.destroy();
		}
	}

	private static Map<String, String> driver(String version) {
		return Map.of("test.jdbc.FakeDriver", """
				package test.jdbc;
				import java.lang.reflect.Proxy;
				import java.sql.*;
				import java.util.Properties;
				import java.util.logging.Logger;
				public class FakeDriver implements Driver {
					static {
						// as most JDBC drivers do
						try {
							DriverManager.registerDriver(new FakeDriver());
						} catch (SQLException e) {
							throw new IllegalStateException(e);
						}
					}
					public Connection connect(String url, Properties info) {
						if (!acceptsURL(url)) {
							return null;
						}
						return (Connection) Proxy.newProxyInstance(FakeDriver.class.getClassLoader(), new Class<?>[] { Connection.class },
								(proxy, method, args) -> method.getName().equals("toString") ? "%s" : null);
					}
					public boolean acceptsURL(String url) { return url != null && url.startsWith("jdbc:fake:"); }
					public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
					public int getMajorVersion() { return 1; }
					public int getMinorVersion() { return 0; }
					public boolean jdbcCompliant() { return false; }
					public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
				}
				""".formatted(version));
	}

	private static String connect(JdbcConnectionManager jdbc, ClassLoader generation) throws Exception {
		var thread = Thread.currentThread();
		var previous = thread.getContextClassLoader();
		thread.setContextClassLoader(generation);
		try {
			return jdbc.connectWithoutPool(jdbc.loadDriver("test.jdbc.FakeDriver"), "jdbc:fake:db", "", "").toString();
		} finally {
			thread.setContextClassLoader(previous);
		}
	}

	/** Writes the sources (by class name) and resources (by path) of a src folder, replacing it. */
	private static File src(File libsDir, Map<String, String> sources, Map<String, String> resources) throws Exception {
		var src = new File(libsDir, "src");
		FileUtils.deleteQuietly(src);
		for (var source : sources.entrySet()) {
			var file = new File(src, source.getKey().replace('.', '/') + ".java");
			file.getParentFile().mkdirs();
			Files.writeString(file.toPath(), source.getValue());
		}
		for (var resource : resources.entrySet()) {
			var file = new File(src, resource.getKey());
			file.getParentFile().mkdirs();
			Files.writeString(file.toPath(), resource.getValue());
		}
		return src;
	}

	/** A source using a library of the project and the engine. */
	private static Map<String, String> greeter(String greeting) {
		return Map.of("app.Greeter", """
				package app;
				public class Greeter {
					public static String greet() {
						return "%s " + lib.Hello.version() + " " + com.twinsoft.convertigo.engine.Engine.class.getSimpleName();
					}
				}
				""".formatted(greeting));
	}

	private static final Map<String, String> BROKEN = Map.of("app.Greeter", """
			package app;
			public class Greeter {
				public static String greet() { return missing(); }
			}
			""");

	@Test
	public void sourcesAreCompiledAgainstTheLibrariesAndTheEngineWithTheirResources() throws Exception {
		jar(new File(libs, "hello.jar"), hello("v1"));
		src(libs, greeter("hello"), Map.of("app/greeting.txt", "bonjour"));
		var generation = load();
		assertEquals("hello v1 Engine", call(generation.loadClass("app.Greeter"), "greet"));
		assertNotNull("the other files of src are resources", generation.getResource("app/greeting.txt"));
		assertFalse(generation.toString().contains("without"));
		assertTrue("compiled in the copy", new File(generation.getSnapshot(), "source-0/src.classes/app/Greeter.class").isFile());
		assertFalse("never in the project", new File(libs, "src/app/Greeter.class").exists());
	}

	@Test
	public void aChangedSourceMakesANewGeneration() throws Exception {
		jar(new File(libs, "hello.jar"), hello("v1"));
		var src = src(libs, greeter("hello"), Map.of());
		var first = load();
		assertEquals("hello v1 Engine", call(first.loadClass("app.Greeter"), "greet"));

		src(libs, greeter("salut"), Map.of());
		src.setLastModified(System.currentTimeMillis() + 2000);
		var second = load();
		assertNotSame(first, second);
		assertEquals("salut v1 Engine", call(second.loadClass("app.Greeter"), "greet"));
		assertEquals("loaded again unchanged: the same generation", second, load());
	}

	@Test
	public void sourcesThatDoNotCompileLeaveThePreviousGeneration() throws Exception {
		jar(new File(libs, "hello.jar"), hello("v1"));
		src(libs, greeter("hello"), Map.of());
		var working = load();

		src(libs, BROKEN, Map.of());
		assertSame("the project keeps its generation", working, load());
		assertSame("and does not compile them again until they change", working, load());
		assertEquals("hello v1 Engine", call(working.loadClass("app.Greeter"), "greet"));

		src(libs, greeter("fixed"), Map.of());
		var fixed = load();
		assertNotSame(working, fixed);
		assertEquals("fixed v1 Engine", call(fixed.loadClass("app.Greeter"), "greet"));
	}

	@Test
	public void aFirstLoadWithSourcesThatDoNotCompileUsesTheLibrariesWithoutThem() throws Exception {
		jar(new File(libs, "hello.jar"), hello("v1"));
		src(libs, BROKEN, Map.of());
		var generation = load();
		assertEquals("v1", version(generation));
		assertThrows(ClassNotFoundException.class, () -> generation.loadClass("app.Greeter"));
		assertTrue(generation.toString(), generation.toString().contains("without the classes of its sources"));
	}

	@Test
	public void compilationErrorsTellTheFileAndTheLine() throws Exception {
		var src = src(libs, BROKEN, Map.of());
		var error = assertThrows(ProjectSources.CompilationException.class,
				() -> ProjectSources.compile(src, new File(base, "out"), List.of(), "App/libs/src"));
		assertTrue(error.getErrors().toString(), error.getErrors().get(0).startsWith("App/libs/src/app/Greeter.java:3: "));
	}

	@Test
	public void aServerThatDoesNotBuildTakesTheClassesCompiledFromTheSameSources() throws Exception {
		ProjectSources.compileAllowed = () -> false;
		jar(new File(libs, "hello.jar"), hello("v1"));
		var src = src(libs, greeter("prebuilt"), Map.of());
		var build = new File(libs, "build");
		ProjectSources.compile(src, new File(build, "classes"), List.of(new File(libs, "hello.jar")), "App/libs/src");
		Files.writeString(new File(build, "src.sha256").toPath(), ClasspathSnapshot.sourcesFingerprint(src));
		var generation = load();
		assertEquals("prebuilt v1 Engine", call(generation.loadClass("app.Greeter"), "greet"));

		src(libs, greeter("edited"), Map.of());
		var edited = load();
		assertNotSame(generation, edited);
		assertThrows("classes compiled from other sources are not used",
				ClassNotFoundException.class, () -> edited.loadClass("app.Greeter"));
		assertTrue(edited.toString().contains("without the classes of its sources"));

		ProjectSources.compileAllowed = () -> true;
		ProjectLibraries.checkAtNextUse("App");
		assertEquals("a server that builds compiles them", "edited v1 Engine",
				call(load().loadClass("app.Greeter"), "greet"));
	}

	@Test
	public void classesDeliveredWithoutTheirSourcesAreUsed() throws Exception {
		ProjectSources.compileAllowed = () -> false;
		jar(new File(libs, "hello.jar"), hello("v1"));
		var src = src(new File(base, "elsewhere"), greeter("delivered"), Map.of());
		ProjectSources.compile(src, new File(libs, "build/classes"), List.of(new File(libs, "hello.jar")), "elsewhere");
		assertEquals("delivered v1 Engine", call(load().loadClass("app.Greeter"), "greet"));
	}

	@Test
	public void theSourcesOfAReferencedProjectAreCompiledFirst() throws Exception {
		var libLibs = new File(base, "projects/Lib/libs");
		src(libLibs, Map.of("shared.Util", "package shared; public class Util { public static String name() { return \"util\"; } }"),
				Map.of());
		src(libs, Map.of("app.User", "package app; public class User { public static String use() { return shared.Util.name(); } }"),
				Map.of());
		var generation = ProjectLibraries.classLoader("App", () -> List.of(libs, libLibs), "1.0", true);
		assertEquals("util", call(generation.loadClass("app.User"), "use"));
	}

}
