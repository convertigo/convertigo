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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import groovy.lang.GroovyShell;
import javassist.ClassPool;
import javassist.LoaderClassPath;
import net.bytebuddy.ByteBuddy;

public class RuntimeBytecodeTest {
	@Test
	public void compilesScriptsAgainstCurrentJdkAndEngineClasses() throws Exception {
		try (var loader = new groovy.lang.GroovyClassLoader(getClass().getClassLoader())) {
			var shell = new GroovyShell(loader);
			assertEquals("0102", shell.evaluate("java.util.HexFormat.of().formatHex([1, 2] as byte[])"));
			assertEquals("ThreadUtils", shell.evaluate(
					"import com.twinsoft.convertigo.engine.util.ThreadUtils; ThreadUtils.simpleName"));
		}
	}

	@Test
	public void javassistReadsJdkAndEngineMethods() throws Exception {
		var pool = new ClassPool(true);
		pool.insertClassPath(new LoaderClassPath(getClass().getClassLoader()));
		assertTrue(pool.get("java.lang.String").getDeclaredMethods().length > 0);
		assertTrue(pool.get(ThreadUtils.class.getName()).getDeclaredMethods().length > 0);
	}

	@Test
	public void byteBuddyDefinesClassesOnCurrentJdk() throws Exception {
		try (var generated = new ByteBuddy().subclass(Object.class).make()) {
			var type = generated.load(getClass().getClassLoader()).getLoaded();
			assertEquals(Object.class, type.getDeclaredConstructor().newInstance().getClass().getSuperclass());
		}
	}
}
