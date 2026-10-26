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
	public static final java.util.concurrent.CountDownLatch interruptedCallStarted = new java.util.concurrent.CountDownLatch(1);

	@Test public void bridgeReturnsRuntimeAfterScriptFailureAndThreadInterruption() throws Exception {
		Engine.logEngine = org.apache.log4j.Logger.getLogger("pool-test"); Engine.logBeans = Engine.logEngine;
		EnginePropertiesManager.initProperties();
		var previous = Engine.theApp;
		var directory = java.nio.file.Files.createTempDirectory("pool-failure-");
		var script = directory.resolve("_flow/Engine.js"); java.nio.file.Files.createDirectories(script.getParent());
		java.nio.file.Files.writeString(script, """
			(function(){ var count=0; return {
			  cacheClear:function(){return '{}';},
			  run:function(text){count++;var request=JSON.parse(text);
			    if(request.fail)throw new Error('fixture failure');
			    if(request.interrupt){Packages.com.twinsoft.convertigo.engine.flow.FlowRuntimePoolTest.interruptedCallStarted.countDown();java.lang.Thread.sleep(60000);}
			    return JSON.stringify({ok:true,count:count,hit:__flowBridgeRuntimeCacheHit});
			  }
			};})()
			""");
		var project = new com.twinsoft.convertigo.beans.core.Project() {
			@Override public String getDirPath() { return directory.toString(); }
		};
		project.setName("FailureEngine");
		var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
		try {
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new com.twinsoft.convertigo.engine.DatabaseObjectsManager() {
				@Override public com.twinsoft.convertigo.beans.core.Project getLoadedProjectByName(String name) { return null; }
				@Override public com.twinsoft.convertigo.beans.core.Project getOriginalProjectByName(String name, boolean openable) { return project; }
			};
			FlowEngineBridge.clearCaches(); var bridge = new FlowEngineBridge();
			assertEquals(1, bridge.invoke("FailureEngine.Engine", "run", new org.codehaus.jettison.json.JSONObject(), null, null, null).getInt("count"));
			try { bridge.invoke("FailureEngine.Engine", "run", new org.codehaus.jettison.json.JSONObject().put("fail", true), null, null, null); fail(); }
			catch (com.twinsoft.convertigo.engine.EngineException expected) { }
			var afterError = bridge.invoke("FailureEngine.Engine", "run", new org.codehaus.jettison.json.JSONObject(), null, null, null);
			assertEquals(3, afterError.getInt("count")); assertTrue(afterError.getBoolean("hit"));
			var interrupted = worker.submit(() -> bridge.invoke("FailureEngine.Engine", "run",
					new org.codehaus.jettison.json.JSONObject().put("interrupt", true), null, null, null));
			assertTrue(interruptedCallStarted.await(5, java.util.concurrent.TimeUnit.SECONDS));
			interrupted.cancel(true); worker.shutdown(); assertTrue(worker.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
			var afterInterrupt = bridge.invoke("FailureEngine.Engine", "run", new org.codehaus.jettison.json.JSONObject(), null, null, null);
			assertEquals(5, afterInterrupt.getInt("count")); assertTrue(afterInterrupt.getBoolean("hit"));
			assertNull("No Rhino context leaked on the caller", Context.getCurrentContext());
		} finally {
			worker.shutdownNow(); FlowEngineBridge.clearCaches(); Engine.theApp = previous;
			org.apache.commons.io.FileUtils.deleteDirectory(directory.toFile());
		}
	}

}
