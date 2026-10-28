/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.LocalWorkDirectory;

/**
 * The disk cache of the classes Rhino generates (RhinoBytecodeCache): a script loaded from it runs as the one
 * compileString gives; a damaged, foreign or outdated entry is compiled again and replaced; threads and JVMs share a
 * folder without corruption; the folder stays bounded; nothing is written when the cache does not apply.
 */
public class RhinoBytecodeCacheTest {
	private static final String MODE_PROPERTY = "convertigo.engine.javascript.bytecode_cache";
	private static final byte[] KEY = "test-key-test-key-test-key-12345".getBytes(StandardCharsets.UTF_8);

	private Path folder;
	private Context cx;

	@Before public void setUp() throws Exception {
		folder = Files.createTempDirectory("rhino-bytecode-test");
		RhinoBytecodeCache.reset();
		cx = enter();
	}

	@After public void tearDown() throws Exception {
		Context.exit();
		RhinoBytecodeCache.reset();
		System.clearProperty(MODE_PROPERTY);
		FileUtils.deleteQuietly(folder.toFile());
	}

	private static Context enter() {
		var cx = ContextFactory.getGlobal().enterContext();
		cx.setLanguageVersion(Context.VERSION_ES6);
		cx.setInterpretedMode(false);
		return cx;
	}

	private RhinoBytecodeCache cache() {
		return new RhinoBytecodeCache(folder, RhinoBytecodeCache.DEFAULT_MAX_BYTES, KEY);
	}

	private static String run(Context cx, Script script) {
		Scriptable scope = cx.initStandardObjects();
		return Context.toString(script.exec(cx, scope, scope));
	}

	private String expected(String source, String name) {
		return run(cx, cx.compileString(source, name, 1, null));
	}

	private static String source(int i) {
		return "function f" + i + "(a) { var r = /x(\\d+)/.exec('x' + a); return `${r[1]}-` + [a, " + i + "].map(function (v) { return v * 2; }).join('+'); }\nf" + i + "(" + (i + 3) + ")";
	}

	private List<Path> entries() throws Exception {
		try (var files = Files.list(folder)) {
			return files.filter(f -> f.getFileName().toString().endsWith(".rbc")).sorted().toList();
		}
	}

	private String keyOf(String source, String name) {
		var env = new CompilerEnvirons();
		env.initFromContext(cx);
		return RhinoBytecodeCache.key(RhinoBytecodeCache.options(cx, env), name, source);
	}

	@Test public void environmentNamesRhinoThePatchAndTheFormat() {
		var environment = RhinoBytecodeCache.environmentForTests();
		assertNotNull("the patched ClassCompiler is on the class path", environment);
		assertTrue(environment, environment.contains(";patch=c8o-1;format=" + RhinoBytecodeCache.FORMAT));
		assertEquals(2, RhinoBytecodeCache.FORMAT);
	}

	@Test public void keyDependsOnSourceNameAndOptions() throws Exception {
		var env = new CompilerEnvirons();
		env.initFromContext(cx);
		var options = RhinoBytecodeCache.options(cx, env);
		var key = RhinoBytecodeCache.key(options, "a.js", "1 + 1");
		assertEquals(64, key.length());
		assertEquals(key, RhinoBytecodeCache.key(options, "a.js", "1 + 1"));
		assertNotEquals(key, RhinoBytecodeCache.key(options, "a.js", "1 + 2"));
		assertNotEquals(key, RhinoBytecodeCache.key(options, "b.js", "1 + 1"));
		// the name and the source cannot exchange characters
		assertNotEquals(RhinoBytecodeCache.key(options, "a.js\n1", "+ 1"), RhinoBytecodeCache.key(options, "a.js", "1\n+ 1"));
		// read by the code generation from the current context
		var factory = new ContextFactory() {
			@Override
			protected boolean hasFeature(Context c, int feature) {
				return feature == Context.FEATURE_ENHANCED_JAVA_ACCESS || super.hasFeature(c, feature);
			}
		};
		var other = Executors.newSingleThreadExecutor();
		try {
			assertNotEquals(key, other.submit(() -> {
				var enhanced = factory.enterContext();
				try {
					enhanced.setLanguageVersion(Context.VERSION_ES6);
					enhanced.setInterpretedMode(false);
					var enhancedEnv = new CompilerEnvirons();
					enhancedEnv.initFromContext(enhanced);
					return RhinoBytecodeCache.key(RhinoBytecodeCache.options(enhanced, enhancedEnv), "a.js", "1 + 1");
				} finally {
					Context.exit();
				}
			}).get());
		} finally {
			other.shutdown();
		}
		cx.setLanguageVersion(Context.VERSION_1_8);
		env.initFromContext(cx);
		assertNotEquals(key, RhinoBytecodeCache.key(RhinoBytecodeCache.options(cx, env), "a.js", "1 + 1"));
		cx.setInterpretedMode(true);
		env.initFromContext(cx);
		assertNull("interpreted code is not cached", RhinoBytecodeCache.options(cx, env));
	}

	@Test public void loadedScriptsRunAsCompiledOnes() throws Exception {
		for (int i = 0; i < 5; i++) {
			assertEquals(expected(source(i), "s" + i + ".js"), run(cx, cache().get(cx, source(i), "s" + i + ".js", null)));
		}
		RhinoBytecodeCache.flush();
		assertEquals(5, entries().size());
		assertEquals(5, RhinoBytecodeCache.statistics.misses.get());
		for (int i = 0; i < 5; i++) {
			assertEquals(expected(source(i), "s" + i + ".js"), run(cx, cache().get(cx, source(i), "s" + i + ".js", null)));
		}
		assertEquals(5, RhinoBytecodeCache.statistics.hits.get());
		assertEquals(0, RhinoBytecodeCache.statistics.invalid.get());
	}

	@Test public void errorsStayTheSameAndAreNotCached() throws Exception {
		String expected = null;
		try {
			cx.compileString("var a = ;", "broken.js", 1, null);
			fail();
		} catch (EvaluatorException e) {
			expected = e.getMessage();
		}
		try {
			cache().get(cx, "var a = ;", "broken.js", null);
			fail();
		} catch (EvaluatorException e) {
			assertEquals(expected, e.getMessage());
		}
		RhinoBytecodeCache.flush();
		assertTrue(entries().isEmpty());
	}

	/** Beyond the class file limits, Rhino compiles with its interpreter: the entry only says so. */
	@Test public void sourcesBeyondClassFileLimitsAreInterpretedAsBefore() throws Exception {
		var huge = new StringBuilder("function huge() { var a = 0;\n");
		for (int i = 0; i < 12000; i++) {
			huge.append("a = a + ").append(i % 7).append(";\n");
		}
		huge.append("return a; }\nhuge()");
		var source = huge.toString();
		var expected = expected(source, "huge.js");
		assertEquals(expected, run(cx, cache().get(cx, source, "huge.js", null)));
		RhinoBytecodeCache.flush();
		assertEquals(1, entries().size());
		assertTrue("only a marker", Files.size(entries().get(0)) < 1024);
		assertEquals(expected, run(cx, cache().get(cx, source, "huge.js", null)));
		assertEquals(1, RhinoBytecodeCache.statistics.hits.get());
	}

	private Path entryOf(String source, String name) {
		return folder.resolve(keyOf(source, name) + ".rbc");
	}

	private void assertReplaced(String source, String name, byte[] damaged) throws Exception {
		var entry = entryOf(source, name);
		Files.write(entry, damaged);
		var invalid = RhinoBytecodeCache.statistics.invalid.get();
		assertEquals(expected(source, name), run(cx, cache().get(cx, source, name, null)));
		assertEquals("rejected", invalid + 1, RhinoBytecodeCache.statistics.invalid.get());
		RhinoBytecodeCache.flush();
		assertFalse("replaced", java.util.Arrays.equals(damaged, Files.readAllBytes(entry)));
		var hits = RhinoBytecodeCache.statistics.hits.get();
		assertEquals(expected(source, name), run(cx, cache().get(cx, source, name, null)));
		assertEquals("valid again", hits + 1, RhinoBytecodeCache.statistics.hits.get());
	}

	@Test public void damagedEntriesAreCompiledAgainAndReplaced() throws Exception {
		var source = source(1);
		cache().get(cx, source, "d.js", null);
		RhinoBytecodeCache.flush();
		var good = Files.readAllBytes(entryOf(source, "d.js"));
		// a flipped bit in the classes
		var flipped = good.clone();
		flipped[good.length / 2] ^= 0x10;
		assertReplaced(source, "d.js", flipped);
		// truncated, empty, garbage
		assertReplaced(source, "d.js", java.util.Arrays.copyOf(good, good.length - 7));
		assertReplaced(source, "d.js", new byte[0]);
		assertReplaced(source, "d.js", "not an entry".getBytes(StandardCharsets.UTF_8));
	}

	@Test public void entriesNotSignedByThisInstallationAreReplaced() throws Exception {
		var source = source(2);
		var other = new RhinoBytecodeCache(folder, RhinoBytecodeCache.DEFAULT_MAX_BYTES, "another-installation-key-0123456".getBytes(StandardCharsets.UTF_8));
		other.get(cx, source, "planted.js", null);
		RhinoBytecodeCache.flush();
		assertReplaced(source, "planted.js", Files.readAllBytes(entryOf(source, "planted.js")));
	}

	@Test public void entriesOfAnotherSourceAreReplaced() throws Exception {
		cache().get(cx, source(3), "a.js", null);
		RhinoBytecodeCache.flush();
		// signed by this installation, but for another source: renamed as the entry of this one
		assertReplaced(source(4), "a.js", Files.readAllBytes(entryOf(source(3), "a.js")));
	}

	/** Signed by this installation but refused by the JVM: compiled as before, then no longer tried until it changes. */
	@Test public void classesThatCannotBeLoadedAreNoLongerTried() throws Exception {
		var source = source(9);
		var key = keyOf(source, "unloadable.js");
		var main = RhinoBytecodeCache.mainClassName("unloadable.js", key);
		Files.write(entryOf(source, "unloadable.js"), RhinoBytecodeCache.encode(key, main, RhinoBytecodeCache.KIND_CLASSES, 1,
				new Object[] { main, new byte[] { (byte) 0xCA, (byte) 0xFE, 0, 0 } }, KEY));
		assertEquals(expected(source, "unloadable.js"), run(cx, cache().get(cx, source, "unloadable.js", null)));
		assertEquals(1, RhinoBytecodeCache.statistics.invalid.get());
		RhinoBytecodeCache.flush();
		assertTrue("only a marker", Files.size(entryOf(source, "unloadable.js")) < 1024);
		assertEquals(expected(source, "unloadable.js"), run(cx, cache().get(cx, source, "unloadable.js", null)));
		assertEquals(1, RhinoBytecodeCache.statistics.invalid.get());
		assertEquals(1, RhinoBytecodeCache.statistics.hits.get());
	}

	@Test public void concurrentThreadsShareTheFolder() throws Exception {
		int threads = 8, sources = 12;
		var pool = Executors.newFixedThreadPool(threads);
		try {
			for (int round = 0; round < 2; round++) {
				var gate = new CyclicBarrier(threads);
				var calls = new ArrayList<Future<?>>();
				for (int t = 0; t < threads; t++) {
					calls.add(pool.submit(() -> {
						var local = enter();
						try {
							gate.await();
							for (int i = 0; i < sources; i++) {
								var script = cache().get(local, source(i), "c" + i + ".js", null);
								assertEquals((i + 3) + "-" + (2 * (i + 3)) + "+" + (2 * i), run(local, script));
							}
							return null;
						} finally {
							Context.exit();
						}
					}));
				}
				for (var call : calls) {
					call.get(60, TimeUnit.SECONDS);
				}
				RhinoBytecodeCache.flush();
			}
		} finally {
			pool.shutdownNow();
		}
		assertEquals(sources, entries().size());
		assertEquals(0, RhinoBytecodeCache.statistics.invalid.get());
		assertTrue(RhinoBytecodeCache.statistics.hits.get() >= threads * sources);
		try (var files = Files.list(folder)) {
			assertEquals("no temporary file left", sources, files.count());
		}
	}

	/** Several JVMs compiling the same sources into the same folder at the same time. */
	@Test public void concurrentJvmsShareTheFolder() throws Exception {
		var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		var processes = new ArrayList<Process>();
		for (int p = 0; p < 3; p++) {
			processes.add(new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), Child.class.getName(), folder.toString())
					.redirectErrorStream(true).start());
		}
		for (var process : processes) {
			var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(process.waitFor(120, TimeUnit.SECONDS));
			assertEquals(output, 0, process.exitValue());
		}
		assertEquals(Child.SOURCES, entries().size());
		for (int i = 0; i < Child.SOURCES; i++) {
			assertEquals((i + 3) + "-" + (2 * (i + 3)) + "+" + (2 * i), run(cx, cache().get(cx, source(i), "j" + i + ".js", null)));
		}
		assertEquals(Child.SOURCES, RhinoBytecodeCache.statistics.hits.get());
		assertEquals(0, RhinoBytecodeCache.statistics.invalid.get());
	}

	public static final class Child {
		static final int SOURCES = 16;

		public static void main(String[] args) throws Exception {
			var folder = Path.of(args[0]);
			var cx = enter();
			try {
				for (int round = 0; round < 3; round++) {
					for (int i = 0; i < SOURCES; i++) {
						var script = new RhinoBytecodeCache(folder, RhinoBytecodeCache.DEFAULT_MAX_BYTES, KEY).get(cx, source(i), "j" + i + ".js", null);
						var result = run(cx, script);
						if (!result.equals((i + 3) + "-" + (2 * (i + 3)) + "+" + (2 * i))) {
							System.out.println("wrong result " + result);
							System.exit(1);
						}
					}
					RhinoBytecodeCache.flush();
				}
				System.out.println(RhinoBytecodeCache.statistics);
				System.exit(RhinoBytecodeCache.statistics.invalid.get() == 0 ? 0 : 2);
			} finally {
				Context.exit();
			}
		}
	}

	@Test public void folderStaysBoundedReadEntriesFirst() throws Exception {
		cache().get(cx, source(0), "e0.js", null);
		RhinoBytecodeCache.flush();
		var size = Files.size(entries().get(0));
		var bounded = new RhinoBytecodeCache(folder, size * 10, KEY);
		for (int i = 0; i < 20; i++) {
			bounded.get(cx, source(i), "e" + i + ".js", null);
		}
		RhinoBytecodeCache.flush();
		// oldest first: the entries e0..e9 are made older, e3 read recently
		var now = System.currentTimeMillis();
		for (int i = 0; i < 20; i++) {
			var age = i == 3 ? 0 : (20 - i) * 60_000L;
			Files.setLastModifiedTime(entryOf(source(i), "e" + i + ".js"), FileTime.fromMillis(now - age));
		}
		var stale = folder.resolve(".left-by-a-crash.tmp");
		Files.write(stale, new byte[] { 1 });
		Files.setLastModifiedTime(stale, FileTime.fromMillis(now - TimeUnit.HOURS.toMillis(2)));
		var recent = folder.resolve(".being-written.tmp");
		Files.write(recent, new byte[] { 1 });
		bounded.evict(folder);
		long total = 0;
		for (var entry : entries()) {
			total += Files.size(entry);
		}
		assertTrue(total + " <= " + size * 10, total <= size * 10);
		assertTrue("read recently", Files.exists(entryOf(source(3), "e3.js")));
		assertFalse("read least recently", Files.exists(entryOf(source(0), "e0.js")));
		assertTrue("newest", Files.exists(entryOf(source(19), "e19.js")));
		assertFalse(Files.exists(stale));
		assertTrue(Files.exists(recent));
		assertTrue(RhinoBytecodeCache.statistics.evictions.get() > 0);
	}

	/** The entries of another Rhino, patch or format are never read again: the first scan removes them. */
	@Test public void entriesOfAnotherEnvironmentAreRemoved() throws Exception {
		cache().get(cx, source(11), "kept.js", null);
		RhinoBytecodeCache.flush();
		var kept = entryOf(source(11), "kept.js");
		var older = Files.readAllBytes(kept);
		older[7] = 1;
		var formerFormat = folder.resolve("former-format.rbc");
		Files.write(formerFormat, older);
		var otherRhino = folder.resolve("other-rhino.rbc");
		var other = new java.io.ByteArrayOutputStream();
		var out = new java.io.DataOutputStream(other);
		out.write(java.util.Arrays.copyOf(Files.readAllBytes(kept), 8));
		out.writeUTF("rhino=2.0;classes=0;patch=c8o-9;format=" + RhinoBytecodeCache.FORMAT);
		out.flush();
		Files.write(otherRhino, other.toByteArray());
		var foreign = folder.resolve("not-ours.rbc");
		Files.writeString(foreign, "something else");
		cache().evict(folder);
		assertTrue(Files.exists(kept));
		assertFalse(Files.exists(formerFormat));
		assertFalse(Files.exists(otherRhino));
		assertTrue("only the entries of this cache are recognized", Files.exists(foreign));
	}

	private File project(String name) throws Exception {
		var project = folder.resolve(name).toFile();
		new File(project, "_flow/modules").mkdirs();
		Files.writeString(new File(project, "c8oProject.yaml").toPath(), "convertigo: 8.5.0\n");
		return project;
	}

	private int cacheFiles(File project) throws Exception {
		var cacheFolder = new File(project, RhinoBytecodeCache.FOLDER).toPath();
		if (!Files.isDirectory(cacheFolder)) {
			return 0;
		}
		try (var files = Files.list(cacheFolder)) {
			return (int) files.count();
		}
	}

	@Test public void cacheGoesInThePrivateFolderOfTheProjectOfTheSource() throws Exception {
		System.setProperty(MODE_PROPERTY, "on");
		var project = project("prj");
		var module = new File(project, "_flow/modules/m.js");
		assertEquals(expected(source(5), module.getPath()), run(cx, RhinoBytecodeCache.compile(cx, source(5), module.getPath(), module)));
		RhinoBytecodeCache.flush();
		assertEquals(1, cacheFiles(project));
		if (Files.getFileStore(folder).supportsFileAttributeView("posix")) {
			var entry = Files.list(new File(project, RhinoBytecodeCache.FOLDER).toPath()).findFirst().get();
			assertEquals(java.util.Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), Files.getPosixFilePermissions(entry));
		}
		RhinoBytecodeCache.reset();
		RhinoBytecodeCache.compile(cx, source(5), module.getPath(), module);
		assertEquals(1, RhinoBytecodeCache.statistics.hits.get());
	}

	@Test public void cacheFollowsTheLocalWorkingDirectory() throws Exception {
		System.setProperty(MODE_PROPERTY, "on");
		var project = project("relocated");
		var root = folder.resolve("local").toFile();
		var open = LocalWorkDirectory.class.getDeclaredMethod("open", File.class, String.class, Predicate.class);
		open.setAccessible(true);
		var use = LocalWorkDirectory.class.getDeclaredMethod("use", LocalWorkDirectory.class);
		use.setAccessible(true);
		Predicate<String> exists = name -> true;
		use.invoke(null, open.invoke(null, root, "test", exists));
		try {
			var module = new File(project, "_flow/modules/m.js");
			RhinoBytecodeCache.compile(cx, source(6), module.getPath(), module);
			RhinoBytecodeCache.flush();
			var link = new File(project, RhinoBytecodeCache.FOLDER).toPath();
			assertTrue("a link in the project", Files.isSymbolicLink(link));
			var local = new File(root, "projects/relocated/" + RhinoBytecodeCache.FOLDER).toPath();
			assertEquals(local.toRealPath(), link.toRealPath());
			try (var files = Files.list(local)) {
				assertEquals(1, files.count());
			}
		} finally {
			use.invoke(null, (Object) null);
		}
	}

	@Test public void nothingIsWrittenWhenTheCacheDoesNotApply() throws Exception {
		var project = project("off");
		var module = new File(project, "_flow/modules/m.js");
		// off
		System.setProperty(MODE_PROPERTY, "off");
		assertEquals(expected(source(7), module.getPath()), run(cx, RhinoBytecodeCache.compile(cx, source(7), module.getPath(), module)));
		// auto outside a running engine
		System.setProperty(MODE_PROPERTY, "auto");
		RhinoBytecodeCache.compile(cx, source(7), module.getPath(), module);
		System.setProperty(MODE_PROPERTY, "on");
		// no origin (a draft), no project above the origin
		RhinoBytecodeCache.compile(cx, source(7), "draft", (File) null);
		var outside = folder.resolve("no-project/m.js").toFile();
		RhinoBytecodeCache.compile(cx, source(7), outside.getPath(), outside);
		// interpreted mode
		cx.setInterpretedMode(true);
		assertEquals(expected(source(7), module.getPath()), run(cx, RhinoBytecodeCache.compile(cx, source(7), module.getPath(), module)));
		cx.setInterpretedMode(false);
		RhinoBytecodeCache.flush();
		assertEquals(0, cacheFiles(project));
		assertFalse(new File(folder.toFile(), "no-project/_private").exists());
		assertNull(RhinoBytecodeCache.originOf("frontend-block:x"));
		assertEquals(module, RhinoBytecodeCache.originOf(module.getPath()));
	}

	@Test public void autoTurnsOffWhenLoadingDoesNotPayOff() {
		var measure = new RhinoBytecodeCache.Measure();
		long ms = TimeUnit.MILLISECONDS.toNanos(1);
		// too few scripts, or too little compilation, for a verdict
		for (int i = 0; i < RhinoBytecodeCache.AUTO_MIN_SAMPLES - 1; i++) {
			measure.loaded(90 * ms, 100 * ms);
		}
		assertFalse(measure.isOff());
		var tiny = new RhinoBytecodeCache.Measure();
		for (int i = 0; i < 100; i++) {
			tiny.loaded(ms, ms);
		}
		assertFalse("100 ms of compilation only", tiny.isOff());
		measure.loaded(90 * ms, 100 * ms);
		assertTrue("loading costs 90% of compiling", measure.isOff());
		var good = new RhinoBytecodeCache.Measure();
		for (int i = 0; i < 64; i++) {
			good.loaded(35 * ms, 100 * ms);
		}
		assertFalse("loading costs 35% of compiling", good.isOff());
		var shared = new RhinoBytecodeCache.Measure();
		for (int i = 0; i < 64; i++) {
			shared.loaded(70 * ms, 100 * ms);
		}
		assertFalse("loading costs 70% of compiling (a shared folder of a container)", shared.isOff());
		var edge = new RhinoBytecodeCache.Measure();
		for (int i = 0; i < 64; i++) {
			edge.loaded(76 * ms, 100 * ms);
		}
		assertTrue("loading costs 76% of compiling", edge.isOff());
	}

	/** In-memory caches cleared before the entry is written: one entry, with the time of the first compilation. */
	@Test public void compiledAgainBeforeWrittenKeepsTheFirstCompilationTime() throws Exception {
		var source = source(10);
		cache().get(cx, source, "again.js", null);
		cache().get(cx, source, "again.js", null);
		assertEquals(2, RhinoBytecodeCache.statistics.misses.get());
		RhinoBytecodeCache.flush();
		assertEquals(1, RhinoBytecodeCache.statistics.writes.get());
		var key = keyOf(source, "again.js");
		var entry = RhinoBytecodeCache.decode(Files.readAllBytes(entryOf(source, "again.js")), key,
				RhinoBytecodeCache.mainClassName("again.js", key), KEY);
		assertTrue("the longest of both compilations", entry.compileNanos() * 2 >= RhinoBytecodeCache.statistics.compileNanos.get());
	}

	/**
	 * An engine restarted: the entries written by a JVM, which compiled its sources again several times (in-memory caches
	 * cleared, warm JIT), are loaded by the next one in auto, which keeps the cache.
	 */
	@Test public void aRestartedEngineLoadsTheEntriesOfThePreviousRun() throws Exception {
		var project = project("restarted");
		var configuration = folder.resolve("configuration");
		Files.createDirectories(configuration);
		var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		var outputs = new ArrayList<String>();
		for (var phase : new String[] { "first", "restart" }) {
			var process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), Restart.class.getName(), phase,
					project.getPath(), configuration.toString()).redirectErrorStream(true).start();
			var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(process.waitFor(180, TimeUnit.SECONDS));
			assertEquals(output, 0, process.exitValue());
			outputs.add(output);
		}
		assertTrue(outputs.get(0), outputs.get(0).contains("hits=0,"));
		assertTrue("no key of the installation", Files.size(configuration.resolve("rhino-bytecode.key")) == 32);
		var restart = outputs.get(1);
		assertTrue(restart, restart.contains("hits=" + Restart.SOURCES + ",") && restart.contains("misses=0,"));
		assertTrue(restart, restart.contains("off=false"));
		assertTrue("a verdict was reached: " + restart, restart.contains("verdict=true"));
	}

	public static final class Restart {
		static final int SOURCES = 24;

		static String big(int i) {
			var text = new StringBuilder("var m" + i + " = (function () {\n");
			for (int f = 0; f < 120; f++) {
				text.append("function f").append(f).append("(a, b) { var r = [a, b, ").append(f).append("].map(function (v) { return v * ")
						.append(f).append(" + ").append(i).append("; }); return r.join('-') + /x(\\d+)/.exec('x' + a); }\n");
			}
			return text.append("return { run: f7 }; })();\nm").append(i).append(".run(1, 2)").toString();
		}

		public static void main(String[] args) throws Exception {
			RhinoBytecodeCache.runningEngineForTests = true;
			com.twinsoft.convertigo.engine.Engine.CONFIGURATION_PATH = args[2];
			System.setProperty(MODE_PROPERTY, "auto");
			var project = new File(args[1]);
			var cx = enter();
			try {
				var rounds = args[0].equals("first") ? 3 : 1;
				for (int round = 0; round < rounds; round++) {
					for (int i = 0; i < SOURCES; i++) {
						var module = new File(project, "_flow/modules/m" + i + ".js");
						var script = RhinoBytecodeCache.compile(cx, big(i), module.getPath(), module);
						run(cx, script);
					}
					// the in-memory caches of the engine are cleared between rounds: compiled again, warmer
				}
				RhinoBytecodeCache.flush();
				var measure = RhinoBytecodeCache.measure();
				System.out.println(RhinoBytecodeCache.statistics + ", " + measure.state() + ", verdict=" + measure.verdict());
				System.exit(0);
			} finally {
				Context.exit();
			}
		}
	}

	@Test public void autoOffStopsReadingAndWriting() throws Exception {
		var measure = RhinoBytecodeCache.measure();
		for (int i = 0; i < RhinoBytecodeCache.AUTO_MIN_SAMPLES; i++) {
			measure.loaded(TimeUnit.MILLISECONDS.toNanos(100), TimeUnit.MILLISECONDS.toNanos(100));
		}
		assertTrue(measure.isOff());
		// a cache bound to the verdict skips the writes already queued
		cache().get(cx, source(8), "late.js", measure);
		RhinoBytecodeCache.flush();
		assertTrue(entries().isEmpty());
	}

	@Test public void installationKeyIsCreatedOnceAndPrivate() throws Exception {
		var file = folder.resolve("rhino-bytecode.key");
		var key = RhinoBytecodeCache.readOrCreateKey(file);
		assertEquals(32, key.length);
		assertArrayEquals(key, RhinoBytecodeCache.readOrCreateKey(file));
		if (Files.getFileStore(folder).supportsFileAttributeView("posix")) {
			assertEquals(java.util.Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), Files.getPosixFilePermissions(file));
		}
		Files.write(file, new byte[] { 1, 2, 3 });
		var replaced = RhinoBytecodeCache.readOrCreateKey(file);
		assertEquals(32, replaced.length);
		try (var files = Files.list(folder)) {
			assertEquals("no temporary file left", 1, files.count());
		}
	}
}
