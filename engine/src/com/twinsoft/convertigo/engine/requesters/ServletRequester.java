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

package com.twinsoft.convertigo.engine.requesters;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.translators.DefaultServletTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;

public abstract class ServletRequester extends GenericRequester {
	
//	public static final String REPARSED_PARAMETERS_ATTRIBUTE = "com.twinsoft.convertigo.engine.requesters.ServletRequester.reparsedParameters";
	
    public ServletRequester() {
    }
    
    public String getName() {
        return "ServletRequester";
    }
    
    @Override
	public void checkSecuredConnection() throws EngineException {
		if (context.requestedObject.isSecureConnectionRequired()) {
			if (!context.httpServletRequest.isSecure()) {
				throw new EngineException("Unable to execute the requestable '" + context.requestedObject.getName()
						+ "' because a secured connection is needed");
			}
		}
	}

    @Override
	public void checkAccessibility() throws EngineException {
		// By default, requesters disallow private requestables from being executed
		if (context.requestedObject.isPrivateAccessibility()) {
			if (Engine.isStudioMode()) {
				// In studio mode, all requestables can be executed
				return;
			}
			
			if (Engine.authenticatedSessionManager.hasRole(context.httpServletRequest.getSession(), Role.TEST_PLATFORM_PRIVATE)) {
				// Only admin users can execute private requestables
				return;
			}
			
			throw new EngineException("Unable to execute the requestable '" + context.requestedObject.getName()
					+ "' because it is private (check its accessibility property)");
		}
	}

	private String subPath = null;

	protected void initInternalVariables() throws EngineException {
		HttpServletRequest request = (HttpServletRequest) inputData;
		String requestURI = request.getRequestURI(); 
		Engine.logContext.debug("(ServletRequester) requested URI: " + requestURI);
		
		int projectNameStartIndex = requestURI.indexOf("/system/projects/");
		if (projectNameStartIndex == -1) {
			projectNameStartIndex = requestURI.indexOf("/projects/");
			if (projectNameStartIndex == -1) {
				throw new EngineException("not a projects nor a system url");
			}
			projectNameStartIndex += 10;
		} else {
			RequestAttribute.system.set(request, true);
			projectNameStartIndex += 17;
		}
		
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
		
		initInternalVariables();
		
		if ("true".equals(RequestAttribute.system.string(request))) {
			return new Context("system");
		}
		
		String contextName = getContextName();

		Engine.logContext.debug("(ServletRequester) requested execution context: " + contextName);
		
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

	@Override
	public void initContext(Context context) throws Exception {
		HttpServletRequest request = (HttpServletRequest) inputData;
		context.setRequest(request);
		
		super.initContext(context);

		context.subPath = subPath;

		boolean bConnectorGivenByUser = false;
		
		Engine.logContext.debug("(Servlet requester.initContext) Query string: " + request.getQueryString());
		
		// We transform the HTTP post data into XML data.
		Enumeration<?> parameterNames = request.getParameterNames();
		
		String parameterName, parameterValue;
		String[] parameterValues = null;
		
		while (parameterNames.hasMoreElements()) {
			parameterName = (String) parameterNames.nextElement();
			
			parameterValues = request.getParameterValues(parameterName);
			
			parameterValue = parameterValues[0];
			
			handleParameter(context, parameterName, parameterValue);
			
			if (parameterName.equals(Parameter.Connector.getName())) {
				bConnectorGivenByUser = true;
			}
		}
		
		TestCase tc = TestCase.getTestCase(request, context.projectName);
		if (tc != null) {
			for (TestCaseVariable var: tc.getVariables()) {
				parameterName = var.getName();
				
				if (request.getParameter(parameterName) == null) {
					Object value = var.getValueOrNull();
					if (value == null || (var.isMultiValued() && ((XMLVector<?>) value).isEmpty())) {
						continue;
					}
					parameterValue = (String) (var.isMultiValued() ? ((XMLVector<?>) value).get(0) : value);
					
					handleParameter(context, parameterName, parameterValue);
					
					if (parameterName.equals(Parameter.Connector.getName())) {
						bConnectorGivenByUser = true;
					}
				}
			}
		}
		
		if (!bConnectorGivenByUser) {
			if (context.project != null && context.project.getName().equals(context.projectName)) {
				String defaultConnectorName = context.project.getDefaultConnector().getName();
				if (!defaultConnectorName.equals(context.connectorName)) {
					context.connectorName = defaultConnectorName;
				}
			}
		}

		Engine.logContext.debug("Context initialized!");
	}

	public Translator getTranslator() {
		return new DefaultServletTranslator();
	}

	public void preGetDocument() throws EngineException {
		HttpServletRequest request = (HttpServletRequest) inputData;
		
		HttpUtils.checkCV(request);

		String remoteAddr = request.getRemoteAddr();
		Engine.logContext.info("Remote-Addr: \"" + remoteAddr + "\"");
		context.remoteAddr = remoteAddr;
		Log4jHelper.mdcPut(mdcKeys.ClientIP, remoteAddr);

		String remoteHost = "-";
		context.remoteHost = remoteHost;
		if (EnginePropertiesManager.getProperty(PropertyName.NET_REVERSE_DNS).equalsIgnoreCase("true")) {
			remoteHost = request.getRemoteHost();
			if (remoteHost != null) {
				context.remoteHost = remoteHost;
				Log4jHelper.mdcPut(mdcKeys.ClientHostName, context.remoteHost);
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
		if (Engine.logContext.isInfoEnabled()) {
			Engine.logContext.info("Query string: " + Visibility.maskQueryValues(queryString));
		}

		String userAgent = request.getHeader(HeaderName.UserAgent.value());
		Engine.logContext.info("User-Agent: \"" + userAgent + "\"");
		context.userAgent = userAgent;

		try {
			HttpSessionListener.checkSession(request);
		} catch (Throwable e) {
			throw new EngineException(e.getMessage(), e);
		}
	}
}
