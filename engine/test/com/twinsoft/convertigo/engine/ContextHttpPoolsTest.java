package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.junit.After;
import org.junit.Test;

import com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface;

public class ContextHttpPoolsTest {
	private static final long IDLE = 120_000L;

	private static HttpClient pooled() {
		return new HttpClient(new MultiThreadedHttpConnectionManager());
	}

	private static HttpClientInterface pooled4() {
		return request -> null;
	}

	private static boolean isShutdown(HttpClient client) throws Exception {
		var field = MultiThreadedHttpConnectionManager.class.getDeclaredField("shutdown");
		field.setAccessible(true);
		return (Boolean) field.get(client.getHttpConnectionManager());
	}

	@After public void clear() {
		ContextHttpPools.clear();
	}

	@Test public void sameContextReusesItsPools() {
		long now = 1_000_000L;
		var first = ContextHttpPools.httpClient3("s1_ctx", IDLE, now, ContextHttpPoolsTest::pooled);
		var second = ContextHttpPools.httpClient3("s1_ctx", IDLE, now + 1_000, ContextHttpPoolsTest::pooled);
		assertSame(first, second);
		var first4 = ContextHttpPools.httpClient4("s1_ctx", IDLE, now, ContextHttpPoolsTest::pooled4);
		assertSame(first4, ContextHttpPools.httpClient4("s1_ctx", IDLE, now + 1_000, ContextHttpPoolsTest::pooled4));
		assertEquals(1, ContextHttpPools.size());
	}

	@Test public void contextsNeverSharePools() {
		long now = 1_000_000L;
		var a = ContextHttpPools.httpClient3("s1_ctx", IDLE, now, ContextHttpPoolsTest::pooled);
		var b = ContextHttpPools.httpClient3("s2_ctx", IDLE, now, ContextHttpPoolsTest::pooled);
		var c = ContextHttpPools.httpClient3("s1_other", IDLE, now, ContextHttpPoolsTest::pooled);
		assertNotSame(a, b);
		assertNotSame(a, c);
		assertEquals(3, ContextHttpPools.size());
	}

	@Test public void idlePoolIsClosedAndReplaced() throws Exception {
		long now = 1_000_000L;
		var old = ContextHttpPools.httpClient3("s1_ctx", IDLE, now, ContextHttpPoolsTest::pooled);
		var kept = ContextHttpPools.httpClient3("s2_ctx", IDLE, now, ContextHttpPoolsTest::pooled);
		long later = now + IDLE + ContextHttpPools.SWEEP_INTERVAL_MILLIS + 1;
		ContextHttpPools.httpClient3("s2_ctx", IDLE, now + IDLE, ContextHttpPoolsTest::pooled);
		var fresh = ContextHttpPools.httpClient3("s1_ctx", IDLE, later, ContextHttpPoolsTest::pooled);
		assertNotSame(old, fresh);
		assertSame(kept, ContextHttpPools.httpClient3("s2_ctx", IDLE, later, ContextHttpPoolsTest::pooled));
		assertTrue(isShutdown(old));
		assertFalse(isShutdown(fresh));
	}

	@Test public void releasedContextClosesItsPool() throws Exception {
		long now = 1_000_000L;
		var pool = ContextHttpPools.httpClient3("s1_ctx", IDLE, now, ContextHttpPoolsTest::pooled);
		ContextHttpPools.release("s1_ctx");
		assertTrue(isShutdown(pool));
		assertEquals(0, ContextHttpPools.size());
		assertNotSame(pool, ContextHttpPools.httpClient3("s1_ctx", IDLE, now, ContextHttpPoolsTest::pooled));
	}

	@Test public void shortTimeoutKeepsAMinimumIdleDelay() {
		long now = 1_000_000L;
		var first = ContextHttpPools.httpClient3("s1_ctx", 1_000L, now, ContextHttpPoolsTest::pooled);
		assertSame(first, ContextHttpPools.httpClient3("s1_ctx", 1_000L, now + 30_000L, ContextHttpPoolsTest::pooled));
	}
}
