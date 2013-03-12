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

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.NativeJavaObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DefaultInternalTranslator implements Translator {

	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logContext.debug("Making input document");

		Map<String, Object> request = GenericUtils.cast(inputData);
		
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
		Element item;

		for (Entry<String, Object> entry : request.entrySet()) {
			String parameterName = entry.getKey();
			Object parameterObject = entry.getValue();
			String[] parameterValues = null;  
			String parameterValue = null;
			if (parameterObject instanceof String[]) {
				parameterValues = (String[]) parameterObject;
				if (parameterValues.length > 0) {
					parameterValue = parameterValues[0];
				}
			}
			else if (parameterObject instanceof String) {
				parameterValue = (String) parameterObject;
			}
			
			Element parentItem = javelinActionElement;
            
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
				
				String headerName = parameterName.substring(9);
				String headerValue = parameterValue;
				
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
				
				if (parameterValues != null) {				
						// Handles multivalued parameters
						for (int i = 0 ; i < parameterValues.length ; i++) {
							parameterValue = parameterValues[i];
							item = context.inputDocument.createElement("variable");
							item.setAttribute("name", parameterName);
							item.setAttribute("value", parameterValue);
							Engine.logContext.info("Added requestable variable '" + parameterName + "' = '" + Visibility.maskValue(parameterValue) +"'");
							parentItem.appendChild(item);
						}
						
						// For empty multivalued parameters do not set 'value' attribute (Sequencer only)
						if (parameterValues.length == 0) {
							item = context.inputDocument.createElement("variable");
							item.setAttribute("name", parameterName);
							Engine.logContext.info("Added requestable variable '" + parameterName + "' as an empty array");
							parentItem.appendChild(item);
						}
				} else {
					addParameterObject(context.inputDocument, parentItem, parameterName, parameterObject);
				}
				
				continue;
			}
			
			parentItem.appendChild(item);
		}

		Engine.logContext.info("Input document created");
    }
	
	private void addParameterObject(Document doc, Node parentItem, String parameterName, Object parameterObject) {
		if (parameterObject instanceof NativeJavaObject) {
			parameterObject = ((NativeJavaObject) parameterObject).unwrap();
		}
		if (parameterObject.getClass().isArray()) {
			int len = Array.getLength(parameterObject);
			for (int i = 0 ; i < len ; i++) {
				Object o = Array.get(parameterObject, i);
				if (o != null) {
					addParameterObject(doc, parentItem, parameterName, o);
				}
			}
		} else if (parameterObject instanceof Node) {
			Node node = doc.importNode((Node) parameterObject, true);
			Element item = doc.createElement("variable");
			item.setAttribute("name", parameterName);
			if (node.getNodeType() == Node.TEXT_NODE) {
				item.setAttribute("value", node.getNodeValue());
			} else {
				NodeList nl = node.getChildNodes();
				if (nl.getLength() == 1 && nl.item(0).getNodeType() == Node.TEXT_NODE) {
					item.setAttribute("value", nl.item(0).getNodeValue());
				} else {
					while (nl.getLength() > 0) {
						item.appendChild(node.removeChild(nl.item(0)));
					}
				}
			}
			parentItem.appendChild(item);
		} else if (parameterObject instanceof NodeList) {
			NodeList nl = (NodeList) parameterObject;
			for (int i = 0 ; i < nl.getLength() ; i++) {
				addParameterObject(doc, parentItem, parameterName, nl.item(i));
			}
		} else if (parameterObject instanceof XMLVector) {
			XMLVector<Object> values = GenericUtils.cast(parameterObject);
			for (Object object : values) {
				addParameterObject(doc, parentItem, parameterName, object);
			}
		} else {
			Element item = doc.createElement("variable");
			item.setAttribute("name", parameterName);
			item.setAttribute("value", parameterObject.toString());
			parentItem.appendChild(item);
		}
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