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

package com.twinsoft.convertigo.beans.statements;

import javax.xml.transform.TransformerException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

public class GetNodesStatement extends XpathableStatement {

	private static final long serialVersionUID = 5555147220832481093L;

	protected String variableName = "nodeList";
	
	public GetNodesStatement() {
		super();
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		return (variableName.length()<10) ? variableName : variableName.substring(0, 10)+ "..."+ (!text.equals("") ? " // "+text:"");
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	
	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				HtmlConnector htmlConnector = getConnector();
				Document xmlDocument = htmlConnector.getCurrentXmlDocument();
				TwsCachedXPathAPI xpathApi = htmlConnector.context.getXpathApi();
				
				if ((xmlDocument == null) || (xpathApi == null)) {
					Engine.logBeans.warn((xmlDocument == null) ? "(XPath) Current DOM of HtmlConnector is Null!":"TwsCachedXPathAPI of HtmlConnector is Null!");
					return false;
				}
				
				evaluate(javascriptContext, scope, xpath, "xpath", false);
				String jsXpath = evaluated.toString();
				
				NodeList nodeList = null;
				try {
					nodeList = xpathApi.selectNodeList(xmlDocument, jsXpath);
				} catch (TransformerException e) {
					return false;
				} catch (ClassCastException e) {
					return false;
				}
				
				if (nodeList == null)
					return false;
				
				addToScope(scope, nodeList);
				return true;
			}
		}
		return false;
	}

	protected void addToScope(Scriptable scope, NodeList nodeList) {
		if (nodeList != null)
			scope.put(this.variableName, scope, nodeList);
	}
	
	@Override
	public String toJsString() {
		return variableName;
	}

}