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
import com.twinsoft.convertigo.engine.parsers.events.TabManagementEvent;

public class TabManagementStatement extends Statement implements ITagsProperty{
	private static final long serialVersionUID = 4434921446758559335L;
	
	private String action = TabManagementEvent.ACTION_GETINDEX;
	private String jsIndex = "1";
	private String getIndexVarname = "tabIndex";

	public TabManagementStatement() {
		super();
	}
	
	public TabManagementStatement(String action, String jsIndex, String  getIndexVarname) {
		this.action = action;
		this.jsIndex = jsIndex;
		this.getIndexVarname = getIndexVarname;
	}

    @Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				HtmlTransaction htmlTransaction = (HtmlTransaction)getParentTransaction();
				HtmlConnector htmlConnector = (HtmlConnector)htmlTransaction.getParent();
				
				TabManagementEvent evt;

				if(action.equalsIgnoreCase(TabManagementEvent.ACTION_SETINDEX)){
					try{
						evaluate(javascriptContext, scope, jsIndex, "jsIndex", false);
						int index = (int)Double.parseDouble(evaluated.toString());
						evt = new TabManagementEvent(action, index);
					}catch (Exception e) {
						throw new EngineException("Tab index value isn't good.", e);
					}
				}else{
					evt = new TabManagementEvent(action);
				}

				HtmlParser htmlParser = htmlConnector.getHtmlParser();
				boolean success = htmlParser.dispatchEvent(evt, htmlTransaction.context, null);

				if(!success) Engine.logBeans.debug("NavigationBarStatement has failed");
				else{
					if(action.equalsIgnoreCase(TabManagementEvent.ACTION_GETINDEX)){
						String code = getIndexVarname+"="+evt.getIndex();
						evaluate(javascriptContext, scope, code, "ContextGet", true);
					}else if(action.equalsIgnoreCase(TabManagementEvent.ACTION_GETNBTAB)){
						String code = getIndexVarname+"="+evt.getNbTab();
						evaluate(javascriptContext, scope, code, "ContextGet", true);
					}
				}
				success = true; //TODO: ï¿½a boucle ï¿½ false
				return success;
			}
		}
		return false;
	}

    @Override
	public String toString(){
		String text = this.getComment();
		return action+
		((action.equals(TabManagementEvent.ACTION_GETINDEX)||(action.equals(TabManagementEvent.ACTION_GETNBTAB)))?" "+getIndexVarname:"")+
		(action.equals(TabManagementEvent.ACTION_SETINDEX)?" "+jsIndex:"")+
		(text.equalsIgnoreCase("")?"":" //"+text);
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

	public String getJsIndex() {
		return jsIndex;
	}

	public void setJsIndex(String jsIndex) {
		this.jsIndex = jsIndex;
	}

	public String getGetIndexVarname() {
		return getIndexVarname;
	}

	public void setGetIndexVarname(String getIndexVarname) {
		this.getIndexVarname = getIndexVarname;
	}

	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("action")){
			return new String[] {
					TabManagementEvent.ACTION_NEXT,
					TabManagementEvent.ACTION_PREVIOUS,
					TabManagementEvent.ACTION_SETINDEX,
					TabManagementEvent.ACTION_GETINDEX,
					TabManagementEvent.ACTION_GETNBTAB,
					TabManagementEvent.ACTION_NEW,
					TabManagementEvent.ACTION_CLOSE
			};
		}
		return new String[0];
	}
}