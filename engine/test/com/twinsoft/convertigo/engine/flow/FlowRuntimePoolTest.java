/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class FlowRuntimePoolTest {
	private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
		var method = type.getDeclaredMethod(name, parameters);
		method.setAccessible(true);
		return method;
	}

	private static Object field(Object object, String name) throws Exception {
		return method(object.getClass(), name).invoke(object);
	}

	@Test public void nestedCallsReusePlansWithoutSharingActiveScopesOrKeepingOverflow() throws Exception {
		Engine.logEngine = org.apache.log4j.Logger.getLogger("pool-test");
		Engine.logBeans = Engine.logEngine;
		EnginePropertiesManager.initProperties();
		var bridge = FlowEngineBridge.class;
		var referenceClass = Class.forName(bridge.getName() + "$EngineRef");
		var sourceClass = Class.forName(bridge.getName() + "$CachedEngineSource");
		var lookupClass = Class.forName(bridge.getName() + "$CachedEngineRuntimeLookup");
		var reference = method(referenceClass, "parse", String.class).invoke(null, "PoolTest.Engine");
		var constructor = sourceClass.getDeclaredConstructor(File.class, String.class, long.class, long.class);
		constructor.setAccessible(true);
		var file = new File("pool-test/Engine.js").getAbsoluteFile();
		String script = "({cacheClear:function(){return '{}';}})";
		var source = constructor.newInstance(file, script, 1L, (long) script.length());
		var borrow = method(bridge, "cachedEngineRuntime", referenceClass, File.class, sourceClass, Context.class, boolean.class);
		var release = method(bridge, "releaseEngineRuntime", lookupClass);
		var cx = Context.enter();
		var held = new ArrayList<Object>();
		try {
			FlowEngineBridge.clearCaches();
			// One authoring scope and two active callers must leave a reusable callee.
			for (int i = 0; i < 3; i++) held.add(borrow.invoke(null, reference, file, source, cx, i == 0));
			var child = borrow.invoke(null, reference, file, source, cx, false);
			var childScope = (Scriptable) field(field(child, "runtime"), "scope");
			childScope.put("preparedPlan", childScope, "retained");
			release.invoke(null, child);
			child = borrow.invoke(null, reference, file, source, cx, false);
			assertEquals(true, field(child, "hit"));
			assertSame(childScope, field(field(child, "runtime"), "scope"));
			assertEquals("retained", childScope.get("preparedPlan", childScope));
			held.add(child);
			var scopes = new HashSet<Object>();
			for (var active : held) assertTrue(scopes.add(field(field(active, "runtime"), "scope")));
			// The pool remains bounded; further calls still get independent runtimes.
			for (int i = 4; i < 9; i++) held.add(borrow.invoke(null, reference, file, source, cx, false));
			assertEquals(false, field(held.get(8), "pooled"));
			var spare = held.remove(7);
			release.invoke(null, spare);
			var hot = held.remove(3);
			release.invoke(null, hot);
			var reused = borrow.invoke(null, reference, file, source, cx, false);
			assertSame("sequential calls retain the hottest plan", childScope, field(field(reused, "runtime"), "scope"));
			held.add(reused);
		} finally {
			for (int i = held.size() - 1; i >= 0; i--) release.invoke(null, held.get(i));
			FlowEngineBridge.clearCaches();
			Context.exit();
		}
	}
}
