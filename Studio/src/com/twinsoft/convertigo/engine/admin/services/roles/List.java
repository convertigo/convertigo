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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/engine/admin/services/global_symbols/List.java $
 * $Author: fabienb $
 * $Revision: 30435 $
 * $Date: 2012-05-11 15:21:46 +0200 (Fri, 11 May 2012) $
 */

package com.twinsoft.convertigo.engine.admin.services.roles;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = "the users list"
	)
public class List extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
        
        Element usersListElement = document.createElement("users");
        root.appendChild(usersListElement);
        
    	for (String username : Engine.authenticatedSessionManager.getUsers()) { 
			Element userElement = document.createElement("user");
			userElement.setAttribute("name", username);
			for (Role role: Engine.authenticatedSessionManager.getRoles(username)) {
	        	Element roleElement = document.createElement("role");
	        	roleElement.setAttribute("name", role.name());
	        	userElement.appendChild(roleElement);				
			}
			usersListElement.appendChild(userElement);
    	}
    	        
        Element rolesListElement = document.createElement("roles");
        root.appendChild(rolesListElement);
        
        for (Role role : Role.values()) {
        	if (role.description() != null) {
	        	Element roleElement = document.createElement("role");
	        	roleElement.setAttribute("name", role.name());
	        	roleElement.setAttribute("description", role.description());
	        	rolesListElement.appendChild(roleElement);
        	}
        }
	}
}
