/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.studio.SourcePickerModel;

@ServiceDefinition(name = "Evaluate", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Evaluate extends JSonService {
	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}
		String sourcePriority = request.getParameter("sourcePriority");
		String xpath = request.getParameter("xpath");
		Get.putModel(response, SourcePickerModel.evaluate(id, sourcePriority, xpath));
	}
}
