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

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class RouteActionComponentBeanInfo extends MySimpleBeanInfo {
	
	public RouteActionComponentBeanInfo() {
		try {
			beanClass = RouteActionComponent.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.MobileComponent.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/routeactioncomponent_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/routeactioncomponent_color_32x32.png";

			resourceBundle = getResourceBundle("res/RouteActionComponent");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[7];
			
			properties[0] = new PropertyDescriptor("condition", beanClass, "getCondition", "setCondition");
			properties[0].setDisplayName(getExternalizedString("property.condition.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.condition.short_description"));
			properties[0].setValue("scriptable", Boolean.TRUE);
			
			properties[1] = new PropertyDescriptor("action", beanClass, "getAction", "setAction");
			properties[1].setDisplayName(getExternalizedString("property.action.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.action.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
			
			properties[2] = new PropertyDescriptor("page", beanClass, "getPage", "setPage");
			properties[2].setDisplayName(getExternalizedString("property.page.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.page.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
			properties[3] = new PropertyDescriptor("toastMessage", beanClass, "getToastMessage", "setToastMessage");
			properties[3].setDisplayName(getExternalizedString("property.toastMessage.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.toastMessage.short_description"));
			
			properties[4] = new PropertyDescriptor("toastDuration", beanClass, "getToastDuration", "setToastDuration");
			properties[4].setDisplayName(getExternalizedString("property.toastDuration.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.toastDuration.short_description"));
			
			properties[5] = new PropertyDescriptor("toastPosition", beanClass, "getToastPosition", "setToastPosition");
			properties[5].setDisplayName(getExternalizedString("property.toastPosition.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.toastPosition.short_description"));
			
            properties[6] = new PropertyDescriptor("isEnabled", beanClass, "isEnabled", "setEnabled");
			properties[6].setDisplayName(getExternalizedString("property.isEnabled.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.isEnabled.short_description"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
