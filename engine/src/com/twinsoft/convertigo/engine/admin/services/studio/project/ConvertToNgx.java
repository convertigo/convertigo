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

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.NgxConverter;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * Converts the mobile application of a project to the NGX Mobile Builder, or upgrades its NGX application,
 * as the Convert Mobile Application Ngx action of the Eclipse Studio: the project itself, or a copy of it
 * under a new name.
 * <ul>
 * <li>projectName: the project, saved</li>
 * <li>targetName: the name of the converted project, the project itself when it is the same</li>
 * </ul>
 */
@ServiceDefinition(name = "ConvertToNgx", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class ConvertToNgx extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var manager = Engine.theApp.databaseObjectsManager;
		var project = projectName == null ? null : manager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("Unknown project " + projectName);
		}
		var mobile = project.testAttribute("isMobileApplicationProject", null);
		var targetName = request.getParameter("targetName");
		if (targetName == null || targetName.isBlank()) {
			targetName = projectName;
		}
		if (!StringUtils.isNormalized(targetName)) {
			throw new ServiceException("The name " + targetName + " has special characters.");
		}
		if (targetName.equals(projectName)) {
			// the project converts in place and loads again from its converted files
			var dir = project.getDirFile();
			manager.clearCache(projectName);
			new NgxConverter(dir).convertFile();
			manager.importProject(Engine.projectYamlFile(projectName), true);
		} else {
			if (manager.existsProject(targetName)) {
				throw new ServiceException("A project named " + targetName + " already exists.");
			}
			var car = CarUtils.makeArchive(project, ArchiveExportOption.all);
			var copy = manager.deployProject(car.getAbsolutePath(), targetName, true);
			if (copy == null) {
				throw new ServiceException("The copy " + targetName + " of " + projectName + " cannot be made.");
			}
			new NgxConverter(copy.getDirFile()).convertFile();
			manager.importProject(Engine.projectYamlFile(targetName), true);
		}
		response.put("done", true);
		response.put("project", targetName);
		response.put("message", "The project " + targetName + (mobile ? " is converted to NGX." : " is upgraded."));
	}
}
