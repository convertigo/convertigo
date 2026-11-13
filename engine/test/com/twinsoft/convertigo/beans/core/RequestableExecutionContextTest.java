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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.PluginsManager;
import com.twinsoft.convertigo.engine.RequestableEngineEvent;
import com.twinsoft.convertigo.engine.requesters.JsonServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;

/**
 * Repeated internal calls of the same transaction, as a sequence makes them: each execution fires its start and
 * finish events with its own context, whatever the moment the worker of the previous execution ends.
 */
public class RequestableExecutionContextTest {

	private static final String WORKER_NOTIFIED = "(RequestableObject) The calling thread has been notified!";

	private Engine previous;
	private Logger previousEngineLog, previousBeansLog, previousContextLog;
	private WorkerHoldingLogger log;
	private Project project;
	private Context context;
	private final List<String> delivered = Collections.synchronizedList(new ArrayList<>());

	/** Holds the next worker right after it has woken its caller, before its thread ends. */
	private static final class WorkerHoldingLogger extends Logger {
		private volatile boolean armed;
		private volatile boolean released;
		private volatile Thread held;

		WorkerHoldingLogger() { super("requestable-execution-context-test"); }

		void holdNextWorker() { released = false; held = null; armed = true; }

		@Override public void debug(Object message) {
			if (armed && WORKER_NOTIFIED.equals(message) && Thread.currentThread() instanceof RequestableObject.RequestableThread) {
				armed = false;
				Thread worker = held = Thread.currentThread();
				// The worker holds its own monitor here: waiting on it lets the woken caller return.
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
				synchronized (worker) {
					while (!released) {
						long remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
						if (remaining <= 0) break;
						try { worker.wait(remaining); }
						catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
					}
				}
				return;
			}
			super.debug(message);
		}

		Thread release() {
			Thread worker = held;
			if (worker != null) {
				synchronized (worker) {
					released = true;
					worker.notifyAll();
				}
			}
			return worker;
		}
	}

	/** A transaction whose start and finish events are the real ones: they read the execution context. */
	private static class Probe extends Transaction {
		private static final long serialVersionUID = 1L;
		final List<String> events = Collections.synchronizedList(new ArrayList<>());
		final List<Context> workerContexts = Collections.synchronizedList(new ArrayList<>());
		volatile RequestableThread worker;
		volatile boolean blockNext;
		volatile boolean interrupted;
		private final CountDownLatch unblock = new CountDownLatch(1);

		Probe(String name) throws Exception {
			setName(name);
			setResponseTimeout(30);
		}

		@Override public void prepareForRequestable(Context context, org.mozilla.javascript.Context js, Scriptable scope) { }
		@Override public void handleRequestableEvent(String event, org.mozilla.javascript.Context js) { }
		@Override public boolean hasToRunCore() { return true; }
		@Override public void setStatisticsOfRequestFromCache() { }
		@Override public String getRequestString(Context context) { return getName(); }

		@Override public void fireRequestableEvent(String event) {
			Context current = context;
			events.add(event + "@" + (current == null ? null : current.contextID));
			super.fireRequestableEvent(event);
		}

		@Override public void runCore() {
			worker = runningThread;
			workerContexts.add(context);
			if (!blockNext) return;
			blockNext = false;
			// A cancelled worker ending later than its caller: it ignores the interruption until released.
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (System.nanoTime() < deadline) {
				try {
					if (unblock.await(10, TimeUnit.SECONDS)) return;
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		}

		void unblock() { unblock.countDown(); }

		Document call(Requester requester, Context context) throws Exception {
			// As CacheManager.getDocument: the input is parsed, then the transaction is run.
			parseInputDocument(context);
			return run(requester, context);
		}
	}

	@Before public void initialize() throws Exception {
		previous = Engine.theApp;
		previousEngineLog = Engine.logEngine;
		previousBeansLog = Engine.logBeans;
		previousContextLog = Engine.logContext;
		log = new WorkerHoldingLogger();
		Engine.logEngine = Engine.logBeans = Engine.logContext = log;
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.pluginsManager = new PluginsManager();
		Engine.theApp.addEngineListener(new EngineListener() {
			@Override public void blocksChanged(EngineEvent event) { }
			@Override public void objectDetected(EngineEvent event) { }
			@Override public void documentGenerated(EngineEvent event) { }
			@Override public void stepReached(EngineEvent event) { }
			@Override public void transactionStarted(EngineEvent event) {
				delivered.add("started:" + ((RequestableEngineEvent) event).getProjectName());
			}
			@Override public void transactionFinished(EngineEvent event) {
				delivered.add("finished:" + ((RequestableEngineEvent) event).getProjectName());
			}
			@Override public void sequenceStarted(EngineEvent event) { }
			@Override public void sequenceFinished(EngineEvent event) { }
			@Override public void clearEditor(EngineEvent event) { }
		});
		project = new Project() {
			private static final long serialVersionUID = 1L;
			@Override public ClassLoader getProjectClassLoader() { return RequestableExecutionContextTest.class.getClassLoader(); }
		};
		project.setName("ExecutionContextTest");
		context = newContext("execution_context_test");
	}

	private Context newContext(String id) {
		Context newContext = new Context(id);
		newContext.project = project;
		newContext.projectName = project.getName();
		newContext.connectorName = "Connector";
		newContext.transactionName = "Call";
		return newContext;
	}

	@After public void restore() throws Exception {
		Thread worker = log.release();
		if (worker != null) worker.join(5000);
		Engine.theApp = previous;
		Engine.logEngine = previousEngineLog;
		Engine.logBeans = previousBeansLog;
		Engine.logContext = previousContextLog;
		EnginePropertiesManager.initProperties();
	}

	/** The requester and the context of internal calls of the probe, as the engine prepares them. */
	private static Requester requester(Probe probe, Context context) {
		var requester = new JsonServletRequester();
		requester.context = context;
		context.requestedObject = probe;
		return requester;
	}

	private static List<String> events(Context context, int calls) {
		var expected = new ArrayList<String>();
		for (int i = 0; i < calls; i++) {
			expected.add(RequestableObject.EVENT_REQUESTABLE_STARTED + "@" + context.contextID);
			expected.add(RequestableObject.EVENT_REQUESTABLE_FINISHED + "@" + context.contextID);
		}
		return expected;
	}

	private static Document start(Probe probe, Requester requester, Context context) throws Exception {
		try {
			return probe.run(requester, context);
		} catch (NullPointerException e) {
			throw new AssertionError("The start event of the repeated call must have its context", e);
		}
	}

	@Test public void repeatedCallStartsWithItsContextWhileThePreviousWorkerEnds() throws Exception {
		var probe = new Probe("Call");
		var requester = requester(probe, context);
		log.holdNextWorker();
		probe.call(requester, context);
		Thread first = probe.worker;
		assertSame("The worker of the first call is held after waking its caller", first, log.held);
		assertTrue(first.isAlive());

		// The repeated call binds its context, then the previous worker ends.
		probe.parseInputDocument(context);
		log.release();
		first.join(5000);
		assertFalse(first.isAlive());

		Document response = start(probe, requester, context);
		assertNotNull(response);
		assertEquals(events(context, 2), probe.events);
		assertEquals(List.of(context, context), probe.workerContexts);
		assertNull(probe.context);
		assertNull(probe.runningThread);
	}

	@Test public void normalCompletionReleasesTheReferencesBeforeReturning() throws Exception {
		var probe = new Probe("Call");
		log.holdNextWorker();
		probe.call(requester(probe, context), context);
		Thread worker = probe.worker;
		try {
			assertTrue("The worker has woken its caller and is still ending", worker.isAlive());
			assertNull("No reference is left for the ending worker to release", probe.context);
			assertNull(probe.requester);
			assertNull(probe.runningThread);
			assertNull(probe.scope);
		} finally {
			log.release();
			worker.join(5000);
		}
		assertFalse(worker.isAlive());
	}

	@Test public void cancelledWorkerEndingAfterTheNextCallBindsKeepsItsContext() throws Exception {
		var probe = new Probe("Call");
		var requester = requester(probe, context);
		probe.setResponseTimeout(1);
		probe.blockNext = true;
		probe.parseInputDocument(context);
		assertThrows(TransactionTimeoutException.class, () -> probe.run(requester, context));
		Thread cancelled = probe.worker;
		assertTrue("The cancelled worker is still unwinding", cancelled.isAlive());

		probe.parseInputDocument(context);
		probe.unblock();
		cancelled.join(5000);
		assertFalse(cancelled.isAlive());

		probe.setResponseTimeout(30);
		Document response = start(probe, requester, context);
		assertNotNull(response);
		assertEquals(events(context, 2), probe.events);
		assertNull(probe.context);
		assertNull(probe.runningThread);
	}

	@Test public void repeatedCallsNotifyOnceWithTheirContext() throws Exception {
		var probe = new Probe("Call");
		var requester = requester(probe, context);
		int calls = 500;
		for (int i = 0; i < calls; i++) {
			probe.parseInputDocument(context);
			Document response = start(probe, requester, context);
			assertEquals("document", response.getDocumentElement().getTagName());
			assertEquals("Call", response.getDocumentElement().getAttribute("transaction"));
			assertFalse(response.getDocumentElement().getAttribute("generated").isEmpty());
		}
		assertEquals(events(context, calls), probe.events);
		assertEquals(calls, probe.workerContexts.size());
		assertEquals(calls * 2, delivered.size());
		for (int i = 0; i < calls; i++) {
			assertEquals("started:" + project.getName(), delivered.get(2 * i));
			assertEquals("finished:" + project.getName(), delivered.get(2 * i + 1));
		}
	}

	@Test public void concurrentExecutionsKeepTheirOwnContext() throws Exception {
		int threads = 4, calls = 100;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			var failures = new AtomicInteger();
			var results = new ArrayList<Future<?>>();
			for (int t = 0; t < threads; t++) {
				var own = newContext("concurrent_" + t);
				var probe = new Probe("Call");
				results.add(pool.submit(() -> {
					var requester = requester(probe, own);
					for (int i = 0; i < calls; i++) {
						probe.call(requester, own);
						if (probe.context != null) failures.incrementAndGet();
					}
					assertEquals(events(own, calls), probe.events);
					assertEquals(Collections.nCopies(calls, own), probe.workerContexts);
					return null;
				}));
			}
			for (var result : results) result.get(60, TimeUnit.SECONDS);
			assertEquals(0, failures.get());
		} finally {
			pool.shutdownNow();
		}
	}
}
