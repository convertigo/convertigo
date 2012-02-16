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

import com.twinsoft.convertigo.engine.parsers.events.SimpleEvent;

public class MouseStatement extends SimpleEventStatement {

	private static final long serialVersionUID = -9049228809267286116L;

	public MouseStatement() {
		this(SimpleEvent.action_click, "");
	}
	
	public MouseStatement(String xpath) {
		this(SimpleEvent.action_click, xpath);
	}

	public MouseStatement(String action, String xpath) {
		super(action, xpath);
	}
	
	@Override
	public String toString() {
		return action + super.toString();
	}

	@Override
	public String[] getActionStrings() {
		return SimpleEvent.getMouseActions();
	}
}