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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.statements;

import javax.xml.transform.TransformerException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.engine.EngineException;

public class IfXpathExistsStatement extends BlockStatement implements IXPathable {

	private static final long serialVersionUID = 3824442373791368174L;

	public IfXpathExistsStatement() {
		super();
	}

	public IfXpathExistsStatement(String condition) {
		super(condition);
	}
	
	public String getXpath() {
		return getCondition();
	}

	public void setXpath(String xpath) {
		xpath = "'"+xpath.replace("\\", "\\\\").replace("'", "\\'")+"'";
		setCondition(xpath);
	}
	
	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, condition, "xpath", false);
				String jsXpath 				= evaluated.toString();
				NodeList nodeList 			= null;
				
				try {
					nodeList = getParentTransaction().context.getXpathApi().selectNodeList(getConnector().getCurrentXmlDocument(), jsXpath);
				} catch (TransformerException e) {
					return false;
				} catch (ClassCastException e) {
					return false;
				}
				
				if (nodeList == null)
					return false;
				if (nodeList.getLength() == 0)
					return false;
				return executeNextStatement(javascriptContext, scope);
			}
		}
		return false;
	}
	
	public String toString() {
		String text = this.getComment();
		return "IfExists node at "+ condition + (!text.equals("") ? " // "+text:"");
	}
	
	public String toJsString() {
		return "";
	}
}
