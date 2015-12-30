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

package com.twinsoft.convertigo.engine.admin.services.connections;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.tas.KeyManager;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN },
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
		return s;
	}
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		
		Element rootElement = document.getDocumentElement();
        
        Element connectionsListElement = document.createElement("connections");
        rootElement.appendChild(connectionsListElement);
        
        Element contextsInUseElement = document.createElement("contextsInUse");
        contextsInUseElement.setTextContent("" + Engine.theApp.contextManager.getNumberOfContexts());
        rootElement.appendChild(contextsInUseElement);
        
        Element contextsNumberElement = document.createElement("contextsNumber");
        contextsNumberElement.setTextContent("" + EnginePropertiesManager.getProperty(PropertyName.CONVERTIGO_MAX_CONTEXTS));
        rootElement.appendChild(contextsNumberElement);
        
        Element sessionsInUseElement = document.createElement("sessionsInUse");
        sessionsInUseElement.setTextContent("" + (KeyManager.getMaxCV(Session.EmulIDSE) - KeyManager.getCV(Session.EmulIDSE)));
        rootElement.appendChild(sessionsInUseElement);
        
        Element sessionsIsOverflowElement = document.createElement("sessionsIsOverflow");
        sessionsIsOverflowElement.setTextContent(KeyManager.isOverflow(Session.EmulIDSE) ? "true" : "false");
        rootElement.appendChild(sessionsIsOverflowElement);
        
        Element sessionsNumberElement = document.createElement("sessionsNumber");
        sessionsNumberElement.setTextContent("" + KeyManager.getMaxCV(Session.EmulIDSE));
        rootElement.appendChild(sessionsNumberElement);
        
        Element threadsInUseElement = document.createElement("threadsInUse");
        threadsInUseElement.setTextContent("" + com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads);
        rootElement.appendChild(threadsInUseElement);
        
        Element threadsNumberElement = document.createElement("threadsNumber");
        threadsNumberElement.setTextContent(EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_MAX_WORKER_THREADS));
        rootElement.appendChild(threadsNumberElement);
        
        Element httpTimeoutElement = document.createElement("httpTimeout");
        httpTimeoutElement.setTextContent(formatTime(request.getSession().getMaxInactiveInterval()));
        rootElement.appendChild(httpTimeoutElement);
        
        String order = request.getParameter("sortParam");
        int orderInt=-1;
        ArrayList<Element> list=null;
        String[] attributes={"connected","contextName", "project","connector","requested","status","user","contextCreationDate","lastContextAccessDate"};
        
        if(order!=null){
        	list=new ArrayList<Element>(); 
        	for(int i=0;i<attributes.length;i++){
        		if(attributes[i].equals(order))
        			orderInt=i;    
        	}
        	if(orderInt==-1){
        		order=null;
        	}
        }
       
      
        
        
        for(Context context : Engine.theApp.contextManager.getContexts()) {
        	int i = context.contextID.indexOf('_');
        	String sessionID = context.contextID.substring(0, i);
        	String contextName = context.contextID.substring(i + 1);
        	com.twinsoft.api.Session apiSession = Engine.theApp.sessionManager.getSession(context.contextID);
    		boolean bConnected = ((apiSession != null) && apiSession.isConnected());
            Element connectionElement = document.createElement("connection");
            connectionElement.setAttribute(attributes[0], Boolean.toString(bConnected));
			connectionElement.setAttribute(attributes[1], sessionID + "_" + contextName);
            connectionElement.setAttribute(attributes[2], context.projectName);
            connectionElement.setAttribute(attributes[3], context.connectorName);
            connectionElement.setAttribute(attributes[4], (context.requestedObject instanceof Transaction) ? context.transactionName:context.sequenceName);
            connectionElement.setAttribute(attributes[5], (context.requestedObject == null || context.requestedObject.runningThread == null ? "finished" : (context.requestedObject.runningThread.bContinue ? "in progress" : "finished")) + "("+ context.waitingRequests+")");
            connectionElement.setAttribute(attributes[6], context.getAuthenticatedUser()==null ? "" : context.getAuthenticatedUser());
            connectionElement.setAttribute(attributes[7], DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.creationTime)));
            connectionElement.setAttribute(attributes[8], DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.lastAccessTime)));
            try {
				connectionElement.setAttribute("contextInactivityTime", formatTime((System.currentTimeMillis() - context.lastAccessTime) / 1000)+" / "+formatTime(Engine.theApp.databaseObjectsManager.getProjectByName(context.projectName).getHttpSessionTimeout()));
			} catch (Exception e) {
				// TODO: document = DOMUtils.handleError(e); << USELESS 
			}
			connectionElement.setAttribute("clientComputer", context.remoteHost + " (" + context.remoteAddr + "), " + context.userAgent);
            if(order==null)
            	connectionsListElement.appendChild(connectionElement);
            else{
            	int j=0;   
            	//increment j until the good index
            	for(;j<list.size()&&connectionElement.getAttribute(attributes[orderInt]).toLowerCase().compareTo(list.get(j).getAttribute(attributes[orderInt]).toLowerCase())>0;j++){           		
            		
            	}            	
            	list.add(j,connectionElement);
            }
        }
        if(order!=null){
        	if(request.getParameter("desc")==null){
		        for(int k=0;k<list.size();k++){
		        	connectionsListElement.appendChild(list.get(k));
		        }
        	}
        	else{
        		for(int k=list.size()-1;k>=0;k--){
		        	connectionsListElement.appendChild(list.get(k));
		        }
        	}
        }
        
	}
}
