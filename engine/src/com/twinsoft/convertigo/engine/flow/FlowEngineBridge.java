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
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.events.AdminEventBus;
import com.twinsoft.convertigo.engine.events.StudioEvent;
import com.twinsoft.convertigo.engine.events.StudioEventListener;
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
	private static final AtomicLong dataGeneration = new AtomicLong();
	private static final LongAdder methodResponseCacheHits = new LongAdder();
	private static final LongAdder methodResponseCacheMisses = new LongAdder();
	private static final LongAdder methodResponseCacheInvalidations = new LongAdder();
	private static final int METHOD_RESPONSE_CACHE_LIMIT = 256;
	private static final int ENGINE_RUNTIME_POOL_LIMIT = 2;
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

	private record CachedEngineRuntime(String sourceName, long generation, long dataGeneration, Scriptable scope,
			Scriptable engineObject) {
	}

	private static final class CachedEngineRuntimePool {
		private final String sourceName;
		private final long generation;
		private final ArrayDeque<CachedEngineRuntime> available = new ArrayDeque<>();
		private Scriptable frontendAuthoringScope;
		private int cachedCount;

		private CachedEngineRuntimePool(String sourceName, long generation) {
			this.sourceName = sourceName;
			this.generation = generation;
		}
	}

	private record CachedEngineRuntimeLookup(CachedEngineRuntime runtime, CachedEngineRuntimePool pool, boolean hit,
			boolean pooled, String key, long generation, int size) {
	}

	private record CachedMethodResponse(String response, long generation, long dataGeneration) {
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

	public static void clearCaches() {
		var runtimes = new ArrayList<CachedEngineRuntime>();
		synchronized (engineRuntimeCacheLock) {
			cacheGeneration.incrementAndGet();
			engineRuntimeCache.values().forEach(pool -> runtimes.addAll(drainAvailableRuntimes(pool)));
			engineRuntimeCache.clear();
		}
		engineSourceCache.clear();
		rootFingerprints.clear();
		disposeEngineRuntimes(runtimes, null);
		methodResponseCache.clear();
		methodResponseCacheInvalidations.increment();
		RhinoUtils.clearCachedJavascript();
	}

	public static void invalidateDataCaches() {
		dataGeneration.incrementAndGet();
		rootFingerprints.clear();
		clearMethodResponseCache();
	}

	public static long cacheGeneration() {
		return cacheGeneration.get();
	}

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
		invalidateDataCaches();
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

	public JSONObject run(Flow flow, Context convertigoContext, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		var started = System.nanoTime();
		try {
			var engineQName = effectiveEngineQName(flow);
			var flowSource = flow.getFlowSource();
			var sourceFinished = System.nanoTime();
			var request = baseRequest(engineQName, flowSource, flow.getQName(), convertigoContext)
					.put("flowName", flow.getName())
					.put("projectDir", flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("includeTrace", flow.isIncludeTrace());
			var requestFinished = System.nanoTime();
			request.put("input", flow.getFlowInput());
			var inputFinished = System.nanoTime();
			var profileEnabled = convertigoContext != null && convertigoContext.httpServletRequest != null
					&& "true".equals(convertigoContext.httpServletRequest.getParameter("__flowProfile"));
			if (profileEnabled) {
				request.put("profile", true);
			}
			var response = invoke(engineQName, "run", request, convertigoContext, javascriptContext, scope);
			if (profileEnabled) {
				var profile = response.optJSONObject("profile");
				if (profile == null) {
					profile = new JSONObject();
					response.put("profile", profile);
				}
				profile.put("javaBridge", new JSONObject()
						.put("sourceMs", nanosToMillis(sourceFinished - started))
						.put("requestMs", nanosToMillis(requestFinished - sourceFinished))
						.put("inputMs", nanosToMillis(inputFinished - requestFinished))
						.put("invokeMs", nanosToMillis(System.nanoTime() - inputFinished))
						.put("totalMs", nanosToMillis(System.nanoTime() - started)));
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
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			return invoke(engineQName, "syncInputs", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow input synchronization request.", e);
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
		if (Engine.isStudioMode()) {
			projectNames.addAll(Engine.theApp.databaseObjectsManager.getStudioProjects().getProjects(true).keySet());
			if (currentProjectName != null && !currentProjectName.isBlank()) {
				projectNames.add(currentProjectName);
			}
		} else {
			projectNames.addAll(Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false));
		}

		var orderedProjectNames = new java.util.ArrayList<String>(projectNames.size());
		if (currentProjectName != null && !currentProjectName.isBlank() && projectNames.remove(currentProjectName)) {
			orderedProjectNames.add(currentProjectName);
		}
		orderedProjectNames.addAll(projectNames);
		return orderedProjectNames;
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
			Project referencedProject;
			try {
				referencedProject = Engine.theApp.databaseObjectsManager
						.getOriginalProjectByName(referencedProjectName, true);
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

	private static JSONObject requestableProject(Project project, boolean current) throws JSONException {
		var children = new JSONArray();
		for (Sequence sequence : project.getSequencesList()) {
			children.put(requestableLeaf(project.getName(), "", sequence.getName(), sequence instanceof Flow ? "flow" : "sequence"));
		}
		for (Connector connector : project.getConnectorsList()) {
			var transactions = new JSONArray();
			for (Transaction transaction : connector.getTransactionsList()) {
				transactions.put(requestableLeaf(project.getName(), connector.getName(), transaction.getName(), "transaction"));
			}
			if (transactions.length() > 0) {
				children.put(new JSONObject()
						.put("kind", "connector")
						.put("name", connector.getName())
						.put("qname", project.getName() + "." + connector.getName())
						.put("children", transactions));
			}
		}
		return new JSONObject()
				.put("kind", "project")
				.put("name", project.getName())
				.put("qname", project.getName())
				.put("current", current)
				.put("children", children);
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
					methodCacheKey = methodResponseCacheKey(engineRef, engineFile, engineSource, method, request);
					methodFingerprintNanos = System.nanoTime() - fingerprintStarted;
					var cacheLookupStarted = System.nanoTime();
					var cachedResponse = cachedMethodResponse(methodCacheKey);
					methodCacheLookupNanos += System.nanoTime() - cacheLookupStarted;
					FlowStudioSupport.performanceProfileMark("bridge." + method + ".methodCacheBeforeLock");
					if (cachedResponse != null && frontendAuthoringMutationGate.canServe(optimisticReadStamp)) {
						methodCacheHit = true;
						return new JSONObject(cachedResponse.response());
					}
					frontendAuthoringFlight = acquireFrontendAuthoringFlight(methodCacheKey);
					if (frontendAuthoringFlight != null && !frontendAuthoringFlight.leader()) {
						FlowStudioSupport.performanceProfileMark("bridge." + method + ".singleFlightFollower");
						return frontendAuthoringFlight.await();
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
						methodCacheKey = methodResponseCacheKey(engineRef, engineFile, engineSource, method, request);
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
				} else if (invalidatesMethodResponseCache(method, request)) {
					clearMethodResponseCache();
				}
			}
			if (cx == null) {
				cx = org.mozilla.javascript.Context.enter();
				entered = true;
				if (useThreadRuntime) {
					runtimeLookup = cachedEngineRuntime(engineRef, engineFile, engineSource, cx, frontendAuthoring);
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
				methodCacheKey = methodResponseCacheKey(engineRef, engineFile, engineSource, method, request);
				methodFingerprintNanos += System.nanoTime() - fingerprintStarted;
			}
			var stableAuthoringRead = !frontendAuthoring || frontendAuthoringMutation
					|| frontendAuthoringMutationGate.canServe(optimisticReadStamp);
			if (stableAuthoringRead) {
				storeMethodResponse(methodCacheKey, engineRef, method, request, response);
			} else {
				FlowStudioSupport.performanceProfileMark("bridge." + method + ".staleSnapshotSkipped");
			}
			FlowStudioSupport.performanceProfileMark("bridge." + method + ".store");
			if (frontendAuthoringFlight != null && frontendAuthoringFlight.leader()) {
				frontendAuthoringFlight.complete(response);
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
				.put("sourceCacheSize", engineSourceCache.size())
				.put("runtimeCacheSize", engineRuntimeCache.size())
				.put("runtimePoolLimit", ENGINE_RUNTIME_POOL_LIMIT)
				.put("pooledRuntimeCount", pooledRuntimeCount)
				.put("availableRuntimeCount", availableRuntimeCount)
				.put("methodResponseCacheSize", methodResponseCache.size())
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

	private JSONObject invokePrepared(EngineRef engineRef, File engineFile, CachedEngineSource engineSource, String method,
			JSONObject request, Context convertigoContext, org.mozilla.javascript.Context cx, Scriptable engineScope,
			Scriptable engineObject, CachedEngineRuntimeLookup runtimeLookup) throws EngineException {
		if (convertigoContext != null) {
			var jsContext = org.mozilla.javascript.Context.toObject(convertigoContext, engineScope);
			engineScope.put("context", engineScope, jsContext);
		} else {
			engineScope.delete("context");
		}

		initializeSourceScope(engineScope, engineFile);
		// Packages, java... of the generation of the class path of the project of the call, until it returns
		var generation = callGeneration(request, convertigoContext);
		FlowPackages.bind(cx, engineScope, generation);
		try {
			return invokeBound(engineRef, engineSource, method, request, cx, engineScope, engineObject, runtimeLookup);
		} finally {
			FlowPackages.restore(engineScope, generation);
		}
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

	private JSONObject invokeBound(EngineRef engineRef, CachedEngineSource engineSource, String method, JSONObject request,
			org.mozilla.javascript.Context cx, Scriptable engineScope, Scriptable engineObject,
			CachedEngineRuntimeLookup runtimeLookup) throws EngineException {
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
			var engine = RhinoUtils.evalCachedJavascript(cx, engineScope, engineSource.source(), engineSource.sourceName(), 1, null);
			if (engine == null || Undefined.isUndefined(engine)) {
				engine = ScriptableObject.getProperty(engineScope, engineRef.objectName);
			}
			if (!(engine instanceof Scriptable)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" must evaluate to a JavaScript object.");
			}
			engineObject = (Scriptable) engine;
		}

		var function = ScriptableObject.getProperty(engineObject, method);
		if (!(function instanceof Function)) {
			throw new EngineException("Flow engine \"" + engineRef.qname + "\" does not expose method \"" + method + "\".");
		}

		var result = ((Function) function).call(cx, engineScope, engineObject, new Object[] { request.toString() });
		FlowStudioSupport.performanceProfileMark("bridge." + method + ".engineCall");
		var response = toJsonObject(result, engineRef.qname, method);
		FlowStudioSupport.performanceProfileMark("bridge." + method + ".serialization");
		return response;
	}

	private static CachedMethodResponse cachedMethodResponse(String key) {
		var cached = methodResponseCache.get(key);
		if (cached != null && cached.generation() == cacheGeneration.get()
				&& cached.dataGeneration() == dataGeneration.get()) {
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
		methodResponseCache.put(key,
				new CachedMethodResponse(serializedResponse(response), cacheGeneration.get(), dataGeneration.get()));
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
		return cached != null && cached.generation() == cacheGeneration.get()
				&& cached.dataGeneration() == dataGeneration.get() ? cached : null;
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

	private static String methodResponseCacheKey(EngineRef engineRef, File engineFile, CachedEngineSource engineSource, String method,
			JSONObject request) {
		return engineRef.qname + "|" + engineSource.sourceName() + "|"
				+ methodResponseDependencyFingerprint(engineFile, request) + "|" + cacheGeneration.get() + "|"
				+ dataGeneration.get() + "|" + method + "|"
				+ request.toString();
	}

	// The dependency fingerprint walks every source file of the engine, the project and its
	// references: about a thousand metadata calls, seconds on a network file system, on each
	// cacheable call. Writes made through Convertigo already bump the cache generations
	// (part of the key) and clear these entries; files changed outside Convertigo (git,
	// shell, deployment) are seen through a cheap per-root stamp, or after a few seconds.
	private static final long ROOT_FINGERPRINT_TTL_MS = 5000;
	private static final Map<String, RootFingerprint> rootFingerprints = new ConcurrentHashMap<>();

	private record RootFingerprint(String stamp, String value, long computedAt) {
	}

	private static String cachedRootFingerprint(String kind, File root, File flowRoot, java.util.function.Consumer<StringBuilder> walk) {
		var key = kind + ":" + canonicalPath(root);
		var stamp = new StringBuilder();
		for (var file : new File[] { root, flowRoot, new File(root, "c8oProject.yaml"), new File(root, "flow-deploy.json"),
				new File(flowRoot, "Engine.js"), new File(flowRoot, "engine.yaml") }) {
			stamp.append(file.lastModified()).append(':').append(file.length()).append(';');
		}
		var now = System.currentTimeMillis();
		var cached = rootFingerprints.get(key);
		if (cached != null && cached.stamp().equals(stamp.toString()) && now - cached.computedAt() < ROOT_FINGERPRINT_TTL_MS) {
			return cached.value();
		}
		var value = new StringBuilder();
		walk.accept(value);
		rootFingerprints.put(key, new RootFingerprint(stamp.toString(), value.toString(), now));
		return value.toString();
	}

	private static String methodResponseDependencyFingerprint(File engineFile, JSONObject request) {
		var source = new StringBuilder();
		var engineRoot = engineFile == null ? null : engineFile.getParentFile();
		if (engineRoot == null) {
			appendFlowRootFingerprint(source, "engine", null);
		} else {
			source.append(cachedRootFingerprint("engine", engineRoot.getParentFile() == null ? engineRoot : engineRoot.getParentFile(),
					engineRoot, walk -> appendFlowRootFingerprint(walk, "engine", engineRoot)));
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
		source.append(cachedRootFingerprint("project", projectRoot, new File(projectRoot, ENGINE_BASE_PATH),
				walk -> appendProjectTreeFingerprint(walk, projectRoot)));
	}

	private static void appendProjectTreeFingerprint(StringBuilder source, File projectRoot) {
		var label = "project";
		appendFileFingerprint(source, new File(projectRoot, "c8oProject.yaml"));
		var flowRoot = new File(projectRoot, ENGINE_BASE_PATH);
		appendFlowRootFingerprint(source, label + ".flow", flowRoot);
		appendDirectoryFingerprint(source, new File(projectRoot, FlowSourceLayout.current().flows()));
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

	private static void appendFlowRootFingerprint(StringBuilder source, String label, File flowRoot) {
		source.append(label).append(":").append(canonicalPath(flowRoot)).append("\n");
		appendFileFingerprint(source, new File(flowRoot, "Engine.js"));
		appendFileFingerprint(source, new File(flowRoot, "engine.yaml"));
		appendDirectoryFingerprint(source, new File(flowRoot, "modules"));
		appendDirectoryFingerprint(source, new File(flowRoot, "blocks"));
		appendDirectoryFingerprint(source, new File(flowRoot, "types"));
		appendDirectoryFingerprint(source, new File(flowRoot, "lib"));
		appendDirectoryFingerprint(source, new File(flowRoot, "resources"));
		appendDirectoryFingerprint(source, new File(flowRoot, "schemas"));
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

	private static void appendDirectoryFingerprint(StringBuilder source, File dir) {
		if (dir == null) {
			source.append("d:null\n");
			return;
		}
		source.append("d:").append(canonicalPath(dir));
		if (!dir.isDirectory()) {
			source.append(":missing\n");
			return;
		}
		source.append(":").append(dir.lastModified()).append("\n");
		var files = dir.listFiles();
		if (files == null) {
			return;
		}
		Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
		for (var file : files) {
			if (file.isDirectory()) {
				appendDirectoryFingerprint(source, file);
			} else {
				appendFileFingerprint(source, file);
			}
		}
	}

	private static String canonicalPath(File file) {
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
								engineRuntimeCache.size());
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
				var fresh = createEngineRuntime(engineRef, engineFile, engineSource, cx, generation);
				var pooled = pool.cachedCount < ENGINE_RUNTIME_POOL_LIMIT;
				if (pooled) {
					pool.cachedCount++;
					if (frontendAuthoring) {
						pool.frontendAuthoringScope = fresh.scope();
					}
				}
				return new CachedEngineRuntimeLookup(fresh, pool, false, pooled, key, generation, engineRuntimeCache.size());
			}
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to initialize Flow engine runtime \"" + engineRef.qname + "\".", e);
		}
	}

	private static CachedEngineRuntime takeAvailableRuntime(CachedEngineRuntimePool pool, boolean frontendAuthoring) {
		if (frontendAuthoring) {
			if (pool.frontendAuthoringScope != null) {
				for (var iterator = pool.available.iterator(); iterator.hasNext();) {
					var runtime = iterator.next();
					if (runtime.scope() == pool.frontendAuthoringScope) {
						iterator.remove();
						return runtime;
					}
				}
			}
			return pool.available.pollFirst();
		}
		for (var iterator = pool.available.iterator(); iterator.hasNext();) {
			var runtime = iterator.next();
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

	private static CachedEngineRuntime createEngineRuntime(EngineRef engineRef, File engineFile, CachedEngineSource engineSource,
			org.mozilla.javascript.Context cx, long generation) throws EngineException {
		try {
			// shared by all the projects: the packages of the engine, a call binds those of its project
			var scope = FlowPackages.createScope(cx);
			initializeSourceScope(scope, engineFile);
			var engine = RhinoUtils.evalCachedJavascript(cx, scope, engineSource.source(), engineSource.sourceName(), 1, null);
			if (engine == null || Undefined.isUndefined(engine)) {
				engine = ScriptableObject.getProperty(scope, engineRef.objectName);
			}
			if (!(engine instanceof Scriptable engineObject)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" must evaluate to a JavaScript object.");
			}
			return new CachedEngineRuntime(engineSource.sourceName(), generation, dataGeneration.get(), scope, engineObject);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to initialize Flow engine runtime \"" + engineRef.qname + "\".", e);
		}
	}

	private static void releaseEngineRuntime(CachedEngineRuntimeLookup lookup) {
		if (lookup == null) {
			return;
		}
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
		synchronized (pool) {
			if (engineRuntimeCache.get(lookup.key()) == pool && pool.generation == lookup.generation()
					&& pool.sourceName.equals(runtime.sourceName())) {
				pool.available.addLast(runtime);
				released = true;
			} else if (pool.cachedCount > 0) {
				pool.cachedCount--;
			}
		}
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
				runtime.engineObject());
	}

	private static List<CachedEngineRuntime> drainAvailableRuntimes(CachedEngineRuntimePool pool) {
		var runtimes = new ArrayList<CachedEngineRuntime>();
		synchronized (pool) {
			while (!pool.available.isEmpty()) {
				runtimes.add(pool.available.removeFirst());
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
