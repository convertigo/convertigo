/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

public class JavascriptDebugParticipantTest {
    private JSONObject paused(JavascriptDebugSession session, long previous) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            var state = session.state();
            if (state.getBoolean("stopped") && state.getLong("pauseId") > previous) return state;
            Thread.sleep(5);
        }
        throw new AssertionError("Participant did not pause");
    }
    @Test public void fakePluginSharesSteppingInspectionAndMixedStack() throws Exception {
        try (var session = new JavascriptDebugSession(); var pool = Executors.newSingleThreadExecutor()) {
            session.breakpoint("fake:revision/node", 2, true);
            var future = pool.submit(() -> {
                try (var lease = session.enter(); var suspended = JavascriptDebugger.suspendForFlow()) {
                    var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
                    var inspect = (Function) cx.evaluateString(scope,
                        "(function(id,action,expression){return JSON.stringify(action==='variables'?{scopes:[{name:'Input',variables:[{name:'value',value:'41'}]}]}:{result:'42'});})", "fake-inspect", 1, null);
                    var plugin = JavascriptDebugger.participant("fake.plugin", inspect);
                    assertNotNull(plugin);
                    try (var construction = JavascriptDebugger.providerConstruction()) {
                        assertNull(JavascriptDebugger.participant("refused", inspect));
                    }
                    plugin.enter("first", "{\"url\":\"fake:revision/node\",\"line\":2,\"label\":\"Fake node\",\"source\":\"fake source\\nnode\\n\"}");
                    plugin.at("first", "before");
                    var value = JavascriptDebugger.compileProviderImplementation("({run:function(){\nvar value=41;\nvalue++;\nreturn value;\n}})", "Fake/project.js", scope);
                    var answer = JavascriptDebugger.callProviderFunction((Function)value.get("run",value), value, new Object[0], scope);
                    plugin.at("first", "after");
                    try { plugin.leave("wrong"); fail("Non-nested leave was accepted"); } catch (IllegalStateException expected) { }
                    plugin.leave("first");
                    return ((Number)answer).intValue();
                }
            });
            var before = paused(session, 0); long id = before.getLong("pauseId");
            assertEquals("fake.plugin", before.getJSONArray("frames").getJSONObject(0).getString("kind"));
            assertTrue(session.variables(0,id).toString().contains("Input"));
            assertEquals("42", session.evaluate(0,id,"plugin-owned-expression").getString("result"));
            session.resume("stepInto",id);
            var inside = paused(session,id); id = inside.getLong("pauseId");
            assertEquals("rhino",inside.getJSONArray("frames").getJSONObject(0).getString("kind"));
            assertEquals("fake.plugin",inside.getJSONArray("frames").getJSONObject(1).getString("kind"));
            session.resume("stepOut",id);
            var after = paused(session,id); id = after.getLong("pauseId");
            assertEquals("after",after.getJSONArray("frames").getJSONObject(0).getString("phase"));
            session.resume("go",id);
            assertEquals(42,(int)future.get(5,TimeUnit.SECONDS));
            try { session.variables(0,id); fail("Stale inspection accepted"); } catch (IllegalStateException expected) { }
        }
    }
    @Test public void stopAndMaximumPauseReleaseThePluginWorker() throws Exception {
        for (boolean stop : new boolean[]{true,false}) {
            try (var session = new JavascriptDebugSession(stop ? 5000 : 100); var pool = Executors.newSingleThreadExecutor()) {
                session.breakpoint("fake:timeout",1,true);
                var future = pool.submit(() -> {
                    try (var lease=session.enter(); var suspended=JavascriptDebugger.suspendForFlow()) {
                        var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                        var inspect=(Function)cx.evaluateString(scope,"(function(){return '{}';})","inspect",1,null);
                        var plugin=JavascriptDebugger.participant("fake",inspect);
                        plugin.enter("x","{\"url\":\"fake:timeout\",\"line\":1}");plugin.at("x","before");plugin.leave("x");
                        return true;
                    }
                });
                if(stop){paused(session,0);session.close();}
                assertTrue(future.get(5,TimeUnit.SECONDS));
                assertFalse(session.state().getBoolean("stopped"));
            }
        }
    }
}
