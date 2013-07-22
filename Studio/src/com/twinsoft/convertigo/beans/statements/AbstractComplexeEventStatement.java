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

import com.twinsoft.convertigo.engine.parsers.triggers.NoWaitTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;


abstract public class AbstractComplexeEventStatement extends AbstractEventStatement {
	private static final long serialVersionUID = 7966864043544697918L;
	
	private boolean uiEvent = false;
	
	public AbstractComplexeEventStatement() {
		this("");
	}
	
	public AbstractComplexeEventStatement(String xpath) {
		this(xpath, false);
	}
	
	public AbstractComplexeEventStatement(String xpath, boolean uiEvent) {
		super(xpath);
		this.uiEvent = uiEvent;

		setTrigger(new TriggerXMLizer(new NoWaitTrigger(false)));
	}

	public boolean getUiEvent() {
		return uiEvent;
	}

	public void setUiEvent(boolean uiEvent) {
		this.uiEvent = uiEvent;
	}

}
