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

import jakarta.servlet.http.HttpServletRequest;
import org.w3c.dom.Document;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.ProjectBundle;

@ServiceDefinition(name = "PreviewBundle", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class PreviewBundle extends XmlService {
	protected ProjectBundle.Selection preview(java.util.List<String> selected, boolean dependencies,
			java.util.List<String> included, java.util.List<String> excluded) throws Exception {
		return ProjectBundle.preview(selected, dependencies, included, excluded);
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		var selection = preview(ExportBundle.projectNames(request, "projects", true),
				Boolean.parseBoolean(request.getParameter("includeDependencies")),
				ExportBundle.projectNames(request, "includeProjects", false), ExportBundle.projectNames(request, "excludeProjects", false));
		var bundle = document.createElement("bundle");
		document.getDocumentElement().appendChild(bundle);
		for (var info : selection.plan().projects()) {
			var project = document.createElement("project");
			project.setAttribute("name", info.name());
			project.setAttribute("version", info.version());
			project.setAttribute("selected", Boolean.toString(selection.roots().contains(info.name())));
			bundle.appendChild(project);
		}
		for (var name : selection.excludedProjects()) {
			var excluded = document.createElement("excludedProject");
			excluded.setAttribute("name", name);
			bundle.appendChild(excluded);
		}
	}
}
