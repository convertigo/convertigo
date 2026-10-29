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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/**
 * Prepares in the background the authoring responses the Studio asks first that the disk cache does not hold yet
 * ({@link FlowAuthoringDiskCache}), so that the Studio finds them ready. What to prepare is the runtime provider's
 * decision: for each project and stage, it answers a plan ({@value #PLAN_METHOD}), an ordered list of its own requests,
 * each with its method, its options over the request the Studio sends for that method (of the project, or of one of its
 * Flows), a priority and the items of the plan it runs after. This class knows none of them: a new kind of response
 * needs a change of the provider only. A response on disk is neither computed nor read: a probe of its key tells it is
 * there ({@link FlowEngineBridge#probing}); a method whose responses are not kept on disk runs each time.
 * <p>
 * When: the stage {@value #STUDIO} of every loaded project with a Flow engine from the first call of the web Studio
 * after a start, the listing of its projects ({@link #studioOpened}), not at the start of the engine: after a restart
 * the disk holds these responses already and nothing is computed, after a deployment only those of the projects changed
 * are, and only on a server whose Studio is used, the processor of a small shared server staying for its requests
 * otherwise. The stage {@value #PROJECT} of a project when it is opened in the Studio ({@link #projectOpened}), first.
 * A project loaded again (deployed, imported, reloaded) once the Studio is opened has its stage {@value #STUDIO}, and
 * that of the other projects, which may use it, planned again.
 * <p>
 * A provider without plan (an older version, a failure) gets the tree of the project at the stage {@value #STUDIO}, the
 * request of its first expansion, and nothing at the stage {@value #PROJECT}.
 * <p>
 * How: one thread of low priority, one request at a time, each started once no other call of the runtime providers
 * ran for {@value #QUIET_MILLIS} ms: a call of the Studio or a request arriving meanwhile waits at most for the response
 * in progress (the same response joins it). The works of the projects opened in the Studio come first, the last opened
 * first, then those of the stage {@value #STUDIO}; among them, the plans to ask, then the items by priority, the higher
 * first, whatever their project (the trees of every project before their palettes, when the providers say so), in the
 * order of their plan for the same priority. An item runs after those it names, never before them, and is skipped when
 * one of them failed. Stopped with the engine, between two requests. The calls of a plan never invalidate a cache: a
 * plan prepares, it changes nothing ({@link FlowEngineBridge#inBackground}).
 * <p>
 * Engine property javascript.authoring_warmup: auto (default) on a server with the web Studio enabled, not in the
 * Studio; on: also in the Studio; off: never. Never without the disk cache, nor in the command line mode.
 */
public final class FlowAuthoringWarmup {
	/** A request is prepared once no call of the Studio nor of the runtime providers ran for this time. */
	static final long QUIET_MILLIS = 1000;
	/** The method of the runtime providers answering the warm-up plan of a project for a stage. */
	static final String PLAN_METHOD = "authoringWarmupPlan";
	/** The stage of the plan of a project when the Studio lists the projects. */
	static final String STUDIO = "studio";
	/** The stage of the plan of a project when it is opened (expanded) in the Studio. */
	static final String PROJECT = "project";
	/** The items of a plan kept, at most. */
	static final int PLAN_LIMIT = 256;
	/** The id of the item of the plan without provider plan: the tree of the project. */
	static final String FALLBACK_TREE = "tree";

	/**
	 * A request of a plan.
	 *
	 * @param flow the Flow of the project whose request it is, empty for the project
	 * @param request its options, as JSON text; null for the request of the Studio itself (the tree of a provider without plan)
	 * @param after the ids of the items of the same plan it runs after
	 */
	record Item(String project, String id, String method, String flow, String request, int priority, List<String> after) {
		Item {
			flow = flow == null ? "" : flow;
			after = after == null ? List.of() : List.copyOf(after);
		}
	}

	/**
	 * What is pending: the plan of a project for a stage to ask (item null), or an item of a plan.
	 *
	 * @param rank the order of the opening of its project in the Studio, the last one higher, 0 for the stage {@value #STUDIO}
	 * @param priority the priority of its item, never higher than that of the items it runs after; a plan to ask comes first
	 * @param seq the order it was added in
	 */
	record Work(String project, String stage, Item item, int rank, int priority, long seq) {
		boolean same(Work other) {
			return project.equals(other.project) && stage.equals(other.stage) && Objects.equals(item, other.item);
		}
	}

	/** The order of the works: the projects opened last first, then the higher priority, then the order they were added in. */
	private static final Comparator<Work> ORDER = Comparator.comparingInt(Work::rank).reversed()
			.thenComparing(Comparator.comparingInt(Work::priority).reversed()).thenComparingLong(Work::seq);

	/** The plan of a project for a stage, replaced by the tests. */
	interface Planner {
		List<Item> plan(String project, String stage) throws Exception;
	}

	/** The preparation of an item, replaced by the tests. */
	interface Preparation {
		/** @return whether the response was computed, not found on disk */
		boolean prepare(Item item) throws Exception;
	}

	static final Planner PLANNER = FlowAuthoringWarmup::providerPlan;
	static volatile Planner planner = PLANNER;
	static final Preparation PREPARATION = FlowAuthoringWarmup::prepareItem;
	static volatile Preparation preparation = PREPARATION;
	/** The loaded projects with a Flow engine, in their load order, replaced by the tests. */
	static final Supplier<List<String>> LOADED_PROJECTS = FlowAuthoringWarmup::flowProjects;
	static volatile Supplier<List<String>> projects = LOADED_PROJECTS;
	/** The order of a batch of projects, replaced by the tests: those the fewest of them reference first. */
	static volatile Function<List<String>, List<String>> ordering = FlowRuntimeWarmup::leastReferencedFirst;

	private static final Object lock = new Object();
	private static final List<Work> pending = new ArrayList<>();
	/** The number of works added, and of projects opened, since the start. */
	private static long added;
	private static int openings;
	/** The projects whose stage {@value #PROJECT} was planned since their last load. */
	private static final Set<String> opened = new HashSet<>();
	/** The ids of the items of the last plan of each project and stage that failed or were skipped: their dependents are skipped. */
	private static final Map<String, Set<String>> failed = new HashMap<>();
	private static boolean studio;
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
	 * The web Studio lists the projects: the first time after a start, the stage {@value #STUDIO} of the loaded projects
	 * with a Flow engine is planned, those referenced by the fewest of them first (the applications before the libraries
	 * they use).
	 */
	public static void studioOpened() {
		synchronized (lock) {
			if (studio) {
				return;
			}
		}
		if (!enabled()) {
			return;
		}
		var names = projects.get();
		synchronized (lock) {
			if (studio) {
				return;
			}
			studio = true;
			for (var name : ordering.apply(names)) {
				add(name, STUDIO, null, 0, Integer.MAX_VALUE);
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

	/** A project is opened (expanded) in the Studio: its stage {@value #PROJECT} is planned next, once per load. */
	public static void projectOpened(String name) {
		if (name == null || name.isBlank()) {
			return;
		}
		synchronized (lock) {
			if (opened.contains(name)) {
				return;
			}
		}
		if (!enabled()) {
			return;
		}
		synchronized (lock) {
			if (!opened.add(name)) {
				return;
			}
			add(name, PROJECT, null, ++openings, Integer.MAX_VALUE);
			schedule();
		}
	}

	/**
	 * A project is loaded again (deployed, imported, reloaded): its stage {@value #PROJECT} is planned again when it is
	 * opened, and once the Studio is opened the stage {@value #STUDIO} of the projects, which may use it, again.
	 */
	static void projectLoaded(String name) {
		synchronized (lock) {
			opened.remove(name);
			if (!studio) {
				return;
			}
		}
		if (!enabled()) {
			return;
		}
		var names = projects.get();
		synchronized (lock) {
			if (!studio) {
				return;
			}
			for (var project : ordering.apply(names)) {
				add(project, STUDIO, null, 0, Integer.MAX_VALUE);
			}
			schedule();
		}
	}

	/** The engine stops: the preparation stops, between two requests, and starts again at the next opening of the Studio. */
	public static void stop() {
		ExecutorService stopped;
		synchronized (lock) {
			epoch++;
			pending.clear();
			added = 0;
			openings = 0;
			opened.clear();
			failed.clear();
			studio = false;
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

	/** Must hold the lock: adds a work in its order, unless it is pending already (it then keeps the earlier rank of both). */
	private static void add(String project, String stage, Item item, int rank, int priority) {
		var work = new Work(project, stage, item, rank, priority, added++);
		for (var i = 0; i < pending.size(); i++) {
			var other = pending.get(i);
			if (other.same(work)) {
				if (other.rank() >= rank) {
					return;
				}
				pending.remove(i);
				break;
			}
		}
		pending.add(work);
		pending.sort(ORDER);
	}

	/** For the tests: the works pending, in their order. */
	static List<Work> pendingWorks() {
		synchronized (lock) {
			return List.copyOf(pending);
		}
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
				Work work;
				synchronized (lock) {
					if (epoch != run || pending.isEmpty()) {
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
					work = pending.remove(0);
				}
				if (work.item() == null) {
					plan(run, work);
				} else {
					prepare(work);
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

	/** Asks the plan of a project for a stage: its items are prepared next, in their order. */
	private static void plan(long run, Work work) {
		List<Item> items;
		var started = System.nanoTime();
		try {
			items = FlowEngineBridge.inBackground(() -> planner.plan(work.project(), work.stage()));
		} catch (Throwable t) {
			if (FlowRuntimeApi.interruption(t) || Thread.currentThread().isInterrupted()) {
				return;
			}
			debug("(FlowAuthoringWarmup) Unable to plan the stage " + work.stage() + " of the project " + work.project(), t);
			return;
		}
		var ordered = ordered(items == null ? List.of() : items);
		debug("(FlowAuthoringWarmup) Plan of the stage " + work.stage() + " of the project " + work.project() + ": " + ordered.size()
				+ " item" + (ordered.size() == 1 ? "" : "s") + " in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + " ms", null);
		synchronized (lock) {
			if (epoch != run) {
				return;
			}
			failed.remove(planKey(work));
			// never before the items it runs after: no higher priority than theirs
			var effective = new HashMap<String, Integer>();
			for (var item : ordered) {
				var priority = item.priority();
				for (var id : item.after()) {
					priority = Math.min(priority, effective.getOrDefault(id, priority));
				}
				effective.put(item.id(), priority);
				add(work.project(), work.stage(), item, work.rank(), priority);
			}
		}
	}

	private static String planKey(Work work) {
		return work.project() + "\n" + work.stage();
	}

	/** Prepares an item, unless one of the items it runs after failed. */
	private static void prepare(Work work) {
		var item = work.item();
		synchronized (lock) {
			var failures = failed.get(planKey(work));
			if (failures != null && item.after().stream().anyMatch(failures::contains)) {
				failures.add(item.id());
				debug("(FlowAuthoringWarmup) " + describe(item) + " skipped: an item it runs after failed", null);
				return;
			}
		}
		try {
			var itemStarted = System.nanoTime();
			var wasComputed = FlowEngineBridge.inBackground(() -> preparation.prepare(item));
			if (wasComputed) {
				computed.incrementAndGet();
				debug("(FlowAuthoringWarmup) " + describe(item) + " prepared in "
						+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - itemStarted) + " ms", null);
			} else {
				found.incrementAndGet();
			}
		} catch (Throwable t) {
			if (FlowRuntimeApi.interruption(t) || Thread.currentThread().isInterrupted()) {
				Thread.currentThread().interrupt();
				return;
			}
			synchronized (lock) {
				failed.computeIfAbsent(planKey(work), key -> new HashSet<>()).add(item.id());
			}
			debug("(FlowAuthoringWarmup) Unable to prepare " + describe(item), t);
		}
	}

	private static String describe(Item item) {
		return item.method() + " " + item.id() + " of the project " + item.project()
				+ (item.flow().isEmpty() ? "" : " (Flow " + item.flow() + ")");
	}

	/**
	 * @return the items in the order they run: the higher priority first, the order of the plan for the same priority,
	 *         each after the items it names; those naming an unknown item or in a cycle are dropped, as the duplicates
	 */
	static List<Item> ordered(List<Item> items) {
		var byId = new LinkedHashMap<String, Item>();
		for (var item : items) {
			if (item != null && byId.size() < PLAN_LIMIT) {
				byId.putIfAbsent(item.id(), item);
			}
		}
		var sorted = new ArrayList<>(byId.values());
		// stable: the order of the plan for the same priority
		sorted.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
		var placed = new LinkedHashSet<String>();
		var result = new ArrayList<Item>(sorted.size());
		for (boolean progress = true; progress;) {
			progress = false;
			for (var iterator = sorted.iterator(); iterator.hasNext();) {
				var item = iterator.next();
				if (placed.containsAll(item.after())) {
					iterator.remove();
					placed.add(item.id());
					result.add(item);
					progress = true;
					// the first ready one in the order, then the order again: an item never jumps over a higher one
					break;
				}
			}
		}
		if (!sorted.isEmpty()) {
			debug("(FlowAuthoringWarmup) " + sorted.size() + " item" + (sorted.size() == 1 ? "" : "s")
					+ " of a plan dropped: they run after an unknown item or in a cycle", null);
		}
		return result;
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

	/** The plan the provider of a project answers for a stage, or the plan without provider plan. */
	private static List<Item> providerPlan(String name, String stage) throws Exception {
		var project = loadedProject(name);
		var flowEngine = project == null ? null : project.getFlowEngine();
		if (flowEngine == null) {
			return List.of();
		}
		JSONObject answer;
		try {
			answer = new FlowEngineBridge().authoringWarmupPlan(flowEngine, stage);
		} catch (Exception e) {
			if (FlowRuntimeApi.interruption(e)) {
				throw e;
			}
			debug("(FlowAuthoringWarmup) No plan from the provider of the project " + name + ": " + e.getMessage(), null);
			answer = null;
		}
		var items = answer == null || !answer.optBoolean("ok", false) ? null : items(name, answer.optJSONArray("items"));
		return items != null ? items : fallback(name, stage);
	}

	/** @return the items of the plan a provider answered, null when it is not a plan */
	static List<Item> items(String project, JSONArray array) {
		if (array == null) {
			return null;
		}
		var items = new ArrayList<Item>();
		for (var i = 0; i < array.length() && items.size() < PLAN_LIMIT; i++) {
			var entry = array.optJSONObject(i);
			var method = entry == null ? "" : entry.optString("method", "").trim();
			if (method.isEmpty()) {
				continue;
			}
			var options = entry.optJSONObject("request");
			var after = new ArrayList<String>();
			var names = entry.optJSONArray("after");
			for (var j = 0; names != null && j < names.length(); j++) {
				var id = names.optString(j, "");
				if (!id.isEmpty()) {
					after.add(id);
				}
			}
			items.add(new Item(project, entry.optString("id", "item" + i), method, entry.optString("flow", ""),
					options == null ? "{}" : options.toString(), entry.optInt("priority", 0), after));
		}
		return items;
	}

	/** The plan without provider plan: the tree of the project at the stage {@value #STUDIO}, its first expansion. */
	static List<Item> fallback(String project, String stage) {
		return STUDIO.equals(stage) ? List.of(new Item(project, FALLBACK_TREE, "describeTree", "", null, 0, List.of()))
				: List.of();
	}

	/**
	 * The preparation of an item: its response probed on disk, computed when missing (and kept by the bridge).
	 *
	 * @throws Exception when it cannot be prepared, or its provider answers a failure
	 */
	private static boolean prepareItem(Item item) throws Exception {
		var project = loadedProject(item.project());
		var flowEngine = project == null ? null : project.getFlowEngine();
		if (flowEngine == null) {
			return false;
		}
		Flow flow = null;
		if (!item.flow().isEmpty()) {
			for (var candidate : FlowRuntimeWarmup.flows(project)) {
				if (item.flow().equals(candidate.getName())) {
					flow = candidate;
				}
			}
			if (flow == null) {
				throw new IllegalStateException("no Flow " + item.flow());
			}
		}
		var target = flow;
		var started = System.nanoTime();
		var response = FlowEngineBridge.probing(item.method(), () -> item.request() == null
				? new FlowEngineBridge().describeTree(flowEngine)
				: new FlowEngineBridge().planned(flowEngine, target, item.method(), new JSONObject(item.request())));
		if (FlowEngineBridge.probeAnswered()) {
			return false;
		}
		if (response != null && !response.optBoolean("ok", true)) {
			throw new IllegalStateException(failure(response));
		}
		return System.nanoTime() - started >= TimeUnit.MILLISECONDS.toNanos(FlowAuthoringDiskCache.MIN_COMPUTE_MILLIS);
	}

	private static String failure(JSONObject response) {
		var error = response.opt("error");
		try {
			return error instanceof JSONObject object ? object.optString("code", "") + " " + object.optString("message", "")
					: String.valueOf(error);
		} catch (RuntimeException e) {
			return "failure";
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
