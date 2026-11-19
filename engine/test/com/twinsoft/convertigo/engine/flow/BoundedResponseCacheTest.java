package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BoundedResponseCacheTest {
	private static BoundedResponseCache<String> cache(int entries, long characters, long entry) {
		return new BoundedResponseCache<>(entries, characters, entry, String::length);
	}

	@Test
	public void evictsTheLeastRecentlyUsedBeyondTheEntryBound() {
		var cache = cache(2, 1000, 1000);
		cache.put("a", "1");
		cache.put("b", "2");
		cache.get("a");
		cache.put("c", "3");
		assertNotNull("read last, kept", cache.get("a"));
		assertNull("least recently used, evicted", cache.get("b"));
		assertNotNull(cache.get("c"));
		assertEquals(1, cache.evictions());
	}

	@Test
	public void evictsOneAtATimeBeyondTheCharacterBound() {
		var cache = cache(100, 10, 10);
		cache.put("a", "xxxx");
		cache.put("b", "xxxx");
		cache.put("c", "xxxx");
		assertNull("only the oldest leaves, not everything", cache.get("a"));
		assertNotNull(cache.get("b"));
		assertNotNull(cache.get("c"));
		assertEquals(8, cache.characters());
	}

	@Test
	public void neverKeepsAResponseAboveTheEntryBound() {
		var cache = cache(100, 100, 5);
		assertTrue(cache.put("small", "xxx"));
		assertFalse(cache.put("big", "xxxxxx"));
		assertNull(cache.get("big"));
		assertNotNull("the others stay", cache.get("small"));
		assertFalse("a key replaced by a value too big is dropped", cache.put("small", "xxxxxx"));
		assertNull(cache.get("small"));
		assertEquals(0, cache.characters());
	}

	@Test
	public void keepsTheCharacterCountExactOnReplaceRemoveAndFilter() {
		var cache = cache(100, 1000, 1000);
		cache.put("a", "xx");
		cache.put("a", "xxxx");
		cache.put("b", "xxx");
		assertEquals(7, cache.characters());
		cache.remove("a");
		assertEquals(3, cache.characters());
		cache.put("c", "x");
		assertTrue(cache.removeIf(value -> value.length() == 3));
		assertEquals(1, cache.characters());
		assertFalse(cache.containsKey("b"));
		cache.clear();
		assertTrue(cache.isEmpty());
		assertEquals(0, cache.characters());
	}
}
