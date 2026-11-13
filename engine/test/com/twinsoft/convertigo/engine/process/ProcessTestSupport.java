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

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Helpers for the process lifecycle tests: they only start and stop their own processes. */
final class ProcessTestSupport {
	static final boolean WINDOWS = File.separatorChar == '\\';

	private final List<Process> started = new ArrayList<>();
	private final List<Long> pids = new ArrayList<>();

	/** The tests use a POSIX shell. */
	static void assumeUnix() {
		assumeFalse("POSIX shell tests", WINDOWS);
	}

	/** The node executable: -DnodeExecutable, else the first node of the PATH; the test is skipped without it. */
	static File node() {
		String property = System.getProperty("nodeExecutable");
		if (property != null && new File(property).canExecute()) {
			return new File(property);
		}
		String path = System.getenv("PATH");
		if (path != null) {
			for (String dir : path.split(File.pathSeparator)) {
				File node = new File(dir, WINDOWS ? "node.exe" : "node");
				if (node.canExecute()) {
					try {
						return node.getCanonicalFile();
					} catch (IOException e) {
						return node;
					}
				}
			}
		}
		assumeNotNull((Object) null);
		return null;
	}

	Process start(ProcessBuilder builder) throws IOException {
		Process process = builder.start();
		started.add(process);
		return process;
	}

	/** A process of the test started elsewhere (child JVM), stopped at the end with its tree. */
	void own(long pid) {
		pids.add(pid);
	}

	/** A shell that only waits. */
	Process sleeper() throws IOException {
		return start(new ProcessBuilder("sleep", "600"));
	}

	/** A child JVM running main of the test classpath. */
	Process java(Class<?> main, String... args) throws IOException {
		List<String> command = new ArrayList<>();
		command.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add(main.getName());
		command.addAll(List.of(args));
		return start(new ProcessBuilder(command).redirectErrorStream(true));
	}

	/** Reads lines until one starts with prefix, at most timeout; the rest of the output stays readable. */
	static String readLine(BufferedReader reader, String prefix, Duration timeout) throws Exception {
		String[] found = new String[1];
		Thread thread = new Thread(() -> {
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith(prefix)) {
						found[0] = line.substring(prefix.length()).trim();
						return;
					}
				}
			} catch (IOException e) {
				// ended
			}
		});
		thread.setDaemon(true);
		thread.start();
		thread.join(Math.max(1, timeout.toMillis()));
		return found[0];
	}

	static BufferedReader reader(Process process) {
		return new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
	}

	static boolean alive(long pid) {
		return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
	}

	static boolean waitDead(long pid, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (!alive(pid)) {
				return true;
			}
			Thread.sleep(50);
		}
		return !alive(pid);
	}

	static boolean waitFor(java.util.function.BooleanSupplier condition, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean()) {
				return true;
			}
			Thread.sleep(50);
		}
		return condition.getAsBoolean();
	}

	/** Rewrites some recorded values, as if the entry had been written for another process. */
	static void rewrite(File file, Map<String, String> values) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(file.toPath())) {
			properties.load(in);
		}
		values.forEach(properties::setProperty);
		try (OutputStream out = Files.newOutputStream(file.toPath())) {
			properties.store(out, null);
		}
	}

	/** The identity of a process that has exited, as a dead engine. */
	Map<String, String> deadOwner() throws Exception {
		Process process = start(new ProcessBuilder("sleep", "0.2"));
		long start = ProcessRegistry.startOf(process.info());
		process.waitFor();
		return Map.of("ownerPid", Long.toString(process.pid()), "ownerStart", Long.toString(start));
	}

	/** Stops every process started by the test, with their trees. */
	void cleanup() throws InterruptedException {
		for (long pid : pids) {
			ProcessHandle.of(pid).ifPresent(handle -> ProcessTrees.stop(handle, Duration.ofSeconds(1)));
		}
		for (Process process : started) {
			if (process.isAlive()) {
				ProcessTrees.stop(process.toHandle(), Duration.ofSeconds(1));
			}
			process.waitFor(5, TimeUnit.SECONDS);
		}
	}
}
