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
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.RemoteAdmin;

/**
 * Deploys a project on a remote Convertigo server, as the deployment wizard of the Eclipse Studio: its
 * archive is made with the export options of the project, then sent to the server with the credentials of
 * one of its administrators.
 * <ul>
 * <li>projectName: the project to deploy</li>
 * <li>server: the server, as "host[:port]/convertigo"</li>
 * <li>https, trustAllCertificates: how to reach the server</li>
 * <li>user, password: an administrator of the server</li>
 * <li>assembleXsl: true to assemble the XSL sheets, for the legacy web clipping projects</li>
 * </ul>
 */
@ServiceDefinition(name = "Deploy", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class Deploy extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var server = request.getParameter("server");
		var user = request.getParameter("user");
		var password = request.getParameter("password");
		if (projectName == null || server == null || server.isBlank() || user == null || password == null) {
			throw new ServiceException("missing projectName, server, user or password parameter");
		}
		server = server.trim().replaceFirst("^https?://", "").replaceFirst("/+$", "");
		var https = "true".equals(request.getParameter("https"));
		var trustAllCertificates = "true".equals(request.getParameter("trustAllCertificates"));
		var assembleXsl = "true".equals(request.getParameter("assembleXsl"));

		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("The project " + projectName + " does not exist.");
		}
		var options = ArchiveExportOption.load(project.getDirFile());
		File archive;
		try {
			archive = CarUtils.makeArchive(new File(Engine.PROJECTS_PATH, projectName + ".car"), project, options);
		} catch (Exception e) {
			throw new ServiceException("The archive of " + projectName + " cannot be made: " + e.getMessage(), e);
		}
		try {
			var remoteAdmin = new RemoteAdmin(server, https, trustAllCertificates);
			remoteAdmin.login(user, password);
			remoteAdmin.deployArchive(archive, assembleXsl);
		} catch (Exception e) {
			throw new ServiceException("The deployment of " + projectName + " on " + server + " failed: " + e.getMessage(), e);
		}

		var base = (https ? "https://" : "http://") + server;
		response.put("done", true);
		response.put("dashboard", base + "/dashboard/" + projectName + "/backend/");
		if (new File(project.getDirFile(), "DisplayObjects/mobile/index.html").isFile()) {
			response.put("application", base + "/projects/" + projectName + "/DisplayObjects/mobile/");
		}
	}
}
