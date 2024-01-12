/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.FileUtils;

@ServiceDefinition(
		name = "ExportOption",
		roles = { Role.TEST_PLATFORM, Role.PROJECTS_CONFIG, Role.PROJECTS_VIEW },
		parameters = {},
		returnValue = "return export option for projects"
	)
public class ExportOptions extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String projectName = request.getParameter("projectName");
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		File projectDir = project.getDirFile();
		
		Element root = document.getDocumentElement();
		Element options = document.createElement("options");
		for (ArchiveExportOption o : ArchiveExportOption.values()) {
			long size = o.size(projectDir);
			if (size > 0) {
				Element e = document.createElement("option");
				e.setAttribute("name", o.name());
				if (o == ArchiveExportOption.includeTestCase) {
					e.setAttribute("display", o.display());
				} else {
					e.setAttribute("display", o.display() + " [" + FileUtils.byteCountToDisplaySize(size) +"]");
				}
				options.appendChild(e);
			}
		};

		root.appendChild(options);
	}
}
