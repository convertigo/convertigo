/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

public class FlowStatelessRuntimeTest {
    private static java.lang.ref.WeakReference<Object> attachRequestValue(FlowRuntimeApi api, Context cx) {
        var value = new Object();
        try (var frame = api.enter(cx)) {
            frame.scope.put("context", frame.scope, Context.javaToJS(value, frame.scope));
            api.state().put("privateValue", api.state(), Context.javaToJS(value, frame.scope));
            return new java.lang.ref.WeakReference<>(value);
        }
    }

    @Test public void closedFramesReleaseRequestValuesWhileTheRevisionLives() throws Exception {
        try (var cx = Context.enter()) {
            var api = new FlowRuntimeApi(cx);
            var reference = attachRequestValue(api, cx);
            for (int i = 0; i < 50 && reference.get() != null; i++) {
                System.gc(); Thread.sleep(20);
            }
            assertNull("Closed frames must not retain request values", reference.get());
            try (var next = api.enter(cx)) {
                assertFalse(next.scope.has("context", next.scope));
                assertFalse(api.state().has("privateValue", api.state()));
            }
        }
    }

    @Test public void sharedSymbolCompilationUsesTheTypedStringOverload() throws Exception {
        var previous = com.twinsoft.convertigo.engine.Engine.theApp;
        var previousLog = com.twinsoft.convertigo.engine.Engine.logEngine;
        var pool = Executors.newFixedThreadPool(5);
        try (var cx = Context.enter()) {
            com.twinsoft.convertigo.engine.Engine.logEngine = org.apache.log4j.Logger.getLogger("symbol-dispatch-test");
            var app = new com.twinsoft.convertigo.engine.Engine();
            app.databaseObjectsManager = new com.twinsoft.convertigo.engine.DatabaseObjectsManager() {
                @Override public String getCompiledValue(String text) { return "compiled:" + text; }
                @Override public Object getCompiledValue(Object value) { throw new AssertionError("Wrong overload"); }
            };
            com.twinsoft.convertigo.engine.Engine.theApp = app;
            var api = new FlowRuntimeApi(cx);
            var scope = api.sharedScope();
            scope.put("api", scope, Context.javaToJS(api, scope));
            var function = (Function)cx.evaluateString(scope,
                    "(function(text){return String(api.compileSymbols(text));})", "symbols", 1, null);
            scope.sealObject();
            var jobs = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 5; i++) {
                var text = "request-" + i;
                jobs.add(pool.submit(() -> {
                    try (var thread = Context.enter()) {
                        for (int n = 0; n < 100; n++) try (var frame = api.enter(thread)) {
                            assertEquals("compiled:" + text, function.call(thread, frame.scope, frame.scope, new Object[]{text}));
                        }
                    }
                }));
            }
            for (var job : jobs) job.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            com.twinsoft.convertigo.engine.Engine.theApp = previous;
            com.twinsoft.convertigo.engine.Engine.logEngine = previousLog;
        }
    }

    @Test public void concurrentCallsKeepLexicalBindingsAndStateOnTheirOwnFrames() throws Exception {
        FlowRuntimeApi api;
        Function function;
        try (var cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            api = new FlowRuntimeApi(cx);
            var scope = api.sharedScope();
            scope.put("api", scope, Context.javaToJS(api, scope));
            function = (Function) cx.evaluateString(scope,
                    "(function(){ var state=api.state(); state.count=(state.count||0)+1; return __flowProjectDir+':'+context+':'+state.count; })",
                    "isolation", 1, null);
            scope.sealObject();
        }
        var pool = Executors.newFixedThreadPool(5);
        var ready = new CountDownLatch(5);
        var results = new ArrayList<java.util.concurrent.Future<?>>();
        try {
            for (int i = 0; i < 5; i++) {
                var name = "request-" + i;
                results.add(pool.submit(() -> {
                    try (var cx = Context.enter()) {
                        ready.countDown(); assertTrue(ready.await(5, TimeUnit.SECONDS));
                        for (int n = 0; n < 30; n++) try (var outer = api.enter(cx)) {
                            outer.scope.put("__flowProjectDir", outer.scope, name);
                            outer.scope.put("context", outer.scope, name);
                            assertEquals(name+":"+name+":1", Context.toString(function.call(cx, outer.scope, outer.scope, new Object[0])));
                            try (var inner = api.enter(cx)) {
                                inner.scope.put("__flowProjectDir", inner.scope, "child");
                                inner.scope.put("context", inner.scope, "child");
                                assertEquals("child:child:1", Context.toString(function.call(cx, inner.scope, inner.scope, new Object[0])));
                            }
                            assertEquals(name+":"+name+":2", Context.toString(function.call(cx, outer.scope, outer.scope, new Object[0])));
                        }
                        try { api.state(); fail("Request state retained after close"); } catch (IllegalStateException expected) { }
                    } catch (InterruptedException e) { throw new AssertionError(e); }
                }));
            }
            for (var task : results) task.get(20, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
    }
    @Test public void compilationIsSingleFlightFailuresRetryAndPublishedValuesAreStable() throws Exception {
        FlowRuntimeApi api;
        try (var cx = Context.enter()) { api = new FlowRuntimeApi(cx); }
        var builds = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(5);
        var values = new ArrayList<java.util.concurrent.Future<Object>>();
        try {
            for (int i=0;i<20;i++) values.add(pool.submit(() -> api.prepare("one", () -> {
                builds.incrementAndGet(); return new Object();
            })));
            var first = values.get(0).get();
            for (var value : values) assertSame(first, value.get());
            assertEquals(1, builds.get());
            try { api.prepare("failure", () -> { throw new IllegalArgumentException(); }); fail(); }
            catch (IllegalArgumentException expected) { }
            assertEquals("retry", api.prepare("failure", () -> "retry"));
        } finally { pool.shutdownNow(); }
    }
    @Test public void standardPrototypesAreSealedAndFrameFailureRestoresTheParent() {
        try (var cx = Context.enter()) {
            var api = new FlowRuntimeApi(cx);
            assertTrue(((ScriptableObject) ScriptableObject.getObjectPrototype(api.sharedScope())).isSealed());
            try (var parent = api.enter(cx)) {
                var state = api.state();
                assertTrue(com.twinsoft.convertigo.engine.util.SanitizedJson.ordinaryRealm(cx, parent.scope));
                try (var child = api.enter(cx)) { assertNotSame(state, api.state()); throw new IllegalArgumentException(); }
                catch (IllegalArgumentException expected) { }
                assertSame(state, api.state());
            }
        }
    }

    @Test public void evictingDynamicPlansDoesNotRebuildRevisionServices() throws Exception {
        try (var cx = Context.enter()) {
            var api = new FlowRuntimeApi(cx);
            var module = api.prepare("module:service", Object::new);
            for (int i = 0; i < 8193; i++) api.prepare("plan:" + i, Object::new);
            assertSame(module, api.prepare("module:service", () -> { throw new AssertionError("Module evicted"); }));
        }
    }

    @Test public void sharedLexicalWritesBelongToTheCurrentFrame() {
        try (var cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            var api = new FlowRuntimeApi(cx);
            var write = (Function) cx.evaluateString(api.sharedScope(),
                    "(function(value) { invocationGlobal = value; return invocationGlobal; })", "writes", 1, null);
            api.sharedScope().sealObject();
            try (var first = api.enter(cx)) {
                assertEquals("first", write.call(cx, first.scope, first.scope, new Object[]{"first"}));
                try (var second = api.enter(cx)) {
                    assertFalse(ScriptableObject.hasProperty(second.scope, "invocationGlobal"));
                    assertEquals("second", write.call(cx, second.scope, second.scope, new Object[]{"second"}));
                }
                assertEquals("first", ScriptableObject.getProperty(first.scope, "invocationGlobal"));
            }
            assertFalse(ScriptableObject.hasProperty(api.sharedScope(), "invocationGlobal"));
        }
    }
}
