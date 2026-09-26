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
import java.util.HashSet;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;

/**
 * The version of a project and what its archive includes, as the dialog of the Eclipse Studio before an
 * export or a deployment shows them and keeps them.
 * <ul>
 * <li>projectName: the project</li>
 * <li>options: the included parts to keep, as a JSON object of booleans; missing to read them</li>
 * <li>version: the version of the project to set with them</li>
 * </ul>
 */
@ServiceDefinition(name = "ArchiveOptions", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class ArchiveOptions extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var project = projectName == null ? null : Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("Unknown project " + projectName);
		}
		var dir = project.getDirFile();
		var selected = ArchiveExportOption.load(dir);
		var chosen = request.getParameter("options");
		if (chosen != null) {
			var json = new JSONObject(chosen);
			selected = new HashSet<>(ArchiveExportOption.all);
			for (var option : ArchiveExportOption.values()) {
				if (json.has(option.name()) && !json.getBoolean(option.name())) {
					selected.remove(option);
				}
			}
			// the options are kept in the private folder of the project, which a new project may not have yet
			new File(dir, "_private").mkdirs();
			ArchiveExportOption.save(dir, selected);
			var version = request.getParameter("version");
			if (version != null && !version.equals(project.getVersion())) {
				project.setVersion(version);
				project.hasChanged = true;
				response.put("versionChanged", true);
			}
		}
		var options = new JSONArray();
		for (var option : ArchiveExportOption.values()) {
			var size = option.size(dir);
			if (size > 0) {
				var item = new JSONObject()
						.put("name", option.name())
						.put("display", option.display())
						.put("selected", selected.contains(option));
				if (option != ArchiveExportOption.includeTestCase) {
					item.put("size", FileUtils.byteCountToDisplaySize(size));
				}
				options.put(item);
			}
		}
		response.put("version", project.getVersion());
		response.put("options", options);
	}
}
