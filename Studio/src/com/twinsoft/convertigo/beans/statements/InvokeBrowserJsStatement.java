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

import java.util.Iterator;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.InvokeBrowserJsEvent;
import com.twinsoft.convertigo.engine.parsers.triggers.NoWaitTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.util.GenericUtils;


public class InvokeBrowserJsStatement extends AbstractEventStatement {
	private static final long serialVersionUID = 6803619557375078473L;

	protected String codeJS = "//todo";
	protected XMLVector<XMLVector<String>> variables = new XMLVector<XMLVector<String>>();
		
	public InvokeBrowserJsStatement() {
		super("'/HTML'");
		trigger = new TriggerXMLizer(new NoWaitTrigger(false));
	}
	
	public InvokeBrowserJsStatement(String xpath) {
		super(xpath);
		trigger = new TriggerXMLizer(new NoWaitTrigger(false));
	}

	public InvokeBrowserJsStatement(String codeJS, String xpath) {
		this(xpath);
		this.codeJS = codeJS;
	}
	
	public InvokeBrowserJsStatement(String codeJS, String xpath, XMLVector<XMLVector<String>> variables) {
		this(xpath,codeJS);
		this.variables = variables;		
	}
	
	/**
	 * @return the action
	 */
	public String getCodeJS() {
		return codeJS;
	}

	/**
	 * @param codeJS the action to set
	 */
	public void setCodeJS(String codeJS) {
		this.codeJS = codeJS;
	}
	
	public XMLVector<XMLVector<String>> getVariables() {
		return variables;
	}
	
	public void setVariables(XMLVector<XMLVector<String>> variables) {
		this.variables = variables;
	}
	
	@Override
	public String toString(){
		return "invoke "+((codeJS.length()<12)?codeJS:(codeJS.substring(0, 12)+"..."));
	}
	
	@Override
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) throws EngineException {
		String jsCode = "";
		for (Iterator<XMLVector<String>> i = variables.iterator(); i.hasNext();) {
			String varName = null;
			try {
				XMLVector<String> line = GenericUtils.cast(i.next());
				varName = line.get(0);
				String description = line.get(1);
				String jsValue = line.get(2);
				evaluate(javascriptContext, scope, jsValue, "jsValue", false);
				jsValue = evaluated.toString();
				jsCode += "var " + varName + "='" + jsValue + "';//" + description + "\n";
			} catch (Exception e) {
				Engine.logBeans.error("Invoke Browser Js failed to set " + varName, e);
			}
		}
		jsCode += codeJS;
		Engine.logBeans.trace("InvokeBrowserJsStatement prepare this jsCode for invokation :\n" + jsCode);
		return new InvokeBrowserJsEvent(xpath, jsCode);
	}
}