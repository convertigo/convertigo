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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
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
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.events.StudioEvent;
import com.twinsoft.convertigo.engine.events.StudioEventListener;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class FlowEngineBridge {

	public static final String DEFAULT_ENGINE_QNAME = "lib_flow_engine.Engine";

	private static final String ENGINE_BASE_PATH = "libs/flow/";
	private static final Map<String, CachedEngineSource> engineSourceCache = new ConcurrentHashMap<>();
	private static final Map<String, CachedEngineRuntimePool> engineRuntimeCache = new ConcurrentHashMap<>();
	private static final Map<String, CachedMethodResponse> methodResponseCache = new ConcurrentHashMap<>();
	private static final Map<String, InvocationStats> invocationStats = new ConcurrentHashMap<>();
	private static final AtomicLong cacheGeneration = new AtomicLong();
	private static final LongAdder methodResponseCacheHits = new LongAdder();
	private static final LongAdder methodResponseCacheMisses = new LongAdder();
	private static final LongAdder methodResponseCacheInvalidations = new LongAdder();
	private static final int METHOD_RESPONSE_CACHE_LIMIT = 256;
	private static final int ENGINE_RUNTIME_POOL_LIMIT = 2;

	private record CachedEngineSource(File file, String source, long lastModified, long length) {
		String sourceName() {
			return file.getAbsolutePath() + "#" + lastModified + ":" + length;
		}
	}

	private record CachedEngineRuntime(String sourceName, long generation, Scriptable scope, Scriptable engineObject) {
	}

	private static final class CachedEngineRuntimePool {
		private final String sourceName;
		private final long generation;
		private final ArrayDeque<CachedEngineRuntime> available = new ArrayDeque<>();
		private int cachedCount;

		private CachedEngineRuntimePool(String sourceName, long generation) {
			this.sourceName = sourceName;
			this.generation = generation;
		}
	}

	private record CachedEngineRuntimeLookup(CachedEngineRuntime runtime, CachedEngineRuntimePool pool, boolean hit,
			boolean pooled, String key, long generation, int size) {
	}

	private record CachedMethodResponse(String response, long generation) {
	}

	public static void clearCaches() {
		cacheGeneration.incrementAndGet();
		engineSourceCache.clear();
		engineRuntimeCache.clear();
		methodResponseCache.clear();
		methodResponseCacheInvalidations.increment();
		RhinoUtils.clearCachedJavascript();
	}

	public static long cacheGeneration() {
		return cacheGeneration.get();
	}

	public static void notifySourceMutation(String projectDir, String sourcePath) {
		clearCaches();
		if (!Engine.isStudioMode() || Engine.theApp == null || Engine.theApp.eventManager == null) {
			return;
		}
		var projectName = projectNameForDir(projectDir);
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
					.put("sourcePath", sourcePath == null ? "" : sourcePath);
			Engine.theApp.eventManager.dispatchEvent(
					new StudioEvent(StudioEvent.FLOW_SOURCE_CHANGED, payload),
					StudioEventListener.class);
		} catch (Exception e) {
			Engine.logEngine.warn("(FlowEngineBridge) Unable to notify a Flow source mutation.", e);
		}
	}

	public static void notifyStudioBrowser(String browserJson) {
		if (!Engine.isStudioMode() || Engine.theApp == null || Engine.theApp.eventManager == null
				|| browserJson == null || browserJson.isBlank()) {
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
			var request = baseRequest(engineQName, flow.getFlowSource(), flow.getQName(), null)
					.put("target", "flow")
					.put("flowName", flow.getName())
					.put("projectDir", flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("allowRequestableSchema", false);
			var sourceFile = flow.getFlowSourceFile();
			if (sourceFile != null) {
				request.put("sourceFile", sourceFile.getAbsolutePath());
			}
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

	public JSONObject propertyEditor(FlowEngine flowEngine) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine));
			return invoke(engineQName, "propertyEditor", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine property editor request.", e);
		}
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
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine));
			merge(request, options);
			return invoke(engineQName, "contextMenu", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine context menu request.", e);
		}
	}

	public JSONObject contextAction(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine));
			merge(request, options);
			return invoke(engineQName, "contextAction", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine context action request.", e);
		}
	}

	public JSONObject authoringPalette(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine));
			merge(request, options);
			return invoke(engineQName, "authoringPalette", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine authoring palette request.", e);
		}
	}

	public JSONObject authoringTree(FlowEngine flowEngine, JSONObject options) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine));
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
			var request = baseRequest(engineQName, "", flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine.getEngineSource())
					.put("projectDir", flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("includeFlowCatalog", true)
					.put("flowCatalogOrigin", "project")
					.put("includeCatalogLibraries", false)
					.put("includeBindings", false)
					.put("prewarmFrontendDocumentServer", Engine.isStudioMode())
					.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine));
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
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow.getFlowSource(), flow.getQName(), null)
					.put("target", "flow")
					.put("flowName", flow.getName())
					.put("projectDir", flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			var sourceFile = flow.getFlowSourceFile();
			if (sourceFile != null) {
				request.put("sourceFile", sourceFile.getAbsolutePath());
			}
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
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine.getEngineSource())
					.put("projectDir", flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("mutation", mutation == null ? new JSONObject() : mutation);
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
			if (!sourceFile.isFile()) {
				throw new EngineException("Flow source file not found: " + sourcePath);
			}
			if (sourceFile.getName().endsWith(".front.json")) {
				return applyJsonSourceMutation(flowEngine, sourceFile, mutation);
			}
			if (sourceFile.getName().endsWith(".flow.svelte")) {
				return applyFlowSvelteSourceMutation(flowEngine, sourceFile, mutation, authoringRootPath);
			}
			var source = flowEngine == null
					? FileUtils.readFileToString(sourceFile, "UTF-8")
					: flowEngine.getSource(sourceFile.getAbsolutePath());
			var request = baseRequest(engineQName, source, flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "flow")
					.put("flowSource", source)
					.put("sourceFile", sourceFile.getAbsolutePath())
					.put("sourcePath", sourceFile.getAbsolutePath())
					.put("projectDir", projectDir)
					.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine))
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
		var request = baseRequest(engineQName, source, flowEngine == null ? "" : flowEngine.getQName(), null)
				.put("target", "frontendSource")
				.put("source", source)
				.put("sourceFile", sourcePath)
				.put("sourcePath", sourcePath)
				.put("projectDir", projectDir)
				.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
				.put("frontendSourceDrafts", frontendSourceDrafts(flowEngine))
				.put("mutation", mutation == null ? new JSONObject() : mutation);
		if (authoringRootPath != null && !authoringRootPath.isBlank()) {
			request.put("authoringRootPath", authoringRootPath);
		}
		var response = invoke(engineQName, "applySourceMutation", request, null, null, null);
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
		return response;
	}

	private static JSONObject applyJsonSourceMutation(FlowEngine flowEngine, File sourceFile, JSONObject mutation) throws Exception {
		var source = flowEngine == null
				? FileUtils.readFileToString(sourceFile, "UTF-8")
				: flowEngine.getFrontendSource(sourceFile.getAbsolutePath());
		var root = new JSONObject(source);
		var op = mutation == null ? "" : mutation.optString("op", "");
		var path = mutation == null ? "" : mutation.optString("path", "");
		var value = mutation == null ? JSONObject.NULL : mutation.opt("value");
		if ("replace".equals(op) || "set".equals(op)) {
			jsonSet(root, jsonPathTokens(path), value);
		} else if ("append".equals(op)) {
			var target = jsonGet(root, jsonPathTokens(path));
			if (!(target instanceof JSONArray array)) {
				throw new EngineException("Flow JSON mutation target is not an array: " + path);
			}
			array.put(value == null ? JSONObject.NULL : value);
		} else if ("insert".equals(op)) {
			var tokens = jsonPathTokens(path);
			var target = jsonGet(root, tokens);
			if (!(target instanceof JSONArray array)) {
				throw new EngineException("Flow JSON mutation target is not an array: " + path);
			}
			var index = Math.max(0, Math.min(mutation.optInt("index", array.length()), array.length()));
			var copy = new JSONArray();
			for (int i = 0; i < index; i++) {
				copy.put(array.get(i));
			}
			copy.put(value == null ? JSONObject.NULL : value);
			for (int i = index; i < array.length(); i++) {
				copy.put(array.get(i));
			}
			jsonSet(root, tokens, copy);
		} else if ("delete".equals(op) || "remove".equals(op)) {
			jsonRemove(root, jsonPathTokens(path));
		} else {
			throw new EngineException("Unsupported Flow JSON mutation operation: " + op);
		}
		var newSource = root.toString(2) + "\n";
		var changed = !newSource.equals(source);
		if (changed) {
			if (flowEngine == null) {
				FileUtils.writeStringToFile(sourceFile, newSource, "UTF-8");
			} else {
				flowEngine.setFrontendSource(sourceFile.getAbsolutePath(), newSource);
			}
		}
		return new JSONObject()
				.put("ok", true)
				.put("target", "json")
				.put("source", newSource)
				.put("sourceFile", sourceFile.getAbsolutePath())
				.put("changed", changed);
	}

	static JSONObject frontendSourceDrafts(FlowEngine... flowEngines) throws JSONException {
		var drafts = new JSONObject();
		var visited = new HashSet<String>();
		for (var flowEngine : flowEngines) {
			if (flowEngine == null) {
				continue;
			}
			appendFrontendSourceDrafts(drafts, flowEngine);
			appendFrontendSourceDrafts(drafts, flowEngine.getProject(), visited);
		}
		return drafts;
	}

	private static void appendFrontendSourceDrafts(JSONObject drafts, FlowEngine flowEngine) throws JSONException {
		if (flowEngine == null) {
			return;
		}
		for (var entry : flowEngine.getSourceDrafts().entrySet()) {
			drafts.put(entry.getKey(), entry.getValue());
		}
	}

	private static void appendFrontendSourceDrafts(JSONObject drafts, Project project, Set<String> visited)
			throws JSONException {
		if (project == null || !visited.add(project.getName())) {
			return;
		}
		appendFrontendSourceDrafts(drafts, project.getFlowEngine());
		for (var reference : project.getReferenceList()) {
			if (!(reference instanceof ProjectSchemaReference projectReference)) {
				continue;
			}
			var referencedProjectName = projectReference.getParser().getProjectName();
			if (referencedProjectName == null || referencedProjectName.isBlank()) {
				continue;
			}
			try {
				var referencedProject = Engine.theApp.databaseObjectsManager
						.getOriginalProjectByName(referencedProjectName, true);
				appendFrontendSourceDrafts(drafts, referencedProject, visited);
			} catch (Exception e) {
				Engine.logEngine.debug("(FlowEngineBridge) Unable to collect Flow drafts from referenced project \""
						+ referencedProjectName + "\".", e);
			}
		}
	}

	private static List<Object> jsonPathTokens(String path) throws EngineException {
		var tokens = new ArrayList<Object>();
		var key = new StringBuilder();
		for (int i = 0; i < path.length(); i++) {
			var ch = path.charAt(i);
			if (ch == '.') {
				if (key.length() > 0) {
					tokens.add(key.toString());
					key.setLength(0);
				}
			} else if (ch == '[') {
				if (key.length() > 0) {
					tokens.add(key.toString());
					key.setLength(0);
				}
				var end = path.indexOf(']', i);
				if (end == -1) {
					throw new EngineException("Invalid Flow JSON mutation path: " + path);
				}
				tokens.add(Integer.valueOf(path.substring(i + 1, end)));
				i = end;
			} else {
				key.append(ch);
			}
		}
		if (key.length() > 0) {
			tokens.add(key.toString());
		}
		if (tokens.isEmpty()) {
			throw new EngineException("Empty Flow JSON mutation path.");
		}
		return tokens;
	}

	private static Object jsonGet(Object root, List<Object> tokens) throws Exception {
		var current = root;
		for (var token : tokens) {
			current = jsonChild(current, token);
		}
		return current;
	}

	private static Object jsonChild(Object current, Object token) throws Exception {
		if (token instanceof Integer index && current instanceof JSONArray array) {
			return array.get(index);
		}
		if (token instanceof String key && current instanceof JSONObject object) {
			return object.get(key);
		}
		throw new EngineException("Invalid Flow JSON mutation path segment: " + token);
	}

	private static void jsonSet(Object root, List<Object> tokens, Object value) throws Exception {
		var parent = root;
		for (int i = 0; i < tokens.size() - 1; i++) {
			parent = jsonChild(parent, tokens.get(i));
		}
		var last = tokens.get(tokens.size() - 1);
		if (last instanceof Integer index && parent instanceof JSONArray array) {
			array.put(index, value == null ? JSONObject.NULL : value);
			return;
		}
		if (last instanceof String key && parent instanceof JSONObject object) {
			object.put(key, value == null ? JSONObject.NULL : value);
			return;
		}
		throw new EngineException("Invalid Flow JSON mutation target: " + last);
	}

	private static void jsonRemove(Object root, List<Object> tokens) throws Exception {
		var last = tokens.get(tokens.size() - 1);
		if (tokens.size() == 1) {
			if (last instanceof String key && root instanceof JSONObject object) {
				object.remove(key);
				return;
			}
			throw new EngineException("Invalid Flow JSON mutation delete target: " + last);
		}
		var parentTokens = tokens.subList(0, tokens.size() - 1);
		var parent = jsonGet(root, parentTokens);
		if (last instanceof String key && parent instanceof JSONObject object) {
			object.remove(key);
			return;
		}
		if (last instanceof Integer index && parent instanceof JSONArray array) {
			if (index < 0 || index >= array.length()) {
				throw new EngineException("Flow JSON mutation delete index out of range: " + index);
			}
			var copy = new JSONArray();
			for (int i = 0; i < array.length(); i++) {
				if (i != index) {
					copy.put(array.get(i));
				}
			}
			jsonSet(root, parentTokens, copy);
			return;
		}
		throw new EngineException("Invalid Flow JSON mutation delete target: " + last);
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
			var file = new File(projectDir, "libs/flow/types/editors/" + typeName + ".html");
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

	private JSONObject invoke(String engineQName, String method, JSONObject request, Context convertigoContext,
			org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		var engineRef = EngineRef.parse(normalizeEngineQName(engineQName));
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

		try {
			engineFile = resolveEngineFile(engineRef);
			engineSource = cachedEngineSource(engineFile);
			if (useThreadRuntime) {
				if (isCacheableMethod(method, request)) {
					var cachedResponse = cachedMethodResponse(engineRef, engineFile, engineSource, method, request);
					if (cachedResponse != null) {
						methodCacheHit = true;
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
					runtimeLookup = cachedEngineRuntime(engineRef, engineFile, engineSource, cx);
					engineScope = runtimeLookup.runtime().scope();
					engineObject = runtimeLookup.runtime().engineObject();
				} else {
					engineScope = cx.initStandardObjects();
				}
			} else if (engineScope == null) {
				engineScope = cx.initStandardObjects();
			}

			var response = invokePrepared(engineRef, engineFile, engineSource, method, request, convertigoContext, cx, engineScope,
					engineObject, runtimeLookup);
			storeMethodResponse(engineRef, engineFile, engineSource, method, request, response);
			return response;
		} catch (EngineException e) {
			failed = true;
			throw e;
		} catch (Exception e) {
			failed = true;
			throw new EngineException("Unable to invoke Flow engine \"" + engineRef.qname + "\" method \"" + method + "\".", e);
		} finally {
			recordInvocation(engineRef, method, System.nanoTime() - started, runtimeLookup, failed, methodCacheHit);
			releaseEngineRuntime(runtimeLookup);
			if (entered) {
				org.mozilla.javascript.Context.exit();
			}
		}
	}

	private static void recordInvocation(EngineRef engineRef, String method, long durationNanos, CachedEngineRuntimeLookup runtimeLookup,
			boolean error, boolean methodCacheHit) {
		var key = engineRef.qname + "|" + method;
		var stats = invocationStats.computeIfAbsent(key, k -> new InvocationStats(engineRef.qname, method));
		stats.record(durationNanos, runtimeLookup, error, methodCacheHit);
	}

	private static JSONObject bridgeCacheInfo(String engineQName) throws JSONException {
		var normalizedEngineQName = normalizeEngineQName(engineQName);
		var methods = new JSONObject();
		var pooledRuntimeCount = 0;
		var availableRuntimeCount = 0;
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
		return new JSONObject()
				.put("generation", cacheGeneration.get())
				.put("sourceCacheSize", engineSourceCache.size())
				.put("runtimeCacheSize", engineRuntimeCache.size())
				.put("runtimePoolLimit", ENGINE_RUNTIME_POOL_LIMIT)
				.put("pooledRuntimeCount", pooledRuntimeCount)
				.put("availableRuntimeCount", availableRuntimeCount)
				.put("methodResponseCacheSize", methodResponseCache.size())
				.put("methodResponseCacheHits", methodResponseCacheHits.sum())
				.put("methodResponseCacheMisses", methodResponseCacheMisses.sum())
				.put("methodResponseCacheInvalidations", methodResponseCacheInvalidations.sum())
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

		engineScope.put("__flowEngineDir", engineScope, engineFile.getParentFile().getAbsolutePath());
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
		return toJsonObject(result, engineRef.qname, method);
	}

	private static CachedMethodResponse cachedMethodResponse(EngineRef engineRef, File engineFile, CachedEngineSource engineSource,
			String method, JSONObject request) {
		var key = methodResponseCacheKey(engineRef, engineFile, engineSource, method, request);
		var cached = methodResponseCache.get(key);
		if (cached != null && cached.generation() == cacheGeneration.get()) {
			methodResponseCacheHits.increment();
			return cached;
		}
		methodResponseCacheMisses.increment();
		return null;
	}

	private static void storeMethodResponse(EngineRef engineRef, File engineFile, CachedEngineSource engineSource, String method,
			JSONObject request, JSONObject response) {
		if (!isCacheableMethod(method, request) || response == null) {
			return;
		}
		if (methodResponseCache.size() >= METHOD_RESPONSE_CACHE_LIMIT) {
			methodResponseCache.clear();
			methodResponseCacheInvalidations.increment();
		}
		methodResponseCache.put(methodResponseCacheKey(engineRef, engineFile, engineSource, method, request),
				new CachedMethodResponse(response.toString(), cacheGeneration.get()));
	}

	private static String methodResponseCacheKey(EngineRef engineRef, File engineFile, CachedEngineSource engineSource, String method,
			JSONObject request) {
		return engineRef.qname + "|" + engineSource.sourceName() + "|"
				+ methodResponseDependencyFingerprint(engineFile, request) + "|" + cacheGeneration.get() + "|" + method + "|"
				+ request.toString();
	}

	private static String methodResponseDependencyFingerprint(File engineFile, JSONObject request) {
		var source = new StringBuilder();
		appendFlowRootFingerprint(source, "engine", engineFile == null ? null : engineFile.getParentFile());
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
		appendFileFingerprint(source, new File(projectRoot, "c8oProject.yaml"));
		var flowRoot = new File(projectRoot, ENGINE_BASE_PATH);
		appendFlowRootFingerprint(source, label + ".flow", flowRoot);
		appendDirectoryFingerprint(source, new File(projectRoot, "libs/flows"));
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
		case "describeTree", "catalog", "context", "contextMenu", "propertyEditor", "authoringPalette", "icons", "syncInputs", "blockGet", "typeGet" -> true;
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
			methodResponseCacheInvalidations.increment();
		}
	}

	private static CachedEngineRuntimeLookup cachedEngineRuntime(EngineRef engineRef, File engineFile, CachedEngineSource engineSource,
			org.mozilla.javascript.Context cx) throws EngineException {
		var baseKey = engineRef.qname + "|" + engineSource.sourceName();
		var key = baseKey + "|pool";
		var generation = cacheGeneration.get();
		engineRuntimeCache.entrySet().removeIf(entry -> entry.getKey().startsWith(engineRef.qname + "|")
				&& (entry.getValue().generation != generation || !entry.getKey().equals(key)));
		var pool = engineRuntimeCache.computeIfAbsent(key,
				ignored -> new CachedEngineRuntimePool(engineSource.sourceName(), generation));
		try {
			synchronized (pool) {
				var cached = pool.available.pollFirst();
				if (cached != null) {
					return new CachedEngineRuntimeLookup(cached, pool, true, true, key, generation, engineRuntimeCache.size());
				}
				var fresh = createEngineRuntime(engineRef, engineFile, engineSource, cx, generation);
				var pooled = pool.cachedCount < ENGINE_RUNTIME_POOL_LIMIT;
				if (pooled) {
					pool.cachedCount++;
				}
				return new CachedEngineRuntimeLookup(fresh, pool, false, pooled, key, generation, engineRuntimeCache.size());
			}
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to initialize Flow engine runtime \"" + engineRef.qname + "\".", e);
		}
	}

	private static CachedEngineRuntime createEngineRuntime(EngineRef engineRef, File engineFile, CachedEngineSource engineSource,
			org.mozilla.javascript.Context cx, long generation) throws EngineException {
		try {
			var scope = cx.initStandardObjects();
			scope.put("__flowEngineDir", scope, engineFile.getParentFile().getAbsolutePath());
			var engine = RhinoUtils.evalCachedJavascript(cx, scope, engineSource.source(), engineSource.sourceName(), 1, null);
			if (engine == null || Undefined.isUndefined(engine)) {
				engine = ScriptableObject.getProperty(scope, engineRef.objectName);
			}
			if (!(engine instanceof Scriptable engineObject)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" must evaluate to a JavaScript object.");
			}
			return new CachedEngineRuntime(engineSource.sourceName(), generation, scope, engineObject);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to initialize Flow engine runtime \"" + engineRef.qname + "\".", e);
		}
	}

	private static void releaseEngineRuntime(CachedEngineRuntimeLookup lookup) {
		if (lookup == null || !lookup.pooled()) {
			return;
		}
		var pool = lookup.pool();
		synchronized (pool) {
			if (pool.generation == lookup.generation() && pool.sourceName.equals(lookup.runtime().sourceName())) {
				pool.available.addLast(lookup.runtime());
			}
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

	private static JSONObject toJsonObject(Object result, String engineQName, String method) throws EngineException {
		if (result == null || Undefined.isUndefined(result)) {
			return new JSONObject();
		}
		try {
			var json = result instanceof CharSequence ? result.toString() : RhinoUtils.jsonStringify(result);
			return new JSONObject(json);
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
		private final LongAdder totalNanos = new LongAdder();
		private final AtomicLong maxNanos = new AtomicLong();

		private InvocationStats(String engineQName, String method) {
			this.engineQName = engineQName;
			this.method = method;
		}

		private void record(long durationNanos, CachedEngineRuntimeLookup runtimeLookup, boolean error, boolean methodCacheHit) {
			count.increment();
			totalNanos.add(durationNanos);
			updateMax(durationNanos);
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
		}

		private void updateMax(long durationNanos) {
			var previous = maxNanos.get();
			while (durationNanos > previous && !maxNanos.compareAndSet(previous, durationNanos)) {
				previous = maxNanos.get();
			}
		}

		private JSONObject toJson() throws JSONException {
			var calls = count.sum();
			var total = totalNanos.sum();
			return new JSONObject()
					.put("calls", calls)
					.put("errors", errors.sum())
					.put("runtimeHits", runtimeHits.sum())
					.put("runtimeMisses", runtimeMisses.sum())
					.put("runtimeDisabled", runtimeDisabled.sum())
					.put("methodCacheHits", methodCacheHits.sum())
					.put("totalMs", nanosToMillis(total))
					.put("avgMs", calls == 0 ? 0 : nanosToMillis(total / calls))
					.put("maxMs", nanosToMillis(maxNanos.get()));
		}
	}

	private static double nanosToMillis(long nanos) {
		return Math.round((nanos / 1_000_000.0) * 100.0) / 100.0;
	}
}
