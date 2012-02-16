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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.triggers.DocumentCompletedTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.ITriggerOwner;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

abstract public class AbstractEventStatement extends XpathableStatement implements ITriggerOwner {
	private static final long serialVersionUID = -8398273995003637112L;
	protected TriggerXMLizer trigger = new TriggerXMLizer(new DocumentCompletedTrigger(1,60000));
	
	public AbstractEventStatement() {
		super();
	}
	
	public AbstractEventStatement(String xpath) {
		super(xpath);
	}
	
	abstract public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) throws EngineException;
	
	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, xpath, "xpath", false);
				String jsXpath = evaluated.toString();
				
				HtmlTransaction htmlTransaction = (HtmlTransaction)getParentTransaction();
				HtmlConnector htmlConnector = (HtmlConnector)htmlTransaction.getParent();
				
				Engine.logBeans.trace("Getting event...");
				AbstractEvent event = getEvent(javascriptContext, scope);
				event.setXPath(jsXpath);
				
				Engine.logBeans.trace("Dispatching event...");
				boolean dispatch = htmlConnector.dispatchEvent(event, htmlTransaction.context, trigger.getTrigger());
				
				Engine.logBeans.trace("Getting DOM...");
				htmlTransaction.setCurrentXmlDocument(htmlConnector.getHtmlParser().getDom(htmlTransaction.context));
						
				if (dispatch)
					Engine.logBeans.trace("For target '"+ jsXpath +" "+event+" has been successfully dispatched on webViewer");
				else {
					Engine.logBeans.warn("For target '"+ jsXpath +" "+event+" has not been well dispatched on webViewer");
					throw new EngineException("Error when dispatch Html event");
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString(){
		String text = this.getComment();
		return (!text.equals("") ? " // "+text.replace('\n', ';'):"");
	}
	
	@Override
	public String toJsString() {
		return "";
	}

	public TriggerXMLizer getTrigger() {
		return trigger;
	}

	public void setTrigger(TriggerXMLizer trigger) {
		this.trigger = trigger;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Statement#configure(org.w3c.dom.Element)
	 */
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compare(version, "4.0.4") < 0) {
        	xpath = "'" + xpath + "'";
			hasChanged = true;
			Engine.logBeans.warn("[HttpStatement] The object \"" + getName()+ "\" has been updated to version 4.0.4");
        }
	}
}