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

package com.twinsoft.convertigo.engine.admin.services.store;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.StoreFiles;
import com.twinsoft.convertigo.engine.util.FileUtils;

@ServiceDefinition(
		name = "DeleteCustomStore",
		roles = { Role.WEB_ADMIN, Role.STORE_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class DeleteCustomStore extends XmlService {
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
		Element message = document.createElement("message");
		root.appendChild(message);
		
		File storeDirectory = new File(Engine.USER_WORKSPACE_PATH, StoreFiles.STORE_DIRECTORY_NAME);
		if (storeDirectory.exists()) {
			FileUtils.deleteDirectory(storeDirectory);
			message.setTextContent("The custom Store has correctly been deleted.");
		}
		else {
			message.setTextContent("No custom Store has been uploaded.");
		}
	}
}
