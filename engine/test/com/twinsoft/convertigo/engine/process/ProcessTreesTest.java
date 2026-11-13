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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProcessTreesTest {
	private final ProcessTestSupport support = new ProcessTestSupport();

	@Before
	public void unix() {
		ProcessTestSupport.assumeUnix();
	}

	@After
	public void cleanup() throws Exception {
		support.cleanup();
	}

	/** root sh -> (sh -> sleep, sleep) */
	private Process tree() throws Exception {
		Process root = support.start(new ProcessBuilder("sh", "-c", "sh -c 'sleep 600 & wait' & sleep 600 & wait"));
		assertTrue(ProcessTestSupport.waitFor(() -> root.toHandle().descendants().count() >= 3, Duration.ofSeconds(5)));
		return root;
	}

	@Test
	public void listsLeavesFirstAndRootLast() throws Exception {
		Process root = tree();
		List<ProcessHandle> order = ProcessTrees.leavesFirst(root.toHandle());
		assertEquals(4, order.size());
		assertEquals(root.pid(), order.get(order.size() - 1).pid());
		for (int i = 0; i < order.size(); i++) {
			for (ProcessHandle descendant : order.get(i).descendants().toList()) {
				assertTrue("a descendant comes before its ancestor", order.indexOf(descendant) < i);
			}
		}
	}

	@Test
	public void stopsTheWholeTree() throws Exception {
		Process root = tree();
		List<ProcessHandle> tree = ProcessTrees.leavesFirst(root.toHandle());
		List<ProcessHandle> alive = ProcessTrees.stop(root.toHandle(), Duration.ofSeconds(5));
		assertTrue(alive.isEmpty());
		for (ProcessHandle handle : tree) {
			assertFalse(handle.isAlive());
		}
	}

	@Test
	public void destroysForciblyAfterTheGrace() throws Exception {
		// ignores SIGTERM, its sleep too
		Process root = support.start(new ProcessBuilder("sh", "-c", "trap '' TERM; while true; do sleep 1; done"));
		Thread.sleep(300);
		long started = System.nanoTime();
		List<ProcessHandle> alive = ProcessTrees.stop(root.toHandle(), Duration.ofMillis(500));
		assertTrue(alive.isEmpty());
		assertFalse(root.isAlive());
		assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(5)) < 0);
	}

	@Test
	public void asksGentlyFirst() throws Exception {
		// cat exits by itself (code 0) at the end of its stdin, before any signal
		Process cat = support.start(new ProcessBuilder("cat"));
		AtomicBoolean asked = new AtomicBoolean();
		List<ProcessHandle> alive = ProcessTrees.stop(cat.toHandle(), Duration.ofSeconds(5), () -> {
			asked.set(true);
			try {
				cat.getOutputStream().close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, Duration.ofSeconds(3));
		assertTrue(alive.isEmpty());
		assertTrue(asked.get());
		assertEquals(0, cat.waitFor());
	}

	@Test
	public void neverStopsTheCurrentProcess() {
		assertTrue(ProcessTrees.stop(ProcessHandle.current(), Duration.ofMillis(100)).isEmpty());
		assertTrue(ProcessHandle.current().isAlive());
	}
}
