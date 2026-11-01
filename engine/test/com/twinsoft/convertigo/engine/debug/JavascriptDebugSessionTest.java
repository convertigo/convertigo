package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.codehaus.jettison.json.JSONObject;

public class JavascriptDebugSessionTest {
    private static final String SCRIPT = """
        function add(value) {
          var local = value + 1;
          local += 2;
          return local;
        }
        var input = 4;
        var result = add(input);
        result += 10;
        result;
        """;
    private Future<Object> execute(ExecutorService pool, JavascriptDebugSession session, String script) {
        return pool.submit(() -> {
            try (var lease = session.enter()) {
                var cx = Context.getCurrentContext();
                return com.twinsoft.convertigo.engine.util.RhinoUtils.evalCachedJavascript(cx, cx.initStandardObjects(), script, "probe", 1, null);
            }
        });
    }
    private JSONObject paused(JavascriptDebugSession session, long previous) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var state = session.state();
            if (state.getBoolean("stopped") && state.getLong("pauseId") > previous) return state;
            Thread.sleep(5);
        }
        throw new AssertionError("No pause: " + session.state());
    }
    @Test public void breakpointFramesScopeEvaluationAndAllSteps() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.breakpoint("probe", 7, true);
            var task = execute(pool, session, SCRIPT);
            long pause = paused(session, 0).getLong("pauseId");
            assertEquals("4", session.evaluate(0, pause, "input").getString("result"));
            var values = session.variables(0, pause).getJSONArray("scope");
            int inputs = 0;
            for (int i = 0; i < values.length(); i++) if ("input".equals(values.getJSONObject(i).getString("name"))) {
                assertEquals("4", values.getJSONObject(i).getString("value")); inputs++;
            }
            assertEquals(1, inputs);
            assertFalse(task.isDone());
            session.resume("stepInto", pause);
            pause = paused(session, pause).getLong("pauseId");
            assertEquals(2, session.state().getJSONArray("frames").length());
            assertEquals("4", session.evaluate(0, pause, "value").getString("result"));
            assertEquals("4", session.evaluate(1, pause, "input").getString("result"));
            assertTrue(session.variables(0, pause).toString().contains("value"));
            session.resume("stepOver", pause);
            pause = paused(session, pause).getLong("pauseId");
            if (session.state().getJSONArray("frames").getJSONObject(0).getInt("line") == 2) {
                session.resume("stepOver", pause); pause = paused(session, pause).getLong("pauseId");
            }
            assertEquals(session.state().toString(), "5", session.evaluate(0, pause, "local").getString("result"));
            session.resume("stepOut", pause);
            pause = paused(session, pause).getLong("pauseId");
            assertEquals(1, session.state().getJSONArray("frames").length());
            assertEquals("7", session.evaluate(0, pause, "result").getString("result"));
            final long stale = pause - 1;
            assertThrows(IllegalStateException.class, () -> session.resume("go", stale));
            session.resume("go", pause);
            assertEquals(17.0, ((Number) task.get(5, TimeUnit.SECONDS)).doubleValue(), 0);
            assertFalse(session.state().getBoolean("running"));
        }
    }
    @Test public void concurrentRequestsAndDetachAreIndependent() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var a = new JavascriptDebugSession(); var b = new JavascriptDebugSession()) {
            a.requestPause(); b.requestPause();
            var first = execute(pool, a, SCRIPT); var second = execute(pool, b, SCRIPT);
            paused(a, 0); var stopped = paused(b, 0);
            assertNotEquals(a.id(), b.id());
            assertThrows(IllegalStateException.class, a::enter);
            assertEquals(42, (int) pool.submit(() -> { try (var cx = Context.enter()) { assertNull(cx.getDebugger()); return 42; } }).get());
            a.close(); assertEquals(17, ((Number) first.get(5, TimeUnit.SECONDS)).intValue());
            assertFalse(second.isDone());
            b.resume("go", stopped.getLong("pauseId")); second.get(5, TimeUnit.SECONDS);
        }
    }
    @Test public void inspectionSkipsGettersAndEvaluationHasDeadline() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.breakpoint("probe", 4, true);
            var task = execute(pool, session, "var touched = 0;\nObject.defineProperty(this, 'getter', { enumerable:true, get:function(){ touched++; return 123; } });\nlet lexical = 8;\nlexical;\n");
            long pause = paused(session, 0).getLong("pauseId");
            assertTrue(session.variables(0, pause).toString().contains("<accessor>"));
            assertEquals("0", session.evaluate(0, pause, "touched").getString("result"));
            assertEquals("8", session.evaluate(0, pause, "lexical").getString("result"));
            assertTrue(session.evaluate(0, pause, "while(true){}").getBoolean("evaluationError"));
            assertEquals("9", session.evaluate(0, pause, "lexical + 1").getString("result"));
            session.resume("go", pause); task.get(5, TimeUnit.SECONDS);
        }
    }
    @Test public void exceptionAndDebuggerStatementPauseWithoutLoggingValues() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.breakOnExceptions(true);
            var task = execute(pool, session, "try { throw new Error('private-value'); } catch (e) {}\ndebugger;\n42;");
            var state = paused(session, 0); long pause = state.getLong("pauseId");
            assertFalse(state.toString().contains("private-value"));
            assertTrue(state.toString().contains("exception"));
            session.resume("go", pause);
            pause = paused(session, pause).getLong("pauseId");
            session.resume("go", pause);
            assertEquals(42, ((Number)task.get(5, TimeUnit.SECONDS)).intValue());
        }
    }
    @Test public void abandonedPauseReleasesItsWorker() throws Exception {
        try (var pool = Executors.newCachedThreadPool(); var session = new JavascriptDebugSession()) {
            session.requestPause(); var task = execute(pool, session, "42;"); paused(session, 0);
            var seen = JavascriptDebugSession.class.getDeclaredField("seen"); seen.setAccessible(true);
            seen.setLong(session, System.currentTimeMillis() - JavascriptDebugSession.IDLE_MILLIS - 1);
            assertEquals(42, ((Number)task.get(3, TimeUnit.SECONDS)).intValue());
            assertFalse(session.state().getBoolean("attached"));
            assertEquals(0, session.state().getJSONArray("frames").length());
        }
    }
    @Test public void sourcesAndEventsStayBounded() throws Exception {
        try (var session = new JavascriptDebugSession(); var lease = session.enter()) {
            var cx = Context.getCurrentContext();
            for (int i = 0; i < 80; i++) cx.compileString("42;", "source-" + i, 1, null);
            assertEquals(64, session.state().getJSONArray("sources").length());
            assertEquals(64, session.state().getJSONArray("events").length());
            assertThrows(IllegalArgumentException.class, () -> session.source("source-0"));
            assertEquals("42;", session.source("source-79").getString("source"));
        }
    }

}
