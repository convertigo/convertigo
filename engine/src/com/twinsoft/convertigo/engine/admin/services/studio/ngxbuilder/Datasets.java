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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.io.File;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * The datasets of an NGX application, as the application editor of the Eclipse Studio keeps them: the
 * session data of the application in the dataset folder of the project, which the preview restores.
 * <ul>
 * <li>project</li>
 * <li>action: list (default), get (name), save (name, data: a JSON array), remove (name)</li>
 * </ul>
 */
@ServiceDefinition(name = "Datasets", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Datasets extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(request.getParameter("project"));
		if (project == null) {
			throw new ServiceException("Unknown project " + request.getParameter("project"));
		}
		var dir = new File(project.getDirPath(), "dataset");
		var action = request.getParameter("action");
		var name = request.getParameter("name");
		if ("get".equals(action) || "save".equals(action) || "remove".equals(action)) {
			if (name == null || !name.matches("[\\w .-]+") || "none".equals(name)) {
				throw new ServiceException("Invalid dataset name " + name);
			}
			var file = new File(dir, name + ".json");
			if ("get".equals(action)) {
				response.put("data", file.exists() ? FileUtils.readFileToString(file, "UTF-8") : "[]");
			} else if ("remove".equals(action)) {
				// a dataset already removed from the disk is removed
				if (!file.delete() && file.exists()) {
					throw new ServiceException("The dataset " + name + " cannot be removed.");
				}
				response.put("done", true);
			} else {
				dir.mkdirs();
				FileUtils.write(file, new JSONArray(request.getParameter("data")).toString(2), "UTF-8");
				response.put("done", true);
			}
			response.put("name", name);
			return;
		}
		var names = new TreeSet<String>();
		var files = dir.listFiles((d, n) -> n.endsWith(".json"));
		for (var file : files == null ? new File[0] : files) {
			names.add(file.getName().replaceFirst("\\.json$", ""));
		}
		var list = new JSONArray();
		for (var n : names) {
			list.put(n);
		}
		response.put("datasets", list);
	}
}
