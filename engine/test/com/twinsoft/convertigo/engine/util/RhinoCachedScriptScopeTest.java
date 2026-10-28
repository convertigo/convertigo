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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * A script compiled once and kept by {@link RhinoUtils#evalCachedJavascript}, run in the scope of each request: reading
 * one of its top-level const must not keep the scopes it ran in. Rhino 1.9.1 kept them in the call site of the read,
 * up to eight, with all they reach (the context of the request), as long as the script stayed cached; fixed by Rhino
 * 1.9.2 (mozilla/rhino#2300).
 */
public class RhinoCachedScriptScopeTest {
	public static final class Payload {
		private final byte[] data = new byte[1 << 20];

		public int size() {
			return data.length;
		}
	}

	@Test public void scopesOfACachedScriptReadingAConstAreReleased() throws Exception {
		assertReleased("const LIMIT = 7;\nvar r = context.size() + LIMIT;\nr;");
	}

	@Test public void scopesOfACachedScriptReadingAVarAreReleased() throws Exception {
		assertReleased("var LIMIT = 7;\nvar r = context.size() + LIMIT;\nr;");
	}

	private static void assertReleased(String code) throws Exception {
		var source = code + "\n// " + UUID.randomUUID();
		var payloads = new ArrayList<WeakReference<Payload>>();
		Context cx = ContextFactory.getGlobal().enterContext();
		try {
			cx.setLanguageVersion(Context.VERSION_ES6);
			for (int i = 0; i < 4; i++) {
				Scriptable scope = cx.initStandardObjects();
				var payload = new Payload();
				payloads.add(new WeakReference<>(payload));
				scope.put("context", scope, Context.javaToJS(payload, scope));
				assertEquals(payload.size() + 7, Context.toNumber(RhinoUtils.evalCachedJavascript(cx, scope, source, "cached.js", 1, null)), 0);
			}
		} finally {
			Context.exit();
		}
		for (int i = 0; i < 40 && alive(payloads) > 0; i++) {
			System.gc();
			Thread.sleep(25);
		}
		assertEquals("scopes kept by the cached script", 0, alive(payloads));
	}

	private static int alive(List<WeakReference<Payload>> payloads) {
		int alive = 0;
		for (var payload : payloads) {
			if (payload.get() != null) {
				alive++;
			}
		}
		return alive;
	}
}
