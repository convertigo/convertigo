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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * A Java map or list that the JSON writer of RhinoUtils refuses (a date, a Java array, a character, a bean, a nesting
 * deeper than 256) is written by JSON.stringify of Rhino, wrapped for JavaScript, and never sent back to the writer
 * (it used to loop until a StackOverflowError).
 */
public class JsonStringifyFallbackTest {
	private Context cx;
	private Scriptable scope;

	@Before
	public void enter() {
		cx = Context.enter();
		scope = cx.initStandardObjects();
	}

	@After
	public void exit() {
		Context.exit();
	}

	private String rhino(Object javaValue) {
		NativeJSON json = (NativeJSON) ScriptableObject.getProperty(scope, "JSON");
		return ScriptableObject.callMethod(json, "stringify", new Object[] {Context.javaToJS(javaValue, scope)}).toString();
	}

	private static Map<String, Object> deep(int depth) {
		Map<String, Object> root = new LinkedHashMap<>();
		Map<String, Object> current = root;
		for (int i = 0; i < depth; i++) {
			Map<String, Object> child = new LinkedHashMap<>();
			current.put("k", child);
			current = child;
		}
		current.put("leaf", 1);
		return root;
	}

	private static Map<String, Object> notJsonData() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("text", "a \"quoted\" text");
		map.put("number", 3);
		map.put("date", new Date(0));
		map.put("array", new int[] {1, 2, 3});
		map.put("char", 'x');
		map.put("dates", Arrays.asList(new Date(1000), new Date(2000)));
		map.put("deep", deep(300));
		return map;
	}

	@Test
	public void aMapTheWriterRefusesIsWrittenByRhino() {
		Map<String, Object> map = notJsonData();
		String expected = rhino(map);
		assertTrue(expected, expected.startsWith("{\"text\":"));
		assertEquals(expected, RhinoUtils.jsonStringify(map));
		assertEquals(expected, RhinoUtils.jsonStringifyData(map));
		assertEquals(expected, RhinoUtils.jsonStringify(Context.javaToJS(map, scope)));
	}

	@Test
	public void eachRefusedValueAloneIsWrittenByRhino() {
		for (Object value : new Object[] {new Date(0), new int[] {1, 2}, 'x', new Object() {
			@Override
			public String toString() {
				return "bean";
			}
		}}) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("v", value);
			assertEquals(rhino(map), RhinoUtils.jsonStringify(map));
		}
		List<Object> dates = new ArrayList<>(Arrays.asList(new Date(0), new Date(1000)));
		assertEquals(rhino(dates), RhinoUtils.jsonStringify(dates));
		Map<String, Object> deep = deep(300);
		assertEquals(rhino(deep), RhinoUtils.jsonStringify(deep));
	}

	@Test
	public void theWritersDropTheTextOfARefusedValue() throws Exception {
		Map<String, Object> map = notJsonData();
		StringBuilder sb = new StringBuilder("prefix:");
		RhinoUtils.writeJsonData(sb, map);
		assertEquals("prefix:" + rhino(map), sb.toString());

		StringWriter writer = new StringWriter();
		RhinoUtils.writeJsonData(writer, map);
		assertEquals(rhino(map), writer.toString());
	}

	@Test
	public void plainDataIsStillWrittenByTheWriter() throws Exception {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", 1);
		map.put("b", Arrays.asList("x", 2.5, null, true));
		map.put("c", deep(10));
		assertTrue(RhinoUtils.isJsonData(map));
		assertEquals(rhino(map), RhinoUtils.jsonStringify(map));
		StringWriter writer = new StringWriter();
		RhinoUtils.writeJsonData(writer, map);
		assertEquals(rhino(map), writer.toString());
	}
}
