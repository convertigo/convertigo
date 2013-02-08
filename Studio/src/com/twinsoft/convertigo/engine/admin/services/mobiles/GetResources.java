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
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.GenericUtils;

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
	
	private void listFiles(File directory, JSONObject response) throws JSONException, IOException {
		
		File canonicalDir = directory.getCanonicalFile();
		URI uriDirectory = canonicalDir.toURI();
		Collection<File> filesFound = GenericUtils.cast(FileUtils.listFiles(canonicalDir, null, true));
		JSONArray jArray = new JSONArray();
		
		 for (File f : filesFound) {
			 File canonnicalF = f.getCanonicalFile();
			 JSONObject jObj = new JSONObject();
			 URI uriFile = canonnicalF.toURI();
			 jObj.put("uri", uriFile.toString().substring(uriDirectory.toString().length()));
			 jObj.put("date", canonnicalF.lastModified());
			 jObj.put("size", canonnicalF.length());
			 jArray.put(jObj);
		 }
		 response.put("file", jArray);
	}
}
