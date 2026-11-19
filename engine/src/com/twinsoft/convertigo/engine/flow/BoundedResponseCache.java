/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.flow;

import java.util.LinkedHashMap;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Responses kept in memory, bounded both in entries and in characters: beyond either bound the least recently used ones
 * go first, one at a time, instead of all at once. A response larger than the entry bound is never kept (the disk cache
 * keeps it). Thread safe.
 */
final class BoundedResponseCache<V> {
	private final int maxEntries;
	private final long maxCharacters;
	private final long maxEntryCharacters;
	private final ToLongFunction<V> characters;
	private final LinkedHashMap<String, V> entries = new LinkedHashMap<>(16, 0.75f, true);
	private long total;
	private long evictions;

	BoundedResponseCache(int maxEntries, long maxCharacters, long maxEntryCharacters, ToLongFunction<V> characters) {
		this.maxEntries = maxEntries;
		this.maxCharacters = maxCharacters;
		this.maxEntryCharacters = maxEntryCharacters;
		this.characters = characters;
	}

	synchronized V get(String key) {
		return entries.get(key);
	}

	/** @return whether the response is kept: false when it is larger than the entry bound */
	synchronized boolean put(String key, V value) {
		var size = characters.applyAsLong(value);
		if (size > maxEntryCharacters) {
			remove(key);
			return false;
		}
		var previous = entries.put(key, value);
		if (previous != null) {
			total -= characters.applyAsLong(previous);
		}
		total += size;
		var eldest = entries.entrySet().iterator();
		while ((entries.size() > maxEntries || total > maxCharacters) && eldest.hasNext()) {
			var entry = eldest.next();
			if (entry.getKey().equals(key)) {
				continue;
			}
			total -= characters.applyAsLong(entry.getValue());
			eldest.remove();
			evictions++;
		}
		return true;
	}

	synchronized void remove(String key) {
		var removed = entries.remove(key);
		if (removed != null) {
			total -= characters.applyAsLong(removed);
		}
	}

	synchronized boolean removeIf(Predicate<V> filter) {
		var changed = false;
		for (var iterator = entries.values().iterator(); iterator.hasNext();) {
			var value = iterator.next();
			if (filter.test(value)) {
				total -= characters.applyAsLong(value);
				iterator.remove();
				changed = true;
			}
		}
		return changed;
	}

	synchronized boolean containsKey(String key) {
		return entries.containsKey(key);
	}

	synchronized void clear() {
		entries.clear();
		total = 0;
	}

	synchronized int size() {
		return entries.size();
	}

	synchronized boolean isEmpty() {
		return entries.isEmpty();
	}

	synchronized long characters() {
		return total;
	}

	synchronized long evictions() {
		return evictions;
	}
}
