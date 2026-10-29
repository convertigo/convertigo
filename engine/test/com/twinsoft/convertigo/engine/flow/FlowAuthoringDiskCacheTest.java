/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The disk cache of the authoring responses: what is written is read back as written, a damaged, truncated, foreign
 * or outdated entry is a miss (and removed), readers never see a partial entry, the folder stays bounded (the entries
 * read least recently go first), cheap responses and responses showing a secret are never written.
 */
public class FlowAuthoringDiskCacheTest {
	private static final byte[] KEY = "test-installation-key-of-32-byte".getBytes(StandardCharsets.UTF_8);
	private Path folder;
	private FlowAuthoringDiskCache cache;

	@Before
	public void create() throws Exception {
		folder = Files.createTempDirectory("authoring-cache-");
		cache = new FlowAuthoringDiskCache(folder, FlowAuthoringDiskCache.DEFAULT_MAX_BYTES, KEY);
	}

	@After
	public void remove() throws Exception {
		FlowAuthoringDiskCache.flush();
		FlowAuthoringDiskCache.secrets = List::of;
		FileUtils.deleteDirectory(folder.toFile());
	}

	private static String key(int i) {
		var bytes = new byte[32];
		bytes[0] = (byte) i;
		bytes[31] = (byte) (i >> 8);
		return HexFormat.of().formatHex(bytes);
	}

	private Path file(String key) {
		return folder.resolve(key + ".arc");
	}

	@Test
	public void readsBackWhatItWrote() {
		var parts = Map.of("response", "{\"ok\":true,\"label\":\"é \\u2603 ☃\"}", "virtualDefinitions", "{\"vd-1\":\"{}\"}");
		cache.writeNow(key(1), parts);
		assertEquals(parts, cache.read(key(1)));
		assertTrue(cache.contains(key(1)));
		assertNull("another key", cache.read(key(2)));
		assertFalse(cache.contains(key(2)));
		assertNull("not a key", cache.read("../" + key(1)));
	}

	@Test
	public void aDamagedEntryIsAMissAndIsRemoved() throws Exception {
		cache.writeNow(key(1), Map.of("response", "{\"ok\":true}".repeat(100)));
		var data = Files.readAllBytes(file(key(1)));
		data[data.length / 2] ^= 0x40;
		Files.write(file(key(1)), data);
		var invalid = FlowAuthoringDiskCache.statistics.invalid.get();
		assertNull(cache.read(key(1)));
		assertEquals(invalid + 1, FlowAuthoringDiskCache.statistics.invalid.get());
		assertFalse("removed", Files.exists(file(key(1))));
	}

	@Test
	public void aTruncatedOrEmptyEntryIsAMiss() throws Exception {
		cache.writeNow(key(1), Map.of("response", "{\"ok\":true}"));
		var data = Files.readAllBytes(file(key(1)));
		Files.write(file(key(1)), java.util.Arrays.copyOf(data, data.length - 7));
		assertNull(cache.read(key(1)));
		Files.write(file(key(2)), new byte[0]);
		assertNull(cache.read(key(2)));
	}

	@Test
	public void anEntryOfAnotherKeyOrInstallationIsAMiss() throws Exception {
		cache.writeNow(key(1), Map.of("response", "{\"ok\":1}"));
		Files.copy(file(key(1)), file(key(2)));
		assertNull("an entry renamed to another key", cache.read(key(2)));
		var other = new FlowAuthoringDiskCache(folder, FlowAuthoringDiskCache.DEFAULT_MAX_BYTES,
				"another-installation-key-32-byte".getBytes(StandardCharsets.UTF_8));
		assertNull("signed by another installation", other.read(key(1)));
		assertFalse(Files.exists(file(key(1))));
	}

	@Test
	public void anEntryOfAnotherFormatIsRemovedByTheScan() throws Exception {
		cache.writeNow(key(1), Map.of("response", "{}"));
		var data = Files.readAllBytes(file(key(1)));
		data[7] = (byte) (FlowAuthoringDiskCache.FORMAT + 1);
		Files.write(file(key(1)), data);
		assertTrue(FlowAuthoringDiskCache.outdated(file(key(1))));
		cache.evict();
		assertFalse(Files.exists(file(key(1))));
	}

	@Test
	public void theFolderStaysBoundedTheLeastRecentlyReadFirst() throws Exception {
		var text = new StringBuilder();
		var random = new java.util.Random(7);
		for (var i = 0; i < 20_000; i++) {
			text.append((char) ('a' + random.nextInt(26)));
		}
		var small = new FlowAuthoringDiskCache(folder, 64 * 1024, KEY);
		var now = System.currentTimeMillis();
		for (var i = 0; i < 10; i++) {
			small.writeNow(key(i), Map.of("response", text.toString() + i));
			// written (or read) longer ago for the lower ones
			Files.setLastModifiedTime(file(key(i)), FileTime.fromMillis(now - TimeUnit.HOURS.toMillis(10 - i)));
		}
		var stale = folder.resolve("." + key(99) + ".1-1.tmp");
		Files.writeString(stale, "partial");
		Files.setLastModifiedTime(stale, FileTime.fromMillis(now - TimeUnit.HOURS.toMillis(2)));
		small.evict();
		assertTrue("bounded: " + small.size(), small.size() <= 64 * 1024 / 4 * 3);
		assertNotNull("the most recent kept", small.read(key(9)));
		assertNull("the oldest removed", small.read(key(0)));
		assertFalse("a temporary file left by a crash removed", Files.exists(stale));
	}

	@Test
	public void writesInTheBackgroundOnlyWhatIsWorthReading() throws Exception {
		cache.write(key(1), () -> Map.of("response", "{\"cheap\":true}"), TimeUnit.MILLISECONDS.toNanos(1));
		cache.write(key(2), () -> Map.of("response", "{\"expensive\":true}"), TimeUnit.SECONDS.toNanos(2));
		cache.write(key(3), () -> null, TimeUnit.SECONDS.toNanos(2));
		FlowAuthoringDiskCache.flush();
		assertFalse("computed in 1 ms", cache.contains(key(1)));
		assertEquals("{\"expensive\":true}", cache.read(key(2)).get("response"));
		assertFalse("nothing to write", cache.contains(key(3)));
	}

	@Test
	public void neverWritesAResponseShowingASecret() throws Exception {
		FlowAuthoringDiskCache.secrets = () -> List.of("s3cr3t-value", "line\\nbreak");
		var refused = FlowAuthoringDiskCache.statistics.refused.get();
		cache.writeNow(key(1), Map.of("response", "{\"config\":{\"password\":\"s3cr3t-value\"}}"));
		cache.writeNow(key(2), Map.of("response", "{\"ok\":true}", "virtualDefinitions", "{\"x\":\"line\\nbreak\"}"));
		cache.writeNow(key(3), Map.of("response", "{\"config\":{\"password\":\"${app.smtp.password.secret=}\"}}"));
		assertFalse(cache.contains(key(1)));
		assertFalse("in any part", cache.contains(key(2)));
		assertTrue("a reference to a secret is not its value", cache.contains(key(3)));
		assertEquals(refused + 2, FlowAuthoringDiskCache.statistics.refused.get());
	}

	@Test
	public void theCacheClearActionRemovesEveryEntry() throws Exception {
		cache.writeNow(key(1), Map.of("response", "{}"));
		cache.writeNow(key(2), Map.of("response", "{}"));
		cache.clear();
		assertFalse(cache.contains(key(1)));
		assertFalse(cache.contains(key(2)));
		assertEquals(0, cache.size());
	}

	@Test
	public void readersNeverSeeAPartialEntry() throws Exception {
		var a = "{\"v\":\"" + "a".repeat(400_000) + "\"}";
		var b = "{\"v\":\"" + "b".repeat(300_000) + "\"}";
		var pool = Executors.newFixedThreadPool(3);
		var stop = new AtomicBoolean();
		var reads = new AtomicInteger();
		var errors = new ArrayList<String>();
		try {
			for (var writer = 0; writer < 2; writer++) {
				var text = writer == 0 ? a : b;
				pool.submit(() -> {
					while (!stop.get()) {
						cache.writeNow(key(1), Map.of("response", text));
					}
				});
			}
			var reader = pool.submit(() -> {
				for (var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2); System.nanoTime() < deadline;) {
					var parts = cache.read(key(1));
					if (parts != null) {
						reads.incrementAndGet();
						var text = parts.get("response");
						if (!a.equals(text) && !b.equals(text)) {
							synchronized (errors) {
								errors.add(text == null ? "null" : text.substring(0, Math.min(40, text.length())));
							}
						}
					}
				}
			});
			reader.get(30, TimeUnit.SECONDS);
		} finally {
			stop.set(true);
			pool.shutdown();
			pool.awaitTermination(30, TimeUnit.SECONDS);
		}
		assertTrue("entries read: " + reads, reads.get() > 0);
		assertTrue("partial entries read: " + errors, errors.isEmpty());
	}
}
