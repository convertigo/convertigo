/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class JavascriptDebugLiveParticipantTest {
    @Test public void attachAfterStartKeepsTheOrdinaryContextAndHonorsConstructionBarriers() throws Exception {
        EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.Engine.logAdmin = org.apache.log4j.Logger.getLogger("debugger-live-test");
        var enabled = EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_DEBUGGER);
        var studio = EnginePropertiesManager.getProperty(PropertyName.WEB_STUDIO);
        EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "true");
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, "disabled");
        var entered = new CountDownLatch(1); var nextBlock = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            assertTrue(JavascriptDebugger.enabled()); assertFalse(JavascriptDebugger.rhinoEnabled());
            assertTrue(com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger.class
                .getAnnotation(com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.class).available_without_web_studio());
            var running = pool.submit(() -> {
                var request = new JavascriptDebuggerTest().request("live", null, true);
                try (var registration = JavascriptDebugger.bind(request, "Already running"); var cx = Context.enter()) {
                    assertFalse(cx.isInterpretedMode()); assertNull(cx.getDebugger());
                    var scope = cx.initStandardObjects();
                    var inspect = (Function) cx.evaluateString(scope, "(function(){return '{\"scopes\":[],\"result\":\"42\"}';})", "compiled-inspector", 1, null);
                    entered.countDown(); assertTrue(nextBlock.await(5, TimeUnit.SECONDS));
                    try (var construction = JavascriptDebugger.providerConstruction()) {
                        assertNull(JavascriptDebugger.participant("fake", inspect));
                        assertFalse(JavascriptDebugger.participantSelected());
                    }
                    var participant = JavascriptDebugger.participant("fake", inspect); assertNotNull(participant);
                    participant.enter("next", "{\"url\":\"fake:next\",\"line\":1}");
                    try (var construction = JavascriptDebugger.providerConstruction()) {
                        participant.at("next", "before");
                        assertFalse(JavascriptDebugger.session("live", false).state().getBoolean("stopped"));
                    }
                    participant.at("next", "before"); participant.leave("next");
                    assertFalse(cx.isInterpretedMode()); assertNull(cx.getDebugger());
                    assertFalse(JavascriptDebugger.providerDebugging());
                    return 42;
                }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals(1, JavascriptDebugger.targets("live").length());
            assertEquals(0, JavascriptDebugger.targets("other").length());
            var session = JavascriptDebugger.session("live", true);
            JavascriptDebugger.session("other", true);
            var target = JavascriptDebugger.targets("live").getJSONObject(0).getString("id");
            assertThrows(IllegalArgumentException.class, () -> JavascriptDebugger.attach("other", target));
            session.breakpoint("fake:next", 1, true, false);
            JavascriptDebugger.attach("live", target);
            assertTrue(session.state().getBoolean("running"));
            assertEquals(1, session.state().getInt("workers"));
            nextBlock.countDown();
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!session.state().getBoolean("stopped") && System.nanoTime() < end) Thread.sleep(5);
            var state = session.state(); assertTrue(state.getBoolean("stopped"));
            assertEquals("fake", state.getJSONArray("frames").getJSONObject(0).getString("kind"));
            assertEquals("42", session.evaluate(0, state.getLong("pauseId"), "path").getString("result"));
            JavascriptDebugger.stop("live");
            assertEquals(42, (int) running.get(5, TimeUnit.SECONDS));
            assertEquals(0, JavascriptDebugger.targets("live").length());
            JavascriptDebugger.stop("other"); assertFalse(JavascriptDebugger.signal.active);
        } finally {
            nextBlock.countDown(); JavascriptDebugger.stop("live"); JavascriptDebugger.stop("other");
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, enabled);
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, studio);
        }
    }
    @Test public void emptyStudioSelectionDoesNotEnterRhinoOrChangeTheCompilationMode() throws Exception {
        EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.Engine.logAdmin = org.apache.log4j.Logger.getLogger("debugger-live-test");
        var enabled = EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_DEBUGGER);
        var studio = EnginePropertiesManager.getProperty(PropertyName.WEB_STUDIO);
        EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "true");
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, "enabled");
        try {
            var session = JavascriptDebugger.session("compiled", true);
            var request = new JavascriptDebuggerTest().request("compiled", session.id(), true);
            try (var binding = JavascriptDebugger.bind(request)) {
                assertNull(Context.getCurrentContext());
                try (var cx = Context.enter()) { assertFalse(cx.isInterpretedMode()); assertNull(cx.getDebugger()); }
            }
            assertFalse(session.state().getBoolean("running"));
            session.breakpoint("Project/code.js", 1, true);
            try (var binding = JavascriptDebugger.bind(request)) {
                assertTrue(Context.getCurrentContext().isInterpretedMode());
                assertSame(session, Context.getCurrentContext().getDebugger());
            }
        } finally {
            JavascriptDebugger.stop("compiled");
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, enabled);
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, studio);
        }
    }
}
