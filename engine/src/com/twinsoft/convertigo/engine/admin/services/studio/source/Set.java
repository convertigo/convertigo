/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

/**
 * Saves a source the Studio edits, as studio.source.Get gave it, only if it did not change since it was
 * read:
 * <ul>
 * <li>a text file of a project, whose id is "Project/relative/path";</li>
 * <li>an edited Flow source, stored as a working copy of its FlowEngine: the project Save writes it.</li>
 * </ul>
 * Parameters: id, content, and revision, the one studio.source.Get gave when the source was read.
 */
@ServiceDefinition(name = "Set", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Set extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var content = request.getParameter("content");
		if (id == null || id.isBlank() || content == null) {
			throw new ServiceException("missing id or content parameter");
		}
		if (NgxClasses.isClassId(id)) {
			var dbo = NgxClasses.target(id);
			var revision = request.getParameter("revision");
			if (revision != null && !revision.isEmpty() && !revision.equals(NgxClasses.revision(dbo))) {
				throw new ServiceException("The class of " + dbo.getName() + " changed since it was opened: open it again before saving.");
			}
			NgxClasses.save(dbo, content);
			response.put("done", true);
			response.put("id", id);
			response.put("revision", NgxClasses.revision(dbo));
			response.put("changed", true);
			return;
		}
		var revision = request.getParameter("revision");
		if (ProjectFiles.isFileId(id)) {
			var file = ProjectFiles.resolve(id);
			if (!ProjectFiles.isText(file)) {
				throw new ServiceException("The file " + file.getName() + " is not a text file the Studio can edit.");
			}
			var current = Get.sha256(Files.readString(file.toPath(), StandardCharsets.UTF_8));
			if (revision != null && !revision.isEmpty() && !revision.equals(current)) {
				throw new ServiceException("The file " + file.getName() + " changed since it was opened: open it again before saving.");
			}
			Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
			response.put("done", true);
			response.put("id", id);
			response.put("revision", Get.sha256(content));
			return;
		}
		var source = Get.sourceDocument(Utils.getDbo(id));
		if (!source.writable()) {
			throw new ServiceException("The selected Flow source is read-only.");
		}
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
