/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.flow;

/** Limits requestable inspection to declared configuration on this thread. */
public final class FlowGenericView {
	private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

	private FlowGenericView() { }

	public static Scope open() {
		DEPTH.set(DEPTH.get() + 1);
		return new Scope();
	}

	public static boolean active() {
		return DEPTH.get() > 0;
	}

	public static final class Scope implements AutoCloseable {
		private final Thread owner = Thread.currentThread();
		private boolean closed;

		@Override
		public void close() {
			if (Thread.currentThread() != owner) throw new IllegalStateException("Flow view closed by another thread");
			if (closed) return;
			int depth = DEPTH.get() - 1;
			if (depth < 0) throw new IllegalStateException("Flow view scope underflow");
			closed = true;
			if (depth == 0) DEPTH.remove();
			else DEPTH.set(depth);
		}
	}
}
