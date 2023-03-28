/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.tas.KeyManager;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN, Role.CONNECTIONS_CONFIG, Role.CONNECTIONS_VIEW },
		parameters = {},
		returnValue = ""
	)
public class List extends XmlService{

	/**
	 * <admin service="Connections">
	 * 		<contextsInUse>1</contextsInUse>
	 * 		<threadsInUse>0</threadsInUse>
	 * 		<threadsNumber>100</threadsNumber>
	 * 		<connections>
	 * 			<connection connected="true" contextName="" project="" connector="" requested="" status="" user="" contextCreationDate="" lastContextAccessDate="" contextInactivityTime="" clientComputer=""/>
	 * 		</connections>
	 * </admin>
	 */
	
	public String formatTime(long time) {
		String s = "";
		long seconds = time % 60;
		long minutes = (time / 60) % 60;
		long hours = (time / 3600) % 24;
		long days = time / 86400; // 86400 = 3600 x 24
		if (days != 0) s += days + "d";
		if (hours != 0) s += hours + "h";
		if (minutes != 0) s += minutes + "min";
		if (seconds != 0) s += seconds + "s";
		return s.isEmpty() ? "0" : s;
	}
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		HttpSession currentSession = request.getSession();
		
		Element rootElement = document.getDocumentElement();
        
        Element connectionsListElement = document.createElement("connections");
        rootElement.appendChild(connectionsListElement);
        
        Element sessionsListElement = document.createElement("sessions");
        rootElement.appendChild(sessionsListElement);
        
        Element contextsInUseElement = document.createElement("contextsInUse");
        contextsInUseElement.setTextContent("" + Math.max(0, Engine.theApp.contextManager.getNumberOfContexts()));
        rootElement.appendChild(contextsInUseElement);
        
        Element contextsNumberElement = document.createElement("contextsNumber");
        contextsNumberElement.setTextContent("" + EnginePropertiesManager.getProperty(PropertyName.CONVERTIGO_MAX_CONTEXTS));
        rootElement.appendChild(contextsNumberElement);
        
        Element sessionsInUseElement = document.createElement("sessionsInUse");
        sessionsInUseElement.setTextContent("" + HttpSessionListener.countSessions());
        rootElement.appendChild(sessionsInUseElement);
        
        Element sessionsIsOverflowElement = document.createElement("sessionsIsOverflow");
        sessionsIsOverflowElement.setTextContent(KeyManager.isOverflow(Session.EmulIDSE) ? "true" : "false");
        rootElement.appendChild(sessionsIsOverflowElement);
        
        Element sessionsNumberElement = document.createElement("sessionsNumber");
        sessionsNumberElement.setTextContent("" + Math.max(0, KeyManager.getMaxCV(Session.EmulIDSE)));
        rootElement.appendChild(sessionsNumberElement);
        
        Element threadsInUseElement = document.createElement("threadsInUse");
        threadsInUseElement.setTextContent("" + Math.max(0, com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads));
        rootElement.appendChild(threadsInUseElement);
        
        Element threadsNumberElement = document.createElement("threadsNumber");
        threadsNumberElement.setTextContent(EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_MAX_WORKER_THREADS));
        rootElement.appendChild(threadsNumberElement);
        
        Element httpTimeoutElement = document.createElement("httpTimeout");
        httpTimeoutElement.setTextContent(formatTime(currentSession.getMaxInactiveInterval()));
        rootElement.appendChild(httpTimeoutElement);
        
        long now = System.currentTimeMillis();
        
        Collection<Context> contexts = null;
        
        String sessionToFilter = request.getParameter("session");
        
        if (StringUtils.isNotBlank(sessionToFilter)) {
            HttpSession session = HttpSessionListener.getHttpSession(sessionToFilter);
            if (session != null) {
            	contexts = Engine.theApp.contextManager.getContexts(session);
            }
        }
        if (contexts == null) {
        	contexts = Engine.theApp.contextManager.getContexts();
        }
        
        for (Context context : contexts) {
        	String authenticatedUser = null;
        	try {
        		authenticatedUser = context.getAuthenticatedUser();
        	} catch (Exception e) {
				Engine.logAdmin.trace("connection.List failed to get the authenticated user: " + e);
			}
        	
        	com.twinsoft.api.Session apiSession = Engine.theApp.sessionManager.getSession(context.contextID);
    		boolean bConnected = ((apiSession != null) && apiSession.isConnected());
            Element connectionElement = document.createElement("connection");
            connectionElement.setAttribute("connected", Boolean.toString(bConnected));
			connectionElement.setAttribute("contextName", context.contextID);
            connectionElement.setAttribute("project", context.projectName);
            connectionElement.setAttribute("connector", context.connectorName);
            connectionElement.setAttribute("requested", (context.requestedObject instanceof Transaction) ? context.transactionName:context.sequenceName);
            connectionElement.setAttribute("status", (context.requestedObject == null || context.requestedObject.runningThread == null ? "finished" : (context.requestedObject.runningThread.bContinue ? "in progress" : "finished")) + "("+ context.waitingRequests+")");
            connectionElement.setAttribute("user", authenticatedUser == null ? "" : authenticatedUser);
            connectionElement.setAttribute("contextCreationDate", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.creationTime)));
            connectionElement.setAttribute("lastContextAccessDate", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.lastAccessTime)));
            try {
				connectionElement.setAttribute("contextInactivityTime", formatTime((now - context.lastAccessTime) / 1000)+" / "+formatTime(Engine.theApp.databaseObjectsManager.getOriginalProjectByName(context.projectName).getContextTimeout()));
			} catch (Exception e) {
				// TODO: document = DOMUtils.handleError(e); << USELESS 
			}
			connectionElement.setAttribute("clientComputer", context.remoteHost + " (" + context.remoteAddr + "), " + context.userAgent);
            connectionsListElement.appendChild(connectionElement);            
        }
        
        if (!"false".equals(request.getParameter("sessions"))) {
	        for (HttpSession session: HttpSessionListener.getSessions()) {
	        	Element sessionElement = document.createElement("session");
	        	java.util.List<Context> ctxs = Engine.theApp.contextManager.getContexts(session);
	        	sessionElement.setAttribute("sessionID", session.getId());
	        	sessionElement.setAttribute("authenticatedUser", SessionAttribute.authenticatedUser.string(session));
	        	sessionElement.setAttribute("contexts", Integer.toString(ctxs == null ? 0 : ctxs.size()));
	        	sessionElement.setAttribute("clientIP", SessionAttribute.clientIP.string(session));
	        	sessionElement.setAttribute("deviceUUID", SessionAttribute.deviceUUID.string(session));
	        	sessionElement.setAttribute("lastSessionAccessDate", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(session.getLastAccessedTime())));
	        	sessionElement.setAttribute("sessionInactivityTime", formatTime((now - session.getLastAccessedTime()) / 1000) + " / " + formatTime(session.getMaxInactiveInterval()));
	        	Role[] r = (Role[]) session.getAttribute(SessionKey.ADMIN_ROLES.toString());
	        	sessionElement.setAttribute("adminRoles", Integer.toString(r == null ? 0 : r.length));
	        	if (session == currentSession) {
	        		sessionElement.setAttribute("isCurrentSession", "true");
	        	}
	        	Set<HttpServletRequest> set = SessionAttribute.fullSyncRequests.get(session);
	        	sessionElement.setAttribute("isFullSyncActive", Boolean.toString(set != null && !set.isEmpty()));
	        	sessionsListElement.appendChild(sessionElement);
	        }
        }
	}
}
