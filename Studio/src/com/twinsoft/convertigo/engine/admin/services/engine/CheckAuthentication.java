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

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Authenticate",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = "the authentication status"
	)
	
public class CheckAuthentication extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		HttpSession httpSession = request.getSession();
		
		// Handle anonymous access for test platform user
		if (EnginePropertiesManager.getProperty(PropertyName.TEST_PLATFORM_USERNAME).length() == 0) {
			if (!Engine.authenticatedSessionManager.isAuthenticated(httpSession)) {
				Engine.authenticatedSessionManager.addAuthenticatedSession(httpSession, new Role[] { Role.TEST_PLATFORM });
			}
		}
		
		boolean bAuthenticated = Engine.authenticatedSessionManager.isAuthenticated(httpSession);
		Role[] roles = Engine.authenticatedSessionManager.getRoles(httpSession);
		if (roles != null) {
			Engine.logAdmin.info("Added roles: " + Arrays.toString(roles));
			ServiceUtils.addRoleNodes(document.getDocumentElement(), roles);
		}
		
		if (bAuthenticated) {
			Engine.logAdmin.info("Check authentication success");
			ServiceUtils.addMessage(document, document.getDocumentElement(), "" + httpSession.getAttribute(SessionKey.ADMIN_USER.toString()), "user", false);
			ServiceUtils.addMessage(document, document.getDocumentElement(), "true", "authenticated", false);
		} else {
			Engine.logAdmin.info("Check authentication failed (no role defined)");
			ServiceUtils.addMessage(document, document.getDocumentElement(), "false", "authenticated", false);
		}
	}
}
