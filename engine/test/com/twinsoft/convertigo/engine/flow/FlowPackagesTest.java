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

package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;

import javax.tools.ToolProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.util.FileUtils;

/** The packages of a Flow call on a runtime shared by the projects, on Rhino, without engine. */
public class FlowPackagesTest {
	private File base;

	@Before
	public void setUp() throws Exception {
		base = Files.createTempDirectory("flow-packages-test").toFile();
		com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
	}

	@After
	public void tearDown() {
		FileUtils.deleteQuietly(base);
	}

	/** @return a class loader with a class probe.V whose v() returns the version */
	private URLClassLoader generation(String version) throws Exception {
		var dir = new File(base, version);
		var source = new File(dir, "probe/V.java");
		source.getParentFile().mkdirs();
		Files.writeString(source.toPath(), "package probe; public class V { public static String v() { return \"" + version + "\"; } }");
		var compiler = ToolProvider.getSystemJavaCompiler();
		try (var files = compiler.getStandardFileManager(null, null, null)) {
			assertTrue(compiler.getTask(null, files, null, List.of("-d", dir.getPath()), null, files.getJavaFileObjects(source)).call());
		}
		return new URLClassLoader(new URL[] { dir.toURI().toURL() }, FlowPackagesTest.class.getClassLoader());
	}

	private static String eval(Context cx, Scriptable scope, String script) {
		return Context.toString(cx.evaluateString(scope, script, "test", 1, null));
	}

	@Test
	public void sharedRevisionRoutesProjectPackagesThroughTheCallingFrame() throws Exception {
		try (var a = generation("SharedA"); var b = generation("SharedB"); var cx = Context.enter()) {
			var api = new FlowRuntimeApi(cx);
			var function = (org.mozilla.javascript.Function) cx.evaluateString(api.sharedScope(),
					"(function() { return String(Packages.probe.V.v()); })", "shared-packages", 1, null);
			api.sharedScope().sealObject();
			try (var parent = api.enter(cx)) {
				FlowPackages.initializeChildScope(parent.scope, api.sharedScope());
				FlowPackages.bind(cx, parent.scope, a);
				assertEquals("SharedA", function.call(cx, parent.scope, parent.scope, new Object[0]));
				try (var child = api.enter(cx)) {
					FlowPackages.initializeChildScope(child.scope, api.sharedScope());
					FlowPackages.bind(cx, child.scope, b);
					assertEquals("SharedB", function.call(cx, child.scope, child.scope, new Object[0]));
				}
				assertEquals("SharedA", function.call(cx, parent.scope, parent.scope, new Object[0]));
			}
			assertEquals("false", eval(cx, api.sharedScope(), "typeof Packages.probe.V.v == 'function'"));
		}
	}

	@Test
	public void aCallUsesTheGenerationOfItsProjectAndGivesBackTheEngineOnes() throws Exception {
		var a = generation("A");
		var b = generation("B");
		var thread = Thread.currentThread();
		var previous = thread.getContextClassLoader();
		var cx = Context.enter();
		try {
			// a runtime created by a request of project A
			thread.setContextClassLoader(a);
			Scriptable scope = FlowPackages.createScope(cx);
			assertEquals("the runtime has the packages of the engine, not those of the creating thread", "false",
					eval(cx, scope, "Packages.probe.V instanceof java.lang.Class || typeof Packages.probe.V.v == 'function'"));

			FlowPackages.bind(cx, scope, b);
			assertEquals("a call of project B sees its classes", "B", eval(cx, scope, "String(Packages.probe.V.v())"));
			assertEquals("through every root", "B", eval(cx, scope, "String(new JavaImporter(Packages.probe).V ? Packages.probe.V.v() : '')"));
			assertEquals("the JDK", "x", eval(cx, scope, "String(new java.lang.String('x'))"));
			assertEquals("and the engine", "FlowPackages",
					eval(cx, scope, "String(com.twinsoft.convertigo.engine.flow.FlowPackages.__javaObject__.getSimpleName())"));

			FlowPackages.restore(scope);
			assertEquals("the packages of the engine come back", "false",
					eval(cx, scope, "typeof Packages.probe.V.v == 'function'"));
			assertEquals("", eval(cx, scope, "String(" + FlowPackages.GENERATION_ID + ")"));

			FlowPackages.bind(cx, scope, a);
			assertEquals("A", eval(cx, scope, "String(Packages.probe.V.v())"));
			FlowPackages.bind(cx, scope, null);
			assertEquals("no project: the engine", "false", eval(cx, scope, "typeof Packages.probe.V.v == 'function'"));
		} finally {
			Context.exit();
			thread.setContextClassLoader(previous);
		}
	}

	private WeakReference<ClassLoader> releasedSharedRevision() throws Exception {
		try (var loader = generation("Retired"); var cx = Context.enter()) {
			var api = new FlowRuntimeApi(cx);
			try (var frame = api.enter(cx)) {
				FlowPackages.initializeChildScope(frame.scope, api.sharedScope());
				FlowPackages.bind(cx, frame.scope, loader);
				assertEquals("Retired", eval(cx, frame.scope, "String(Packages.probe.V.v())"));
			}
			return new WeakReference<>(loader);
		}
	}

	@Test
	public void retiredSharedRevisionReleasesItsProjectGeneration() throws Exception {
		var reference = releasedSharedRevision();
		for (int i = 0; i < 50 && reference.get() != null; i++) {
			System.gc(); Thread.sleep(20);
		}
		assertNull("Retired shared metadata must not retain its project generation", reference.get());
	}

	@Test
	public void aRuntimeKeepsNoGenerationAfterACall() throws Exception {
		var cx = Context.enter();
		try {
			Scriptable scope = FlowPackages.createScope(cx);
			var generation = generation("C");
			FlowPackages.bind(cx, scope, generation);
			assertEquals("C", eval(cx, scope, "String(Packages.probe.V.v())"));
			assertEquals("an engine class learnt during the call", "x", eval(cx, scope, "String(new java.lang.String('x'))"));
			FlowPackages.restore(scope, generation);
			assertEquals("is still known after it", "x", eval(cx, scope, "String(new java.lang.String('x'))"));
			var reference = new WeakReference<ClassLoader>(generation);
			generation.close();
			generation = null;
			for (int i = 0; i < 50 && reference.get() != null; i++) {
				System.gc();
				Thread.sleep(20);
			}
			assertNull("the generation is collected while the runtime lives on", reference.get());
			assertNotNull(scope);
		} finally {
			Context.exit();
		}
	}

	@Test
	public void nativeDefaultSharesOnlyReflectionAcrossConcurrentProjectGenerations() throws Exception {
		var a = generation("A");
		var b = generation("B");
		var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
		var barrier = new java.util.concurrent.CyclicBarrier(2);
		var caches = new java.util.concurrent.ConcurrentLinkedQueue<org.mozilla.javascript.ClassCache>();
		try {
			var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
			for (var loader : List.of(a, b)) futures.add(workers.submit(() -> {
				try (var cx = Context.enter()) {
					var scope = FlowPackages.createScope(cx);
					caches.add(org.mozilla.javascript.ClassCache.get(scope));
					FlowPackages.bind(cx, scope, loader);
					String expected = loader == a ? "A" : "B";
					eval(cx, scope, "var privateValue = '" + expected + "'; Array.prototype.privateValue = privateValue;");
					barrier.await();
					for (int i = 0; i < 50; i++) {
						assertEquals(expected, eval(cx, scope, "String(Packages.probe.V.v())"));
						assertEquals(expected, eval(cx, scope, "[].privateValue"));
					}
					FlowPackages.restore(scope, loader);
					assertEquals("false", eval(cx, scope, "typeof Packages.probe.V.v == 'function'"));
				} catch (Exception e) { throw new AssertionError(e); }
			}));
			for (var future : futures) future.get(30, java.util.concurrent.TimeUnit.SECONDS);
			var iterator = caches.iterator(); assertSame(iterator.next(), iterator.next());
		} finally { workers.shutdownNow(); a.close(); b.close(); }
	}
}
