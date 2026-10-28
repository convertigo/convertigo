/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sessions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.engine.Engine;

/**
 * Values made by scripts (objects, arrays, concatenated strings, wrapped Java values) are kept by the Redis stores of
 * sessions and contexts as JSON, never with the Java serialization of Rhino, disabled by default since Rhino 1.9.2.
 */
public class RhinoSessionValuesTest {
	private static final String SCRIPT = "var token = 'Bearer '; for (var i = 0; i < 3; i++) token += i;"
			+ "({token: token, items: [1, 'two', token + '!', {deep: true}], count: 3, ratio: 0.5, ok: true, none: null})";

	@BeforeClass public static void logs() {
		Engine.logRedis = org.apache.log4j.Logger.getLogger("rhino-session-values-test");
		Engine.logEngine = Engine.logRedis;
	}

	// the stores below must not depend on it: a NativeObject is refused unless rhino.serialization.enabled is set
	@Test public void javaSerializationOfRhinoObjectsIsOff() throws Exception {
		Context cx = ContextFactory.getGlobal().enterContext();
		try {
			var value = cx.evaluateString(cx.initStandardObjects(), SCRIPT, "values", 1, null);
			var out = new ObjectOutputStream(new ByteArrayOutputStream());
			assertThrows(NotSerializableException.class, () -> out.writeObject(value));
		} finally {
			Context.exit();
		}
	}

	@Test public void scriptValuesOfASession() throws Exception {
		var codec = new SessionValueCodec();
		Context cx = ContextFactory.getGlobal().enterContext();
		try {
			Scriptable scope = cx.initStandardObjects();
			var value = scriptValue(cx, scope);
			assertValues(codec.deserialize("profile", codec.serialize("profile", value)));
			var list = Context.javaToJS(new ArrayList<>(List.of("a", "b")), scope);
			assertEquals(List.of("a", "b"), codec.deserialize("list", codec.serialize("list", list)));
		} finally {
			Context.exit();
		}
	}

	@Test public void scriptValuesOfAContext() throws Exception {
		var codec = new ContextValueCodec();
		Context cx = ContextFactory.getGlobal().enterContext();
		try {
			Scriptable scope = cx.initStandardObjects();
			var value = scriptValue(cx, scope);
			assertValues(codec.deserialize(codec.serialize(value)));
			var token = ScriptableObject.getProperty(value, "token");
			assertEquals("Bearer 012", codec.deserialize(codec.serialize(token)));
			var list = Context.javaToJS(new ArrayList<>(List.of("a", "b")), scope);
			assertEquals(List.of("a", "b"), codec.deserialize(codec.serialize(list)));
		} finally {
			Context.exit();
		}
	}

	private static NativeObject scriptValue(Context cx, Scriptable scope) {
		var value = (NativeObject) cx.evaluateString(scope, SCRIPT, "values", 1, null);
		assertTrue("a concatenated string of Rhino", ScriptableObject.getProperty(value, "token") instanceof ConsString);
		assertTrue(ScriptableObject.getProperty(value, "items") instanceof NativeArray);
		return value;
	}

	private static void assertValues(Object decoded) {
		assertTrue("a map: " + decoded, decoded instanceof Map);
		var map = (Map<?, ?>) decoded;
		assertEquals("Bearer 012", map.get("token"));
		assertEquals(3d, ((Number) map.get("count")).doubleValue(), 0);
		assertEquals(0.5, ((Number) map.get("ratio")).doubleValue(), 0);
		assertEquals(Boolean.TRUE, map.get("ok"));
		assertEquals(null, map.get("none"));
		var items = (List<?>) map.get("items");
		assertEquals(4, items.size());
		assertEquals(1d, ((Number) items.get(0)).doubleValue(), 0);
		assertEquals("two", items.get(1));
		assertEquals("Bearer 012!", items.get(2));
		assertEquals(Map.of("deep", true), items.get(3));
	}
}
