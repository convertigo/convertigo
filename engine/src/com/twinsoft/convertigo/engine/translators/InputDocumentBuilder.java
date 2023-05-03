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

package com.twinsoft.convertigo.engine.translators;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.XMLUtils;

class InputDocumentBuilder {
	private Context context;
	private Element root;
	private final Element transactionHeadersElement;
	private final Element javelinActionElement;
	private final Element webviewerActionElement;
	final Element transactionVariablesElement;
	
	InputDocumentBuilder(Context context) {
		this.context = context;
		
		root = context.inputDocument.createElement("input");
		context.inputDocument.appendChild(root);

		transactionVariablesElement = context.inputDocument.createElement("transaction-variables");
		root.appendChild(transactionVariablesElement);
		
		transactionHeadersElement = context.inputDocument.createElement("transaction-headers");
		root.appendChild(transactionHeadersElement);
		
		javelinActionElement = context.inputDocument.createElement("javelin-action");
		root.appendChild(javelinActionElement);
		
		webviewerActionElement = context.inputDocument.createElement("webviewer-action");
		root.appendChild(webviewerActionElement);
	}
	
	void addVariable(String parameterName, String parameterValue) {
		addVariable(parameterName, parameterValue, false);
	}
	
	private void addVariable(String parameterName, String parameterValue, boolean isHandleComplex) {
		if (parameterName == null || parameterValue == null) {
			return;
		}
		
		Element item = context.inputDocument.createElement("variable");
		item.setAttribute("name", parameterName);
		
		// For empty multivalued parameters do not set 'value' attribute (Sequencer only)
		if (parameterValue.equals("_empty_array_")) {
			Engine.logContext.info("Input variable " + parameterName + " is an empty array");
		} else {
			if (isHandleComplex && parameterValue.startsWith("<")) {
				try {
					Document doc = XMLUtils.parseDOMFromString(parameterValue);
					Node node = context.inputDocument.importNode(doc.getDocumentElement(),true);
					item.appendChild(node);
				} catch (Exception e) {
					item.setAttribute("value", parameterValue);
					Engine.logContext.info("Unable to handle '" + parameterName + "' complex variable's value. Set it to String value");
				}
			}
			else {
				item.setAttribute("value", parameterValue);
			}
			Engine.logContext.info("Input variable " + parameterName + " = '" + Visibility.maskValue(parameterValue) +"'");
		}
		transactionVariablesElement.appendChild(item);
	}
	
	void addVariable(String parameterName, String[] parameterValues) {
		addVariable(parameterName, parameterValues, false);
	}
	
	void addVariable(String parameterName, String[] parameterValues, boolean isHandleComplex) {
		// For empty multivalued parameters do not set 'value' attribute (Sequencer only)
		if (parameterValues.length == 0) {
			Element item = context.inputDocument.createElement("variable");
			item.setAttribute("name", parameterName);
			transactionVariablesElement.appendChild(item);
			
			Engine.logContext.info("Added requestable variable '" + parameterName + "' as an empty array");
		} else {
			for (int i = 0 ; i < parameterValues.length ; i++) {
				addVariable(parameterName, parameterValues[i], isHandleComplex);
			}
		}
	}
	
	boolean handleSpecialParameter(String parameterName, Object parameterObject) {
		String[] parameterValues = null;
		
		if (parameterObject instanceof String) {
			parameterValues = new String[] { (String) parameterObject };
		} else if (parameterObject instanceof String[]) {
			parameterValues = (String[]) parameterObject;
		} else if (parameterObject instanceof NodeList) {
			NodeList nl = (NodeList) parameterObject;
			parameterValues = new String[nl.getLength()];
			for (int i = 0 ; i < parameterValues.length ; i++) {
				parameterValues[i] = nl.item(i).getTextContent();
			}
		} else {
			parameterValues = new String[0];
		}
		
		return handleSpecialParameter(parameterName, parameterValues);
	}
	
	boolean handleSpecialParameter(String parameterName, String[] parameterValues) {
		String parameterValue = parameterValues.length > 0 ? parameterValues[0] : null;
		
		// This is a Javelin field's value.
		if (parameterName.indexOf(Parameter.JavelinField.getName()) == 0) {
			Element item = context.inputDocument.createElement("field");
			item.setAttribute("name", parameterName);
			item.setAttribute("value", parameterValue);
			javelinActionElement.appendChild(item);
			
			Engine.logContext.info("Javelin field: '" + parameterName + "' = '" + parameterValue + "'");
		}
		// This is the document signature
		else if (parameterName.equals(Parameter.JavelinSignature.getName())) {
			Element item = context.inputDocument.createElement("document-signature");
			item.setAttribute("value", parameterValue);
			javelinActionElement.appendChild(item);
			
			Engine.logContext.info("The document signature is \"" + parameterValue + "\".");
		}
		// This is the Javelin action.
		else if (parameterName.equals(Parameter.JavelinAction.getName())) {
			Element item = context.inputDocument.createElement("action");
			item.setAttribute("name", parameterValue);
			javelinActionElement.appendChild(item);
			
			Engine.logContext.info("Javelin action: '" + parameterValue + "'");
		}
		// This is the Javelin current field
		else if (parameterName.equals(Parameter.JavelinCurrentField.getName())) {
			Element item = context.inputDocument.createElement("current-field");
			item.setAttribute("name", parameterValue);
			javelinActionElement.appendChild(item);
			
			Engine.logContext.info("Javelin current field: '" + parameterValue + "'");
		}
		// This is the Javelin modified fields
		else if (parameterName.equals(Parameter.JavelinModifiedFields.getName())) {
			Element item = context.inputDocument.createElement("modified-fields");
			item.setAttribute("value", parameterValue);
			javelinActionElement.appendChild(item);
			
			Engine.logContext.info("Javelin modified fields: '" + parameterValue + "'");
		}
		// This is a transaction header
		else if (parameterName.indexOf(Parameter.HttpHeader.getName()) == 0) {
			String headerName = parameterName.substring(9);
			String headerValue = parameterValue;
			
			Element item = context.inputDocument.createElement("header");
			item.setAttribute("name", headerName);
			item.setAttribute("value", headerValue);
			transactionHeadersElement.appendChild(item);
			
			Engine.logContext.info("Header parameter: '" + headerName + "' = '" + headerValue + "'");
		}
		// This is a transaction uri
		else if (parameterName.indexOf(Parameter.HttpUri.getName()) == 0) {
			Element item = context.inputDocument.createElement("uri");
			//item.setAttribute("name", parameterName);
			item.setAttribute("value", parameterValue);
			root.appendChild(item);
			
			Engine.logContext.info("URI parameter: '" + parameterValue + "'");
		}
		// This is an HTML transaction parameter
		else if (parameterName.indexOf(Parameter.HtmlStatefull.getName()) == 0) {
			Element item = context.inputDocument.createElement("statefull");
			//item.setAttribute("name", parameterName);
			item.setAttribute("value", parameterValue);
			root.appendChild(item);
			
			Engine.logContext.info("Statefull parameter: '" + parameterValue + "'");
		}
		// This is a dynamic variable definition
		else if (parameterName.indexOf(Parameter.DynamicVariablePost.getName()) == 0 || parameterName.indexOf(Parameter.DynamicVariableGet.getName()) == 0) {			
			// Handles multi-valued parameters
			for (int i = 0 ; i < parameterValues.length ; i++) {
				parameterValue = parameterValues[i];
				Element item = context.inputDocument.createElement("variable");
				item.setAttribute("name", parameterName);
				item.setAttribute("value", parameterValue);
				item.setAttribute("multi", (parameterValues.length > 1) ? "true" : "false");
				transactionVariablesElement.appendChild(item);
				
				Engine.logContext.info("Dynamically defining a requestable variable '" + parameterName + "' = '" + Visibility.maskValue(parameterValue) +"'");
			}
		}
		else if (parameterName.indexOf(Parameter.HttpDownloadFolder.getName()) == 0) {
			Element item = context.inputDocument.createElement("download-folder");
			item.setAttribute("value", parameterValue);
			root.appendChild(item);
			
			Engine.logContext.info("Download folder parameter: '" + parameterValue + "'");
		}
		else if (parameterName.indexOf(Parameter.HttpDownloadFilename.getName()) == 0) {
			Element item = context.inputDocument.createElement("download-filename");
			item.setAttribute("value", parameterValue);
			root.appendChild(item);
			
			Engine.logContext.info("Download filename parameter: '" + parameterValue + "'");
		}
		// Action of event on WebViewer definition
		else if (parameterName.equals(Parameter.WebEventAction.getName())) {
			Element item = context.inputDocument.createElement("action");
			item.setAttribute("value", parameterValue);
			webviewerActionElement.appendChild(item);
		}
		// Event on WebViewer definition
		else if (parameterName.indexOf(Parameter.WebEvent.getName()) == 0) {
			Element item = context.inputDocument.createElement("event");
			item.setAttribute("name", parameterName.substring(Parameter.WebEvent.getName().length()));
			item.setAttribute("value", parameterValue);
			webviewerActionElement.appendChild(item);
		}
		// This is an internal parameter
		else if (parameterName.startsWith("__")) {
			// The Body part for HTTP requests; handled in connector
			if (parameterName.indexOf(Parameter.HttpBody.getName()) == 0) {
				return false;
			}
			// The Body content type for HTTP requests; handled in connector
			else if (parameterName.indexOf(Parameter.HttpContentType.getName()) == 0) {
				return false;
			}
			
			// Probably handled somewhere else; ignoring it
			Engine.logContext.debug("Convertigo internal variable \"" + parameterName + "\" ignored! (not handled here)");
		}
		// This is a variable, eventually multi-valued.
		else {
			return false;
		}
		
		return true;
	}
}
