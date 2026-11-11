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

package com.twinsoft.convertigo.engine.admin.services.studio.debug;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.JsonStreamRowsStep;
import com.twinsoft.convertigo.beans.steps.JsonTransformStep;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.admin.services.studio.treeview.Get;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;
import com.twinsoft.convertigo.engine.studio.NativeJsonSupport;

/**
 * The JSON of the native steps for the web Studio, as the Native JSON steps panel of the sequence editor and the JSON
 * picker of the Eclipse Studio show it. An execution of the web Studio passes its own token in the request parameter
 * __native to have the outputs of its native steps kept.
 * <ul>
 * <li>action=list, token, from: the native step executions of the run from the index given; with id (a sequence), its
 * kind (native, nativeSteps) and the XML steps that read the XML view of a native step (bridges)</li>
 * <li>action=json, token, index: the JSON of one execution</li>
 * <li>action=sample, id (a step): the JSON the step produced at its last execution in the web Studio</li>
 * <li>action=steps, id (a step), includeStep: the steps before it that give JSON, with their last samples</li>
 * </ul>
 */
@ServiceDefinition(name = "NativeJson", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class NativeJson extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var action = request.getParameter("action");
		if (action == null) {
			action = "list";
		}
		switch (action) {
			case "list" -> list(request, response);
			case "json" -> json(request, response);
			case "sample" -> sample(request, response);
			case "steps" -> steps(request, response);
			default -> throw new ServiceException("unknown action " + action);
		}
	}

	private static void list(HttpServletRequest request, JSONObject response) throws Exception {
		var token = request.getParameter("token");
		var from = parseInt(request.getParameter("from"), 0);
		var rows = new JSONArray();
		if (token != null) {
			for (var row : NativeJsonSupport.rows(token, from)) {
				var output = row.output;
				var step = output.getStep();
				var json = new JSONObject();
				json.put("index", row.index);
				json.put("stepId", step.getFullQName());
				json.put("stepName", step.getName());
				json.put("icon", "studio.dbo.GetIcon?iconPath=" + Get.iconPath(step));
				json.put("variable", output.getVariableName());
				json.put("count", output.getCount());
				json.put("durationMicros", output.getDurationMicros());
				json.put("size", output.getJson().length());
				json.put("truncated", output.isTruncated());
				json.put("kept", row.kept);
				if (output.getNote() != null) {
					json.put("note", output.getNote());
				}
				rows.put(json);
			}
		}
		response.put("rows", rows);
		var id = request.getParameter("id");
		if (id != null && !id.isBlank()) {
			var dbo = Utils.getDbo(id);
			var sequence = dbo instanceof Sequence s ? s : dbo instanceof Step st ? st.getSequence() : null;
			if (sequence != null) {
				response.put("native", NativeJsonSupport.isNativeSequence(sequence));
				response.put("nativeExecution", sequence.isNativeExecution());
				response.put("nativeSteps", NativeJsonSupport.hasNativeSteps(sequence));
				var bridges = new JSONArray();
				for (var bridge : NativeJsonSupport.getBridges(sequence).entrySet()) {
					var names = new JSONArray();
					for (var bridged : bridge.getValue()) {
						names.put(bridged.getName());
					}
					bridges.put(new JSONObject().put("stepId", bridge.getKey().getFullQName())
							.put("stepName", bridge.getKey().getName()).put("reads", names));
				}
				response.put("bridges", bridges);
			}
		}
	}

	private static void json(HttpServletRequest request, JSONObject response) throws Exception {
		var row = NativeJsonSupport.row(request.getParameter("token"), parseInt(request.getParameter("index"), -1));
		if (row == null) {
			response.put("found", false);
			return;
		}
		response.put("found", true);
		putOutput(response, row.output, row.kept);
	}

	private static void sample(HttpServletRequest request, JSONObject response) throws Exception {
		var step = step(request.getParameter("id"));
		response.put("stepId", step.getFullQName());
		response.put("stepName", step.getName());
		response.put("variable", NativeJsonSupport.getVariableName(step));
		response.put("stream", step instanceof JsonStreamRowsStep);
		var output = NativeJsonSupport.getSample(step);
		response.put("found", output != null);
		if (output != null) {
			putOutput(response, output, true);
			if (output.getInput() != null) {
				response.put("input", output.getInput());
			}
		}
	}

	private static void steps(HttpServletRequest request, JSONObject response) throws Exception {
		var step = step(request.getParameter("id"));
		var sequence = step.getSequence();
		response.put("sequenceName", sequence == null ? "" : sequence.getName());
		response.put("stepName", step.getName());
		var steps = new JSONArray();
		var list = NativeJsonSupport.getPreviousJsonSteps(step);
		if ("true".equals(request.getParameter("includeStep")) && NativeJsonSupport.isJsonProducer(step)) {
			list.add(step);
		}
		for (var previous : list) {
			var json = new JSONObject();
			json.put("id", previous.getFullQName());
			json.put("name", previous.getName());
			json.put("icon", "studio.dbo.GetIcon?iconPath=" + Get.iconPath(previous));
			json.put("variable", NativeJsonSupport.getVariableName(previous));
			json.put("stream", previous instanceof JsonStreamRowsStep);
			json.put("transform", previous instanceof JsonTransformStep);
			var output = NativeJsonSupport.getSample(previous);
			json.put("found", output != null);
			if (output != null) {
				json.put("count", output.getCount());
				json.put("truncated", output.isTruncated());
				if (output.getNote() != null) {
					json.put("note", output.getNote());
				}
			}
			steps.put(json);
		}
		response.put("steps", steps);
	}

	private static void putOutput(JSONObject response, NativeStepOutput output, boolean kept) throws Exception {
		response.put("stepId", output.getStep().getFullQName());
		response.put("stepName", output.getStep().getName());
		response.put("variable", output.getVariableName());
		response.put("count", output.getCount());
		response.put("truncated", output.isTruncated());
		response.put("kept", kept);
		if (output.getNote() != null) {
			response.put("note", output.getNote());
		}
		response.put("json", kept ? output.getJson() : "");
	}

	private static Step step(String id) throws Exception {
		DatabaseObject dbo = id == null ? null : Utils.getDbo(id);
		if (dbo instanceof Step step) {
			return step;
		}
		throw new ServiceException("no step " + id);
	}

	private static int parseInt(String value, int defaultValue) {
		try {
			return value == null ? defaultValue : Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
