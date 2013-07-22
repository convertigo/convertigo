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

package com.twinsoft.convertigo.engine.servlets;

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.twinj.Javelin;
import com.twinsoft.twinj.dataAlteredListener;
import com.twinsoft.twinj.twinxEvent0;
import com.twinsoft.twinj.twinxEvent1;
import com.twinsoft.twinj.twinxEvent2;
import com.twinsoft.twinj.twinxEvent3;
import com.twinsoft.twinj.twinxListener;

public class JavelinServlet extends HttpServlet {

	private static final long serialVersionUID = -582234360912914054L;

	public JavelinServlet() {
    }

    public String getName() {
        return "JavelinServlet";
    }

    public String getDefaultContentType() {
    	return "application/octet-stream";
    }

    public String getServletInfo() {
        return "Twinsoft Convertigo JavelinServlet";
    }
    
    public String getDocumentExtension() {
        return ".jav";
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        doRequest(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        doRequest(request, response);
    }
    
    class JavelinEventHandler implements twinxListener, dataAlteredListener {
    	public boolean dataAltered = false;

		public synchronized void handleDataAltered(twinxEvent1 evt) {
			Engine.logEngine.debug("(JavelinServlet) Data altered => notifying semaphore: " + this);
			dataAltered = true;
			notify();
		}

		public synchronized void handleDataStable(twinxEvent3 evt) {
			Engine.logEngine.debug("(JavelinServlet) Data Stable => notifying semaphore: " + this);
			dataAltered = true;
			notify();
		}

		public void handleCommDropped(twinxEvent0 evt) {
			// TODO Auto-generated method stub
			
		}

		public void handleConnected(twinxEvent0 evt) {
			// TODO Auto-generated method stub
			
		}

		public void handleConnectToUrl(twinxEvent2 evt) {
			// TODO Auto-generated method stub
			
		}


		public void handleDblClick(twinxEvent0 evt) {
			// TODO Auto-generated method stub
			
		}

		public void handleFirstData(twinxEvent0 evt) {
			// TODO Auto-generated method stub
			
		}

		public void handleStartLearn(twinxEvent0 evt) {
			// TODO Auto-generated method stub
			
		}

		public void handleStopLearn(twinxEvent0 evt) {
			// TODO Auto-generated method stub
			
		}

		public void handleWaitAtDone(twinxEvent1 evt) {
			// TODO Auto-generated method stub
			
		}
    }
    
    protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
    	try {
    		String poolName = null;
    		String projectName = null;
    		
    		String servletPath = request.getServletPath();
    		Engine.logEngine.debug("(JavelinServlet) servlet path: " + servletPath);
    		
    		int projectNameStartIndex = servletPath.indexOf("/projects/") + 10; 
    		int slashIndex = servletPath.indexOf("/", projectNameStartIndex);

    		// Find the project name
    		try {
    			projectName = servletPath.substring(projectNameStartIndex, slashIndex);
    			Engine.logEngine.debug("(JavelinServlet) project name: " + projectName);
    		}
    		catch(StringIndexOutOfBoundsException e) {
    			throw new EngineException("Unable to find the project name into the provided URL (\"" + servletPath + "\").");
    		}

    		// Find the pool name
    		int slashIndex2 = servletPath.lastIndexOf("/");
    		String subPath = servletPath.substring(slashIndex, slashIndex2);
    		Engine.logEngine.debug("(JavelinServlet) sub path: " + subPath);
    		try {
    			poolName = servletPath.substring(slashIndex2 + 1, servletPath.lastIndexOf("."));
    			Engine.logEngine.debug("(JavelinServlet) pool name: " + poolName);
    		}
    		catch(StringIndexOutOfBoundsException e) {
    			// Silently ignore
    		}

    		// Find the context name
    		String contextName = request.getParameter(Parameter.Context.getName());
    		if ((contextName == null) || (contextName.length() == 0)) contextName = "default";
    		else if (contextName.equals("*")) contextName = "default*";

    		HttpSession httpSession = request.getSession();
    		String sessionID = httpSession.getId();
    		
    		String sequenceName = request.getParameter(Parameter.Sequence.getName());
    		String connectorName = request.getParameter(Parameter.Connector.getName());
    		
    		Context context = Engine.theApp.contextManager.get(null, contextName, sessionID, poolName, projectName, connectorName, sequenceName);

    		Engine.logEngine.debug("(JavelinServlet) Found context: " + context.contextID);
    		
    		Javelin javelin = ((JavelinConnector) context.getConnector()).javelin;
    		
    		JavelinEventHandler semaphore = (JavelinEventHandler) context.get("convertigo.javelin_servlet.semaphore");
    		
    		if (semaphore != null) {
    			synchronized (semaphore) {
        			semaphore.notifyAll();
				}
    		}
    		
    		semaphore = new JavelinEventHandler();
    		context.set("convertigo.javelin_servlet.semaphore", semaphore);
    		
    		javelin.addDataAlteredListener(semaphore);
    		javelin.addtwinxListener(semaphore);
    		
    		try {
        		PrintWriter writer = response.getWriter();

        		Engine.logEngine.debug("(JavelinServlet) semaphore=" + semaphore);
        		synchronized (semaphore) {
        			semaphore.dataAltered = false;
            		try {
        				semaphore.wait(30000);
        			} catch (InterruptedException e) {
        	    		writeXml(writer, "interrupted");
        	    		Engine.logEngine.debug("(JavelinServlet) Interrupted");
        	    		return;
        			}
    			}

        		if (semaphore.dataAltered) {
            		writeXml(writer, "refresh");
            		Engine.logEngine.debug("(JavelinServlet) Refresh required");
        		}
        		else {
            		writeXml(writer, "timeout");
            		Engine.logEngine.debug("(JavelinServlet) Timeout occured");
        		}
    		}
    		finally {
    			javelin.removeDataAlteredListener(semaphore);
    			javelin.removetwinxListener(semaphore);
    		}
    	}
    	catch(Exception e) {
    		Engine.logEngine.error("(JavelinServlet) Unable to process the request!", e);
    		throw new ServletException(e);
    	}
    }
    
    private void writeXml(PrintWriter writer, String event) {
    	writer.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
    	writer.println("<event>" + event + "</event>");
    }

}
