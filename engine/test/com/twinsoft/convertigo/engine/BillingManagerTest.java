/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;

/**
 * The billing tickets of concurrent requests: each request has its own ticket, with its own values, written once by
 * every ticket manager, and the threads of the billing end with it.
 */
public class BillingManagerTest {
	private BillingManager billing;

	@BeforeClass
	public static void setUp() throws Exception {
		BillingBench.setUpEngine();
	}

	@After
	public void tearDown() throws Exception {
		if (billing != null) {
			billing.destroy();
			billing = null;
		}
	}

	/** Keeps the tickets it is given. */
	static class Recorder implements ITicketManager {
		final ConcurrentLinkedQueue<Ticket> tickets = new ConcurrentLinkedQueue<>();

		@Override public void addTicket(Ticket ticket) throws com.twinsoft.convertigo.engine.billing.BillingException {
			tickets.add(ticket);
		}

		@Override public void removeTicket(Ticket ticket) {
		}

		@Override public Ticket newTicket() {
			return new Ticket();
		}

		@Override public void destroy() {
		}
	}

	static void waitFor(Recorder recorder, int count) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (recorder.tickets.size() < count && System.nanoTime() < deadline) {
			Thread.sleep(5);
		}
	}

	static long billingThreads() {
		return Thread.getAllStackTraces().keySet().stream()
				.filter(thread -> thread.isAlive() && thread.getName().startsWith("BillingManager"))
				.count();
	}

	static void waitForNoBillingThread() throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (billingThreads() > 0 && System.nanoTime() < deadline) {
			Thread.sleep(20);
		}
	}

	@Test
	public void concurrentRequestsHaveEachTheirOwnTicket() throws Exception {
		var first = new Recorder();
		var second = new Recorder();
		billing = BillingBench.start(List.of(first, second));
		int threads = 20;
		int perThread = 2000;
		var barrier = new CyclicBarrier(threads);
		var requestable = BillingBench.sequence("Concurrent");
		var workers = new ArrayList<Thread>();
		var failures = new ConcurrentLinkedQueue<Throwable>();
		for (int i = 0; i < threads; i++) {
			var context = BillingBench.context(i, requestable);
			var worker = new Thread(() -> {
				try {
					barrier.await();
					for (int n = 0; n < perThread; n++) {
						billing.insertBilling(context);
					}
				} catch (Throwable t) {
					failures.add(t);
				}
			});
			workers.add(worker);
			worker.start();
		}
		for (var worker : workers) {
			worker.join();
		}
		assertTrue(failures.toString(), failures.isEmpty());
		for (var recorder : List.of(first, second)) {
			waitFor(recorder, threads * perThread);
			assertEquals(threads * perThread, recorder.tickets.size());
			Map<String, Integer> perSession = new HashMap<>();
			for (var ticket : recorder.tickets) {
				var index = ticket.getSessionID().substring("BENCHSESSION".length());
				assertEquals("user" + index, ticket.getUserName());
				assertEquals("ui-" + index, ticket.getUiID());
				assertEquals("web-" + index, ticket.getDeviceUUID());
				assertEquals("Concurrent", ticket.getRequestableName());
				perSession.merge(ticket.getSessionID(), 1, Integer::sum);
			}
			assertEquals(threads, perSession.size());
			for (var count : perSession.values()) {
				assertEquals(perThread, count.intValue());
			}
		}
		Thread.sleep(200);
		assertEquals("written once", threads * perThread, first.tickets.size());
	}

	@Test
	public void theBillingThreadsEndWithTheBilling() throws Exception {
		waitForNoBillingThread();
		for (int i = 0; i < 3; i++) {
			var recorder = new Recorder();
			billing = BillingBench.start(List.of(recorder));
			billing.insertBilling(BillingBench.context(i, BillingBench.sequence("Cycle")));
			waitFor(recorder, 1);
			assertEquals(1, recorder.tickets.size());
			billing.destroy();
			billing = null;
		}
		waitForNoBillingThread();
		assertEquals("no billing thread left", 0, billingThreads());
	}

	@Test
	public void nothingIsBilledWithoutTicketManager() throws Exception {
		billing = BillingBench.start(List.of());
		assertFalse(billing.hasActiveManagers());
		billing.insertBilling(BillingBench.context(0, BillingBench.sequence("None")));
		billing.insertBillingSession("start", 0, "id", "ip", "agent", "user", "device", 1);
	}
}
