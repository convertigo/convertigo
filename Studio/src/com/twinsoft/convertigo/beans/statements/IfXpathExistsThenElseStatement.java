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
import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

public class IfXpathExistsThenElseStatement extends BlockStatement implements IThenElseStatementContainer, IXPathable {

	private static final long serialVersionUID = 8091155349126369180L;
	private transient ThenStatement thenStatement = null;
	private transient ElseStatement elseStatement = null;
	
	
	public IfXpathExistsThenElseStatement () {
		super();
	}
	
	public IfXpathExistsThenElseStatement(String condition) {
		super(condition);
		setXpath(condition);
	}
	
    @Override 
	public Object clone() throws CloneNotSupportedException {
    	IfXpathExistsThenElseStatement clonedObject = (IfXpathExistsThenElseStatement) super.clone();
		clonedObject.thenStatement = null;
		clonedObject.elseStatement = null;
		return clonedObject;
	}

    public boolean hasThenElseStatements() {
		checkSubLoaded();
		return true;
	}
	public void setThenStatement(ThenStatement thenStatement) {
		checkSubLoaded();
		this.thenStatement = thenStatement;
	}

	public ThenStatement getThenStatement() {
		checkSubLoaded();
		return thenStatement;
	}

	public void setElseStatement(ElseStatement elseStatement) {
		checkSubLoaded();
		this.elseStatement = elseStatement;
	}

	public ElseStatement getElseStatement() {
		checkSubLoaded();
		return elseStatement;
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

				
				evaluate(javascriptContext, scope, condition, "xpath", false);
				String jsXpath = evaluated.toString();
					
				NodeList nodeList = null;
				try {
					nodeList = xpathApi.selectNodeList(xmlDocument, jsXpath);
				} catch (TransformerException e) {
					return false;
				} catch (ClassCastException e) {
					return false;
				}
				
				if (nodeList == null){
					return false;
				}
				if (nodeList.getLength() == 0) {
					elseStatement = getElseStatement();
					if (elseStatement != null) {
						elseStatement.execute(javascriptContext, scope);
					}
					return false;
				}
					
				thenStatement = getThenStatement();
				if (thenStatement != null) {
					thenStatement.execute(javascriptContext, scope);
					return true;
				}
				return false;
			}
		}
		return false;
	}
	
	@Override
	public void addStatement(Statement statement) throws EngineException {
		checkSubLoaded();
		
		if (hasThenElseStatements()) {
			if ((!(statement instanceof ThenStatement)) && (!(statement instanceof ElseStatement))) {
				throw new EngineException("You cannot add to this statement a database object of type " + statement.getClass().getName());
			}
			
			if ((thenStatement == null) || (elseStatement == null)) {
				if ((statement instanceof ThenStatement)) {
					if (thenStatement == null) {
						super.addStatement(statement);
						thenStatement = (ThenStatement)statement;
					}
					else
						throw new EngineException("You cannot add to this statement another database object of type " + statement.getClass().getName());
				}
				else if ((statement instanceof ElseStatement)) {
					if (elseStatement == null) {
						super.addStatement(statement);
						elseStatement = (ElseStatement)statement;
					}
					else
						throw new EngineException("You cannot add to this statement another database object of type " + statement.getClass().getName());
				}
			}
			else {
				throw new EngineException("You cannot add to this step another database object of type " + statement.getClass().getName());
			}
		}
		else {
			super.addStatement(statement);
		}
	}
	
	@Override
	public void removeStatement(Statement statement) {
		checkSubLoaded();
		
		super.removeStatement(statement);
		if (hasThenElseStatements()) {
			if (statement.equals(thenStatement)) {
				thenStatement = null;
			}
			else if (statement.equals(elseStatement)) {
				elseStatement = null;
			}
		}
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		return "ifExists node at "+ condition + (!text.equals("") ? " // "+text:"");
	}
	
	@Override
	public String toJsString() {
		String code = "";
		if (!condition.equals("")) {
			code += " if ("+ condition +") {\n";
			code += super.toString();
			code += " \n}\n";
		}
		return code;
	}

	public String getXpath() {
		return getCondition();
	}

	public void setXpath(String xpath) {
		xpath = "'"+xpath.replace("\\", "\\\\").replace("'", "\\'")+"'";
		setCondition(xpath);
	}

}
