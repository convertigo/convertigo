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

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.SimpleEvent;


abstract public class SimpleEventStatement extends AbstractEventStatement implements ITagsProperty{	
	private static final long serialVersionUID = 2414729337804241305L;
	private String action = "";
		
	public SimpleEventStatement() {
		super();
	}
	
	public SimpleEventStatement(String xpath) {
		super(xpath);
	}

	public SimpleEventStatement(String action, String xpath) {
		super(xpath);
		this.action = action;
	}
	
	abstract public String[] getActionStrings();
	
	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}
	
	@Override
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope)  throws EngineException {
		return new SimpleEvent(getXpath(), action);
	}

	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("action")) {
			return getActionStrings();
		}
		return new String[0];
	}
}
