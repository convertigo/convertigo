/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;

public class FlowMetadataCacheTest {
	@Test public void twentyColdCallersBuildOnceAndReuseImmutableText() throws Exception {
		var cache = new FlowMetadataCache(32, 4096);
		var workers = Executors.newFixedThreadPool(20);
		var gate = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		try {
			var futures = new ArrayList<Future<String>>();
			for (int i = 0; i < 20; i++) futures.add(workers.submit(() -> {
				gate.await();
				return cache.get("project/catalog", "revision1", () -> { release.await(); return "{\"answer\":42}"; });
			}));
			gate.countDown();
			long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (cache.joins() < 19 && System.nanoTime() < until) Thread.sleep(1);
			assertEquals(19, cache.joins());
			release.countDown();
			for (var future : futures) assertEquals("{\"answer\":42}", future.get(5, TimeUnit.SECONDS));
			assertEquals(1, cache.builds());
			assertEquals("{\"answer\":42}", cache.get("project/catalog", "revision1", () -> { fail("Hot builder"); return ""; }));
		} finally { release.countDown(); workers.shutdownNow(); }
	}

	@Test public void revisionAndClearRejectOldInflightPublication() throws Exception {
		var cache = new FlowMetadataCache(32, 4096);
		var started = new CountDownLatch(1); var release = new CountDownLatch(1);
		var worker = Executors.newSingleThreadExecutor();
		try {
			var old = worker.submit(() -> cache.get("catalog", "old", () -> { started.countDown(); release.await(); return "old"; }));
			assertTrue(started.await(5, TimeUnit.SECONDS));
			cache.clear();
			assertEquals("new", cache.get("catalog", "new", () -> "new"));
			release.countDown(); assertEquals("old", old.get(5, TimeUnit.SECONDS));
			assertEquals("new", cache.get("catalog", "new", () -> { fail("Old publication replaced the new generation"); return ""; }));
			assertEquals("changed", cache.get("catalog", "changed", () -> "changed"));
			assertEquals(1, cache.size()); assertEquals(7, cache.characters());
		} finally { release.countDown(); worker.shutdownNow(); }
	}

	@Test public void failedBuilderCanRetryAndRecursiveBuilderCannotDeadlock() throws Exception {
		var cache = new FlowMetadataCache(2, 16);
		try { cache.get("a", "1", () -> { throw new IllegalArgumentException("fixture failure"); }); fail(); }
		catch (IllegalArgumentException expected) { }
		assertEquals(0, cache.size());
		assertEquals("ok", cache.get("a", "1", () -> "ok"));
		try { cache.get("b", "1", () -> cache.get("b", "1", () -> "bad")); fail(); }
		catch (IllegalStateException expected) { }
		assertEquals("retry", cache.get("b", "1", () -> "retry"));
	}

	@Test public void interruptedFollowerDoesNotCancelOwnerOrLeakItsSlot() throws Exception {
		var cache = new FlowMetadataCache(4, 4096);
		var started = new CountDownLatch(1); var release = new CountDownLatch(1);
		var workers = Executors.newFixedThreadPool(2);
		var interrupted = new AtomicBoolean();
		try {
			var owner = workers.submit(() -> cache.get("a", "1", () -> { started.countDown(); release.await(); return "ready"; }));
			assertTrue(started.await(5, TimeUnit.SECONDS));
			var waiting = workers.submit(() -> {
				try { return cache.get("a", "1", () -> "wrong"); }
				catch (InterruptedException expected) { interrupted.set(Thread.currentThread().isInterrupted()); throw expected; }
			});
			long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (cache.joins() == 0 && System.nanoTime() < until) Thread.sleep(1);
			assertEquals(1, cache.joins()); waiting.cancel(true);
			workers.shutdown(); release.countDown();
			assertEquals("ready", owner.get(5, TimeUnit.SECONDS));
			assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS)); assertTrue(interrupted.get());
			assertEquals("ready", cache.get("a", "1", () -> "wrong"));
			assertEquals(1, cache.builds());
		} finally { release.countDown(); workers.shutdownNow(); }
	}

	@Test public void entryAndMemoryBoundsApplyWithoutKeepingOversizedValues() throws Exception {
		var cache = new FlowMetadataCache(2, 8);
		cache.get("a", "1", () -> "1234"); cache.get("b", "1", () -> "5678");
		cache.get("c", "1", () -> "abcde");
		assertTrue(cache.size() <= 2); assertTrue(cache.characters() <= 8);
		assertEquals("123456789", cache.get("large", "1", () -> "123456789"));
		assertTrue(cache.size() <= 2); assertTrue(cache.characters() <= 8);
		long previous = cache.builds(); cache.get("large", "1", () -> "123456789");
		assertEquals(previous + 1, cache.builds());
		cache.clear(); assertEquals(0, cache.size()); assertEquals(0, cache.characters());
	}
}
