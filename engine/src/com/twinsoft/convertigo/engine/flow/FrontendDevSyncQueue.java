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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.flow;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Synchronizations run on one daemon thread, started on demand and stopped after 30 s without work, in the order they
 * are asked: the sources written for a key while it synchronizes are coalesced into its next synchronization. The
 * caller never waits for them: a synchronization of the development viewer of a frontend takes the frontend authoring
 * lock of the server, which the Studio holds while it describes its tree.
 *
 * @param <K> what a synchronization is for (a FlowEngine)
 */
final class FrontendDevSyncQueue<K> {
	private final Map<K, Set<String>> pending = new ConcurrentHashMap<>();
	private final ThreadPoolExecutor executor;
	private final BiConsumer<K, Set<String>> sync;
	private final BiConsumer<K, Throwable> failure;

	FrontendDevSyncQueue(String threadName, BiConsumer<K, Set<String>> sync, BiConsumer<K, Throwable> failure) {
		this.sync = sync;
		this.failure = failure;
		executor = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), runnable -> {
			var thread = new Thread(runnable, threadName);
			thread.setDaemon(true);
			return thread;
		});
	}

	/** Asks the synchronization of a key after a write of one of its sources; returns at once. */
	void submit(K key, String sourcePath) {
		pending.compute(key, (ignored, paths) -> {
			var next = paths == null ? new LinkedHashSet<String>() : paths;
			next.add(sourcePath == null ? "" : sourcePath);
			return next;
		});
		executor.execute(() -> {
			var paths = pending.remove(key);
			if (paths == null) {
				return; // synchronized by an earlier task, with this source
			}
			try {
				sync.accept(key, paths);
			} catch (RuntimeException | LinkageError e) {
				failure.accept(key, e);
			}
		});
	}
}
