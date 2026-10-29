/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Bounded server snapshots, invalidated by notifications or periodic filesystem checks.
 * <p>
 * One cache serves every project: a snapshot is read without a lock, and a missing one is loaded by its first caller
 * while the other callers of that key wait for it, the other keys loading at the same time; an expired one is served
 * while one caller refreshes it. No file is read under a lock shared by different keys: the monitor of the cache only
 * orders the publications, the evictions and the clears. A snapshot whose load started before a clear is not kept, and
 * no caller after a clear waits for a load started before it.
 */
public final class FlowSourceCache {
	private static final int CHARACTER_LIMIT = 16 * 1024 * 1024;
	private static final int MAX_TEXT_LENGTH = 2_000_000;
	private static final String FILE = "file:", DIRECTORY = "dir:";

	/** A snapshot: its stamp, its text once read (not for a large file), when it was checked, when it was last used. */
	private static final class Entry {
		final String stamp, text;
		final long checked;
		volatile long used;

		Entry(String stamp, String text, long checked) {
			this.stamp = stamp;
			this.text = text;
			this.checked = checked;
			used = checked;
		}

		int length() {
			return stamp.length() + (text == null ? 0 : text.length());
		}
	}

	@FunctionalInterface
	private interface Load {
		Entry load(Entry current) throws IOException;
	}

	private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
	/** The loads in progress, by key: removed once published or failed, so a failure is never kept. */
	private final ConcurrentHashMap<String, CompletableFuture<Entry>> loads = new ConcurrentHashMap<>();
	private final int limit;
	private final long ttlNanos;
	/** Guarded by this. */
	private long characters;
	/** Advanced by each clear, under the monitor of this cache: a load started before it is not kept. */
	private volatile long clears;

	public FlowSourceCache(int limit, long ttlMillis) {
		if (limit < 1 || ttlMillis < 0) throw new IllegalArgumentException();
		this.limit = limit;
		this.ttlNanos = TimeUnit.MILLISECONDS.toNanos(ttlMillis);
	}

	private static String path(File file) {
		return file.toPath().toAbsolutePath().normalize().toString();
	}

	/** The stamp of a file, and its text when wanted: kept from the snapshot before while the stamp is the same. */
	private static Entry snapshot(File file, Entry current, boolean text) throws IOException {
		var now = System.nanoTime();
		var canonical = file.getCanonicalPath();
		String stamp;
		try {
			var attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			stamp = canonical + "#" + attributes.lastModifiedTime().toMillis() + ":" + attributes.size();
		} catch (IOException e) {
			// as File.exists, a file that cannot be read is missing
			stamp = "missing:" + canonical;
		}
		var known = current != null && current.stamp.equals(stamp) ? current.text : null;
		return new Entry(stamp, text && known == null ? Files.readString(file.toPath(), StandardCharsets.UTF_8) : known, now);
	}

	public String fingerprint(File file) throws IOException {
		return entry(FILE + path(file), entry -> true, current -> snapshot(file, current, false)).stamp;
	}

	public String text(File file) throws IOException {
		return entry(FILE + path(file), entry -> entry.text != null, current -> snapshot(file, current, true)).text;
	}

	public String directory(File file, Supplier<String> compute) {
		try {
			return entry(DIRECTORY + path(file), entry -> true, current -> new Entry("", compute.get(), System.nanoTime())).text;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Single flight for each key: its first caller loads the snapshot, on its thread, and publishes it; the other callers
	 * of that key wait for it (they see its failure, never kept, the next caller loading it again), while the other keys
	 * load at the same time. An expired snapshot is served while one caller refreshes it.
	 */
	private Entry entry(String key, Predicate<Entry> wanted, Load load) throws IOException {
		for (;;) {
			var current = entries.get(key);
			var now = System.nanoTime();
			if (current != null && wanted.test(current) && now - current.checked < ttlNanos) {
				current.used = now;
				return current;
			}
			var loading = loads.get(key);
			if (loading == null) {
				var created = new CompletableFuture<Entry>();
				loading = loads.putIfAbsent(key, created);
				if (loading == null) {
					return lead(key, created, current, load);
				}
			}
			if (current != null && wanted.test(current)) {
				// expired: served while another caller refreshes it
				current.used = now;
				return current;
			}
			var loaded = await(loading);
			if (loaded != null && wanted.test(loaded)) {
				return loaded;
			}
		}
	}

	private Entry lead(String key, CompletableFuture<Entry> loading, Entry current, Load load) throws IOException {
		var started = clears;
		Entry loaded;
		try {
			loaded = load.load(current);
		} catch (Throwable failure) {
			loads.remove(key, loading);
			// the interruption of this caller is not a failure of the snapshot: a caller waiting for it loads it again
			if (FlowRuntimeApi.interruption(failure)) loading.complete(null);
			else loading.completeExceptionally(failure);
			throw failure;
		}
		// published before the load ends: a caller missing the load finds the snapshot
		publish(key, loaded, started);
		loads.remove(key, loading);
		loading.complete(loaded);
		return loaded;
	}

	/** @return the snapshot of a load in progress, null when its caller was interrupted: it is loaded again */
	private static Entry await(CompletableFuture<Entry> loading) throws IOException {
		try {
			return loading.get();
		} catch (ExecutionException failure) {
			var cause = failure.getCause();
			if (cause instanceof IOException io) throw io;
			if (cause instanceof RuntimeException runtime) throw runtime;
			if (cause instanceof Error error) throw error;
			throw new IOException(cause);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			var stopped = new InterruptedIOException("Interrupted while waiting for a source snapshot");
			stopped.initCause(interrupted);
			throw stopped;
		}
	}

	private void publish(String key, Entry loaded, long started) {
		var kept = loaded;
		if (loaded.text != null && loaded.text.length() > MAX_TEXT_LENGTH) {
			// a large file: its stamp only, its text read at each use
			kept = key.startsWith(FILE) ? new Entry(loaded.stamp, null, loaded.checked) : null;
		}
		if (kept == null || kept.text == null && key.startsWith(DIRECTORY)) {
			return;
		}
		synchronized (this) {
			// a clear since its load started: it may be older than the clear
			if (clears != started) {
				return;
			}
			var old = entries.put(key, kept);
			characters += kept.length() - (old == null ? 0 : old.length());
			evict();
		}
	}

	/** Must hold the monitor: the snapshots used the longest ago go, an eighth more than needed. */
	private void evict() {
		if (entries.size() <= limit && characters <= CHARACTER_LIMIT) {
			return;
		}
		var keys = new ArrayList<String>();
		var values = new ArrayList<Entry>();
		entries.forEach((key, value) -> {
			keys.add(key);
			values.add(value);
		});
		// the dates of use read once: a caller may change them during the sort
		var used = new long[values.size()];
		var order = new Integer[values.size()];
		for (int i = 0; i < used.length; i++) {
			used[i] = values.get(i).used;
			order[i] = i;
		}
		Arrays.sort(order, (a, b) -> Long.compare(used[a], used[b]));
		for (var index : order) {
			if (entries.size() <= limit - limit / 8 && characters <= CHARACTER_LIMIT - CHARACTER_LIMIT / 8) {
				break;
			}
			if (entries.remove(keys.get(index), values.get(index))) {
				characters -= values.get(index).length();
			}
		}
	}

	public synchronized void clear() {
		clears++;
		loads.clear();
		entries.clear();
		characters = 0;
	}

	/** Forgets the snapshots of the files of a directory and of its subdirectories, by their path or their canonical path. */
	public synchronized void clear(File directory) {
		clears++;
		// a load in progress may be of a file of the directory by another path: none is joined after this clear
		loads.clear();
		var prefixes = new String[] { path(directory), canonicalPath(directory) };
		for (var iterator = entries.entrySet().iterator(); iterator.hasNext();) {
			var entry = iterator.next();
			var key = entry.getKey();
			var stamp = entry.getValue().stamp;
			// a file snapshot names its canonical path in its stamp: "<path>#<date>:<length>" or "missing:<path>"
			var canonical = stamp.startsWith("missing:") ? stamp.substring(8)
					: stamp.lastIndexOf('#') > 0 ? stamp.substring(0, stamp.lastIndexOf('#')) : "";
			for (var prefix : prefixes) {
				if (under(key.substring(key.indexOf(':') + 1), prefix) || under(canonical, prefix)) {
					characters -= entry.getValue().length();
					iterator.remove();
					break;
				}
			}
		}
	}

	private static boolean under(String path, String directory) {
		return !directory.isEmpty() && (path.equals(directory) || path.startsWith(directory.endsWith(File.separator) ? directory : directory + File.separator));
	}

	private static String canonicalPath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return "";
		}
	}
}
