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

package com.twinsoft.convertigo.engine.migration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class Migration001 {

    public static Element migrate(Document document, Element projectNode) throws Exception {
		Engine.logDatabaseObjectManager.debug("[Migration m001] Starting migration");

		// Analyze all HTTP connectors
		NodeList connectorList = projectNode.getElementsByTagName("connector");
		int lenConnectors = connectorList.getLength();
		for (int i = 0; i < lenConnectors; i++) {
			Element connectorElement = (Element) connectorList.item(i);
			String connectorClassName = connectorElement.getAttribute("classname");
			
			// Filter HTTP connectors
			if ("com.twinsoft.convertigo.beans.connectors.HttpConnector".equals(connectorClassName) ||
			"com.twinsoft.convertigo.beans.connectors.HtmlConnector".equals(connectorClassName)) {
				Engine.logDatabaseObjectManager.debug("[Migration m001] Found HTTP connector");
				
				NodeList transactionList = connectorElement.getElementsByTagName("transaction");
				int lenTransactions = transactionList.getLength();
				
				// Enumerate all transactions
				for (int j = 0; j < lenTransactions; j++) {
					Element transactionElement = (Element) transactionList.item(j);
					
					// Enumerate all variables
					NodeList variableList = transactionElement.getElementsByTagName("variable");
					int lenVariables = variableList.getLength();
					for (int k = 0; k < lenVariables; k++) {
						Element variableElement = (Element) variableList.item(k);
						
						Element httpNameElement = (Element) XMLUtils.findNodeByAttributeValue(
							variableElement.getElementsByTagName("property"),
							"name",
							"httpName");
						
						if (httpNameElement == null) continue;

						String httpName = ((Element) httpNameElement.getElementsByTagName("java.lang.String").item(0)).getAttribute("value");

						// Skip variables without httpName or with empty httpName property
						if (httpName.equals("")) continue;
						
						Element httpMethodElement = (Element) XMLUtils.findNodeByAttributeValue(
							variableElement.getElementsByTagName("property"),
							"name",
							"httpMethod");
						
						// Skip variables that do not have an httpMethod property (testcase variables)
						if (httpMethodElement == null) continue;
						
						String httpMethod = ((Element) httpMethodElement.getElementsByTagName("java.lang.String").item(0)).getAttribute("value");
						
						// If at least one variable has HTTP method set to POST, set the underlying
						// HTTP transaction HTTP verb to POST.
						if ("POST".equals(httpMethod)) {
							Engine.logDatabaseObjectManager.debug("[Migration m001] Found POST variable");

							Element httpVerbPropertyElement = document.createElement("property");
							transactionElement.appendChild(httpVerbPropertyElement);
							httpVerbPropertyElement.setAttribute("name", "httpVerb");
							Element httpVerbPropertyValueElement = document.createElement("java.lang.Integer");
							httpVerbPropertyElement.appendChild(httpVerbPropertyValueElement);
							httpVerbPropertyValueElement.setAttribute("value", "" + HttpTransaction.HTTP_VERB_POST);
							
							Engine.logDatabaseObjectManager.info("[Migration m001] Found at least one POST variable => parent transaction's HTTP verb property set to POST");
							break;
						}
					}
				}
			}
		}
    	
    	return projectNode;
    }

}
