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

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.translators.DefaultInternalTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InternalRequester extends GenericRequester {
	
    public InternalRequester() {
    }
    
    public String getName() {
        return "InternalRequester";
    }

    protected String subPath = null;
    
    protected void initInternalVariables() throws EngineException {
		Map<String, String[]> request = GenericUtils.cast(inputData);

		// Find the project name
		try {
			projectName = request.get(Parameter.Project.getName())[0];
			Engine.logContext.debug("(InternalRequester) project name: " + projectName);
		} catch (NullPointerException e) {
			// Just ignore
		}

		// Find the pool name
		try {
			poolName = request.get(Parameter.Pool.getName())[0];
			Engine.logContext.debug("(InternalRequester) pool name: " + poolName);
		} catch (NullPointerException e) {
			// Just ignore
		}

		// Find the sequence name
		try {
			sequenceName = request.get(Parameter.Sequence.getName())[0];
			Engine.logContext.debug("(InternalRequester) sequence name: " + sequenceName);
		} catch (NullPointerException e) {
			// Just ignore
		}

		// Find the connector name
		try {
			connectorName = request.get(Parameter.Connector.getName())[0];
			Engine.logContext.debug("(InternalRequester) connector name: " + connectorName);
		} catch (NullPointerException e) {
			// Just ignore
		}
    }
    
	public Context getContext() throws Exception {
		Map<String, String[]> request = GenericUtils.cast(inputData);

		String contextName = getContextName();

		initInternalVariables();
		
		String sessionID = request.get(Parameter.SessionId.getName())[0];
		Engine.logContext.debug("(ServletRequester) Requested execution sessionID: " + sessionID);
		
		context = Engine.theApp.contextManager.get(this, contextName, sessionID, poolName, projectName, connectorName, sequenceName);

		return context;
	}

	public String getContextName() throws Exception {
		Map<String, String[]> request = GenericUtils.cast(inputData);

		// Find the context name
		String contextName = request.get(Parameter.Context.getName())[0];
		
		if ((contextName == null) || (contextName.length() == 0)) 
			contextName = "default";
		
		else if (contextName.equals("*"))
			contextName = "default*";

		Engine.logContext.debug("(InternalRequester) Context name: " + contextName);
		return contextName;
	}

    public void initContext(Context context) throws Exception {
    	super.initContext(context);

    	Map<String, Object> request = GenericUtils.cast(inputData);

		// We transform the HTTP post data into XML data.
		Set<String> parameterNames = request.keySet();
		boolean bConnectorGivenByUser = false;

		for (String parameterName : parameterNames) {
			String parameterValue;

			// Handle only convertigo parameters
			if (parameterName.startsWith("__")) {
				Object parameterObjectValue = request.get(parameterName);
				// Case of convertigo parameter passed as CallTr/CallSeq variable
				if (parameterObjectValue instanceof String) {
					parameterValue = (String) parameterObjectValue;
				}
				// Case of sourced parameter
				else if (parameterObjectValue instanceof NodeList) {
					NodeList parameterValueNodeList = (NodeList) parameterObjectValue;
					parameterValue = parameterValueNodeList.item(0).getNodeValue();
				}
				// Case of legacy convertigo parameter
				else if (parameterObjectValue instanceof String[]) {
					String[] parameterValues = (String[]) parameterObjectValue;
					parameterValue = parameterValues[0];
				}
				else {
					parameterValue = parameterObjectValue.toString();
				}
				
				handleParameter(context, parameterName, parameterValue);
				
				if (parameterName.equals(Parameter.Connector.getName())) {
					bConnectorGivenByUser = true;
				}
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
		return new DefaultInternalTranslator();
	}

	public void preGetDocument() {
		String contextID = context.contextID;
		Engine.logContext.debug("Context ID: " + contextID);

		context.servletPath = null;
		context.userAgent = "Convertigo engine internal requester";
		context.remoteAddr = "127.0.0.1";
		context.remoteHost = "localhost";
	}

	public void setStyleSheet(Document document) {
		// Do nothing
	}
	
	protected Object addStatisticsAsData(Object result) { 
		return EngineStatistics.addStatisticsAsXML(context, result); 
	} 
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{ 
		if (result != null) { 
                if (stats == null) stats = context.statistics.printStatistics(); 
                if (result instanceof Document) { 
                        Document document = (Document) result; 
                        Comment comment = document.createComment("\n" + stats); 
                        document.appendChild(comment); 
                } 
                else if (result instanceof byte[]) { 
                        String encodingCharSet = "UTF-8"; 
                        if (context.requestedObject != null) 
                                encodingCharSet = context.requestedObject.getEncodingCharSet(); 
                        String sResult = new String((byte[]) result, encodingCharSet); 
                        sResult += "<!--\n" + stats + "\n-->"; 
                        result = sResult.getBytes(encodingCharSet); 
                } 
        } 
        return result;
	} 
}
