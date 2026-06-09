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
			return invoke(engineQName, "describeTree", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow tree request.", e);
		}
	}

	public JSONObject catalog(Flow flow) throws EngineException {
		return catalog(flow, false);
	}

	public JSONObject catalog(Flow flow, boolean includePrivate) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
			if (includePrivate) {
				request.put("includePrivate", true);
			}
			return invoke(engineQName, "catalog", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow catalog request.", e);
		}
	}

	public JSONObject outputSchema(Flow flow) throws EngineException {
		try {
			var engineQName = effectiveEngineQName(flow);
			var request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("flowName", flow == null ? "" : flow.getName())
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("allowRequestableSchema", false);
			return invoke(engineQName, "outputSchema", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow output schema request.", e);
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
		try {
			var engineQName = effectiveEngineQName(flowEngine);
			var request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			if (includePrivate) {
				request.put("includePrivate", true);
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
			var sourceFile = new File(sourcePath == null ? "" : sourcePath);
			if (!sourceFile.isFile()) {
				throw new EngineException("Flow source file not found: " + sourcePath);
			}
			var source = FileUtils.readFileToString(sourceFile, "UTF-8");
			var request = baseRequest(engineQName, source, flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("target", "flow")
					.put("flowSource", source)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			var response = invoke(engineQName, "applyMutation", request, null, null, null);
			if (response.optBoolean("ok", false) && response.has("source")) {
				FileUtils.writeStringToFile(sourceFile, response.optString("source", source), "UTF-8");
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
				throw new EngineException("Flow block name is defined by its *.block.yaml file and cannot be edited as a property.");
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
							.put("file", runtime.equals("flow") ? localName + ".flow.yaml" : localName + ".js"));
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
		var engineFile = resolveEngineFile(engineRef);
		var cx = javascriptContext;
		var engineScope = scope;
		var entered = false;

		if (cx == null) {
			cx = org.mozilla.javascript.Context.enter();
			engineScope = cx.initStandardObjects();
			entered = true;
		} else if (engineScope == null) {
			engineScope = cx.initStandardObjects();
		}

		try {
			if (convertigoContext != null) {
				var jsContext = org.mozilla.javascript.Context.toObject(convertigoContext, engineScope);
				engineScope.put("context", engineScope, jsContext);
			}

			var source = FileUtils.readFileToString(engineFile, "UTF-8");
			engineScope.put("__flowEngineDir", engineScope, engineFile.getParentFile().getAbsolutePath());
			var projectDir = request.optString("projectDir", "");
			engineScope.put("__flowProjectDir", engineScope, projectDir);
			var engine = RhinoUtils.evalInterpretedJavascript(cx, engineScope, source,
					engineFile.getAbsolutePath() + "#" + engineFile.lastModified(), 1, null);
			if (engine == null || Undefined.isUndefined(engine)) {
				engine = ScriptableObject.getProperty(engineScope, engineRef.objectName);
			}
			if (!(engine instanceof Scriptable)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" must evaluate to a JavaScript object.");
			}

			var engineObject = (Scriptable) engine;
			var function = ScriptableObject.getProperty(engineObject, method);
			if (!(function instanceof Function)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" does not expose method \"" + method + "\".");
			}

			var result = ((Function) function).call(cx, engineScope, engineObject, new Object[] { request.toString() });
			return toJsonObject(result, engineRef.qname, method);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to invoke Flow engine \"" + engineRef.qname + "\" method \"" + method + "\".", e);
		} finally {
			if (entered) {
				org.mozilla.javascript.Context.exit();
			}
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
}
