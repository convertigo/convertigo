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

import java.util.HashSet;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.VARIABLE)
public class UICompVariable extends UIComponent {

	private static final long serialVersionUID = -4337991111463413731L;
	
	transient private Set<String> nullProps = new HashSet<String>();
	
	public UICompVariable() {
		super();
	}
	
	@Override
	public UICompVariable clone() throws CloneNotSupportedException {
		UICompVariable cloned = (UICompVariable) super.clone();
		cloned.nullProps = nullProps;
		return cloned;
	}

	public String getVariableName() {
		return getName();
	}
	
	private String value = "''";
	
	public String getVariableValue() {
		return value;
	}

	public void setVariableValue(String paramValue) {
		this.value = paramValue;
	}

	@Override
	public String computeTemplate() {
		return "";
	}

	@Override
	public String toString() {
		String val = getVariableValue();
		return getVariableName() + (val.isEmpty() ? "" : "="+val.toString());
	}
}
