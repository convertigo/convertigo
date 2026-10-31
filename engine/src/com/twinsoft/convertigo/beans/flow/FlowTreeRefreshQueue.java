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

package com.twinsoft.convertigo.beans.flow;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Coalesces the refreshes of the Flow virtual tree of one project. An agent writes its sources many times a minute,
 * and each write used to queue a complete reload of the tree on the UI thread: on a large project these reloads ran
 * one after the other for minutes, and every call waiting for the UI thread with them. Here one pass runs at a time;
 * the changes arriving meanwhile are merged into a single next pass.
 */
public final class FlowTreeRefreshQueue {

	/** What changed in the sources of a project since the last pass. */
	public record Changes(Set<String> flowNames, boolean runtime, boolean catalog, boolean frontend, String revealSourcePath) {

		public Changes {
			flowNames = Collections.unmodifiableSet(new TreeSet<>(flowNames == null ? Set.of() : flowNames));
			revealSourcePath = revealSourcePath == null ? "" : revealSourcePath;
		}

		/** @return whether the whole tree of the Flow engine is projected again, not only the trees of Flows */
		public boolean engineTree() {
			return runtime || catalog || frontend;
		}

		public boolean isEmpty() {
			return !engineTree() && flowNames.isEmpty();
		}

		/** @return the changes of both, the source to reveal being the latest one given */
		public Changes merge(Changes next) {
			var names = new TreeSet<>(flowNames);
			names.addAll(next.flowNames);
			return new Changes(names, runtime || next.runtime, catalog || next.catalog, frontend || next.frontend,
					next.revealSourcePath.isBlank() ? revealSourcePath : next.revealSourcePath);
		}
	}

	private final Executor background;
	private final BiConsumer<Changes, Runnable> pass;
	private Changes pending;
	private boolean running;

	/**
	 * @param background where a pass starts, never the UI thread
	 * @param pass runs a pass of the changes, then calls its runnable once when the pass is over, including the part
	 *        it hands to the UI thread; the next pass starts then
	 */
	public FlowTreeRefreshQueue(Executor background, BiConsumer<Changes, Runnable> pass) {
		this.background = background;
		this.pass = pass;
	}

	public void submit(Changes changes) {
		if (changes == null || changes.isEmpty()) {
			return;
		}
		synchronized (this) {
			pending = pending == null ? changes : pending.merge(changes);
			if (running) {
				return;
			}
			running = true;
		}
		background.execute(this::next);
	}

	/** @return whether a pass runs or waits (for tests and diagnostics) */
	public synchronized boolean isBusy() {
		return running;
	}

	private void next() {
		Changes changes;
		synchronized (this) {
			changes = pending;
			pending = null;
			if (changes == null) {
				running = false;
				return;
			}
		}
		var over = new AtomicBoolean();
		Runnable done = () -> {
			if (over.compareAndSet(false, true)) {
				background.execute(this::next);
			}
		};
		try {
			pass.accept(changes, done);
		} catch (RuntimeException | Error e) {
			done.run();
			throw e;
		}
	}
}
