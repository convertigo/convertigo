/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;
import org.junit.Test;

/** Load only the distributable artifacts: build/classes must not hide packaging errors. */
public class JsonRepresentationPackagingTest {
	@Test
	public void packagedEngineNeedsOnlyPublicRhinoClasses() throws Exception {
		var engine = Path.of(System.getProperty("representation.engineJar"));
		var dependencies = Path.of(System.getProperty("representation.dependenciesJar"));
		try (var jar = new JarFile(engine.toFile())) {
			assertFalse("Engine classes must not extend Rhino internals from its package",
					jar.stream().anyMatch(entry -> entry.getName().startsWith("org/mozilla/")));
			for (var name : new String[] {"JsonValues", "JsonData", "DetachedJson", "SanitizedJson", "DetachedJsonWriter"})
				assertNotNull(name, jar.getEntry("com/twinsoft/convertigo/engine/util/" + name + ".class"));
		}
		var thread = Thread.currentThread();
		var previousLoader = thread.getContextClassLoader();
		try (var loader = new URLClassLoader(new java.net.URL[] {engine.toUri().toURL(), dependencies.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
			thread.setContextClassLoader(loader); // ServiceLoader must use the same isolated artifacts.
			var values = loader.loadClass("com.twinsoft.convertigo.engine.util.JsonValues");
			var parsed = values.getMethod("parse", String.class).invoke(null, "{\"rows\":[{\"x\":1,\"nil\":null}]}");
			assertTrue(parsed instanceof Map<?, ?>);
			assertPlainJson(parsed);
			var context = loader.loadClass("org.mozilla.javascript.Context");
			var scriptable = loader.loadClass("org.mozilla.javascript.Scriptable");
			var cx = context.getMethod("enter").invoke(null);
			try {
				context.getMethod("setLanguageVersion", int.class).invoke(cx, 200);
				var scope = context.getMethod("initStandardObjects").invoke(cx);
				var projection = loader.loadClass("com.twinsoft.convertigo.engine.util.JsonData").getMethod("wrap", Object.class, scriptable).invoke(null, parsed, scope);
				assertEquals("org.mozilla.javascript.NativeObject", projection.getClass().getName());
				var detached = loader.loadClass("com.twinsoft.convertigo.engine.util.DetachedJson").getMethod("map", Object.class, context, scriptable).invoke(null, projection, cx, scope);
				assertTrue(detached instanceof Map<?, ?>);
				assertPlainJson(detached);
				var writer = new java.io.StringWriter();
				loader.loadClass("com.twinsoft.convertigo.engine.util.DetachedJsonWriter").getMethod("write", Object.class, java.io.Writer.class).invoke(null, detached, writer);
				assertEquals("{\"rows\":[{\"x\":1,\"nil\":null}]}", writer.toString());
			} finally { context.getMethod("exit").invoke(null); }
			for (var name : new String[] {"org.mozilla.javascript.MaterializedObject", "com.twinsoft.convertigo.engine.util.HostJsonData"}) {
				try { loader.loadClass(name); fail("Experimental class packaged: " + name); }
				catch (ClassNotFoundException expected) { }
			}
		} finally { thread.setContextClassLoader(previousLoader); }
	}
	private static void assertPlainJson(Object value) {
		if (value instanceof Map<?, ?> object) {
			for (var entry : object.entrySet()) {
				assertTrue(entry.getKey() instanceof String); assertPlainJson(entry.getValue());
			}
		} else if (value instanceof java.util.List<?> array) {
			for (var item : array) assertPlainJson(item);
		} else assertTrue("Only JSON scalars may leave the realm", value == null || value instanceof String || value instanceof Number || value instanceof Boolean);
	}

}
