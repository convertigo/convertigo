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
            assertEquals("8", find(variables.getJSONArray("scope"), "lexical").getString("value"));
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
            // Rhino localizes the rest of the message with the JVM locale.
            assertTrue(session.evaluate(0, pause, "missingVariable").getString("result").startsWith("ReferenceError: \"missingVariable\""));
            String ref = many.getString("ref");
            session.resume("stepOver", pause);
            long next = paused(session, pause).getLong("pauseId");
            assertThrows(IllegalStateException.class, () -> session.properties(pause, ref, 0));
            assertThrows(java.util.concurrent.ExecutionException.class, () -> session.properties(next, ref, 0));
            session.close(); assertNotNull(future.get(2, TimeUnit.SECONDS));
        }
    }

    public static class HostileDecimal extends java.math.BigDecimal {
        private static final long serialVersionUID = 1L;
        public HostileDecimal() { super("3"); }
        @Override public String toString() { throw new AssertionError("Must not stringify user subclasses"); }
    }
    @Test public void wrappedJavaScalarsShowTheirValuesInScopesPropertiesAndEvaluation() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.breakpoint("java-scalars", 2, true);
            var future = pool.submit(() -> {
                try (var lease = session.enter()) {
                    var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
                    var values = new Object[][] {
                        {"javaText", "studio_JavascriptDebuggerTest:S:Probe"}, {"javaLong", 42L},
                        {"javaDouble", 42.0}, {"javaBoolean", true}, {"javaCharacter", 'Z'},
                        {"javaInteger", new java.math.BigInteger("12345678901234567890")},
                        {"javaDecimal", new java.math.BigDecimal("1.50")},
                        {"javaHostile", new HostileDecimal()}
                    };
                    for (var item : values) scope.put((String)item[0], scope,
                        new org.mozilla.javascript.NativeJavaObject(scope, item[1], org.mozilla.javascript.lc.type.TypeInfoFactory.GLOBAL.create(item[1].getClass())));
                    return cx.evaluateString(scope, "var nested={name:javaText};\njavaText;", "java-scalars", 1, null);
                }
            });
            long pause = paused(session, 0).getLong("pauseId");
            var values = session.variables(0, pause).getJSONArray("scope");
            var expected = new String[][] {
                {"javaText", "studio_JavascriptDebuggerTest:S:Probe"}, {"javaLong", "42"},
                {"javaDouble", "42"}, {"javaBoolean", "true"}, {"javaCharacter", "Z"},
                {"javaInteger", "12345678901234567890"}, {"javaDecimal", "1.50"}
            };
            for (var item : expected) {
                var variable = find(values, item[0]);
                assertEquals(item[0], item[1], variable.getString("value"));
                assertFalse(item[0], variable.getBoolean("expandable"));
                assertEquals(item[0], item[1], session.evaluate(0, pause, item[0]).getString("result"));
            }
            assertEquals("java.lang.String", find(values, "javaText").getString("type"));
            var nested = find(values, "nested");
            assertEquals("studio_JavascriptDebuggerTest:S:Probe", find(
                session.properties(pause, nested.getString("ref"), 0).getJSONArray("variables"), "name").getString("value"));
            assertTrue(find(values, "javaHostile").getString("value").startsWith("[Java "));
            assertTrue(session.evaluate(0, pause, "javaHostile").getString("result").startsWith("[Java "));
            session.resume("go", pause); future.get(2, TimeUnit.SECONDS);
        }
    }
    @Test public void wrappedJavaStringsKeepPreviewAndFullValueLimits() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.breakpoint("java-long-string", 2, true);
            var future = pool.submit(() -> {
                try (var lease = session.enter()) {
                    var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
                    scope.put("javaText", scope, new org.mozilla.javascript.NativeJavaObject(scope, "x".repeat(70000), org.mozilla.javascript.lc.type.TypeInfo.STRING));
                    return cx.evaluateString(scope, "var holder={value:javaText};\njavaText;", "java-long-string", 1, null);
                }
            });
            long pause = paused(session, 0).getLong("pauseId");
            var text = find(session.variables(0, pause).getJSONArray("scope"), "javaText");
            assertEquals("x".repeat(500) + "…", text.getString("value"));
            assertTrue(text.getBoolean("fullValue"));
            var full = session.value(pause, text.getString("ref"));
            assertEquals(65536, full.getString("value").length());
            assertTrue(full.getBoolean("truncated"));
            assertEquals("x".repeat(500) + "…", session.evaluate(0, pause, "javaText").getString("result"));
            session.resume("go", pause); future.get(2, TimeUnit.SECONDS);
        }
    }

}
