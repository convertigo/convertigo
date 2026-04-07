/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;

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
	static String timezone = Calendar.getInstance().getTimeZone().getDisplayName(false, TimeZone.SHORT);

	private static void setPublicDashboardAccess(HttpSession httpSession, Role[] roles) {
		if (roles == null) {
			roles = new Role[0];
		}
		Set<Role> merged = new LinkedHashSet<>(Arrays.asList(roles));
		merged.add(Role.TEST_PLATFORM);
		Engine.authenticatedSessionManager.addAuthenticatedSession(httpSession, merged.toArray(new Role[0]));
		httpSession.setAttribute(SessionKey.ADMIN_PUBLIC_DASHBOARD.toString(), Boolean.TRUE);
	}

	private static void clearPublicDashboardAccess(HttpSession httpSession, Role[] roles) {
		httpSession.removeAttribute(SessionKey.ADMIN_PUBLIC_DASHBOARD.toString());
		if (roles == null || !Engine.authenticatedSessionManager.isAuthenticated(httpSession)) {
			Engine.authenticatedSessionManager.removeAuthenticatedSession(httpSession);
			return;
		}
		Set<Role> retained = new LinkedHashSet<>(Arrays.asList(roles));
		retained.remove(Role.TEST_PLATFORM);
		Engine.authenticatedSessionManager.addAuthenticatedSession(httpSession, retained.toArray(new Role[0]));
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		HttpSession httpSession = request.getSession(false);
		
		boolean anonymousDashboard = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANONYMOUS_DASHBOARD);
		if (anonymousDashboard) {
			if (httpSession == null) {
				httpSession = request.getSession(true);
			}
			Role[] roles = Engine.authenticatedSessionManager.getRoles(httpSession);
			if (!Engine.authenticatedSessionManager.hasRole(httpSession, Role.TEST_PLATFORM)) {
				setPublicDashboardAccess(httpSession, roles);
			}
		} else if (
			httpSession != null &&
			Boolean.TRUE.equals(httpSession.getAttribute(SessionKey.ADMIN_PUBLIC_DASHBOARD.toString()))
		) {
			clearPublicDashboardAccess(httpSession, Engine.authenticatedSessionManager.getRoles(httpSession));
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
			ServiceUtils.addMessage(document, document.getDocumentElement(), "" + System.currentTimeMillis(), "ts", false);
			ServiceUtils.addMessage(document, document.getDocumentElement(), timezone, "tz", false);
		} else {
			Engine.logAdmin.info("Check authentication failed (no role defined)");
			ServiceUtils.addMessage(document, document.getDocumentElement(), "false", "authenticated", false);
		}
	}
}
