/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.engine.debug;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.rules.ExternalResource;

/**
 * Replaces the clock of the inspection quota for one test class. Frozen by default, so that a loaded build machine never
 * cuts a page in a test that is not about the quota; a test about the quota advances it explicitly.
 */
public class FrozenInspectionClock extends ExternalResource {
	private static final AtomicLong NOW = new AtomicLong();

	/** Advances the quota clock, as a slow unit of work would, without waiting. Callable from test scripts. */
	public static void advance(long millis) {
		NOW.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis));
	}

	/** Every reading of the quota clock advances it, as on a machine too loaded to finish any unit within the quota. */
	public static void advanceOnEachReading(long millis) {
		JavascriptDebugSession.inspectionClock = () -> NOW.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis));
	}

	@Override
	protected void before() {
		NOW.set(0);
		JavascriptDebugSession.inspectionClock = NOW::get;
	}

	@Override
	protected void after() {
		JavascriptDebugSession.inspectionClock = System::nanoTime;
	}
}
