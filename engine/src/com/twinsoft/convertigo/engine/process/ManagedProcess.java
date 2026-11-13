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
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A process started by {@link ManagedProcesses}: it is stopped with its whole tree by {@link #stop()},
 * when the engine stops, and when the engine dies (Node.js preload, process reaper, Windows job), and its
 * entry in the registry of its workspace lets the next session stop it if all of that failed.
 * <p>
 * Never close the stdin of a process started with the Node.js preload: it would exit.
 */
public final class ManagedProcess {
	public enum State {
		/** started, running */
		RUNNING,
		/** {@link #stop()} is stopping its tree */
		STOPPING,
		/** stopped by {@link #stop()} */
		STOPPED,
		/** exited on its own */
		EXITED
	}

	private final ManagedProcesses service;
	final ManagedProcessSpec spec;
	private final Process process;
	private final long startedNanos = System.nanoTime();
	private final Instant startInstant;
	private final Deque<String> output = new ArrayDeque<>();
	private final Object stopLock = new Object();
	volatile ProcessRegistry.Entry entry;
	volatile ProcessRegistry registry;
	private final AtomicReference<State> state = new AtomicReference<>(State.RUNNING);
	private volatile String status = "";
	private List<ProcessHandle> survivors = null;
	private volatile Thread reader = null;

	ManagedProcess(ManagedProcesses service, ManagedProcessSpec spec, Process process) {
		this.service = service;
		this.spec = spec;
		this.process = process;
		this.startInstant = process.info().startInstant().orElse(Instant.now());
	}

	public String getFunction() {
		return spec.function;
	}

	public long pid() {
		return process.pid();
	}

	/**
	 * @return the started process, to read its output when no output consumer was given
	 */
	public Process getProcess() {
		return process;
	}

	public ProcessHandle toHandle() {
		return process.toHandle();
	}

	public Instant getStartInstant() {
		return startInstant;
	}

	public Duration getUptime() {
		return Duration.ofNanos(System.nanoTime() - startedNanos);
	}

	public State getState() {
		State current = state.get();
		if (current == State.RUNNING && !process.isAlive()) {
			return State.EXITED;
		}
		return current;
	}

	public boolean isAlive() {
		return process.isAlive();
	}

	/**
	 * @return whether this process is stopped or stopping by {@link #stop()}: its exit is not a failure
	 */
	public boolean isStopRequested() {
		State current = state.get();
		return current == State.STOPPING || current == State.STOPPED;
	}

	/**
	 * @return a status given by the launcher, e.g. "ready on http://localhost:5981", empty by default
	 */
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status == null ? "" : status;
	}

	/**
	 * @return the free fields given by the launcher, such as port or dir
	 */
	public Map<String, String> getProperties() {
		return Map.copyOf(spec.properties);
	}

	public int waitFor() throws InterruptedException {
		return process.waitFor();
	}

	public boolean waitFor(Duration timeout) throws InterruptedException {
		return process.waitFor(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
	}

	public Optional<Integer> exitValue() {
		try {
			return Optional.of(process.exitValue());
		} catch (IllegalThreadStateException e) {
			return Optional.empty();
		}
	}

	public CompletableFuture<ManagedProcess> onExit() {
		return process.onExit().thenApply(p -> this);
	}

	/**
	 * @return the last lines of the output, when an output consumer was given
	 */
	public List<String> getLastOutput() {
		synchronized (output) {
			return new ArrayList<>(output);
		}
	}

	/**
	 * Stops the process and its whole tree: the Node.js processes started with the preload are first asked
	 * to exit by the end of their stdin, then each process of the tree is asked to stop, leaves first, then
	 * destroyed forcibly after the grace delay. Idempotent; the entry of the registry is removed once the
	 * tree is stopped, kept for the next session otherwise.
	 *
	 * @return the processes of the tree still alive, normally none
	 */
	public List<ProcessHandle> stop() {
		synchronized (stopLock) {
			if (survivors != null && survivors.isEmpty()) {
				return survivors;
			}
			if (!state.compareAndSet(State.RUNNING, State.STOPPING)) {
				state.compareAndSet(State.EXITED, State.STOPPING);
			}
			Runnable gentle = null;
			Duration gentleWait = Duration.ZERO;
			if (spec.nodePreload && process.isAlive()) {
				gentle = () -> {
					try {
						process.getOutputStream().close();
					} catch (IOException e) {
						// the signals stop it
					}
				};
				gentleWait = spec.stopGrace.compareTo(Duration.ofSeconds(3)) < 0 ? spec.stopGrace : Duration.ofSeconds(3);
			}
			List<ProcessHandle> alive = process.isAlive() || process.toHandle().children().findAny().isPresent()
					? ProcessTrees.stop(process.toHandle(), spec.stopGrace, gentle, gentleWait)
					: List.of();
			survivors = alive;
			state.set(State.STOPPED);
			if (alive.isEmpty()) {
				service.forget(this);
			} else {
				service.log().warn("Cannot stop the process " + describe() + ", still alive: " + alive);
			}
			return alive;
		}
	}

	/**
	 * The process exited on its own, or was stopped.
	 */
	void exited() {
		// never waits for a stop in progress, which forgets the process itself
		if (state.compareAndSet(State.RUNNING, State.EXITED)) {
			service.forget(this);
		}
	}

	/**
	 * Waits until the output consumer has read the whole output, once the process exited.
	 *
	 * @return whether the output is read
	 */
	public boolean awaitOutput(Duration timeout) throws InterruptedException {
		Thread thread = reader;
		if (thread == null) {
			return true;
		}
		thread.join(Math.max(1, timeout.toMillis()));
		return !thread.isAlive();
	}

	void readOutput(Charset charset) {
		Thread reader = new Thread(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
				String line;
				while ((line = br.readLine()) != null) {
					try {
						spec.output.accept(line);
					} catch (RuntimeException e) {
						// a consumer error does not stop the reading, which would block the process
					}
					if (spec.outputLines > 0) {
						synchronized (output) {
							output.addLast(line);
							if (output.size() > spec.outputLines) {
								output.removeFirst();
							}
						}
					}
				}
			} catch (IOException e) {
				// process gone
			}
		}, "Output of " + spec.function + " " + process.pid());
		reader.setDaemon(true);
		this.reader = reader;
		reader.start();
	}

	public String describe() {
		return spec.function + " (PID " + process.pid() + ", started at " + startInstant + ")";
	}

	@Override
	public String toString() {
		return describe() + " " + getState();
	}
}
