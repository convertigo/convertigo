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
                    assertEquals("Flow code runs without debugging", after.getString("notice"));
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
