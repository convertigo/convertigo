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
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;

@ServiceDefinition(
		name = "Delete",
		roles = { Role.WEB_ADMIN, Role.CONNECTIONS_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Delete extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String contextName, sessionId;
        if ((contextName = request.getParameter("contextName")) != null) {
        	Engine.theApp.contextManager.remove(contextName);
    		ServiceUtils.addMessage(document, "Context '" + contextName + "' removed", "success");
        } else if ((sessionId = request.getParameter("sessionId")) != null) {
        	HttpSessionListener.terminateSession(sessionId);
    		ServiceUtils.addMessage(document, "Session '" + sessionId + "' removed", "success");
        } else if ("true".equals(request.getParameter("removeAll"))) {
        	HttpSessionListener.removeAllSession();
        	Engine.theApp.contextManager.removeAll();
    		ServiceUtils.addMessage(document, "All contexts and sessions removed", "success");
        } else {
        	throw new IllegalArgumentException();
        }
	}

}
