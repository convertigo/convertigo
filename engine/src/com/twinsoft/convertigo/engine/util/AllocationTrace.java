/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.MDC;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/** Optional allocation attribution, independent of timing profiles and their execution path.
 * Only static boundary names and source node identifiers belong here, never values or URLs.
 * Exclusive bytes partition each participating thread; inclusive bytes must not be summed.
 */
public final class AllocationTrace {
    private static final InheritableThreadLocal<Trace> request = new InheritableThreadLocal<>();
    private static final ThreadLocal<Span> stack = new ThreadLocal<>();
    private static final AtomicLong ids = new AtomicLong();
    private static final java.util.concurrent.atomic.AtomicInteger active = new java.util.concurrent.atomic.AtomicInteger();
    private static final com.sun.management.ThreadMXBean bean = bean();
    private AllocationTrace() { }
    private static com.sun.management.ThreadMXBean bean() {
        var value = ManagementFactory.getThreadMXBean();
        return value instanceof com.sun.management.ThreadMXBean b && b.isThreadAllocatedMemorySupported() ? b : null;
    }
    private static long bytes() { return bean.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    public static boolean isActive() { if (active.get() == 0) return false; var trace = request.get(); return trace != null && !trace.finished; }
    public static Span beginRequest() {
        try {
            if (!EnginePropertiesManager.isAllocationTraceEnabled() || bean == null) return null;
            if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        } catch (RuntimeException e) { return null; }
        return startRequest();
    }
    static Span startRequest() {
        var previous = request.get();
        var trace = new Trace(previous);
        active.incrementAndGet();
        request.set(trace);
        return new Span(trace, "boundary", "servlet", "", true);
    }
    public static Span open(String kind, String id, String type) {
        if (active.get() == 0) return null;
        var trace = request.get();
        return trace == null || trace.finished ? null : new Span(trace, kind, id, type, false);
    }
    private static String label(String value, int limit) {
        return value == null ? "" : value.length() <= limit ? value : value.substring(0, limit);
    }
    private record Key(String kind, String id, String type) { }
    private static final class Counter { long count, inclusive, exclusive, inclusiveNanos, exclusiveNanos; }
    private static final class Trace {
        final long id = ids.incrementAndGet();
        final Trace previous;
        final LinkedHashMap<Key, Counter> counters = new LinkedHashMap<>();
        int threads, workers;
        long total;
        boolean rootClosed;
        volatile boolean finished;
        Trace(Trace previous) { this.previous = previous; }
        synchronized void enterThread(boolean root) { threads++; if (!root) workers++; }
        synchronized void record(Key key, long inclusive, long exclusive, long inclusiveNanos, long exclusiveNanos) {
            if (counters.size() >= 4096 && !counters.containsKey(key)) key = new Key("overflow", "", "");
            var c = counters.computeIfAbsent(key, ignored -> new Counter());
            c.count++; c.inclusive += inclusive; c.exclusive += exclusive;
            c.inclusiveNanos += inclusiveNanos; c.exclusiveNanos += exclusiveNanos;
        }
        synchronized void leaveThread(long allocated, boolean root) {
            total += allocated;
            threads--;
            rootClosed |= root;
            if (rootClosed && threads == 0) {
                finished = true;
                active.decrementAndGet();
                var nolog = MDC.get("nolog");
                try {
                    MDC.remove("nolog");
                    if (Engine.logEngine != null) {
                        // Engine logs cap individual messages; keep every numeric record decodable.
                        var report = snapshot();
                        var entries = report.getJSONArray("entries");
                        int parts = (entries.length() + 7) / 8;
                        for (int part = 0; part < parts; part++) {
                            var batch = new JSONArray();
                            for (int i = part * 8; i < Math.min(entries.length(), (part + 1) * 8); i++) batch.put(entries.get(i));
                            Engine.logEngine.warn("(Allocations) " + new JSONObject().put("id", id)
                                    .put("workers", workers).put("allocatedBytes", total).put("complete", true)
                                    .put("part", part).put("parts", parts).put("entries", batch));
                        }
                    }
                } catch (org.codehaus.jettison.json.JSONException e) {
                    throw new IllegalStateException("Allocation counters cannot be encoded", e);
                } finally { if (nolog != null) MDC.put("nolog", nolog); }
            }
        }
        synchronized JSONObject snapshot() {
            try {
                var rows = new JSONArray();
                for (var entry : counters.entrySet()) {
                    var k = entry.getKey(); var c = entry.getValue();
                    rows.put(new JSONObject().put("kind", k.kind).put("id", k.id).put("type", k.type)
                            .put("count", c.count).put("inclusive", c.inclusive).put("exclusive", c.exclusive)
                            .put("inclusiveNanos", c.inclusiveNanos).put("exclusiveNanos", c.exclusiveNanos));
                }
                return new JSONObject().put("id", id).put("workers", workers).put("allocatedBytes", total)
                        .put("complete", finished).put("entries", rows);
            } catch (Exception e) { throw new IllegalStateException("Allocation counters cannot be encoded", e); }
        }
    }
    public static final class Span implements AutoCloseable {
        private final Trace trace;
        private final Span previous = stack.get();
        private final Span parent;
        private final Key key;
        private final boolean root;
        private final long owner = Thread.currentThread().getId();
        private final long start;
        private final long startNanos;
        private long children, childrenNanos;
        private boolean closed;
        private Span(Trace trace, String kind, String id, String type, boolean root) {
            this.trace = trace; this.root = root;
            parent = previous != null && previous.trace == trace ? previous : null;
            key = new Key(label(kind, 32), label(id, 128), label(type, 96));
            if (parent == null) trace.enterThread(root);
            stack.set(this);
            start = bytes();
            startNanos = System.nanoTime();
        }
        public JSONObject snapshot() { return trace.snapshot(); }
        @Override public void close() {
            if (closed) return;
            if (owner != Thread.currentThread().getId() || stack.get() != this)
                throw new IllegalStateException("Allocation spans must close in order on their owning thread");
            long inclusiveNanos = Math.max(0, System.nanoTime() - startNanos);
            long inclusive = Math.max(0, bytes() - start);
            closed = true;
            trace.record(key, inclusive, Math.max(0, inclusive - children),
                    inclusiveNanos, Math.max(0, inclusiveNanos - childrenNanos));
            stack.set(previous);
            if (parent != null) { parent.children += inclusive; parent.childrenNanos += inclusiveNanos; }
            else trace.leaveThread(inclusive, root);
            if (root) {
                if (trace.previous == null) request.remove(); else request.set(trace.previous);
            } else if (parent == null) request.remove();
            if (previous == null) stack.remove();
        }
    }
}
