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

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Imports a project from its folder or its c8oProject.yaml on the disk of the engine, as the import wizard
 * of the Eclipse Studio imports one from the disk: the project stays in its folder, which the workspace
 * points to, as it points to the projects cloned from their Git repository.
 * <ul>
 * <li>path: the folder of the project or its c8oProject.yaml</li>
 * </ul>
 */
@ServiceDefinition(name = "ImportFolder", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class ImportFolder extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var path = request.getParameter("path");
		if (path == null || path.isBlank()) {
			throw new ServiceException("missing path parameter");
		}
		var file = new File(path.trim()).getCanonicalFile();
		var yaml = file.isDirectory() ? new File(file, "c8oProject.yaml") : file;
		if (!yaml.isFile() || !yaml.getName().equals("c8oProject.yaml")) {
			throw new ServiceException("No c8oProject.yaml in " + file + ".");
		}
		var projectName = DatabaseObjectsManager.getProjectName(yaml);
		if (projectName == null || projectName.isBlank()) {
			throw new ServiceException("The file " + yaml + " is not a Convertigo project.");
		}
		var folder = yaml.getParentFile();
		var target = new File(Engine.PROJECTS_PATH, projectName).getCanonicalFile();
		var manager = Engine.theApp.databaseObjectsManager;
		// a folder the workspace already points to loads in place
		var linked = target.isFile() && new File(FileUtils.readFileToString(target, "UTF-8").trim())
				.getCanonicalFile().equals(folder);
		if (!folder.equals(target) && !linked) {
			if (manager.existsProject(projectName) || target.exists()) {
				throw new ServiceException("A project named " + projectName + " already exists.");
			}
			// the workspace points to the folder of the project, loaded again at each start of the engine
			FileUtils.writeStringToFile(target, folder.getPath(), "UTF-8");
		}
		var project = manager.importProject(yaml, true);
		if (project == null) {
			throw new ServiceException("The project " + projectName + " cannot be loaded.");
		}
		response.put("done", true);
		response.put("project", project.getName());
		response.put("linked", !folder.equals(target));
		response.put("inPlace", folder.equals(target) || linked);
	}
}
