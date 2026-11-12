/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The removal of the contexts at the stop of the Studio, from its user interface thread, while a request holds one of
 * them (a sequence still running, maybe waiting for the user interface).
 */
public class StudioStopContextTest {
	private Engine previousEngine;
	private ContextManager manager;
	private ConcurrentHashMap<String, Context> contexts;

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Engine.logContextManager = Engine.logContext = Engine.logUsageMonitor = Logger.getLogger("studio-stop-context-test");
		previousEngine = Engine.theApp;
		Engine.theApp = new Engine();
		manager = new ContextManager();
		var field = ContextManager.class.getDeclaredField("contexts");
		field.setAccessible(true);
		contexts = new ConcurrentHashMap<>();
		field.set(manager, contexts);
	}

	@After
	public void tearDown() {
		Engine.theApp = previousEngine;
	}

	@Test
	public void theStopDoesNotWaitForARequestThatKeepsItsContext() throws Exception {
		var free = manager.get(null, "default*", "session", null, "Test", null, "Execute");
		var held = manager.get(null, "default*", "session", null, "Test", null, "Execute");
		var holder = Executors.newSingleThreadExecutor();
		var locked = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		try {
			var holding = holder.submit(() -> {
				try (var lock = manager.lockContext(held)) {
					locked.countDown();
					release.await(30, TimeUnit.SECONDS);
				}
				return null;
			});
			assertTrue(locked.await(5, TimeUnit.SECONDS));

			long start = System.nanoTime();
			assertEquals("the held context is left", 1, manager.removeAllWithoutWaitingMoreThan(300));
			long elapsed = System.nanoTime() - start;
			assertTrue("waits for the delay only", elapsed >= TimeUnit.MILLISECONDS.toNanos(250));
			assertTrue("waits for the delay only", elapsed < TimeUnit.SECONDS.toNanos(5));

			assertNull("the free context is removed", contexts.get(free.contextID));
			assertTrue(free.isDestroying);
			assertSame(held, contexts.get(held.contextID));
			assertFalse(held.isDestroying);

			release.countDown();
			holding.get(5, TimeUnit.SECONDS);
			assertEquals(0, manager.removeAllWithoutWaitingMoreThan(300));
			assertNull(contexts.get(held.contextID));
			assertTrue(held.isDestroying);
		} finally {
			release.countDown();
			holder.shutdownNow();
		}
	}

	@Test
	public void aContextReleasedWithinTheDelayIsRemoved() throws Exception {
		var held = manager.get(null, "default*", "session", null, "Test", null, "Execute");
		var holder = Executors.newSingleThreadExecutor();
		var locked = new CountDownLatch(1);
		try {
			var holding = holder.submit(() -> {
				try (var lock = manager.lockContext(held)) {
					locked.countDown();
					Thread.sleep(200);
				}
				return null;
			});
			assertTrue(locked.await(5, TimeUnit.SECONDS));

			long start = System.nanoTime();
			assertEquals(0, manager.removeAllWithoutWaitingMoreThan(10000));
			assertTrue("no wait once released", System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5));
			assertNull(contexts.get(held.contextID));
			assertTrue(held.isDestroying);
			holding.get(5, TimeUnit.SECONDS);
		} finally {
			holder.shutdownNow();
		}
	}
}
