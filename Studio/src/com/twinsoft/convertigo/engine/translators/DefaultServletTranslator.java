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

package com.twinsoft.convertigo.engine.translators;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;

public class DefaultServletTranslator implements Translator {

	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logContext.debug("Making input document");

		HttpServletRequest request = (HttpServletRequest) inputData;
		
		Element root = context.inputDocument.createElement("input");
		Element transactionVariablesElement = context.inputDocument.createElement("transaction-variables");
		Element transactionHeadersElement = context.inputDocument.createElement("transaction-headers");
		Element javelinActionElement = context.inputDocument.createElement("javelin-action");
		Element webviewerActionElement = context.inputDocument.createElement("webviewer-action");

		context.inputDocument.appendChild(root);
		root.appendChild(transactionVariablesElement);
		root.appendChild(transactionHeadersElement);
		root.appendChild(javelinActionElement);
		root.appendChild(webviewerActionElement);
        
		// We transform the HTTP post data into XML data.
		Enumeration<?> parameterNames = request.getParameterNames();
		
		Element parentItem, item;
		String parameterName, parameterValue;
		String headerName, headerValue;
		String[] parameterValues = null;

		// Sometimes, because of a bug about bad recycling of request facade objects in Tomcat,
		// the HTTP parameters parsing is not realized, and then parameterNames is empty. In
		// such a case, a (bad) workaround is to decode by ourselves the query string in GET
		// or the HTTP body in POST...
//		Hashtable<String, String[]> reparsedParameters = (Hashtable<String, String[]>) request.getAttribute(ServletRequester.REPARSED_PARAMETERS_ATTRIBUTE);
//		boolean bReparsedParameters = (reparsedParameters != null); 
//		if (bReparsedParameters) {
//			parameterNames = reparsedParameters.keys();
//		}
		
		while (parameterNames.hasMoreElements()) {
			parameterName = (String) parameterNames.nextElement();
			
//			if (bReparsedParameters) {
//				parameterValues = reparsedParameters.get(parameterName);
//			}
//			else {
				parameterValues = request.getParameterValues(parameterName);
//			}

			parameterValue = parameterValues[0];

			parentItem = javelinActionElement;
            
			// This is a Javelin field's value.
			if (parameterName.indexOf(Parameter.JavelinField.getName()) == 0) {
				item = context.inputDocument.createElement("field");
				item.setAttribute("name", parameterName);
				item.setAttribute("value", parameterValue);
				Engine.logContext.info("Javelin field: '" + parameterName + "' = '" + parameterValue + "'");
			}
			// This is the document signature
			else if (parameterName.equals(Parameter.JavelinSignature.getName())) {
				item = context.inputDocument.createElement("document-signature");
				item.setAttribute("value", parameterValue);
				Engine.logContext.info("The document signature is \"" + parameterValue + "\".");
			}
			// This is the Javelin action.
			else if (parameterName.equals(Parameter.JavelinAction.getName())) {
				item = context.inputDocument.createElement("action");
				item.setAttribute("name", parameterValue);
				Engine.logContext.info("Javelin action: '" + parameterValue + "'");
			}
			// This is the Javelin current field
			else if (parameterName.equals(Parameter.JavelinCurrentField.getName())) {
				item = context.inputDocument.createElement("current-field");
				item.setAttribute("name", parameterValue);
				Engine.logContext.info("Javelin current field: '" + parameterValue + "'");
			}
			// This is a transaction header
			else if (parameterName.indexOf(Parameter.HttpHeader.getName()) == 0) {
				parentItem = transactionHeadersElement;
				
				headerName = parameterName.substring(9);
				headerValue = parameterValue;
				
				item = context.inputDocument.createElement("header");
				item.setAttribute("name", headerName);
				item.setAttribute("value", headerValue);
				Engine.logContext.info("Header parameter: '" + headerName + "' = '" + headerValue + "'");
			}
			// This is a transaction uri
			else if (parameterName.indexOf(Parameter.HttpUri.getName()) == 0) {
				parentItem = root;
				
				item = context.inputDocument.createElement("uri");
				//item.setAttribute("name", parameterName);
				item.setAttribute("value", parameterValue);
				Engine.logContext.info("URI parameter: '" + parameterValue + "'");
			}
			// This is an HTML transaction parameter
			else if (parameterName.indexOf(Parameter.HtmlStatefull.getName()) == 0) {
				parentItem = root;
				
				item = context.inputDocument.createElement("statefull");
				//item.setAttribute("name", parameterName);
				item.setAttribute("value", parameterValue);
				Engine.logContext.info("Statefull parameter: '" + parameterValue + "'");
			}
			// This is a dynamic variable definition
			else if (parameterName.indexOf(Parameter.DynamicVariablePost.getName()) == 0 || parameterName.indexOf(Parameter.DynamicVariableGet.getName()) == 0) {
				parentItem = transactionVariablesElement;
				
				// Handles multi-valued parameters
				for (int i = 0 ; i < parameterValues.length ; i++) {
					parameterValue = parameterValues[i];
					item = context.inputDocument.createElement("variable");
					item.setAttribute("name", parameterName);
					item.setAttribute("value", parameterValue);
					item.setAttribute("multi", ((parameterValues.length > 1) ? "true":"false"));
					Engine.logContext.info("Dynamically defining a requestable variable '" + parameterName + "' = '" + Visibility.maskValue(parameterValue) +"'");
					parentItem.appendChild(item);
				}
				continue;
			}
			// Action of event on WebViewer definition
			else if (parameterName.equals(Parameter.WebEventAction.getName())) {
				parentItem = webviewerActionElement;
				item = context.inputDocument.createElement("action");
				item.setAttribute("value", parameterValue);
			}
			// Event on WebViewer definition
			else if (parameterName.indexOf(Parameter.WebEvent.getName()) == 0) {
				parentItem = webviewerActionElement;
				
				item = context.inputDocument.createElement("event");
				item.setAttribute("name", parameterName.substring(Parameter.WebEvent.getName().length()));
				item.setAttribute("value", parameterValue);
			}
			// Transaction/Sequence test case
			else if (parameterName.indexOf(Parameter.Testcase.getName()) == 0) {
				parentItem = transactionVariablesElement;
				
				item = context.inputDocument.createElement("variable");
				item.setAttribute("name", parameterName);
				item.setAttribute("value", parameterValue);
				Engine.logContext.info("Input test case = '" + parameterValue + "'");
			}
			// User reference
			else if (Parameter.UserReference.getName().equals(parameterName)) {
				context.userReference = parameterValue;
				Engine.logContext.info("User reference = '" + parameterValue + "'");
				continue;
			}
			// This is an internal parameter
			else if (parameterName.startsWith("__")) {
				// Probably handled somewhere else; ignoring it
				continue;
			}
			// This is a variable, eventually multi-valued.
			else {
				parentItem = transactionVariablesElement;
				// Handles multivalued parameters
				for (int i = 0 ; i < parameterValues.length ; i++) {
					parameterValue = parameterValues[i];
					item = context.inputDocument.createElement("variable");
					item.setAttribute("name", parameterName);
					// For empty multivalued parameters do not set 'value' attribute (Sequencer only)
					if (parameterValue.equals("_empty_array_"))
						Engine.logContext.info("Input variable " + parameterName + " is an empty array");
					else {
						item.setAttribute("value", parameterValue);
						Engine.logContext.info("Input variable " + parameterName + " = '" + Visibility.maskValue(parameterValue) +"'");
					}
					parentItem.appendChild(item);
				}
				continue;
			}
			
			parentItem.appendChild(item);
		}

		Engine.logContext.debug("Input document created");
    }

	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
		if (convertigoResponse instanceof String) {
			String encodingCharSet = "UTF-8";
			if (context.requestedObject != null)
				encodingCharSet = context.requestedObject.getEncodingCharSet();
			return (((String)convertigoResponse).getBytes(encodingCharSet));
		}
		return convertigoResponse;
	}

	public String getContextName(byte[] data) throws Exception {
		throw new EngineException("The DefaultServletTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The DefaultServletTranslator translator does not support the getProjectName() method");
	}

}
