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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SimpleTextTranslator implements Translator {

	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logBeans.debug("Making input document");

		String queryString = (String) inputData;
		
		Element root = context.inputDocument.createElement("input");
		Element transactionVariablesElement = context.inputDocument.createElement("transaction-variables");

		context.inputDocument.appendChild(root);
		root.appendChild(transactionVariablesElement);
        
		context.connectorName = null;
		context.transactionName = null;
		
		// We transform the HTTP post data into XML data.
		StringTokenizer st1 = new StringTokenizer(queryString, "&");
		StringTokenizer st2;
		String token;
		String parameterName, parameterValue;
		Element item;
		while (st1.hasMoreTokens()) {
			token = st1.nextToken();
			try {
				st2 = new StringTokenizer(token, "=");
				parameterName = st2.nextToken();
				parameterValue = st2.nextToken();

				// This is the overidden transaction / connector
				if (parameterName.equals(Parameter.Connector.getName())) {
					if ((parameterValue != null) && (!parameterValue.equals(""))) {
						context.connectorName = parameterValue;
						Engine.logBeans.debug("The connector is overridden to \"" + context.connectorName + "\".");
					}
				}
				else if (parameterName.equals(Parameter.Transaction.getName())) {
					if ((parameterValue != null) && (!parameterValue.equals(""))) {
						context.transactionName = parameterValue;
						Engine.logBeans.debug("The transaction is overridden to \"" + context.transactionName + "\".");
					}
				}
				else if (parameterName.equals(Parameter.Xslt.getName())) {
					if ((parameterValue != null) && (!parameterValue.equals(""))) {
						context.isXsltRequest = parameterValue.equalsIgnoreCase("true");
						Engine.logBeans.debug("XSLT request: " + context.isXsltRequest);
					}
				}
				else if (parameterName.equals(Parameter.JavelinSignature.getName())) {
					if ((parameterValue != null) && (!parameterValue.equals(""))) {
						item = context.inputDocument.createElement("document-signature");
						Text valueNode = context.inputDocument.createTextNode(parameterValue);
						root.appendChild(valueNode);
						Engine.logBeans.debug("Document signature: " + parameterValue);
					}
				}
				// This is the overidden service code
				else if (parameterName.equals(Parameter.CariocaService.getName())) {
					if ((context.tasServiceCode == null) || (!context.tasServiceCode.equalsIgnoreCase(parameterValue))) {
				        Engine.logBeans.debug("Service code differs from previous one; requiring new session");
						context.isNewSession = true;
						context.tasServiceCode = parameterValue;
						Engine.logBeans.debug("The service code is overidden to \"" + parameterValue + "\".");
					}
				}
				// This is the key given by a Carioca request
				else if (parameterName.equals(Parameter.CariocaSesskey.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.tasSessionKey = parameterValue;
						Engine.logBeans.debug("The Carioca key is \"" + parameterValue + "\".");
					}
				}
				// Carioca trusted request
				else if (parameterName.equals(Parameter.Carioca.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.isTrustedRequest = (parameterValue.equalsIgnoreCase("true") ? true : false);
						Engine.logBeans.debug("Is Carioca trusted request: " + parameterValue);
					}
				}
				// This is the Carioca user name
				else if (parameterName.equals(Parameter.CariocaUser.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.tasUserName = parameterValue;
						Engine.logBeans.debug("The Carioca user name is \"" + parameterValue + "\".");
					}
				}
				// This is the Carioca user password
				else if (parameterName.equals(Parameter.CariocaPassword.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.tasUserPassword = parameterValue;
						Engine.logBeans.debug("The Carioca user password is \"" + parameterValue + "\".");
					}
				}
				// VIC trusted request
				else if (parameterName.equals(Parameter.Vic.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.isTrustedRequest = (parameterValue.equalsIgnoreCase("true") ? true : false);
						Engine.logBeans.debug("Is VIC trusted request: " + parameterValue);
					}
				}
				// This is the VIC user name
				else if (parameterName.equals(Parameter.VicUser.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.tasUserName = parameterValue;
						Engine.logBeans.debug("The VIC user name is \"" + parameterValue + "\".");
						context.isRequestFromVic = true;
					}
				}
				// This is the VIC group
				else if (parameterName.equals(Parameter.VicGroup.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						int index = parameterValue.indexOf('@');
						if (index == -1) {
							context.tasUserGroup = parameterValue;
							context.tasVirtualServerName = "";
						}
						else {
							context.tasUserGroup = parameterValue.substring(0, index);
							context.tasVirtualServerName = parameterValue.substring(index + 1);
						}
						Engine.logBeans.debug("The VIC group is \"" + context.tasUserGroup + "\".");
						Engine.logBeans.debug("The VIC virtual server is \"" + context.tasVirtualServerName + "\".");
					}
					context.isRequestFromVic = true;
				}
				// This is the VIC service code
				else if (parameterName.equals(Parameter.VicServiceCode.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						if (!parameterValue.equalsIgnoreCase(context.tasServiceCode)) {
							Engine.logBeans.debug("Vic service code differs from previous one; requiring new session");
							context.isNewSession = true;
							context.tasServiceCode = parameterValue;
							Engine.logBeans.debug("The VIC service code is \"" + parameterValue + "\".");
						}
					}
					context.isRequestFromVic = true;
				}
				// This is the VIC dte address
				else if (parameterName.equals(Parameter.VicDteAddress.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.tasDteAddress = parameterValue;
						Engine.logBeans.debug("The VIC dte address is \"" + parameterValue + "\".");
					}
					context.isRequestFromVic = true;
				}
				// This is the VIC comm device
				else if (parameterName.equals(Parameter.VicCommDevice.getName())) {
					if ((parameterValue != null) && (parameterValue.length() > 0)) {
						context.tasCommDevice = parameterValue;
						Engine.logBeans.debug("The VIC comm device is \"" + parameterValue + "\".");
					}
					context.isRequestFromVic = true;
				}
				// No cache option
				else if (parameterName.startsWith(Parameter.NoCache.getName())) {
					context.noCache = (parameterValue.equalsIgnoreCase("true") ? true : false);
					Engine.logBeans.debug("Ignoring cache required: " + parameterValue);
				}
				// Transaction/Sequence test case
				else if (parameterName.indexOf(Parameter.Testcase.getName()) == 0) {
					item = context.inputDocument.createElement("variable");
					item.setAttribute("name", parameterName);
					item.setAttribute("value", parameterValue);
					Engine.logContext.info("Input test case = '" + parameterValue + "'");
					transactionVariablesElement.appendChild(item);
				}
				// This is an internal parameter
				else if (parameterName.startsWith("__")) {
					// Probably handled somewhere else; ignoring it
					continue;
				}
				// This is a variable
				else {
					item = context.inputDocument.createElement("variable");
					item.setAttribute("name", parameterName);
					item.setAttribute("value", parameterValue);
					Engine.logBeans.debug("Added requestable variable '" + parameterName + "' = '" + Visibility.maskValue(parameterValue) +"'");
					transactionVariablesElement.appendChild(item);
				}
			}
			catch(NoSuchElementException e) {
				// Ignore (wrong token)
			}
		}

		Engine.logBeans.debug("Input document created");
    }

	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
		if (convertigoResponse instanceof Document) {
			String s = XMLUtils.prettyPrintDOM((Document) convertigoResponse);
			return s;
		}
		return convertigoResponse;
	}

	public String getContextName(byte[] data) throws Exception {
		String queryString = new String(data);
		String contextName = findParameterValue(queryString, Parameter.Context.getName());

		if ((contextName == null) || (contextName.length() == 0)) contextName = "default";
		else if (contextName.equals("*")) contextName = "default*";

		return contextName;
	}

	public String getProjectName(byte[] data) throws Exception {
		String queryString = new String(data);
		String projectName = findParameterValue(queryString, Parameter.Project.getName());
		return projectName;
	}
	
	private String findParameterValue(String queryString, String parameterName) {
		String parameterValue = null;
		int i = queryString.indexOf(parameterName + "=");
		if (i != -1) {
			i += (parameterName + "=").length(); // Offset of "__param="
			int j = queryString.indexOf("&", i);
			if (j == -1) parameterValue = queryString.substring(i);
			else parameterValue = queryString.substring(i, j);
		}

		return parameterValue;
	}

}
