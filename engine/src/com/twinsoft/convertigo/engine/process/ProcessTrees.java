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

package com.twinsoft.convertigo.engine.process;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Stops a whole process tree with the ProcessHandle API only, never with a command such as kill, pkill or
 * taskkill: the leaves first, gracefully, then forcibly once the grace delay is over.
 * <p>
 * This class only uses the JDK: the process reaper runs it in its own small JVM.
 */
public final class ProcessTrees {

	private ProcessTrees() {
	}

	/**
	 * @return the tree of root, leaves first and root last; only the processes alive now are listed
	 */
	public static List<ProcessHandle> leavesFirst(ProcessHandle root) {
		List<ProcessHandle> order = new ArrayList<>();
		collect(root, order, new LinkedHashSet<>());
		return order;
	}

	private static void collect(ProcessHandle handle, List<ProcessHandle> order, Set<Long> seen) {
		if (!seen.add(handle.pid())) {
			return;
		}
		handle.children().forEach(child -> collect(child, order, seen));
		if (handle.isAlive()) {
			order.add(handle);
		}
	}

	/**
	 * Stops root and its descendants, see {@link #stop(ProcessHandle, Duration, Runnable, Duration)}.
	 *
	 * @return the processes still alive at the end, normally none
	 */
	public static List<ProcessHandle> stop(ProcessHandle root, Duration grace) {
		return stop(root, grace, null, Duration.ZERO);
	}

	/**
	 * Stops root and its descendants. The tree is listed before anything else, while the descendants are
	 * still attached to root. Then gentle, when given, asks the tree to stop on its own (e.g. closes the
	 * stdin a Node.js preload watches) and the tree has gentleWait to exit. Then each process still alive
	 * is asked to stop (SIGTERM on Unix, TerminateProcess on Windows), leaves first; the ones still alive
	 * after grace, with the children started in the meantime, are destroyed forcibly, leaves first again.
	 * The current process is never part of the tree.
	 *
	 * @return the processes still alive at the end, normally none
	 */
	public static List<ProcessHandle> stop(ProcessHandle root, Duration grace, Runnable gentle, Duration gentleWait) {
		ProcessHandle current = ProcessHandle.current();
		if (root == null || root.pid() == current.pid()) {
			return List.of();
		}
		List<ProcessHandle> tree = leavesFirst(root);
		tree.removeIf(handle -> handle.pid() == current.pid());
		if (gentle != null) {
			try {
				gentle.run();
			} catch (RuntimeException e) {
				// the signals below stop the tree
			}
			if (waitFor(tree, gentleWait)) {
				return List.of();
			}
		}
		for (ProcessHandle handle : tree) {
			if (handle.isAlive()) {
				handle.destroy();
			}
		}
		if (!waitFor(tree, grace)) {
			// children started during the grace delay are part of the tree too
			Set<ProcessHandle> remaining = new LinkedHashSet<>();
			if (root.isAlive()) {
				remaining.addAll(leavesFirst(root));
			}
			remaining.addAll(tree);
			remaining.removeIf(handle -> handle.pid() == current.pid() || !handle.isAlive());
			List<ProcessHandle> forced = new ArrayList<>(remaining);
			for (ProcessHandle handle : forced) {
				handle.destroyForcibly();
			}
			waitFor(forced, Duration.ofSeconds(5));
			tree = forced;
		}
		List<ProcessHandle> alive = new ArrayList<>();
		for (ProcessHandle handle : tree) {
			if (handle.isAlive()) {
				alive.add(handle);
			}
		}
		return alive;
	}

	/**
	 * @return whether all the handles exited within timeout
	 */
	public static boolean waitFor(List<ProcessHandle> handles, Duration timeout) {
		List<CompletableFuture<ProcessHandle>> exits = new ArrayList<>();
		for (ProcessHandle handle : handles) {
			exits.add(handle.onExit());
		}
		try {
			CompletableFuture.allOf(exits.toArray(new CompletableFuture[0])).get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			// timeout
		}
		for (ProcessHandle handle : handles) {
			if (handle.isAlive()) {
				return false;
			}
		}
		return true;
	}
}
