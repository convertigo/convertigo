/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import org.junit.Test;
import org.mozilla.javascript.*;

public class DetachedJsonTest {
	@Test
	public void stringsBuiltByTheScriptOfANativeSequenceReachTheCallerAsStrings() throws Exception {
		try (var cx = Context.enter()) {
			cx.setLanguageVersion(Context.VERSION_ES6);
			var scope = cx.initStandardObjects();
			// the outputs of a native sequence keep the values of its script, as the native result of the call has them
			var output = (Scriptable) cx.evaluateString(scope, "var x = 'b'; ({name: 'a' + x, list: ['c' + x]})", "output", 1, null);
			assertTrue(ScriptableObject.getProperty(output, "name") instanceof ConsString);
			var result = new LinkedHashMap<String, Object>();
			result.put("output", output);
			result.put("text", new ConsString("d", "e"));

			var converted = (Scriptable) DetachedJson.toScriptable(result, scope);
			assertEquals(String.class, ScriptableObject.getProperty(converted, "text").getClass());
			ScriptableObject.putProperty(scope, "converted", converted);
			assertEquals("{\"output\":{\"name\":\"ab\",\"list\":[\"cb\"]},\"text\":\"de\"}",
					Context.toString(cx.evaluateString(scope, "JSON.stringify(converted)", "check", 1, null)));
		}
	}
}
