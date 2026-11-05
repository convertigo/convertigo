package com.twinsoft.convertigo.beans.connectors;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.junit.Test;
import org.junit.BeforeClass;
import org.apache.log4j.Logger;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.sessions.ContextStore;

public class SiteClipperReadOnlyContextTest {
	@BeforeClass public static void initializeProperties() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = Logger.getLogger("siteclipper-readonly-test");
		Engine.theApp = new Engine();
	}
	private static Cookie cookie(String name, String value) {
		var cookie = new Cookie("EXAMPLE.org", name, value);
		cookie.setPath("/");
		return cookie;
	}

	private static HttpState state(Cookie... cookies) {
		var state = new HttpState();
		state.addCookies(cookies);
		return state;
	}

	private static String value(HttpState state, String name) {
		for (var cookie : state.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
		return null;
	}

	@Test public void cookieDeltaAddsChangesAndRemovesExpiredCookies() {
		var before = state(cookie("changed", "old"), cookie("removed", "old"));
		var snapshot = new SiteClipperCookieState(before);
		before.addCookie(cookie("changed", "new"));
		before.addCookie(cookie("added", "new"));
		var expired = cookie("removed", "old");
		expired.setExpiryDate(new java.util.Date(0));
		before.addCookie(expired);
		var merged = snapshot.delta(before).apply(state(cookie("other", "kept"), cookie("removed", "old")));
		assertEquals("new", value(merged, "changed"));
		assertEquals("new", value(merged, "added"));
		assertNull(value(merged, "removed"));
		assertEquals("kept", value(merged, "other"));
	}

	@Test public void recreatedStateIsAChangeEvenWithIdenticalCookies() {
		var before = state(cookie("a", "one"));
		var delta = new SiteClipperCookieState(before).delta(state(cookie("a", "one")));
		assertFalse(delta.isEmpty());
		assertEquals("one", value(delta.apply(state(cookie("other", "two"))), "a"));
		var cleared = new SiteClipperCookieState(before).delta(null);
		assertNull(value(cleared.apply(state(cookie("a", "one"), cookie("other", "two"))), "a"));
		assertEquals("two", value(cleared.apply(state(cookie("a", "one"), cookie("other", "two"))), "other"));
	}

	@Test public void localCopyReadsNewerStoredCookies() throws Exception {
		var store = new MemoryStore();
		var manager = new TestManager();
		manager.install(store);
		var local = new Context("session_clip");
		local.name = "clip";
		local.storedAccessTime = 10;
		local.httpState = state(cookie("own", "old"));
		manager.cache(local);
		var stored = new Context(local.contextID);
		stored.lastAccessTime = 11;
		stored.httpState = state(cookie("own", "new"));
		store.data.put(local.contextID, stored);
		assertSame(local, SiteClipperConnector.Shuttle.readRedisContext(manager, store, local.contextID, "clip"));
		assertEquals("new", value(local.httpState, "own"));
		assertEquals(11, local.storedAccessTime);
	}

	@Test public void nominalReadAndTouchNeverSaveOrCreate() throws Exception {
		var store = new MemoryStore();
		var manager = new TestManager();
		manager.install(store);
		var context = new Context("session_clip");
		context.name = "clip";
		context.lastAccessTime = 12;
		store.data.put(context.contextID, context);
		var copy = SiteClipperConnector.Shuttle.readRedisContext(manager, store, context.contextID, "clip");
		assertNotSame(context, copy);
		assertNull(manager.get(context.contextID));
		assertEquals(12, copy.storedAccessTime);
		store.touch(context.contextID, 10);
		assertEquals(1, store.touches.get());
		assertEquals(10, store.lastTtl);
		assertEquals(0, store.saves.get());
		assertEquals(0, store.deletes.get());
		try {
			SiteClipperConnector.Shuttle.readRedisContext(manager, store, "session_absent", "absent");
			fail("Missing context was accepted");
		} catch (ServletException expected) {
			assertTrue(expected.getMessage().contains("isn't initialized"));
		}
		assertEquals(1, store.data.size());
	}

	@Test public void mergeKeepsAnotherRequestsCookiesAndOnlyPatchesHttpState() throws Exception {
		var store = new MemoryStore();
		var manager = new TestManager();
		manager.install(store);
		var local = new Context("session_clip");
		local.httpState = state(cookie("own", "old"));
		var snapshot = new SiteClipperCookieState(local.httpState);
		local.httpState.addCookie(cookie("own", "new"));
		var stored = new Context(local.contextID);
		stored.httpState = state(cookie("own", "old"), cookie("concurrent", "kept"));
		store.data.put(local.contextID, stored);
		SiteClipperConnector.Shuttle.mergeHttpState(manager, store, local, snapshot.delta(local.httpState));
		assertEquals("new", value(store.data.get(local.contextID).httpState, "own"));
		assertEquals("kept", value(store.data.get(local.contextID).httpState, "concurrent"));
		assertEquals(1, store.patches.get());
		assertEquals(0, store.saves.get());
		SiteClipperConnector.Shuttle.mergeHttpState(manager, store, local, new SiteClipperCookieState(local.httpState).delta(local.httpState));
		assertEquals(1, store.patches.get());
	}

	private static final class TestManager extends ContextManager {
		Map<String, Context> cache;
		void cache(Context context) { cache.put(context.contextID, context); }
		void install(ContextStore store) throws Exception {
			Field contexts = ContextManager.class.getDeclaredField("contexts");
			contexts.setAccessible(true);
			cache = new ConcurrentHashMap<String, Context>();
			contexts.set(this, cache);
			Field contextStore = ContextManager.class.getDeclaredField("contextStore");
			contextStore.setAccessible(true);
			contextStore.set(this, store);
		}
		@Override public AutoCloseable lockContext(String id) { return () -> {}; }
	}

	private static final class MemoryStore implements ContextStore {
		final Map<String, Context> data = new ConcurrentHashMap<>();
		final AtomicInteger saves = new AtomicInteger();
		final AtomicInteger patches = new AtomicInteger();
		final AtomicInteger touches = new AtomicInteger();
		final AtomicInteger deletes = new AtomicInteger();
		int lastTtl;
		@Override public Context read(String id) {
			var source = data.get(id);
			if (source == null) return null;
			var copy = new Context(id);
			copy.name = source.name;
			copy.lastAccessTime = source.lastAccessTime;
			copy.httpState = source.httpState == null ? null : state(source.httpState.getCookies());
			return copy;
		}
		@Override public void save(Context context, int ttl) { saves.incrementAndGet(); }
		@Override public void saveHttpState(Context context, int ttl) {
			patches.incrementAndGet();
			data.get(context.contextID).httpState = context.httpState;
		}
		@Override public void touch(String id, int ttl) { touches.incrementAndGet(); lastTtl = ttl; }
		@Override public void delete(String id) { deletes.incrementAndGet(); data.remove(id); }
		@Override public void deleteBySessionPrefix(String prefix) {}
		@Override public void shutdown() {}
	}
}
