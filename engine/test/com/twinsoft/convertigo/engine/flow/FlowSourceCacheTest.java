/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The snapshots of the server, one cache for every project: different keys load at the same time, the callers of a
 * missing key wait for its single load, an expired snapshot is served while one caller refreshes it, a failure is not
 * kept, a clear during a load is never followed by the snapshot of that load, and the bounds hold under concurrency.
 */
public class FlowSourceCacheTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private ExecutorService pool;

	@Before
	public void create() {
		pool = Executors.newCachedThreadPool();
	}

	@After
	public void release() {
		pool.shutdownNow();
	}

	private static Supplier<String> never() {
		return () -> {
			throw new AssertionError("Loaded again");
		};
	}

	/** A load held until released. */
	private static Supplier<String> held(CountDownLatch started, CountDownLatch release, String value) {
		return () -> {
			started.countDown();
			try {
				assertTrue("The load stayed held", release.await(20, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return value;
		};
	}

	private <T> Future<T> later(Callable<T> call) {
		return pool.submit(call);
	}

	private static Map<?, ?> entries(FlowSourceCache cache) throws Exception {
		var field = FlowSourceCache.class.getDeclaredField("entries");
		field.setAccessible(true);
		return (Map<?, ?>) field.get(cache);
	}

	private static long characters(FlowSourceCache cache) throws Exception {
		var field = FlowSourceCache.class.getDeclaredField("characters");
		field.setAccessible(true);
		synchronized (cache) {
			return field.getLong(cache);
		}
	}

	private static long counted(FlowSourceCache cache) throws Exception {
		var length = Class.forName(FlowSourceCache.class.getName() + "$Entry").getDeclaredMethod("length");
		length.setAccessible(true);
		long total = 0;
		synchronized (cache) {
			for (var value : entries(cache).values()) {
				total += (int) length.invoke(value);
			}
		}
		return total;
	}

	@Test
	public void differentKeysLoadAtTheSameTime() throws Exception {
		var cache = new FlowSourceCache(8, 60_000);
		// each load waits for the other one: a lock shared by the keys would time this barrier out
		var together = new CyclicBarrier(2);
		Supplier<String> a = () -> { await(together); return "a"; }, b = () -> { await(together); return "b"; };
		var first = later(() -> cache.directory(new File("a"), a));
		var second = later(() -> cache.directory(new File("b"), b));
		assertEquals("a", first.get(20, TimeUnit.SECONDS));
		assertEquals("b", second.get(20, TimeUnit.SECONDS));
	}

	private static void await(CyclicBarrier barrier) {
		try {
			barrier.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void theCallersOfAMissingKeyWaitForItsSingleLoadWhileAnotherKeyLoads() throws Exception {
		var cache = new FlowSourceCache(8, 60_000);
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var loads = new AtomicInteger();
		var arrived = new CountDownLatch(6);
		var held = held(started, release, "walked");
		var callers = new ArrayList<Future<String>>();
		for (int i = 0; i < 6; i++) {
			callers.add(later(() -> {
				arrived.countDown();
				return cache.directory(new File("tree"), () -> {
					loads.incrementAndGet();
					return held.get();
				});
			}));
		}
		assertTrue(started.await(10, TimeUnit.SECONDS));
		assertTrue(arrived.await(10, TimeUnit.SECONDS));
		Thread.sleep(100);
		try {
			assertEquals("Another key is not delayed", "other", later(() -> cache.directory(new File("other"), () -> "other")).get(5, TimeUnit.SECONDS));
		} finally {
			release.countDown();
		}
		for (var caller : callers) {
			assertEquals("walked", caller.get(20, TimeUnit.SECONDS));
		}
		assertEquals(1, loads.get());
		assertEquals("walked", cache.directory(new File("tree"), never()));
	}

	@Test
	public void anExpiredSnapshotIsServedWhileOneCallerRefreshesIt() throws Exception {
		// every snapshot expired at once
		var cache = new FlowSourceCache(8, 0);
		assertEquals("1", cache.directory(new File("tree"), () -> "1"));
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var refreshing = later(() -> cache.directory(new File("tree"), held(started, release, "2")));
		assertTrue(started.await(10, TimeUnit.SECONDS));
		try {
			assertEquals("Served without waiting for the refresh", "1", later(() -> cache.directory(new File("tree"), never())).get(5, TimeUnit.SECONDS));
		} finally {
			release.countDown();
		}
		assertEquals("2", refreshing.get(20, TimeUnit.SECONDS));
		assertEquals("3", cache.directory(new File("tree"), () -> "3"));
	}

	@Test
	public void theCallersOfAFailedLoadSeeItsFailureWhichIsNotKept() throws Exception {
		var cache = new FlowSourceCache(8, 60_000);
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var arrived = new CountDownLatch(1);
		var failure = new IllegalStateException("unreadable");
		var held = held(started, release, null);
		var owner = later(() -> cache.directory(new File("tree"), () -> {
			held.get();
			throw failure;
		}));
		assertTrue(started.await(10, TimeUnit.SECONDS));
		var waiter = later(() -> {
			arrived.countDown();
			return cache.directory(new File("tree"), never());
		});
		assertTrue(arrived.await(10, TimeUnit.SECONDS));
		Thread.sleep(100);
		release.countDown();
		assertSame(failure, cause(owner));
		assertSame(failure, cause(waiter));
		assertEquals("loaded", cache.directory(new File("tree"), () -> "loaded"));
	}

	private static Throwable cause(Future<?> future) throws Exception {
		try {
			future.get(20, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			return e.getCause();
		}
		fail("Expected a failure");
		return null;
	}

	@Test
	public void aClearDuringALoadIsNeverFollowedByTheSnapshotOfThatLoad() throws Exception {
		var directory = folder.newFolder("project");
		var tree = new File(directory, "_flow/modules");
		var cache = new FlowSourceCache(8, 60_000);
		for (var clear : new String[] { "directory", "all" }) {
			cache.clear();
			var started = new CountDownLatch(1);
			var release = new CountDownLatch(1);
			var before = later(() -> cache.directory(tree, held(started, release, "before " + clear)));
			assertTrue(started.await(10, TimeUnit.SECONDS));
			try {
				// the clear does not wait for the load, nor does a caller after it
				later(() -> {
					if (clear.equals("all")) cache.clear(); else cache.clear(directory);
					return null;
				}).get(5, TimeUnit.SECONDS);
				assertEquals("after " + clear, later(() -> cache.directory(tree, () -> "after " + clear)).get(5, TimeUnit.SECONDS));
			} finally {
				release.countDown();
			}
			assertEquals("Its own callers get the load", "before " + clear, before.get(20, TimeUnit.SECONDS));
			assertEquals("after " + clear, cache.directory(tree, never()));
		}
	}

	@Test
	public void theBoundsHoldUnderConcurrency() throws Exception {
		var cache = new FlowSourceCache(16, 60_000);
		var start = new CyclicBarrier(8);
		var workers = new ArrayList<Future<Object>>();
		for (int t = 0; t < 8; t++) {
			var thread = t;
			workers.add(later(() -> {
				start.await(10, TimeUnit.SECONDS);
				for (int i = 0; i < 300; i++) {
					var key = "k" + thread + "-" + i;
					assertEquals(key, cache.directory(new File(key), () -> key).substring(0, key.length()));
					cache.directory(new File("shared" + (i % 4)), () -> "x".repeat(1000));
				}
				return null;
			}));
		}
		for (var worker : workers) {
			worker.get(60, TimeUnit.SECONDS);
		}
		assertTrue("entries: " + entries(cache).size(), entries(cache).size() <= 16);
		assertEquals("The characters of the snapshots kept", counted(cache), characters(cache));
		// the characters, by large texts
		var large = new FlowSourceCache(64, 60_000);
		for (int i = 0; i < 12; i++) {
			var text = String.valueOf((char) ('a' + i)).repeat(1_900_000);
			large.directory(new File("large" + i), () -> text);
		}
		assertTrue("characters: " + characters(large), characters(large) <= 16 * 1024 * 1024);
		assertEquals(counted(large), characters(large));
	}

	@Test
	public void aLargeFileKeepsItsStampAndIsReadAtEachUse() throws Exception {
		var file = folder.newFile("large.js");
		Files.writeString(file.toPath(), "x".repeat(2_000_001));
		var cache = new FlowSourceCache(8, 60_000);
		var stamp = cache.fingerprint(file);
		assertEquals(2_000_001, cache.text(file).length());
		assertEquals(stamp, cache.fingerprint(file));
		assertTrue("Its text is not kept", characters(cache) < 1000);
		Files.writeString(file.toPath(), "y".repeat(2_000_001));
		assertEquals('y', cache.text(file).charAt(0));
		Files.delete(file.toPath());
		cache.clear();
		assertTrue(cache.fingerprint(file).startsWith("missing:"));
	}
}
