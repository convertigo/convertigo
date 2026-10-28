/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;

/** Preparations of a revision: one at a time for a key, at the same time for different keys. */
public class FlowRuntimePreparationTest {
    private FlowRuntimeApi api;
    private ExecutorService pool;

    @Before public void create() {
        try (var cx = Context.enter()) { api = new FlowRuntimeApi(cx); }
        pool = Executors.newCachedThreadPool();
    }
    @After public void release() { pool.shutdownNow(); }

    private int waiters() throws Exception {
        var field = FlowRuntimeApi.class.getDeclaredField("waiting"); field.setAccessible(true);
        var waiting = (Map<?, ?>) field.get(api);
        synchronized (waiting) { return waiting.size(); }
    }
    private void awaitWaiters(int count) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (waiters() < count) {
            if (System.nanoTime() > deadline) fail("Expected " + count + " waiting callers, got " + waiters());
            Thread.sleep(5);
        }
    }
    private long statistic(String name) { return ((Number) api.statistics().get(name)).longValue(); }

    @Test public void distinctKeysArePreparedAtTheSameTime() throws Exception {
        int count = 8;
        // every builder waits for all the others: a lock shared by the keys would time this barrier out
        var together = new CyclicBarrier(count);
        var results = new ArrayList<Future<Object>>();
        for (int i = 0; i < count; i++) {
            var key = (i % 2 == 0 ? "plan:" : "module:") + i;
            results.add(pool.submit(() -> api.prepare(key, () -> { together.await(10, TimeUnit.SECONDS); return key; })));
        }
        for (int i = 0; i < count; i++) assertEquals((i % 2 == 0 ? "plan:" : "module:") + i, results.get(i).get(20, TimeUnit.SECONDS));
        assertEquals(count, statistic("builds"));
        assertEquals(0, statistic("preparing"));
    }

    @Test public void aSlowPreparationDoesNotDelayAnotherKey() throws Exception {
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(1);
        var slow = pool.submit(() -> api.prepare("catalog:slow", () -> { started.countDown(); release.await(); return "slow"; }));
        assertTrue(started.await(10, TimeUnit.SECONDS));
        try {
            assertEquals("fast", pool.submit(() -> api.prepare("plan:fast", () -> "fast")).get(5, TimeUnit.SECONDS));
        } finally { release.countDown(); }
        assertEquals("slow", slow.get(10, TimeUnit.SECONDS));
    }

    @Test public void callersOfAKeyWaitForItsSinglePreparation() throws Exception {
        int count = 6;
        var builds = new AtomicInteger();
        Callable<Object> builder = () -> { builds.incrementAndGet(); awaitWaiters(count - 1); return new Object(); };
        var results = new ArrayList<Future<Object>>();
        for (int i = 0; i < count; i++) results.add(pool.submit(() -> api.prepare("module:shared", builder)));
        var first = results.get(0).get(20, TimeUnit.SECONDS);
        for (var result : results) assertSame(first, result.get(20, TimeUnit.SECONDS));
        assertEquals(1, builds.get());
        assertEquals(count - 1, statistic("joins"));
        assertSame(first, api.lookup("module:shared"));
        assertSame(first, api.prepare("module:shared", () -> { throw new AssertionError("Prepared again"); }));
    }

    @Test public void waitersSeeAFailureThatIsNotKept() throws Exception {
        var failure = new IllegalArgumentException("broken source");
        var waiter = new Callable<Object>() {
            @Override public Object call() throws Exception {
                return api.prepare("plan:broken", () -> { throw new AssertionError("A waiter built the failed key"); });
            }
        };
        var owner = pool.submit(() -> api.prepare("plan:broken", () -> { awaitWaiters(3); throw failure; }));
        awaitStarted();
        var waiters = new ArrayList<Future<Object>>();
        for (int i = 0; i < 3; i++) waiters.add(pool.submit(waiter));
        for (var future : waiters) assertSame(failure, cause(future));
        assertSame(failure, cause(owner));
        assertNull(api.lookup("plan:broken"));
        assertEquals(0, statistic("preparing"));
        assertEquals("retry", api.prepare("plan:broken", () -> "retry"));
        assertEquals("retry", api.lookup("plan:broken"));
    }
    private void awaitStarted() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (statistic("preparing") == 0) {
            if (System.nanoTime() > deadline) fail("No preparation started");
            Thread.sleep(2);
        }
    }
    private static Throwable cause(Future<?> future) throws Exception {
        try { future.get(20, TimeUnit.SECONDS); } catch (java.util.concurrent.ExecutionException e) { return e.getCause(); }
        fail("Expected a failure"); return null;
    }

    @Test public void anInterruptedPreparationIsPreparedAgainByAWaiter() throws Exception {
        var ownerThread = new java.util.concurrent.atomic.AtomicReference<Thread>();
        var owner = pool.submit(() -> api.prepare("module:interrupted", () -> {
            ownerThread.set(Thread.currentThread());
            new CountDownLatch(1).await(); // until interrupted
            return "never";
        }));
        awaitStarted();
        var waiter = pool.submit(() -> api.prepare("module:interrupted", () -> "taken over"));
        awaitWaiters(1);
        ownerThread.get().interrupt();
        assertTrue(cause(owner) instanceof InterruptedException);
        // the interruption of another caller is not a failure of the preparation
        assertEquals("taken over", waiter.get(20, TimeUnit.SECONDS));
        assertEquals("taken over", api.lookup("module:interrupted"));
    }

    @Test public void anInterruptedWaiterStopsWaitingAndThePreparationGoesOn() throws Exception {
        var release = new CountDownLatch(1);
        var owner = pool.submit(() -> api.prepare("plan:long", () -> { release.await(); return "done"; }));
        awaitStarted();
        var waiterThread = new java.util.concurrent.atomic.AtomicReference<Thread>();
        var interruptedFlag = new java.util.concurrent.atomic.AtomicBoolean();
        var waiter = pool.submit(() -> {
            waiterThread.set(Thread.currentThread());
            try { return api.prepare("plan:long", () -> "waiter"); }
            finally { interruptedFlag.set(Thread.currentThread().isInterrupted()); }
        });
        awaitWaiters(1);
        waiterThread.get().interrupt();
        assertTrue(cause(waiter) instanceof InterruptedException);
        assertTrue("The interruption stays visible to the caller", interruptedFlag.get());
        assertEquals(0, waiters());
        release.countDown();
        assertEquals("done", owner.get(20, TimeUnit.SECONDS));
        assertEquals("done", api.prepare("plan:long", () -> "again"));
    }

    @Test public void aPreparationWaitsForTheKeysItNeedsWithoutHoldingTheOthers() throws Exception {
        var innerStarted = new CountDownLatch(1);
        var innerRelease = new CountDownLatch(1);
        var inner = pool.submit(() -> api.prepare("module:inner", () -> { innerStarted.countDown(); innerRelease.await(); return "inner"; }));
        assertTrue(innerStarted.await(10, TimeUnit.SECONDS));
        // outer needs inner, being prepared by another thread: it waits for it, and another key is prepared meanwhile
        var outer = pool.submit(() -> api.prepare("plan:outer", () -> "outer+" + api.prepare("module:inner", () -> "duplicate")));
        awaitWaiters(1);
        assertEquals("other", pool.submit(() -> api.prepare("plan:other", () -> "other")).get(5, TimeUnit.SECONDS));
        innerRelease.countDown();
        assertEquals("inner", inner.get(20, TimeUnit.SECONDS));
        assertEquals("outer+inner", outer.get(20, TimeUnit.SECONDS));
        assertEquals("outer+inner", api.lookup("plan:outer"));
    }

    @Test public void nestedPreparationsOnOneThreadPublishEachKey() throws Exception {
        var value = api.prepare("plan:root", () -> "root(" + api.prepare("catalog:project", () -> "catalog("
                + api.prepare("module:service", () -> "service") + ")") + ")");
        assertEquals("root(catalog(service))", value);
        assertEquals("catalog(service)", api.lookup("catalog:project"));
        assertEquals("service", api.lookup("module:service"));
        assertEquals(3, statistic("builds"));
    }

    @Test public void aPreparationNeedingItsOwnKeyBuildsItNestedInsteadOfWaitingForItself() throws Exception {
        var depth = new AtomicInteger();
        var builder = new java.util.concurrent.atomic.AtomicReference<Callable<Object>>();
        builder.set(() -> depth.incrementAndGet() == 1 ? "outer+" + api.prepare("module:self", builder.get()) : "nested");
        assertEquals("outer+nested", pool.submit(() -> api.prepare("module:self", builder.get())).get(10, TimeUnit.SECONDS));
        // the outer value is the one published, as when one lock served every key
        assertEquals("outer+nested", api.lookup("module:self"));
    }

    @Test public void preparationsNeedingEachOtherOnTwoThreadsNeverWaitForEachOther() throws Exception {
        var firstStarted = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        // first: env needs plan, being prepared by second, which needs env: waiting would never end
        var first = pool.submit(() -> api.prepare("env:a", () -> {
            firstStarted.countDown();
            assertTrue(secondStarted.await(10, TimeUnit.SECONDS));
            return "a+" + api.prepare("plan:b", () -> "b-from-a");
        }));
        var second = pool.submit(() -> api.prepare("plan:b", () -> {
            secondStarted.countDown();
            assertTrue(firstStarted.await(10, TimeUnit.SECONDS));
            awaitWaiters(1); // first waits for this preparation
            return "b+" + api.prepare("env:a", () -> "a-for-b");
        }));
        assertEquals("b+a-for-b", second.get(20, TimeUnit.SECONDS));
        assertEquals("a+b+a-for-b", first.get(20, TimeUnit.SECONDS));
        assertEquals("a+b+a-for-b", api.lookup("env:a"));
        assertEquals("b+a-for-b", api.lookup("plan:b"));
        assertEquals(0, waiters());
    }

    @Test public void aNullValueIsAFailure() throws Exception {
        try { api.prepare("module:null", () -> null); fail(); } catch (IllegalStateException expected) { }
        assertNull(api.lookup("module:null"));
        assertEquals("value", api.prepare("module:null", () -> "value"));
    }

    @Test public void scriptBuildersOfASealedSharedScopePrepareOnTheirCallingThreads() throws Exception {
        Function prepareAll;
        try (var cx = Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            var scope = api.sharedScope();
            scope.put("api", scope, Context.javaToJS(api, scope));
            prepareAll = (Function) cx.evaluateString(scope,
                    "(function (name, barrier) { return String(api.prepare('plan:' + name, function () {"
                    + " barrier.await();"
                    + " return name + ':' + api.prepare('module:shared', function () { return 'shared'; }); })); })",
                    "preparation", 1, null);
            scope.sealObject();
        }
        int count = 4;
        var together = new CyclicBarrier(count);
        var results = new ArrayList<Future<Object>>();
        for (int i = 0; i < count; i++) {
            var name = "flow" + i;
            results.add(pool.submit(() -> {
                try (var cx = Context.enter(); var frame = api.enter(cx)) {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    return prepareAll.call(cx, frame.scope, frame.scope, new Object[] { name, together });
                }
            }));
        }
        for (int i = 0; i < count; i++) assertEquals("flow" + i + ":shared", String.valueOf(results.get(i).get(20, TimeUnit.SECONDS)));
        assertEquals("shared", String.valueOf(api.lookup("module:shared")));
        assertEquals(count + 1, statistic("builds"));
    }
}
