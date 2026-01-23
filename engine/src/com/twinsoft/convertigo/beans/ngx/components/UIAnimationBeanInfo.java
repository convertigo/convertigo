/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

public class UIAnimationBeanInfo extends MySimpleBeanInfo {
	
	public UIAnimationBeanInfo() {
		try {
			beanClass = UIAnimation.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.ngx.components.UIAttribute.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/ngx/components/images/uianimation_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/ngx/components/images/uianimation_32x32.png";

			resourceBundle = getResourceBundle("res/UIAnimation");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("animationName", beanClass, "getAnimationName", "setAnimationName");
			properties[0].setDisplayName(getExternalizedString("property.animationName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.animationName.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
			
			properties[1] = new PropertyDescriptor("isInfinite", beanClass, "isInfinite", "setInfinite");
			properties[1].setDisplayName(getExternalizedString("property.isInfinite.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.isInfinite.short_description"));

			getPropertyDescriptor("attrName").setHidden(true);
			getPropertyDescriptor("attrValue").setHidden(true);
			getPropertyDescriptor("throttleTime").setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
