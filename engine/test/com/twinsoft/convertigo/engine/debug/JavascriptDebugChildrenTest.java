package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

public class JavascriptDebugChildrenTest {
    @org.junit.Rule public final FrozenInspectionClock inspectionClock = new FrozenInspectionClock();
    @org.junit.Before public void editingEnabledFixture() throws Exception {
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.WEB_STUDIO,"enabled");
    }

    private JSONObject paused(JavascriptDebugSession session, long previous) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            var state = session.state();
            if (state.getBoolean("stopped") && state.getLong("pauseId") > previous) return state;
            Thread.sleep(5);
        }
        throw new AssertionError("No child pause: " + session.state());
    }
    private JavascriptDebugSession.Participant plugin(JavascriptDebugSession session, Object worker) {
        var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
        var inspect = (Function) cx.evaluateString(scope,
            "(function(){return '{\"scopes\":[{\"name\":\"Input\",\"variables\":[]}]}';})", "child-inspect", 1, null);
        return session.participant("fake", worker, inspect);
    }
    private String node(String name, int line) { return "{\"url\":\"fake:" + name + "\",\"line\":" + line + ",\"label\":\"" + name + "\",\"iteration\":7}"; }
    private int child(JavascriptDebugSession session, JavascriptDebugSession.Parent parent, String name,
            AtomicBoolean cancelled, AtomicInteger budgets) throws Exception {
        try (var cx = Context.enter()) {
            var worker = session.joinChild(parent, cancelled::get, () -> {
                budgets.incrementAndGet(); return () -> budgets.decrementAndGet();
            });
            try {
                var plugin = plugin(session, worker);
                plugin.enter(name, node(name, 1));
                try { plugin.at(name, "before"); plugin.at(name, "after"); return 42; }
                finally { plugin.leave(name); }
            } finally { session.finishParticipantWorker(); }
        }
    }
    @Test public void explicitAncestrySteppingReturnAndConcurrentSibling() throws Exception {
        try (var session = new JavascriptDebugSession(10_000); var pool = Executors.newCachedThreadPool()) {
            var captured = new CompletableFuture<JavascriptDebugSession.Parent>();
            var finishedChild = new CountDownLatch(1); var budgets = new AtomicInteger();
            session.breakpoint("fake:caller", 1, true, false);
            var root = pool.submit(() -> {
                try (var cx = Context.enter()) {
                    var worker = session.joinParticipantWorker();
                    try {
                        var plugin = plugin(session, worker);
                        plugin.enter("caller", node("caller", 1)); plugin.at("caller", "before");
                        captured.complete(session.parent(worker)); assertTrue(finishedChild.await(5, TimeUnit.SECONDS));
                        plugin.at("caller", "after"); plugin.leave("caller");
                        plugin.enter("next", node("next", 2)); plugin.at("next", "before"); plugin.leave("next");
                        return 44;
                    } finally { session.finishParticipantWorker(); }
                }
            });
            var caller = paused(session, 0); long id = caller.getLong("pauseId");
            session.resume("stepInto", id);
            var parent = captured.get(5, TimeUnit.SECONDS);
            var first = pool.submit(() -> { try { return child(session, parent, "child", new AtomicBoolean(), budgets); } finally { finishedChild.countDown(); } });
            var inside = paused(session, id); id = inside.getLong("pauseId");
            var stack = inside.getJSONArray("frames"); assertEquals(2, stack.length());
            var current = stack.getJSONObject(0); var ancestor = stack.getJSONObject(1);
            assertTrue(current.getBoolean("inspectable")); assertFalse(ancestor.getBoolean("inspectable"));
            var a = current.getJSONObject("activation"); var b = ancestor.getJSONObject("activation");
            assertEquals(b.getString("id"), a.getString("parent")); assertEquals(b.getString("rootRequest"), a.getString("rootRequest"));
            assertNotEquals(b.getString("worker"), a.getString("worker")); assertNotEquals(b.getString("invocation"), a.getString("invocation"));
            assertEquals(1, a.getInt("depth")); assertEquals(7, a.getInt("iteration"));
            assertTrue(session.variables(0, id).toString().contains("Input"));
            final long pause = id;
            assertThrows(IllegalStateException.class, () -> session.variables(1, pause));
            session.breakpoint("fake:sibling", 1, true, false);
            assertEquals(42, (int) pool.submit(() -> child(session, parent, "sibling", new AtomicBoolean(), budgets)).get(2, TimeUnit.SECONDS));
            assertEquals(id, session.state().getLong("pauseId")); assertFalse(first.isDone()); assertEquals(1, budgets.get());
            session.resume("stepOut", id);
            var returned = paused(session, id); id = returned.getLong("pauseId");
            assertEquals("return", returned.getJSONArray("frames").getJSONObject(0).getString("phase"));
            assertEquals("fake:caller", returned.getJSONArray("frames").getJSONObject(0).getString("url"));
            session.resume("stepOver", id);
            var next = paused(session, id); assertEquals("fake:next", next.getJSONArray("frames").getJSONObject(0).getString("url"));
            session.resume("go", next.getLong("pauseId"));
            assertEquals(42, (int) first.get(2, TimeUnit.SECONDS)); assertEquals(44, (int) root.get(2, TimeUnit.SECONDS));
            assertEquals(0, budgets.get()); assertEquals(0, session.state().getInt("workers"));
            assertEquals(2, session.state().getJSONObject("transport").getInt("version"));
        }
    }
    @Test public void stopDetachesWithoutCancellingChildrenOrTheirParent() throws Exception {
        try (var session = new JavascriptDebugSession(10_000); var pool = Executors.newSingleThreadExecutor(); var cx = Context.enter()) {
            var root = session.joinParticipantWorker();
            try {
                var caller = plugin(session, root); caller.enter("caller", node("caller", 1));
                var parent = session.parent(root); var budgets = new AtomicInteger(); session.breakpoint("fake:child", 1, true, false);
                var child = pool.submit(() -> child(session, parent, "child", new AtomicBoolean(), budgets));
                paused(session, 0); session.close();
                assertEquals(42, (int) child.get(2, TimeUnit.SECONDS)); assertEquals(0, budgets.get());
                assertFalse(session.state().getBoolean("attached")); caller.leave("caller");
            } finally { session.finishParticipantWorker(); }
        }
    }
    @Test public void parentCancellationReleasesPauseAndFailsAtTheSafeBoundary() throws Exception {
        try (var session = new JavascriptDebugSession(10_000); var pool = Executors.newSingleThreadExecutor(); var cx = Context.enter()) {
            var root = session.joinParticipantWorker();
            try {
                var caller = plugin(session, root); caller.enter("caller", node("caller", 1));
                var cancelled = new AtomicBoolean(); session.identifyRoot(root, "cancelled-root", cancelled::get);
                var parent = session.parent(root); var budgets = new AtomicInteger();
                session.breakpoint("fake:child", 1, true, false);
                var child = pool.submit(() -> child(session, parent, "child", cancelled, budgets));
                paused(session, 0); cancelled.set(true);
                var error = assertThrows(ExecutionException.class, () -> child.get(2, TimeUnit.SECONDS));
                assertTrue(error.getCause() instanceof CancellationException); assertEquals(0, budgets.get());
                assertFalse(session.state().getBoolean("stopped")); assertTrue(session.state().getBoolean("attached"));
                assertTrue(session.state().getJSONArray("events").toString().contains("cancelled"));
                assertThrows(CancellationException.class, () -> caller.at("caller", "error"));
                assertFalse("Cancellation must not create another pause on the caller error", session.state().getBoolean("stopped"));
                caller.leave("caller");
            } finally { session.finishParticipantWorker(); }
        }
    }
    @Test public void childPauseRemainsBoundedWithoutCancellingBusinessWork() throws Exception {
        try (var session = new JavascriptDebugSession(80); var pool = Executors.newSingleThreadExecutor(); var cx = Context.enter()) {
            var root = session.joinParticipantWorker();
            try {
                var parent = session.parent(root); var budgets = new AtomicInteger(); session.breakpoint("fake:child", 1, true, false);
                var child = pool.submit(() -> child(session, parent, "child", new AtomicBoolean(), budgets));
                paused(session, 0); assertEquals(42, (int) child.get(2, TimeUnit.SECONDS)); assertEquals(0, budgets.get());
                assertTrue(session.state().getString("notice").contains("resumed automatically"));
            } finally { session.finishParticipantWorker(); }
        }
    }
	@Test public void capturedConstructionBarrierAndPooledThreadCleanupAreExplicit() throws Exception {
		com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
		var key = com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.JAVASCRIPT_DEBUGGER;
		var previous = com.twinsoft.convertigo.engine.EnginePropertiesManager.getProperty(key);
		com.twinsoft.convertigo.engine.Engine.logAdmin = org.apache.log4j.Logger.getLogger("child-debugger-test");
		com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(key, "true");
		try (var pool = Executors.newSingleThreadExecutor()) {
			var session = JavascriptDebugger.session("children", true);
			var request = new JavascriptDebuggerTest().request("children", session.id(), true);
			try (var binding = JavascriptDebugger.bind(request); var cx = Context.enter()) {
				JavascriptDebugger.ParentLink token;
				try (var construction = JavascriptDebugger.providerConstruction()) { token = JavascriptDebugger.captureParent(); }
				assertNotNull(token);
				pool.submit(() -> { try (var child = JavascriptDebugger.bindChild(token); var childCx = Context.enter()) {
					assertFalse(JavascriptDebugger.participantSelected()); assertNull(childCx.getDebugger()); assertFalse(childCx.isInterpretedMode());
				} return null; }).get(2, TimeUnit.SECONDS);
				var selected = JavascriptDebugger.captureParent(); assertNotNull(selected);
				pool.submit(() -> { try (var child = JavascriptDebugger.bindChild(selected); var childCx = Context.enter()) {
					assertTrue(JavascriptDebugger.participantSelected()); assertNull(childCx.getDebugger()); assertFalse(childCx.isInterpretedMode());
				} return null; }).get(2, TimeUnit.SECONDS);
				assertEquals(1, session.state().getInt("workers"));
				pool.submit(() -> { try (var childCx = Context.enter()) { assertFalse(JavascriptDebugger.participantSelected()); } return null; }).get(2, TimeUnit.SECONDS);
			}
		} finally {
			JavascriptDebugger.stop("children");
			com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(key, previous);
		}
	}
    @Test public void aCompletedParentCannotAttachOrCancelTheNextTaskOfThePool() throws Exception {
        try(var session=new JavascriptDebugSession(1000);var pool=Executors.newSingleThreadExecutor();var cx=Context.enter()) {
            var root=session.joinParticipantWorker();var old=session.parent(root);session.finishParticipantWorker();
            pool.submit(()->{try(var childCx=Context.enter()) {
                assertNull(session.joinChild(old,()->true,()->{throw new AssertionError("Late budget lease");}));
                var fresh=session.joinParticipantWorker();
                try { assertNotNull(fresh);assertEquals(1,session.state().getInt("workers")); }
                finally { session.finishParticipantWorker(); }
            }return null;}).get(2,TimeUnit.SECONDS);
            assertEquals(0,session.state().getInt("workers"));
        }
    }

}
