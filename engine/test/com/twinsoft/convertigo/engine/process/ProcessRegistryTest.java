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

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProcessRegistryTest {
	private static final String FUNCTION = "ngx-serve:my project";

	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	private final ProcessTestSupport support = new ProcessTestSupport();
	private final List<String> logs = new ArrayList<>();
	private final ProcessRegistry.Log log = new ProcessRegistry.Log() {

		@Override
		public void info(String message) {
			logs.add("info " + message);
		}

		@Override
		public void warn(String message) {
			logs.add("warn " + message);
		}
	};

	@Before
	public void unix() {
		ProcessTestSupport.assumeUnix();
	}

	@After
	public void cleanup() throws Exception {
		support.cleanup();
	}

	private ProcessRegistry registry() throws IOException {
		return new ProcessRegistry(temporary.newFolder("processes").toPath());
	}

	@Test
	public void recordsTheIdentityWithoutTheArgumentsInClear() throws Exception {
		ProcessRegistry registry = registry();
		Process process = support.start(new ProcessBuilder("sleep", "601"));
		ProcessRegistry.Entry entry = registry.record(FUNCTION, process.toHandle(), Map.of("port", "40001"));
		assertTrue(entry.getFile().getFileName().toString().matches("ngx-serve_my_project-\\d+\\.properties"));
		List<ProcessRegistry.Entry> entries = registry.entries(FUNCTION);
		assertEquals(1, entries.size());
		ProcessRegistry.Entry read = entries.get(0);
		assertEquals(FUNCTION, read.getFunction());
		assertEquals(process.pid(), read.getPid());
		assertEquals(ProcessRegistry.startOf(process.info()), read.getStart());
		assertEquals(process.info().command().orElse(""), read.getCommand());
		assertEquals("40001", read.getProperty("port"));
		assertTrue(read.isOwnedBy(ProcessHandle.current().pid(), ProcessRegistry.startOf(ProcessHandle.current().info())));
		String text = Files.readString(entry.getFile());
		assertFalse("the arguments are not written in clear", text.contains("601"));
		assertTrue(text.contains("commandLineHash="));
		assertTrue(ProcessRegistry.match(read).isPresent());
	}

	@Test
	public void stopsTheProcessOfADeadEngine() throws Exception {
		ProcessRegistry registry = registry();
		Process process = support.start(new ProcessBuilder("sh", "-c", "sleep 600 & wait"));
		Thread.sleep(200);
		long child = process.toHandle().children().findFirst().orElseThrow().pid();
		ProcessRegistry.Entry entry = registry.record(FUNCTION, process.toHandle(), Map.of());
		ProcessTestSupport.rewrite(entry.getFile().toFile(), support.deadOwner());
		List<ProcessRegistry.Cleanup> result = registry.cleanup(FUNCTION, Duration.ofSeconds(5), log);
		assertEquals(ProcessRegistry.Outcome.STOPPED, result.get(0).getOutcome());
		assertTrue(ProcessTestSupport.waitDead(process.pid(), Duration.ofSeconds(2)));
		assertTrue("its tree too", ProcessTestSupport.waitDead(child, Duration.ofSeconds(2)));
		assertTrue(registry.entries().isEmpty());
	}

	@Test
	public void sparesAProcessWithTheSamePidStartedAtAnotherInstant() throws Exception {
		ProcessRegistry registry = registry();
		Process foreign = support.sleeper();
		ProcessRegistry.Entry entry = registry.record(FUNCTION, foreign.toHandle(), Map.of());
		Map<String, String> owner = new java.util.HashMap<>(support.deadOwner());
		owner.put("start", Long.toString(entry.getStart() - 1000));
		ProcessTestSupport.rewrite(entry.getFile().toFile(), owner);
		List<ProcessRegistry.Cleanup> result = registry.cleanup(FUNCTION, Duration.ofSeconds(5), log);
		assertEquals(ProcessRegistry.Outcome.GONE, result.get(0).getOutcome());
		assertTrue("the foreign process is untouched", foreign.isAlive());
		assertTrue(logs.toString(), logs.stream().anyMatch(line -> line.contains("now used by another process")));
		assertTrue("the entry is forgotten", registry.entries().isEmpty());
	}

	@Test
	public void sparesAProcessWithTheSamePidAndAnotherCommand() throws Exception {
		ProcessRegistry registry = registry();
		Process foreign = support.sleeper();
		ProcessRegistry.Entry entry = registry.record(FUNCTION, foreign.toHandle(), Map.of());
		Map<String, String> values = new java.util.HashMap<>(support.deadOwner());
		values.put("command", "/usr/bin/other");
		ProcessTestSupport.rewrite(entry.getFile().toFile(), values);
		registry.cleanup(FUNCTION, Duration.ofSeconds(5), log);
		assertTrue(foreign.isAlive());
		values.put("command", entry.getCommand());
		values.put("commandLineHash", ProcessRegistry.hash("sleep 1"));
		entry = registry.record(FUNCTION, foreign.toHandle(), Map.of());
		ProcessTestSupport.rewrite(entry.getFile().toFile(), values);
		registry.cleanup(FUNCTION, Duration.ofSeconds(5), log);
		assertTrue("other arguments: untouched", foreign.isAlive());
	}

	@Test
	public void leavesTheProcessesOfARunningEngine() throws Exception {
		ProcessRegistry registry = registry();
		Process process = support.sleeper();
		Process otherEngine = support.sleeper();
		ProcessRegistry.Entry entry = registry.record(FUNCTION, process.toHandle(), Map.of());
		ProcessTestSupport.rewrite(entry.getFile().toFile(), Map.of("ownerPid", Long.toString(otherEngine.pid()),
				"ownerStart", Long.toString(ProcessRegistry.startOf(otherEngine.info()))));
		List<ProcessRegistry.Cleanup> result = registry.cleanup(FUNCTION, Duration.ofSeconds(5), log);
		assertEquals(ProcessRegistry.Outcome.OWNER_ALIVE, result.get(0).getOutcome());
		assertTrue(process.isAlive());
		assertEquals("the entry is kept", 1, registry.entries().size());
	}

	@Test
	public void understandsTheEntriesOf848() throws Exception {
		ProcessRegistry registry = registry();
		Process process = support.sleeper();
		ProcessRegistry.Entry entry = registry.record("pouchdb-server", process.toHandle(), Map.of());
		Map<String, String> values = new java.util.HashMap<>(support.deadOwner());
		values.put("commandLine", process.info().commandLine().orElse(""));
		values.put("commandLineHash", "");
		ProcessTestSupport.rewrite(entry.getFile().toFile(), values);
		List<ProcessRegistry.Cleanup> result = registry.cleanup("pouchdb-server", Duration.ofSeconds(5), log);
		assertEquals(ProcessRegistry.Outcome.STOPPED, result.get(0).getOutcome());
		assertTrue(ProcessTestSupport.waitDead(process.pid(), Duration.ofSeconds(2)));
	}

	@Test
	public void reapsOnlyTheProcessesOfTheDeadEngine() throws Exception {
		ProcessRegistry registry = registry();
		Process mine = support.sleeper();
		Process other = support.sleeper();
		Map<String, String> dead = support.deadOwner();
		ProcessTestSupport.rewrite(registry.record(FUNCTION, mine.toHandle(), Map.of()).getFile().toFile(), dead);
		registry.record("other", other.toHandle(), Map.of());
		registry.reap(Long.parseLong(dead.get("ownerPid")), Long.parseLong(dead.get("ownerStart")), Duration.ofSeconds(2), log);
		assertTrue(ProcessTestSupport.waitDead(mine.pid(), Duration.ofSeconds(2)));
		assertTrue(other.isAlive());
		assertEquals(1, registry.entries().size());
	}

	@Test
	public void forgetsTheEntriesOfProcessesGone() throws Exception {
		ProcessRegistry registry = registry();
		Process process = support.start(new ProcessBuilder("sleep", "0.1"));
		registry.record(FUNCTION, process.toHandle(), Map.of());
		process.waitFor();
		List<ProcessRegistry.Cleanup> result = registry.cleanup(null, Duration.ofSeconds(1), log);
		assertEquals(ProcessRegistry.Outcome.GONE, result.get(0).getOutcome());
		assertTrue(registry.entries().isEmpty());
	}

	@Test
	public void refreshesTheIdentityAfterAnExec() throws Exception {
		ProcessRegistry registry = registry();
		// as a launcher script: the shell executes another program
		Process process = support.start(new ProcessBuilder("sh", "-c", "sleep 0.3; exec sleep 600"));
		ProcessRegistry.Entry entry = registry.record(FUNCTION, process.toHandle(), Map.of());
		assertTrue(entry.getCommand().endsWith("sh"));
		Thread.sleep(800);
		assertFalse("the recorded command no longer matches", ProcessRegistry.match(entry).isPresent());
		ProcessRegistry.Entry refreshed = registry.refresh(entry);
		assertTrue(refreshed.getCommand().endsWith("sleep"));
		assertTrue(ProcessRegistry.match(registry.entries().get(0)).isPresent());
	}
}
