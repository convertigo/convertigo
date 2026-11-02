/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.debug.JavascriptDebugSession;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.JsonValues;

/** Exercise the real provider boundary from a debugged legacy script, cold and cached. */
public class FlowDebuggerBoundaryTest {
    private JSONObject paused(JavascriptDebugSession session, long previous) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            var state = session.state();
            if (state.getBoolean("stopped") && state.getLong("pauseId") > previous) return state;
            Thread.sleep(5);
        }
        throw new AssertionError("Legacy breakpoint was not reached");
    }

    @Test public void flowSucceedsAndLegacyBreakpointsSurviveColdAndCachedCalls() throws Exception {
        exercise(false);
    }

    @Test public void flowFailureAlsoRestoresLegacyDebugging() throws Exception {
        exercise(true);
    }

    @Test public void privateImplementationStepsAndConcurrentCachedCallsStayIndependent() throws Exception {
        exercisePrivate(false, false);
    }

    @Test public void stopReleasesPrivateImplementationAndRestoresCaller() throws Exception {
        exercisePrivate(true, false);
    }

    @Test public void interruptionReleasesPrivateImplementationAndRestoresCaller() throws Exception {
        exercisePrivate(false, true);
    }

    @Test public void sharedBuildersAndInitializersCannotAttachPrivateFunctions() throws Exception {
        EnginePropertiesManager.initProperties();
        try (var session = new JavascriptDebugSession(); var lease = session.enter()) {
            var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
            session.breakOnEnter(true); session.breakOnReturn(true); session.requestPause();
            try (var suspension = com.twinsoft.convertigo.engine.debug.JavascriptDebugger.suspendForFlow()) {
                var runtime = new FlowRuntimeApi(cx);
                assertTrue(com.twinsoft.convertigo.engine.debug.JavascriptDebugger.providerDebugging());
                runtime.prepare("module:fixture", () -> {
                    assertNull(cx.getDebugger());
                    assertFalse(com.twinsoft.convertigo.engine.debug.JavascriptDebugger.providerDebugging());
                    try {
                        com.twinsoft.convertigo.engine.debug.JavascriptDebugger.compileProviderImplementation("({})", "invalid", scope);
                        fail("A shared builder attached private code");
                    } catch (IllegalStateException expected) { }
                    return new Object();
                });
                var instance = com.twinsoft.convertigo.engine.debug.JavascriptDebugger.compileProviderImplementation(
                        "(function(){debugger;return {run:function(){throw new Error('fixture');}};}())", "private-init", scope);
                assertNotNull(instance);
                assertNull(cx.getDebugger());
                assertFalse(cx.isInterpretedMode());
                assertFalse(session.state().getBoolean("stopped"));
                try {
                    com.twinsoft.convertigo.engine.debug.JavascriptDebugger.compileProviderImplementation("(", "invalid", scope);
                    fail("Invalid source must fail");
                } catch (org.mozilla.javascript.EvaluatorException expected) { }
                assertNull(cx.getDebugger());
                assertFalse(cx.isInterpretedMode());
            }
            assertSame(session, cx.getDebugger());
            assertTrue(cx.isInterpretedMode());
        }
    }

    private void exercisePrivate(boolean detach, boolean interrupt) throws Exception {
        var previous = Engine.theApp;
        var directory = Files.createTempDirectory("provider-private-debug-");
        EnginePropertiesManager.initProperties();
        Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("provider-debug-test");
        var file = directory.resolve("_flow/Engine.js");
        Files.createDirectories(file.getParent());
        // Factory initialization must ignore debugger statements and function-entry/return settings.
        var implementation = "(function(){ debugger; return {run:function(n){\nvar local=n+1;\nlocal++;\nreturn local-1;\n}}; }())";
        Files.writeString(file, """
            ({runObject:function(text,detach){
              var D=Packages.com.twinsoft.convertigo.engine.debug.JavascriptDebugger;
              if(org.mozilla.javascript.Context.getCurrentContext().getDebugger()!=null) throw new Error('attached engine');
              if (!D.providerDebugging()) return detach({ok:true,result:42});
              var cx=org.mozilla.javascript.Context.getCurrentContext();
              var boundary=cx.newObject(this);
              boundary.engineGlobalSentinel='hidden';
              var instance=D.compileProviderImplementation(%s,'ProviderFixture/_flow/blocks/probe.block.js',boundary);
              var value=D.callProviderFunction(instance.run,instance,[41],boundary);
              if(cx.getDebugger()!=null || cx.isInterpretedMode()) throw new Error('settings not restored');
              return detach({ok:true,result:value});
            }})
            """.formatted(new org.codehaus.jettison.json.JSONArray().put(implementation).toString().substring(1,
                    new org.codehaus.jettison.json.JSONArray().put(implementation).toString().length()-1)));
        try (var pool = Executors.newFixedThreadPool(2); var session = new JavascriptDebugSession()) {
            var project = new Project() { @Override public String getDirPath() { return directory.toString(); } };
            project.setName("ProviderFixture");
            Engine.theApp = new Engine();
            Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
                @Override public Project getOriginalProjectByName(String name, boolean check) { return project; }
                @Override public Project getLoadedProjectByName(String name) { return null; }
            };
            FlowEngineBridge.clearCaches();
            var bridge = new FlowEngineBridge();
            String url = "ProviderFixture/_flow/blocks/probe.block.js";
            session.breakpoint(url, 3, true);
            session.breakpoint("private-caller", 2, true);
            session.breakpoint("private-caller", 3, true);
            var worker = new java.util.concurrent.atomic.AtomicReference<Thread>();
            var future = pool.submit(() -> {
                worker.set(Thread.currentThread());
                try (var lease = session.enter()) {
                    var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
                    scope.put("callProvider", scope, new BaseFunction() {
                        @Override public Object call(Context current, Scriptable parent, Scriptable self, Object[] args) {
                            try {
                                return ((JSONObject) JsonValues.toLegacy(bridge.invokeValue("ProviderFixture.Engine", "run",
                                        new JSONObject(), null, null, null))).getInt("result");
                            } catch (Exception e) { throw new AssertionError(e); }
                            finally { assertSame(session, current.getDebugger()); assertTrue(current.isInterpretedMode()); }
                        }
                    });
                    return RhinoUtils.evalCachedJavascript(cx, scope, "var before=1;\nvar result=callProvider();\nresult;", "private-caller", 1, null);
                }
            });
            var before = paused(session, 0);
            session.resume("go", before.getLong("pauseId"));
            var block = paused(session, before.getLong("pauseId"));
            assertEquals(url, block.getJSONArray("frames").getJSONObject(0).getString("url"));
            assertEquals(3, block.getJSONArray("frames").getJSONObject(0).getInt("line"));
            assertEquals(2, block.getJSONArray("sources").length());
            assertEquals(0, session.source(url).getJSONObject("location").getInt("offset"));
            assertEquals("42", session.evaluate(0, block.getLong("pauseId"), "local").getString("result"));
            var variables = session.variables(0, block.getLong("pauseId"));
            assertFalse(variables.toString().contains("engineGlobalSentinel"));
            assertTrue(variables.toString().contains("local"));
            var concurrent = pool.submit(() -> bridge.invokeValue("ProviderFixture.Engine", "run", new JSONObject(), null, null, null));
            assertEquals(42, ((JSONObject) JsonValues.toLegacy(concurrent.get(2, TimeUnit.SECONDS))).getInt("result"));
            if (detach) session.close();
            else if (interrupt) worker.get().interrupt();
            else {
                session.resume("stepOver", block.getLong("pauseId"));
                var next = paused(session, block.getLong("pauseId"));
                assertEquals(4, next.getJSONArray("frames").getJSONObject(0).getInt("line"));
                assertEquals("43", session.evaluate(0, next.getLong("pauseId"), "local").getString("result"));
                session.resume("stepOut", next.getLong("pauseId"));
                var after = paused(session, next.getLong("pauseId"));
                assertEquals("private-caller", after.getJSONArray("frames").getJSONObject(0).getString("url"));
                assertEquals(3, after.getJSONArray("frames").getJSONObject(0).getInt("line"));
                session.resume("go", after.getLong("pauseId"));
            }
            assertEquals(42, ((Number) future.get(5, TimeUnit.SECONDS)).intValue());
        } finally {
            FlowEngineBridge.clearCaches(); Engine.theApp = previous;
            org.apache.commons.io.FileUtils.deleteDirectory(directory.toFile());
        }
    }

    private void exercise(boolean failing) throws Exception {
        var previous = Engine.theApp;
        var directory = Files.createTempDirectory("flow-debug-boundary-");
        EnginePropertiesManager.initProperties();
        Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("flow-debug-test");
        var file = directory.resolve("_flow/Engine.js");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
            ({runObject:function(text,detach){
              if(org.mozilla.javascript.Context.getCurrentContext().getDebugger() != null)
                throw new Error('Flow must execute without the request debugger');
              if(JSON.parse(text).fail)throw new Error('expected provider failure');
              return detach({ok:true,result:42});
            }})
            """);
        try {
            var project = new Project() {
                @Override public String getDirPath() { return directory.toString(); }
            };
            project.setName("FlowDebugFixture");
            Engine.theApp = new Engine();
            Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
                @Override public Project getOriginalProjectByName(String name, boolean check) { return project; }
                @Override public Project getLoadedProjectByName(String name) { return null; }
            };
            FlowEngineBridge.clearCaches();
            var bridge = new FlowEngineBridge();
            for (int attempt = 0; attempt < 2; attempt++) {
                try (var pool = Executors.newSingleThreadExecutor(); var session = new JavascriptDebugSession()) {
                    session.breakpoint("legacy-flow-caller", 2, true);
                    session.breakpoint("legacy-flow-caller", 3, true);
                    var future = pool.submit(() -> {
                        try (var lease = session.enter()) {
                            var cx = Context.getCurrentContext();
                            var scope = cx.initStandardObjects();
                            var data = cx.getDebuggerContextData();
                            scope.put("callFlow", scope, new BaseFunction() {
                                @Override public Object call(Context current, Scriptable parent, Scriptable self, Object[] args) {
                                    try {
                                        var response = (JSONObject) JsonValues.toLegacy(bridge.invokeValue("FlowDebugFixture.Engine", "run",
                                                new JSONObject().put("fail", failing), null, null, null));
                                        assertFalse("The failure fixture must throw", failing);
                                        assertTrue(response.getBoolean("ok"));
                                        return response.getInt("result");
                                    } catch (EngineException e) {
                                        if (!failing) throw new AssertionError("Debugging must not fail the Flow", e);
                                        Throwable cause = e;
                                        while (cause.getCause() != null) cause = cause.getCause();
                                        assertTrue(cause.getMessage().contains("expected provider failure"));
                                        return 42;
                                    } catch (Exception e) {
                                        throw new AssertionError(e);
                                    } finally {
                                        assertSame(session, current.getDebugger());
                                        assertSame(data, current.getDebuggerContextData());
                                        assertTrue(current.isInterpretedMode());
                                        assertTrue(current.isGeneratingDebug());
                                    }
                                }
                            });
                            return RhinoUtils.evalCachedJavascript(cx, scope,
                                    "var before=1;\nvar result=callFlow();\nresult;", "legacy-flow-caller", 1, null);
                        }
                    });
                    var before = paused(session, 0);
                    assertEquals(2, before.getJSONArray("frames").getJSONObject(0).getInt("line"));
                    session.resume(attempt == 0 ? "go" : "stepOver", before.getLong("pauseId"));
                    var after = paused(session, before.getLong("pauseId"));
                    assertEquals(3, after.getJSONArray("frames").getJSONObject(0).getInt("line"));
                    assertEquals("Flow orchestration runs without line debugging; no private block implementation has executed yet.", after.getString("notice"));
                    assertEquals("42", session.evaluate(0, after.getLong("pauseId"), "result").getString("result"));
                    assertEquals(1, after.getJSONArray("sources").length());
                    session.resume("go", after.getLong("pauseId"));
                    assertEquals(42, ((Number) future.get(5, TimeUnit.SECONDS)).intValue());
                }
            }
        } finally {
            FlowEngineBridge.clearCaches();
            Engine.theApp = previous;
            org.apache.commons.io.FileUtils.deleteDirectory(directory.toFile());
        }
    }
}
