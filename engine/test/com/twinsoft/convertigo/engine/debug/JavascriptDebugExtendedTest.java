package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class JavascriptDebugExtendedTest {
    private JSONObject paused(JavascriptDebugSession session, long previous) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            var state = session.state();
            if (state.getBoolean("stopped") && state.getLong("pauseId") > previous) return state;
            Thread.sleep(5);
        }
        throw new AssertionError("No pause: " + session.state());
    }
    private Future<Object> run(ExecutorService pool, JavascriptDebugSession session, String source) {
        return pool.submit(() -> {
            try (var lease = session.enter()) {
                var cx = Context.getCurrentContext();
                return RhinoUtils.evalCachedJavascript(cx, cx.initStandardObjects(), source, "edited", 1, null);
            }
        });
    }
    private JSONObject find(JSONArray values, String name) throws Exception {
        for (int i = 0; i < values.length(); i++) if (name.equals(values.getJSONObject(i).getString("name"))) return values.getJSONObject(i);
        throw new AssertionError("Missing " + name + " in " + values);
    }
    @Test public void absolutePauseDeadlineSurvivesPolling() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession(250)) {
            session.requestPause(); var future = run(pool, session, "42;"); paused(session, 0);
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (!future.isDone() && System.nanoTime() < end) { session.touch(); session.state(); Thread.sleep(10); }
            assertEquals(42, ((Number) future.get(1, TimeUnit.SECONDS)).intValue());
            assertTrue(session.state().getString("notice").contains("automatically"));
            assertTrue(session.state().getBoolean("attached"));
        }
    }
    @Test public void oneChildPausesWhileSiblingContinuesAndStepsKeepTheirWorker() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession(); var root = session.enter()) {
            session.breakpoint("child", 2, true);
            var first = pool.submit(() -> {
                try (var lease = session.enterChild()) {
                    var cx = Context.getCurrentContext();
                    return cx.evaluateString(cx.initStandardObjects(), "let onlyChild=8;\nonlyChild++;\nonlyChild;", "child", 1, null);
                }
            });
            long pause = paused(session, 0).getLong("pauseId");
            assertEquals("8", session.evaluate(0, pause, "onlyChild").getString("result"));
            var sibling = pool.submit(() -> {
                try (var lease = session.enterChild()) {
                    var cx = Context.getCurrentContext();
                    return cx.evaluateString(cx.initStandardObjects(), "42;", "child", 2, null);
                }
            });
            assertEquals(42, ((Number) sibling.get(2, TimeUnit.SECONDS)).intValue());
            assertFalse(first.isDone());
            assertEquals(1, session.state().getJSONArray("frames").length());
            session.resume("stepOver", pause);
            pause = paused(session, pause).getLong("pauseId");
            assertEquals("9", session.evaluate(0, pause, "onlyChild").getString("result"));
            session.resume("go", pause);
            assertEquals(9, ((Number)first.get(2, TimeUnit.SECONDS)).intValue());
        }
    }
    @Test public void sourceEditsRemapBeforeExecutingAndDeletedLinesDoNotMove() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            String before = "var value=1;\nvalue += 2;\nvalue;";
            assertEquals(3, ((Number)run(pool, session, before).get(2, TimeUnit.SECONDS)).intValue());
            session.breakpoint("edited", 2, true);
            var future = run(pool, session, "// inserted\n" + before);
            long pause = paused(session, 0).getLong("pauseId");
            assertEquals(3, session.state().getJSONArray("frames").getJSONObject(0).getInt("line"));
            assertEquals(3, session.breakpoints("edited").getInt(0));
            session.resume("go", pause); future.get(2, TimeUnit.SECONDS);
            assertTrue(JavascriptDebugSession.remapBreakpoints(before, "var value=1;\nvalue;", Set.of(2), Set.of(1,2)).isEmpty());
            assertTrue(JavascriptDebugSession.remapBreakpoints("x;", "y;\nx;\nx;\nz;", Set.of(1), Set.of(1,2,3,4)).isEmpty());
        }
    }
    public static class Data {
        public int answer = 42;
        @Override public String toString() { throw new AssertionError("Must not stringify Java objects"); }
        public int getSecret() { throw new AssertionError("Must not invoke Java getters"); }
    }
    @Test public void treesIncludeLexicalArraysJavaFieldsCyclesAndSkipGetters() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.breakpoint("tree", 5, true);
            var future = pool.submit(() -> {
                try (var lease = session.enter()) {
                    var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
                    scope.put("javaData", scope, Context.javaToJS(new Data(), scope));
                    return cx.evaluateString(scope, "let lexical=8;\nvar object={items:[1,{answer:42}]};\nobject.self=object;\nObject.defineProperty(object,'bad',{enumerable:true,get:function(){throw Error('getter')}});\nlexical;", "tree", 1, null);
                }
            });
            long pause = paused(session, 0).getLong("pauseId");
            var variables = session.variables(0, pause); var scopes = variables.getJSONArray("scopes");
            JSONObject lexical = null, object = null, java = null;
            for (int i = 0; i < scopes.length(); i++) {
                var values = scopes.getJSONObject(i).getJSONArray("variables");
                for (int j = 0; j < values.length(); j++) {
                    var value = values.getJSONObject(j);
                    switch(value.getString("name")) { case "lexical" -> lexical=value; case "object" -> object=value; case "javaData" -> java=value; }
                }
            }
            assertNotNull(lexical); assertEquals("8", lexical.getString("value"));
            assertNotNull(object); assertFalse(object.has("children"));
            var children = session.properties(pause, object.getString("ref"), 0).getJSONArray("variables");
            assertTrue(find(children, "self").getBoolean("expandable"));
            assertEquals(object.getString("ref"), find(children, "self").getString("ref"));
            assertEquals("<accessor>", find(children, "bad").getString("value"));
            assertEquals("[Array]", find(children, "items").getString("value"));
            assertNotNull(java);
            assertEquals("42", find(session.properties(pause, java.getString("ref"), 0).getJSONArray("variables"), "answer").getString("value"));
            assertTrue(variables.toString().length() < 100_000);
            session.resume("go", pause); future.get(2, TimeUnit.SECONDS);
        }
    }
    @Test public void sharedCompilationIsRefusedBeforeCacheAccessAndAnnounced() throws Exception {
        try (var session = new JavascriptDebugSession(); var lease = session.enter()) {
            var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
            assertThrows(IllegalStateException.class, () -> FlowEngineBridge.evalSharedModule(cx, scope, "42;", "shared"));
            assertThrows(IllegalStateException.class, () -> FlowEngineBridge.compileScript(cx, "42;", "shared", null));
            assertEquals(JavascriptDebugSession.UNSUPPORTED_RUNTIME, session.state().getString("notice"));
        }
    }
    @Test public void propertyPagesLongValuesAndErrorsBelongToTheirPause() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.breakpoint("edited", 2, true);
            var future = run(pool, session, "var many={}; for(var i=0;i<230;i++)many['item'+i]=i; var text='x'.repeat(1200);\nmany;\ntext;");
            long pause = paused(session, 0).getLong("pauseId");
            var variables = session.variables(0, pause);
            var many = find(variables.getJSONArray("scope"), "many");
            var page = session.properties(pause, many.getString("ref"), 0);
            assertEquals(100, page.getJSONArray("variables").length()); assertEquals(100, page.getInt("nextOffset"));
            page = session.properties(pause, many.getString("ref"), 200);
            assertEquals(30, page.getJSONArray("variables").length()); assertEquals(-1, page.getInt("nextOffset"));
            assertEquals("229", find(page.getJSONArray("variables"), "item229").getString("value"));
            var text = find(variables.getJSONArray("scope"), "text");
            assertTrue(text.getBoolean("fullValue"));
            assertEquals(1200, session.value(pause, text.getString("ref")).getString("value").length());
            assertEquals("ReferenceError: \"missingVariable\" is not defined.", session.evaluate(0, pause, "missingVariable").getString("result"));
            String ref = many.getString("ref");
            session.resume("stepOver", pause);
            long next = paused(session, pause).getLong("pauseId");
            assertThrows(IllegalStateException.class, () -> session.properties(pause, ref, 0));
            assertThrows(java.util.concurrent.ExecutionException.class, () -> session.properties(next, ref, 0));
            session.close(); assertNotNull(future.get(2, TimeUnit.SECONDS));
        }
    }

}
