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
                    try (var construction = JavascriptDebugger.providerConstruction()) {
                        plugin.at("first", "before");
                        assertFalse(session.state().getBoolean("stopped"));
                    }
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
    @Test public void logicalStepOverStopsBeforeTheNextSiblingAndErrorFramesRemainInspectable() throws Exception {
        try (var session = new JavascriptDebugSession(); var pool = Executors.newSingleThreadExecutor()) {
            session.breakpoint("fake:steps",1,true,false);
            var task=pool.submit(()->{
                try(var lease=session.enter();var suspension=JavascriptDebugger.suspendForFlow()) {
                    var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                    var inspect=(Function)cx.evaluateString(scope,"(function(){return '{\"scopes\":[]}';})","inspect",1,null);
                    var plugin=JavascriptDebugger.participant("fake",inspect);
                    plugin.enter("first","{\"url\":\"fake:steps\",\"line\":1}");plugin.at("first","before");
                    plugin.enter("child","{\"url\":\"fake:child\",\"line\":1}");plugin.at("child","before");plugin.at("child","after");plugin.leave("child");
                    plugin.at("first","after");plugin.leave("first");
                    plugin.enter("next","{\"url\":\"fake:steps\",\"line\":2}");plugin.at("next","before");plugin.at("next","error");plugin.leave("next");return 42;
                }
            });
            var first=paused(session,0);session.resume("stepOver",first.getLong("pauseId"));
            var next=paused(session,first.getLong("pauseId"));
            assertEquals("next",next.getJSONArray("frames").getJSONObject(0).getString("frameId"));
            assertEquals("before",next.getJSONArray("frames").getJSONObject(0).getString("phase"));
            session.resume("go",next.getLong("pauseId"));
            var error=paused(session,next.getLong("pauseId"));
            assertEquals("error",error.getJSONArray("frames").getJSONObject(0).getString("phase"));
            assertNotNull(session.variables(0,error.getLong("pauseId")));
            session.resume("go",error.getLong("pauseId"));assertEquals(42,(int)task.get(5,TimeUnit.SECONDS));
        }
    }
    @Test public void participantScopesReuseOpaqueReferencesAndBoundedPages() throws Exception {
        try (var session = new JavascriptDebugSession(); var pool = Executors.newSingleThreadExecutor()) {
            session.breakpoint("fake:objects", 1, true, false);
            var future = pool.submit(() -> {
                try (var lease = session.enter(); var suspended = JavascriptDebugger.suspendForFlow()) {
                    var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
                    cx.evaluateString(scope, "var data={items:Array.from({length:250},function(_,i){return i;}),nested:{value:42}};data.again=data.nested;data.self=data;Object.defineProperty(data,'guard',{enumerable:true,get:function(){throw Error('getter invoked');}});for(var i=0;i<250;i++)data['key'+i]=i;", "data", 1, null);
                    var inspect = (Function) cx.evaluateString(scope,
                        "(function(){return JSON.stringify({scopes:[JSON.parse(String(plugin.scope('Arguments',data)))]});})", "inspect", 1, null);
                    var plugin = JavascriptDebugger.participant("fake", inspect);
                    scope.put("plugin", scope, Context.javaToJS(plugin, scope));
                    assertThrows(IllegalStateException.class, () -> plugin.scope("outside", scope));
                    plugin.enter("node", "{\"url\":\"fake:objects\",\"line\":1}");
                    plugin.at("node", "before"); plugin.leave("node"); return true;
                }
            });
            var state = paused(session,0); long id=state.getLong("pauseId");
            var root=session.variables(0,id).getJSONArray("scopes").getJSONObject(0);
            assertEquals(20,root.getJSONArray("variables").length());assertEquals(20,root.getInt("nextOffset"));
            var rows=root.getJSONArray("variables");
            assertEquals(rows.getJSONObject(1).getString("ref"),rows.getJSONObject(2).getString("ref"));
            assertEquals(root.getString("ref"),rows.getJSONObject(3).getString("ref"));
            assertEquals("<accessor>",rows.getJSONObject(4).getString("value"));
            var arrayRef=rows.getJSONObject(0).getString("ref");
            var first=session.properties(id,arrayRef,0);assertEquals(100,first.getJSONArray("variables").length());
            assertEquals(100,first.getInt("nextOffset"));
            var last=session.properties(id,arrayRef,200);assertEquals(50,last.getJSONArray("variables").length());assertEquals(-1,last.getInt("nextOffset"));
            assertEquals(100,session.properties(id,root.getString("ref"),20).getJSONArray("variables").length());
            session.resume("go",id);assertTrue(future.get(5,TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class,()->session.properties(id,arrayRef,0));
        }
    }
    @Test public void pluginPreviewsNeverInvokeArbitraryHostCode() {
        assertEquals("42",JavascriptDebugger.preview(42));
        assertEquals("[Host object]",JavascriptDebugger.preview(new Object() {
            @Override public String toString() { throw new AssertionError("Host toString invoked"); }
        }));
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
