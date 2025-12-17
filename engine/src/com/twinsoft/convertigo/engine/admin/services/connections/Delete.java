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

package com.twinsoft.convertigo.engine.admin.services.connections;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;

@ServiceDefinition(
		name = "Delete",
		roles = { Role.WEB_ADMIN, Role.CONNECTIONS_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Delete extends XmlService {
	private static final long TIMEOUT_MILLIS = 0L;

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String contextName = request.getParameter("contextName");
		if (contextName != null) {
			boolean ok = Engine.theApp.contextManager.tryRemove(contextName, TIMEOUT_MILLIS);
			if (ok) {
				ServiceUtils.addMessage(document, "Context '" + contextName + "' removed", "success");
			} else if (Engine.theApp.contextManager.requestAbort(contextName)) {
				ServiceUtils.addMessage(document, "Context '" + contextName + "' abort requested", "success");
			} else {
				ServiceUtils.addMessage(document, "Context '" + contextName + "' is busy", "warning");
			}
			return;
		}

		String sessionId = request.getParameter("sessionId");
		if (sessionId != null) {
			boolean ok = ConvertigoHttpSessionManager.getInstance().tryTerminateSession(sessionId, TIMEOUT_MILLIS);
			if (ok) {
				ServiceUtils.addMessage(document, "Session '" + sessionId + "' removed", "success");
			} else if (Engine.theApp.contextManager.requestAbortAll(sessionId)) {
				ServiceUtils.addMessage(document, "Session '" + sessionId + "' abort requested", "success");
			} else {
				ServiceUtils.addMessage(document, "Session '" + sessionId + "' is busy", "warning");
			}
			return;
		}

		if ("true".equals(request.getParameter("removeAll"))) {
			Engine.theApp.contextManager.removeAll();
			int sessionsRemoved = ConvertigoHttpSessionManager.getInstance().terminateAllSessions();
			ServiceUtils.addMessage(document,
					"All contexts removed, sessions removed: " + sessionsRemoved,
					"success");
			return;
		}

		throw new IllegalArgumentException();
	}
}
