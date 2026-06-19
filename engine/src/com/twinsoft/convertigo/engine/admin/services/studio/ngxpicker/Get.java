/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.ngxpicker;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.studio.NgxSourcePickerModel;
import com.twinsoft.convertigo.engine.studio.NgxSourcePickerModel.NodeModel;

@ServiceDefinition(name = "Get", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Get extends JSonService {
	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}
		String propertyName = request.getParameter("propertyName");
		if (propertyName == null || propertyName.isBlank()) {
			throw new ServiceException("missing propertyName parameter");
		}
		putModel(response, NgxSourcePickerModel.get(
				id,
				propertyName,
				request.getParameter("filter"),
				request.getParameter("sourceData"),
				request.getParameter("path"),
				request.getParameter("prefix"),
				request.getParameter("suffix"),
				request.getParameter("custom"),
				request.getParameter("useCustom") == null ? null : Boolean.parseBoolean(request.getParameter("useCustom"))));
	}

	static void putModel(JSONObject response, NgxSourcePickerModel.Model model) throws Exception {
		response.put("ownerId", model.ownerId);
		response.put("propertyName", model.propertyName);
		response.put("projectName", model.projectName);
		response.put("filter", model.filter);
		response.put("path", model.path);
		response.put("prefix", model.prefix);
		response.put("suffix", model.suffix);
		response.put("custom", model.custom);
		response.put("input", model.input);
		response.put("computedValue", model.computedValue);
		response.put("useCustom", model.useCustom);
		response.put("available", model.available);
		response.put("message", model.message);
		response.put("sourceData", model.sourceData);
		response.put("sourceValue", model.sourceValue);
		JSONArray filters = new JSONArray();
		for (var filter : model.filters) {
			filters.put(new JSONObject()
					.put("value", filter.value)
					.put("label", filter.label)
					.put("supported", filter.supported));
		}
		response.put("filters", filters);
		response.put("sources", toJson(model.sources));
		response.put("modelTree", toJson(model.modelTree));
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
		json.put("path", node.path);
		json.put("qname", node.qname);
		json.put("source", node.source);
		json.put("sourceData", node.sourceData);
		json.put("selected", node.selected);
		JSONArray children = new JSONArray();
		for (NodeModel child : node.children) {
			children.put(toJson(child));
		}
		json.put("children", children);
		return json;
	}
}
