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

import java.io.File;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.DeleteProjectOption;
import com.twinsoft.convertigo.engine.sync.SharedWorkspaceSyncManager;

/**
 * Removes a project from the Studio, as the deletion of a project in the Eclipse Studio, whose "Delete project
 * content on disk" is not checked by default.
 * <ul>
 * <li>action=info (default), projectName: path (the folder of the project), linked (a project of another folder,
 * imported in place or cloned, that the workspace only links to), git (the folder is a Git repository)</li>
 * <li>action=remove, projectName, deleteFiles: true deletes the folder of the project, as projects.Delete; else the
 * project leaves the Studio and its folder stays on disk: the link to a linked project is removed, a project of the
 * workspace is closed (it stays listed, closed, and can be opened again)</li>
 * </ul>
 */
@ServiceDefinition(name = "Remove", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class Remove extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var manager = Engine.theApp.databaseObjectsManager;
		if (projectName == null || projectName.isBlank() || !manager.getAllProjectNamesList(false).contains(projectName)) {
			throw new ServiceException("The project " + projectName + " is not in the workspace.");
		}
		var dir = new File(Engine.projectDir(projectName));
		var link = new File(Engine.PROJECTS_PATH, projectName);
		var linked = link.isFile();
		if ("remove".equals(request.getParameter("action"))) {
			Engine.theApp.schemaManager.clearCache(projectName);
			if (Boolean.parseBoolean(request.getParameter("deleteFiles"))) {
				manager.deleteProjectAndCar(projectName);
				SharedWorkspaceSyncManager.markProjectDelete(projectName);
				Engine.logStudio.info("The project " + projectName + " and its folder " + dir + " have been deleted");
				response.put("removed", "deleted");
			} else if (linked) {
				manager.deleteProject(projectName, DeleteProjectOption.unloadOnly);
				if (!link.delete()) {
					throw new ServiceException("The link to the project " + projectName + " cannot be removed: " + link);
				}
				manager.getStudioProjects().projectRemoved(projectName);
				Engine.logStudio.info("The project " + projectName + " left the Studio, its folder " + dir + " is kept");
				response.put("removed", "unlinked");
			} else {
				if (!manager.getStudioProjects().setClosed(projectName, true)) {
					throw new ServiceException("The project " + projectName + " cannot be closed here.");
				}
				manager.deleteProject(projectName, DeleteProjectOption.unloadOnly);
				Engine.logStudio.info("The project " + projectName + " is closed, its folder " + dir + " is kept");
				response.put("removed", "closed");
			}
		}
		response.put("projectName", projectName);
		response.put("path", dir.getAbsolutePath());
		response.put("linked", linked);
		response.put("git", new File(dir, ".git").exists());
	}
}
