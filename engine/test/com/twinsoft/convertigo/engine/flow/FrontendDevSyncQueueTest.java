/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * The development viewer of a frontend follows the sources written by a provider (the Flow MCP) on a background thread:
 * its synchronization takes the frontend authoring lock of the server, which the Studio holds while it describes its
 * tree, and a code-set of a page with reveal waited more than 300 s for its answer. The write returns at once, the
 * writes made during a synchronization are coalesced into the next one, and a failing synchronization stops none.
 */
public class FrontendDevSyncQueueTest {

	@Test
	public void aWriteNeverWaitsForTheSynchronizationOfItsViewer() throws Exception {
		var studioBusy = new CountDownLatch(1);
		var firstStarted = new CountDownLatch(1);
		var allDone = new CountDownLatch(2);
		var syncs = Collections.synchronizedList(new ArrayList<String>());
		var queue = new FrontendDevSyncQueue<String>("test dev sync", (key, paths) -> {
			firstStarted.countDown();
			try {
				assertTrue(studioBusy.await(30, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			syncs.add(key + " " + paths);
			allDone.countDown();
		}, (key, e) -> fail("unexpected failure: " + e));

		var started = System.nanoTime();
		queue.submit("App", "routes/+page.flow.svelte");
		assertTrue("the first synchronization did not start", firstStarted.await(30, TimeUnit.SECONDS));
		// written while the viewer synchronizes: answered at once, coalesced into one next synchronization
		queue.submit("App", "routes/items/+page.flow.svelte");
		queue.submit("App", "routes/orders/+page.flow.svelte");
		queue.submit("App", "routes/items/+page.flow.svelte");
		var answeredMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
		assertTrue("the writes waited for the synchronization: " + answeredMs + " ms", answeredMs < 1000);
		assertTrue("nothing synchronized while the Studio was busy", syncs.isEmpty());

		studioBusy.countDown();
		assertTrue("the coalesced synchronization did not run", allDone.await(30, TimeUnit.SECONDS));
		Thread.sleep(200);
		assertEquals(List.of("App [routes/+page.flow.svelte]",
				"App [routes/items/+page.flow.svelte, routes/orders/+page.flow.svelte]"), new ArrayList<>(syncs));
	}

	@Test
	public void aFailingSynchronizationStopsNone() throws Exception {
		var done = new CountDownLatch(1);
		var failures = Collections.synchronizedList(new ArrayList<String>());
		var queue = new FrontendDevSyncQueue<String>("test dev sync", (key, paths) -> {
			if ("Broken".equals(key)) {
				throw new IllegalStateException("viewer unavailable");
			}
			done.countDown();
		}, (key, e) -> failures.add(key + ": " + e.getMessage()));
		queue.submit("Broken", "routes/+page.flow.svelte");
		queue.submit("App", "routes/+page.flow.svelte");
		assertTrue("a failing synchronization stopped the next one", done.await(30, TimeUnit.SECONDS));
		assertEquals(List.of("Broken: viewer unavailable"), new ArrayList<>(failures));
	}
}
