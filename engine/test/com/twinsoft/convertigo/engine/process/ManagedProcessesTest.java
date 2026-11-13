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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ManagedProcessesTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	private final ProcessTestSupport support = new ProcessTestSupport();
	private ManagedProcesses service;
	private File workspace;

	@Before
	public void setUp() throws Exception {
		ProcessTestSupport.assumeUnix();
		workspace = temporary.newFolder("workspace");
		service = new ManagedProcesses();
		// the reaper has its own tests
		service.setReaperMode("off");
		service.open(workspace);
	}

	@After
	public void cleanup() throws Exception {
		if (service != null) {
			service.stopAll();
		}
		support.cleanup();
	}

	private List<ProcessRegistry.Entry> entries() {
		return ManagedProcesses.registry(workspace).entries();
	}

	private ManagedProcess shellTree(String function) throws IOException {
		return service.start(ManagedProcessSpec.of(function).command("sh", "-c", "sleep 600 & sleep 600 & wait"));
	}

	@Test
	public void recordsTheProcessAndStopsItsWholeTree() throws Exception {
		ManagedProcess process = shellTree("ngx-serve:demo");
		assertTrue(ProcessTestSupport.waitFor(() -> process.toHandle().descendants().count() == 2, Duration.ofSeconds(5)));
		List<ProcessHandle> tree = ProcessTrees.leavesFirst(process.toHandle());
		assertEquals(1, entries().size());
		assertEquals("ngx-serve:demo", entries().get(0).getFunction());
		assertEquals(List.of(process), service.list("ngx-serve:demo"));

		assertTrue(process.stop().isEmpty());
		for (ProcessHandle handle : tree) {
			assertFalse(handle.isAlive());
		}
		assertEquals(ManagedProcess.State.STOPPED, process.getState());
		assertTrue("the entry is removed", entries().isEmpty());
		assertTrue(service.list().isEmpty());
		assertTrue("idempotent", process.stop().isEmpty());
	}

	@Test
	public void forgetsAProcessThatExits() throws Exception {
		ManagedProcess process = service.start(ManagedProcessSpec.of("short").command("sleep", "0.2"));
		assertEquals(0, process.waitFor());
		assertTrue(ProcessTestSupport.waitFor(() -> service.list().isEmpty() && entries().isEmpty(), Duration.ofSeconds(2)));
		assertEquals(ManagedProcess.State.EXITED, process.getState());
	}

	@Test
	public void stopsByFunction() throws Exception {
		ManagedProcess serve = shellTree("ngx-serve:demo");
		ManagedProcess build = shellTree("ngx-build:demo");
		ManagedProcess other = shellTree("ngx-serve:other");
		assertTrue(service.stop(p -> p.getFunction().endsWith(":demo")).isEmpty());
		assertFalse(serve.isAlive());
		assertFalse(build.isAlive());
		assertTrue(other.isAlive());
		assertEquals(List.of(other), service.list());
	}

	@Test
	public void stopAllRefusesNewProcessesUntilOpen() throws Exception {
		ManagedProcess first = shellTree("a");
		ManagedProcess second = shellTree("b");
		long started = System.nanoTime();
		assertTrue(service.stopAll().isEmpty());
		assertTrue("stopped at the same time", Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(4)) < 0);
		assertFalse(first.isAlive());
		assertFalse(second.isAlive());
		assertFalse(service.isAccepting());
		try {
			shellTree("c");
			fail("no process starts while the engine stops");
		} catch (IOException e) {
			assertTrue(e.getMessage().contains("stopping"));
		}
		service.open(workspace);
		assertTrue(shellTree("c").isAlive());
	}

	@Test
	public void theCleanupNeverTouchesTheRunningProcesses() throws Exception {
		ManagedProcess process = shellTree("running");
		Thread.sleep(200);
		service.cleanup(workspace, null);
		service.open(workspace);
		assertTrue(process.isAlive());
		assertEquals(1, entries().size());
	}

	@Test
	public void withoutRegistryForAShortProcess() throws Exception {
		ManagedProcess process = service.start(ManagedProcessSpec.of("version").record(false).command("sleep", "600"));
		assertTrue(entries().isEmpty());
		assertTrue(process.stop().isEmpty());
	}

	@Test
	public void readsTheOutput() throws Exception {
		List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
		ManagedProcess process = service.start(ManagedProcessSpec.of("echo").redirectErrorStream(true)
				.command("sh", "-c", "for i in 1 2 3 4 5; do echo line $i; done").output(lines::add, 2));
		process.waitFor();
		assertTrue(ProcessTestSupport.waitFor(() -> lines.size() == 5, Duration.ofSeconds(2)));
		assertEquals(List.of("line 4", "line 5"), process.getLastOutput());
	}

	/** The engine is killed: the next session stops what it left, and spares a PID now used by another process. */
	@Test
	public void theNextSessionStopsTheOrphansOfAKilledEngine() throws Exception {
		Process engine = support.java(ManagedProcessLauncher.class, workspace.getAbsolutePath(), "off", "shell", "shell");
		BufferedReader reader = ProcessTestSupport.reader(engine);
		long orphan = Long.parseLong(ProcessTestSupport.readLine(reader, "shell ", Duration.ofSeconds(20)));
		long spared = Long.parseLong(ProcessTestSupport.readLine(reader, "shell ", Duration.ofSeconds(5)));
		assertNotNull(ProcessTestSupport.readLine(reader, "ready ", Duration.ofSeconds(10)));
		support.own(orphan);
		support.own(spared);
		List<ProcessHandle> orphanTree = ProcessTrees.leavesFirst(ProcessHandle.of(orphan).orElseThrow());
		assertEquals(3, orphanTree.size());

		engine.destroyForcibly().waitFor();
		Thread.sleep(300);
		assertTrue("a shell outlives a killed engine", ProcessTestSupport.alive(orphan));
		// as if the PID of the second one now belonged to another process
		File entry = ManagedProcesses.registry(workspace).getDir().resolve("test-shell-" + spared + ".properties").toFile();
		ProcessRegistry.Entry recorded = ManagedProcesses.registry(workspace).entries().stream().filter(e -> e.getPid() == spared).findFirst().orElseThrow();
		ProcessTestSupport.rewrite(entry, Map.of("start", Long.toString(recorded.getStart() - 1000)));

		ManagedProcesses next = new ManagedProcesses();
		next.setReaperMode("off");
		List<ProcessRegistry.Cleanup> result = next.open(workspace);
		assertEquals(2, result.size());
		for (ProcessHandle handle : orphanTree) {
			assertTrue(ProcessTestSupport.waitDead(handle.pid(), Duration.ofSeconds(2)));
		}
		assertTrue("the foreign process with the same PID is spared", ProcessTestSupport.alive(spared));
		assertTrue(entries().isEmpty());
	}

	/** Node.js stops when the engine is killed, launched directly or in a tree (NODE_OPTIONS). */
	@Test
	public void nodeStopsWhenTheEngineIsKilled() throws Exception {
		File node = ProcessTestSupport.node();
		Process engine = support.java(ManagedProcessLauncher.class, workspace.getAbsolutePath(), "off", "node", "npm");
		BufferedReader reader = ProcessTestSupport.reader(engine);
		long direct = Long.parseLong(ProcessTestSupport.readLine(reader, "node ", Duration.ofSeconds(20)));
		long shell = Long.parseLong(ProcessTestSupport.readLine(reader, "npm ", Duration.ofSeconds(5)));
		assertNotNull(ProcessTestSupport.readLine(reader, "ready ", Duration.ofSeconds(10)));
		support.own(direct);
		support.own(shell);
		long tree = ProcessHandle.of(shell).orElseThrow().children().findFirst().orElseThrow().pid();
		assertTrue(ProcessHandle.of(direct).orElseThrow().info().commandLine().orElse("").contains(NodePreload.FILE_NAME));

		engine.destroyForcibly().waitFor();
		assertTrue("node with --require", ProcessTestSupport.waitDead(direct, Duration.ofSeconds(3)));
		assertTrue("node with NODE_OPTIONS", ProcessTestSupport.waitDead(tree, Duration.ofSeconds(3)));
		assertTrue(node.exists());
	}

	/** A stop first closes the stdin the preload watches: Node.js exits by itself, code 0. */
	@Test
	public void nodeStopsGracefully() throws Exception {
		File node = ProcessTestSupport.node();
		ManagedProcess process = service.start(ManagedProcessSpec.of("node").nodePreload(true)
				.command(node.getAbsolutePath(), "-e", "process.on('SIGTERM', function () { process.exit(143); }); setInterval(function () {}, 1000)"));
		Thread.sleep(500);
		assertTrue(process.stop().isEmpty());
		assertEquals(Integer.valueOf(0), process.exitValue().orElse(-1));
	}

	@Test
	public void quotesThePreloadForNodeOptions() {
		assertEquals("/a/b.js", NodePreload.quote("/a/b.js"));
		assertEquals("\"C:\\\\Users\\\\John Doe\\\\b.js\"", NodePreload.quote("C:\\Users\\John Doe\\b.js"));
		Map<String, String> env = new java.util.HashMap<>(Map.of("NODE_OPTIONS", "--max-old-space-size=4096"));
		NodePreload.addToNodeOptions(env, new File("/w s/p.js"));
		assertEquals("--max-old-space-size=4096 --require \"/w s/p.js\"", env.get("NODE_OPTIONS"));
	}
}
