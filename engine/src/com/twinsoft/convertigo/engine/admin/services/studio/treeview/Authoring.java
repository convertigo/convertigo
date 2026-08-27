/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

@ServiceDefinition(
		name = "Authoring",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = "")
public class Authoring extends JSonService {
	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id != null && !id.isBlank()) {
			var databaseObject = FlowStudioSupport.resolveTreeObject(id);
			var reference = FlowStudioSupport.authoringReference(databaseObject);
			if (reference != null) {
				response.put("id", databaseObject.getFullQName());
				response.put("reference", reference);
			}
			return;
		}

		var serializedReference = request.getParameter("reference");
		if (serializedReference == null || serializedReference.isBlank()) {
			throw new ServiceException("missing id or reference parameter");
		}
		var reference = new JSONObject(serializedReference);
		var projectName = request.getParameter("project");
		if (projectName == null || projectName.isBlank()) {
			projectName = reference.optString("sourceProject", "");
		}
		var databaseObject = FlowStudioSupport.resolveAuthoringReference(projectName, reference);
		if (databaseObject != null) {
			response.put("id", databaseObject.getFullQName());
			var canonicalReference = FlowStudioSupport.authoringReference(databaseObject);
			response.put("reference", canonicalReference == null ? reference : canonicalReference);
		}
	}
}
