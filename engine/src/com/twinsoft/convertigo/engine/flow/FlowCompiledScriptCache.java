/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;

/** Shared code only: evaluated exports and scopes never enter this cache. */
public final class FlowCompiledScriptCache {
	private static final int CHARACTER_LIMIT = 16 * 1024 * 1024;
	private record Key(String name, String source, int language, boolean interpreted) { }
	private final LinkedHashMap<Key, CompletableFuture<Script>> entries = new LinkedHashMap<>(16, .75f, true);
	private final int limit;
	private long characters;
	private final LongAdder compilations = new LongAdder();

	public FlowCompiledScriptCache(int limit) {
		if (limit < 1) throw new IllegalArgumentException();
		this.limit = limit;
	}

	public Script get(Context cx, String source, String name) {
		var key = new Key(name, source, cx.getLanguageVersion(), cx.isInterpretedMode());
		CompletableFuture<Script> pending;
		boolean leader = false;
		synchronized (entries) {
			pending = entries.get(key);
			if (pending == null) {
				while (entries.size() >= limit || characters + source.length() > CHARACTER_LIMIT) {
					var it = entries.entrySet().iterator();
					boolean removed = false;
					while (it.hasNext()) {
						var item = it.next();
						if (item.getValue().isDone()) {
							characters -= item.getKey().source.length();
							it.remove();
							removed = true;
							break;
						}
					}
					if (!removed) break;
				}
				pending = new CompletableFuture<>();
				// Do not retain oversized code or wait for an unrelated in-flight slot.
				if (entries.size() < limit && source.length() <= 2_000_000 && characters + source.length() <= CHARACTER_LIMIT) {
					entries.put(key, pending);
					characters += source.length();
				}
				leader = true;
			}
		}
		if (leader) {
			try {
				compilations.increment();
				pending.complete(cx.compileString(source, name, 1, null));
			} catch (Throwable failure) {
				pending.completeExceptionally(failure);
				synchronized (entries) {
					if (entries.remove(key, pending)) characters -= source.length();
				}
			}
		}
		try {
			return pending.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw Context.throwAsScriptRuntimeEx(e);
		} catch (ExecutionException e) {
			throw Context.throwAsScriptRuntimeEx(e.getCause());
		}
	}

	public void clear() {
		synchronized (entries) {
			entries.clear();
			characters = 0;
		}
	}

	public long compilations() { return compilations.sum(); }
	public int size() { synchronized (entries) { return entries.size(); } }
}
