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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.twinsoft.convertigo.beans.flow.FlowTreeRefreshQueue.Changes;

public class FlowTreeRefreshQueueTest {
	private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
	private final List<Changes> passes = new ArrayList<>();
	private final List<Runnable> dones = new ArrayList<>();

	private void runTasks() {
		while (!tasks.isEmpty()) {
			tasks.poll().run();
		}
	}

	private FlowTreeRefreshQueue queue() {
		return new FlowTreeRefreshQueue(tasks::add, (changes, done) -> {
			passes.add(changes);
			dones.add(done);
		});
	}

	private static Changes page(String reveal) {
		return new Changes(Set.of(), false, false, true, reveal);
	}

	private static Changes flow(String name) {
		return new Changes(Set.of(name), false, false, false, "");
	}

	@Test
	public void changesArrivingDuringAPassMakeOneNextPass() {
		var queue = queue();
		queue.submit(page(""));
		runTasks();
		assertEquals(1, passes.size());
		for (var i = 0; i < 50; i++) {
			queue.submit(i % 2 == 0 ? page("") : flow("flow" + (i % 3)));
		}
		queue.submit(page("_flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte"));
		runTasks();
		assertEquals("no pass while one is running", 1, passes.size());
		dones.get(0).run();
		runTasks();
		assertEquals("one pass for all the changes received meanwhile", 2, passes.size());
		var merged = passes.get(1);
		assertTrue(merged.frontend() && merged.engineTree());
		assertEquals(Set.of("flow0", "flow1", "flow2"), merged.flowNames());
		assertEquals("_flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte", merged.revealSourcePath());
		dones.get(1).run();
		dones.get(1).run();
		runTasks();
		assertEquals("a pass ended twice starts nothing", 2, passes.size());
		assertFalse(queue.isBusy());
	}

	@Test
	public void emptyChangesStartNothing() {
		var queue = queue();
		queue.submit(new Changes(Set.of(), false, false, false, "ignored"));
		queue.submit(null);
		runTasks();
		assertTrue(passes.isEmpty());
		assertFalse(queue.isBusy());
	}

	@Test
	public void aFailedPassLetsTheNextOneRun() {
		var calls = new int[1];
		var queue = new FlowTreeRefreshQueue(tasks::add, (changes, done) -> {
			if (calls[0]++ == 0) {
				throw new IllegalStateException("projection failed");
			}
			passes.add(changes);
			done.run();
		});
		queue.submit(flow("first"));
		try {
			runTasks();
			fail("the failure is not hidden");
		} catch (IllegalStateException e) {
			// expected
		}
		queue.submit(flow("second"));
		runTasks();
		assertEquals(1, passes.size());
		assertEquals(Set.of("second"), passes.get(0).flowNames());
		assertFalse(queue.isBusy());
	}

	@Test
	public void theRevealedSourceIsTheLatestGiven() {
		var merged = page("a").merge(page("")).merge(flow("x"));
		assertEquals("a", merged.revealSourcePath());
		assertEquals("b", merged.merge(page("b")).revealSourcePath());
	}
}
