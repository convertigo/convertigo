/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

public class UIControlAttrValue extends UIComponent {

	private static final long serialVersionUID = 1629185375344957613L;

	public UIControlAttrValue() {
		super();
	}
	
	@Override
	public UIControlAttrValue clone() throws CloneNotSupportedException {
		UIControlAttrValue cloned = (UIControlAttrValue)super.clone();
		return cloned;
	}
	
	/*
	 * The event value
	 */
	private String eventValue = "";
	
	public String getEventValue() {
		return eventValue;
	}

	public void setEventValue(String eventValue) {
		this.eventValue = eventValue;
	}

	
	@Override
	public String computeTemplate() {
		return isEnabled() ? getEventValue():"";
	}	
}
