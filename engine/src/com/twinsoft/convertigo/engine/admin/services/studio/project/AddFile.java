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
import java.util.Map;

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
 * Adds to a project a file of the engine, as the "Add files" wizard of the Eclipse Studio: the stylesheets
 * handling the errors of Convertigo and the minimal one of web clipping.
 * <ul>
 * <li>projectName: the project</li>
 * <li>name: the file to add, missing to list them</li>
 * <li>overwrite: true to replace an existing file, else the service tells it exists</li>
 * </ul>
 */
@ServiceDefinition(name = "AddFile", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class AddFile extends JSonService {

	private static final Map<String, String> FILES = Map.of(
			"error.xsl", "The stylesheet file to handle Convertigo errors",
			"error_fr.xsl", "The French translated stylesheet file to handle Convertigo errors",
			"donothing.xsl", "The minimal stylesheet file to be used by default HTML screen class for Web clipping");

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var name = request.getParameter("name");
		if (name == null) {
			var files = new JSONArray();
			for (var file : new String[] { "error.xsl", "error_fr.xsl", "donothing.xsl" }) {
				files.put(new JSONObject().put("name", file).put("description", FILES.get(file)));
			}
			response.put("files", files);
			return;
		}
		if (!FILES.containsKey(name)) {
			throw new ServiceException("The file " + name + " cannot be added.");
		}
		var projectName = request.getParameter("projectName");
		var project = projectName == null ? null : Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("Unknown project " + projectName);
		}
		var target = new File(project.getDirFile(), name);
		response.put("file", name);
		if (target.exists() && !"true".equals(request.getParameter("overwrite"))) {
			response.put("exists", true);
			return;
		}
		var source = new File(Engine.XSL_PATH, name);
		if (source.exists()) {
			FileUtils.copyFile(source, target);
		} else {
			FileUtils.write(target, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "UTF-8");
		}
		response.put("done", true);
	}
}
