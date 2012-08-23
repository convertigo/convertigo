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

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.HtmlParser;
import com.twinsoft.convertigo.engine.parsers.events.NavigationBarEvent;
import com.twinsoft.convertigo.engine.parsers.triggers.DocumentCompletedTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.ITriggerOwner;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;

public class NavigationBarStatement extends Statement implements ITagsProperty, ITriggerOwner {
	private static final long serialVersionUID = -2138787100756864583L;
	
	private String action = NavigationBarEvent.ACTION_GOTO;
	private String jsUrl = "\"about:blank\"";
	private TriggerXMLizer trigger = new TriggerXMLizer(new DocumentCompletedTrigger(1,60000));


	public NavigationBarStatement() {
		super();
	}
	
	public NavigationBarStatement(String action, String jsUrl, TriggerXMLizer trigger) {
		this.action = action;
		this.jsUrl = jsUrl;
		this.trigger = trigger;
	}

    @Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				HtmlTransaction htmlTransaction = (HtmlTransaction)getParentTransaction();
				HtmlConnector htmlConnector = (HtmlConnector)htmlTransaction.getParent();

				NavigationBarEvent evt;

				if(action.equalsIgnoreCase(NavigationBarEvent.ACTION_GOTO)){
					evaluate(javascriptContext, scope, jsUrl, "jsUrl", false);
					String url = evaluated.toString();
					evt = new NavigationBarEvent(action, url);
				}else{
					evt = new NavigationBarEvent(action);
				}

				HtmlParser htmlParser = htmlConnector.getHtmlParser();
				boolean success = htmlParser.dispatchEvent(evt, htmlTransaction.context, trigger.getTrigger());

				if(!success) Engine.logBeans.debug("NavigationBarStatement has failed");
				
				success = true; //TODO: ï¿½a boucle ï¿½ false
				return success;
			}
		}
		return false;
	}

    @Override
	public String toString(){
		String text = this.getComment();
		return action+(text.equalsIgnoreCase("")?"":" //"+text);
	}

    @Override
	public String toJsString() {
		return "";
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getJsUrl() {
		return jsUrl;
	}

	public void setJsUrl(String jsUrl) {
		this.jsUrl = jsUrl;
	}

	public TriggerXMLizer getTrigger() {
		return trigger;
	}

	public void setTrigger(TriggerXMLizer trigger) {
		this.trigger = trigger;
	}

	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("action")){
			return new String[] {
					NavigationBarEvent.ACTION_FORWARD,
					NavigationBarEvent.ACTION_BACKWARD,
					NavigationBarEvent.ACTION_GOTO,
					NavigationBarEvent.ACTION_REFRESH,
					NavigationBarEvent.ACTION_STOP
			};
		}
		return new String[0];
	}
}