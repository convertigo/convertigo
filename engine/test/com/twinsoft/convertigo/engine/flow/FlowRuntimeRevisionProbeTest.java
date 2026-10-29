/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;

/**
 * The probe of the source revision of a runtime, the fallback for external file edits: one caller computes the
 * fingerprint while the others go on without waiting, a changed fingerprint is seen by every lookup after it, and a
 * fingerprint that cannot be computed is probed again by the next caller.
 */
public class FlowRuntimeRevisionProbeTest {
	private FlowRuntimeApi api;
	private ExecutorService pool;

	@Before
	public void create() {
		try (var cx = Context.enter()) {
			api = new FlowRuntimeApi(cx);
		}
		pool = Executors.newCachedThreadPool();
	}

	@After
	public void release() {
		pool.shutdownNow();
	}

	/** The minute between two probes is over. */
	private void expire() throws Exception {
		var field = FlowRuntimeApi.class.getDeclaredField("nextSourceProbe");
		field.setAccessible(true);
		field.setLong(api, 0);
	}

	private static Supplier<String> never() {
		return () -> {
			throw new AssertionError("Probed again");
		};
	}

	@Test
	public void theCallersGoOnWithoutWaitingWhileOneProbes() throws Exception {
		assertTrue("The first probe takes the revision", api.revisionMatches(() -> "first"));
		assertTrue("Not probed again within a minute", api.revisionMatches(never()));
		expire();
		var probing = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var computed = new AtomicInteger();
		var prober = pool.submit(() -> api.revisionMatches(() -> {
			computed.incrementAndGet();
			probing.countDown();
			try {
				// file walks on a slow file system
				assertTrue(release.await(20, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
			return "first";
		}));
		assertTrue(probing.await(10, TimeUnit.SECONDS));
		try {
			var others = new ArrayList<Future<Boolean>>();
			for (int i = 0; i < 8; i++) {
				others.add(pool.submit(() -> api.revisionMatches(never())));
			}
			for (var other : others) {
				assertTrue("A caller goes on with the current revision", other.get(2, TimeUnit.SECONDS));
			}
		} finally {
			release.countDown();
		}
		assertTrue(prober.get(10, TimeUnit.SECONDS));
		assertEquals(1, computed.get());
		assertTrue("Probed: not again within a minute", api.revisionMatches(never()));
	}

	@Test
	public void aChangedFingerprintIsSeenByEveryLookupAfterIt() throws Exception {
		assertTrue(api.revisionMatches(() -> "first"));
		expire();
		assertTrue("The same fingerprint", api.revisionMatches(() -> "first"));
		expire();
		assertFalse("A file changed outside", api.revisionMatches(() -> "second"));
		assertFalse("Seen by the next lookups, without probing again", api.revisionMatches(never()));
		expire();
		assertFalse(api.revisionMatches(never()));
	}

	@Test
	public void aFingerprintThatCannotBeComputedIsProbedAgainByTheNextCaller() throws Exception {
		var failure = new IllegalStateException("file system unavailable");
		try {
			api.revisionMatches(() -> { throw failure; });
			fail();
		} catch (IllegalStateException expected) {
			assertSame(failure, expected);
		}
		assertTrue("The next caller takes the revision", api.revisionMatches(() -> "first"));
		expire();
		try {
			api.revisionMatches(() -> { throw failure; });
			fail();
		} catch (IllegalStateException expected) {
			assertSame(failure, expected);
		}
		var computed = new AtomicInteger();
		assertTrue("Probed again, not left to another probe in progress", api.revisionMatches(() -> {
			computed.incrementAndGet();
			return "first";
		}));
		assertEquals(1, computed.get());
		expire();
		assertFalse(api.revisionMatches(() -> "second"));
	}
}
