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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * The patched ConsString of Rhino 1.9.1 (patches project): a concatenated string read by several threads at the same
 * time, as a value of a shared sealed scope or of a cache of a shared runtime, gives the same text to all of them.
 * Rhino 1.9.1 read its flat flag without synchronization: another thread could fail on a ClassCastException or copy a
 * wrong text.
 */
public class RhinoConsStringTest {

	// when Rhino is upgraded, check whether ConsString is safe between threads, then remove the patch or this check
	@Test public void patchMatchesTheRhinoVersion() throws Exception {
		assertNotNull("the patched ConsString must come before the one of Rhino", ConsString.class.getDeclaredField("flat"));
		var version = Context.class.getPackage().getImplementationVersion();
		assertTrue("ConsString patch written for Rhino 1.9.1, found " + version, version == null || version.equals("1.9.1"));
	}

	@Test public void sharedTreesGiveTheSameTextToConcurrentReaders() throws Exception {
		int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
		var expected = new StringBuilder();
		for (int i = 0; i < 40; i++) {
			expected.append("part").append(i).append(';');
		}
		var text = expected.toString();
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			for (int round = 0; round < 20000; round++) {
				CharSequence child = new ConsString("part0;", "part1;");
				CharSequence tree = child;
				for (int i = 2; i < 40; i++) {
					tree = new ConsString(tree, "part" + i + ";");
				}
				CharSequence shared = tree, sharedChild = child;
				var gate = new CyclicBarrier(threads);
				var readers = new ArrayList<Future<String>>(threads);
				for (int k = 0; k < threads; k++) {
					boolean childFirst = k % 2 == 0;
					readers.add(pool.submit(() -> {
						gate.await();
						if (childFirst) {
							assertEquals("part0;part1;", sharedChild.toString());
						}
						assertEquals(';', shared.charAt(shared.length() - 1));
						return shared.toString();
					}));
				}
				for (var reader : readers) {
					assertEquals(text, reader.get(10, TimeUnit.SECONDS));
				}
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test public void deepTreesAndSharedChildrenFlattenOnce() {
		CharSequence tree = "";
		var parts = new ArrayList<CharSequence>();
		for (int i = 0; i < 100000; i++) {
			tree = new ConsString(tree, "x");
			if (i % 1000 == 0) {
				parts.add(tree);
			}
		}
		assertEquals(100000, tree.toString().length());
		assertTrue(tree.toString() == tree.toString());
		for (var part : parts) {
			assertEquals(part.length(), part.toString().length());
		}
		assertEquals("xx", tree.subSequence(5, 7).toString());
	}

	@Test public void scriptsShareConcatenatedValuesOfASealedScope() throws Exception {
		Context cx = ContextFactory.getGlobal().enterContext();
		ScriptableObject shared;
		try {
			shared = cx.initStandardObjects();
			cx.evaluateString(shared, "var prefix = 'Bearer '; var token = prefix; for (var i = 0; i < 50; i++) token += 'abc' + i;", "shared", 1, null);
			shared.sealObject();
		} finally {
			Context.exit();
		}
		var expected = new StringBuilder("Bearer ");
		for (int i = 0; i < 50; i++) {
			expected.append("abc").append(i);
		}
		int threads = 8;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			var gate = new CyclicBarrier(threads);
			var calls = new ArrayList<Future<String>>();
			for (int k = 0; k < threads; k++) {
				calls.add(pool.submit(() -> {
					Context local = ContextFactory.getGlobal().enterContext();
					try {
						Scriptable scope = local.newObject(shared);
						scope.setPrototype(shared);
						scope.setParentScope(null);
						gate.await();
						return Context.toString(local.evaluateString(scope, "token.split(' ')[1].length + ':' + token", "call", 1, null));
					} finally {
						Context.exit();
					}
				}));
			}
			for (var call : calls) {
				assertEquals((expected.length() - 7) + ":" + expected, call.get(10, TimeUnit.SECONDS));
			}
		} finally {
			pool.shutdownNow();
		}
	}
}
