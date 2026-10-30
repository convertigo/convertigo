/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.core;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.PluginsManager;
import com.twinsoft.convertigo.engine.requesters.JsonServletRequester;

public class RequestableTimeoutTest {

	private Engine previous;
	private Context context;
	private Probe probe;

	private enum Work { SLEEP, LOOP, STREAM, CLEANUP_RACE, UNWIND, COMPLETE }

	private static class Probe extends GenericSequence {
		private final Work work;
		private volatile RequestableThread worker;
		private volatile boolean interrupted;
		private volatile boolean cancelled;
		private int cleanups;
		private final AtomicReference<Throwable> failure = new AtomicReference<>();
		private final CountDownLatch finishUnwind = new CountDownLatch(1);
		private final CountDownLatch callerClearing = new CountDownLatch(1);

		Probe(Work work) { this.work = work; }
		@Override protected boolean requiresJavascriptScope() { return false; }
		@Override public void prepareForRequestable(Context context, org.mozilla.javascript.Context js, Scriptable scope) { }
		@Override public void handleRequestableEvent(String event, org.mozilla.javascript.Context js) { }
		@Override public void fireRequestableEvent(String event) {
			if (work == Work.CLEANUP_RACE && EVENT_REQUESTABLE_FINISHED.equals(event)) {
				// Hold the real cleanup window: context cleared, runningThread still assigned.
				context = null;
				callerClearing.countDown();
				try { worker.join(2000); }
				catch (InterruptedException e) { throw new AssertionError(e); }
			}
		}
		@Override protected void cleanup() { cleanups++; }
		@Override public boolean hasToRunCore() { return true; }

		@Override public void runCore() {
			worker = runningThread;
			worker.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
			Context executionContext = context;
			long started = System.nanoTime();
			try {
				if (work == Work.COMPLETE) return;
				if (work == Work.STREAM) {
					while (System.nanoTime() - started < TimeUnit.MILLISECONDS.toNanos(1400)) {
						executionContext.rowStreamProgress = System.currentTimeMillis();
						Thread.sleep(100);
					}
				}
				if (work == Work.LOOP) {
					while (worker.bContinue && System.nanoTime() - started < TimeUnit.SECONDS.toNanos(5)) Thread.onSpinWait();
				} else {
					Thread.sleep(5000);
				}
			} catch (InterruptedException expected) {
				interrupted = true;
				if (work == Work.UNWIND) {
					try {
						assertTrue(finishUnwind.await(3, TimeUnit.SECONDS));
						assertSame(executionContext, context);
						assertNotNull(requester);
					} catch (Throwable error) { failure.set(error); }
				}
				if (work == Work.CLEANUP_RACE) {
					try { assertTrue(callerClearing.await(2, TimeUnit.SECONDS)); }
					catch (InterruptedException e) { throw new AssertionError(e); }
				}
			} finally {
				cancelled = !worker.bContinue;
			}
		}
	}

	@Before public void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logEngine = Engine.logBeans = Engine.logContext = Logger.getLogger("requestable-timeout-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.pluginsManager = new PluginsManager();
		Project project = new Project() {
			@Override public ClassLoader getProjectClassLoader() { return RequestableTimeoutTest.class.getClassLoader(); }
		};
		project.setName("TimeoutTest");
		context = new Context("timeout_test");
		context.project = project;
		context.projectName = project.getName();
		context.nativeJson = true;
	}

	@After public void restore() throws Exception {
		if (probe != null && probe.worker != null) {
			probe.finishUnwind.countDown();
			probe.worker.bContinue = false;
			probe.worker.interrupt();
			probe.worker.join(3000);
		}
		Engine.theApp = previous;
		EnginePropertiesManager.initProperties();
	}

	private void timeout(Work work, boolean legacyStop) throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.DOCUMENT_THREADING_USE_STOP_METHOD, Boolean.toString(legacyStop));
		probe = new Probe(work);
		probe.setName("Wait");
		probe.setResponseTimeout(1);
		long started = System.nanoTime();
		assertThrows(TransactionTimeoutException.class, () -> probe.run(new JsonServletRequester(), context));
		long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
		assertTrue("Request must keep its timeout", elapsed >= (work == Work.STREAM ? 2000 : 800) && elapsed < 6000);
		assertNotNull(probe.worker);
		probe.worker.join(2000);
		assertFalse("Cooperative worker must exit", probe.worker.isAlive());
		assertTrue("Cancellation must be published before interruption", probe.cancelled);
		assertEquals(1, probe.cleanups);
		assertNull("Worker finalization must remain safe after the caller returns", probe.failure.get());
		if (work != Work.LOOP) assertTrue("Blocking work must be interrupted", probe.interrupted);
	}

	@Test public void defaultTimeoutInterruptsBlockingWork() throws Exception { timeout(Work.SLEEP, false); }
	@Test public void oldStopSettingStillReturnsATimeoutOnJava25() throws Exception { timeout(Work.SLEEP, true); }
	@Test public void cancellationFlagStopsCooperativeCpuWork() throws Exception { timeout(Work.LOOP, false); }
	@Test public void streamingProgressKeepsTheIdleTimeout() throws Exception { timeout(Work.STREAM, false); }
	@Test public void callerCanClearContextBeforeWorkerFinishes() throws Exception { timeout(Work.CLEANUP_RACE, false); }
	@Test public void cancellationRetainsExecutionReferencesUntilWorkerFinishes() throws Exception {
		probe = new Probe(Work.UNWIND);
		probe.setName("Unwind");
		probe.setResponseTimeout(1);
		assertThrows(TransactionTimeoutException.class, () -> probe.run(new JsonServletRequester(), context));
		try {
			assertSame("Cancelled work still needs its context while unwinding", context, probe.context);
			assertSame(probe.worker, probe.runningThread);
		} finally {
			probe.finishUnwind.countDown();
			probe.worker.join(2000);
		}
		assertFalse(probe.worker.isAlive());
		assertNull(probe.failure.get());
		assertNull(probe.context);
		assertNull(probe.requester);
		assertNull(probe.runningThread);
		assertNull(probe.scope);
	}
	@Test public void scopeCleanupKeepsItsOwnReference() {
		probe = new Probe(Work.COMPLETE);
		var scope = new org.mozilla.javascript.NativeObject() {
			@Override public Object[] getIds() {
				probe.scope = null;
				return super.getIds();
			}
		};
		scope.put("value", scope, "temporary");
		probe.scope = scope;
		probe.removeObjectsFromScope();
		assertFalse(scope.has("value", scope));
	}
	@Test public void completedWorkIsNotInterrupted() throws Exception {
		probe = new Probe(Work.COMPLETE);
		probe.setName("Complete");
		probe.setResponseTimeout(1);
		probe.run(new JsonServletRequester(), context);
		probe.worker.join(2000);
		assertFalse(probe.worker.isAlive());
		assertFalse(probe.interrupted);
		assertFalse(probe.worker.isInterrupted());
		assertEquals(0, probe.cleanups);
		assertNull(probe.failure.get());
	}
}
