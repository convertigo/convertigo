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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

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
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class FlowEngineBridge {

	public static final String DEFAULT_ENGINE_QNAME = "lib_flow_engine.Engine";

	private static final String ENGINE_BASE_PATH = "libs/flow/";
	private static final Map<String, CachedEngineSource> engineSourceCache = new ConcurrentHashMap<>();
	private static final Map<String, CachedEngineRuntime> engineRuntimeCache = new ConcurrentHashMap<>();
	private static final Map<String, CachedMethodResponse> methodResponseCache = new ConcurrentHashMap<>();
	private static final Map<String, InvocationStats> invocationStats = new ConcurrentHashMap<>();
	private static final AtomicLong cacheGeneration = new AtomicLong();
	private static final LongAdder methodResponseCacheHits = new LongAdder();
	private static final LongAdder methodResponseCacheMisses = new LongAdder();
	private static final LongAdder methodResponseCacheInvalidations = new LongAdder();
	private static final int METHOD_RESPONSE_CACHE_LIMIT = 256;

	private record CachedEngineSource(File file, String source, long lastModified, long length) {
		String sourceName() {
			return file.getAbsolutePath() + "#" + lastModified + ":" + length;
		}
	}

	private record CachedEngineRuntime(String sourceName, long generation, Scriptable scope, Scriptable engineObject) {
	}

	private record CachedEngineRuntimeLookup(CachedEngineRuntime runtime, boolean hit, String key, long generation, int size) {
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

	public JSONObject run(Flow flow, Context convertigoContext, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow.getFlowSource(), flow.getQName(), convertigoContext)
					.put("flowName", flow.getName())
					.put("projectDir", flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("input", flow.getFlowInput())
					.put("includeTrace", flow.isIncludeTrace());
			return invoke(engineQName, "run", request, convertigoContext, javascriptContext, scope);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow engine request.", e);
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
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "context", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine context request.", e);
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
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
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
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine == null ? "" : flowEngine.getEngineSource())
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			merge(request, options);
			return invoke(engineQName, "contextAction", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine context action request.", e);
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
					.put("projectDir", flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
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
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var projectDir = flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath();
			var sourceFile = new File(sourcePath == null ? "" : sourcePath);
			if (!sourceFile.isFile()) {
				throw new EngineException("Flow source file not found: " + sourcePath);
			}
			var source = FileUtils.readFileToString(sourceFile, "UTF-8");
			var request = baseRequest(engineQName, source, flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "flow")
					.put("flowSource", source)
					.put("sourceFile", sourceFile.getAbsolutePath())
					.put("projectDir", projectDir)
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			var response = invoke(engineQName, "applyMutation", request, null, null, null);
			if (response.optBoolean("ok", false) && response.has("source")) {
				var newSource = response.optString("source", source);
				FileUtils.writeStringToFile(sourceFile, newSource, "UTF-8");
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

	private static String flowNameFromSourceFile(File sourceFile) {
		if (sourceFile == null) {
			return "";
		}
		var name = sourceFile.getName();
		return name.endsWith(".flow.js") ? name.substring(0, name.length() - ".flow.js".length()) : name;
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
			var descriptor = new JSONObject()
					.put("version", 1)
					.put("icon", runtime.equals("flow") ? "mdi:source-branch" : "mdi:language-javascript")
					.put("description", runtime.equals("flow") ? "New composite Flow block." : "New JavaScript Flow block.")
					.put("tags", new JSONArray().put(runtime.equals("flow") ? "composite" : "javascript"))
					.put("props", new JSONObject())
					.put("hooks", new JSONObject().put("file", localName + ".hooks.js"))
					.put("implementation", new JSONObject()
							.put("runtime", runtime)
							.put("file", runtime.equals("flow") ? localName + ".flow.js" : localName + ".js"));
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
					var cachedResponse = cachedMethodResponse(engineRef, engineSource, method, request);
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

			if (runtimeLookup != null) {
				synchronized (runtimeLookup.runtime()) {
					var response = invokePrepared(engineRef, engineFile, engineSource, method, request, convertigoContext, cx, engineScope,
							engineObject, runtimeLookup);
					storeMethodResponse(engineRef, engineSource, method, request, response);
					return response;
				}
			}
			var response = invokePrepared(engineRef, engineFile, engineSource, method, request, convertigoContext, cx, engineScope,
					engineObject, null);
			storeMethodResponse(engineRef, engineSource, method, request, response);
			return response;
		} catch (EngineException e) {
			failed = true;
			throw e;
		} catch (Exception e) {
			failed = true;
			throw new EngineException("Unable to invoke Flow engine \"" + engineRef.qname + "\" method \"" + method + "\".", e);
		} finally {
			recordInvocation(engineRef, method, System.nanoTime() - started, runtimeLookup, failed, methodCacheHit);
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
		for (var entry : invocationStats.entrySet()) {
			var stats = entry.getValue();
			if (stats.engineQName.equals(normalizedEngineQName)) {
				methods.put(stats.method, stats.toJson());
			}
		}
		return new JSONObject()
				.put("generation", cacheGeneration.get())
				.put("sourceCacheSize", engineSourceCache.size())
				.put("runtimeCacheSize", engineRuntimeCache.size())
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

	private static CachedMethodResponse cachedMethodResponse(EngineRef engineRef, CachedEngineSource engineSource, String method,
			JSONObject request) {
		var key = methodResponseCacheKey(engineRef, engineSource, method, request);
		var cached = methodResponseCache.get(key);
		if (cached != null && cached.generation() == cacheGeneration.get()) {
			methodResponseCacheHits.increment();
			return cached;
		}
		methodResponseCacheMisses.increment();
		return null;
	}

	private static void storeMethodResponse(EngineRef engineRef, CachedEngineSource engineSource, String method, JSONObject request,
			JSONObject response) {
		if (!isCacheableMethod(method, request) || response == null) {
			return;
		}
		if (methodResponseCache.size() >= METHOD_RESPONSE_CACHE_LIMIT) {
			methodResponseCache.clear();
			methodResponseCacheInvalidations.increment();
		}
		methodResponseCache.put(methodResponseCacheKey(engineRef, engineSource, method, request),
				new CachedMethodResponse(response.toString(), cacheGeneration.get()));
	}

	private static String methodResponseCacheKey(EngineRef engineRef, CachedEngineSource engineSource, String method, JSONObject request) {
		return engineRef.qname + "|" + engineSource.sourceName() + "|" + cacheGeneration.get() + "|" + method + "|"
				+ request.toString();
	}

	private static boolean isCacheableMethod(String method, JSONObject request) {
		if ("nodeOutputSchema".equals(method)) {
			return isReadOnlySchemaRequest(request);
		}
		if ("outputSchema".equals(method)) {
			return isReadOnlySchemaRequest(request);
		}
		return switch (method) {
		case "describeTree", "catalog", "context", "contextMenu", "propertyEditor", "icons", "syncInputs", "blockGet", "typeGet" -> true;
		default -> false;
		};
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
		case "run", "analyze", "search", "resourceSearch", "resourceList", "resourceGet", "flowSourceGet", "flowSourceValidate",
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
		var thread = Thread.currentThread();
		var key = baseKey + "|thread:" + thread.getName();
		var generation = cacheGeneration.get();
		var cached = engineRuntimeCache.get(key);
		if (cached != null && cached.generation() == generation && cached.sourceName().equals(engineSource.sourceName())) {
			return new CachedEngineRuntimeLookup(cached, true, key, generation, engineRuntimeCache.size());
		}
		engineRuntimeCache.entrySet().removeIf(entry -> entry.getKey().startsWith(engineRef.qname + "|")
				&& (entry.getValue().generation() != generation || !entry.getKey().startsWith(baseKey + "|thread:")));
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
			var fresh = new CachedEngineRuntime(engineSource.sourceName(), generation, scope, engineObject);
			engineRuntimeCache.put(key, fresh);
			return new CachedEngineRuntimeLookup(fresh, false, key, generation, engineRuntimeCache.size());
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to initialize Flow engine runtime \"" + engineRef.qname + "\".", e);
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
