/*
 * Copyright (c) 2001-2022 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.statements;

import com.twinsoft.convertigo.engine.parsers.events.SimpleEvent;

public class CreateEventStatement extends SimpleEventStatement {
	private static final long serialVersionUID = -3143932444773103163L;

	public CreateEventStatement() {
		this(SimpleEvent.action_click, "");
	}
	
	public CreateEventStatement(String xpath) {
		this(SimpleEvent.action_click, xpath);
	}

	public CreateEventStatement(String action, String xpath) {
		super(action, xpath);
	}
	
	public String[] getActionStrings() {
		return SimpleEvent.getActions();
	}
	
	@Override
	public String toString() {
		return getAction() + super.toString();
	}
}
