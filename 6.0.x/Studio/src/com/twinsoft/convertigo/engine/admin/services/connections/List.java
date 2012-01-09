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
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;

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
        Text connectedUsersTextNode = document.createTextNode(Integer.toString(Engine.theApp.contextManager.getNumberOfContexts()));
        contextsInUseElement.appendChild(connectedUsersTextNode);
        rootElement.appendChild(contextsInUseElement);
        
        Element threadsInUseElement = document.createElement("threadsInUse");
        Text threadsInUseTextNode = document.createTextNode("" + com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads);
        threadsInUseElement.appendChild(threadsInUseTextNode);
        rootElement.appendChild(threadsInUseElement);
        
        Element threadsNumberElement = document.createElement("threadsNumber");
        Text threadsNumberTextNode = document.createTextNode(EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_MAX_WORKER_THREADS));
        threadsNumberElement.appendChild(threadsNumberTextNode);
        rootElement.appendChild(threadsNumberElement);
        
        Element httpTimeoutElement = document.createElement("httpTimeout");
        Text httpTimeoutTextNode = document.createTextNode(formatTime(request.getSession().getMaxInactiveInterval()));
        httpTimeoutElement.appendChild(httpTimeoutTextNode);
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
            connectionElement.setAttribute(attributes[6], context.tasVirtualServerName + "/" + context.tasUserName);
            connectionElement.setAttribute(attributes[7], DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.creationTime)));
            connectionElement.setAttribute(attributes[8], DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.lastAccessTime)));
            try {
				connectionElement.setAttribute("contextInactivityTime", formatTime((System.currentTimeMillis() - context.lastAccessTime) / 1000)+" / "+formatTime(Engine.theApp.databaseObjectsManager.getProjectByName0(context.projectName).getHttpSessionTimeout()));
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
