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

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class UIFontStyleBeanInfo extends MySimpleBeanInfo {
	
	public UIFontStyleBeanInfo() {
		try {
			beanClass = UIFontStyle.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.ngx.components.UIStyle.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/ngx/components/images/uifontstyle_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/ngx/components/images/uifontstyle_color_32x32.png";

			resourceBundle = getResourceBundle("res/UIFontStyle");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[5];
			
			properties[0] = new PropertyDescriptor("fontFamily", beanClass, "getFontFamily", "setFontFamily");
			properties[0].setDisplayName(getExternalizedString("property.fontFamily.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.fontFamily.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[0].setValue("category", "@Font");
			
			properties[1] = new PropertyDescriptor("fontSize", beanClass, "getFontSize", "setFontSize");
			properties[1].setDisplayName(getExternalizedString("property.fontSize.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.fontSize.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[1].setValue("category", "@Font");
            
			properties[2] = new PropertyDescriptor("fontStyle", beanClass, "getFontStyle", "setFontStyle");
			properties[2].setDisplayName(getExternalizedString("property.fontStyle.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.fontStyle.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[2].setValue("category", "@Font");
            
			properties[3] = new PropertyDescriptor("fontWeight", beanClass, "getFontWeight", "setFontWeight");
			properties[3].setDisplayName(getExternalizedString("property.fontWeight.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.fontWeight.short_description"));
			properties[3].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[3].setValue("category", "@Font");
            
			properties[4] = new PropertyDescriptor("ruleTargets", beanClass, "getRuleTargets", "setRuleTargets");
			properties[4].setDisplayName(getExternalizedString("property.ruleTargets.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.ruleTargets.short_description"));
			properties[4].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[4].setValue("category", "@Rule");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
