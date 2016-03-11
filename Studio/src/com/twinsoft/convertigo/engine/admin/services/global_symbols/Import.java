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

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.UploadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Import",
		roles = { Role.WEB_ADMIN, Role.SYMBOLS_CONFIG },
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
		
		if (!item.getName().endsWith(".properties")) {
			ServiceUtils.addMessage(document, document.getDocumentElement(), "The import of the grobal symbol file "
					+ item.getName() + " has failed. The file is not valid (.properties required).", "error",
					false);
		}
		
		//We save the global symbols imported file
		Properties prop = new Properties();
		try {
			prop.load(item.getInputStream());				
		} catch (IOException ioe) {
			String message = "Unable to load property file:\n" + ioe.getMessage();
			ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
			throw new EngineException("Unable to load property file", ioe);
		}
		
		Engine.theApp.databaseObjectsManager.symbolsUpdate(prop, actionImport);	
		
		String message = "The global symbols file has been successfully imported.";
		Engine.logAdmin.info(message);
		ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
	}

	@Override
	protected String getRepository() {
		return Engine.CONFIGURATION_PATH + "/";
	}	
}
