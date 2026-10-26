/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Bounded server snapshots, invalidated by notifications or periodic filesystem checks. */
public final class FlowSourceCache {
	private static final int CHARACTER_LIMIT = 16 * 1024 * 1024;
	private static final int MAX_TEXT_LENGTH = 2_000_000;
	private record Entry(String stamp, String text, long checked) { }
	private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(16, .75f, true);
	private final int limit;
	private final long ttlNanos;
	private long characters;

	public FlowSourceCache(int limit, long ttlMillis) {
		if (limit < 1 || ttlMillis < 0) throw new IllegalArgumentException();
		this.limit = limit;
		this.ttlNanos = TimeUnit.MILLISECONDS.toNanos(ttlMillis);
	}

	private static String path(File file) {
		return file.toPath().toAbsolutePath().normalize().toString();
	}

	private Entry snapshot(File file, String key) throws IOException {
		var now = System.nanoTime();
		var old = entries.get(key);
		if (old != null && now - old.checked < ttlNanos) return old;
		var canonical = file.getCanonicalPath();
		var stamp = file.exists() ? canonical + "#" + file.lastModified() + ":" + file.length() : "missing:" + canonical;
		var fresh = new Entry(stamp, old != null && old.stamp.equals(stamp) ? old.text : null, now);
		put(key, fresh);
		return fresh;
	}

	public synchronized String fingerprint(File file) throws IOException {
		return snapshot(file, "file:" + path(file)).stamp;
	}

	public synchronized String text(File file) throws IOException {
		var key = "file:" + path(file);
		var snapshot = snapshot(file, key);
		if (snapshot.text != null) return snapshot.text;
		var text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		if (text.length() <= MAX_TEXT_LENGTH) put(key, new Entry(snapshot.stamp, text, snapshot.checked));
		return text;
	}

	public synchronized String directory(File file, Supplier<String> compute) {
		var key = "dir:" + path(file);
		var now = System.nanoTime();
		var old = entries.get(key);
		if (old != null && now - old.checked < ttlNanos) return old.text;
		var text = compute.get();
		if (text != null && text.length() <= MAX_TEXT_LENGTH) put(key, new Entry("", text, now));
		return text;
	}

	private void put(String key, Entry value) {
		var old = entries.put(key, value);
		if (old != null) characters -= length(old);
		characters += length(value);
		while (entries.size() > limit || characters > CHARACTER_LIMIT) {
			characters -= length(entries.remove(entries.keySet().iterator().next()));
		}
	}

	private static int length(Entry entry) {
		return entry.stamp.length() + (entry.text == null ? 0 : entry.text.length());
	}

	public synchronized void clear() {
		entries.clear();
		characters = 0;
	}
}
