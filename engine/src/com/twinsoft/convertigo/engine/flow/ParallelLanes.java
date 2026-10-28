/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The lanes of a parallel chunk loop: threads of a bounded pool of the server, each running a script function in its own
 * Rhino context and execution frame. The calling thread submits the tasks, reads their outcomes and cancels; a lane only
 * takes tasks and gives outcomes through this object. The pool never queues: a lane without a free thread is not started,
 * so nested or saturated loops never wait for a thread.
 * <p>
 * A task whose outcome is dropped (the tasks after a failed one, or every task once the lanes are cancelled) is stopped
 * at its next block, and its lane is interrupted at once: a blocking call of that task (an HTTP request, a wait) ends
 * instead of running to its timeout. The interruption only reaches a lane while it runs such a task, never between
 * tasks. The caller can then ask which tasks after a failed one had started.
 * <p>
 * Every method has a single signature: scripts call them without Rhino's lazily filled overload table.
 */
public final class ParallelLanes implements AutoCloseable {
	static final int THREADS = Math.max(1, Math.min(512,
			Integer.getInteger("convertigo.flow.parallel_threads", 2 * Runtime.getRuntime().availableProcessors())));
	private static final long GRACE_MILLIS = Math.max(0, Long.getLong("convertigo.flow.parallel_grace_ms", 10_000L));
	private static final AtomicInteger THREAD_IDS = new AtomicInteger();
	private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(0, THREADS, 60, TimeUnit.SECONDS,
			new SynchronousQueue<>(), runnable -> {
				var thread = new Thread(runnable, "chunk-lane-" + THREAD_IDS.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			});

	/** What a lane runs on its thread: it enters its own context and frame, then runs the lane function. */
	interface Lane {
		void run(ParallelLanes lanes, int index) throws Exception;
	}

	private record Task(long index, Object value) {
	}

	// one per lane after the last task: a waiting lane ends at once instead of at its next poll
	private static final Task END = new Task(-1, null);

	private final ArrayBlockingQueue<Task> tasks;
	private final LinkedBlockingQueue<Object> outcomes = new LinkedBlockingQueue<>();
	private final List<Thread> threads = new ArrayList<>();
	private final Object lock = new Object();
	// under lock: the task each lane thread runs, and the tasks started
	private final Map<Thread, Long> runningTasks = new IdentityHashMap<>();
	private final BitSet startedTasks = new BitSet();
	private int started, running;
	private volatile boolean finished;
	private volatile String cancelled;
	private volatile long stopAfter = Long.MAX_VALUE;
	private volatile Throwable failure;

	private ParallelLanes(int requested) {
		// the window of the caller, then the end marks
		tasks = new ArrayBlockingQueue<>(Math.max(2, 2 * requested) + requested);
	}

	/** Up to requested lanes on free threads of the pool; null when none is free. */
	static ParallelLanes open(int requested, Lane lane) {
		var lanes = new ParallelLanes(requested);
		for (int i = 0; i < requested; i++) {
			int index = i;
			synchronized (lanes.lock) {
				lanes.running++;
			}
			try {
				POOL.execute(() -> lanes.runLane(lane, index));
				lanes.started++;
			} catch (RejectedExecutionException saturated) {
				synchronized (lanes.lock) {
					lanes.running--;
					lanes.lock.notifyAll();
				}
				break;
			}
		}
		if (lanes.started == 0) {
			return null;
		}
		return lanes;
	}

	private void runLane(Lane lane, int index) {
		var thread = Thread.currentThread();
		synchronized (lock) {
			threads.add(thread);
		}
		try {
			lane.run(this, index);
		} catch (Throwable error) {
			if (failure == null) failure = error;
			cancel("lane failure");
		} finally {
			synchronized (lock) {
				threads.remove(thread);
				runningTasks.remove(thread);
				running--;
				lock.notifyAll();
			}
			// a pooled thread starts its next lane without the interruption of this one
			Thread.interrupted();
		}
	}

	/** The lanes started. */
	public int count() {
		return started;
	}

	/** The tasks the caller may have submitted and not yet settled: bounds the memory of the loop. */
	public int window() {
		return Math.max(2, 2 * started);
	}

	/** Caller: gives a task to the lanes; the window of the caller keeps the queue from filling. */
	public void submit(Object task, double index) {
		if (!tasks.offer(new Task((long) index, task))) {
			throw new IllegalStateException("More tasks than the window of the lanes.");
		}
	}

	/** Caller: no more tasks; the lanes end once the queue is empty. */
	public void finish() {
		if (finished) return;
		finished = true;
		for (int i = 0; i < started; i++) tasks.offer(END);
	}

	/**
	 * Caller: the next outcome given by a lane, null after millis. The failure of a lane outside its tasks comes first: it
	 * cancelled the other lanes, whose outcomes would only say so.
	 */
	public Object poll(double millis) throws Exception {
		var outcome = outcomes.poll((long) Math.max(0, millis), TimeUnit.MILLISECONDS);
		var failed = failure;
		if (failed != null) {
			if (failed instanceof Exception exception) throw exception;
			throw new IllegalStateException("A parallel lane failed.", failed);
		}
		return outcome;
	}

	/**
	 * Caller: the tasks after index are useless (a failed task): lanes skip them, and the running ones stop at their
	 * next block, their lanes interrupted.
	 */
	public void stopAfter(double index) {
		var value = (long) index;
		synchronized (lock) {
			if (value < stopAfter) stopAfter = value;
			interruptTasksAfter(stopAfter);
		}
	}

	/** Caller: stops every lane at its next block, the running tasks interrupted. */
	public void cancel(String reason) {
		synchronized (lock) {
			if (cancelled == null) cancelled = reason == null || reason.isEmpty() ? "cancelled" : reason;
			interruptTasksAfter(-1);
		}
	}

	private void interruptTasksAfter(long index) {
		for (var entry : runningTasks.entrySet()) {
			if (entry.getValue() > index) entry.getKey().interrupt();
		}
	}

	/** Caller, once the lanes are closed: the tasks after index that a lane started, in order. */
	public int[] startedAfter(double index) {
		synchronized (lock) {
			var from = (int) Math.max(0, Math.min(Integer.MAX_VALUE - 1L, (long) index + 1));
			return startedTasks.stream().filter(task -> task >= from).toArray();
		}
	}

	/** The reason of a cancellation, null while the lanes may go on. */
	public String cancelled() {
		return cancelled;
	}

	/** Lane: the next task, null once the tasks are finished or the lanes cancelled. */
	public Object next() throws InterruptedException {
		while (cancelled == null) {
			var task = tasks.poll(50, TimeUnit.MILLISECONDS);
			if (task == END) return null;
			if (task != null) {
				synchronized (lock) {
					// checked with the interruptions: a task given after stopAfter is never one it drops
					if (task.index() > stopAfter || cancelled != null) continue;
					runningTasks.put(Thread.currentThread(), task.index());
					if (task.index() <= Integer.MAX_VALUE) startedTasks.set((int) task.index());
				}
				return task.value();
			}
			if (finished && tasks.isEmpty()) return null;
		}
		return null;
	}

	/** Lane: whether the task of index may run its next block. */
	public boolean canContinue(double index) {
		return cancelled == null && (long) index <= stopAfter;
	}

	/** Lane: the outcome of a task, read by the caller; an interruption meant for this task ends with it. */
	public void complete(Object outcome) {
		synchronized (lock) {
			runningTasks.remove(Thread.currentThread());
			Thread.interrupted();
		}
		outcomes.add(outcome);
	}

	/**
	 * Caller: ends the loop and waits for the lanes; past the grace delay they are interrupted, then left behind with a
	 * warning. A lane left behind never gives its outcome to anyone.
	 */
	@Override
	public void close() {
		finish();
		synchronized (lock) {
			var deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(GRACE_MILLIS);
			awaitLanes(deadline);
			if (running > 0) {
				cancel("timeout");
				for (var thread : threads) thread.interrupt();
				awaitLanes(System.nanoTime() + TimeUnit.SECONDS.toNanos(1));
			}
			if (running > 0 && com.twinsoft.convertigo.engine.Engine.logEngine != null) {
				com.twinsoft.convertigo.engine.Engine.logEngine.warn("(ParallelLanes) " + running
						+ " parallel lane(s) did not stop in time and are left behind.");
			}
		}
		tasks.clear();
		outcomes.clear();
	}

	private void awaitLanes(long deadline) {
		var interrupted = false;
		while (running > 0) {
			var left = deadline - System.nanoTime();
			if (left <= 0) break;
			try {
				lock.wait(Math.max(1, TimeUnit.NANOSECONDS.toMillis(left)));
			} catch (InterruptedException e) {
				interrupted = true;
				cancel("interrupted");
			}
		}
		if (interrupted) Thread.currentThread().interrupt();
	}

	/** Threads busy in lanes of the whole server, for diagnostics and tests. */
	public static int activeThreads() {
		return POOL.getActiveCount();
	}
}
