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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.SimpleCipher;

@ServiceDefinition(
		name = "Authenticate",
		roles = { Role.ANONYMOUS },
		parameters = {
				@ServiceParameterDefinition(
						name = "authType",
						description = "the authentication type required: login | logout"
					),
				@ServiceParameterDefinition(
						name = "authUserName",
						description = "the authentication user name"
					),
				@ServiceParameterDefinition(
						name = "authPassword",
						description = "the authentication user password (in clear text)"
					)
		},
		returnValue = "the authentication status"
	)
	
public class Authenticate extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		boolean logIn = "login".equals(ServiceUtils.getRequiredParameter(request, "authType"));

		HttpSession httpSession = request.getSession();
		String sessionId = httpSession.getId();
		
		// Login
		if (logIn) {

			String user = ServiceUtils.getRequiredParameter(request, "authUserName");
			String password = ServiceUtils.getRequiredParameter(request, "authPassword");

			httpSession.setAttribute("user", user);
			Engine.logAdmin.info("User '" + user + "' is trying to login");

			Role[] roles = null;

			// Legacy authentication
			if (EnginePropertiesManager.getProperty(PropertyName.ADMIN_USERNAME).equals(user)
					&& EnginePropertiesManager.checkProperty(PropertyName.ADMIN_PASSWORD, password)) {
				roles =  new Role[] { Role.WEB_ADMIN, Role.TEST_PLATFORM, Role.AUTHENTICATED };
			} else if (EnginePropertiesManager.getProperty(PropertyName.TEST_PLATFORM_USERNAME).equals(user)
					&& EnginePropertiesManager.checkProperty(PropertyName.TEST_PLATFORM_PASSWORD, password)) {
				roles = new Role[] { Role.TEST_PLATFORM, Role.AUTHENTICATED }; 
			}
			// Trial authentication
			else {
				File hackTrial = new File(Engine.CONFIGURATION_PATH + "/hackTrial.txt");
				if (hackTrial.exists()) {
					try {
						BufferedReader br = new BufferedReader(new FileReader(hackTrial));
						String line = br.readLine();
						br.close();
						if (!"ok, you can deploy !!".equals(line)) {
							Engine.logAdmin.error("Trial authentication failure: wrong internal data!");
						}
						else if (user.matches(".+@.+\\.[a-z]+") && user.equals(SimpleCipher.decode(password))) {
							roles =  new Role[] { Role.TRIAL };
							httpSession.setAttribute("trial_user", true);
						}
						else {
							Engine.logAdmin.error("Trial authentication failure: wrong username/password");
						}
					} catch (Exception e) {
						Engine.logAdmin.error("Trial authentication failure: wrong internal data!", e);
					}
				}
			}

			if (roles == null) {
				Engine.theApp.authenticatedSessionManager.removeAuthenticatedSession(sessionId);
				ServiceUtils.addMessage(document, document.getDocumentElement(), "", "error");		
			} else {
				Engine.theApp.authenticatedSessionManager.addAuthenticatedSession(sessionId, roles);

				ServiceUtils.addMessage(document, document.getDocumentElement(), "", "success");
				ServiceUtils.addRoleNodes(document.getDocumentElement(), roles);
				
				Engine.logAdmin.info("User '" + user + "' has been successfully authenticated");
			}
		}
		// Logout
		else {
			Engine.theApp.authenticatedSessionManager.removeAuthenticatedSession(sessionId);
			ServiceUtils.addMessage(document, document.getDocumentElement(), "", "success");
		}
	}
}
