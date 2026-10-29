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

package com.twinsoft.convertigo.engine.flow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/**
 * Prepares in the background the authoring responses the Studio asks first that the disk cache does not hold yet
 * ({@link FlowAuthoringDiskCache}), so that the Studio finds them ready: the tree of each loaded project with a Flow
 * engine (its first expansion), then, for a project opened in the Studio, the palettes of the roots of its Flows and of
 * the pages and layouts of its frontends. A response on disk is neither computed nor read: a probe of its key tells
 * it is there ({@link FlowEngineBridge#probing}).
 * <p>
 * When: from the first call of the web Studio after a start, the listing of its projects ({@link #studioOpened}), not
 * at the start of the engine: after a restart the disk holds these responses already and nothing is computed, after a
 * deployment only those of the projects changed are, and only on a server whose Studio is used, the processor of a small
 * shared server staying for its requests otherwise. The palettes of a project when it is opened in the Studio
 * ({@link #projectOpened}), the project first. A project loaded again (deployed, imported, reloaded) once the Studio is
 * opened has its tree, and those of the other projects, which may use it, checked again.
 * <p>
 * How: one thread of low priority, one response at a time, each started once no other call of the runtime providers
 * ran for {@value #QUIET_MILLIS} ms: a call of the Studio or a request arriving meanwhile waits at most for the response
 * in progress (the same frontend response joins it). Stopped with the engine, between two responses.
 * <p>
 * Engine property javascript.authoring_warmup: auto (default) on a server with the web Studio enabled, not in the
 * Studio; on: also in the Studio; off: never. Never without the disk cache, nor in the command line mode.
 */
public final class FlowAuthoringWarmup {
	/** A response is prepared once no call of the Studio nor of the runtime providers ran for this time. */
	static final long QUIET_MILLIS = 1000;
	/** The pages and layouts of the frontends of a project whose palettes are prepared, at most, in the order of its tree. */
	static final int FRONTEND_PALETTES_LIMIT = 8;

	/** A response to prepare: the tree of a project, or its palettes. */
	record Item(String project, boolean palettes) { }

	/** The preparation of an item, replaced by the tests. */
	interface Preparation {
		/** @return whether the response was computed, not found on disk */
		boolean prepare(Item item) throws Exception;
	}

	static final Preparation PREPARATION = FlowAuthoringWarmup::prepareItem;
	static volatile Preparation preparation = PREPARATION;
	/** The loaded projects with a Flow engine, in their load order, replaced by the tests. */
	static final java.util.function.Supplier<List<String>> LOADED_PROJECTS = FlowAuthoringWarmup::flowProjects;
	static volatile java.util.function.Supplier<List<String>> projects = LOADED_PROJECTS;
	/** The order of a batch of projects, replaced by the tests: those the fewest of them reference first. */
	static volatile java.util.function.Function<List<String>, List<String>> ordering = FlowRuntimeWarmup::leastReferencedFirst;

	private static final Object lock = new Object();
	private static final LinkedHashSet<Item> pending = new LinkedHashSet<>();
	/** The projects whose palettes were prepared since their last load. */
	private static final Set<String> palettesPrepared = new HashSet<>();
	private static boolean opened;
	private static boolean draining;
	private static ExecutorService executor;
	/** Changed by each stop: a pass of a previous start stops. */
	private static long epoch;
	private static final AtomicInteger computed = new AtomicInteger(), found = new AtomicInteger();
	/** The calls of the web Studio in progress, and when the last one ended. */
	private static final AtomicInteger studioCalls = new AtomicInteger();
	private static volatile long lastStudioCall = System.nanoTime();

	private FlowAuthoringWarmup() {
	}

	/** @return whether the responses are prepared in advance, as the engine property tells */
	static boolean enabled() {
		if (Engine.isCliMode() || FlowAuthoringDiskCache.get() == null) {
			return false;
		}
		String value;
		try {
			value = EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_AUTHORING_WARMUP);
		} catch (Exception notInitialized) {
			return false;
		}
		return switch (value == null ? "auto" : value.trim().toLowerCase()) {
		case "off", "false" -> false;
		case "on", "true" -> true;
		default -> !Engine.isStudioMode() && Engine.isWebStudioAllowed();
		};
	}

	/**
	 * The web Studio lists the projects: the first time after a start, the trees of the loaded projects with a Flow
	 * engine missing from the disk are prepared, those referenced by the fewest of them first (the applications before
	 * the libraries they use).
	 */
	public static void studioOpened() {
		synchronized (lock) {
			if (opened) {
				return;
			}
		}
		if (!enabled()) {
			return;
		}
		var names = projects.get();
		synchronized (lock) {
			if (opened) {
				return;
			}
			opened = true;
			for (var name : ordering.apply(names)) {
				pending.add(new Item(name, false));
			}
			schedule();
		}
	}

	/** A call of the web Studio starts, or ends: the preparation waits until the Studio is quiet. */
	public static void studioCall(boolean started) {
		if (started) {
			studioCalls.incrementAndGet();
		} else {
			lastStudioCall = System.nanoTime();
			studioCalls.decrementAndGet();
		}
	}

	/** A project is opened (expanded) in the Studio: its palettes missing from the disk are prepared next. */
	public static void projectOpened(String name) {
		if (name == null || name.isBlank()) {
			return;
		}
		synchronized (lock) {
			if (palettesPrepared.contains(name)) {
				return;
			}
		}
		if (!enabled()) {
			return;
		}
		synchronized (lock) {
			if (!palettesPrepared.add(name)) {
				return;
			}
			var others = new ArrayList<>(pending);
			pending.clear();
			pending.add(new Item(name, true));
			pending.addAll(others);
			schedule();
		}
	}

	/**
	 * A project is loaded again (deployed, imported, reloaded): its palettes are prepared again when it is opened, and
	 * once the Studio is opened the trees of the projects, which may use it, are checked again.
	 */
	static void projectLoaded(String name) {
		synchronized (lock) {
			palettesPrepared.remove(name);
			if (!opened) {
				return;
			}
		}
		if (!enabled()) {
			return;
		}
		var names = projects.get();
		synchronized (lock) {
			if (!opened) {
				return;
			}
			for (var project : ordering.apply(names)) {
				pending.add(new Item(project, false));
			}
			schedule();
		}
	}

	/** The engine stops: the preparation stops, between two responses, and starts again at the next opening of the Studio. */
	public static void stop() {
		ExecutorService stopped;
		synchronized (lock) {
			epoch++;
			pending.clear();
			palettesPrepared.clear();
			opened = false;
			draining = false;
			stopped = executor;
			executor = null;
			lock.notifyAll();
		}
		if (stopped != null) {
			stopped.shutdownNow();
		}
		FlowAuthoringDiskCache.stop();
	}

	/** For the tests and diagnostics: waits until nothing is pending nor being prepared. */
	static boolean awaitIdle(long timeout, TimeUnit unit) throws InterruptedException {
		var deadline = System.nanoTime() + unit.toNanos(timeout);
		synchronized (lock) {
			while (draining || !pending.isEmpty()) {
				var left = deadline - System.nanoTime();
				if (left <= 0) {
					return false;
				}
				TimeUnit.NANOSECONDS.timedWait(lock, left);
			}
			return true;
		}
	}

	/** @return the responses computed, and those found on disk, by the preparations of this JVM */
	static int[] counts() {
		return new int[] { computed.get(), found.get() };
	}

	/** Must hold the lock. */
	private static void schedule() {
		if (draining || pending.isEmpty()) {
			return;
		}
		draining = true;
		var run = epoch;
		executor().execute(() -> drain(run));
	}

	private static void drain(long run) {
		Log4jHelper.mdcClear();
		var started = System.nanoTime();
		var computedBefore = computed.get();
		var foundBefore = found.get();
		try {
			for (;;) {
				// quiet first: a project opened meanwhile comes first
				if (!awaitQuiet(run)) {
					return;
				}
				Item item;
				synchronized (lock) {
					var next = pending.iterator();
					if (epoch != run || !next.hasNext()) {
						if (epoch == run) {
							draining = false;
							lock.notifyAll();
							var count = computed.get() - computedBefore;
							var ready = found.get() - foundBefore;
							if (count + ready > 0) {
								info("(FlowAuthoringWarmup) " + count + " authoring item" + (count == 1 ? "" : "s") + " computed, " + ready
										+ " found on disk or in memory, in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");
							}
						}
						return;
					}
					item = next.next();
					next.remove();
				}
				try {
					var itemStarted = System.nanoTime();
					var wasComputed = FlowEngineBridge.inBackground(() -> preparation.prepare(item));
					if (wasComputed) {
						computed.incrementAndGet();
						debug("(FlowAuthoringWarmup) " + (item.palettes() ? "Palettes" : "Tree") + " of the project " + item.project()
								+ " prepared in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - itemStarted) + " ms", null);
					} else {
						found.incrementAndGet();
					}
				} catch (Throwable t) {
					if (FlowRuntimeApi.interruption(t) || Thread.currentThread().isInterrupted()) {
						return;
					}
					debug("(FlowAuthoringWarmup) Unable to prepare the " + (item.palettes() ? "palettes" : "tree") + " of the project "
							+ item.project(), t);
				}
			}
		} finally {
			synchronized (lock) {
				if (epoch == run && draining) {
					draining = false;
					lock.notifyAll();
					if (!Thread.currentThread().isInterrupted()) {
						schedule();
					}
				}
			}
		}
	}

	/**
	 * Waits until no call of the Studio nor of the runtime providers (the requests) ran for {@value #QUIET_MILLIS} ms.
	 *
	 * @return false when the preparation stops meanwhile
	 */
	private static boolean awaitQuiet(long run) {
		for (;;) {
			synchronized (lock) {
				if (epoch != run) {
					return false;
				}
			}
			var now = System.nanoTime();
			var idle = TimeUnit.NANOSECONDS.toMillis(Math.min(now - FlowEngineBridge.lastForegroundCall(), now - lastStudioCall));
			if (FlowEngineBridge.foregroundCalls() == 0 && studioCalls.get() == 0 && idle >= QUIET_MILLIS) {
				return true;
			}
			try {
				Thread.sleep(Math.max(20, Math.min(QUIET_MILLIS, QUIET_MILLIS - idle)));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
	}

	/** @return the loaded projects with a Flow engine, in their load order */
	private static List<String> flowProjects() {
		var names = new ArrayList<String>();
		var app = Engine.theApp;
		var manager = app == null ? null : app.databaseObjectsManager;
		if (manager == null) {
			return names;
		}
		for (var project : manager.getLoadedProjects()) {
			if (project.getFlowEngine() != null) {
				names.add(project.getName());
			}
		}
		return names;
	}

	private static Project loadedProject(String name) {
		var app = Engine.theApp;
		var manager = app == null ? null : app.databaseObjectsManager;
		return manager == null ? null : manager.getLoadedProjectByName(name);
	}

	/** The preparation of an item: its responses probed on disk, those missing computed (and kept by the bridge). */
	private static boolean prepareItem(Item item) throws Exception {
		var project = loadedProject(item.project());
		var flowEngine = project == null ? null : project.getFlowEngine();
		if (flowEngine == null) {
			return false;
		}
		if (!item.palettes()) {
			// the request of the first expansion of the project (FlowVirtualProjector)
			var started = System.nanoTime();
			FlowEngineBridge.probing("describeTree", () -> new FlowEngineBridge().describeTree(flowEngine));
			return !FlowEngineBridge.probeAnswered()
					&& System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(FlowAuthoringDiskCache.MIN_COMPUTE_MILLIS);
		}
		var any = false;
		for (var flow : FlowRuntimeWarmup.flows(project)) {
			if (stopped()) {
				return any;
			}
			var root = FlowStudioSupport.resolveTreeObject(project.getName() + ".sq:" + flow.getName() + ".flow");
			if (root != null && FlowStudioSupport.isFlowPaletteTarget(root)) {
				any |= palette(root);
			}
		}
		var frontends = new ArrayList<DatabaseObject>();
		frontendTargets(flowEngine, frontends, 0);
		for (var target : frontends) {
			if (stopped()) {
				return any;
			}
			any |= palette(target);
		}
		return any;
	}

	/** @return whether the palette of a target was computed, not found on disk nor in memory */
	private static boolean palette(DatabaseObject target) throws Exception {
		if (!awaitQuiet(currentEpoch())) {
			return false;
		}
		var started = System.nanoTime();
		FlowEngineBridge.probing("authoringPalette", () -> FlowStudioSupport.paletteCategories(target));
		return !FlowEngineBridge.probeAnswered()
				&& System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(FlowAuthoringDiskCache.MIN_COMPUTE_MILLIS);
	}

	/** The pages and layouts of the frontends of a Flow engine, as projected in its tree. */
	private static void frontendTargets(DatabaseObject parent, List<DatabaseObject> targets, int depth) throws Exception {
		if (depth > 8 || targets.size() >= FRONTEND_PALETTES_LIMIT) {
			return;
		}
		for (var child : parent.getDatabaseObjectChildren()) {
			if (!(child instanceof FlowVirtualObject virtual)) {
				continue;
			}
			var path = virtual.getVirtualPath();
			if (depth == 0 && !"frontends".equals(path)) {
				continue;
			}
			var type = virtual.getVirtualType();
			if (("page".equals(type) || "layout".equals(type)) && FlowStudioSupport.isFlowPaletteTarget(virtual)) {
				targets.add(virtual);
				if (targets.size() >= FRONTEND_PALETTES_LIMIT) {
					return;
				}
			}
			frontendTargets(virtual, targets, depth + 1);
		}
	}

	private static boolean stopped() {
		return Thread.currentThread().isInterrupted();
	}

	private static long currentEpoch() {
		synchronized (lock) {
			return epoch;
		}
	}

	/** Must hold the lock. */
	private static ExecutorService executor() {
		if (executor == null) {
			executor = Executors.newSingleThreadExecutor(runnable -> {
				var thread = new Thread(runnable, "AuthoringWarmup");
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			});
		}
		return executor;
	}

	private static void info(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.info(message);
		}
	}

	private static void debug(String message, Throwable t) {
		if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
			Engine.logEngine.debug(message, t);
		}
	}

	/** For the tests: forgets what was prepared and opened, as a new start. */
	static void reset() {
		stop();
		computed.set(0);
		found.set(0);
		studioCalls.set(0);
	}
}
