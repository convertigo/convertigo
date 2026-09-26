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

package com.twinsoft.convertigo.engine.admin.services.studio.project;

import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

/**
 * The projects a project uses without referencing them, as the Eclipse Studio checks them after a load or a
 * save, and adds their reference objects.
 * <ul>
 * <li>projectName: the project</li>
 * <li>add: true to add a reference to each of them, else the service lists them</li>
 * </ul>
 */
@ServiceDefinition(name = "MissingReferences", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class MissingReferences extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var project = projectName == null ? null : Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("Unknown project " + projectName);
		}
		var missing = new TreeSet<>(project.getMissingProjectReferences().keySet());
		if ("true".equals(request.getParameter("add"))) {
			var added = new JSONArray();
			for (var target : missing) {
				var reference = new ProjectSchemaReference();
				reference.setName(target + "_reference");
				reference.setProjectName(ProjectUrlParser.getUrl(target));
				reference.hasChanged = true;
				project.add(reference);
				added.put(target);
			}
			response.put("added", added);
			return;
		}
		var references = new JSONArray();
		for (var target : missing) {
			references.put(target);
		}
		response.put("references", references);
		var projects = new JSONArray();
		for (var target : new TreeSet<>(project.getMissingProjects().keySet())) {
			projects.put(target);
		}
		response.put("projects", projects);
	}
}
