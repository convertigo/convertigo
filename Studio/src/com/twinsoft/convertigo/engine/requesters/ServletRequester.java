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

package com.twinsoft.convertigo.engine.requesters;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.translators.DefaultServletTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

public abstract class ServletRequester extends GenericRequester {
	
//	public static final String REPARSED_PARAMETERS_ATTRIBUTE = "com.twinsoft.convertigo.engine.requesters.ServletRequester.reparsedParameters";
	
    public ServletRequester() {
    }
    
    public String getName() {
        return "ServletRequester";
    }

    protected String subPath = null;
    
    protected void initInternalVariables() throws EngineException {
		HttpServletRequest request = (HttpServletRequest) inputData;
    	String requestURI = request.getRequestURI(); 
		Engine.logContext.debug("(ServletRequester) requested URI: " + requestURI);
		
		int projectNameStartIndex = requestURI.indexOf("/projects/") + 10; 
		int slashIndex = requestURI.indexOf("/", projectNameStartIndex);

		// Find the project name
		projectName = request.getParameter(Parameter.Project.getName());
		try {
			if (projectName == null) {
				projectName = requestURI.substring(projectNameStartIndex, slashIndex);
			}
			Engine.logContext.debug("(ServletRequester) project name: " + projectName);
		}
		catch(StringIndexOutOfBoundsException e) {
			throw new EngineException("Unable to find the project name into the provided URL (\"" + requestURI + "\").");
		}

		// Find the pool name
		int slashIndex2 = requestURI.lastIndexOf("/");
		subPath = requestURI.substring(slashIndex, slashIndex2);
		Engine.logContext.debug("(ServletRequester) sub path: " + subPath);
		try {
			poolName = requestURI.substring(slashIndex2 + 1, requestURI.lastIndexOf("."));
			Engine.logContext.debug("(ServletRequester) pool name: " + poolName);
		}
		catch(StringIndexOutOfBoundsException e) {
			// Silently ignore
		}

		// Find the sequence name
		sequenceName = request.getParameter(Parameter.Sequence.getName());
		Engine.logContext.debug("(ServletRequester) sequence name: " + sequenceName);
		
		// Find the connector name
		connectorName = request.getParameter(Parameter.Connector.getName());
		Engine.logContext.debug("(ServletRequester) connector name: " + connectorName);
    }
    
	public Context getContext() throws Exception {
		HttpServletRequest request = (HttpServletRequest) inputData;

		String contextName = getContextName();

		Engine.logContext.debug("(ServletRequester) requested execution context: " + contextName);

		initInternalVariables();
		
		HttpSession httpSession = request.getSession();
		String sessionID = httpSession.getId();
		Engine.logContext.debug("(ServletRequester) requested execution sessionID: " + sessionID);
		
		context = Engine.theApp.contextManager.get(this, contextName, sessionID, poolName, projectName, connectorName, sequenceName);

		return context;
	}

	public String getContextName() throws Exception {
		HttpServletRequest request = (HttpServletRequest) inputData;

		Engine.logContext.trace("(Servlet requester.getContextName) Query string: " + request.getQueryString());
		
		// Find the context name
		String contextName;
		contextName = request.getParameter(Parameter.Context.getName());
		
		if ((contextName == null) || (contextName.length() == 0)) 
			contextName = "default";
		
		else if (contextName.equals("*"))
			contextName = "default*";

		Engine.logContext.trace("(Servlet requester.getContextName) Context name: " + contextName);
		return contextName;
	}

    public void initContext(Context context) throws Exception {
    	super.initContext(context);

		HttpServletRequest request = (HttpServletRequest) inputData;
		Engine.logContext.debug("(Servlet requester.initContext) Query string: " + request.getQueryString());
		
		/* Fix: #1754 - Slower transaction execution with many session */
		// HTTP session maintain its own context list in order to
		// improve context removal on session unbound process
		HttpSession httpSession = request.getSession();
		if (httpSession != null) {
			synchronized (httpSession) {
				try {
					@SuppressWarnings("unchecked")
					ArrayList<Context> contextList = (ArrayList<Context>)httpSession.getAttribute("contexts");
					if (contextList == null)
						contextList = new ArrayList<Context>();
					if (!contextList.contains(context)) {
						contextList.add(context);
						Engine.logContext.debug("(ServletRequester) context " + context.contextID + " has been added to http session's context list");
					}
					httpSession.setAttribute("contexts", contextList);
				}
				catch (Exception e) {
				}
			}
		}

		context.setRequest(request);
		context.subPath = subPath;

		// We transform the HTTP post data into XML data.
		Enumeration<?> parameterNames = request.getParameterNames();

		String parameterName, parameterValue;
		String[] parameterValues = null;
		boolean bConnectorGivenByUser = false;

		while (parameterNames.hasMoreElements()) {
			parameterName = (String) parameterNames.nextElement();
			
			parameterValues = request.getParameterValues(parameterName);

			parameterValue = parameterValues[0];
			
			handleParameter(context, parameterName, parameterValue);
			
			if (parameterName.equals(Parameter.Connector.getName())) {
				bConnectorGivenByUser = true;
			}
		}
		
		if (!bConnectorGivenByUser) {
			if (context.project != null && context.project.getName().equals(context.projectName)) {
				String defaultConnectorName = context.project.getDefaultConnector().getName();
				if (!defaultConnectorName.equals(context.connectorName)) {
					context.isNewSession = true;
					context.connectorName = defaultConnectorName;
				}
			}
		}
		
		Engine.logContext.debug("Context initialized!");
	}

	public Translator getTranslator() {
		return new DefaultServletTranslator();
	}

	public void preGetDocument() {
		HttpServletRequest request = (HttpServletRequest) inputData;
		
		HttpSession httpSession = request.getSession();

		String remoteAddr = request.getRemoteAddr();
		Engine.logContext.info("Remote-Addr: \"" + remoteAddr + "\"");
		context.remoteAddr = remoteAddr;
		Log4jHelper.mdcPut("ClientIP", remoteAddr);

		String remoteHost = "-";
		context.remoteHost = remoteHost;
		if (EnginePropertiesManager.getProperty(PropertyName.NET_REVERSE_DNS).equalsIgnoreCase("true")) {
			remoteHost = request.getRemoteHost();
			if (remoteHost != null) {
				context.remoteHost = remoteHost;
				Log4jHelper.mdcPut("ClientHostName", context.remoteHost);
				Engine.logContext.info("Remote-Host: \"" + remoteHost + "\"");
			}
		}
		
		String contextID = context.contextID;
		Engine.logContext.debug("Context ID: " + contextID);

		String requestURI = request.getRequestURI();
		Engine.logContext.debug("Request URI: " + requestURI);

		String servletPath = request.getServletPath();
		Engine.logContext.debug("Servlet path: " + servletPath);
		context.servletPath = servletPath;

		String queryString = request.getQueryString();
		Engine.logContext.info("Query string: " + queryString);

		String userAgent = request.getHeader("User-Agent");
		Engine.logContext.info("User-Agent: \"" + userAgent + "\"");
		context.userAgent = userAgent;

		if (httpSession.getAttribute("__sessionListener") == null) {
			Engine.logContext.trace("Inserting HTTP session listener into the HTTP session");
			httpSession.setAttribute("__sessionListener", new HttpSessionListener());
		}
	}
}
