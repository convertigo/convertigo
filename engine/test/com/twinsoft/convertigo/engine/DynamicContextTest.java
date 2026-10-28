/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The contexts of a session asked for with a dynamic name (__context=*) by concurrent requests, and their removal at
 * the end of a request (__removeContext) while another request holds them.
 */
public class DynamicContextTest {
	private Engine previousEngine;
	private ContextManager manager;
	private ConcurrentHashMap<String, Context> contexts;

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Engine.logContextManager = Engine.logContext = Engine.logUsageMonitor = Logger.getLogger("dynamic-context-test");
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
	public void concurrentRequestsOfASessionGetTheirOwnDynamicContext() throws Exception {
		int threads = 16;
		var pool = Executors.newFixedThreadPool(threads);
		try {
			for (int round = 0; round < 300; round++) {
				var barrier = new CyclicBarrier(threads);
				var futures = new ArrayList<Future<Context>>();
				for (int i = 0; i < threads; i++) {
					futures.add(pool.submit(() -> {
						barrier.await(5, TimeUnit.SECONDS);
						return manager.get(null, "default*", "session", null, "Test", null, "Execute");
					}));
				}
				var ids = new HashSet<String>();
				var identities = new HashSet<Integer>();
				for (var future : futures) {
					var context = future.get(10, TimeUnit.SECONDS);
					ids.add(context.contextID);
					identities.add(System.identityHashCode(context));
				}
				assertEquals("round " + round + ": each request has its own context", threads, ids.size());
				assertEquals(threads, identities.size());
				contexts.clear();
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void theEndOfARequestDoesNotWaitForAnotherRequestHoldingItsContext() throws Exception {
		var context = manager.get(null, "default*", "session", null, "Test", null, "Execute");
		var holder = Executors.newSingleThreadExecutor();
		var held = new java.util.concurrent.CountDownLatch(1);
		var release = new java.util.concurrent.CountDownLatch(1);
		try {
			var holding = holder.submit(() -> {
				try (var lock = manager.lockContext(context)) {
					held.countDown();
					release.await(10, TimeUnit.SECONDS);
				}
				return null;
			});
			assertTrue(held.await(5, TimeUnit.SECONDS));
			long start = System.nanoTime();
			assertFalse(manager.removeAtEndOfRequest(context, null));
			assertTrue("no wait", System.nanoTime() - start < TimeUnit.SECONDS.toNanos(1));
			assertTrue("left to the request holding it", context.removalRequired());
			assertSame(context, contexts.get(context.contextID));
			release.countDown();
			holding.get(5, TimeUnit.SECONDS);
			// the holder (or the next request taking it) removes it; free, it is removed at once
			assertTrue(manager.removeAtEndOfRequest(context, null));
			assertNull(contexts.get(context.contextID));
			assertTrue(context.isDestroying);
			assertNotSame(context, manager.get(null, context.name, "session", null, "Test", null, "Execute"));
		} finally {
			release.countDown();
			holder.shutdownNow();
		}
	}
}
