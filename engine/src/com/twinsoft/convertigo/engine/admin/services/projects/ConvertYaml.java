/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.admin.services.projects;

import jakarta.servlet.http.HttpServletRequest;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

/** Explicitly saves a loaded project in standard YAML, requiring engine 8.5.0 or later. */
@ServiceDefinition(name = "ConvertYaml", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG },
		parameters = { @ServiceParameterDefinition(name = "projectName", description = "Loaded project to convert to standard YAML (requires engine 8.5.0 or later)") }, returnValue = "Conversion result; the project now requires engine 8.5.0 or later")
public class ConvertYaml extends XmlService {
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		if (!"POST".equals(request.getMethod())) throw new ServiceException("YAML conversion requires POST");
		String name = request.getParameter("projectName");
		if (name == null || name.isBlank()) throw new ServiceException("Missing projectName parameter");
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
		if (project == null) throw new ServiceException("Project does not exist");
		// as a save: the authoring caches and the other instances of a shared workspace see the new files
		Engine.theApp.databaseObjectsManager.convertProjectToStandardYaml(project);
		ServiceUtils.addMessage(document, "Project saved in standard YAML; minimum engine version: " + project.getMinVersion(), "success");
	}
}
