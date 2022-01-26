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

package com.twinsoft.convertigo.beans.ngx.components;

public class UIActionElseEvent extends UIActionEvent {

	private static final long serialVersionUID = -8199147136898609941L;

	public UIActionElseEvent() {
		super();
	}
	
	@Override
	public UIActionElseEvent clone() throws CloneNotSupportedException {
		UIActionElseEvent cloned = (UIActionElseEvent) super.clone();
		return cloned;
	}
	
	@Override
	public String toString() {
		return "doElse";
	}

	@Override
	public String computeEvent() {
		return super.computeEvent();
	}
}
