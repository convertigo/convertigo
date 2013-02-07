/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetResources",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = ""
	)
public class GetResources extends JSonService {
	
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String application = request.getParameter("application");
		String platform = request.getParameter("platform");
		String uuid = request.getParameter("uuid");
		
		Engine.logAdmin.debug("(mobile.GetResources) Requested for application " + application + " by the platform " + platform + " and the uuid " + uuid);
		
		// Checking the project exists
		if (!Engine.theApp.databaseObjectsManager.existsProject(application)) {
			throw new ServiceException("Unable to build application '" + application
					+ "'; reason: the project does not exist");
		}
		
		response.put("needUpdate", "yes");
		
		listFiles(new File(Engine.PROJECTS_PATH + "/" + application + "/_private/flashupdate"), response);
	}
	
	private void listFiles(File directory, JSONObject response) {
		
	}
}
