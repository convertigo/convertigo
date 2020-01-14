/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;

@ServiceDefinition(
		name = "Reload",
		roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Reload extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String projectName = request.getParameter("projectName");
    	Engine.theApp.databaseObjectsManager.clearCache(projectName);
    	Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
		Engine.logAdmin.info("The project '" + projectName + "' has been reloaded");
	}
	
}
