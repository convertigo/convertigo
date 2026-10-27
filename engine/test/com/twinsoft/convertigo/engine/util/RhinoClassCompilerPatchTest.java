/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.JSScript;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.optimizer.ClassCompiler;

/**
 * The patched ClassCompiler of Rhino 1.9.1 (patches project), whose class files the disk cache of the JavaScript
 * bytecode loads. Rhino 1.9.1 generated classes that the JVM refused (VerifyError: Local variable table overflow, as
 * soon as a function had a parameter or a nested function) and failed on a source above 64 KB (Too big string); it
 * built each nested descriptor again for each of its parents.
 */
public class RhinoClassCompilerPatchTest {
	private Context cx;

	@Before public void enter() {
		cx = ContextFactory.getGlobal().enterContext();
		cx.setLanguageVersion(Context.VERSION_ES6);
		cx.setInterpretedMode(false);
	}

	@After public void exit() {
		Context.exit();
	}

	// when Rhino is upgraded, check whether ClassCompiler still needs these fixes, then remove the patch or this check
	@Test public void patchMatchesTheRhinoVersion() throws Exception {
		assertNotNull("the patched ClassCompiler must come before the one of Rhino", ClassCompiler.class.getField("C8O_PATCH").get(null));
		var version = Context.class.getPackage().getImplementationVersion();
		assertTrue("ClassCompiler patch written for Rhino 1.9.1, found " + version, version == null || version.equals("1.9.1"));
	}

	/** Rhino 1.9.1: VerifyError (Local variable table overflow) on the class of the descriptors. */
	@Test public void descriptorsOfFunctionsWithParametersPassVerification() throws Exception {
		var source = "function add(a, b) { var c = a + b; function twice(x) { return 2 * x; } return twice(c); }\nadd(1, 2) + ':' + [1, 2, 3].map(function (v) { return v * v; }).join(',')";
		assertEquals("6:1,4,9", run(load(source, "verify.js")));
	}

	/** Rhino 1.9.1: Too big string, the source being a class file constant of each descriptor. */
	@Test public void sourcesAbove64KBCompileAndKeepTheirText() throws Exception {
		var big = new StringBuilder("var text = '");
		// multi-byte characters, a surrogate pair across a part boundary of 16000 characters
		while (big.length() < 15999) {
			big.append('é');
		}
		big.append("😀");
		while (big.length() < 70000) {
			big.append("abc中");
		}
		big.append("';\nfunction inner(a) { return a.length; }\nfunction outer() { return inner(text) + ':' + inner.toString(); }\nouter();");
		var source = big.toString();
		assertTrue(source.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65535);
		var expected = Context.toString(cx.compileString(source, "big.js", 1, null).exec(cx, cx.initStandardObjects()));
		var script = load(source, "big.js");
		assertEquals(expected, run(script));
		assertTrue(expected.endsWith("function inner(a) { return a.length; }"));
	}

	/** The nested descriptors are built once: the same objects in their parent and in the descriptors of the class. */
	@Test public void nestedDescriptorsAreSharedWithTheClass() throws Exception {
		var source = "function a() { function b() { function c() { return 1; } return c; } function d() { return 2; } return [b, d]; }\n"
				+ "var e = function () { return a; };\n(function f() { return e()()[0]()(); })()";
		var classes = compileToClasses(source, "nested.js", "org.mozilla.javascript.gen.nested_test");
		assertEquals("1", run(classes.script(cx)));
		var descriptors = (JSDescriptor<?>[]) classes.scriptClass().getField("_descriptors").get(null);
		var all = new IdentityHashMap<JSDescriptor<?>, Boolean>();
		for (var descriptor : descriptors) {
			assertNotNull(descriptor);
			all.put(descriptor, true);
		}
		assertEquals(descriptors.length, all.size());
		int nested = 0;
		for (var descriptor : descriptors) {
			for (var child : descriptor.nestedFunctions) {
				assertTrue("a nested function is a descriptor of the class", all.containsKey(child));
				assertSame(descriptor, child.getParent());
				nested++;
			}
		}
		assertEquals(descriptors.length - 1, nested);
	}

	@Test public void regularExpressionsAndTemplateLiteralsAreInitialized() throws Exception {
		var source = "var re = /a(b+)c/g; var who = 'world'; var m = 'xabbbcx abc'.replace(re, function (all, bs) { return bs.length; });\n"
				+ "`hello ${who} ${m}` + String.raw`\\n${1}`";
		var expected = Context.toString(cx.compileString(source, "literals.js", 1, null).exec(cx, cx.initStandardObjects()));
		assertEquals("hello world x3x 1\\n1", expected);
		assertEquals(expected, run(load(source, "literals.js")));
	}

	private Script load(String source, String name) throws Exception {
		var main = "org.mozilla.javascript.gen.patch_test_" + Math.abs(name.hashCode());
		return compileToClasses(source, name, main).script(cx);
	}

	private String run(Script script) {
		Scriptable scope = cx.initStandardObjects();
		return Context.toString(script.exec(cx, scope, scope));
	}

	private Compiled compileToClasses(String source, String name, String main) {
		var env = new CompilerEnvirons();
		env.initFromContext(cx);
		var compiled = new ClassCompiler(env).compileToClassFiles(source, name, 1, main);
		var classes = new HashMap<String, byte[]>();
		for (int i = 0; i < compiled.length; i += 2) {
			classes.put((String) compiled[i], (byte[]) compiled[i + 1]);
		}
		return new Compiled(main, classes);
	}

	/** The classes compiled for a test, defined in their own loader as the disk cache of the engine does. */
	private record Compiled(String main, Map<String, byte[]> classes) {
		/** The class of the script, defined in a new loader, its descriptors built by the static initialization of its companion. */
		Class<?> scriptClass() throws Exception {
			var loader = new ClassLoader(Context.class.getClassLoader()) {
				@Override
				protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
					var bytes = className.startsWith(main) ? classes.get(className) : null;
					if (bytes == null) {
						return super.loadClass(className, resolve);
					}
					synchronized (getClassLoadingLock(className)) {
						var loaded = findLoadedClass(className);
						return loaded != null ? loaded : defineClass(className, bytes, 0, bytes.length);
					}
				}
			};
			var scriptClass = loader.loadClass(main);
			Class.forName(main + "Main", true, loader);
			return scriptClass;
		}

		/** The script, its regular expressions and template literals initialized as Rhino does after compiling in memory. */
		@SuppressWarnings("unchecked")
		Script script(Context cx) throws Exception {
			var scriptClass = scriptClass();
			var descriptors = (JSDescriptor<?>[]) scriptClass.getField("_descriptors").get(null);
			try {
				scriptClass.getMethod("_reInit", Context.class).invoke(null, cx);
			} catch (NoSuchMethodException none) {
				// no regular expression
			}
			scriptClass.getMethod("_qInit").invoke(null);
			return new JSScript((JSDescriptor<JSScript>) descriptors[0], null);
		}
	}
}
