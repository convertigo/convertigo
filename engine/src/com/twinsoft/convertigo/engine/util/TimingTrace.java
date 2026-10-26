/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.MDC;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * Diagnostic (timing.trace, off by default): the start and the duration of each request, requestable, HTTP call and
 * CouchDB transaction, logged on cems.Engine at WARN to break down the time of a request.
 */
public final class TimingTrace {
	private static final InheritableThreadLocal<Span> current = new InheritableThreadLocal<>();
	private static final AtomicLong ids = new AtomicLong();
	private static final com.sun.management.ThreadMXBean allocations = allocationBean();


	private static com.sun.management.ThreadMXBean allocationBean() {
		var bean = ManagementFactory.getThreadMXBean();
		return bean instanceof com.sun.management.ThreadMXBean supported
				&& supported.isThreadAllocatedMemorySupported() ? supported : null;
	}


	private TimingTrace() {
	}

	public static boolean isEnabled() {
		try {
			return EnginePropertiesManager.getPropertyAsBoolean(PropertyName.TIMING_TRACE);
		} catch (Throwable t) {
			return false;
		}
	}

	/** the start of a timed section, 0 when the trace is off */
	public static long start() {
		return isEnabled() ? System.currentTimeMillis() : 0;
	}

	public static void end(long start, String kind, String name) {
		end(start, kind, name, 0);
	}

	/**
	 * Logs the section when it lasted at least minMs. The line holds only a kind, a name and durations, so it is
	 * written even while the logs are off (MDC nolog, set around the calls that answer a token): without that, the
	 * time of those calls is missing from the trace.
	 */
	public static void end(long start, String kind, String name, long minMs) {
		if (start == 0) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - start < minMs) {
			return;
		}
		Object nolog = MDC.get("nolog");
		if (nolog != null) {
			MDC.remove("nolog");
		}
		try {
			Engine.logEngine.warn("(Timing) kind=" + kind + " start=" + start + " ms=" + (now - start) + " thread="
					+ Thread.currentThread().getName().replace(' ', '_') + " name=" + name);
		} finally {
			if (nolog != null) {
				MDC.put("nolog", nolog);
			}
		}
	}
	public static Span open(String kind, String name) {
		return EnginePropertiesManager.isAllocationTraceEnabled() ? new Span(kind, name) : null;
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
		private final XmlUseCounter.Counts xmlCounts = XmlUseCounter.currentCounts();
		private final int documentsStart = xmlCounts == null ? 0 : xmlCounts.documents.get();
		private final String kind, name;
		private boolean closed;
		private long closedDocuments;
		private long allocatedBytes = -1;

		private Span(String kind, String name) {
			this.kind = kind;
			this.name = name;
			current.set(this);
		}

		public long allocatedBytes() { return allocatedBytes; }
		/** -1 means the XML diagnostic was not active when this span opened. */
		public long documents() { return closed ? closedDocuments : xmlCounts == null ? -1 : xmlCounts.documents.get() - documentsStart; }

		@Override
		public void close() {
			if (closed) return;
			closedDocuments = documents();
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
						+ " allocatedBytes=" + allocatedBytes + " xmlDocuments=" + documents()
						+ " thread=" + threadId);
			} finally {
				if (nolog != null) MDC.put("nolog", nolog);
			}
		}
	}
}
