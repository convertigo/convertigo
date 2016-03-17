/*
 * Copyright (c) 2001-2014 Convertigo SA.
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
 */

package com.twinsoft.convertigo.engine.admin.services.roles;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.UploadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Import",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class Import extends UploadService {
	
	@Override
	protected void doUpload(HttpServletRequest request, Document document, FileItem item) throws Exception {
		String actionImport = request.getParameter("action-import");
		if (actionImport.equals("on")){
			actionImport = request.getParameter("priority");
		}
		
		if (!item.getName().endsWith(".json")) {
			ServiceUtils.addMessage(document, document.getDocumentElement(), "The import of the user file "
					+ item.getName() + " has failed. The file is not valid (.json required).", "error",
					false);
		}
		
		//We save the users imported file
		try {
			byte[] data = IOUtils.toByteArray(item.getInputStream());
			String json = new String(data, "UTF-8");
			JSONObject users = new JSONObject(json);
			Engine.authenticatedSessionManager.updateUsers(users, actionImport);
			
		} catch (IOException ioe) {
			String message = "Unable to load the db file:\n" + ioe.getMessage();
			ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
			throw new EngineException("Unable to load the db file", ioe);
		}	
		
		String message = "The users file has been successfully imported.";
		Engine.logAdmin.info(message);
		ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
	}

	@Override
	protected String getRepository() {
		return Engine.CONFIGURATION_PATH + "/";
	}	
}
