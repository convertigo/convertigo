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

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class ApplicationComponentBeanInfo extends MySimpleBeanInfo {
	
	public ApplicationComponentBeanInfo() {
		try {
			beanClass = ApplicationComponent.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.MobileComponent.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/applicationcomponent_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/applicationcomponent_color_32x32.png";

			resourceBundle = getResourceBundle("res/ApplicationComponent");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[6];
			
			properties[0] = new PropertyDescriptor("orderedPages", beanClass, "getOrderedPages", "setOrderedPages");
			properties[0].setDisplayName(getExternalizedString("property.orderedPages.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.orderedPages.short_description"));
			properties[0].setHidden(true);

			properties[1] = new PropertyDescriptor("orderedRoutes", beanClass, "getOrderedRoutes", "setOrderedRoutes");
			properties[1].setDisplayName(getExternalizedString("property.orderedRoutes.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.orderedRoutes.short_description"));
			properties[1].setHidden(true);
			
			properties[2] = new PropertyDescriptor("orderedComponents", beanClass, "getOrderedComponents", "setOrderedComponents");
			properties[2].setDisplayName(getExternalizedString("property.orderedComponents.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.orderedComponents.short_description"));
			properties[2].setHidden(true);
			
			properties[3] = new PropertyDescriptor("orderedMenus", beanClass, "getOrderedMenus", "setOrderedMenus");
			properties[3].setDisplayName(getExternalizedString("property.orderedMenus.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.orderedMenus.short_description"));
			properties[3].setHidden(true);

			properties[4] = new PropertyDescriptor("componentScriptContent", beanClass, "getComponentScriptContent", "setComponentScriptContent");
			properties[4].setDisplayName(getExternalizedString("property.componentScriptContent.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.componentScriptContent.short_description"));
			properties[4].setHidden(true);

			properties[5] = new PropertyDescriptor("tplProjectName", beanClass, "getTplProjectName", "setTplProjectName");
			properties[5].setDisplayName(getExternalizedString("property.tplProjectName.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.tplProjectName.short_description"));
			properties[5].setPropertyEditorClass(getEditorClass("PropertyWithDynamicTagsEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
