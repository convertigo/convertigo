/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineStatistics;

/**
 * The totals of Statistics are shared by all its instances, one EngineStatistics per context: concurrent requests, each
 * with its own instance, must keep exact totals. The instance methods only locked their own instance, so concurrent
 * requests changed the same HashMap without a common lock: it could lose entries, fail on a ClassCastException
 * (HashMap$Node cannot be cast to HashMap$TreeNode) or loop.
 */
public class StatisticsTest {
	private static final String TASK = EngineStatistics.EXECUTE_SEQUENCE_STEPS;
	private static final List<String> warnings = new CopyOnWriteArrayList<>();
	private static Logger previous;
	private static Logger logger;
	private static AppenderSkeleton appender;

	@BeforeClass
	public static void setUp() {
		previous = Engine.logEngine;
		logger = Logger.getLogger("statistics-test");
		logger.setLevel(Level.WARN);
		logger.setAdditivity(false);
		appender = new AppenderSkeleton() {
			@Override
			protected void append(LoggingEvent event) {
				warnings.add("" + event.getMessage());
			}

			@Override
			public void close() {
			}

			@Override
			public boolean requiresLayout() {
				return false;
			}
		};
		logger.addAppender(appender);
		Engine.logEngine = logger;
	}

	@AfterClass
	public static void tearDown() {
		logger.removeAppender(appender);
		Engine.logEngine = previous;
		Statistics.reset();
	}

	@Test(timeout = 120000)
	public void concurrentInstancesKeepExactTotals() throws Exception {
		Statistics.reset();
		warnings.clear();
		int threads = Math.max(8, Runtime.getRuntime().availableProcessors());
		int rounds = 50000;
		var results = runConcurrently(threads, index -> () -> {
			var statistics = new EngineStatistics();
			String own = "task-" + index;
			long[] totals = new long[4];
			for (int i = 0; i < rounds; i++) {
				// a step of a sequence and a nested call
				String outer = statistics.start(TASK);
				String inner = statistics.start(own);
				long duration = statistics.stop(inner);
				assertTrue("stop failed", duration >= 0);
				totals[2]++;
				totals[3] += duration;

				duration = statistics.stop(outer, i % 2 == 1);
				assertTrue("stop failed", duration >= 0);
				assertEquals("the stopped statistic is lost", duration, statistics.getLatestDuration(TASK));
				totals[0]++;
				totals[1] += duration;

				statistics.add(TASK, i % 5);
				totals[0]++;
				totals[1] += i % 5;

				Statistics.getAverage(TASK);
				Statistics.reset("scratch-" + index);
			}
			return totals;
		});

		assertEquals("unexpected warnings: " + warnings, 0, warnings.size());
		assertEquals("statistics left started", 0, table("tasks").size());
		long count = 0, sum = 0;
		for (int k = 0; k < threads; k++) {
			long[] totals = results.get(k);
			count += totals[0];
			sum += totals[1];
			assertEquals(totals[2], table("dividers").get("task-" + k));
			assertEquals(totals[3], table("sums").get("task-" + k));
		}
		assertEquals(2L * threads * rounds, count);
		assertEquals(count, table("dividers").get(TASK));
		assertEquals(sum, table("sums").get(TASK));
		assertEquals(sum / count, Statistics.getAverage(TASK));
	}

	@Test(timeout = 120000)
	public void resetWhileRequestsRun() throws Exception {
		Statistics.reset();
		warnings.clear();
		int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
		var running = new AtomicBoolean(true);
		var results = runConcurrently(threads + 1, index -> () -> {
			if (index == threads) {
				for (int i = 0; i < 20; i++) {
					Statistics.reset();
					Thread.sleep(5);
				}
				running.set(false);
				return null;
			}
			var statistics = new EngineStatistics();
			while (running.get()) {
				String t = statistics.start(TASK);
				assertTrue("stop failed", statistics.stop(t, true) >= 0);
				statistics.add(TASK, 1);
				Statistics.getAverage(TASK);
			}
			return null;
		});
		assertEquals(threads + 1, results.size());
		assertEquals("unexpected warnings: " + warnings, 0, warnings.size());
	}

	private interface Worker {
		Callable<long[]> of(int index);
	}

	private static List<long[]> runConcurrently(int threads, Worker worker) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
			var thread = new Thread(r, "statistics-test");
			// a corrupted HashMap can loop forever: do not keep the JVM alive
			thread.setDaemon(true);
			return thread;
		});
		try {
			var gate = new CyclicBarrier(threads);
			var runs = new ArrayList<Future<long[]>>(threads);
			for (int k = 0; k < threads; k++) {
				var task = worker.of(k);
				runs.add(pool.submit(() -> {
					gate.await();
					return task.call();
				}));
			}
			var results = new ArrayList<long[]>(threads);
			for (var run : runs) {
				results.add(run.get(100, TimeUnit.SECONDS));
			}
			return results;
		} finally {
			pool.shutdownNow();
		}
	}

	@SuppressWarnings("unchecked")
	private static <V> Map<String, V> table(String name) throws Exception {
		var field = Statistics.class.getDeclaredField(name);
		field.setAccessible(true);
		synchronized (Statistics.class) {
			return new HashMap<>((Map<String, V>) field.get(null));
		}
	}
}
