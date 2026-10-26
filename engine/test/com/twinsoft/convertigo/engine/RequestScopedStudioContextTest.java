/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.junit.Test;

public class RequestScopedStudioContextTest {
	@Test public void statelessClientsAreIndependentButEditorContextsStayShared() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Engine.logContextManager = Engine.logContext = Engine.logUsageMonitor = Logger.getLogger("scoped-studio-test");
		var mode = Engine.class.getDeclaredField("bStudioMode"); mode.setAccessible(true);
		var previous = mode.getBoolean(null);
		var previousEngine = Engine.theApp; Engine.theApp = new Engine();
		var manager = new ContextManager();
		var contexts = ContextManager.class.getDeclaredField("contexts"); contexts.setAccessible(true);
		contexts.set(manager, new ConcurrentHashMap<String, Context>());
		var executor = Executors.newSingleThreadExecutor();
		try {
			mode.setBoolean(null, true);
			var first = manager.get(null, "default", "request-first", null, "Test", null, "Execute");
			var second = manager.get(null, "default", "request-second", null, "Test", null, "Execute");
			assertNotSame(first, second);
			assertEquals("request-first_default", first.contextID);
			try (var held = manager.lockContext(first)) {
				assertTrue(executor.submit(() -> { try (var lock = manager.lockContext(second)) { return true; } })
						.get(2, TimeUnit.SECONDS));
			}
			var editor = manager.get(null, "default", "session-first", null, "Test", null, "Execute");
			assertSame(editor, manager.get(null, "default", "session-second", null, "Test", null, "Execute"));
			assertEquals("studio_Test:S:Execute", editor.contextID);
			assertNotSame(editor, manager.get(null, editor.name, "request-third", null, "Test", null, "Execute"));
			assertSame(first, manager.get(null, "default", "request-first", null, "Test", null, "Execute"));
		} finally { executor.shutdownNow(); mode.setBoolean(null, previous); Engine.theApp = previousEngine; }
	}
}
