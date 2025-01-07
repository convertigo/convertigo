/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.roles;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Edit",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = "edit an existing user"
	)
public class Edit extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		String[] roles = request.getParameterValues("roles");
		String oldUsername = request.getParameter("oldUsername");
		
		Element root = document.getDocumentElement();
		Element response = document.createElement("response");

		if (roles == null) {
			roles = new String[]{};
		}
		
		try {
			Set<Role> set = new HashSet<Role>(roles.length);
			for (String role: roles) {
				try {
					set.add(Role.valueOf(role));
				} catch (Exception e) {
				}
			}
			
			if (StringUtils.isBlank(password)) {
				password = Engine.authenticatedSessionManager.getPassword(oldUsername);
			} else {
				AuthenticatedSessionManager.validatePassword(password);
				password = DigestUtils.sha512Hex(password);
			}
			
			if (!username.equals(oldUsername)) {
				if (Engine.authenticatedSessionManager.hasUser(username)) {
					throw new IllegalArgumentException("User '" + username + "' already exists");
				}
				Engine.authenticatedSessionManager.setUser(username, password, set);
				Engine.authenticatedSessionManager.deleteUser(oldUsername);
			} else {
				Engine.authenticatedSessionManager.setUser(username, password, set);
			}
			response.setAttribute("state", "success");
			response.setAttribute("message","User '" + username + "' have been successfully edited!");
		} catch (Exception e) {
			Engine.logAdmin.error("Error during editing the user!\n" + e.getMessage());
			
			response.setAttribute("state", "error");
			response.setAttribute("message","Error during editing the user!\n" + e.getMessage());
		}
		root.appendChild(response);		
	}
}
