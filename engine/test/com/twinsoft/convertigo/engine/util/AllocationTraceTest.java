package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class AllocationTraceTest {
    private static volatile Object sink;
    private static void allocate() { sink = new byte[16384]; }
    private void verify(JSONObject report, int workers) throws Exception {
        assertTrue(report.getBoolean("complete"));
        assertEquals(workers, report.getInt("workers"));
        long sum = 0;
        var entries = report.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            var row = entries.getJSONObject(i);
            assertTrue(row.getLong("inclusive") >= row.getLong("exclusive"));
            assertTrue(row.getLong("inclusiveNanos") >= row.getLong("exclusiveNanos"));
            assertTrue(row.getLong("exclusiveNanos") >= 0);
            sum += row.getLong("exclusive");
        }
        assertEquals(report.getLong("allocatedBytes"), sum);
        assertTrue(sum > 16384L * (workers + 1));
    }
    @Test public void disabledDoesNotCreateSpans() {
        assertNull(AllocationTrace.beginRequest());
        assertFalse(AllocationTrace.isActive());
        assertNull(AllocationTrace.open("boundary", "ignored", ""));
    }
    @Test public void nestedSpansPartitionBytesAndCloseOnFailure() throws Exception {
        var root = AllocationTrace.startRequest();
        try (root) {
            allocate();
            try (var outer = AllocationTrace.open("node", "first", "rows.map")) {
                allocate();
                try (var inner = AllocationTrace.open("boundary", "copy", "")) { allocate(); }
                try (var failed = AllocationTrace.open("node", "failure", "throw")) {
                    allocate(); throw new IllegalArgumentException();
                } catch (IllegalArgumentException expected) { }
            }
        }
        root.close();
        verify(root.snapshot(), 0);
        var entries = root.snapshot().getJSONArray("entries");
        long elapsed = 0, rootElapsed = 0;
        for (int i = 0; i < entries.length(); i++) {
            var entry = entries.getJSONObject(i);
            elapsed += entry.getLong("exclusiveNanos");
            if (entry.getString("id").equals("servlet")) rootElapsed = entry.getLong("inclusiveNanos");
        }
        assertEquals(rootElapsed, elapsed);
        assertTrue(rootElapsed > 0);
        assertFalse(AllocationTrace.isActive());
    }
    @Test public void concurrentRequestsIncludeNestedWorkersWithoutDoubleCounting() throws Exception {
        var executor = Executors.newFixedThreadPool(5);
        var error = new AtomicReference<Throwable>();
        var ready = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) executor.submit(() -> {
            try {
                var root = AllocationTrace.startRequest();
                try (root) {
                    ready.countDown(); assertTrue(ready.await(5, TimeUnit.SECONDS)); allocate();
                    var child = new Thread(() -> {
                        try (var worker = AllocationTrace.open("boundary", "worker", "one")) {
                            allocate();
                            var nested = new Thread(() -> {
                                try (var worker2 = AllocationTrace.open("boundary", "worker", "two")) { allocate(); }
                            });
                            nested.start();
                            try { nested.join(); } catch (InterruptedException e) { throw new AssertionError(e); }
                        }
                    });
                    child.start(); child.join();
                }
                verify(root.snapshot(), 2);
                assertFalse(AllocationTrace.isActive());
            } catch (Throwable e) { error.compareAndSet(null, e); }
        });
        executor.shutdown(); assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }
    @Test public void workerFinishingAfterServletDefersCompletion() throws Exception {
        var entered = new CountDownLatch(1); var finish = new CountDownLatch(1);
        var root = AllocationTrace.startRequest();
        var child = new Thread(() -> {
            try (var worker = AllocationTrace.open("boundary", "worker", "")) {
                entered.countDown();
                try { finish.await(); } catch (InterruptedException e) { throw new AssertionError(e); }
                allocate();
            }
        });
        child.start(); entered.await(); allocate(); root.close();
        assertFalse(root.snapshot().getBoolean("complete"));
        finish.countDown(); child.join(); verify(root.snapshot(), 1);
    }
    @Test public void nestedRequestRestoresOuterScope() throws Exception {
        var outer = AllocationTrace.startRequest();
        try (outer) {
            allocate();
            var inner = AllocationTrace.startRequest();
            try (inner) { allocate(); }
            verify(inner.snapshot(), 0);
            assertTrue(AllocationTrace.isActive());
        }
        assertFalse(AllocationTrace.isActive());
    }

    @Test public void optionChangesAndInitializationRefreshTheGuard() throws Exception {
        var option = com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.TIMING_TRACE_ALLOCATIONS;
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        try {
            assertNull(AllocationTrace.beginRequest());
            com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(option, "true");
            try (var root = AllocationTrace.beginRequest()) { assertNotNull(root); allocate(); }
            com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(option, "false");
            assertNull(AllocationTrace.beginRequest());
            com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(option, "true");
            var clear = com.twinsoft.convertigo.engine.EnginePropertiesManager.class.getDeclaredMethod("clearProperty", option.getClass());
            clear.setAccessible(true); clear.invoke(null, option);
            assertNull(AllocationTrace.beginRequest());
            com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(option, "true");
            com.twinsoft.convertigo.engine.EnginePropertiesManager.unload();
            assertNull(AllocationTrace.beginRequest());
            com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
            assertNull(AllocationTrace.beginRequest());
        } finally { com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(option, "false"); }
    }
    @Test public void disabledGuardsAllocateNoBytesAfterInitialization() throws Exception {
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        var mx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        mx.setThreadAllocatedMemoryEnabled(true);
        long tid = Thread.currentThread().getId();
        for (int i = 0; i < 5; i++) assertEquals(5000, exerciseDisabledGuards());
        long before = mx.getThreadAllocatedBytes(tid);
        int disabled = exerciseDisabledGuards();
        long allocated = mx.getThreadAllocatedBytes(tid) - before;
        assertEquals(5000, disabled);
        assertEquals(0, allocated);
    }
    private static int exerciseDisabledGuards() {
        int disabled = 0;
        for (int i = 0; i < 5000; i++) {
            if (AllocationTrace.beginRequest() == null && AllocationTrace.open("node", "test", "test") == null
                    && !AllocationTrace.isActive()) disabled++;
        }
        return disabled;
    }
}
