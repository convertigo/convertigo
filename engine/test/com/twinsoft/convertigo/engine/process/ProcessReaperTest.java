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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The process reaper stops the processes of an engine killed abruptly, Node.js or not.
 */
public class ProcessReaperTest {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	private final ProcessTestSupport support = new ProcessTestSupport();
	private File workspace;

	@Before
	public void setUp() throws Exception {
		ProcessTestSupport.assumeUnix();
		workspace = temporary.newFolder("workspace");
	}

	@After
	public void cleanup() throws Exception {
		support.cleanup();
	}

	private static class Engine {
		Process process;
		BufferedReader reader;
		long reaper;
	}

	private Engine engine(String... kinds) throws Exception {
		String[] args = new String[kinds.length + 2];
		args[0] = workspace.getAbsolutePath();
		args[1] = "on";
		System.arraycopy(kinds, 0, args, 2, kinds.length);
		Engine engine = new Engine();
		engine.process = support.java(ManagedProcessLauncher.class, args);
		engine.reader = ProcessTestSupport.reader(engine.process);
		return engine;
	}

	private long next(Engine engine, String prefix) throws Exception {
		String value = ProcessTestSupport.readLine(engine.reader, prefix + " ", Duration.ofSeconds(20));
		long pid = Long.parseLong(value);
		support.own(pid);
		return pid;
	}

	private String log() throws Exception {
		File file = new File(workspace, ManagedProcesses.REGISTRY_DIR + "/" + ProcessReaper.LOG);
		return file.exists() ? Files.readString(file.toPath()) : "";
	}

	@Test
	public void stopsTheTreeOfAKilledEngineWithin2Seconds() throws Exception {
		Engine engine = engine("shell");
		long shell = next(engine, "shell");
		engine.reaper = next(engine, "reaper");
		next(engine, "ready");
		assertTrue(engine.reaper > 0);
		List<ProcessHandle> tree = ProcessTrees.leavesFirst(ProcessHandle.of(shell).orElseThrow());
		assertEquals("sh and its two sleep", 3, tree.size());

		long killed = System.nanoTime();
		engine.process.destroyForcibly();
		for (ProcessHandle handle : tree) {
			assertTrue(ProcessTestSupport.waitDead(handle.pid(), Duration.ofSeconds(2)));
		}
		Duration elapsed = Duration.ofNanos(System.nanoTime() - killed);
		assertTrue("stopped in " + elapsed.toMillis() + " ms", elapsed.compareTo(Duration.ofSeconds(2)) < 0);
		assertTrue("the reaper leaves", ProcessTestSupport.waitDead(engine.reaper, Duration.ofSeconds(3)));
		assertTrue(ManagedProcesses.registry(workspace).entries().isEmpty());
		assertTrue(log(), log().contains("1 process(es) stopped"));
	}

	@Test
	public void sparesAForeignProcessWithTheSamePid() throws Exception {
		Engine engine = engine("shell", "shell");
		long mine = next(engine, "shell");
		long foreign = next(engine, "shell");
		engine.reaper = next(engine, "reaper");
		next(engine, "ready");
		// as if the PID of the second one now belonged to another process: another start instant
		ProcessRegistry.Entry entry = ManagedProcesses.registry(workspace).entries().stream()
				.filter(e -> e.getPid() == foreign).findFirst().orElseThrow();
		ProcessTestSupport.rewrite(entry.getFile().toFile(), Map.of("start", Long.toString(entry.getStart() - 1000)));

		engine.process.destroyForcibly();
		assertTrue(ProcessTestSupport.waitDead(mine, Duration.ofSeconds(2)));
		assertTrue(ProcessTestSupport.waitDead(engine.reaper, Duration.ofSeconds(3)));
		assertTrue("the foreign process is spared", ProcessTestSupport.alive(foreign));
		assertTrue(log(), log().contains("now used by another process"));
	}

	@Test
	public void leavesAtOnceAfterANormalStop() throws Exception {
		Engine engine = engine("shell");
		long shell = next(engine, "shell");
		engine.reaper = next(engine, "reaper");
		next(engine, "ready");
		// a normal stop: the hook stops the processes and the reaper
		engine.process.getOutputStream().close();
		assertEquals(0, engine.process.waitFor());
		assertFalse(ProcessTestSupport.alive(shell));
		assertTrue(ProcessTestSupport.waitDead(engine.reaper, Duration.ofSeconds(2)));
		assertTrue(ManagedProcesses.registry(workspace).entries().isEmpty());
		assertFalse(log(), log().contains("died"));
	}

	@Test
	public void neverTwoReapers() throws Exception {
		ManagedProcesses service = new ManagedProcesses();
		service.setReaperMode("on");
		service.open(workspace);
		try {
			service.start(ManagedProcessSpec.of("a").command("sleep", "600"));
			long reaper = service.getReaperPid();
			assertTrue(reaper > 0);
			service.start(ManagedProcessSpec.of("b").command("sleep", "600"));
			service.start(ManagedProcessSpec.of("c").owner(temporary.newFolder("other")).command("sleep", "600"));
			assertEquals(reaper, service.getReaperPid());
			assertEquals(1, ProcessHandle.current().children()
					.filter(child -> child.info().commandLine().orElse("").contains(ProcessReaper.class.getName())).count());

			service.stopAll();
			assertTrue("the reaper leaves with the last process", ProcessTestSupport.waitDead(reaper, Duration.ofSeconds(2)));
			service.open(workspace);
			service.start(ManagedProcessSpec.of("d").command("sleep", "600"));
			assertNotEquals(reaper, service.getReaperPid());
			assertTrue(service.getReaperPid() > 0);
		} finally {
			service.stopAll();
		}
	}

	@Test
	public void noReaperWhenOff() throws Exception {
		ManagedProcesses service = new ManagedProcesses();
		service.setReaperMode("off");
		service.open(workspace);
		try {
			service.start(ManagedProcessSpec.of("a").command("sleep", "600"));
			assertEquals(-1, service.getReaperPid());
			assertFalse("this test does not run as the main process of a container", ManagedProcesses.isContainerMainProcess());
		} finally {
			service.stopAll();
		}
	}
}
