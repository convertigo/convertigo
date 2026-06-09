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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import com.twinsoft.convertigo.beans.core.Project;
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
			String engineQName = effectiveEngineQName(flow);
			JSONObject request = baseRequest(engineQName, flow.getFlowSource(), flow.getQName(), convertigoContext)
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
			String engineQName = effectiveEngineQName(flow);
			JSONObject request = baseRequest(engineQName, flow.getFlowSource(), flow.getQName(), null)
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
		try {
			String engineQName = effectiveEngineQName(flow);
			JSONObject request = baseRequest(engineQName, flow == null ? "" : flow.getFlowSource(), flow == null ? "" : flow.getQName(), null)
					.put("projectDir", flow == null || flow.getProject() == null ? "" : flow.getProject().getDirPath());
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

	public JSONObject describeTree(FlowEngine flowEngine) throws EngineException {
		try {
			String engineQName = effectiveEngineQName(flowEngine);
			JSONObject request = baseRequest(engineQName, "", flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine.getEngineSource())
					.put("projectDir", flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			return invoke(engineQName, "describeTree", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine tree request.", e);
		}
	}

	public JSONObject catalog(FlowEngine flowEngine) throws EngineException {
		try {
			String engineQName = effectiveEngineQName(flowEngine);
			JSONObject request = baseRequest(engineQName, "", flowEngine == null ? "" : flowEngine.getQName(), null)
					.put("projectDir", flowEngine == null || flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath());
			return invoke(engineQName, "catalog", request, null, null, null);
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine catalog request.", e);
		}
	}

	public JSONObject applyMutation(Flow flow, JSONObject mutation) throws EngineException {
		try {
			String engineQName = effectiveEngineQName(flow);
			JSONObject request = baseRequest(engineQName, flow.getFlowSource(), flow.getQName(), null)
					.put("target", "flow")
					.put("flowName", flow.getName())
					.put("projectDir", flow.getProject() == null ? "" : flow.getProject().getDirPath())
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			JSONObject response = invoke(engineQName, "applyMutation", request, null, null, null);
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
			String engineQName = effectiveEngineQName(flowEngine);
			JSONObject request = baseRequest(engineQName, "", flowEngine.getQName(), null)
					.put("target", "engine")
					.put("engineSource", flowEngine.getEngineSource())
					.put("projectDir", flowEngine.getProject() == null ? "" : flowEngine.getProject().getDirPath())
					.put("mutation", mutation == null ? new JSONObject() : mutation);
			JSONObject response = invoke(engineQName, "applyMutation", request, null, null, null);
			if (response.optBoolean("ok", false) && response.has("source")) {
				flowEngine.setEngineSource(response.optString("source", flowEngine.getEngineSource()));
			}
			return response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build FlowEngine mutation request.", e);
		}
	}

	private String effectiveEngineQName(Flow flow) {
		if (flow == null) {
			return DEFAULT_ENGINE_QNAME;
		}
		Project project = flow.getProject();
		FlowEngine flowEngine = project == null ? null : project.getFlowEngine();
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
		JSONObject request = new JSONObject()
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

	private JSONObject invoke(String engineQName, String method, JSONObject request, Context convertigoContext,
			org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		EngineRef engineRef = EngineRef.parse(normalizeEngineQName(engineQName));
		File engineFile = resolveEngineFile(engineRef);
		org.mozilla.javascript.Context cx = javascriptContext;
		Scriptable engineScope = scope;
		boolean entered = false;

		if (cx == null) {
			cx = org.mozilla.javascript.Context.enter();
			engineScope = cx.initStandardObjects();
			entered = true;
		} else if (engineScope == null) {
			engineScope = cx.initStandardObjects();
		}

		try {
			if (convertigoContext != null) {
				Scriptable jsContext = org.mozilla.javascript.Context.toObject(convertigoContext, engineScope);
				engineScope.put("context", engineScope, jsContext);
			}

			String source = FileUtils.readFileToString(engineFile, "UTF-8");
			engineScope.put("__flowEngineDir", engineScope, engineFile.getParentFile().getAbsolutePath());
			String projectDir = request.optString("projectDir", "");
			engineScope.put("__flowProjectDir", engineScope, projectDir);
			Object engine = RhinoUtils.evalInterpretedJavascript(cx, engineScope, source,
					engineFile.getAbsolutePath() + "#" + engineFile.lastModified(), 1, null);
			if (engine == null || Undefined.isUndefined(engine)) {
				engine = ScriptableObject.getProperty(engineScope, engineRef.objectName);
			}
			if (!(engine instanceof Scriptable)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" must evaluate to a JavaScript object.");
			}

			Scriptable engineObject = (Scriptable) engine;
			Object function = ScriptableObject.getProperty(engineObject, method);
			if (!(function instanceof Function)) {
				throw new EngineException("Flow engine \"" + engineRef.qname + "\" does not expose method \"" + method + "\".");
			}

			Object result = ((Function) function).call(cx, engineScope, engineObject, new Object[] { request.toString() });
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
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(engineRef.projectName, true);
			File engineFile = new File(project.getDirPath(), ENGINE_BASE_PATH + engineRef.scriptPath + ".js");
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
			String json = result instanceof CharSequence ? result.toString() : RhinoUtils.jsonStringify(result);
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
			int dot = qname.indexOf('.');
			if (dot <= 0 || dot == qname.length() - 1) {
				throw new EngineException("Invalid Flow engine QName \"" + qname + "\". Expected \"project.Engine\".");
			}
			String projectName = qname.substring(0, dot);
			String objectName = qname.substring(dot + 1);
			String scriptPath = objectName.replace('.', File.separatorChar);
			return new EngineRef(qname, projectName, objectName, scriptPath);
		}
	}
}
