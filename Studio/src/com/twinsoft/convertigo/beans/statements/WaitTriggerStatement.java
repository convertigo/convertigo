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

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.triggers.ITriggerOwner;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.parsers.triggers.WaitTimeTrigger;

public class WaitTriggerStatement extends Statement implements ITriggerOwner{
	private static final long serialVersionUID = -1336157462556957815L;
	
	private TriggerXMLizer trigger = new TriggerXMLizer(new WaitTimeTrigger(1000));
	
	public WaitTriggerStatement() {
		super();
	}
	
	public TriggerXMLizer getTrigger() {
		return trigger;
	}

	public void setTrigger(TriggerXMLizer trigger) {
		this.trigger = trigger;
	}
	
	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {				
				HtmlTransaction htmlTransaction = getParentTransaction();
				HtmlConnector htmlConnector = (HtmlConnector)htmlTransaction.getParent();
				
				htmlConnector.getHtmlParser().waitTrigger(htmlTransaction.context, trigger.getTrigger());
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		String msg = "Wait " + trigger.toString();
		return  StringUtils.abbreviate(msg, 30);
	}
	
	@Override
	public String toJsString() {
		return null;
	}
}