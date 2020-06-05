/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import com.twinsoft.convertigo.engine.enums.FolderType;

public class UIStackVariable extends UIComponent {

	private static final long serialVersionUID = -3544805847766179482L;

	transient private Set<String> nullProps = new HashSet<String>();
	
	public UIStackVariable() {
		super();
	}
	
	@Override
	public UIStackVariable clone() throws CloneNotSupportedException {
		UIStackVariable cloned = (UIStackVariable) super.clone();
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
		return null;
	}

	@Override
	public String toString() {
		String val = getVariableValue();
		return getVariableName() + (val.isEmpty() ? "" : "="+val.toString());
	}

	@Override
	public FolderType getFolderType() {
		return FolderType.VARIABLE;
	}
}
