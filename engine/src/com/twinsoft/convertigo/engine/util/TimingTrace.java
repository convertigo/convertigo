/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.MDC;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** Opt-in numeric diagnostics. Callers supply a static kind and a requestable QName, never a URL or data. */
public final class TimingTrace {

	private static final InheritableThreadLocal<Span> current = new InheritableThreadLocal<>();
	private static final AtomicLong ids = new AtomicLong();
	private static final com.sun.management.ThreadMXBean allocations = allocationBean();

	private TimingTrace() { }

	private static com.sun.management.ThreadMXBean allocationBean() {
		var bean = ManagementFactory.getThreadMXBean();
		return bean instanceof com.sun.management.ThreadMXBean supported
				&& supported.isThreadAllocatedMemorySupported() ? supported : null;
	}

	public static boolean isEnabled() {
		try {
			return EnginePropertiesManager.getPropertyAsBoolean(PropertyName.TIMING_TRACE);
		} catch (IllegalStateException e) {
			return false;
		}
	}

	public static Span open(String kind, String name) {
		return isEnabled() ? new Span(kind, name) : null;
	}

	/** Counts actual DOM construction/parsing, including children running on requestable threads. */
	public static void documentCreated() {
		for (var span = current.get(); span != null; span = span.parent) {
			span.documents.incrementAndGet();
		}
	}

	private static long allocated() {
		try {
			if (allocations == null) return -1;
			if (!allocations.isThreadAllocatedMemoryEnabled()) allocations.setThreadAllocatedMemoryEnabled(true);
			return allocations.getThreadAllocatedBytes(Thread.currentThread().getId());
		} catch (RuntimeException e) {
			return -1;
		}
	}

	public static final class Span implements AutoCloseable {
		private final Span parent = current.get();
		private final long id = ids.incrementAndGet();
		private final long threadId = Thread.currentThread().getId();
		private final long start = System.nanoTime();
		private final long startEpochMs = System.currentTimeMillis();
		private final long allocationStart = allocated();
		private final AtomicLong otherThreadBytes = new AtomicLong();
		private final AtomicLong documents = new AtomicLong();
		private final String kind, name;
		private boolean closed;
		private long allocatedBytes = -1;

		private Span(String kind, String name) {
			this.kind = kind;
			this.name = name;
			current.set(this);
		}

		public long allocatedBytes() { return allocatedBytes; }
		public long documents() { return documents.get(); }

		@Override
		public void close() {
			if (closed) return;
			closed = true;
			long end = allocated();
			allocatedBytes = allocationStart < 0 || end < 0 ? -1 : end - allocationStart + otherThreadBytes.get();
			// A same-thread parent's counter already includes this span's own bytes.
			// Only child-thread work is forwarded; do not sum inclusive parent/child records.
			if (parent != null) {
				parent.otherThreadBytes.addAndGet(threadId == parent.threadId
						? otherThreadBytes.get() : Math.max(0, allocatedBytes));
			}
			current.set(parent);
			var nolog = MDC.get("nolog");
			try {
				MDC.remove("nolog");
				if (Engine.logEngine != null) Engine.logEngine.warn("(Timing) id=" + id
						+ " parent=" + (parent == null ? 0 : parent.id) + " kind=" + kind
						+ " name=" + name + " start=" + startEpochMs
						+ " ms=" + ((System.nanoTime() - start) / 1_000_000d)
						+ " allocatedBytes=" + allocatedBytes + " xmlDocuments=" + documents.get()
						+ " thread=" + threadId);
			} finally {
				if (nolog != null) MDC.put("nolog", nolog);
			}
		}
	}
}
