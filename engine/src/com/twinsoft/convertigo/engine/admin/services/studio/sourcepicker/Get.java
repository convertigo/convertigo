/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.studio.SourcePickerModel;
import com.twinsoft.convertigo.engine.studio.SourcePickerModel.NodeModel;

@ServiceDefinition(name = "Get", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Get extends JSonService {
	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}
		String sourcePriority = request.getParameter("sourcePriority");
		String xpath = request.getParameter("xpath");
		putModel(response, SourcePickerModel.get(id, sourcePriority, xpath));
	}

	static void putModel(JSONObject response, SourcePickerModel.SourceModel model) throws Exception {
		response.put("ownerId", model.ownerId);
		response.put("sourceId", model.sourceId);
		response.put("sourceName", model.sourceName);
		response.put("sourcePriority", model.sourcePriority);
		response.put("schemaSourceId", model.schemaSourceId);
		response.put("schemaSourceName", model.schemaSourceName);
		response.put("anchor", model.anchor);
		response.put("xpath", model.xpath);
		response.put("displayXpath", model.displayXpath);
		response.put("available", model.available);
		response.put("message", model.message);
		response.put("tree", toJson(model.tree));
		response.put("result", toJson(model.result));
		response.put("jsonTree", toJson(model.jsonTree));
		response.put("jsonResult", toJson(model.jsonResult));
	}

	static JSONObject toJson(NodeModel node) throws Exception {
		if (node == null) {
			return null;
		}
		JSONObject json = new JSONObject();
		json.put("type", node.type);
		json.put("label", node.label);
		json.put("name", node.name);
		json.put("value", node.value);
		json.put("xpath", node.xpath);
		json.put("displayXpath", node.displayXpath);
		JSONArray children = new JSONArray();
		for (NodeModel child : node.children) {
			children.put(toJson(child));
		}
		json.put("children", children);
		return json;
	}
}
