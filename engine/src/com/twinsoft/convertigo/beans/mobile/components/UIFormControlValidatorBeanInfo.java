/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class UIFormControlValidatorBeanInfo extends MySimpleBeanInfo {
	
	public UIFormControlValidatorBeanInfo() {
		try {
			beanClass = UIFormControlValidator.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.UIFormValidator.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/uiformvalidator_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/uiformvalidator_color_32x32.png";

			resourceBundle = getResourceBundle("res/UIFormControlValidator");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[6];
			
			properties[0] = new PropertyDescriptor("required", beanClass, "getRequired", "setRequired");
			properties[0].setDisplayName(getExternalizedString("property.required.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.required.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[0].setExpert(true);
			
			properties[1] = new PropertyDescriptor("requiredTrue", beanClass, "getRequiredTrue", "setRequiredTrue");
			properties[1].setDisplayName(getExternalizedString("property.requiredTrue.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.requiredTrue.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[1].setExpert(true);

			properties[2] = new PropertyDescriptor("email", beanClass, "getEmail", "setEmail");
			properties[2].setDisplayName(getExternalizedString("property.email.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.email.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[2].setExpert(true);

			properties[3] = new PropertyDescriptor("minLength", beanClass, "getMinLength", "setMinLength");
			properties[3].setDisplayName(getExternalizedString("property.minLength.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.minLength.short_description"));
            properties[3].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[3].setExpert(true);

			properties[4] = new PropertyDescriptor("maxLength", beanClass, "getMaxLength", "setMaxLength");
			properties[4].setDisplayName(getExternalizedString("property.maxLength.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.maxLength.short_description"));
            properties[4].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[4].setExpert(true);

			properties[5] = new PropertyDescriptor("pattern", beanClass, "getPattern", "setPattern");
			properties[5].setDisplayName(getExternalizedString("property.pattern.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.pattern.short_description"));
            properties[5].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
            properties[5].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
