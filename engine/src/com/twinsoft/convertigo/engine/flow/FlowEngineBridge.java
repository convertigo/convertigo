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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.flow;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.util.JsonValues;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ProjectClassLoader;
import com.twinsoft.convertigo.engine.ProjectLibraries;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.events.AdminEventBus;
import com.twinsoft.convertigo.engine.events.StudioEvent;
import com.twinsoft.convertigo.engine.events.StudioEventListener;
import com.twinsoft.convertigo.engine.util.RhinoBytecodeCache;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class FlowEngineBridge {

	public static final String DEFAULT_ENGINE_QNAME = "lib_flow_engine.Engine";

	private static final String ENGINE_BASE_PATH = FlowSourceLayout.current().root() + "/";
	private static final Map<String, CachedEngineSource> engineSourceCache = new ConcurrentHashMap<>();
	private static final Map<String, CachedEngineRuntimePool> engineRuntimeCache = new ConcurrentHashMap<>();
	private static final Object engineRuntimeCacheLock = new Object();
	private static final Map<String, CachedMethodResponse> methodResponseCache = new ConcurrentHashMap<>();
	private static final Map<String, String> methodResponseAliases = new ConcurrentHashMap<>();
	private static final Map<String, InvocationStats> invocationStats = new ConcurrentHashMap<>();
	private static final AtomicLong cacheGeneration = new AtomicLong();
	/** Advanced by each invalidation of data caches, of every project or of some of them: the revision it gives them. */
	private static final AtomicLong dataGeneration = new AtomicLong();
	/** The revision given to every project by the last invalidation of the data caches of all of them. */
	private static final AtomicLong commonDataRevision = new AtomicLong();
	/**
	 * The revision given to a project, by canonical directory, by the last invalidation of its data caches or of those
	 * of a project its runtime uses.
	 */
	private static final Map<String, AtomicLong> sourceRevisions = new ConcurrentHashMap<>();
	private static final FlowCompiledScriptCache compiledModules = new FlowCompiledScriptCache(256);
	private static final FlowSourceCache serverSources = new FlowSourceCache(2048, 60_000);
	private static final FlowMetadataCache sharedMetadata = new FlowMetadataCache(1024, 16 * 1024 * 1024);
	/**
	 * What providers compute from the content of their sources (a parse, a compiled descriptor), as text: kept across
	 * the invalidations of the data caches, as its keys name the content and the revisions they depend on.
	 */
	private static final FlowMetadataCache sourceMemo = new FlowMetadataCache(8192, 32 * 1024 * 1024);
	private static final LongAdder methodResponseCacheHits = new LongAdder();
	private static final LongAdder methodResponseCacheMisses = new LongAdder();
	private static final LongAdder methodResponseCacheInvalidations = new LongAdder();
	private static final int METHOD_RESPONSE_CACHE_LIMIT = 256;
	/** @param project the canonical directory of the project, empty for none; dataRevision the revision of its data */
	private record SharedRuntimeKey(String provider, String source, String project, ClassLoader loader, long codeRevision, long dataRevision) { }
	/** Its monitor orders their publication and the invalidations of data caches, never held while one is prepared. */
	private static final Map<SharedRuntimeKey, CachedEngineRuntime> sharedRuntimes = new ConcurrentHashMap<>();
	/** The preparations of shared runtimes in progress, by key: removed once published or failed, so a failure is never kept. */
	private static final Map<SharedRuntimeKey, SharedRuntimeFlight> sharedRuntimeFlights = new ConcurrentHashMap<>();
	private static final AtomicInteger sharedRuntimeWaiters = new AtomicInteger();
	private static final ThreadLocal<RuntimeInvocation> currentRuntime = new ThreadLocal<>();
	private static final String[] INVOCATION_BINDINGS = { "context", "__flowProjectDir", "__flowBridgeClassSource",
			"__flowBridgeClassResource", "__flowBridgeInfo", "__flowBridgeRuntimeCacheEnabled", "__flowBridgeRuntimeCacheHit",
			"__flowBridgeRuntimeCacheKey", "__flowBridgeRuntimeCacheGeneration", "__flowBridgeRuntimeCacheSize" };
	private static final int FRONTEND_AUTHORING_SINGLE_FLIGHT_LIMIT = 32;
	private static final Map<String, CompletableFuture<String>> frontendAuthoringFlights = new ConcurrentHashMap<>();
	private static final Object frontendAuthoringFlightsLock = new Object();
	private static final LongAdder frontendAuthoringFlightLeaders = new LongAdder();
	private static final LongAdder frontendAuthoringFlightFollowers = new LongAdder();
	private static final LongAdder frontendAuthoringFlightBypasses = new LongAdder();
	private static final ReentrantLock[] frontendAuthoringLocks = createFrontendAuthoringLocks(32);
	private static final FrontendAuthoringLockState[] frontendAuthoringLockStates = createFrontendAuthoringLockStates(
			frontendAuthoringLocks.length);
	private static final FrontendAuthoringMutationGate[] frontendAuthoringMutationGates = createFrontendAuthoringMutationGates(
			frontendAuthoringLocks.length);

	private record CachedEngineSource(File file, String source, long lastModified, long length) {
		String sourceName() {
			return file.getAbsolutePath() + "#" + lastModified + ":" + length;
		}
	}

	/** @param dataGeneration the data generation of a pooled runtime, the data revision of its project for a shared one */
	private record CachedEngineRuntime(String sourceName, long generation, long dataGeneration, Scriptable scope,
			Scriptable engineObject, FlowRuntimeApi api) {
	}

	/** A preparation of a shared runtime in progress: the thread preparing it, then its runtime for the callers waiting for it. */
	private static final class SharedRuntimeFlight {
		private final Thread owner = Thread.currentThread();
		/** Completed with null when the thread preparing it is interrupted: a caller waiting for it prepares it again. */
		private final CompletableFuture<CachedEngineRuntime> outcome = new CompletableFuture<>();
	}

	private record IdleEngineRuntime(CachedEngineRuntime runtime, long since) { }
	private record RuntimeInvocation(CachedEngineRuntimeLookup lookup, ClassLoader projectGeneration, boolean execution) { }

	private static final class CachedEngineRuntimePool {
		private final String sourceName;
		private final long generation;
		private final ArrayDeque<IdleEngineRuntime> available = new ArrayDeque<>();
		private Scriptable frontendAuthoringScope;
		private int cachedCount;

		private CachedEngineRuntimePool(String sourceName, long generation) {
			this.sourceName = sourceName;
			this.generation = generation;
		}
	}

	private record CachedEngineRuntimeLookup(CachedEngineRuntime runtime, CachedEngineRuntimePool pool, boolean hit,
			boolean pooled, String key, long generation, int size, boolean nested) {
	}

	/** @param root the canonical directory of the project of its request, empty for none; dataRevision the revision of its data */
	private record CachedMethodResponse(String response, long generation, long dataRevision, String root) {
		boolean current() {
			return generation == cacheGeneration.get() && dataRevision == FlowEngineBridge.dataRevision(root);
		}
	}

	private static final class FrontendAuthoringLockState {
		private String engineQName = "";
		private String method = "";
		private String thread = "";
		private long threadId;
		private long acquiredAtMillis;

		private synchronized void acquired(String engineQName, String method) {
			var current = Thread.currentThread();
			this.engineQName = engineQName;
			this.method = method;
			thread = current.getName();
			threadId = System.identityHashCode(current);
			acquiredAtMillis = System.currentTimeMillis();
		}

		private synchronized void released() {
			if (threadId != System.identityHashCode(Thread.currentThread())) {
				return;
			}
			engineQName = "";
			method = "";
			thread = "";
			threadId = 0;
			acquiredAtMillis = 0;
		}

		private synchronized JSONObject toJson(int index, ReentrantLock lock) throws JSONException {
			var now = System.currentTimeMillis();
			return new JSONObject()
					.put("slot", index)
					.put("locked", lock.isLocked())
					.put("queued", lock.getQueueLength())
					.put("engineQName", engineQName)
					.put("method", method)
					.put("thread", thread)
					.put("threadId", threadId)
					.put("heldMs", acquiredAtMillis == 0 ? 0 : Math.max(0, now - acquiredAtMillis));
		}
	}

	static final class FrontendAuthoringMutationGate {
		private final AtomicInteger pending = new AtomicInteger();
		private final AtomicLong epoch = new AtomicLong();

		long readStamp() {
			return pending.get() == 0 ? epoch.get() : -1;
		}

		boolean canServe(long stamp) {
			return stamp >= 0 && pending.get() == 0 && epoch.get() == stamp;
		}

		void beginMutation() {
			pending.incrementAndGet();
			epoch.incrementAndGet();
		}

		void endMutation() {
			pending.decrementAndGet();
		}

		int pendingMutations() {
			return pending.get();
		}
	}

	static final class FrontendAuthoringFlight {
		private final String key;
		private final CompletableFuture<String> future;
		private final boolean leader;

		private FrontendAuthoringFlight(String key, CompletableFuture<String> future, boolean leader) {
			this.key = key;
			this.future = future;
			this.leader = leader;
		}

		boolean leader() {
			return leader;
		}

		void complete(JSONObject response) {
			future.complete(serializedResponse(response));
		}

		void complete(String serializedResponse) {
			future.complete(serializedResponse);
		}

		void completeExceptionally(Throwable error) {
			future.completeExceptionally(error);
		}

		void abandon() {
			future.completeExceptionally(new EngineException("Flow frontend authoring request ended without a response."));
		}

		JSONObject await() throws EngineException {
			try {
				return new JSONObject(future.join());
			} catch (CompletionException e) {
				var cause = e.getCause();
				if (cause instanceof EngineException engineException) {
					throw engineException;
				}
				throw new EngineException("Unable to join a Flow frontend authoring request.", cause == null ? e : cause);
			} catch (Exception e) {
				throw new EngineException("Unable to read a shared Flow frontend authoring response.", e);
			}
		}
	}

	private static final ProjectLibraries.Holder runtimeGenerations =
			new ProjectLibraries.Holder() {
		@Override public boolean inUse(ProjectClassLoader generation) {
			// Removing a cache entry does not dispose the runtime a running caller already holds.
			return false;
		}
		@Override public void release(ProjectClassLoader generation) {
			synchronized (sharedRuntimes) {
				sharedRuntimes.keySet().removeIf(key -> key.loader() == generation);
			}
		}
	};
	static {
		ProjectLibraries.addHolder(runtimeGenerations);
	}

	public static void clearCaches() {
		ProjectLibraries.addHolder(runtimeGenerations);
		compiledModules.clear();
		serverSources.clear();
		sharedMetadata.clear();
		sourceMemo.clear();
		var runtimes = new ArrayList<CachedEngineRuntime>();
		synchronized (sharedRuntimes) {
			cacheGeneration.incrementAndGet();
			sharedRuntimes.clear();
		}
		synchronized (engineRuntimeCacheLock) {
			engineRuntimeCache.values().forEach(pool -> runtimes.addAll(drainAvailableRuntimes(pool)));
			engineRuntimeCache.clear();
		}
		engineSourceCache.clear();
		forgetRootFingerprints();
		disposeEngineRuntimes(runtimes, null);
		methodResponseCache.clear();
		methodResponseCacheInvalidations.increment();
		RhinoUtils.clearCachedJavascript();
	}

	/** The data caches of every project: their shared runtimes, their method responses, the sources and fingerprints read. */
	public static void invalidateDataCaches() {
		sharedMetadata.clear();
		synchronized (sharedRuntimes) {
			serverSources.clear();
			commonDataRevision.accumulateAndGet(dataGeneration.incrementAndGet(), Math::max);
			sharedRuntimes.clear();
		}
		forgetRootFingerprints();
		clearMethodResponseCache();
	}

	/**
	 * The data caches of the project of a directory and of the projects using it, as {@link #projectUnloaded} does: the
	 * directory its files are read from (Project.getDirFile), whose snapshots are forgotten.
	 */
	public static void invalidateProjectDataCaches(String directory) {
		invalidateProjectDataCaches(directory, null);
	}

	/**
	 * A project is unloaded (deployed, imported, reloaded or removed): the data caches of the project and of the loaded
	 * projects whose runtime uses it are invalidated, those referencing it, directly or through other projects, and those
	 * whose runtime it provides: their shared runtimes and their method responses, then the sources and fingerprints read
	 * from its own files. The other projects keep their prepared runtimes.
	 */
	public static void projectUnloaded(Project project) {
		invalidateProjectDataCaches(project.getDirFile().getPath(), project.getName());
	}

	/**
	 * A project is loaded again after the start (deployed, imported, reloaded): as at its unload, the data caches of it
	 * and of the projects using it are invalidated, a runtime prepared from its files while they were replaced being
	 * prepared again. Not for the loads of a server start, whose files are not being replaced.
	 */
	public static void projectLoaded(Project project) {
		if (FlowRuntimeWarmup.started()) {
			invalidateProjectDataCaches(project.getDirFile().getPath(), project.getName());
			FlowAuthoringWarmup.projectLoaded(project.getName());
		}
	}

	/** @param name the name of the project, null to find it from its directory */
	private static void invalidateProjectDataCaches(String directory, String name) {
		if (directory == null || directory.isBlank()) { invalidateDataCaches(); return; }
		var root = canonicalPath(new File(directory));
		Set<String> roots;
		try {
			roots = loadedDependents(root, name);
		} catch (RuntimeException e) {
			// without the projects using it, the data caches of every project
			if (Engine.logEngine != null) {
				Engine.logEngine.warn("(FlowEngineBridge) Unable to find the projects using the project of " + root + ".", e);
			}
			invalidateDataCaches();
			return;
		}
		roots.add(root);
		synchronized (sharedRuntimes) {
			// a runtime being prepared for their previous revision is not published (publishSharedRuntime)
			var revision = dataGeneration.incrementAndGet();
			for (var each : roots) {
				sourceRevisions.computeIfAbsent(each, ignored -> new AtomicLong()).accumulateAndGet(revision, Math::max);
			}
			sharedRuntimes.keySet().removeIf(key -> roots.contains(key.project()));
			// the snapshots of the other projects are of their own files
			serverSources.clear(new File(directory));
		}
		forgetRootFingerprints("engine:" + root, "project:" + root, "authoring:" + root);
		clearMethodResponseCache(roots);
	}

	/**
	 * @param name the name of the project, null to find it from its directory
	 * @return the canonical directories of the loaded projects whose runtime uses a project: they reference it, directly
	 *         or through other projects, or it provides their runtime; a project referencing one that is not loaded may use
	 *         it through that one
	 */
	private static Set<String> loadedDependents(String root, String name) {
		var dependents = new HashSet<String>();
		var app = Engine.theApp;
		if (app == null || app.databaseObjectsManager == null) {
			return dependents;
		}
		var loaded = new HashSet<String>();
		var users = new HashMap<String, List<Project>>();
		for (var project : app.databaseObjectsManager.getLoadedProjects()) {
			loaded.add(project.getName());
			if (name == null && root.equals(canonicalPath(project.getDirFile()))) {
				name = project.getName();
			}
			for (var used : usedProjectNames(project)) {
				users.computeIfAbsent(used, ignored -> new ArrayList<>()).add(project);
			}
		}
		var pending = new ArrayDeque<String>();
		pending.add(name == null ? projectNameForDir(root) : name);
		for (var used : users.keySet()) {
			if (!loaded.contains(used)) {
				pending.add(used);
			}
		}
		var seen = new HashSet<String>(pending);
		while (!pending.isEmpty()) {
			for (var user : users.getOrDefault(pending.poll(), List.of())) {
				if (seen.add(user.getName())) {
					dependents.add(canonicalPath(user.getDirFile()));
					pending.add(user.getName());
				}
			}
		}
		return dependents;
	}

	/** @return the names of the projects the runtime of a project uses: those it references and the provider of its runtime */
	private static Set<String> usedProjectNames(Project project) {
		var names = new HashSet<String>();
		var flowEngine = project.getFlowEngine();
		var provider = normalizeEngineQName(flowEngine == null ? null : flowEngine.getEngineQName());
		if (provider.indexOf('.') > 0) {
			names.add(provider.substring(0, provider.indexOf('.')));
		}
		for (var reference : project.getReferenceList()) {
			if (reference instanceof ProjectSchemaReference schemaReference) {
				var referenced = schemaReference.getParser().getProjectName();
				if (referenced != null && !referenced.isBlank()) {
					names.add(referenced);
				}
			}
		}
		names.remove(project.getName());
		return names;
	}

	public static long metadataSourceRevision(String directory) {
		if (directory == null || directory.isBlank()) return 0;
		var revision = sourceRevisions.get(canonicalPath(new File(directory)));
		return revision == null ? 0 : revision.get();
	}

	/** @return the canonical directory of a project, empty for none */
	static String projectRoot(String directory) {
		return directory == null || directory.isBlank() ? "" : canonicalPath(new File(directory));
	}

	/**
	 * @return the revision of the data caches of the project of a canonical directory: it changes when they are
	 *         invalidated, with those of every project or of a project its runtime uses; for none (empty), at each
	 *         invalidation, as a call of no project may depend on any of them
	 */
	static long dataRevision(String root) {
		if (root.isEmpty()) return dataGeneration.get();
		var own = sourceRevisions.get(root);
		return Math.max(commonDataRevision.get(), own == null ? 0 : own.get());
	}

	/** Only immutable metadata text crosses scopes. Caller revisions include source and dependency fingerprints. */
	public static String sharedMetadata(String key, String revision, java.util.concurrent.Callable<String> builder) throws Exception {
		return sharedMetadata.get(key, cacheGeneration.get() + ":" + revision, builder);
	}

	/**
	 * A value computed from the content of sources, shared by every runtime: only its text crosses them, each caller
	 * parsing its own copy. A builder nested in a build of the same key, or joining a build failing on another thread,
	 * builds again without keeping its value.
	 *
	 * @param key what the value is computed from, its content included
	 * @param revision the revisions of what else it depends on
	 */
	public static String sharedSourceMemo(String key, String revision, java.util.concurrent.Callable<String> builder) throws Exception {
		return sourceMemo.memo(key, cacheGeneration.get() + ":" + revision, builder);
	}

	/** Compile once across overflow runtimes; evaluating the script still creates caller-owned exports. */
	public static Object evalSharedModule(org.mozilla.javascript.Context cx, Scriptable scope, String source, String name) {
		if (RhinoUtils.debugMode) return RhinoUtils.evalInterpretedJavascript(cx, scope, source, name, 1, null);
		var interpreted = cx.isInterpretedMode();
		try { cx.setInterpretedMode(false); return compiledModules.get(cx, source, name).exec(cx, scope); }
		finally { cx.setInterpretedMode(interpreted); }
	}
	/**
	 * Compiles shared code of a provider as cx.compileString(source, name, 1, null), also kept compiled on disk in the
	 * private folder of the project of its file (RhinoBytecodeCache).
	 *
	 * @param origin the path of the file of the code; null or empty for code of no file (a draft): memory only
	 */
	public static org.mozilla.javascript.Script compileScript(org.mozilla.javascript.Context cx, String source, String name, String origin) {
		return RhinoBytecodeCache.compile(cx, source, name, origin == null || origin.isBlank() ? null : new File(origin));
	}
	public static String serverFileFingerprint(File file) throws java.io.IOException {
		return Engine.theApp == null || Engine.isStudioMode() ? null : serverSources.fingerprint(file);
	}
	public static String readSourceFile(File file) throws java.io.IOException {
		return Engine.theApp == null || Engine.isStudioMode() ? FileUtils.readFileToString(file, java.nio.charset.StandardCharsets.UTF_8) : serverSources.text(file);
	}
	public static String serverDirectoryFingerprint(File file, java.util.function.Supplier<String> compute) {
		return Engine.theApp == null || Engine.isStudioMode() ? compute.get() : serverSources.directory(file, compute);
	}

	public static void clearSharedMetadata() { sharedMetadata.clear(); sourceMemo.clear(); serverSources.clear(); }

	public static long cacheGeneration() {
		return cacheGeneration.get();
	}

	/**
	 * @return the generation of the data caches of every project, changed by each invalidation, of all of them or of
	 *         some: that of the pooled runtimes, which serve every project (see {@link #dataRevision} for one project)
	 */
	public static long dataGeneration() {
		return dataGeneration.get();
	}

	public static boolean requiresRuntimeCacheInvalidation(String projectRelativePath) {
		return FlowSourceLayout.current().requiresRuntimeInvalidation(projectRelativePath);
	}

	public static boolean isFrontendAuthoringSourcePath(String projectRelativePath) {
		return FlowSourceLayout.current().isFrontendAuthoringSource(projectRelativePath);
	}

	public static void notifySourceMutation(String projectDir, String sourcePath) {
		notifySourceMutation(projectDir, sourcePath, false);
	}

	/**
	 * Rhino-safe entry point carrying the reveal intent without relying on Java
	 * overload resolution. Keep the overloaded methods for binary compatibility.
	 */
	public static void notifySourceMutationWithReveal(String projectDir, String sourcePath, boolean reveal) {
		notifySourceMutation(projectDir, sourcePath, reveal);
	}

	public static void notifySourceMutation(String projectDir, String sourcePath, boolean reveal) {
		if (projectDir == null || projectDir.isBlank()) invalidateDataCaches();
		else invalidateProjectDataCaches(projectDir);
		var projectName = projectNameForDir(projectDir);
		try {
			var payload = new JSONObject()
					.put("project", projectName)
					.put("projectDir", projectDir == null ? "" : projectDir)
					.put("sourcePath", sourcePath == null ? "" : sourcePath)
					.put("reveal", reveal);
			AdminEventBus.publish("flow.source.changed", payload);
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to publish a Flow source mutation.", e);
		}
		try {
			AdminEventBus.publishProjectChanged(projectName, projectName, "flow", sourcePath);
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to publish a Flow source mutation.", e);
		}
		if (!Engine.isStudioMode() || Engine.theApp == null || Engine.theApp.eventManager == null) {
			return;
		}
		try {
			var project = projectName.isBlank() ? null
					: Engine.theApp.databaseObjectsManager.getLoadedProjectByName(projectName);
			if (project != null && project.getFlowEngine() != null) {
				FlowStudioSupport.afterSourceMutation(project.getFlowEngine(), sourcePath);
			}
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to synchronize a mutated Flow frontend source.", e);
		}
		try {
			var payload = new JSONObject()
					.put("projectName", projectName)
					.put("projectDir", projectDir == null ? "" : projectDir)
					.put("sourcePath", sourcePath == null ? "" : sourcePath)
					.put("reveal", reveal);
			Engine.theApp.eventManager.dispatchEvent(
					new StudioEvent(StudioEvent.FLOW_SOURCE_CHANGED, payload),
					StudioEventListener.class);
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to notify a Flow source mutation.", e);
		}
	}

	public static void notifyStudioBrowser(String browserJson) {
		if (browserJson == null || browserJson.isBlank()) {
			return;
		}
		try {
			AdminEventBus.publish("flow.browser.open", browserEventPayload(browserJson));
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to publish a Flow browser.", e);
		}
		if (!Engine.isStudioMode() || Engine.theApp == null || Engine.theApp.eventManager == null) {
			return;
		}
		try {
			Engine.theApp.eventManager.dispatchEvent(
					new StudioEvent(StudioEvent.FLOW_BROWSER_OPEN, new JSONObject(browserJson)),
					StudioEventListener.class);
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to notify a Flow browser.", e);
		}
	}

	static JSONObject browserEventPayload(String browserJson) throws Exception {
		return new JSONObject(browserJson);
	}

	private static String projectNameForDir(String projectDir) {
		if (projectDir == null || projectDir.isBlank()) {
			return "";
		}
		try {
			var target = new File(projectDir).getCanonicalFile();
			if (Engine.theApp != null && Engine.theApp.databaseObjectsManager != null && Engine.isStudioMode()) {
				for (var entry : Engine.theApp.databaseObjectsManager.getStudioProjects().getProjects(false).entrySet()) {
					if (target.equals(entry.getValue().getCanonicalFile())) {
						return entry.getKey();
					}
				}
			}
			return target.getName();
		} catch (Exception e) {
			return new File(projectDir).getName();
		}
	}

	public Map<String, Object> run(Flow flow, Context convertigoContext, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		var started = System.nanoTime();
		// a project waiting for the preparation of its runtime in the background is prepared next
		FlowRuntimeWarmup.requested(flow.getProject());
		try {
			var engineQName = effectiveEngineQName(flow);
			var flowSource = flow.getFlowSource();
			var sourceFinished = System.nanoTime();
			var request = baseRequest(engineQName, flowSource, flow.getQName(), convertigoContext)
					.put("flowName", flow.getName())
					.put("projectDir", flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("includeTrace", flow.isIncludeTrace());
			var requestFinished = System.nanoTime();
			try (var allocation = com.twinsoft.convertigo.engine.util.AllocationTrace.open("boundary", "inputs", flow.getQName())) {
				request.put("input", flow.getFlowInput());
			}
			var inputFinished = System.nanoTime();
			var profileEnabled = com.twinsoft.convertigo.engine.util.TimingTrace.isEnabled()
					|| convertigoContext != null && convertigoContext.httpServletRequest != null
					&& "true".equals(convertigoContext.httpServletRequest.getParameter("__flowProfile"));
			if (profileEnabled) {
				request.put("profile", true);
			}
			var response = JsonValues.map(JsonValues.fromLegacy(invokeValue(engineQName, "run", request, convertigoContext, javascriptContext, scope)));
			if (profileEnabled) {
				var profile = response.get("profile") instanceof Map<?, ?> ? JsonValues.map(response.get("profile")) : null;
				if (profile == null) {
					profile = JsonValues.object();
					response.put("profile", profile);
				}
				profile.put("javaBridge", JsonValues.object("sourceMs", nanosToMillis(sourceFinished - started), "requestMs", nanosToMillis(requestFinished - sourceFinished), "inputMs", nanosToMillis(inputFinished - requestFinished), "invokeMs", nanosToMillis(System.nanoTime() - inputFinished), "totalMs", nanosToMillis(System.nanoTime() - started)));
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow engine request.", e);
		}
	}

	public JSONObject prepare(Flow flow) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			return invoke(engineQName, "prepare", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to prepare Flow plan.", e);
		}
	}

	public JSONObject describeTree(Flow flow) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = flowAuthoringRequest(engineQName, flow, null)
					.put("allowRequestableSchema", false);
			return invoke(engineQName, "describeTree", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow tree request.", e);
		}
	}

	public JSONObject syncInputs(Flow flow) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("flowName", flow == null ? "" : flow.getName())
					.put("project", flow == null || flow.getProject() == null ? "" : flow.getProject().getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("metadataOnly", !Engine.isStudioMode());
			if (Engine.isStudioMode()) return invoke(engineQName, "syncInputs", request, null, null, null);
			var revision = org.apache.commons.codec.digest.DigestUtils.sha256Hex(request.toString()) + ":"
					+ dataRevision(projectRoot(request.optString("projectDir", "")));
			var result = sharedMetadata("input-contract\n" + engineQName + "\n" + request.optString("projectDir") + "\n" + request.optString("flowName"), revision,
					() -> invoke(engineQName, "syncInputs", request, null, null, null).toString());
			return new JSONObject(result);
		} catch (Exception e) {
			throw new EngineException("Unable to build Flow input synchronization request.", e);
		}
	}

	/**
	 * Checks a FlowScript of the Flow without running it, as the code-check of the Flow MCP does.
	 * @param code the FlowScript to check, as its working copy
	 */
	public JSONObject flowCodeCheck(Flow flow, String code) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var project = flow.getProject();
			var request = baseRequest(engineQName, code, flow.getQName(), null)
					.put("qname", flow.getQName())
					.put("name", flow.getName())
					.put("flowName", flow.getName())
					.put("code", code)
					.put("maxDiagnostics", 25)
					.put("project", project == null ? "" : project.getName())
					.put("projectDir", project == null ? "" : project.getDirPath());
			return invoke(engineQName, "flowCodeCheck", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build the FlowScript check request.", e);
		}
	}

	public JSONObject catalog(Flow flow) throws EngineException {
		return catalog(flow, false);
	}

	public JSONObject catalog(Flow flow, boolean includePrivate) throws EngineException {
		return catalog(flow, includePrivate, false);
	}

	public JSONObject catalog(Flow flow, boolean includePrivate, boolean includeInternal) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			if (includePrivate) {
				request.put("includePrivate", true);
			}
			if (includeInternal) {
				request.put("includeInternal", true);
			}
			return invoke(engineQName, "catalog", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow catalog request.", e);
		}
	}

	public JSONObject outputSchema(Flow flow) throws EngineException {
		return outputSchema(flow, null);
	}

	public JSONObject outputSchema(Flow flow, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("allowRequestableSchema", false);
			merge(request, options);
			return invoke(engineQName, "outputSchema", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow output schema request.", e);
		}
	}

	public JSONObject contextMenu(Flow flow, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("target", "flow")
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "contextMenu", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow context menu request.", e);
		}
	}

	public JSONObject contextAction(Flow flow, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("target", "flow")
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "contextAction", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow context action request.", e);
		}
	}

	public JSONObject writeCodeMirror(Flow flow, File sourceFile) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			return writeCodeMirror(engineQName, flow == null ? "" : flow.getQName(),
					flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath(),
					flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getName(), sourceFile);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowScript mirror request.", e);
		}
	}

	public JSONObject context(Flow flow, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "context", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow context request.", e);
		}
	}

	public JSONObject context(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "context", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine context request.", e);
		}
	}

	public JSONObject preload(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var project = flowEngine == null ? null : flowEngine.getProject();
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("project", project == null ? "" : project.getName())
					.put("projectDir", project == null ? "" : project.getDirPath());
			return invoke(engineQName, "preload", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine preload request.", e);
		}
	}

	/**
	 * Prepares the run plan of a Flow as its first run prepares it, in the runtime its runs use (the shared runtime of
	 * its project on a stateless server), without running it: the request of a run without its input and context.
	 *
	 * @return the answer of the provider: plans holds the outcome for the Flow, absent when the provider prepares no
	 *         plan in advance (before lib_flow_engine 0.1.119: only its catalog and its compiler are prepared)
	 */
	public JSONObject preloadPlan(Flow flow) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var project = flow.getProject();
			var request = baseRequest(engineQName, "", flow.getQName(), null)
					.put("flowName", flow.getName())
					.put("project", project == null ? "" : project.getName())
					.put("projectDir", project == null ? "" : project.getDirPath())
					.put("flows", new JSONArray().put(new JSONObject()
							.put("flowQName", flow.getQName())
							.put("flowName", flow.getName())
							.put("flowSource", flow.getFlowSource())));
			return invoke(engineQName, "preload", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow plan preload request.", e);
		}
	}

	public JSONObject propertyEditor(Flow flow) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			return invoke(engineQName, "propertyEditor", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow property editor request.", e);
		}
	}

	public Object propertyValue(DatabaseObject owner, JSONObject definition, String text) throws EngineException {
		try {
			var engineQName = owner instanceof Flow flow ? effectiveEngineQName(flow)
					: effectiveEngineQName((FlowEngine) owner);
			var request = baseRequest(engineQName, "", owner.getQName(), null)
					.put("projectDir", owner.getProject() == null ? "" : owner.getProject().getDirPath())
					.put("propertyDefinition", definition == null ? new JSONObject() : definition)
					.put("text", text == null ? "" : text);
			var result = invoke(engineQName, "propertyValue", request, null, null, null);
			if (!result.optBoolean("ok", false) || !result.has("value")) {
				throw new EngineException("Unable to convert Flow property value: " + result.opt("error"));
			}
			return result.get("value");
		} catch (JSONException e) {
			throw new EngineException("Unable to read Flow property value.", e);
		}
	}

	public JSONObject propertyEditor(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			return invoke(engineQName, "propertyEditor", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine property editor request.", e);
		}
	}

	public JSONObject tagContribution(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine.getQName(), null), flowEngine)
					.put("projectDir", flowEngine.getProject().getDirPath());
			return invoke(engineQName, "tagContribution", request, null, null, null);
		} catch (JSONException e) { throw new EngineException("Unable to describe Flow tag contribution.", e); }
	}

	public JSONObject requestables(Flow flow) throws EngineException {
		var currentProjectName = flow == null || flow.getProject() == null ? "" : flow.getProject().getName();
		return requestables(currentProjectName);
	}

	public JSONObject requestables(FlowEngine flowEngine) throws EngineException {
		var currentProjectName = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getName();
		return requestables(currentProjectName);
	}

	private JSONObject requestables(String currentProjectName) throws EngineException {
		try {
			var projects = new JSONArray();
			for (var projectName : requestableProjectNames(currentProjectName)) {
				try {
					var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, true);
					projects.put(requestableProject(project, projectName.equals(currentProjectName)));
				} catch (Exception e) {
					Engine.logEngine.debug("(FlowEngineBridge) Unable to list requestables for project \"" + projectName + "\".", e);
				}
			}
			return new JSONObject()
					.put("ok", true)
					.put("projects", projects);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow requestable list.", e);
		}
	}

	public JSONObject icons(Flow flow, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "icons", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow icon catalog request.", e);
		}
	}

	/** The _flow/dependencies.json content of a project: the versions of the definer projects it uses. */
	public JSONObject dependencies(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			return invoke(engineQName, "dependencies", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow dependencies request.", e);
		}
	}

	public JSONObject icons(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "icons", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine icon catalog request.", e);
		}
	}

	public JSONObject cacheInfo(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			var response = invoke(engineQName, "cacheInfo", request, null, null, null);
			response.put("bridge", bridgeCacheInfo(engineQName));
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine cache info request.", e);
		}
	}

	public JSONObject cacheClear(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			clearCaches();
			clearPersistedResponses();
			return invoke(engineQName, "cacheClear", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine cache clear request.", e);
		}
	}

	public JSONObject contextMenu(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "contextMenu", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine context menu request.", e);
		}
	}

	public JSONObject contextAction(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "contextAction", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine context action request.", e);
		}
	}

	public JSONObject authoringPalette(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "authoringPalette", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine authoring palette request.", e);
		}
	}

	public JSONObject authoringPalette(Flow flow, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = flowAuthoringRequest(engineQName, flow, options);
			return invoke(engineQName, "authoringPalette", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow authoring palette request.", e);
		}
	}

	public JSONObject authoringMutate(Flow flow, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = flowAuthoringRequest(engineQName, flow, options).put("write", false).put("persist", false);
			var response = invoke(engineQName, "authoringMutate", request, null, null, null);
			if (response.optBoolean("ok", false) && response.has("source") && !request.optBoolean("dryRun", false)) {
				flow.setFlowSource(response.getString("source"));
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow authoring mutation request.", e);
		}
	}

	public JSONObject authoringMutate(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			if (flowEngine != null) {
				// A loaded model owns its draft. Only project Save may write the
				// original source, irrespective of the provider's persistence default.
				request.put("write", false).put("persist", false);
			}
			var response = invoke(engineQName, "authoringMutate", request, null, null, null);
			if (flowEngine != null && response.optBoolean("ok", false) && !request.optBoolean("dryRun", false)) {
				// Providers return source text; the owner alone manages working copies.
				// Validate the complete batch before advancing the Engine source too.
				var engineSourceUpdate = "engine".equals(response.optString("target")) && response.has("source")
						? response.getString("source") : null;
				var sources = new LinkedHashMap<String, String>();
				var removals = new ArrayList<String>();
				if (response.has("sourceChanges")) {
					var changes = response.getJSONObject("sourceChanges");
					for (var keys = changes.keys(); keys.hasNext();) {
						var path = keys.next().toString();
						if (!(changes.get(path) instanceof String source)) {
							throw new EngineException("Flow source change must contain text: " + path);
						}
						sources.put(path, source);
					}
				}
				if (response.has("sourceRemovals")) {
					var paths = response.getJSONArray("sourceRemovals");
					for (var index = 0; index < paths.length(); index++) {
						if (!(paths.get(index) instanceof String path)) throw new EngineException("Flow source removal must contain a path.");
						removals.add(path);
					}
				}
				if (!sources.isEmpty() || !removals.isEmpty()) flowEngine.applySourceChanges(sources, removals);
				if (engineSourceUpdate != null) {
					flowEngine.setEngineSource(engineSourceUpdate);
				}
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine authoring mutation request.", e);
		}
	}

	public JSONObject authoringTree(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "authoringTree", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine authoring tree request.", e);
		}
	}

	private static Iterable<String> requestableProjectNames(String currentProjectName) {
		var projectNames = new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		appendRequestableProjectNames(currentProjectName, projectNames);
		var orderedProjectNames = new ArrayList<String>(projectNames.size());
		if (currentProjectName != null && projectNames.remove(currentProjectName)) {
			orderedProjectNames.add(currentProjectName);
		}
		orderedProjectNames.addAll(projectNames);
		return orderedProjectNames;
	}

	private static void appendRequestableProjectNames(String name, Set<String> visited) {
		if (name == null || name.isBlank() || !visited.add(name)) {
			return;
		}
		try {
			var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, true);
			for (var reference : project.getReferenceList()) {
				if (reference instanceof ProjectSchemaReference projectReference) {
					appendRequestableProjectNames(projectReference.getParser().getProjectName(), visited);
				}
			}
		} catch (Exception e) {
			Engine.logEngine.debug("(FlowEngineBridge) Unable to list references for project \"" + name + "\".", e);
		}
	}

	public JSONObject describeTree(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = sourceRequest(baseRequest(engineQName, "", flowEngine.getQName(), null), flowEngine)
					.put("target", "engine")
					.put("engineSource", flowEngine.getEngineSource())
					.put("projectDir", flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("includeFlowCatalog", true)
					.put("flowCatalogOrigin", "project")
					.put("includeCatalogLibraries", false)
					.put("includeBindings", false)
					.put("prewarmFrontendDocumentServer", Engine.isStudioMode());
			return invoke(engineQName, "describeTree", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine tree request.", e);
		}
	}

	public JSONObject catalog(FlowEngine flowEngine) throws EngineException {
		return catalog(flowEngine, false);
	}

	public JSONObject catalog(FlowEngine flowEngine, boolean includePrivate) throws EngineException {
		return catalog(flowEngine, includePrivate, false);
	}

	public JSONObject catalog(FlowEngine flowEngine, boolean includePrivate, boolean includeInternal) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			if (includePrivate) {
				request.put("includePrivate", true);
			}
			if (includeInternal) {
				request.put("includeInternal", true);
			}
			return invoke(engineQName, "catalog", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine catalog request.", e);
		}
	}

	public JSONObject applyMutation(Flow flow, JSONObject mutation) throws EngineException {
		return applyMutation(flow, mutation, true);
	}

	public JSONObject applyMutation(Flow flow, JSONObject mutation, boolean includeTree) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = flowAuthoringRequest(engineQName, flow, null)
					.put("includeTree", includeTree)
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			var response = invoke(engineQName, "applyMutation", request, null, null, null);
			if (response.optBoolean("ok", false) && response.has("source")) {
				flow.setFlowSource(response.optString("source", flow.getFlowSource()));
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow mutation request.", e);
		}
	}

	public JSONObject applyMutation(FlowEngine flowEngine, JSONObject mutation) throws EngineException {
		return applyMutation(flowEngine, mutation, true);
	}

	public JSONObject applyMutation(FlowEngine flowEngine, JSONObject mutation, boolean includeTree) throws EngineException {
		return applyMutation(flowEngine, mutation, includeTree, "");
	}

	public JSONObject applyMutation(FlowEngine flowEngine, JSONObject mutation, boolean includeTree, String projectionPath) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine.getQName(), null)
					.put("target", "engine")
					.put("includeTree", includeTree)
					.put("engineSource", flowEngine.getEngineSource())
					.put("projectDir", flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			if (projectionPath != null && !projectionPath.isBlank()) {
				request.put("projectionPaths", new JSONArray().put(projectionPath));
			}
			var response = invoke(engineQName, "applyMutation", request, null, null, null);
			if (response.optBoolean("ok", false) && response.has("source")) {
				flowEngine.setEngineSource(response.optString("source", flowEngine.getEngineSource()));
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine mutation request.", e);
		}
	}

	public JSONObject applySourceMutation(FlowEngine flowEngine, String sourcePath, JSONObject mutation) throws EngineException {
		return applySourceMutation(flowEngine, sourcePath, mutation, "");
	}

	public JSONObject applySourceMutation(FlowEngine flowEngine, String sourcePath, JSONObject mutation,
			String authoringRootPath) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
			var sourceFile = new File(sourcePath == null ? "" : sourcePath);
			if (flowEngine == null ? !sourceFile.isFile() : !flowEngine.hasSource(sourcePath)) {
				throw new EngineException("Flow source file not found: " + sourcePath);
			}
			if (sourceFile.getName().endsWith(".flow.svelte")) {
				return applyFlowSvelteSourceMutation(flowEngine, sourceFile, mutation, authoringRootPath);
			}
			var source = flowEngine == null
					? FileUtils.readFileToString(sourceFile, "UTF-8")
					: flowEngine.getSource(sourceFile.getAbsolutePath());
			var request = sourceRequest(baseRequest(engineQName, source, flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
					.put("target", "flow")
					.put("flowSource", source)
					.put("sourceFile", sourceFile.getAbsolutePath())
					.put("sourcePath", sourceFile.getAbsolutePath())
					.put("projectDir", projectDir)
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			var response = invoke(engineQName, "applySourceMutation", request, null, null, null);
			if (response.optBoolean("ok", false) && response.has("source")) {
				var newSource = response.optString("source", source);
				var changed = !newSource.equals(source);
				response.put("changed", changed);
				if (changed && flowEngine == null) {
					FileUtils.writeStringToFile(sourceFile, newSource, "UTF-8");
				} else if (flowEngine != null) {
					flowEngine.setSource(sourceFile.getAbsolutePath(), newSource);
				}
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow source mutation request.", e);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to apply Flow source mutation.", e);
		}
	}

	private JSONObject applyFlowSvelteSourceMutation(FlowEngine flowEngine, File sourceFile, JSONObject mutation,
			String authoringRootPath) throws Exception {
		var engineQName = effectiveEngineQName(flowEngine);
		var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
		var sourcePath = sourceFile.getAbsolutePath();
		var source = flowEngine == null
				? FileUtils.readFileToString(sourceFile, "UTF-8")
				: flowEngine.getFrontendSource(sourcePath);
		FlowStudioSupport.performanceProfileMark("sourceMutation.readSource");
		var request = sourceRequest(baseRequest(engineQName, source, flowEngine == null ? "" : flowEngine.getQName(), null), flowEngine)
				.put("target", "frontendSource")
				.put("source", source)
				.put("sourceFile", sourcePath)
				.put("sourcePath", sourcePath)
				.put("projectDir", projectDir)
				.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
				.put("mutation", mutation == null ? new JSONObject() : mutation);
		if (authoringRootPath != null && !authoringRootPath.isBlank()) {
			request.put("authoringRootPath", authoringRootPath);
		}
		FlowStudioSupport.performanceProfileMark("sourceMutation.buildRequest");
		var response = invoke(engineQName, "applySourceMutation", request, null, null, null);
		FlowStudioSupport.performanceProfileMark("sourceMutation.providerAndProjection");
		if (response.optBoolean("ok", false) && response.has("source")) {
			if (authoringRootPath != null && !authoringRootPath.isBlank()) {
				var authoringTree = response.optJSONObject("authoringTree");
				if (authoringTree == null || !authoringTree.optBoolean("ok", false)) {
					var error = authoringTree == null ? null : authoringTree.optJSONObject("error");
					throw new EngineException("Flow frontend mutation returned no valid tree projection"
							+ (error == null ? "." : ": " + error.optString("message", "unknown projection error")));
				}
			}
			var newSource = response.optString("source", source);
			var changed = !newSource.equals(source);
			response.put("changed", changed);
			if (flowEngine == null) {
				if (changed) {
					FileUtils.writeStringToFile(sourceFile, newSource, "UTF-8");
				}
			} else {
				flowEngine.setFrontendSource(sourcePath, newSource);
			}
		}
		FlowStudioSupport.performanceProfileMark("sourceMutation.persistDraft");
		return response;
	}

	private static JSONObject sourceRequest(JSONObject request, FlowEngine... flowEngines) throws JSONException, EngineException {
		merge(request, sourceWorkingCopies(flowEngines));
		return request;
	}

	/** A single provider request carries both text drafts and explicit absence. */
	static JSONObject sourceWorkingCopies(FlowEngine... flowEngines) throws JSONException, EngineException {
		var drafts = new JSONObject();
		var removals = new java.util.TreeSet<String>();
		var visited = new HashSet<String>();
		for (var flowEngine : flowEngines) {
			if (flowEngine == null) {
				continue;
			}
			appendSourceWorkingCopies(drafts, removals, flowEngine);
			appendSourceWorkingCopies(drafts, removals, flowEngine.getProject(), visited);
		}
		return new JSONObject().put("frontendSourceDrafts", drafts).put("sourceRemovals", new JSONArray(removals));
	}

	private static void appendSourceWorkingCopies(JSONObject drafts, Set<String> removals, FlowEngine flowEngine) throws JSONException, EngineException {
		if (flowEngine == null) {
			return;
		}
		var changes = com.twinsoft.convertigo.beans.flow.FlowWorkingCopies.sourceChanges(flowEngine);
		for (var entry : changes.writes().entrySet()) {
			drafts.put(entry.getKey(), entry.getValue());
		}
		removals.addAll(changes.removals());
	}

	private static void appendSourceWorkingCopies(JSONObject drafts, Set<String> removals, Project project, Set<String> visited)
			throws JSONException, EngineException {
		if (project == null || !visited.add(project.getName())) {
			return;
		}
		appendSourceWorkingCopies(drafts, removals, project.getFlowEngine());
		for (var reference : project.getReferenceList()) {
			if (!(reference instanceof ProjectSchemaReference projectReference)) {
				continue;
			}
			var referencedProjectName = projectReference.getParser().getProjectName();
			if (referencedProjectName == null || referencedProjectName.isBlank()) {
				continue;
			}
			var databaseObjectsManager = Engine.theApp.databaseObjectsManager;
			Project referencedProject = databaseObjectsManager.getLoadedProjectByName(referencedProjectName);
			// a referenced project that is neither loaded nor deployed has no draft: no lookup, which fails with an
			// exception at each call
			if (referencedProject == null && !databaseObjectsManager.existsProject(referencedProjectName)) {
				continue;
			}
			try {
				if (referencedProject == null) {
					referencedProject = databaseObjectsManager.getOriginalProjectByName(referencedProjectName, true);
				}
			} catch (Exception e) {
				Engine.logEngine.debug("(FlowEngineBridge) Unable to collect Flow drafts from referenced project \""
						+ referencedProjectName + "\".", e);
				continue;
			}
			appendSourceWorkingCopies(drafts, removals, referencedProject, visited);
		}
	}

	private JSONObject writeCodeMirror(String engineQName, String flowQName, String projectDir, String source, String name, File sourceFile)
			throws EngineException, JSONException {
		var request = baseRequest(engineQName, source, flowQName, null)
				.put("flowName", name == null ? "" : name)
				.put("name", name == null ? "" : name)
				.put("projectDir", projectDir == null ? "" : projectDir);
		if (sourceFile != null) {
			request.put("sourceFile", sourceFile.getAbsolutePath());
		}
		return invoke(engineQName, "writeCodeMirror", request, null, null, null);
	}

	public JSONObject setBlockProperty(FlowEngine flowEngine, String blockName, String propertyName, Object value) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
			var getRequest = new JSONObject()
					.put("name", blockName == null ? "" : blockName)
					.put("projectDir", projectDir);
			var current = invoke(engineQName, "blockGet", getRequest, null, null, null);
			if (current.has("ok") && !current.optBoolean("ok", false)) {
				return current;
			}
			var descriptor = current.optJSONObject("descriptor");
			if (descriptor == null) {
				throw new EngineException("Flow block descriptor not returned for " + blockName);
			}
			if ("name".equals(propertyName)) {
				throw new EngineException("Flow block name is defined by its *.block.js file and cannot be edited as a property.");
			}
			descriptor.put(propertyName, value == null ? JSONObject.NULL : value);
			var editRequest = new JSONObject()
					.put("name", blockName == null ? "" : blockName)
					.put("projectDir", projectDir)
					.put("descriptor", descriptor);
			return invoke(engineQName, "blockEdit", editRequest, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to update Flow block property.", e);
		}
	}

	public JSONObject createBlock(FlowEngine flowEngine, String blockName, String runtime) throws EngineException {
		try {
			runtime = runtime == null || runtime.isBlank() ? "flow" : runtime.trim();
			if (!runtime.equals("flow") && !runtime.equals("rhino")) {
				throw new EngineException("Unsupported Flow block runtime: " + runtime);
			}
			var engineQName = effectiveEngineQName(flowEngine);
			var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
			var localName = blockName == null ? "" : blockName.substring(blockName.lastIndexOf('.') + 1);
			if (runtime.equals("flow")) {
				var request = new JSONObject()
						.put("name", blockName)
						.put("projectDir", projectDir)
						.put("code", defaultFlowBlockCode(blockName, localName));
				return invoke(engineQName, "blockCreate", request, null, null, null);
			}
			var descriptor = new JSONObject()
					.put("version", 1)
					.put("icon", "mdi:language-javascript")
					.put("description", "New JavaScript Flow block.")
					.put("tags", new JSONArray().put("javascript"))
					.put("props", new JSONObject())
					.put("hooks", new JSONObject().put("file", localName + ".hooks.js"))
					.put("implementation", new JSONObject()
							.put("runtime", runtime)
							.put("file", localName + ".js"));
			var request = new JSONObject()
					.put("name", blockName)
					.put("projectDir", projectDir)
					.put("descriptor", descriptor)
					.put("implementationSource", runtime.equals("flow") ? defaultFlowImplementationSource() : defaultRhinoImplementationSource())
					.put("hooksSource", defaultHooksSource());
			return invoke(engineQName, "blockCreate", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to create Flow block.", e);
		}
	}

	public JSONObject setTypeProperty(FlowEngine flowEngine, String typeName, String propertyName, Object value) throws EngineException {
		try {
			if ("name".equals(propertyName)) {
				throw new EngineException("Flow type name is defined by its *.type.yaml file and cannot be edited as a property.");
			}
			var descriptor = projectTypeDescriptor(flowEngine, typeName);
			descriptor.put(propertyName, value == null ? JSONObject.NULL : value);
			return writeTypeDescriptor(flowEngine, typeName, descriptor);
		} catch (JSONException e) {
			throw new EngineException("Unable to update Flow type property.", e);
		}
	}

	public JSONObject setTypeResourceProperty(FlowEngine flowEngine, String typeName, String role, String propertyName, Object value) throws EngineException {
		try {
			var descriptor = projectTypeDescriptor(flowEngine, typeName);
			var resource = descriptor.optJSONObject(role);
			if (resource == null) {
				resource = new JSONObject();
				descriptor.put(role, resource);
			}
			resource.put(propertyName, value == null ? JSONObject.NULL : value);
			return writeTypeDescriptor(flowEngine, typeName, descriptor);
		} catch (JSONException e) {
			throw new EngineException("Unable to update Flow type resource property.", e);
		}
	}

	private JSONObject projectTypeDescriptor(FlowEngine flowEngine, String typeName) throws EngineException, JSONException {
		var engineQName = effectiveEngineQName(flowEngine);
		var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
		var getRequest = new JSONObject()
				.put("name", typeName == null ? "" : typeName)
				.put("projectDir", projectDir);
		var current = invoke(engineQName, "typeGet", getRequest, null, null, null);
		if (current.has("ok") && !current.optBoolean("ok", false)) {
			throw new EngineException("Flow type lookup failed: " + current);
		}
		var descriptor = current.optJSONObject("descriptor");
		if (descriptor == null) {
			throw new EngineException("Flow type descriptor not returned for " + typeName);
		}
		if (!"project".equals(current.optString("origin", ""))) {
			throw new EngineException("Only project-local Flow types can be edited.");
		}
		return descriptor;
	}

	private JSONObject writeTypeDescriptor(FlowEngine flowEngine, String typeName, JSONObject descriptor) throws EngineException, JSONException {
		var engineQName = effectiveEngineQName(flowEngine);
		var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
		var editRequest = new JSONObject()
				.put("name", typeName == null ? "" : typeName)
				.put("projectDir", projectDir)
				.put("descriptor", descriptor)
				.put("overwrite", true);
		return invoke(engineQName, "typeCreate", editRequest, null, null, null);
	}

	public JSONObject createType(FlowEngine flowEngine, String typeName) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
			var descriptor = new JSONObject()
					.put("version", 1)
					.put("name", typeName)
					.put("label", typeName)
					.put("icon", "mdi:form-textbox")
					.put("type", "string")
					.put("description", "Project-local Flow property type.")
					.put("editor", new JSONObject()
							.put("label", "Text editor")
							.put("kind", "webcomponent")
							.put("component", "flow-text-editor")
							.put("file", "editors/" + typeName + ".html")
							.put("icon", "mdi:application-brackets-outline"));
			var request = new JSONObject()
					.put("name", typeName)
					.put("projectDir", projectDir)
					.put("descriptor", descriptor);
			var response = invoke(engineQName, "typeCreate", request, null, null, null);
			if (isSuccessResponse(response)) {
				createTypeEditorFile(flowEngine, typeName, projectDir);
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to create Flow property type.", e);
		}
	}

	private static boolean isSuccessResponse(JSONObject response) {
		return response != null && !response.has("error") && (!response.has("ok") || response.optBoolean("ok", false));
	}

	private static String defaultFlowImplementationSource() {
		return "version: 1\nnodes: []\n";
	}

	private static String defaultFlowBlockCode(String blockName, String localName) {
		var functionName = localName == null || localName.isBlank() ? "flowBlock" : localName;
		functionName = functionName.replaceAll("[^A-Za-z0-9_$]", "_");
		if (functionName.isBlank() || !Character.isJavaIdentifierStart(functionName.charAt(0))) {
			functionName = "flowBlock";
		}
		return """
				const _meta = {
				  "version": 1,
				  "runtime": "flow",
				  "icon": "mdi:source-branch",
				  "description": "New composite Flow block.",
				  "tags": ["composite"],
				  "properties": {},
				  "outputs": {
				    "out": { "type": "unknown" }
				  }
				}

				function %s({ input, config, result }) {
				  return result
				}
				""".formatted(functionName);
	}

	private static String defaultRhinoImplementationSource() {
		return """
				(function () {
					return {
						run: function (ctx, node) {
							return null;
						}
					};
				}())
				""";
	}

	private static String defaultHooksSource() {
		return """
				(function () {
					return {
					};
				}())
				""";
	}

	private static void createTypeEditorFile(FlowEngine flowEngine, String typeName, String projectDir) throws EngineException {
		try {
			if (projectDir == null || projectDir.isBlank()) {
				return;
			}
			FlowSourceLayout.current().ensureHttpIgnore(new File(projectDir));
			var file = new File(projectDir, FlowSourceLayout.current().path("types/editors/" + typeName + ".html"));
			if (file.isFile()) {
				return;
			}
			FileUtils.forceMkdirParent(file);
			FileUtils.writeStringToFile(file, """
					<template id="flow-text-editor">
					  <input data-flow-value type="text" />
					</template>
					""", "UTF-8");
		} catch (Exception e) {
			throw new EngineException("Unable to create Flow type editor file for " + typeName + ".", e);
		}
	}

	private String effectiveEngineQName(Flow flow) {
		if (flow == null) {
			return DEFAULT_ENGINE_QNAME;
		}
		var project = flow.getProject();
		var flowEngine = project == null ? null : project.getFlowEngine();
		if (flowEngine != null && flowEngine.getEngineQName() != null && !flowEngine.getEngineQName().isBlank()) {
			return flowEngine.getEngineQName();
		}
		return DEFAULT_ENGINE_QNAME;
	}

	private String effectiveEngineQName(FlowEngine flowEngine) {
		if (flowEngine != null && flowEngine.getEngineQName() != null && !flowEngine.getEngineQName().isBlank()) {
			return flowEngine.getEngineQName();
		}
		return DEFAULT_ENGINE_QNAME;
	}

	/** Tree, palette and mutations resolve blocks against the same owner and working copies. */
	private JSONObject flowAuthoringRequest(String engineQName, Flow flow, JSONObject options) throws JSONException, EngineException {
		var project = flow.getProject();
		var request = baseRequest(engineQName, flow.getFlowSource(), flow.getQName(), null);
		merge(request, options);
		sourceRequest(request, project == null ? null : project.getFlowEngine())
				.put("target", "flow")
				.put("flowName", flow.getName())
				.put("projectDir", project == null ? "" : project.getDirPath());
		var file = flow.getFlowSourceFile();
		if (file != null) request.put("sourceFile", file.getAbsolutePath());
		return request;
	}

	private JSONObject baseRequest(String engineQName, String flowSource, String flowQName, Context convertigoContext) throws JSONException {
		var request = new JSONObject()
				.put("engineQName", normalizeEngineQName(engineQName))
				.put("flowQName", flowQName == null ? "" : flowQName)
				.put("flowSource", flowSource == null ? "" : flowSource);
		if (convertigoContext != null) {
			request.put("context", new JSONObject()
					.put("project", valueOrEmpty(convertigoContext.projectName))
					.put("sequence", valueOrEmpty(convertigoContext.sequenceName))
					.put("connector", valueOrEmpty(convertigoContext.connectorName))
					.put("transaction", valueOrEmpty(convertigoContext.transactionName))
					.put("convertigoUrl", safeContextUrl(convertigoContext, "convertigo"))
					.put("projectUrl", safeContextUrl(convertigoContext, "project"))
					.put("absoluteRequestedUrl", safeContextUrl(convertigoContext, "absolute")));
		}
		return request;
	}

	private static void merge(JSONObject target, JSONObject source) throws JSONException {
		if (source == null) {
			return;
		}
		for (var keys = source.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			target.put(key, source.opt(key));
		}
	}

	private record RequestableEntry(String connector, String name, String kind) { }

	private static JSONObject requestableProject(Project project, boolean current) throws Exception {
		return new JSONObject(requestableProjectText(project, current));
	}

	/** Metadata only; callers still resolve and authorize actual executions normally. */
	public static String requestableProjectCatalog(String name) throws Exception {
		return requestableProjectText(Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false), true);
	}

	private static String requestableProjectText(Project project, boolean current) throws Exception {
		var name = project.getName();
		var snapshot = new ArrayList<RequestableEntry>();
		for (Sequence sequence : project.getSequencesList()) {
			snapshot.add(new RequestableEntry("", sequence.getName(), sequence instanceof Flow ? "flow" : "sequence"));
		}
		for (Connector connector : project.getConnectorsList()) {
			for (Transaction transaction : connector.getTransactionsList()) {
				snapshot.add(new RequestableEntry(connector.getName(), transaction.getName(), "transaction"));
			}
		}
		// Inspect only names/types to detect unsaved DBO edits as well as reloads.
		// The fingerprint and the builder consume the same captured membership.
		var revision = new StringBuilder();
		for (var entry : snapshot) for (var part : List.of(entry.connector(), entry.name(), entry.kind())) {
			revision.append(part.length()).append(':').append(part);
		}
		return sharedMetadata("requestables\n" + name + "\n" + current, sha256Hex(revision.toString()), () -> {
			var children = new JSONArray();
			var connectors = new LinkedHashMap<String, JSONArray>();
			for (var entry : snapshot) {
				var leaf = requestableLeaf(name, entry.connector(), entry.name(), entry.kind());
				if (entry.connector().isEmpty()) children.put(leaf);
				else connectors.computeIfAbsent(entry.connector(), ignored -> new JSONArray()).put(leaf);
			}
			for (var entry : connectors.entrySet()) {
				children.put(new JSONObject().put("kind", "connector").put("name", entry.getKey())
						.put("qname", name + "." + entry.getKey()).put("children", entry.getValue()));
			}
			return new JSONObject().put("kind", "project").put("name", name).put("qname", name)
					.put("current", current).put("children", children).toString();
		});
	}

	private static JSONObject requestableLeaf(String project, String connector, String name, String kind) throws JSONException {
		return new JSONObject()
				.put("kind", kind)
				.put("name", name)
				.put("qname", project + "." + (connector == null || connector.isBlank() ? "" : connector + ".") + name);
	}

	/** Capture generic tag/source drafts before acquiring any Rhino/provider lock. */
	public static JSONObject prepareProjectRequest(JSONObject request) throws EngineException {
		request.remove("tagContext"); // This is server-owned context, not a caller override.
		if (Engine.theApp == null || Engine.theApp.databaseObjectsManager == null) return request;
		try {
			String qname = request.optString("flowQName", "");
			String name = qname.contains(".") ? qname.substring(0, qname.indexOf('.'))
					: request.optString("project", projectNameForDir(request.optString("projectDir", "")));
			var project = Engine.theApp.databaseObjectsManager.getLoadedProjectByName(name);
			if (project == null) return request;
			String directory = request.optString("projectDir", "");
			if (!directory.isBlank() && !new File(directory).getCanonicalFile().equals(project.getDirFile().getCanonicalFile())) return request;
			// Read from an immutable snapshot, without the tag monitor; a diagnosed tag source travels in the context.
			request.put("tagContext", new JSONObject(com.twinsoft.convertigo.engine.tags.TagManager.get().runContext(project)));
			sourceRequest(request, project.getFlowEngine());
			return request;
		} catch (Exception e) { throw new EngineException("Unable to capture project authoring context.", e); }
	}

	JSONObject invoke(String engineQName, String method, JSONObject request, Context convertigoContext,
			org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		try { return (JSONObject) JsonValues.toLegacy(invokeValue(engineQName, method, request, convertigoContext, javascriptContext, scope)); }
		catch (JSONException e) { throw new EngineException("Unable to adapt authoring response", e); }
	}
	Object invokeValue(String engineQName, String method, JSONObject request, Context convertigoContext,
			org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		prepareProjectRequest(request);
		var engineRef = EngineRef.parse(normalizeEngineQName(engineQName));
		var frontendAuthoring = usesFrontendDocumentProvider(method, request);
		var frontendAuthoringLockIndex = frontendAuthoring ? frontendAuthoringLockIndex(engineRef.qname) : -1;
		var frontendAuthoringLock = frontendAuthoring ? frontendAuthoringLocks[frontendAuthoringLockIndex] : null;
		var frontendAuthoringMutation = frontendAuthoring && invalidatesMethodResponseCache(method, request);
		var frontendAuthoringMutationGate = frontendAuthoring
				? frontendAuthoringMutationGates[frontendAuthoringLockIndex]
				: null;
		var useThreadRuntime = javascriptContext == null && scope == null;
		var cx = javascriptContext;
		var engineScope = scope;
		File engineFile = null;
		CachedEngineSource engineSource = null;
		Scriptable engineObject = null;
		CachedEngineRuntimeLookup runtimeLookup = null;
		var entered = false;
		var started = System.nanoTime();
		var failed = false;
		var methodCacheHit = false;
		var methodCacheKey = "";
		var methodFingerprintNanos = 0L;
		var methodCacheLookupNanos = 0L;
		var lockContended = false;
		var lockWaitNanos = 0L;
		var lockAcquiredNanos = 0L;
		var lockHeldNanos = 0L;
		var lockAcquired = false;
		var mutationRegistered = false;
		var optimisticReadStamp = -1L;
		FrontendAuthoringFlight frontendAuthoringFlight = null;
		// the responses kept on disk across restarts (see persistentResponseKey)
		var diskCache = useThreadRuntime && isPersistedMethod(method, request) ? FlowAuthoringDiskCache.get() : null;
		String diskKey = null;
		// keyed for the current read stamp, and read from disk with that key
		var diskKeyed = false;
		var diskRead = false;
		var foreground = background.get() == null;
		if (foreground) {
			foregroundCalls.incrementAndGet();
		}

		try {
			if (frontendAuthoringMutation) {
				frontendAuthoringMutationGate.beginMutation();
				mutationRegistered = true;
			}
			if (frontendAuthoring && !frontendAuthoringMutation && useThreadRuntime && isCacheableMethod(method, request)) {
				optimisticReadStamp = frontendAuthoringMutationGate.readStamp();
				if (optimisticReadStamp >= 0) {
					engineFile = resolveEngineFile(engineRef);
					engineSource = cachedEngineSource(engineFile);
					var fingerprintStarted = System.nanoTime();
					if (diskCache != null) {
						// the capture of the request and the source of the provider, apart from the key
						FlowStudioSupport.performanceProfileMark("bridge." + method + ".prepare");
						diskKey = persistentResponseKey(engineRef.qname, engineFile, method, request);
						diskKeyed = true;
						FlowStudioSupport.performanceProfileMark("bridge." + method + ".diskKey");
					}
					methodCacheKey = responseCacheKey(engineRef, engineFile, engineSource, method, request, diskKey);
					methodFingerprintNanos = System.nanoTime() - fingerprintStarted;
					var cacheLookupStarted = System.nanoTime();
					var cachedResponse = cachedMethodResponse(methodCacheKey);
					methodCacheLookupNanos += System.nanoTime() - cacheLookupStarted;
					FlowStudioSupport.performanceProfileMark("bridge." + method + ".methodCacheBeforeLock");
					if (cachedResponse != null && frontendAuthoringMutationGate.canServe(optimisticReadStamp)) {
						methodCacheHit = true;
						return new JSONObject(cachedResponse.response());
					}
					if (diskCache != null && probed(method, diskCache, diskKey)) {
						return probeAnswer();
					}
					frontendAuthoringFlight = acquireFrontendAuthoringFlight(methodCacheKey);
					if (frontendAuthoringFlight != null && !frontendAuthoringFlight.leader()) {
						FlowStudioSupport.performanceProfileMark("bridge." + method + ".singleFlightFollower");
						return frontendAuthoringFlight.await();
					}
					// before the authoring lock, which a long action of another project may hold
					var persisted = persistedResponse(diskCache, diskKey, method);
					diskRead = true;
					FlowStudioSupport.performanceProfileMark("bridge." + method + ".diskCacheBeforeLock");
					if (persisted != null && frontendAuthoringMutationGate.canServe(optimisticReadStamp)) {
						methodCacheHit = true;
						storeMethodResponse(methodCacheKey, engineRef, method, request, persisted);
						if (frontendAuthoringFlight != null) {
							frontendAuthoringFlight.complete(persisted);
						}
						return persisted;
					}
				}
			}
			// Different frontend reads still share the runtime pool's authoring scope.
			// Keep them serialized until the provider lifecycle is independent from that
			// Rhino scope; the single-flight above removes duplicate misses safely.
			if (frontendAuthoringLock != null) {
				FlowStudioSupport.performanceProfileMark("bridge." + method + ".beforeLock");
				var lockWaitStarted = System.nanoTime();
				lockContended = frontendAuthoringLock.isLocked() || frontendAuthoringLock.hasQueuedThreads();
				frontendAuthoringLock.lock();
				lockWaitNanos = System.nanoTime() - lockWaitStarted;
				lockAcquiredNanos = System.nanoTime();
				lockAcquired = true;
				frontendAuthoringLockStates[frontendAuthoringLockIndex].acquired(engineRef.qname, method);
				if (!frontendAuthoringMutation
						&& (optimisticReadStamp < 0 || !frontendAuthoringMutationGate.canServe(optimisticReadStamp))) {
					var currentReadStamp = frontendAuthoringMutationGate.readStamp();
					if (currentReadStamp >= 0) {
						optimisticReadStamp = currentReadStamp;
						methodCacheKey = "";
						diskKeyed = false;
						diskRead = false;
					}
				}
			}
			FlowStudioSupport.performanceProfileMark("bridge." + method + ".lockWait");
			if (engineFile == null) {
				engineFile = resolveEngineFile(engineRef);
			}
			if (engineSource == null) {
				engineSource = cachedEngineSource(engineFile);
			}
			FlowStudioSupport.performanceProfileMark("bridge." + method + ".engineSource");
			if (useThreadRuntime) {
				if (isCacheableMethod(method, request)) {
					var fingerprintStarted = System.nanoTime();
					if (methodCacheKey.isEmpty()) {
						if (diskCache != null && !diskKeyed) {
							diskKey = persistentResponseKey(engineRef.qname, engineFile, method, request);
							diskKeyed = true;
						}
						methodCacheKey = responseCacheKey(engineRef, engineFile, engineSource, method, request, diskKey);
						methodFingerprintNanos += System.nanoTime() - fingerprintStarted;
					}
					var cacheLookupStarted = System.nanoTime();
					var cachedResponse = cachedMethodResponse(methodCacheKey);
					methodCacheLookupNanos += System.nanoTime() - cacheLookupStarted;
					FlowStudioSupport.performanceProfileMark("bridge." + method + ".methodCache");
					if (cachedResponse != null) {
						methodCacheHit = true;
						if (frontendAuthoringFlight != null && frontendAuthoringFlight.leader()) {
							frontendAuthoringFlight.complete(cachedResponse.response());
						}
						return new JSONObject(cachedResponse.response());
					}
					if (diskCache != null && !diskRead) {
						diskRead = true;
						if (frontendAuthoringFlight == null && probed(method, diskCache, diskKey)) {
							return probeAnswer();
						}
						var persisted = persistedResponse(diskCache, diskKey, method);
						FlowStudioSupport.performanceProfileMark("bridge." + method + ".diskCache");
						if (persisted != null && (!frontendAuthoring || frontendAuthoringMutationGate.canServe(optimisticReadStamp))) {
							methodCacheHit = true;
							storeMethodResponse(methodCacheKey, engineRef, method, request, persisted);
							if (frontendAuthoringFlight != null && frontendAuthoringFlight.leader()) {
								frontendAuthoringFlight.complete(persisted);
							}
							return persisted;
						}
						// the same response asked meanwhile (the Studio and a background preparation) is computed once; a call
						// nested in another one of this thread never waits for it
						if (!frontendAuthoring && frontendAuthoringFlight == null && currentRuntime.get() == null) {
							frontendAuthoringFlight = acquireFrontendAuthoringFlight(methodCacheKey);
							if (frontendAuthoringFlight != null && !frontendAuthoringFlight.leader()) {
								FlowStudioSupport.performanceProfileMark("bridge." + method + ".singleFlightFollower");
								return frontendAuthoringFlight.await();
							}
						}
					}
				} else if (invalidatesMethodResponseCache(method, request)) {
					clearMethodResponseCache();
				}
			}
			var computeStarted = System.nanoTime();
			if (cx == null) {
				cx = org.mozilla.javascript.Context.enter();
				entered = true;
				if (useThreadRuntime) {
					runtimeLookup = reusableCallerRuntime(engineRef, engineSource, method, request, convertigoContext);
					if (runtimeLookup == null && sharesRuntime(method, request) && !frontendAuthoring
							&& Boolean.getBoolean("convertigo.javascript.stateless") && !Engine.isStudioMode())
						runtimeLookup = sharedEngineRuntime(engineRef, engineFile, engineSource, cx, request, convertigoContext);
					if (runtimeLookup == null) runtimeLookup = cachedEngineRuntime(engineRef, engineFile, engineSource, cx, frontendAuthoring);
					engineScope = runtimeLookup.runtime().scope();
					engineObject = runtimeLookup.runtime().engineObject();
				} else {
					engineScope = cx.initStandardObjects();
				}
			} else if (engineScope == null) {
				engineScope = cx.initStandardObjects();
			}
			FlowStudioSupport.performanceProfileMark("bridge." + method + ".runtime");
			attachDescribeTreeSnapshot(engineRef, method, request);

			var response = invokePrepared(engineRef, engineFile, engineSource, method, request, convertigoContext, cx, engineScope,
					engineObject, runtimeLookup);
			FlowStudioSupport.performanceProfileMark("bridge." + method + ".invoke");
			if (isCacheableMethod(method, request) && methodCacheKey.isEmpty()) {
				var fingerprintStarted = System.nanoTime();
				methodCacheKey = responseCacheKey(engineRef, engineFile, engineSource, method, request, diskKey);
				methodFingerprintNanos += System.nanoTime() - fingerprintStarted;
			}
			var stableAuthoringRead = !frontendAuthoring || frontendAuthoringMutation
					|| frontendAuthoringMutationGate.canServe(optimisticReadStamp);
			if (stableAuthoringRead && response instanceof JSONObject legacyResponse) {
				storeMethodResponse(methodCacheKey, engineRef, method, request, legacyResponse);
				if (diskCache != null && diskKey != null && isCacheableResponse(method, request, legacyResponse)) {
					var text = serializedResponse(legacyResponse);
					diskCache.write(diskKey, () -> persistedParts(text), System.nanoTime() - computeStarted);
				}
			} else {
				FlowStudioSupport.performanceProfileMark("bridge." + method + ".staleSnapshotSkipped");
			}
			FlowStudioSupport.performanceProfileMark("bridge." + method + ".store");
			if (frontendAuthoringFlight != null && frontendAuthoringFlight.leader()) {
				frontendAuthoringFlight.complete((JSONObject) response);
			}
			return response;
		} catch (EngineException e) {
			failed = true;
			if (frontendAuthoringFlight != null && frontendAuthoringFlight.leader()) {
				frontendAuthoringFlight.completeExceptionally(e);
			}
			throw e;
		} catch (Exception e) {
			failed = true;
			var wrapped = new EngineException("Unable to invoke Flow engine \"" + engineRef.qname + "\" method \"" + method + "\".", e);
			if (frontendAuthoringFlight != null && frontendAuthoringFlight.leader()) {
				frontendAuthoringFlight.completeExceptionally(wrapped);
			}
			throw wrapped;
		} finally {
			try {
				releaseEngineRuntime(runtimeLookup);
				FlowStudioSupport.performanceProfileMark("bridge." + method + ".release");
				if (entered) {
					org.mozilla.javascript.Context.exit();
				}
			} finally {
				releaseFrontendAuthoringFlight(frontendAuthoringFlight);
				if (lockAcquired) {
					lockHeldNanos = lockAcquiredNanos == 0 ? 0 : System.nanoTime() - lockAcquiredNanos;
					frontendAuthoringLockStates[frontendAuthoringLockIndex].released();
					frontendAuthoringLock.unlock();
				}
				if (mutationRegistered) {
					frontendAuthoringMutationGate.endMutation();
				}
				if (foreground) {
					lastForegroundCall = System.nanoTime();
					foregroundCalls.decrementAndGet();
				}
				recordInvocation(engineRef, method, System.nanoTime() - started, runtimeLookup, failed, methodCacheHit,
						lockAcquired, lockContended, lockWaitNanos, lockHeldNanos,
						methodFingerprintNanos, methodCacheLookupNanos);
			}
		}
	}

	private static ReentrantLock[] createFrontendAuthoringLocks(int size) {
		var locks = new ReentrantLock[size];
		for (var i = 0; i < size; i++) {
			locks[i] = new ReentrantLock();
		}
		return locks;
	}

	private static FrontendAuthoringLockState[] createFrontendAuthoringLockStates(int size) {
		var states = new FrontendAuthoringLockState[size];
		for (var i = 0; i < size; i++) {
			states[i] = new FrontendAuthoringLockState();
		}
		return states;
	}

	private static FrontendAuthoringMutationGate[] createFrontendAuthoringMutationGates(int size) {
		var gates = new FrontendAuthoringMutationGate[size];
		for (var i = 0; i < size; i++) {
			gates[i] = new FrontendAuthoringMutationGate();
		}
		return gates;
	}

	static FrontendAuthoringFlight acquireFrontendAuthoringFlight(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		synchronized (frontendAuthoringFlightsLock) {
			var existing = frontendAuthoringFlights.get(key);
			if (existing != null) {
				frontendAuthoringFlightFollowers.increment();
				return new FrontendAuthoringFlight(key, existing, false);
			}
			if (frontendAuthoringFlights.size() >= FRONTEND_AUTHORING_SINGLE_FLIGHT_LIMIT) {
				frontendAuthoringFlightBypasses.increment();
				return null;
			}
			var future = new CompletableFuture<String>();
			frontendAuthoringFlights.put(key, future);
			frontendAuthoringFlightLeaders.increment();
			return new FrontendAuthoringFlight(key, future, true);
		}
	}

	static void releaseFrontendAuthoringFlight(FrontendAuthoringFlight flight) {
		if (flight == null || !flight.leader()) {
			return;
		}
		flight.abandon();
		synchronized (frontendAuthoringFlightsLock) {
			frontendAuthoringFlights.remove(flight.key, flight.future);
		}
	}

	private static int frontendAuthoringLockIndex(String engineQName) {
		return (engineQName.hashCode() & Integer.MAX_VALUE) % frontendAuthoringLocks.length;
	}

	static boolean usesFrontendDocumentProvider(String method, JSONObject request) {
		if ("authoringMutate".equals(method)) {
			var action = request == null ? null : request.optJSONObject("action");
			return request != null && "frontend".equals(action == null
					? request.optString("surface") : action.optString("surface", request.optString("surface")));
		}
		if ("authoringTree".equals(method) || "authoringPalette".equals(method)) {
			return request == null || "frontend".equals(request.optString("surface", "frontend"));
		}
		if (request == null || !request.has("frontendSourceDrafts")) {
			return false;
		}
		// a long action telling it needs no authoring state, as a production build: run out of the authoring lock, on
		// another runtime, instead of holding the frontend authoring of every project until it ends (lib_flow_engine
		// 0.1.29 and later guard its own work with server-wide locks)
		if ("contextAction".equals(method)) {
			var action = request.optJSONObject("action");
			if (action != null && !action.optBoolean("authoring", true)) {
				return false;
			}
		}
		return switch (method) {
		case "propertyEditor", "describeTree", "contextMenu", "contextAction", "applySourceMutation" -> true;
		default -> false;
		};
	}

	private static void recordInvocation(EngineRef engineRef, String method, long durationNanos, CachedEngineRuntimeLookup runtimeLookup,
			boolean error, boolean methodCacheHit, boolean frontendAuthoring, boolean lockContended,
			long lockWaitNanos, long lockHeldNanos, long methodFingerprintNanos, long methodCacheLookupNanos) {
		var key = engineRef.qname + "|" + method;
		var stats = invocationStats.computeIfAbsent(key, k -> new InvocationStats(engineRef.qname, method));
		stats.record(durationNanos, runtimeLookup, error, methodCacheHit, frontendAuthoring, lockContended,
				lockWaitNanos, lockHeldNanos, methodFingerprintNanos, methodCacheLookupNanos);
	}

	private static JSONObject bridgeCacheInfo(String engineQName) throws JSONException {
		var normalizedEngineQName = normalizeEngineQName(engineQName);
		var methods = new JSONObject();
		var authoringLocks = new JSONArray();
		var pooledRuntimeCount = 0;
		var availableRuntimeCount = 0;
		var pendingAuthoringMutations = 0;
		for (var entry : invocationStats.entrySet()) {
			var stats = entry.getValue();
			if (stats.engineQName.equals(normalizedEngineQName)) {
				methods.put(stats.method, stats.toJson());
			}
		}
		for (var entry : engineRuntimeCache.entrySet()) {
			if (!entry.getKey().startsWith(normalizedEngineQName + "|")) {
				continue;
			}
			var pool = entry.getValue();
			synchronized (pool) {
				pooledRuntimeCount += pool.cachedCount;
				availableRuntimeCount += pool.available.size();
			}
		}
		for (var i = 0; i < frontendAuthoringLocks.length; i++) {
			var lock = frontendAuthoringLocks[i];
			pendingAuthoringMutations += frontendAuthoringMutationGates[i].pendingMutations();
			if (lock.isLocked() || lock.hasQueuedThreads()) {
				authoringLocks.put(frontendAuthoringLockStates[i].toJson(i, lock));
			}
		}
		return new JSONObject()
				.put("generation", cacheGeneration.get())
				.put("dataGeneration", dataGeneration.get())
				.put("sharedRuntimeCount", sharedRuntimes.size())
				.put("sharedRuntimePreparing", sharedRuntimeFlights.size())
				.put("sharedRuntimeWaiting", sharedRuntimeWaiters.get())
				.put("childInvocations", new JSONObject(com.twinsoft.convertigo.engine.requesters.RequestableInvocation.statistics()))
				.put("sourceCacheSize", engineSourceCache.size())
				.put("runtimeCacheSize", engineRuntimeCache.size())
				.put("runtimePoolLimit", runtimePoolLimit())
				.put("runtimeIdleSeconds", runtimeIdleSeconds())
				.put("pooledRuntimeCount", pooledRuntimeCount)
				.put("availableRuntimeCount", availableRuntimeCount)
				.put("methodResponseCacheSize", methodResponseCache.size())
				.put("sharedMetadata", new JSONObject().put("entries", sharedMetadata.size())
						.put("characters", sharedMetadata.characters()).put("builds", sharedMetadata.builds())
						.put("hits", sharedMetadata.hits()).put("joins", sharedMetadata.joins()))
				.put("sourceMemo", new JSONObject().put("entries", sourceMemo.size())
						.put("characters", sourceMemo.characters()).put("builds", sourceMemo.builds())
						.put("hits", sourceMemo.hits()).put("joins", sourceMemo.joins()))
				.put("methodResponseCacheHits", methodResponseCacheHits.sum())
				.put("methodResponseCacheMisses", methodResponseCacheMisses.sum())
				.put("methodResponseCacheInvalidations", methodResponseCacheInvalidations.sum())
				.put("frontendAuthoringSingleFlightLimit", FRONTEND_AUTHORING_SINGLE_FLIGHT_LIMIT)
				.put("frontendAuthoringSingleFlightActive", frontendAuthoringFlights.size())
				.put("frontendAuthoringSingleFlightLeaders", frontendAuthoringFlightLeaders.sum())
				.put("frontendAuthoringSingleFlightFollowers", frontendAuthoringFlightFollowers.sum())
				.put("frontendAuthoringSingleFlightBypasses", frontendAuthoringFlightBypasses.sum())
				.put("frontendAuthoringPendingMutations", pendingAuthoringMutations)
				.put("frontendAuthoringLocks", authoringLocks)
				.put("methods", methods);
	}

	private static String bridgeCacheInfoString(String engineQName) {
		try {
			return bridgeCacheInfo(engineQName).toString();
		} catch (JSONException e) {
			return "{}";
		}
	}

	private Object invokePrepared(EngineRef engineRef, File engineFile, CachedEngineSource engineSource, String method,
			JSONObject request, Context convertigoContext, org.mozilla.javascript.Context cx, Scriptable engineScope,
			Scriptable engineObject, CachedEngineRuntimeLookup runtimeLookup) throws EngineException {
		var api = runtimeLookup == null ? null : runtimeLookup.runtime().api();
		try (var frame = api == null ? null : api.enter(cx)) {
		if (frame != null) {
			engineScope = frame.scope;
			FlowPackages.initializeChildScope(engineScope, api.sharedScope());
		}
		var previous = currentRuntime.get();
		var nested = runtimeLookup != null && runtimeLookup.nested() && api == null;
		var saved = nested ? new Object[INVOCATION_BINDINGS.length] : null;
		if (nested) for (int i = 0; i < saved.length; i++) saved[i] = engineScope.get(INVOCATION_BINDINGS[i], engineScope);
		var generation = callGeneration(request, convertigoContext);
		currentRuntime.set(new RuntimeInvocation(runtimeLookup, generation, "run".equals(method)));
		try {
			if (convertigoContext != null) {
				engineScope.put("context", engineScope, org.mozilla.javascript.Context.toObject(convertigoContext, engineScope));
			} else engineScope.delete("context");
			initializeSourceScope(engineScope, engineFile);
			// Nested execution already has the same project packages. Do not purge or
			// replace them while the caller's JavaScript stack is still active.
			if (!nested) FlowPackages.bind(cx, engineScope, generation);
			return invokeBound(engineRef, engineSource, method, request, cx, engineScope, engineObject, runtimeLookup, generation);
		} finally {
			try {
				try { if (!nested && api == null) FlowPackages.restore(engineScope, generation); }
				finally {
					if (runtimeLookup != null) for (int i = 0; i < INVOCATION_BINDINGS.length; i++) {
						if (nested && saved[i] != Scriptable.NOT_FOUND) engineScope.put(INVOCATION_BINDINGS[i], engineScope, saved[i]);
						else engineScope.delete(INVOCATION_BINDINGS[i]);
					}
				}
			} finally {
				if (previous == null) currentRuntime.remove(); else currentRuntime.set(previous);
			}
		}
		}
	}

	private static CachedEngineRuntimeLookup reusableCallerRuntime(EngineRef ref, CachedEngineSource source,
			String method, JSONObject request, Context context) {
		var frame = currentRuntime.get();
		if (!"run".equals(method) || frame == null || !frame.execution() || frame.lookup() == null
				|| !EnginePropertiesManager.getPropertyAsBoolean(PropertyName.JAVASCRIPT_RUNTIME_REENTRANT)) return null;
		var parent = frame.lookup();
		var runtime = parent.runtime();
		if (runtime.generation() != cacheGeneration.get() || runtime.dataGeneration() != dataGeneration.get()
				|| !parent.key().equals(ref.qname + "|" + source.sourceName() + "|pool")
				|| frame.projectGeneration() != callGeneration(request, context)
				|| !Boolean.TRUE.equals(ScriptableObject.getProperty(runtime.engineObject(), "supportsNestedExecution"))) return null;
		return new CachedEngineRuntimeLookup(runtime, parent.pool(), true, false, parent.key(), parent.generation(), parent.size(), true);
	}

	/** @return the class path generation of the call running on this thread, null for none (the engine) */
	static ClassLoader currentProjectGeneration() {
		var frame = currentRuntime.get();
		return frame == null ? null : frame.projectGeneration();
	}

	/**
	 * @return the generation of the class path of the project of a call: the one of the Convertigo context running a
	 *         Flow, else the one of the project the request names; null for none
	 */
	static ClassLoader callGeneration(JSONObject request, Context convertigoContext) {
		try {
			Project project = convertigoContext == null ? null : convertigoContext.project;
			if (project == null && Engine.theApp != null && Engine.theApp.databaseObjectsManager != null) {
				var qname = request.optString("flowQName", "");
				var name = qname.contains(".") ? qname.substring(0, qname.indexOf('.'))
						: request.optString("project", projectNameForDir(request.optString("projectDir", "")));
				if (name != null && !name.isBlank()) {
					project = Engine.theApp.databaseObjectsManager.getLoadedProjectByName(name);
				}
			}
			return project == null ? null : project.getProjectClassLoader();
		} catch (Exception e) {
			return null;
		}
	}

	private Object invokeBound(EngineRef engineRef, CachedEngineSource engineSource, String method, JSONObject request,
			org.mozilla.javascript.Context cx, Scriptable engineScope, Scriptable engineObject,
			CachedEngineRuntimeLookup runtimeLookup, ClassLoader generation) throws EngineException {
		var projectDir = request.optString("projectDir", "");
		engineScope.put("__flowProjectDir", engineScope, projectDir);
		engineScope.put("__flowBridgeClassSource", engineScope, bridgeClassSource());
		engineScope.put("__flowBridgeClassResource", engineScope, bridgeClassResource());
		engineScope.put("__flowBridgeInfo", engineScope, bridgeCacheInfoString(engineRef.qname));
		if (runtimeLookup == null) {
			engineScope.put("__flowBridgeRuntimeCacheEnabled", engineScope, false);
			engineScope.delete("__flowBridgeRuntimeCacheHit");
			engineScope.delete("__flowBridgeRuntimeCacheKey");
			engineScope.delete("__flowBridgeRuntimeCacheGeneration");
			engineScope.delete("__flowBridgeRuntimeCacheSize");
		} else {
			engineScope.put("__flowBridgeRuntimeCacheEnabled", engineScope, true);
			engineScope.put("__flowBridgeRuntimeCacheHit", engineScope, runtimeLookup.hit());
			engineScope.put("__flowBridgeRuntimeCacheKey", engineScope, runtimeLookup.key());
			engineScope.put("__flowBridgeRuntimeCacheGeneration", engineScope, runtimeLookup.generation());
			engineScope.put("__flowBridgeRuntimeCacheSize", engineScope, runtimeLookup.size());
		}
		if (engineObject == null) {
			var engine = RhinoUtils.evalCachedJavascript(cx, engineScope, engineSource.source(), engineSource.sourceName(), engineSource.file());
			if (engine == null || Undefined.isUndefined(engine)) {
				engine = ScriptableObject.getProperty(engineScope, engineRef.objectName);
			}
			if (!(engine instanceof Scriptable)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" must evaluate to a JavaScript object.");
			}
			engineObject = (Scriptable) engine;
		}

		var function = ScriptableObject.getProperty(engineObject, method);
		// Optional execution-only protocol. Older providers and authoring methods
		// keep their text contract. Detach while the runtime is still borrowed.
		var objectResult = false;
		if ("run".equals(method)) {
			var objectFunction = ScriptableObject.getProperty(engineObject, "runObject");
			if (objectFunction instanceof Function) {
				function = objectFunction;
				objectResult = true;
			}
		}
		if (!(function instanceof Function)) {
			throw new EngineException("Flow engine \"" + engineRef.qname + "\" does not expose method \"" + method + "\".");
		}

		var detacher = objectResult ? new ResultDetacher(engineScope, generation) : null;
		String inputJson;
		try (var allocation = com.twinsoft.convertigo.engine.util.AllocationTrace.open("boundary", "input-encode", method)) {
			inputJson = request.toString();
		}
		var arguments = objectResult ? com.twinsoft.convertigo.engine.util.AllocationTrace.isActive()
				? new Object[] { inputJson, detacher, new org.mozilla.javascript.NativeJavaClass(engineScope,
						com.twinsoft.convertigo.engine.util.AllocationTrace.class) }
				: new Object[] { inputJson, detacher } : new Object[] { inputJson };
		var result = ((Function) function).call(cx, engineScope, engineObject, arguments);
		FlowStudioSupport.performanceProfileMark("bridge." + method + ".engineCall");
		if (objectResult && (result == null || result != detacher.token)) {
			throw new EngineException("Object execution must return the detached result token.");
		}
		var response = objectResult ? detacher.value : toJsonObject(result, engineRef.qname, method);
		FlowStudioSupport.performanceProfileMark("bridge." + method + ".serialization");
		return response;
	}

	/** Called inside the provider's error boundary, while its realm is borrowed. */
	private static final class ResultDetacher extends org.mozilla.javascript.BaseFunction {
		private static final long serialVersionUID = 1L;
		private Map<String, Object> value;
		private Object token;
		ResultDetacher(Scriptable scope) { this(scope, null); }
		ResultDetacher(Scriptable scope, ClassLoader generation) {
			setParentScope(scope);
			setPrototype(ScriptableObject.getFunctionPrototype(scope));
			var sanitized = new org.mozilla.javascript.BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
				private static final long serialVersionUID = 1L;
				@Override public Object call(org.mozilla.javascript.Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
					if (args.length != 2 || !(args[1] instanceof org.mozilla.javascript.Callable policy)) {
						throw org.mozilla.javascript.Context.reportRuntimeError("A result sanitization policy is required");
					}
					try (var allocation = com.twinsoft.convertigo.engine.util.AllocationTrace.open("boundary", "sanitize-detach", "")) {
						value = com.twinsoft.convertigo.engine.util.SanitizedJson.map(args[0], cx, callScope, policy);
						return token = org.mozilla.javascript.Context.javaToJS(value, callScope);
					} catch (JSONException e) {
						throw org.mozilla.javascript.Context.throwAsScriptRuntimeEx(e);
					}
				}
			};
			defineProperty("sanitize", sanitized, ScriptableObject.READONLY | ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
			var rows = new org.mozilla.javascript.BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
				private static final long serialVersionUID = 1L;
				@Override public Object call(org.mozilla.javascript.Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
					if (args.length != 3 || !(args[2] instanceof org.mozilla.javascript.Callable policy))
						throw org.mozilla.javascript.Context.reportRuntimeError("A registered row plan is required");
					com.twinsoft.convertigo.engine.flow.rows.ResultDocument document = null;
					try {
						var envelope = com.twinsoft.convertigo.engine.util.SanitizedJson.map(args[0], cx, callScope, policy);
						var plan = com.twinsoft.convertigo.engine.util.SanitizedJson.map(args[1], cx, callScope, policy);
						document = com.twinsoft.convertigo.engine.flow.rows.RowPlanFactory.create(plan, generation);
						value = new com.twinsoft.convertigo.engine.flow.rows.StreamResult(envelope, document);
						return token = org.mozilla.javascript.Context.javaToJS(value, callScope);
					} catch (Exception error) {
						if (document != null) try { document.close(); } catch (Exception ignored) { }
						// A plan can contain credentials. Do not retain an exception with that plan or URL.
						var failure = cx.newObject(callScope);
						ScriptableObject.putProperty(failure,"code","ROW_STREAM_PLAN");
						ScriptableObject.putProperty(failure,"message","Unable to prepare bounded row result");
						ScriptableObject.putProperty(failure,"status",error instanceof com.twinsoft.convertigo.engine.flow.rows.RowStreamException bounded ? bounded.status() : 400);
						throw new org.mozilla.javascript.JavaScriptException(failure,"row-result",0);
					}
				}
			};
			defineProperty("rows", rows, ScriptableObject.READONLY | ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
			// a result whose field was emitted in stream: its emitted rows come from the spool, its other keys follow them
			var emitted = new org.mozilla.javascript.BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
				private static final long serialVersionUID = 1L;
				@Override public Object call(org.mozilla.javascript.Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
					if (args.length != 4 || !(args[3] instanceof org.mozilla.javascript.Callable policy)
							|| !(org.mozilla.javascript.Context.jsToJava(args[2], Object.class) instanceof com.twinsoft.convertigo.engine.flow.rows.EmitSpool spool))
						throw org.mozilla.javascript.Context.reportRuntimeError("A registered emitted field is required");
					com.twinsoft.convertigo.engine.flow.rows.ResultDocument document = null;
					try {
						var envelope = com.twinsoft.convertigo.engine.util.SanitizedJson.map(args[0], cx, callScope, policy);
						var field = org.mozilla.javascript.Context.toString(args[1]);
						var result = envelope.get("result") instanceof java.util.Map<?, ?> map ? new java.util.LinkedHashMap<String, Object>(com.twinsoft.convertigo.engine.util.JsonValues.map(map)) : new java.util.LinkedHashMap<String, Object>();
						// attr is not written by a JSON response (NativeJsonSupport.projectResult)
						result.remove(field);
						result.remove("attr");
						if (spool.committed()) {
							// the rows are in the response already: the other keys end it, the servlet writes nothing more
							spool.finish(result);
							var written = new java.util.LinkedHashMap<String, Object>(envelope);
							written.put("result", com.twinsoft.convertigo.engine.util.JsonValues.object());
							value = written;
							return token = org.mozilla.javascript.Context.javaToJS(value, callScope);
						}
						document = new com.twinsoft.convertigo.engine.flow.rows.ResultDocument(spool, field, result,
								new com.twinsoft.convertigo.engine.flow.rows.ResultDocument.Limits(1_000_000, 256L * 1024 * 1024, 256 * 1024, 2, 512 * 1024));
						value = new com.twinsoft.convertigo.engine.flow.rows.StreamResult(envelope, document);
						return token = org.mozilla.javascript.Context.javaToJS(value, callScope);
					} catch (Exception error) {
						if (document != null) try { document.close(); } catch (Exception ignored) { }
						else try { spool.close(); } catch (Exception ignored) { }
						var failure = cx.newObject(callScope);
						ScriptableObject.putProperty(failure,"code","ROW_STREAM_EMIT");
						ScriptableObject.putProperty(failure,"message","Unable to prepare the emitted result");
						ScriptableObject.putProperty(failure,"status",error instanceof com.twinsoft.convertigo.engine.flow.rows.RowStreamException bounded ? bounded.status() : 500);
						throw new org.mozilla.javascript.JavaScriptException(failure,"emitted-result",0);
					}
				}
			};
			defineProperty("emitted", emitted, ScriptableObject.READONLY | ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);

		}
		@Override public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			try (var allocation = com.twinsoft.convertigo.engine.util.AllocationTrace.open("boundary", "detach", "")) {
				value = com.twinsoft.convertigo.engine.util.DetachedJson.map(args.length == 0 ? null : args[0], cx, scope);
				return token = org.mozilla.javascript.Context.javaToJS(value, scope);
			} catch (JSONException e) {
				throw org.mozilla.javascript.Context.throwAsScriptRuntimeEx(e);
			}
		}
	}

	private static CachedMethodResponse cachedMethodResponse(String key) {
		var cached = methodResponseCache.get(key);
		if (cached != null && cached.current()) {
			methodResponseCacheHits.increment();
			return cached;
		}
		methodResponseCacheMisses.increment();
		return null;
	}

	private static void storeMethodResponse(String key, EngineRef engineRef, String method, JSONObject request, JSONObject response) {
		if (!isCacheableResponse(method, request, response)) {
			return;
		}
		if (methodResponseCache.size() >= METHOD_RESPONSE_CACHE_LIMIT) {
			methodResponseCache.clear();
			methodResponseAliases.clear();
			methodResponseCacheInvalidations.increment();
		}
		var root = projectRoot(request == null ? "" : request.optString("projectDir", ""));
		methodResponseCache.put(key,
				new CachedMethodResponse(serializedResponse(response), cacheGeneration.get(), dataRevision(root), root));
		methodResponseAliases.put(methodResponseAliasKey(engineRef, method, request), key);
	}

	private static String methodResponseAliasKey(EngineRef engineRef, String method, JSONObject request) {
		var projectDir = request == null ? "" : request.optString("projectDir", "");
		return engineRef.qname + "|" + method + "|"
				+ (request == null ? "" : request.optString("target", "")) + "|"
				+ canonicalPath(projectDir == null || projectDir.isBlank() ? null : new File(projectDir));
	}

	private static CachedMethodResponse aliasedMethodResponse(EngineRef engineRef, String method, JSONObject request) {
		var key = methodResponseAliases.get(methodResponseAliasKey(engineRef, method, request));
		if (key == null || key.isBlank()) {
			return null;
		}
		var cached = methodResponseCache.get(key);
		return cached != null && cached.current() ? cached : null;
	}

	private static void attachDescribeTreeSnapshot(EngineRef engineRef, String method, JSONObject request) throws JSONException {
		if (!"authoringTree".equals(method) || request == null
				|| !"frontend".equals(request.optString("surface", "frontend"))) {
			return;
		}
		var describeRequest = new JSONObject()
				.put("target", "engine")
				.put("projectDir", request.optString("projectDir", ""));
		var cached = aliasedMethodResponse(engineRef, "describeTree", describeRequest);
		FlowStudioSupport.performanceProfileMark(cached == null
				? "bridge.authoringTree.describeSnapshotMissing"
				: "bridge.authoringTree.describeSnapshotFound");
		if (cached != null) {
			request.put("__flowBridgeDescribeTreeSnapshot", new JSONObject(cached.response()));
		}
	}

	/**
	 * @return the key of a response in memory: from its key on disk when it has one, which walks every file the
	 *         dependency fingerprint walks and more (one walk instead of two), else from the dependency fingerprint
	 */
	private static String responseCacheKey(EngineRef engineRef, File engineFile, CachedEngineSource engineSource, String method,
			JSONObject request, String diskKey) {
		if (diskKey == null) {
			return methodResponseCacheKey(engineRef, engineFile, engineSource, method, request);
		}
		return engineRef.qname + "|" + engineSource.sourceName() + "|disk:" + diskKey + "|" + cacheGeneration.get() + "|"
				+ dataRevision(projectRoot(request.optString("projectDir", ""))) + "|" + method;
	}

	private static String methodResponseCacheKey(EngineRef engineRef, File engineFile, CachedEngineSource engineSource, String method,
			JSONObject request) {
		return engineRef.qname + "|" + engineSource.sourceName() + "|"
				+ methodResponseDependencyFingerprint(engineFile, request) + "|" + cacheGeneration.get() + "|"
				+ dataRevision(projectRoot(request.optString("projectDir", ""))) + "|" + method + "|"
				+ request.toString();
	}

	// The dependency fingerprint walks every source file of the engine, the project and its
	// references: about a thousand entries, each one read of its attributes, still seconds on a
	// network file system, on each cacheable call. Writes made through Convertigo already bump the
	// cache generations (part of the key) and clear these entries; files changed outside Convertigo
	// (git, shell, deployment) are seen through a cheap per-root stamp, or after a few seconds.
	private static final long ROOT_FINGERPRINT_TTL_MS = 5000;
	/** Its monitor orders the publications and the removals, never held while a root is walked. */
	private static final Map<String, RootFingerprint> rootFingerprints = new ConcurrentHashMap<>();
	/** The walks in progress, by root: removed once published or failed, so a failure is never kept. */
	private static final Map<String, CompletableFuture<RootFingerprint>> rootFingerprintWalks = new ConcurrentHashMap<>();
	/** Advanced by each removal of fingerprints, under the monitor of the map: a walk started before it is not kept. */
	private static volatile long rootFingerprintRemovals;
	/** The entries of a source root ({@link FlowSourceLayout#root}) the runtimes read, relative to it. */
	private static final String[] FLOW_ROOT_ENTRIES = { "Engine.js", "engine.yaml", "modules", "blocks", "types", "lib", "resources", "schemas" };

	/**
	 * @param files the state of its files, the hashes of those read: kept with this entry only
	 * @param newest the most recent date of its files, Long.MIN_VALUE for none
	 */
	record RootFingerprint(String stamp, String value, long computedAt, Map<String, FlowSourceFingerprint.FileState> files,
			long newest) {
	}

	/**
	 * @param root the folder of the project, its key
	 * @param flowRoot its source root
	 * @param base the folder walk names its entries from
	 */
	static String cachedRootFingerprint(String kind, File root, File flowRoot, File base,
			java.util.function.Consumer<FlowSourceFingerprint> walk) {
		return cachedRoot(kind, root, flowRoot, base, walk).value();
	}

	/** {@link #cachedRootFingerprint}, with the date of the walk and that of its most recent file. */
	static RootFingerprint cachedRoot(String kind, File root, File flowRoot, File base,
			java.util.function.Consumer<FlowSourceFingerprint> walk) {
		var key = kind + ":" + canonicalPath(root);
		var stamp = new StringBuilder();
		for (var file : new File[] { root, flowRoot, new File(root, "c8oProject.yaml"), new File(root, "flow-deploy.json"),
				new File(flowRoot, "Engine.js"), new File(flowRoot, "engine.yaml") }) {
			try {
				var attributes = java.nio.file.Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes.class);
				stamp.append(attributes.lastModifiedTime().toMillis()).append(':').append(attributes.isRegularFile() ? attributes.size() : 0);
			} catch (java.io.IOException | RuntimeException e) {
				stamp.append("0:0");
			}
			stamp.append(';');
		}
		// expired only (the same stamp): its value of a few seconds ago, while another caller walks it again
		return rootFingerprint(key, stamp.toString(), base, walk,
				cached -> System.currentTimeMillis() - cached.computedAt() < ROOT_FINGERPRINT_TTL_MS, true);
	}

	/** The kind of the walks of the persistent keys ({@link FlowAuthoringKeys}), the key of a root being this prefix and its canonical path. */
	static final String AUTHORING_ROOT = "authoring:";

	/**
	 * The walk of the files of a project an authoring response may depend on ({@link #authoringWalk}), without reading
	 * any attribute when its last walk is recent enough: the changes made through the engine forget it at once
	 * ({@link #forgetRootFingerprints}), those made outside are seen by the next walk, which {@link FlowAuthoringKeys}
	 * does in the background before this age is reached. A walk older than this age is never served: its callers wait
	 * for the walk of the root, one for all of them.
	 *
	 * @param key {@link #AUTHORING_ROOT} and the canonical path of the folder
	 * @param maxAge the age of a walk from which it is walked again
	 * @param settle whether a walk showing a recent file ({@link #RECENT_FILE_MS}) is walked again once its file is no
	 *        longer recent, so that it can give a key
	 */
	static RootFingerprint authoringRoot(String key, File root, long maxAge, boolean settle) {
		return rootFingerprint(key, "", root, FlowEngineBridge::authoringWalk, cached -> {
			if (System.currentTimeMillis() - cached.computedAt() >= maxAge) {
				return false;
			}
			return !settle || !unsettled(cached) || System.currentTimeMillis() < cached.newest() + RECENT_FILE_MS;
		}, false);
	}

	/** @return whether a walk showed a file modified too recently for its date to tell its next change ({@link #RECENT_FILE_MS}) */
	static boolean unsettled(RootFingerprint walked) {
		return walked.newest() != Long.MIN_VALUE && walked.computedAt() - walked.newest() < RECENT_FILE_MS;
	}

	/** @return the number of removals of fingerprints so far: a value computed from walks before another one may be outdated */
	static long rootFingerprintRemovals() {
		return rootFingerprintRemovals;
	}

	/** @return the walk of a root kept in memory, null for none: no file read */
	static RootFingerprint knownRoot(String key) {
		return rootFingerprints.get(key);
	}

	/**
	 * @param stamp what tells, without walking it, that the root is the same: a walk of another stamp is not served
	 * @param fresh whether a walk of the same stamp is served without walking the root again
	 * @param servedWhileWalked whether a walk of the same stamp that is not fresh is served while another caller walks
	 *        the root again, else its callers wait for that walk
	 */
	private static RootFingerprint rootFingerprint(String key, String stamp, File base,
			java.util.function.Consumer<FlowSourceFingerprint> walk, java.util.function.Predicate<RootFingerprint> fresh,
			boolean servedWhileWalked) {
		// single flight for each root: one caller walks it, the others wait for that walk, other roots walked meanwhile
		for (;;) {
			var cached = rootFingerprints.get(key);
			var same = cached != null && cached.stamp().equals(stamp);
			if (same && fresh.test(cached)) {
				return cached;
			}
			var walking = rootFingerprintWalks.get(key);
			if (walking == null) {
				var created = new CompletableFuture<RootFingerprint>();
				walking = rootFingerprintWalks.putIfAbsent(key, created);
				if (walking == null) {
					return walkRoot(key, created, stamp, cached, base, walk);
				}
			}
			if (same && servedWhileWalked) {
				return cached;
			}
			try {
				var value = walking.get();
				if (value != null) {
					return value;
				}
			} catch (java.util.concurrent.ExecutionException failure) {
				if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
				if (failure.getCause() instanceof Error error) throw error;
				throw new IllegalStateException(failure.getCause());
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for the walk of " + key, interrupted);
			}
		}
	}

	private static RootFingerprint walkRoot(String key, CompletableFuture<RootFingerprint> walking, String stamp,
			RootFingerprint cached, File base, java.util.function.Consumer<FlowSourceFingerprint> walk) {
		var started = rootFingerprintRemovals;
		var now = System.currentTimeMillis();
		RootFingerprint value;
		if (key.startsWith(AUTHORING_ROOT)) {
			FlowAuthoringKeys.statistics.walks.increment();
		}
		try {
			// a file rewritten with the same content keeps showing as the walk before showed it
			var fingerprint = new FlowSourceFingerprint(base, cached == null ? null : cached.files());
			walk.accept(fingerprint);
			var text = fingerprint.text();
			// unchanged: the same instance, which its users compare first
			if (cached != null && cached.value().equals(text)) {
				text = cached.value();
			}
			value = new RootFingerprint(stamp, text, now, fingerprint.files(), fingerprint.newest());
		} catch (Throwable failure) {
			rootFingerprintWalks.remove(key, walking);
			// the interruption of this caller is not a failure of the walk: a caller waiting for it walks again
			if (FlowRuntimeApi.interruption(failure)) walking.complete(null);
			else walking.completeExceptionally(failure);
			throw failure;
		}
		synchronized (rootFingerprints) {
			// forgotten since the walk started: it may be older than the change
			if (rootFingerprintRemovals == started) {
				rootFingerprints.put(key, value);
			}
		}
		rootFingerprintWalks.remove(key, walking);
		walking.complete(value);
		return value;
	}

	/**
	 * Forgets the fingerprints of these roots, of all of them without any: no walk started before is kept or joined,
	 * and the persistent keys walk them again at their next use ({@link FlowAuthoringKeys#forgotten}).
	 */
	private static void forgetRootFingerprints(String... keys) {
		synchronized (rootFingerprints) {
			rootFingerprintRemovals++;
			rootFingerprintWalks.clear();
			if (keys.length == 0) {
				rootFingerprints.clear();
			} else {
				for (var key : keys) {
					rootFingerprints.remove(key);
				}
			}
		}
		FlowAuthoringKeys.forgotten(keys.length == 0);
	}

	/**
	 * Files of a project were written by the engine without a notification of what they hold (its beans saved, files
	 * added from the Studio): the persistent keys of the authoring responses walk the project again at their next use,
	 * instead of seeing the change within the delay of the changes made outside the engine.
	 *
	 * @param directory the folder of the project
	 */
	public static void projectFilesWritten(File directory) {
		if (directory != null) {
			forgetRootFingerprints(AUTHORING_ROOT + canonicalPath(directory));
		}
	}

	/** Changed when the persistent key no longer tells everything a response depends on: every entry is computed again. */
	static final String PERSISTENT_KEY_FORMAT = "authoring-response-1";
	/**
	 * A file modified less than this before the walk that saw it could still change without its date changing (the
	 * resolution of the dates of some file systems): the responses of such a state are not kept on disk, as git does
	 * not trust such an entry of its index.
	 */
	static final long RECENT_FILE_MS = 3000;
	/** The projects a frontend builder of a project resolves by their name, besides its references. */
	private static final String[] FRONTEND_BUILDER_PROJECTS = { "lib_flow_frontbuilder_svelte" };
	/** The calls of the background preparations, which give way to the others. */
	private static final ThreadLocal<Boolean> background = new ThreadLocal<>();
	/** The calls in progress of other threads than the background preparations, and when the last one ended. */
	private static final AtomicInteger foregroundCalls = new AtomicInteger();
	private static volatile long lastForegroundCall = System.nanoTime();
	/** The method whose calls only tell, in a probe, whether their response is on disk. */
	private static final ThreadLocal<String> probedMethod = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> probeAnswered = new ThreadLocal<>();
	private static volatile String engineStamp;

	/** Removes the responses kept on disk, as asked by the cache clear action of the Studio: they are computed again. */
	public static void clearPersistedResponses() {
		var diskCache = FlowAuthoringDiskCache.get();
		if (diskCache != null) {
			diskCache.clear();
		}
	}

	/** Runs an action of a background preparation: its calls do not count as calls of the Studio or of requests. */
	static <T> T inBackground(java.util.concurrent.Callable<T> action) throws Exception {
		var previous = background.get();
		background.set(Boolean.TRUE);
		try {
			return action.call();
		} finally {
			if (previous == null) background.remove(); else background.set(previous);
		}
	}

	/** @return the calls of other threads than the background preparations in progress */
	static int foregroundCalls() {
		return foregroundCalls.get();
	}

	/** @return the time ({@link System#nanoTime}) the last call of another thread than the background preparations ended */
	static long lastForegroundCall() {
		return lastForegroundCall;
	}

	/**
	 * Runs an action whose calls of a cacheable method answer {"ok": true, "persisted": true} without computing nor
	 * reading their response when it is on disk ({@link #probeAnswered}); the other calls run as usual.
	 */
	static <T> T probing(String method, java.util.concurrent.Callable<T> action) throws Exception {
		var previous = probedMethod.get();
		probedMethod.set(method);
		probeAnswered.remove();
		try {
			return action.call();
		} finally {
			if (previous == null) probedMethod.remove(); else probedMethod.set(previous);
		}
	}

	/** @return whether a call of the probe of this thread answered that its response is on disk */
	public static boolean probeAnswered() {
		return Boolean.TRUE.equals(probeAnswered.get());
	}

	private static boolean probed(String method, FlowAuthoringDiskCache diskCache, String diskKey) {
		if (diskKey == null || !method.equals(probedMethod.get()) || !diskCache.contains(diskKey)) {
			return false;
		}
		probeAnswered.set(Boolean.TRUE);
		return true;
	}

	private static JSONObject probeAnswer() throws JSONException {
		return new JSONObject().put("ok", true).put("persisted", true);
	}

	/** The part of an entry on disk holding the response. */
	private static final String RESPONSE_PART = "response";
	/**
	 * The part of an entry on disk holding the shared definitions its response references: the provider publishes the
	 * definitions of the nodes of a tree once in the server map (FlowVirtualObject.resolveSharedDefinitions) and the
	 * nodes only carry their reference, a hash of their content.
	 */
	private static final String DEFINITIONS_PART = "virtualDefinitions";
	private static final String SHARED_DEFINITIONS_KEY = "flow.virtualDefinitions";
	private static final Pattern DEFINITION_REF = Pattern.compile("vd-[0-9a-f]{32}");
	/** The icon files a response shows by their path (rendered by the provider in the workspace), escaped or not. */
	private static final Pattern ICON_FILE = Pattern.compile("iconFile(?:16|32)?\\\\*\"\\s*:\\s*\\\\*\"(/[^\"\\\\]+)");

	/**
	 * @return the response of a key read from disk, null when it is not there; the shared definitions it references
	 *         published first, as its computation published them
	 */
	static JSONObject persistedResponse(FlowAuthoringDiskCache diskCache, String diskKey, String method) {
		if (diskCache == null || diskKey == null) {
			return null;
		}
		var parts = diskCache.read(diskKey);
		var text = parts == null ? null : parts.get(RESPONSE_PART);
		if (text == null) {
			return null;
		}
		try {
			var definitions = parts.get(DEFINITIONS_PART);
			if (!iconFilesExist(text) || definitions != null && !iconFilesExist(definitions)) {
				// rendered when it was computed, removed since: computed again, which renders them
				return null;
			}
			var response = new EngineJSONObject(text);
			if (definitions != null && !definitions.isEmpty()) {
				var registry = sharedDefinitions(true);
				if (registry == null) {
					return null;
				}
				var entries = new JSONObject(definitions);
				for (var keys = entries.keys(); keys.hasNext();) {
					var ref = String.valueOf(keys.next());
					registry.putIfAbsent(ref, entries.getString(ref));
				}
			}
			return response;
		} catch (JSONException e) {
			// signed but not JSON: computed again and replaced
			Engine.logEngine.debug("(FlowEngineBridge) Unreadable " + method + " response on disk, computed again.", e);
			return null;
		}
	}

	/** @return whether the icon files a response shows by their path exist */
	static boolean iconFilesExist(String text) {
		var seen = new HashSet<String>();
		for (var matcher = ICON_FILE.matcher(text); matcher.find() && seen.size() < 1024;) {
			var path = matcher.group(1);
			if (seen.add(path) && !new File(path).isFile()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the parts of the entry on disk of a response: its text and the shared definitions it references; null when
	 *         one of them is no longer published (the response could not be shown as computed)
	 */
	static Map<String, String> persistedParts(String text) {
		var parts = new LinkedHashMap<String, String>();
		parts.put(RESPONSE_PART, text);
		var matcher = DEFINITION_REF.matcher(text);
		if (!matcher.find()) {
			return parts;
		}
		var registry = sharedDefinitions(false);
		if (registry == null) {
			return null;
		}
		var definitions = new JSONObject();
		try {
			do {
				var ref = matcher.group();
				if (!definitions.has(ref)) {
					if (!(registry.get(ref) instanceof CharSequence entry)) {
						return null;
					}
					definitions.put(ref, entry.toString());
				}
			} while (matcher.find());
		} catch (JSONException e) {
			return null;
		}
		parts.put(DEFINITIONS_PART, definitions.toString());
		return parts;
	}

	/** @return the shared definitions published in the server map, created when asked, null without engine */
	@SuppressWarnings("unchecked")
	private static Map<Object, Object> sharedDefinitions(boolean create) {
		var app = Engine.theApp;
		var shared = app == null ? null : app.getShareServerMap();
		if (shared == null) {
			return null;
		}
		// the monitor the provider creates it under (sharedServerJavaMap)
		synchronized (shared) {
			var registry = shared.get(SHARED_DEFINITIONS_KEY);
			if (registry == null && create) {
				registry = new ConcurrentHashMap<Object, Object>();
				shared.set(SHARED_DEFINITIONS_KEY, registry);
			}
			return registry instanceof Map<?, ?> map ? (Map<Object, Object>) map : null;
		}
	}

	/**
	 * The key of the response of a cacheable call on disk ({@link FlowAuthoringDiskCache}), valid across restarts: the
	 * format of the key, the version and the code of the engine, the settings the providers read, the provider, the
	 * method, its request (the tag revision of this run left out), the files it names, and the state of the files of
	 * the provider, of the project of the request and of the projects it uses: those it references, directly or not,
	 * and those its frontend builder resolves by their name. Each of them by a walk of its descriptor, of its _c8oProject folder, of
	 * its source root and of its libs folder, the installed packages shown by their lock file (see
	 * {@link FlowSourceFingerprint}).
	 *
	 * The walks and the other reads of files a key shows are kept in memory and verified again in the background
	 * ({@link FlowAuthoringKeys}): a change made through the engine changes the key at once, a change made outside it
	 * within a bounded delay, and a key computed from them is the key computed from the files.
	 *
	 * @return null when the response is not kept on disk: a request carrying working copies (drafts), or files modified
	 *         too recently ({@link #RECENT_FILE_MS}) for their dates to tell their next change
	 */
	static String persistentResponseKey(String engineQName, File engineFile, String method, JSONObject request) {
		try {
			if (hasWorkingCopies(request)) {
				return null;
			}
			var prefix = PERSISTENT_KEY_FORMAT + '\n' + engineStamp() + '\n' + environmentStamp() + '\n' + engineQName + '\n' + method + '\n';
			return FlowAuthoringKeys.key(prefix, engineFile, request);
		} catch (Exception e) {
			Engine.logEngine.debug("(FlowEngineBridge) No persistent key for a " + method + " request.", e);
			return null;
		}
	}

	/** @return the line of a file in a key: its canonical path, its date and size, or that it is missing */
	static String fileFingerprintLine(File file) {
		var line = new StringBuilder();
		appendFileFingerprint(line, file);
		return line.toString();
	}

	/** The files of a project an authoring response may depend on: its descriptor, its beans, its source root, its Java libraries. */
	private static void authoringWalk(FlowSourceFingerprint walk) {
		walk.summarizing("node_modules", ".package-lock.json").header("authoring").entry("c8oProject.yaml").entry("_c8oProject")
				.entry(FlowSourceLayout.current().root()).entry("libs");
	}

	/** @return whether a request carries working copies: its response is not that of the files */
	static boolean hasWorkingCopies(JSONObject request) {
		if (request == null) {
			return false;
		}
		for (var name : List.of("sourceDrafts", "frontendSourceDrafts", "drafts", "sourceRemovals")) {
			var value = request.opt(name);
			if (value == null || value == JSONObject.NULL || value instanceof JSONObject object && object.length() == 0
					|| value instanceof JSONArray array && array.length() == 0) {
				continue;
			}
			return true;
		}
		return false;
	}

	/** @return the request as its persistent key shows it: without what only this run tells */
	static JSONObject persistentRequest(JSONObject request) throws JSONException {
		var copy = new JSONObject(request.toString());
		copy.remove("__flowBridgeDescribeTreeSnapshot");
		var tags = copy.optJSONObject("tagContext");
		if (tags != null && tags.has("revision")) {
			// fingerprint:generation, the generation counting the changes of this run: the fingerprint tells the content
			var revision = tags.optString("revision", "");
			var colon = revision.lastIndexOf(':');
			tags.put("revision", colon < 0 ? revision : revision.substring(0, colon));
		}
		return copy;
	}

	/** @return the JSON text of a value with the keys of its objects sorted: the same for the same content */
	static String canonicalJson(Object value) throws JSONException {
		var out = new StringBuilder();
		canonicalJson(value, out);
		return out.toString();
	}

	private static void canonicalJson(Object value, StringBuilder out) throws JSONException {
		if (value instanceof JSONObject object) {
			var keys = new ArrayList<String>();
			for (var iterator = object.keys(); iterator.hasNext();) {
				keys.add(String.valueOf(iterator.next()));
			}
			java.util.Collections.sort(keys);
			out.append('{');
			for (var i = 0; i < keys.size(); i++) {
				if (i > 0) out.append(',');
				out.append(JSONObject.quote(keys.get(i))).append(':');
				canonicalJson(object.opt(keys.get(i)), out);
			}
			out.append('}');
		} else if (value instanceof JSONArray array) {
			out.append('[');
			for (var i = 0; i < array.length(); i++) {
				if (i > 0) out.append(',');
				canonicalJson(array.opt(i), out);
			}
			out.append(']');
		} else if (value == null || value == JSONObject.NULL) {
			out.append("null");
		} else if (value instanceof String text) {
			out.append(JSONObject.quote(text));
		} else {
			out.append(value);
		}
	}

	/**
	 * @return the projects a response of a request may depend on, by their folders, in a stable order: the provider of
	 *         the runtime, the project of the request, the projects it references (directly or through other projects)
	 *         and the projects a frontend builder resolves by their name (or its folder given by the environment)
	 */
	static List<File> persistentDependencyRoots(File engineFile, JSONObject request) {
		var roots = new java.util.TreeMap<String, File>();
		var engineRoot = engineFile == null ? null : engineFile.getParentFile();
		if (engineRoot != null && engineRoot.getParentFile() != null) {
			var provider = engineRoot.getParentFile();
			roots.put(canonicalPath(provider), provider);
		}
		var projectDir = request == null ? "" : request.optString("projectDir", "");
		if (projectDir == null || projectDir.isBlank()) {
			return new ArrayList<>(roots.values());
		}
		var pending = new ArrayDeque<File>();
		pending.add(new File(projectDir));
		var seen = new HashSet<String>();
		var frontendBuilder = false;
		while (!pending.isEmpty() && roots.size() < 128) {
			var root = pending.poll();
			var path = canonicalPath(root);
			if (!seen.add(path)) {
				continue;
			}
			roots.put(path, root);
			var descriptor = new File(root, "c8oProject.yaml");
			if (!descriptor.isFile()) {
				continue;
			}
			try {
				var matcher = Pattern.compile("projectName:\\s*([A-Za-z0-9_.-]+)").matcher(FileUtils.readFileToString(descriptor, "UTF-8"));
				while (matcher.find()) {
					var referenced = projectRootByName(matcher.group(1), root.getParentFile());
					if (referenced != null) {
						pending.add(referenced);
					}
				}
				var engineYaml = new File(root, ENGINE_BASE_PATH + "engine.yaml");
				frontendBuilder = frontendBuilder || engineYaml.isFile() && FileUtils.readFileToString(engineYaml, "UTF-8").contains("frontbuilder");
			} catch (Exception e) {
				roots.put(path + "#unreadable", new File(path + "#unreadable"));
			}
		}
		if (frontendBuilder || request.optString("engineSource", "").contains("frontbuilder")) {
			for (var name : FRONTEND_BUILDER_PROJECTS) {
				var builder = projectRootByName(name, new File(projectDir).getParentFile());
				if (builder != null) {
					roots.putIfAbsent(canonicalPath(builder), builder);
				}
			}
			var configured = System.getenv("FLOW_FRONTBUILDER_RESOURCE_ROOT");
			for (var folder = configured == null || configured.isBlank() ? null : new File(configured.trim()); folder != null; folder = folder.getParentFile()) {
				if (new File(folder, "c8oProject.yaml").isFile()) {
					roots.putIfAbsent(canonicalPath(folder), folder);
					break;
				}
			}
		}
		return new ArrayList<>(roots.values());
	}

	/** @return the folder of a project by its name: the loaded one, else the one the engine knows, else a sibling folder */
	private static File projectRootByName(String name, File parent) {
		if (name == null || name.isBlank()) {
			return null;
		}
		try {
			var app = Engine.theApp;
			var loaded = app == null || app.databaseObjectsManager == null ? null : app.databaseObjectsManager.getLoadedProjectByName(name);
			if (loaded != null) {
				return loaded.getDirFile();
			}
			if (app != null) {
				var yaml = Engine.projectYamlFile(name);
				if (yaml != null && yaml.isFile()) {
					return yaml.getParentFile();
				}
			}
		} catch (Exception | LinkageError e) {
			// a sibling folder
		}
		var sibling = referencedProjectRoot(parent, name);
		// unknown: shown as missing, its arrival changes the key
		return sibling != null ? sibling : parent == null ? null : new File(parent, name);
	}

	/**
	 * @return the version of the engine and the code of the classes the authoring calls go through, read once per
	 *         JVM: a build of the engine changing them computes the responses again
	 */
	static String engineStamp() {
		var stamp = engineStamp;
		if (stamp != null) {
			return stamp;
		}
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			digest.update(com.twinsoft.convertigo.engine.ProductVersion.fullProductVersionID.getBytes(StandardCharsets.UTF_8));
			for (var type : new Class<?>[] { FlowEngineBridge.class, FlowStudioSupport.class, FlowSourceLayout.class,
					FlowSourceFingerprint.class, FlowAuthoringKeys.class, FlowPackages.class, FlowRuntimeApi.class, JsonValues.class, RhinoUtils.class,
					com.twinsoft.convertigo.beans.flow.FlowEngine.class, com.twinsoft.convertigo.beans.flow.Flow.class,
					com.twinsoft.convertigo.beans.flow.FlowWorkingCopies.class, com.twinsoft.convertigo.engine.tags.TagManager.class }) {
				try (var in = type.getResourceAsStream(type.getSimpleName() + ".class")) {
					digest.update(type.getName().getBytes(StandardCharsets.UTF_8));
					if (in != null) {
						digest.update(in.readAllBytes());
					}
				}
			}
			stamp = com.twinsoft.convertigo.engine.ProductVersion.fullProductVersionID + ":"
					+ java.util.HexFormat.of().formatHex(digest.digest()).substring(0, 32);
		} catch (Exception e) {
			stamp = com.twinsoft.convertigo.engine.ProductVersion.fullProductVersionID + ":" + System.nanoTime();
		}
		return engineStamp = stamp;
	}

	/** @return the settings of the engine the authoring responses of the providers depend on */
	static String environmentStamp() {
		String url;
		try {
			url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		} catch (Exception notInitialized) {
			url = "";
		}
		String build;
		try {
			build = String.valueOf(Engine.serverBuild());
		} catch (Exception | LinkageError notInitialized) {
			build = "";
		}
		return "studio=" + Engine.isStudioMode() + ";build=" + build + ";url=" + url + ";layout=" + FlowSourceLayout.current().key()
				+ ";frontbuilder=" + System.getenv("FLOW_FRONTBUILDER_RESOURCE_ROOT");
	}

	static String methodResponseDependencyFingerprint(File engineFile, JSONObject request) {
		var source = new StringBuilder();
		var engineRoot = engineFile == null ? null : engineFile.getParentFile();
		if (engineRoot == null) {
			source.append("engine:null\n");
		} else {
			var root = engineRoot.getParentFile() == null ? engineRoot : engineRoot.getParentFile();
			source.append(cachedRootFingerprint("engine", root, engineRoot, engineRoot, walk -> {
				walk.header("engine");
				for (var entry : FLOW_ROOT_ENTRIES) {
					walk.entry(entry);
				}
			}));
		}
		var projectDir = request == null ? "" : request.optString("projectDir", "");
		if (projectDir != null && !projectDir.isBlank()) {
			var projectRoot = new File(projectDir);
			appendProjectFingerprint(source, "project", projectRoot);
			appendReferencedProjectFingerprints(source, projectRoot);
		}
		appendRequestFileFingerprint(source, "sourceFile", request == null ? "" : request.optString("sourceFile", ""));
		appendRequestFileFingerprint(source, "sourcePath", request == null ? "" : request.optString("sourcePath", ""));
		return sha256Hex(source.toString());
	}

	private static void appendProjectFingerprint(StringBuilder source, String label, File projectRoot) {
		if (projectRoot == null) {
			source.append(label).append(":null\n");
			return;
		}
		source.append(label).append(":").append(canonicalPath(projectRoot)).append("\n");
		source.append(cachedRootFingerprint("project", projectRoot, new File(projectRoot, ENGINE_BASE_PATH), projectRoot, walk -> {
			walk.header("project").entry("c8oProject.yaml");
			for (var entry : FLOW_ROOT_ENTRIES) {
				walk.entry(ENGINE_BASE_PATH + entry);
			}
			walk.entry(FlowSourceLayout.current().flows());
		}));
	}

	private static void appendReferencedProjectFingerprints(StringBuilder source, File projectRoot) {
		var descriptor = new File(projectRoot, "c8oProject.yaml");
		if (!descriptor.isFile()) {
			return;
		}
		try {
			var text = FileUtils.readFileToString(descriptor, "UTF-8");
			var matcher = Pattern.compile("projectName:\\s*([A-Za-z0-9_.-]+)").matcher(text);
			var parent = projectRoot.getParentFile();
			var seen = ConcurrentHashMap.<String>newKeySet();
			while (matcher.find()) {
				var name = matcher.group(1);
				if (name == null || name.isBlank() || !seen.add(name)) {
					continue;
				}
				var root = referencedProjectRoot(parent, name);
				if (root != null) {
					appendProjectFingerprint(source, "reference:" + name, root);
				}
			}
		} catch (Exception e) {
			source.append("references:error:").append(e.getClass().getName()).append("\n");
		}
	}

	private static File referencedProjectRoot(File parent, String name) {
		if (parent == null) {
			return null;
		}
		var slug = name.replace("_", "-");
		var candidates = new File[] {
				new File(parent, name),
				new File(parent, "c8oprj-" + name),
				new File(parent, slug),
				new File(parent, "c8oprj-" + slug)
		};
		for (var candidate : candidates) {
			if (new File(candidate, ENGINE_BASE_PATH).isDirectory()) {
				return candidate;
			}
		}
		return null;
	}

	private static void appendRequestFileFingerprint(StringBuilder source, String label, String path) {
		if (path == null || path.isBlank()) {
			return;
		}
		source.append(label).append(":");
		appendFileFingerprint(source, new File(path));
	}

	private static void appendFileFingerprint(StringBuilder source, File file) {
		if (file == null) {
			source.append("f:null\n");
			return;
		}
		source.append("f:").append(canonicalPath(file));
		if (file.isFile()) {
			source.append(":").append(file.lastModified()).append(":").append(file.length());
		} else {
			source.append(":missing");
		}
		source.append("\n");
	}

	static String canonicalPath(File file) {
		if (file == null) {
			return "";
		}
		try {
			return file.getCanonicalPath();
		} catch (Exception e) {
			return file.getAbsolutePath();
		}
	}

	private static String sha256Hex(String text) {
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
			var out = new StringBuilder(digest.length * 2);
			for (var b : digest) {
				out.append(String.format("%02x", b & 0xff));
			}
			return out.toString();
		} catch (Exception e) {
			return Integer.toHexString(String.valueOf(text).hashCode());
		}
	}

	/**
	 * @return whether a response is kept: not when the Flow engine marks it "cacheable": false, as a tree showing a
	 *         frontend model that could not be described (the toolchain may be installed since, which the dependency
	 *         fingerprint does not see)
	 */
	static boolean isCacheableResponse(String method, JSONObject request, JSONObject response) {
		return response != null && isCacheableMethod(method, request) && response.optBoolean("cacheable", true);
	}

	/**
	 * @return whether the responses of a method are also kept on disk ({@link FlowAuthoringDiskCache}): the cacheable
	 *         methods the Studio asks first whose computation takes seconds after a start, the trees, the palettes, the
	 *         catalogs and the property editors; not the small metadata a server prepares at its start (syncInputs)
	 */
	static boolean isPersistedMethod(String method, JSONObject request) {
		return switch (method) {
		case "describeTree", "authoringTree", "authoringPalette", "catalog", "propertyEditor" -> isCacheableMethod(method, request);
		default -> false;
		};
	}

	private static boolean isCacheableMethod(String method, JSONObject request) {
		if ("nodeOutputSchema".equals(method)) {
			return isReadOnlySchemaRequest(request);
		}
		if ("outputSchema".equals(method)) {
			return isReadOnlySchemaRequest(request);
		}
		if ("contextMenu".equals(method) && isFrontendContextRequest(request)) {
			return false;
		}
		return switch (method) {
		case "describeTree", "catalog", "context", "contextMenu", "propertyEditor", "authoringPalette", "authoringTree", "icons", "syncInputs", "blockGet", "typeGet" -> true;
		default -> false;
		};
	}

	private static boolean isFrontendContextRequest(JSONObject request) {
		if (request == null) {
			return false;
		}
		var target = request.optJSONObject("targetObject");
		if (target == null) {
			return false;
		}
		var kind = target.optString("kind", "");
		var path = target.optString("path", "");
		return kind.startsWith("frontend") || path.equals("frontends") || path.startsWith("frontends.");
	}

	private static boolean invalidatesMethodResponseCache(String method, JSONObject request) {
		if ("cacheInfo".equals(method)) {
			return false;
		}
		if (isCacheableMethod(method, request) || isReadOnlyMethod(method, request)) {
			return false;
		}
		return switch (method) {
		case "cacheClear", "applyMutation", "writeCodeMirror", "contextAction", "schemaReset", "resourcePatch", "flowSourcePatch",
				"flowCodeDiscard", "flowCodeSet", "flowCodePatch", "flowCodePromote", "blockCodeSet", "blockCodePatch",
				"blockCreate", "blockDuplicate", "blockEdit", "typeCreate" -> true;
		default -> true;
		};
	}

	private static boolean isReadOnlyMethod(String method, JSONObject request) {
		if (isReadOnlyContextAction(method, request)) {
			return true;
		}
		return switch (method) {
		case "preload", "run", "analyze", "search", "resourceSearch", "resourceList", "resourceGet", "flowSourceGet", "flowSourceValidate",
				"flowCodeGet", "flowCodeStatus", "flowCodeCheck", "flowCodeRg", "flowCodeRun", "flowCodeAnalyze", "blockCodeGet",
				"blockCodeRg", "requestableList", "requestableSchema", "types" -> true;
		default -> false;
		};
	}

	private static boolean isReadOnlyContextAction(String method, JSONObject request) {
		if (!"contextAction".equals(method)) {
			return false;
		}
		return switch (actionId(request)) {
		case "flow.outputSchema.inspect", "flow.nodeOutputSchema.inspect" -> true;
		default -> false;
		};
	}

	private static boolean isReadOnlySchemaRequest(JSONObject request) {
		if (request == null) {
			return true;
		}
		if (request.optBoolean("adopt", false) || request.optBoolean("remove", false) || request.optBoolean("reset", false)
				|| request.optBoolean("delete", false)) {
			return false;
		}
		var action = request.opt("action");
		var text = action == null || JSONObject.NULL.equals(action) ? "" : String.valueOf(action).trim().toLowerCase();
		return text.isEmpty() || "read".equals(text) || "get".equals(text) || "inspect".equals(text);
	}

	private static String actionId(JSONObject request) {
		if (request == null) {
			return "";
		}
		var action = request.optJSONObject("action");
		if (action != null) {
			return action.optString("id", "");
		}
		return request.optString("actionId", "");
	}

	private static void clearMethodResponseCache() {
		if (!methodResponseCache.isEmpty()) {
			methodResponseCache.clear();
			methodResponseAliases.clear();
			methodResponseCacheInvalidations.increment();
		}
	}

	/** Removes the responses of these projects (canonical directories) and those of no project, which may use any of them. */
	private static void clearMethodResponseCache(Set<String> roots) {
		if (methodResponseCache.values().removeIf(cached -> cached.root().isEmpty() || roots.contains(cached.root()))) {
			methodResponseAliases.values().removeIf(key -> !methodResponseCache.containsKey(key));
			methodResponseCacheInvalidations.increment();
		}
	}

	private static int runtimeOption(PropertyName name, int fallback, int maximum) {
		try { return Math.max(0, Math.min(maximum, Integer.parseInt(EnginePropertiesManager.getProperty(name).trim()))); }
		catch (RuntimeException e) { return fallback; }
	}

	private static int runtimePoolLimit() {
		var configured = runtimeOption(PropertyName.JAVASCRIPT_RUNTIME_POOL_SIZE, 0, 256);
		return configured == 0 ? Engine.isStudioMode() ? 8 : 128 : configured;
	}

	private static int runtimeIdleSeconds() {
		return runtimeOption(PropertyName.JAVASCRIPT_RUNTIME_IDLE_SECONDS, 300, 86400);
	}

	/** Caller owns the pool monitor. Never evict a leased scope or the authoring service's scope. */
	private static List<CachedEngineRuntime> expiredRuntimes(CachedEngineRuntimePool pool, long now) {
		var expired = new ArrayList<CachedEngineRuntime>();
		long ttl = java.util.concurrent.TimeUnit.SECONDS.toNanos(runtimeIdleSeconds());
		int limit = runtimePoolLimit();
		for (var iterator = pool.available.iterator(); iterator.hasNext();) {
			var idle = iterator.next();
			if (idle.runtime().scope() != pool.frontendAuthoringScope
					&& (now - idle.since() >= ttl || pool.cachedCount > limit)) {
				iterator.remove(); pool.cachedCount--; expired.add(idle.runtime());
			}
		}
		return expired;
	}

	private static CachedEngineRuntimeLookup cachedEngineRuntime(EngineRef engineRef, File engineFile, CachedEngineSource engineSource,
			org.mozilla.javascript.Context cx, boolean frontendAuthoring) throws EngineException {
		var baseKey = engineRef.qname + "|" + engineSource.sourceName();
		var key = baseKey + "|pool";
		var staleRuntimes = new ArrayList<CachedEngineRuntime>();
		long generation;
		CachedEngineRuntimePool pool;
		synchronized (engineRuntimeCacheLock) {
			generation = cacheGeneration.get();
			engineRuntimeCache.forEach((runtimeKey, runtimePool) -> {
				if (runtimeKey.startsWith(engineRef.qname + "|")
						&& (runtimePool.generation != generation || !runtimeKey.equals(key))
						&& engineRuntimeCache.remove(runtimeKey, runtimePool)) {
					staleRuntimes.addAll(drainAvailableRuntimes(runtimePool));
				}
			});
			pool = engineRuntimeCache.computeIfAbsent(key,
					ignored -> new CachedEngineRuntimePool(engineSource.sourceName(), generation));
		}
		disposeEngineRuntimes(staleRuntimes, cx);
		List<CachedEngineRuntime> idleRuntimes;
		synchronized (pool) { idleRuntimes = expiredRuntimes(pool, System.nanoTime()); }
		disposeEngineRuntimes(idleRuntimes, cx);
		boolean pooled = false;
		try {
			synchronized (pool) {
				var cached = takeAvailableRuntime(pool, frontendAuthoring);
				if (cached != null) {
					try {
						cached = refreshEngineRuntimeDataCaches(cached, cx);
						if (frontendAuthoring) {
							pool.frontendAuthoringScope = cached.scope();
						}
						return new CachedEngineRuntimeLookup(cached, pool, true, true, key, generation,
								engineRuntimeCache.size(), false);
					} catch (Exception e) {
						if (cached.scope() == pool.frontendAuthoringScope) {
							pool.frontendAuthoringScope = null;
						}
						if (pool.cachedCount > 0) {
							pool.cachedCount--;
						}
						Engine.logEngine.warn("(FlowEngineBridge) Unable to refresh a cached engine runtime; replacing it.", e);
					}
				}
				pooled = pool.cachedCount < runtimePoolLimit();
				if (pooled) pool.cachedCount++;
			}
			// Reserve under the monitor, initialize outside it: a cold execution must
			// not serialize every other caller behind JavaScript initialization.
			var fresh = createEngineRuntime(engineRef, engineFile, engineSource, cx, generation);
			if (pooled && frontendAuthoring) synchronized (pool) { pool.frontendAuthoringScope = fresh.scope(); }
			return new CachedEngineRuntimeLookup(fresh, pool, false, pooled, key, generation, engineRuntimeCache.size(), false);
		} catch (EngineException e) {
			if (pooled) synchronized (pool) { pool.cachedCount--; }
			throw e;
		} catch (Exception e) {
			if (pooled) synchronized (pool) { pool.cachedCount--; }
			throw new EngineException("Unable to initialize Flow engine runtime \"" + engineRef.qname + "\".", e);
		}
	}

	private static CachedEngineRuntime takeAvailableRuntime(CachedEngineRuntimePool pool, boolean frontendAuthoring) {
		if (frontendAuthoring) {
			if (pool.frontendAuthoringScope != null) {
				for (var iterator = pool.available.iterator(); iterator.hasNext();) {
					var runtime = iterator.next().runtime();
					if (runtime.scope() == pool.frontendAuthoringScope) {
						iterator.remove();
						return runtime;
					}
				}
			}
			var idle = pool.available.pollLast();
			return idle == null ? null : idle.runtime();
		}
		// Reuse the most recently released scope: FIFO cycles cold plans through
		// every retained scope even for a sequential caller.
		for (var iterator = pool.available.descendingIterator(); iterator.hasNext();) {
			var runtime = iterator.next().runtime();
			if (runtime.scope() != pool.frontendAuthoringScope) {
				iterator.remove();
				return runtime;
			}
		}
		return null;
	}

	static void initializeSourceScope(Scriptable scope, File engineFile) {
		scope.put("__flowEngineDir", scope, engineFile.getParentFile().getAbsolutePath());
		// Bootstrap-owned, never accepted from an authoring/runtime request.
		scope.put("__flowSourceLayout", scope, FlowSourceLayout.current().key());
	}

	/**
	 * @return whether a call runs in the shared runtime of its project on a stateless server: a run, the read of an
	 *         input contract, and a preload of run plans, which prepares them where the runs of the project find them
	 */
	static boolean sharesRuntime(String method, JSONObject request) {
		return switch (method) {
		case "run" -> true;
		case "syncInputs" -> request.optBoolean("metadataOnly");
		case "preload" -> request.has("flows");
		default -> false;
		};
	}

	private static CachedEngineRuntimeLookup sharedEngineRuntime(EngineRef ref, File file, CachedEngineSource source,
			org.mozilla.javascript.Context cx, JSONObject request, Context context) throws EngineException {
		// An older provider may have initialization side effects. Never evaluate it
		// speculatively in a sealed realm and then evaluate it a second time on fallback.
		if (!source.source().startsWith("// @convertigo-runtime: stateless-v1\n")) return null;
		// Working copies are request-owned and must never enter a revision registry.
		for (var name : List.of("sourceDrafts", "frontendSourceDrafts", "drafts", "sourceRemovals")) {
            var value = request.opt(name);
            if (value == null || value == JSONObject.NULL) continue;
            if (value instanceof JSONObject object && object.length() == 0) continue;
            if (value instanceof JSONArray array && array.length() == 0) continue;
            return null;
        }
		// the revision of the data of its project only: another project changing keeps this runtime
		var root = projectRoot(request.optString("projectDir", ""));
		var key = new SharedRuntimeKey(ref.qname, source.sourceName(), root, callGeneration(request, context),
				cacheGeneration.get(), dataRevision(root));
		var runtime = sharedRuntimes.get(key);
		if (runtime != null && !runtime.api().revisionMatches(() -> methodResponseDependencyFingerprint(file, request))) {
			sharedRuntimes.remove(key, runtime);
			serverSources.clear();
			runtime = null;
		}
		boolean hit = runtime != null;
		if (runtime == null) runtime = preparedSharedRuntime(key, file, source, cx, request);
		return new CachedEngineRuntimeLookup(runtime, null, hit, false, ref.qname + "|" + source.sourceName() + "|shared",
				key.codeRevision(), sharedRuntimes.size(), false);
	}

	/**
	 * Single flight for each key: its first caller prepares the runtime, on its thread, and publishes it; the other
	 * callers of that key wait for it (they see its failure, never kept, the next caller preparing it again), while the
	 * runtimes of the other keys are prepared at the same time. A preparation needing its own key again on its thread
	 * prepares one for itself without publishing it, as a nested call did under a single lock; one stopped by the
	 * interruption of its thread is prepared again by a caller waiting for it, and an interrupted caller stops waiting.
	 */
	private static CachedEngineRuntime preparedSharedRuntime(SharedRuntimeKey key, File file, CachedEngineSource source,
			org.mozilla.javascript.Context cx, JSONObject request) throws EngineException {
		for (;;) {
			var runtime = sharedRuntimes.get(key);
			if (runtime != null) return runtime;
			var flight = sharedRuntimeFlights.get(key);
			if (flight == null) {
				var created = new SharedRuntimeFlight();
				flight = sharedRuntimeFlights.putIfAbsent(key, created);
				if (flight == null) return leadSharedRuntime(key, created, file, source, cx, request);
			}
			if (flight.owner == Thread.currentThread()) return createSharedRuntime(key, file, source, cx, request);
			runtime = awaitSharedRuntime(flight);
			if (runtime != null) return runtime;
		}
	}

	private static CachedEngineRuntime leadSharedRuntime(SharedRuntimeKey key, SharedRuntimeFlight flight, File file,
			CachedEngineSource source, org.mozilla.javascript.Context cx, JSONObject request) throws EngineException {
		// published by the previous preparation of the key after this caller missed it
		var runtime = sharedRuntimes.get(key);
		if (runtime == null) {
			try {
				runtime = createSharedRuntime(key, file, source, cx, request);
			} catch (Throwable failure) {
				sharedRuntimeFlights.remove(key, flight);
				if (FlowRuntimeApi.interruption(failure)) flight.outcome.complete(null);
				else flight.outcome.completeExceptionally(failure);
				throw failure;
			}
			// published before the flight ends: a caller missing the flight finds it
			publishSharedRuntime(key, runtime);
		}
		sharedRuntimeFlights.remove(key, flight);
		flight.outcome.complete(runtime);
		return runtime;
	}

	private static CachedEngineRuntime createSharedRuntime(SharedRuntimeKey key, File file, CachedEngineSource source,
			org.mozilla.javascript.Context cx, JSONObject request) throws EngineException {
		var api = new FlowRuntimeApi(cx, key.project(), key.codeRevision(), key.dataRevision());
		api.revisionMatches(() -> methodResponseDependencyFingerprint(file, request));
		var scope = api.sharedScope();
		initializeSourceScope(scope, file);
		scope.defineProperty("__flowRuntimeApi", org.mozilla.javascript.Context.javaToJS(api, scope),
				ScriptableObject.READONLY | ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
		var value = RhinoUtils.evalCachedJavascript(cx, scope, source.source(), source.sourceName(), source.file());
		if (!(value instanceof Scriptable engine) || !Boolean.TRUE.equals(ScriptableObject.getProperty(engine, "supportsStatelessExecution")))
			throw new EngineException("The provider advertised a shared runtime without implementing its protocol.");
		scope.sealObject();
		return new CachedEngineRuntime(source.sourceName(), key.codeRevision(), key.dataRevision(), scope, engine, api);
	}

	/** Kept while the revisions of its key are still those of its project: else it only serves the callers of its preparation. */
	private static void publishSharedRuntime(SharedRuntimeKey key, CachedEngineRuntime runtime) {
		synchronized (sharedRuntimes) {
			if (key.codeRevision() != cacheGeneration.get() || key.dataRevision() != dataRevision(key.project())
					|| key.loader() instanceof ProjectClassLoader generation && generation.isRetired()) return;
			if (sharedRuntimes.size() >= 256) sharedRuntimes.clear();
			sharedRuntimes.put(key, runtime);
		}
	}

	/** @return the runtime of a preparation in progress, null when its thread was interrupted: it is prepared again */
	private static CachedEngineRuntime awaitSharedRuntime(SharedRuntimeFlight flight) throws EngineException {
		sharedRuntimeWaiters.incrementAndGet();
		try {
			return flight.outcome.get();
		} catch (java.util.concurrent.ExecutionException failure) {
			var cause = failure.getCause();
			if (cause instanceof EngineException engineException) throw engineException;
			if (cause instanceof RuntimeException runtimeException) throw runtimeException;
			if (cause instanceof Error error) throw error;
			throw new EngineException("Unable to prepare a shared runtime.", cause);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new EngineException("Interrupted while waiting for the preparation of a shared runtime.", interrupted);
		} finally {
			sharedRuntimeWaiters.decrementAndGet();
		}
	}

	private static CachedEngineRuntime createEngineRuntime(EngineRef engineRef, File engineFile, CachedEngineSource engineSource,
			org.mozilla.javascript.Context cx, long generation) throws EngineException {
		try {
			// shared by all the projects: the packages of the engine, a call binds those of its project
			var scope = FlowPackages.createScope(cx);
			initializeSourceScope(scope, engineFile);
			var engine = RhinoUtils.evalCachedJavascript(cx, scope, engineSource.source(), engineSource.sourceName(), engineSource.file());
			if (engine == null || Undefined.isUndefined(engine)) {
				engine = ScriptableObject.getProperty(scope, engineRef.objectName);
			}
			if (!(engine instanceof Scriptable engineObject)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" must evaluate to a JavaScript object.");
			}
			return new CachedEngineRuntime(engineSource.sourceName(), generation, dataGeneration.get(), scope, engineObject, null);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to initialize Flow engine runtime \"" + engineRef.qname + "\".", e);
		}
	}

	private static void releaseEngineRuntime(CachedEngineRuntimeLookup lookup) {
		if (lookup == null || lookup.nested()) {
			return;
		}
		if (lookup.runtime().api() != null) return;
		if (!lookup.pooled()) {
			disposeEngineRuntime(lookup.runtime(), org.mozilla.javascript.Context.getCurrentContext());
			return;
		}
		var pool = lookup.pool();
		var runtime = lookup.runtime();
		try {
			runtime = refreshEngineRuntimeDataCaches(runtime, org.mozilla.javascript.Context.getCurrentContext());
		} catch (Exception e) {
			synchronized (pool) {
				if (runtime.scope() == pool.frontendAuthoringScope) {
					pool.frontendAuthoringScope = null;
				}
				if (pool.cachedCount > 0) {
					pool.cachedCount--;
				}
			}
			Engine.logEngine.warn("(FlowEngineBridge) Unable to refresh a released engine runtime; discarding it.", e);
			return;
		}
		var released = false;
		List<CachedEngineRuntime> expired;
		synchronized (pool) {
			if (engineRuntimeCache.get(lookup.key()) == pool && pool.generation == lookup.generation()
					&& pool.sourceName.equals(runtime.sourceName()) && pool.cachedCount <= runtimePoolLimit()) {
				pool.available.addLast(new IdleEngineRuntime(runtime, System.nanoTime()));
				released = true;
			} else if (pool.cachedCount > 0) {
				pool.cachedCount--;
			}
			expired = expiredRuntimes(pool, System.nanoTime());
		}
		disposeEngineRuntimes(expired, org.mozilla.javascript.Context.getCurrentContext());
		if (!released) {
			disposeEngineRuntime(runtime, org.mozilla.javascript.Context.getCurrentContext());
		}
	}

	private static CachedEngineRuntime refreshEngineRuntimeDataCaches(CachedEngineRuntime runtime,
			org.mozilla.javascript.Context cx) {
		var generation = dataGeneration.get();
		if (runtime == null || runtime.dataGeneration() == generation || cx == null) {
			return runtime;
		}
		clearEngineRuntimeDataCaches(runtime, cx);
		return new CachedEngineRuntime(runtime.sourceName(), runtime.generation(), generation, runtime.scope(),
				runtime.engineObject(), runtime.api());
	}

	private static List<CachedEngineRuntime> drainAvailableRuntimes(CachedEngineRuntimePool pool) {
		var runtimes = new ArrayList<CachedEngineRuntime>();
		synchronized (pool) {
			while (!pool.available.isEmpty()) {
				runtimes.add(pool.available.removeFirst().runtime());
			}
			pool.frontendAuthoringScope = null;
			pool.cachedCount = Math.max(0, pool.cachedCount - runtimes.size());
		}
		return runtimes;
	}

	private static void disposeEngineRuntimes(List<CachedEngineRuntime> runtimes, org.mozilla.javascript.Context cx) {
		if (runtimes.isEmpty()) {
			return;
		}
		var entered = false;
		try {
			if (cx == null) {
				cx = org.mozilla.javascript.Context.enter();
				entered = true;
			}
			for (var runtime : runtimes) {
				disposeEngineRuntime(runtime, cx);
			}
		} finally {
			if (entered) {
				org.mozilla.javascript.Context.exit();
			}
		}
	}

	private static void disposeEngineRuntime(CachedEngineRuntime runtime, org.mozilla.javascript.Context cx) {
		if (runtime == null || cx == null) {
			return;
		}
		try {
			clearEngineRuntimeDataCaches(runtime, cx);
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to clear a discarded engine runtime.", e);
		}
	}

	private static void clearEngineRuntimeDataCaches(CachedEngineRuntime runtime, org.mozilla.javascript.Context cx) {
		var function = ScriptableObject.getProperty(runtime.engineObject(), "cacheClear");
		if (function instanceof Function cacheClear) {
			cacheClear.call(cx, runtime.scope(), runtime.engineObject(), new Object[] { "{}" });
		}
	}

	private static CachedEngineSource cachedEngineSource(File engineFile) throws EngineException {
		try {
			var key = engineFile.getCanonicalPath();
			var lastModified = engineFile.lastModified();
			var length = engineFile.length();
			var cached = engineSourceCache.get(key);
			if (cached != null && cached.lastModified() == lastModified && cached.length() == length) {
				return cached;
			}
			var fresh = new CachedEngineSource(engineFile, FileUtils.readFileToString(engineFile, "UTF-8"), lastModified, length);
			engineSourceCache.put(key, fresh);
			return fresh;
		} catch (Exception e) {
			throw new EngineException("Unable to read Flow engine file \"" + engineFile.getAbsolutePath() + "\".", e);
		}
	}

	private static String bridgeClassSource() {
		try {
			var codeSource = FlowEngineBridge.class.getProtectionDomain().getCodeSource();
			var location = codeSource == null ? null : codeSource.getLocation();
			return location == null ? "" : location.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String bridgeClassResource() {
		try {
			var resource = FlowEngineBridge.class.getResource("FlowEngineBridge.class");
			return resource == null ? "" : resource.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private File resolveEngineFile(EngineRef engineRef) throws EngineException {
		try {
			var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(engineRef.projectName, true);
			var engineFile = new File(project.getDirPath(), ENGINE_BASE_PATH + engineRef.scriptPath + ".js");
			if (!engineFile.isFile()) {
				throw new EngineException("Flow engine file not found: " + engineFile.getAbsolutePath());
			}
			return engineFile;
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to resolve Flow engine \"" + engineRef.qname + "\".", e);
		}
	}

	// The engine answers with JSON text. Keep that text with the parsed object so the
	// method cache and the shared authoring flight reuse it instead of serializing the
	// same (often multi-megabyte) response again.
	static final class EngineJSONObject extends JSONObject {
		private final String json;

		EngineJSONObject(String json) throws JSONException {
			super(json);
			this.json = json;
		}
	}

	static String serializedResponse(JSONObject response) {
		return response instanceof EngineJSONObject engineResponse ? engineResponse.json : response.toString();
	}

	private static JSONObject toJsonObject(Object result, String engineQName, String method) throws EngineException {
		if (result == null || Undefined.isUndefined(result)) {
			return new JSONObject();
		}
		try {
			var json = result instanceof CharSequence ? result.toString() : RhinoUtils.jsonStringify(result);
			return new EngineJSONObject(json);
		} catch (Exception e) {
			throw new EngineException("Flow engine \"" + engineQName + "\" method \"" + method
					+ "\" must return a JSON object or a JSON object string.", e);
		}
	}

	private static String normalizeEngineQName(String engineQName) {
		return engineQName == null || engineQName.isBlank() ? DEFAULT_ENGINE_QNAME : engineQName.trim();
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}

	private static String safeContextUrl(Context context, String kind) {
		try {
			return switch (kind) {
			case "convertigo" -> valueOrEmpty(context.getConvertigoUrl());
			case "project" -> valueOrEmpty(context.getProjectUrl());
			case "absolute" -> valueOrEmpty(context.getAbsoluteRequestedUrl());
			default -> "";
			};
		} catch (Exception e) {
			return "";
		}
	}

	private static class EngineRef {
		private final String qname;
		private final String projectName;
		private final String objectName;
		private final String scriptPath;

		private EngineRef(String qname, String projectName, String objectName, String scriptPath) {
			this.qname = qname;
			this.projectName = projectName;
			this.objectName = objectName;
			this.scriptPath = scriptPath;
		}

		private static EngineRef parse(String qname) throws EngineException {
			var dot = qname.indexOf('.');
			if (dot <= 0 || dot == qname.length() - 1) {
				throw new EngineException("Invalid Flow engine QName \"" + qname + "\". Expected \"project.Engine\".");
			}
			var projectName = qname.substring(0, dot);
			var objectName = qname.substring(dot + 1);
			var scriptPath = objectName.replace('.', File.separatorChar);
			return new EngineRef(qname, projectName, objectName, scriptPath);
		}
	}

	private static final class InvocationStats {
		private final String engineQName;
		private final String method;
		private final LongAdder count = new LongAdder();
		private final LongAdder errors = new LongAdder();
		private final LongAdder runtimeHits = new LongAdder();
		private final LongAdder runtimeNestedReuses = new LongAdder();
		private final LongAdder runtimeMisses = new LongAdder();
		private final LongAdder runtimeDisabled = new LongAdder();
		private final LongAdder methodCacheHits = new LongAdder();
		private final LongAdder authoringLockCalls = new LongAdder();
		private final LongAdder authoringLockContentions = new LongAdder();
		private final LongAdder authoringLockWaitNanos = new LongAdder();
		private final LongAdder authoringLockHeldNanos = new LongAdder();
		private final LongAdder methodFingerprintNanos = new LongAdder();
		private final LongAdder methodCacheLookupNanos = new LongAdder();
		private final LongAdder totalNanos = new LongAdder();
		private final AtomicLong maxNanos = new AtomicLong();
		private final AtomicLong maxAuthoringLockWaitNanos = new AtomicLong();
		private final AtomicLong maxAuthoringLockHeldNanos = new AtomicLong();
		private final AtomicLong maxMethodFingerprintNanos = new AtomicLong();

		private InvocationStats(String engineQName, String method) {
			this.engineQName = engineQName;
			this.method = method;
		}

		private void record(long durationNanos, CachedEngineRuntimeLookup runtimeLookup, boolean error, boolean methodCacheHit,
				boolean frontendAuthoring, boolean lockContended, long lockWaitNanos, long lockHeldNanos,
				long methodFingerprintNanos, long methodCacheLookupNanos) {
			count.increment();
			totalNanos.add(durationNanos);
			updateMax(maxNanos, durationNanos);
			if (error) {
				errors.increment();
			}
			if (methodCacheHit) {
				methodCacheHits.increment();
			}
			if (runtimeLookup == null) {
				runtimeDisabled.increment();
			} else if (runtimeLookup.hit()) {
				runtimeHits.increment();
				if (runtimeLookup.nested()) runtimeNestedReuses.increment();
			} else {
				runtimeMisses.increment();
			}
			if (frontendAuthoring) {
				authoringLockCalls.increment();
				if (lockContended) {
					authoringLockContentions.increment();
				}
				authoringLockWaitNanos.add(lockWaitNanos);
				authoringLockHeldNanos.add(lockHeldNanos);
				updateMax(maxAuthoringLockWaitNanos, lockWaitNanos);
				updateMax(maxAuthoringLockHeldNanos, lockHeldNanos);
			}
			this.methodFingerprintNanos.add(methodFingerprintNanos);
			this.methodCacheLookupNanos.add(methodCacheLookupNanos);
			updateMax(maxMethodFingerprintNanos, methodFingerprintNanos);
		}

		private void updateMax(AtomicLong maximum, long durationNanos) {
			var previous = maximum.get();
			while (durationNanos > previous && !maximum.compareAndSet(previous, durationNanos)) {
				previous = maximum.get();
			}
		}

		private JSONObject toJson() throws JSONException {
			var calls = count.sum();
			var total = totalNanos.sum();
			var lockCalls = authoringLockCalls.sum();
			var lockWait = authoringLockWaitNanos.sum();
			var lockHeld = authoringLockHeldNanos.sum();
			var fingerprint = methodFingerprintNanos.sum();
			var cacheLookup = methodCacheLookupNanos.sum();
			return new JSONObject()
					.put("calls", calls)
					.put("errors", errors.sum())
					.put("runtimeHits", runtimeHits.sum())
					.put("runtimeNestedReuses", runtimeNestedReuses.sum())
					.put("runtimeMisses", runtimeMisses.sum())
					.put("runtimeDisabled", runtimeDisabled.sum())
					.put("methodCacheHits", methodCacheHits.sum())
					.put("totalMs", nanosToMillis(total))
					.put("avgMs", calls == 0 ? 0 : nanosToMillis(total / calls))
					.put("maxMs", nanosToMillis(maxNanos.get()))
					.put("authoringLockCalls", lockCalls)
					.put("authoringLockContentions", authoringLockContentions.sum())
					.put("authoringLockWaitTotalMs", nanosToMillis(lockWait))
					.put("authoringLockWaitAvgMs", lockCalls == 0 ? 0 : nanosToMillis(lockWait / lockCalls))
					.put("authoringLockWaitMaxMs", nanosToMillis(maxAuthoringLockWaitNanos.get()))
					.put("authoringLockHeldTotalMs", nanosToMillis(lockHeld))
					.put("authoringLockHeldAvgMs", lockCalls == 0 ? 0 : nanosToMillis(lockHeld / lockCalls))
					.put("authoringLockHeldMaxMs", nanosToMillis(maxAuthoringLockHeldNanos.get()))
					.put("methodFingerprintTotalMs", nanosToMillis(fingerprint))
					.put("methodFingerprintAvgMs", calls == 0 ? 0 : nanosToMillis(fingerprint / calls))
					.put("methodFingerprintMaxMs", nanosToMillis(maxMethodFingerprintNanos.get()))
					.put("methodCacheLookupTotalMs", nanosToMillis(cacheLookup))
					.put("methodCacheLookupAvgMs", calls == 0 ? 0 : nanosToMillis(cacheLookup / calls));
		}
	}

	private static double nanosToMillis(long nanos) {
		return Math.round((nanos / 1_000_000.0) * 100.0) / 100.0;
	}
}
