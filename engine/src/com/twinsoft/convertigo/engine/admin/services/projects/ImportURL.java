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

package com.twinsoft.convertigo.engine.admin.services.projects;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.sync.SharedWorkspaceSyncManager;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

@ServiceDefinition(
		name = "ExportOption",
		roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG },
		parameters = {},
		returnValue = "return export option for projects"
	)
public class ImportURL extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String error = null;
		String projectName = null;
		try {
			String url = request.getParameter("url");
			ProjectUrlParser parser = new ProjectUrlParser(url);
			if (parser.isValid()) {
				Project project;
				if (url.trim().matches("(?i)https?://.*") && parser.getGitUrl() != null) {
					// an archive given without a name keeps the name of its project, as a deployed archive does,
					// instead of the name of its file, as lib_OAuth_8.0.0.car
					project = Engine.theApp.databaseObjectsManager.deployProject(parser.getGitUrl(), null, true);
				} else {
					project = Engine.theApp.referencedProjectManager.importProject(parser, true);
				}
				if (project == null) {
					error = "No project loaded with: " + url;
				} else {
					projectName = project.getName();
					Engine.theApp.schemaManager.clearCache(projectName);
					Project.executeAutoStartSequences(projectName);
					SharedWorkspaceSyncManager.markProjectReload(projectName);
				}
			} else {
				error = "The format is invalid";
			}
		} catch (Exception e) {
			error = "Failed to import project from URL, " + e.getClass().getSimpleName() + ": " + e.getMessage();
			Engine.logAdmin.warn(error, e);
		}
		
		Element root = document.getDocumentElement();
		Element elt;
		if (error == null) {
			elt = document.createElement("success");
			elt.setTextContent("The project '" + projectName + "' has been successfully imported.");
		} else {
			elt = document.createElement("error");
			elt.setTextContent(error);
		}

		root.appendChild(elt);
	}
}
