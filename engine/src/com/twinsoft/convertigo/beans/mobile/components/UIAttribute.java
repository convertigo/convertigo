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

import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.ATTRIBUTE)
public class UIAttribute extends UIComponent implements ITagsProperty {

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
	
	private MobileSmartSourceType attrValue = new MobileSmartSourceType("value");
	
	public MobileSmartSourceType getAttrSmartType() {
		return attrValue;
	}

	public void setAttrSmartType(MobileSmartSourceType attrValue) {
		this.attrValue = attrValue;
	}
	
	protected boolean isAttrPropertyBind() {
		String attr = getAttrName();
		return attr.startsWith("[") && attr.endsWith("]");
	}
	
	protected boolean isAttrEventBind() {
		String attr = getAttrName();
		return attr.startsWith("(") && attr.endsWith(")");
	}
	
	protected String getAttrValue() {
		String value = attrValue.getValue();
		if (isAttrPropertyBind()) {
			if (Mode.PLAIN.equals(attrValue.getMode())) {
				if (!value.startsWith("'") && !value.endsWith("'")) {
					value = "'" + MobileSmartSourceType.escapeStringForTpl(value) + "'";
				}
			}
		} else if (isAttrEventBind()) {
			
		} else {
			if (!Mode.PLAIN.equals(attrValue.getMode())) {
				value = "{{" + value + "}}";
			}
		}
		return value;
	}

	protected String getAttrLabel() {
		String label = attrValue.getLabel();
		if (!Mode.PLAIN.equals(attrValue.getMode())) {
			if (!isAttrPropertyBind() && !isAttrEventBind()) {
				label = "{{" + label + "}}";
			}
		}
		return label;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String attrVal = getAttrValue();
	        if (attrName.isEmpty()) {
	        	return attrVal.isEmpty() ? "":" "+ attrVal;
	        }
	        else {
	        	return (" "+attrName+"=\""+ attrVal +"\"");
	        }
		}
		else
			return "";
	}

	@Override
	public String toString() {
		String label = attrName;
		label = label + (label.isEmpty() ? "":"=") + getAttrLabel();
		return label;
	}

	@Override
	public boolean isFormControlAttribute() {
		return getAttrName().equals("formControlName");
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("attrValue")) {
			return new String[] {""};
		}
		return new String[0];
	}
	
	@Override
	public boolean updateSmartSource(String oldString, String newString) {
		boolean updated = false;
		String smartValue = attrValue.getSmartValue();
		if (smartValue.indexOf(oldString) != -1|| Pattern.compile(oldString).matcher(smartValue).find()) {
			attrValue.setSmartValue(smartValue.replaceAll(oldString, newString));
			updated = this.hasChanged = true;
		}
		return updated;
	}
}
