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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.ProjectLibraries;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/**
 * Prepares in the background the runtimes of the loaded projects that have Flows, as their first requests would
 * prepare them, so that these requests do not wait for it: for each Flow, its input contract and its run plan, with the
 * runtime of its project (the shared runtime on a stateless server), its block catalog, the compiled implementations
 * of its blocks and the modules they need, as the Java beans of the projects are loaded at the start of the engine.
 * Nothing runs: no request, no context, no session, no Flow executed. The classes generated for the compiled code are
 * kept on disk as by any compilation (javascript.bytecode_cache).
 * <p>
 * When: once the projects are loaded at the start of a server ({@link #projectsLoaded}), then when a project is loaded
 * again (deployed, imported, reloaded: {@link #projectLoaded}), for it and for the loaded projects whose runtime uses
 * it, those referencing it directly or through other projects (deploying the provider of the runtimes prepares those of
 * every project using it). In both cases, the projects referenced by the fewest of them come first: the applications
 * before the libraries they use, whose own Flows are less likely requested. A project already prepared costs little:
 * its plans are found prepared.
 * <p>
 * How: one thread of low priority, one project at a time, one Flow at a time, never delaying the start of the engine.
 * A request needing a plan this thread is preparing waits for it instead of preparing it again, a request needing
 * another plan prepares it at the same time ({@link FlowRuntimeApi#prepare}). A project requested while waiting for
 * its preparation is prepared next, the project in progress giving way to it after its current Flow
 * ({@link #requested}): its other Flows are prepared while its first requests prepare theirs. The preparation of a project replaced
 * meanwhile stops, as its new version is prepared next; a project whose runtime changed during its preparation (a
 * new generation of its libraries, a project loaded again) is prepared again once. Stopped with the engine, between two
 * Flows. A Flow that cannot be prepared is logged and skipped, its first request reports the error as before.
 * <p>
 * Engine property javascript.runtime_warmup: auto (default) on a server, not in the Studio; on: also in the Studio;
 * off: never. Never in the command line mode.
 */
public final class FlowRuntimeWarmup {
	/** The preparation of a project, replaced by the tests. */
	interface ProjectPass {
		/** @return whether the runtime of the project changed during its preparation: it is prepared again once */
		boolean prepare(String project, Cancellation cancellation) throws Exception;
	}

	/** Tells a pass to stop, between two Flows. */
	interface Cancellation {
		boolean cancelled();

		/** @return whether a request asks for another project: the pass gives way to it, after one Flow at least */
		default boolean givingWay() {
			return false;
		}
	}

	static volatile ProjectPass pass = FlowRuntimeWarmup::prepareProject;
	/** The loaded projects whose runtime uses a project (references, transitively), replaced by the tests. */
	static volatile Function<String, Collection<String>> dependents = FlowRuntimeWarmup::loadedDependents;
	/** The order of a batch of projects to prepare, replaced by the tests: those the fewest of them reference first. */
	static volatile Function<List<String>, List<String>> ordering = FlowRuntimeWarmup::leastReferencedFirst;

	private static final Object lock = new Object();
	/** The projects to prepare, in order, and whether those whose runtime uses them follow. */
	private static final LinkedHashMap<String, Boolean> pending = new LinkedHashMap<>();
	/** The projects prepared again once already since their last load. */
	private static final Set<String> retried = new HashSet<>();
	/** The loaded projects seen with Flows: those a project loaded again may concern. */
	private static final Set<String> known = new LinkedHashSet<>();
	private static ExecutorService executor;
	/** The projects of a server are prepared once all of them are loaded at the start. */
	private static boolean released;
	private static boolean draining;
	/** Projects are pending: a request of one of them moves it first ({@link #requested}). */
	private static volatile boolean waiting;
	/** A project was requested while another one is being prepared: that one gives way after its current Flow. */
	private static volatile boolean giveWay;
	private static String current;
	/** Changed by each stop: a pass of a previous start stops. */
	private static long epoch;
	/** The projects with Flows prepared, for the summary of a series of preparations. */
	private static final AtomicInteger preparedProjects = new AtomicInteger();

	private FlowRuntimeWarmup() {
	}

	/** @return whether the runtimes are prepared in advance, as the engine property tells */
	static boolean enabled() {
		if (Engine.isCliMode()) {
			return false;
		}
		String value;
		try {
			value = EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP);
		} catch (Exception notInitialized) {
			return false;
		}
		return switch (value == null ? "auto" : value.trim().toLowerCase()) {
		case "off", "false" -> false;
		case "on", "true" -> true;
		default -> !Engine.isStudioMode();
		};
	}

	/**
	 * A project is loaded (at the start, deployed, imported or reloaded): its runtime is prepared, then those of the
	 * loaded projects using it. At the start of a server, nothing is prepared before all the projects are loaded.
	 */
	public static void projectLoaded(Project project) {
		if (project == null || !enabled()) {
			return;
		}
		var name = project.getName();
		synchronized (lock) {
			if (Engine.isStudioMode()) {
				// no loading of all the projects at the start: each one opens on its own
				released = true;
			}
			retried.remove(name);
			// at the start, every project comes: those using it are already there
			pending.merge(name, released, Boolean::logicalOr);
			waiting = true;
			schedule();
		}
	}

	/**
	 * A request runs a Flow of a project: when the project is waiting for its preparation, it is prepared next, the
	 * preparation of another project in progress giving way to it after its current Flow (then going on, its prepared
	 * Flows found prepared). Its other Flows are then prepared while its first requests prepare those they need.
	 */
	public static void requested(Project project) {
		if (!waiting || project == null) {
			return;
		}
		var name = project.getName();
		synchronized (lock) {
			var withDependents = pending.remove(name);
			if (withDependents == null) {
				return;
			}
			var others = new LinkedHashMap<>(pending);
			pending.clear();
			pending.put(name, withDependents);
			pending.putAll(others);
			if (current != null && !current.equals(name)) {
				giveWay = true;
			}
		}
	}

	/**
	 * The projects are loaded at the start of a server: their runtimes are prepared from now on, those referenced by the
	 * fewest loaded projects first (the applications, then the libraries, the provider of the runtimes last), in their
	 * load order otherwise.
	 */
	public static void projectsLoaded() {
		List<String> loaded;
		synchronized (lock) {
			loaded = new ArrayList<>(pending.keySet());
		}
		// the applications before the libraries they use, the provider of the runtimes last
		var ordered = order(loaded);
		synchronized (lock) {
			putFirst(ordered, true);
			released = true;
			if (enabled() && !pending.isEmpty() && Engine.logEngine != null) {
				Engine.logEngine.info("(FlowRuntimeWarmup) Preparing in the background the runtimes of the projects loaded at the start");
			}
			schedule();
		}
	}

	/** The engine stops: the preparation stops, between two Flows, and nothing is prepared until the next start. */
	public static void stop() {
		ExecutorService stopped;
		synchronized (lock) {
			epoch++;
			pending.clear();
			waiting = false;
			giveWay = false;
			retried.clear();
			known.clear();
			released = false;
			draining = false;
			current = null;
			stopped = executor;
			executor = null;
			lock.notifyAll();
		}
		if (stopped != null) {
			stopped.shutdownNow();
		}
	}

	/** For the tests and diagnostics: waits until nothing is pending nor being prepared. */
	static boolean awaitIdle(long timeout, TimeUnit unit) throws InterruptedException {
		var deadline = System.nanoTime() + unit.toNanos(timeout);
		synchronized (lock) {
			while (draining || released && !pending.isEmpty()) {
				var left = deadline - System.nanoTime();
				if (left <= 0) {
					return false;
				}
				TimeUnit.NANOSECONDS.timedWait(lock, left);
			}
			return true;
		}
	}

	/** @return the project being prepared, null when none */
	static String current() {
		synchronized (lock) {
			return current;
		}
	}

	/** Must hold the lock. */
	private static void schedule() {
		if (!released || draining || pending.isEmpty()) {
			return;
		}
		draining = true;
		var run = epoch;
		executor().execute(() -> drain(run));
	}

	private static void drain(long run) {
		Log4jHelper.mdcClear();
		class Signals implements Cancellation {
			/** The current pass saw a project requested and gave way to it. */
			boolean gaveWay;

			@Override
			public boolean cancelled() {
				return Thread.currentThread().isInterrupted() || !enabled() || stale(run);
			}

			@Override
			public boolean givingWay() {
				return gaveWay = gaveWay || giveWay;
			}
		}
		var cancellation = new Signals();
		var started = System.nanoTime();
		var projects = preparedProjects.get();
		try {
			for (;;) {
				String name;
				boolean withDependents;
				synchronized (lock) {
					if (epoch != run) {
						return;
					}
					var next = pending.entrySet().iterator();
					if (!next.hasNext() || cancellation.cancelled()) {
						if (!enabled()) {
							pending.clear();
						}
						waiting = !pending.isEmpty();
						draining = false;
						lock.notifyAll();
						var count = preparedProjects.get() - projects;
						if (count > 0 && !next.hasNext()) {
							info("(FlowRuntimeWarmup) Runtimes of " + count + " project" + (count > 1 ? "s" : "") + " prepared in "
									+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms");
						}
						return;
					}
					var entry = next.next();
					next.remove();
					name = entry.getKey();
					withDependents = entry.getValue();
					if (!withDependents) {
						current = name;
						giveWay = false;
						cancellation.gaveWay = false;
					}
					waiting = !pending.isEmpty();
				}
				if (withDependents) {
					// a project loaded again: it and the projects using it, the least referenced first
					var batch = new ArrayList<String>();
					batch.add(name);
					try {
						for (var dependent : dependents.apply(name)) {
							if (!batch.contains(dependent)) {
								batch.add(dependent);
							}
						}
					} catch (Throwable t) {
						warn("(FlowRuntimeWarmup) Unable to find the projects using the project " + name, t);
					}
					var ordered = order(batch);
					synchronized (lock) {
						if (epoch != run) {
							return;
						}
						putFirst(ordered, false);
						waiting = true;
					}
					continue;
				}
				var again = false;
				try {
					again = pass.prepare(name, cancellation);
				} catch (Throwable t) {
					if (!cancellation.cancelled()) {
						warn("(FlowRuntimeWarmup) Unable to prepare the runtime of the project " + name, t);
					}
				}
				var gaveWay = cancellation.gaveWay;
				synchronized (lock) {
					if (epoch != run) {
						return;
					}
					current = null;
					giveWay = false;
					if (gaveWay) {
						// after the project requested, first in the queue: the rest of this one, then the others
						var first = pending.entrySet().iterator();
						var requested = first.hasNext() ? first.next() : null;
						var others = new LinkedHashMap<>(pending);
						pending.clear();
						if (requested != null) {
							pending.put(requested.getKey(), requested.getValue());
						}
						pending.merge(name, false, Boolean::logicalOr);
						others.forEach((other, flag) -> pending.merge(other, flag, Boolean::logicalOr));
					} else if (again && retried.add(name)) {
						pending.putIfAbsent(name, false);
					}
					waiting = !pending.isEmpty();
					lock.notifyAll();
				}
			}
		} finally {
			synchronized (lock) {
				if (epoch == run) {
					draining = false;
					current = null;
					lock.notifyAll();
					// work queued while this pass was stopping, without the engine stopping
					if (!Thread.currentThread().isInterrupted()) {
						schedule();
					}
				}
			}
		}
	}

	/**
	 * Must hold the lock: these projects first, in this order, then the other pending ones.
	 *
	 * @param onlyPending to leave out those that are not pending
	 */
	private static void putFirst(List<String> names, boolean onlyPending) {
		var others = new LinkedHashMap<>(pending);
		pending.clear();
		for (var name : names) {
			var withDependents = others.remove(name);
			if (withDependents != null || !onlyPending) {
				pending.put(name, withDependents != null && withDependents);
			}
		}
		pending.putAll(others);
	}

	private static List<String> order(List<String> names) {
		try {
			return ordering.apply(names);
		} catch (Throwable t) {
			debug("(FlowRuntimeWarmup) Projects not ordered", t);
			return names;
		}
	}

	private static boolean stale(long run) {
		synchronized (lock) {
			return epoch != run;
		}
	}

	/** The preparation of a loaded project: each of its Flows, one at a time. */
	private static boolean prepareProject(String name, Cancellation cancellation) throws Exception {
		var app = Engine.theApp;
		var manager = app == null ? null : app.databaseObjectsManager;
		var project = manager == null ? null : manager.getLoadedProjectByName(name);
		var flows = project == null ? List.<Flow>of() : flows(project);
		synchronized (lock) {
			if (flows.isEmpty()) {
				known.remove(name);
			} else {
				known.add(name);
			}
		}
		if (flows.isEmpty()) {
			return false;
		}
		var started = System.nanoTime();
		ClassLoader generation = null;
		try {
			// the generation its next requests use, a library of a referenced project deployed again rebuilt now
			generation = ProjectLibraries.checkedClassLoader(project);
		} catch (Exception e) {
			debug("(FlowRuntimeWarmup) Libraries of the project " + name + " not checked", e);
		}
		var codeRevision = FlowEngineBridge.cacheGeneration();
		var dataRevision = FlowEngineBridge.dataGeneration();
		var bridge = new FlowEngineBridge();
		var failures = new ArrayList<String>();
		var prepared = 0;
		var plans = true;
		for (var flow : flows) {
			if (cancellation.cancelled()) {
				debug("(FlowRuntimeWarmup) Preparation of the project " + name + " stopped", null);
				return false;
			}
			if (manager.getLoadedProjectByName(name) != project) {
				// replaced: its new version is prepared next
				debug("(FlowRuntimeWarmup) Project " + name + " replaced during its preparation", null);
				return false;
			}
			var errors = new ArrayList<String>();
			try {
				// the input contract a request of the Flow reads first (Flow.synchronizeFlowInputs), shared on a server
				if (!Engine.isStudioMode() && flow.getFlowSource().contains("_flow")) {
					var inputs = bridge.syncInputs(flow);
					if (!inputs.optBoolean("ok", false)) {
						errors.add("inputs: " + errorText(inputs));
					}
				}
			} catch (Exception e) {
				errors.add("inputs: " + message(e));
				debug("(FlowRuntimeWarmup) Failure of the input contract of the Flow " + flow.getQName(), e);
			}
			try {
				var outcome = planOutcome(bridge.preloadPlan(flow));
				if (outcome == null) {
					// a provider preparing no plan in advance: its catalog and its compiler are, once for the project
					plans = false;
				} else if (!outcome.isEmpty()) {
					errors.add("plan: " + outcome);
				}
			} catch (Exception e) {
				errors.add("plan: " + message(e));
				debug("(FlowRuntimeWarmup) Failure of the plan of the Flow " + flow.getQName(), e);
			}
			if (cancellation.cancelled()) {
				return false;
			}
			if (errors.isEmpty()) {
				prepared++;
			} else {
				failures.add(flow.getName());
				warn("(FlowRuntimeWarmup) Unable to prepare the Flow " + flow.getQName() + ": " + String.join("; ", errors), null);
			}
			if (!plans) {
				break;
			}
			if (cancellation.givingWay()) {
				debug("(FlowRuntimeWarmup) Preparation of the project " + name + " giving way to a project requested", null);
				return false;
			}
		}
		preparedProjects.incrementAndGet();
		var ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
		info("(FlowRuntimeWarmup) Project " + name + ": " + (plans ? prepared + "/" + flows.size() + " Flows prepared"
				: "catalog prepared, its provider prepares no Flow plan in advance") + " in " + ms + " ms"
				+ (failures.isEmpty() ? "" : ", not prepared: " + String.join(", ", failures)));
		return codeRevision != FlowEngineBridge.cacheGeneration() || dataRevision != FlowEngineBridge.dataGeneration()
				|| generation != null && generation != project.getProjectClassLoader();
	}

	/** @return the Flows of a project, in their order */
	static List<Flow> flows(Project project) {
		var flows = new ArrayList<Flow>();
		for (var sequence : project.getSequencesList()) {
			if (sequence instanceof Flow flow) {
				flows.add(flow);
			}
		}
		return flows;
	}

	/**
	 * @param answer the answer of the provider to a preload of the plan of one Flow
	 * @return an empty text when the plan is prepared, the error when it cannot be, null when the provider prepares no
	 *         plan in advance
	 */
	static String planOutcome(JSONObject answer) {
		if (!answer.optBoolean("ok", false)) {
			return errorText(answer);
		}
		var plans = answer.optJSONArray("plans");
		if (plans == null) {
			return null;
		}
		var plan = plans.optJSONObject(0);
		if (plan == null) {
			return "no plan in the answer of the provider";
		}
		return plan.optBoolean("ok", false) ? "" : errorText(plan);
	}

	private static String message(Exception e) {
		var cause = e.getCause() != null && e instanceof EngineException ? e.getCause() : e;
		return cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage();
	}

	private static String errorText(JSONObject answer) {
		var error = answer.optJSONObject("error");
		if (error == null) {
			return answer.toString();
		}
		var code = error.optString("code", "");
		var message = error.optString("message", error.toString());
		return code.isEmpty() || message.startsWith(code) ? message : code + ": " + message;
	}

	/**
	 * @return the loaded projects seen with Flows whose runtime uses the project: they reference it, directly or
	 *         through other loaded projects, or their Flow engine is one of its own
	 */
	private static Collection<String> loadedDependents(String name) {
		var app = Engine.theApp;
		var manager = app == null ? null : app.databaseObjectsManager;
		if (manager == null) {
			return List.of();
		}
		List<String> candidates;
		synchronized (lock) {
			candidates = new ArrayList<>(known);
		}
		var using = new ArrayList<String>();
		for (var candidate : candidates) {
			if (candidate.equals(name)) {
				continue;
			}
			var project = manager.getLoadedProjectByName(candidate);
			if (project == null) {
				synchronized (lock) {
					known.remove(candidate);
				}
			} else if (uses(project, name, manager, new HashSet<>())) {
				using.add(candidate);
			}
		}
		return using;
	}

	private static boolean uses(Project project, String target, DatabaseObjectsManager manager, Set<String> seen) {
		if (!seen.add(project.getName()) || seen.size() > 256) {
			return false;
		}
		var names = referencedNames(project);
		if (names.contains(target)) {
			return true;
		}
		for (var referenced : names) {
			var next = referenced == null || referenced.isBlank() ? null : manager.getLoadedProjectByName(referenced);
			if (next != null && uses(next, target, manager, seen)) {
				return true;
			}
		}
		return false;
	}

	/** @return the projects a project references, and the one of the provider of its Flow engine */
	private static Set<String> referencedNames(Project project) {
		var names = new LinkedHashSet<String>();
		var flowEngine = project.getFlowEngine();
		var engineQName = flowEngine == null ? null : flowEngine.getEngineQName();
		if (engineQName != null && engineQName.indexOf('.') > 0) {
			names.add(engineQName.substring(0, engineQName.indexOf('.')));
		}
		for (var reference : project.getReferenceList()) {
			if (reference instanceof ProjectSchemaReference schemaReference) {
				names.add(schemaReference.getParser().getProjectName());
			}
		}
		names.remove(project.getName());
		return names;
	}

	/**
	 * @return the projects, those referenced by the fewest of them first, in their order otherwise: the applications
	 *         before the libraries they use, whose own Flows are less likely requested, the provider of the runtimes last
	 */
	static List<String> leastReferencedFirst(List<String> names) {
		var app = Engine.theApp;
		var manager = app == null ? null : app.databaseObjectsManager;
		if (manager == null || names.size() < 2) {
			return names;
		}
		var referencing = new java.util.HashMap<String, Integer>();
		for (var name : names) {
			var project = manager.getLoadedProjectByName(name);
			if (project != null) {
				for (var referenced : referencedNames(project)) {
					referencing.merge(referenced, 1, Integer::sum);
				}
			}
		}
		var ordered = new ArrayList<>(names);
		ordered.sort(java.util.Comparator.comparingInt(name -> referencing.getOrDefault(name, 0)));
		return ordered;
	}

	/** Must hold the lock. */
	private static ExecutorService executor() {
		if (executor == null) {
			executor = Executors.newSingleThreadExecutor(runnable -> {
				var thread = new Thread(runnable, "RuntimeWarmup");
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

	private static void warn(String message, Throwable t) {
		if (Engine.logEngine != null) {
			if (t == null) {
				Engine.logEngine.warn(message);
			} else {
				Engine.logEngine.warn(message, t);
			}
		}
	}

	private static void debug(String message, Throwable t) {
		if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
			Engine.logEngine.debug(message, t);
		}
	}
}
