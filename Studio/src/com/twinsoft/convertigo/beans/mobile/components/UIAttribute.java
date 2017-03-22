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

public class UIAttribute extends UIComponent {

	private static final long serialVersionUID = 4407761661788130893L;

	public UIAttribute() {
		super();
	}

	@Override
	public UIAttribute clone() throws CloneNotSupportedException {
		UIAttribute cloned = (UIAttribute) super.clone();
		return cloned;
	}

	private String attrName = "attr";
	
	public String getAttrName() {
		return attrName;
	}

	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}
	
	private String attrValue = "";
	
	public String getAttrValue() {
		return attrValue;
	}

	public void setAttrValue(String attrValue) {
		this.attrValue = attrValue;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
	        if (attrName.isEmpty()) { 
	        	return "";
	        }
	        else if (attrValue.isEmpty()) {
	        	return " "+attrName;
	        }
	        else {
	        	return (" "+attrName+"=\""+attrValue+"\"");
	        }
		}
		else
			return "";
	}

}
