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

package com.twinsoft.convertigo.beans.mobile.components;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class UIElementBeanInfo extends MySimpleBeanInfo {

	public UIElementBeanInfo() {
		try {
			beanClass = UIElement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.UIComponent.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/uielement_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/uielement_color_32x32.png";

			resourceBundle = getResourceBundle("res/UIElement");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[3];

			properties[0] = new PropertyDescriptor("tagName", beanClass, "getTagName", "setTagName");
			properties[0].setDisplayName(getExternalizedString("property.tagName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.tagName.short_description"));

			properties[1] = new PropertyDescriptor("selfClose", beanClass, "isSelfClose", "setSelfClose");
			properties[1].setDisplayName(getExternalizedString("property.selfClose.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.selfClose.short_description"));

			properties[2] = new PropertyDescriptor("identifier", beanClass, "getIdentifier", "setIdentifier");
			properties[2].setDisplayName(getExternalizedString("property.identifier.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.identifier.short_description"));
			properties[2].setValue(CATEGORY, "@Component");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
