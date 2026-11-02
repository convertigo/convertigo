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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.BillingException;
import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.billing.TicketQueue;

/**
 * The billing tickets of concurrent requests: each request has its own ticket, with its own values, written once by
 * every ticket manager, and the threads of the billing end with it. Each ticket manager has its own bounded queue: a
 * slow one does not delay the others, a ticket beyond the capacity is dropped and counted, and the queued tickets are
 * written when the billing stops.
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
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_QUEUE_CAPACITY,
				PropertyName.ANALYTICS_QUEUE_CAPACITY.getDefaultValue());
	}

	/** Keeps the tickets it is given. */
	static class Recorder implements ITicketManager {
		final ConcurrentLinkedQueue<Ticket> tickets = new ConcurrentLinkedQueue<>();

		@Override public void addTicket(Ticket ticket) throws BillingException {
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

	/** Waits for its gate to open before each ticket. */
	static class Gated extends Recorder {
		final CountDownLatch gate = new CountDownLatch(1);

		@Override public void addTicket(Ticket ticket) throws BillingException {
			try {
				gate.await();
			} catch (InterruptedException e) {
				throw new BillingException("interrupted");
			}
			super.addTicket(ticket);
		}
	}

	@Test
	public void theQueuedTicketsAreWrittenWhenTheBillingStops() throws Exception {
		var slow = new Recorder() {
			@Override public void addTicket(Ticket ticket) throws BillingException {
				java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(50));
				super.addTicket(ticket);
			}
		};
		billing = BillingBench.start(List.of(slow));
		var context = BillingBench.context(0, BillingBench.sequence("Stop"));
		int tickets = 20000;
		for (int n = 0; n < tickets; n++) {
			billing.insertBilling(context);
		}
		assertTrue("still queued", slow.tickets.size() < tickets);
		billing.destroy();
		billing = null;
		assertEquals("all written before the stop returns", tickets, slow.tickets.size());
	}

	@Test
	public void aSlowTicketManagerDoesNotDelayTheOthers() throws Exception {
		var slow = new Gated();
		var fast = new Recorder();
		billing = BillingBench.start(List.of(slow, fast));
		var context = BillingBench.context(0, BillingBench.sequence("Slow"));
		for (int n = 0; n < 1000; n++) {
			billing.insertBilling(context);
		}
		waitFor(fast, 1000);
		assertEquals(1000, fast.tickets.size());
		assertEquals(0, slow.tickets.size());
		slow.gate.countDown();
		waitFor(slow, 1000);
		assertEquals(1000, slow.tickets.size());
	}

	@Test
	public void beyondItsCapacityATicketIsDroppedAndCounted() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_QUEUE_CAPACITY, "100");
		var slow = new Gated();
		billing = BillingBench.start(List.of(slow));
		var queue = queues(billing).get(0);
		var context = BillingBench.context(0, BillingBench.sequence("Full"));
		long dropped;
		try {
			for (int n = 0; n < 1000; n++) {
				billing.insertBilling(context);
			}
			assertTrue("bounded: " + queue.size(), queue.size() <= 100);
			dropped = queue.dropped();
			assertTrue("dropped " + dropped, dropped >= 1000 - 100 - TicketQueue.BATCH);
		} finally {
			slow.gate.countDown();
		}
		waitFor(slow, (int) (1000 - dropped));
		Thread.sleep(200);
		assertEquals("written or counted as dropped", 1000, slow.tickets.size() + dropped);
		assertEquals(dropped, queue.dropped());
	}

	@SuppressWarnings("unchecked")
	static List<TicketQueue> queues(BillingManager billing) throws Exception {
		var field = BillingManager.class.getDeclaredField("queues");
		field.setAccessible(true);
		return (List<TicketQueue>) field.get(billing);
	}

	@Test
	public void theTicketsAreHandedTogether() throws Exception {
		var sizes = new ConcurrentLinkedQueue<Integer>();
		var batched = new Recorder() {
			@Override public boolean addTickets(List<Ticket> tickets) {
				sizes.add(tickets.size());
				this.tickets.addAll(tickets);
				return true;
			}
		};
		billing = BillingBench.start(List.of(batched));
		var context = BillingBench.context(0, BillingBench.sequence("Batch"));
		for (int n = 0; n < 5000; n++) {
			billing.insertBilling(context);
		}
		waitFor(batched, 5000);
		assertEquals(5000, batched.tickets.size());
		assertTrue("together: " + sizes, sizes.stream().anyMatch(size -> size > 1));
		assertTrue("at most a batch: " + sizes, sizes.stream().allMatch(size -> size <= TicketQueue.BATCH));
	}

	@Test
	public void ticketsNotWrittenTogetherAreWrittenOneByOne() throws Exception {
		var attempts = new AtomicInteger();
		var failing = new Recorder() {
			@Override public boolean addTickets(List<Ticket> tickets) throws BillingException {
				attempts.incrementAndGet();
				throw new BillingException("not together");
			}
		};
		billing = BillingBench.start(List.of(failing));
		var context = BillingBench.context(0, BillingBench.sequence("OneByOne"));
		for (int n = 0; n < 3000; n++) {
			billing.insertBilling(context);
		}
		waitFor(failing, 3000);
		Thread.sleep(300);
		assertEquals("each once", 3000, failing.tickets.size());
		assertEquals(3000, new java.util.HashSet<>(failing.tickets).size());
		assertTrue(attempts.get() > 0);
	}

	@Test
	public void noTicketIsLostWhenTheTicketManagersAreReplaced() throws Exception {
		var first = new Recorder();
		billing = BillingBench.start(List.of(first));
		var running = new java.util.concurrent.atomic.AtomicBoolean(true);
		var produced = new java.util.concurrent.atomic.AtomicLong();
		var requestable = BillingBench.sequence("Renewed");
		var workers = new ArrayList<Thread>();
		for (int i = 0; i < 4; i++) {
			var context = BillingBench.context(i, requestable);
			var worker = new Thread(() -> {
				try {
					while (running.get()) {
						billing.insertBilling(context);
						produced.incrementAndGet();
						// requests, not a flood beyond the capacity of a queue
						java.util.concurrent.locks.LockSupport.parkNanos(20_000);
					}
				} catch (EngineException e) {
					throw new RuntimeException(e);
				}
			});
			workers.add(worker);
			worker.start();
		}
		var recorders = new ArrayList<Recorder>(List.of(first));
		var replaced = new ArrayList<TicketQueue>();
		for (int i = 0; i < 5; i++) {
			Thread.sleep(50);
			var next = new Recorder();
			recorders.add(next);
			replaced.addAll(queues(billing));
			BillingBench.useTicketManagers(billing, List.of(next));
		}
		replaced.addAll(queues(billing));
		running.set(false);
		for (var worker : workers) {
			worker.join();
		}
		billing.destroy();
		billing = null;
		// the replaced queues were closed without waiting for their writers
		for (var queue : replaced) {
			queue.close(true);
		}
		long written = 0;
		for (var recorder : recorders) {
			written += recorder.tickets.size();
		}
		long dropped = 0;
		for (var queue : replaced) {
			dropped += queue.dropped();
		}
		assertEquals("none dropped", 0, dropped);
		assertEquals("each ticket written by the managers of its time", produced.get(), written);
	}

	@Test
	public void theStopDoesNotWaitLongForAStuckTicketManager() throws Exception {
		var stuck = new Gated();
		billing = BillingBench.start(List.of(stuck));
		var queues = queues(billing);
		var context = BillingBench.context(0, BillingBench.sequence("Stuck"));
		for (int n = 0; n < 10; n++) {
			billing.insertBilling(context);
		}
		Thread.sleep(300);
		long t0 = System.nanoTime();
		billing.destroy();
		billing = null;
		long took = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
		try {
			assertTrue("stop took " + took + " ms", took <= TicketQueue.FLUSH_TIMEOUT + 1500);
		} finally {
			stuck.gate.countDown();
		}
		Thread.sleep(300);
		assertEquals("written or counted as dropped", 10, stuck.tickets.size() + queues.get(0).dropped());
	}
}
