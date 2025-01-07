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

package com.twinsoft.convertigo.beans.ngx.components;

import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIUseVariable extends UIControlVariable {

	private static final long serialVersionUID = -5137292309214244420L;

	public enum BindingType {
		oneWayBinding,
		twoWayBinding
		;
	}
	
	public UIUseVariable() {
		super();
	}

	@Override
	public UIUseVariable clone() throws CloneNotSupportedException {
		UIUseVariable cloned = (UIUseVariable) super.clone();
		return cloned;
	}

	private BindingType binding = BindingType.oneWayBinding;
	
	public BindingType getBinding() {
		return binding;
	}

	public void setBinding(BindingType binding) {
		this.binding = binding;
	}

	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			if (BindingType.oneWayBinding.equals(binding)) {
				return " ["+ getVarName() + "]=\"" + getVarValue() + "\"";
			}
			if (BindingType.twoWayBinding.equals(binding)) {
				return " [("+ getVarName() + ")]=\"" + getVarValue() + "\"";
			}
		}
		return "";
	}
	
	@Override
	public String toString() {
		String label = super.toString();
		if (BindingType.oneWayBinding.equals(binding)) {
			label = label.replace("=", "=>");
		}
		if (BindingType.twoWayBinding.equals(binding)) {
			label = label.replace("=", "<=>");
		}
		return label;
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("binding")) {
			return EnumUtils.toNames(BindingType.class);
		}
		return super.getTagsForProperty(propertyName);
	}

}
