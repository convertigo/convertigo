/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

/** Stores an edited Flow source as a working copy of its FlowEngine: the project Save writes it. */
@ServiceDefinition(name = "Set", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Set extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var content = request.getParameter("content");
		if (id == null || id.isBlank() || content == null) {
			throw new ServiceException("missing id or content parameter");
		}
		var source = Get.sourceDocument(Utils.getDbo(id));
		if (!source.writable()) {
			throw new ServiceException("The selected Flow source is read-only.");
		}
		var revision = request.getParameter("revision");
		if (revision != null && !revision.isBlank() && !revision.equals(source.revision())) {
			throw new ServiceException("The source changed since it was opened; reopen it before saving.");
		}
		var path = source.file().getPath();
		if (!content.equals(source.content())) {
			source.flowEngine().setSource(path, content);
			FlowEngineBridge.invalidateDataCaches();
			FlowStudioSupport.clearCatalogCache(source.flowEngine());
			FlowStudioSupport.afterSourceMutation(source.flowEngine(), path);
		}
		response.put("id", id);
		response.put("revision", Get.sha256(content));
		response.put("dirty", source.flowEngine().isSourceDirty(path));
	}
}
