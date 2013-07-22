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

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.InputCheckEvent;

public class InputHtmlSetCheckedStatement extends AbstractComplexeEventStatement {
	private static final long serialVersionUID = 7889858083151761748L;

	private boolean checked = true;
	
	public InputHtmlSetCheckedStatement() {
		super();
	}
		
	public InputHtmlSetCheckedStatement(String xpath, boolean checked) {
		super(xpath);
		this.checked = checked;
	}

	@Override
	public String toString() {
		return (checked ? "check" : "uncheck") + super.toString();
	}
	
	public boolean getChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	@Override
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) throws EngineException {
		return new InputCheckEvent(getXpath(), getUiEvent(), checked);
	}
}