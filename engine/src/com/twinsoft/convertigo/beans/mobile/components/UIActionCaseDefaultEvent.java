/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

public class UIActionCaseDefaultEvent extends UIActionCaseEvent {

	private static final long serialVersionUID = 4722725360986393645L;
	
	public UIActionCaseDefaultEvent() {
		super();
		isDefault = true;
		caseValue = "'__default__'";
	}
	
	@Override
	public UIActionCaseDefaultEvent clone() throws CloneNotSupportedException {
		UIActionCaseDefaultEvent cloned = (UIActionCaseDefaultEvent) super.clone();
		return cloned;
	}
	@Override
	public String toString() {
		return "default";
	}

}
