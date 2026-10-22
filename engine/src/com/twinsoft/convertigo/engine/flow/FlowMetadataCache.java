/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;

/** Bounded immutable text snapshots. Neither builders nor Rhino values are retained. */
public final class FlowMetadataCache {
	private static final class Entry {
		final String revision;
		final CompletableFuture<String> value = new CompletableFuture<>();
		Thread owner = Thread.currentThread();
		int length;
		Entry(String revision) { this.revision = revision; }
	}
	private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(16, .75f, true);
	private final int entryLimit, characterLimit;
	private long characters;
	private final LongAdder builds = new LongAdder(), hits = new LongAdder(), joins = new LongAdder();

	public FlowMetadataCache(int entryLimit, int characterLimit) {
		if (entryLimit < 1 || characterLimit < 1) throw new IllegalArgumentException("Positive cache bounds required");
		this.entryLimit = entryLimit;
		this.characterLimit = characterLimit;
	}

	public String get(String key, String revision, Callable<String> builder) throws Exception {
		Objects.requireNonNull(key); Objects.requireNonNull(revision); Objects.requireNonNull(builder);
		Entry entry;
		boolean leader = false;
		synchronized (entries) {
			entry = entries.get(key);
			if (entry == null || !entry.revision.equals(revision)) {
				if (entry != null) { entries.remove(key); characters -= entry.length; }
				while (entries.size() >= entryLimit && evictCompleted()) { }
				if (entries.size() < entryLimit) {
					entry = new Entry(revision);
					entries.put(key, entry);
				} else entry = null; // All slots build: bounded admission, no nested-call deadlock.
				leader = true;
			} else if (!entry.value.isDone()) {
				if (entry.owner == Thread.currentThread()) throw new IllegalStateException("Recursive metadata construction");
				joins.increment();
			} else hits.increment();
		}
		if (!leader) {
			try { return entry.value.get(); }
			catch (InterruptedException error) { Thread.currentThread().interrupt(); throw error; }
			catch (ExecutionException error) { return rethrow(error.getCause()); }
		}
		builds.increment();
		try {
			String value = Objects.requireNonNull(builder.call(), "Missing metadata snapshot");
			if (entry != null) {
				synchronized (entries) {
					entry.owner = null;
					entry.value.complete(value);
					if (entries.get(key) == entry) {
						if (value.length() > characterLimit) entries.remove(key);
						else { entry.length = value.length(); characters += entry.length; }
						while (characters > characterLimit && evictCompleted()) { }
					}
				}
			}
			return value;
		} catch (Throwable error) {
			if (entry != null) synchronized (entries) {
				entry.owner = null;
				if (entries.get(key) == entry) { entries.remove(key); characters -= entry.length; }
				entry.value.completeExceptionally(error);
			}
			return rethrow(error);
		}
	}

	private static String rethrow(Throwable error) throws Exception {
		if (error instanceof InterruptedException) Thread.currentThread().interrupt();
		if (error instanceof Exception exception) throw exception;
		if (error instanceof Error fatal) throw fatal;
		throw new IllegalStateException(error);
	}

	/** Must hold entries. In-flight builders stay present until completion. */
	private boolean evictCompleted() {
		for (var iterator = entries.entrySet().iterator(); iterator.hasNext();) {
			var entry = iterator.next().getValue();
			if (entry.value.isDone()) { characters -= entry.length; iterator.remove(); return true; }
		}
		return false;
	}

	public void clear() { synchronized (entries) { entries.clear(); characters = 0; } }
	public long builds() { return builds.sum(); }
	public long hits() { return hits.sum(); }
	public long joins() { return joins.sum(); }
	public int size() { synchronized (entries) { return entries.size(); } }
	public long characters() { synchronized (entries) { return characters; } }
}
