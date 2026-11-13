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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The process reaper: a small JVM the engine starts with its first recorded process, which stops the
 * recorded processes of the engine when the engine dies abruptly (crash, kill -9, task manager), whatever
 * they are: Node.js or not (cordova, gradle, java, sdkmanager, Process execute steps).
 * <p>
 * Usage: ProcessReaper &lt;engine PID&gt; &lt;engine start instant, ms&gt; &lt;registry dir&gt;...
 * <p>
 * It learns the death of the engine from the end of its stdin, a pipe the engine keeps open, from its own
 * adoption by another process on Unix, and from the exit of the engine PID, whose start instant is checked:
 * a PID reused by another process is not the engine. It then stops, with their trees, the recorded
 * processes of this engine only, and only when their PID, start instant and command fingerprint match. It
 * leaves at once when the engine writes "exit" on its stdin, after a normal stop. "registry &lt;dir&gt;"
 * adds a registry to watch. It writes a short log in process-reaper.log of the first registry.
 * <p>
 * It only uses the JDK and the classes of this package that need nothing else: the engine copies them
 * to run it, whatever its class loader is (Tomcat, OSGi).
 */
public final class ProcessReaper implements ProcessRegistry.Log {
	static final String LOG = "process-reaper.log";
	private static final long LOG_MAX = 64 * 1024;
	private static final Duration GRACE = Duration.ofMillis(1500);
	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");

	/** the classes the reaper needs, copied by the engine to run it */
	static final List<Class<?>> CLASSES = List.of(ProcessReaper.class, ProcessTrees.class, ProcessRegistry.class,
			ProcessRegistry.Log.class, ProcessRegistry.Entry.class, ProcessRegistry.Outcome.class,
			ProcessRegistry.Cleanup.class);

	private final long enginePid;
	private final long engineStart;
	private final Set<Path> registries = new CopyOnWriteArraySet<>();
	private final CountDownLatch signal = new CountDownLatch(1);
	private volatile boolean exitRequested = false;
	private volatile boolean stdinEnded = false;
	/** whether the engine started this reaper: the reaper is adopted by another process when the engine dies */
	private final boolean engineIsParent;

	private ProcessReaper(long enginePid, long engineStart) {
		this.enginePid = enginePid;
		this.engineStart = engineStart;
		engineIsParent = ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(-1L) == enginePid;
	}

	public static void main(String[] args) throws Exception {
		ProcessReaper reaper = new ProcessReaper(Long.parseLong(args[0]), Long.parseLong(args[1]));
		for (int i = 2; i < args.length; i++) {
			reaper.registries.add(Paths.get(args[i]));
		}
		// a signal to the group of the engine (terminal closed, Ctrl+C): the engine stops its processes itself
		ignore("HUP");
		ignore("INT");
		int code = 1;
		try {
			code = reaper.run();
		} catch (Throwable e) {
			reaper.log("failed: " + e);
		}
		System.exit(code);
	}

	private int run() throws InterruptedException {
		if (!engineAlive()) {
			log("the engine " + enginePid + " is already gone");
			return reap();
		}
		Thread reader = new Thread(this::readCommands, "stdin");
		reader.setDaemon(true);
		reader.start();
		ProcessHandle.of(enginePid).ifPresent(engine -> engine.onExit().thenRun(signal::countDown));
		boolean stdinChecked = false;
		while (true) {
			// the exit of a process that is not a child is polled by the JDK, slowly after a while: check too
			if (signal.getCount() > 0) {
				signal.await(1, TimeUnit.SECONDS);
			} else {
				Thread.sleep(1000);
			}
			if (exitRequested) {
				return 0;
			}
			if (!engineAlive() || adopted()) {
				return reap();
			}
			if (stdinEnded && !stdinChecked) {
				// the system closes the pipe while the engine dies: it may still look alive for a moment
				stdinChecked = true;
				if (engineGone(Duration.ofSeconds(5))) {
					return reap();
				}
				// the engine runs without holding the stdin anymore: its PID alone is watched
			}
		}
	}

	private void readCommands() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = in.readLine()) != null) {
				if (line.equals("exit")) {
					exitRequested = true;
					signal.countDown();
					return;
				}
				if (line.startsWith("registry ")) {
					registries.add(Paths.get(line.substring(9)));
				}
			}
		} catch (IOException e) {
			// ended
		}
		stdinEnded = true;
		signal.countDown();
	}

	private boolean engineAlive() {
		return ProcessRegistry.isAlive(enginePid, engineStart);
	}

	/**
	 * @return on Unix, whether this process was adopted: its parent, the engine, is gone (a zombie still
	 *         looks alive until its own parent reaps it)
	 */
	private boolean adopted() {
		if (WINDOWS || !engineIsParent) {
			return false;
		}
		return ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(-1L) != enginePid;
	}

	private boolean engineGone(Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (exitRequested) {
				return false;
			}
			if (!engineAlive() || adopted()) {
				return true;
			}
			Thread.sleep(50);
		}
		return false;
	}

	private int reap() {
		int stopped = 0;
		for (Path dir : registries) {
			for (ProcessRegistry.Cleanup cleanup : new ProcessRegistry(dir).reap(enginePid, engineStart, GRACE, this)) {
				if (cleanup.outcome == ProcessRegistry.Outcome.STOPPED) {
					stopped++;
				}
			}
		}
		log("the engine " + enginePid + " died: " + stopped + " process(es) stopped");
		return 0;
	}

	@Override
	public void info(String message) {
		log(message);
	}

	@Override
	public void warn(String message) {
		log(message);
	}

	private void log(String message) {
		if (registries.isEmpty()) {
			return;
		}
		Path file = registries.iterator().next().resolve(LOG);
		try {
			boolean big = Files.isRegularFile(file) && Files.size(file) > LOG_MAX;
			Files.writeString(file, Instant.now() + " [reaper " + ProcessHandle.current().pid() + "] " + message + System.lineSeparator(),
					StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					big ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.APPEND);
		} catch (IOException e) {
			// nothing to log to
		}
	}

	private static void ignore(String name) {
		if (WINDOWS) {
			return;
		}
		try {
			Class<?> signal = Class.forName("sun.misc.Signal");
			Class<?> handler = Class.forName("sun.misc.SignalHandler");
			Object ignore = handler.getField("SIG_IGN").get(null);
			signal.getMethod("handle", signal, handler).invoke(null, signal.getConstructor(String.class).newInstance(name), ignore);
		} catch (Throwable e) {
			// the default: the reaper stops with the group, as the engine
		}
	}
}
