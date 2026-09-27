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

import java.util.ArrayList;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.DeleteProjectOption;

/**
 * Closes projects, as the Eclipse Studio closes one: a closed project stays in the workspace, shown closed in
 * the tree, and does not load until it is opened again. Or opens them again.
 * <ul>
 * <li>projects: the JSON array of the names of the projects</li>
 * <li>open: true to open the projects again</li>
 * </ul>
 */
@ServiceDefinition(name = "Close", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class Close extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projects = request.getParameter("projects");
		if (projects == null || projects.isBlank()) {
			throw new ServiceException("missing projects parameter");
		}
		var open = "true".equals(request.getParameter("open"));
		var manager = Engine.theApp.databaseObjectsManager;
		var studio = manager.getStudioProjects();
		var names = new JSONArray(projects);
		var workspace = manager.getAllProjectNamesList(false);
		var done = new JSONArray();
		var errors = new ArrayList<String>();
		for (int i = 0; i < names.length(); i++) {
			var name = names.getString(i);
			try {
				if (!workspace.contains(name)) {
					errors.add("The project " + name + " is not in the workspace.");
				} else if (open) {
					if (!studio.setClosed(name, false)) {
						errors.add("The project " + name + " cannot be opened here.");
					} else if (manager.getOriginalProjectByName(name) == null) {
						errors.add("The project " + name + " cannot be loaded.");
					} else {
						done.put(name);
					}
				} else if (!studio.setClosed(name, true)) {
					errors.add("The project " + name + " cannot be closed here.");
				} else {
					// the project leaves the memory of the engine, with its contexts
					Engine.theApp.schemaManager.clearCache(name);
					manager.deleteProject(name, DeleteProjectOption.unloadOnly);
					done.put(name);
				}
			} catch (Exception e) {
				Engine.logStudio.warn("Unable to " + (open ? "open" : "close") + " the project " + name, e);
				errors.add("The project " + name + " cannot be " + (open ? "opened" : "closed") + ": " + e.getMessage());
			}
		}
		response.put("done", errors.isEmpty());
		response.put("projects", done);
		if (!errors.isEmpty()) {
			response.put("error", String.join("\n", errors));
		}
	}
}
