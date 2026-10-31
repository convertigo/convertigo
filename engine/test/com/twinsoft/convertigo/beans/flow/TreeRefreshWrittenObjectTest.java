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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;

import com.twinsoft.convertigo.beans.flow.FlowTreeRefreshQueue.Changes;

/** The reloads asked by the writers of objects (a Flow saved, objects added, sources written outside the workspace). */
public class TreeRefreshWrittenObjectTest {
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

	private static Changes written(String flowName, boolean project, boolean frontend, boolean resources,
			Consumer<Runnable> around) {
		return new Changes(Set.of(), false, false, frontend, "", flowName == null ? Set.of() : Set.of(flowName), project,
				resources, around);
	}

	private static Consumer<Runnable> hook(List<String> calls, String name) {
		return reload -> {
			calls.add(name + ":before");
			reload.run();
			calls.add(name + ":after");
		};
	}

	@Test
	public void writtenObjectsMergeIntoOneNextPassAroundTheLatestHook() {
		var calls = new ArrayList<String>();
		var first = hook(calls, "first");
		var latest = hook(calls, "latest");
		var queue = queue();
		queue.submit(written("Promoted", false, false, false, first));
		runTasks();
		assertEquals(1, passes.size());
		assertSame(first, passes.get(0).aroundReload());
		queue.submit(written("Synced", false, false, false, latest));
		queue.submit(new Changes(Set.of("Edited"), false, false, false, ""));
		queue.submit(written(null, false, true, true, null));
		queue.submit(written(null, true, false, false, null));
		runTasks();
		assertEquals("no pass while one is running", 1, passes.size());
		dones.get(0).run();
		runTasks();
		assertEquals(2, passes.size());
		var merged = passes.get(1);
		assertEquals(Set.of("Synced"), merged.writtenFlowNames());
		assertEquals(Set.of("Edited"), merged.flowNames());
		assertTrue(merged.writtenProject() && merged.resources() && merged.frontend());
		assertSame("a change without hook keeps the hook of a writer", latest, merged.aroundReload());
		dones.get(1).run();
		runTasks();
		assertFalse(queue.isBusy());
	}

	@Test
	public void writtenObjectsInvalidateNoSourceCache() {
		var flow = written("Promoted", false, false, false, null);
		assertFalse(flow.isEmpty());
		assertFalse("a saved Flow invalidates nothing, its writer did", flow.sourcesChanged());
		assertFalse(flow.engineTree());
		var project = written(null, true, false, false, null);
		assertFalse(project.isEmpty());
		assertFalse(project.sourcesChanged());
		assertFalse("resources refreshed alone make a pass", written(null, false, false, true, null).isEmpty());
		assertTrue(written(null, false, false, false, reload -> reload.run()).isEmpty());
		var frontend = written(null, false, true, false, null);
		assertTrue(frontend.sourcesChanged() && frontend.engineTree());
		var removed = new Changes(Set.of(), false, true, false, "", Set.of(), false, true, null);
		assertTrue("sources removed invalidate the caches of the catalog", removed.sourcesChanged() && removed.engineTree());
		var sources = new Changes(Set.of("Edited"), false, false, false, "");
		assertTrue(sources.writtenFlowNames().isEmpty());
		assertFalse(sources.writtenProject() || sources.resources());
		assertNull(sources.aroundReload());
	}

	@Test
	public void theHookRunsTheReloadOnce() {
		var calls = new ArrayList<String>();
		var failure = FlowTreeRefreshQueue.reloadAround(reload -> {
			calls.add("before");
			reload.run();
			reload.run();
			calls.add("after");
		}, () -> calls.add("reload"));
		assertNull(failure);
		assertEquals(List.of("before", "reload", "after"), calls);
		calls.clear();
		assertNull(FlowTreeRefreshQueue.reloadAround(null, () -> calls.add("reload")));
		assertEquals(List.of("reload"), calls);
	}

	@Test
	public void aFailingHookStillReloads() {
		var calls = new ArrayList<String>();
		var before = FlowTreeRefreshQueue.reloadAround(reload -> {
			throw new IllegalStateException("lock helper failed");
		}, () -> calls.add("reload"));
		assertEquals("lock helper failed", before.getMessage());
		assertEquals(List.of("reload"), calls);
		calls.clear();
		var after = FlowTreeRefreshQueue.reloadAround(reload -> {
			reload.run();
			throw new IllegalStateException("note failed");
		}, () -> calls.add("reload"));
		assertEquals("note failed", after.getMessage());
		assertEquals("a reload already run is not run again", List.of("reload"), calls);
		calls.clear();
		assertNull(FlowTreeRefreshQueue.reloadAround(reload -> calls.add("forgot"), () -> calls.add("reload")));
		assertEquals(List.of("forgot", "reload"), calls);
		calls.clear();
		var reloadFailed = FlowTreeRefreshQueue.reloadAround(reload -> reload.run(), () -> {
			calls.add("reload");
			throw new IllegalStateException("tree failed");
		});
		assertEquals("tree failed", reloadFailed.getMessage());
		assertEquals(List.of("reload"), calls);
	}
}
