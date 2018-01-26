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

public class PageComponentBeanInfo extends MySimpleBeanInfo {
	
	public PageComponentBeanInfo() {
		try {
			beanClass = PageComponent.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.MobileComponent.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/pagecomponent_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/pagecomponent_color_32x32.png";

			resourceBundle = getResourceBundle("res/PageComponent");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[8];
			
			properties[0] = new PropertyDescriptor("orderedComponents", beanClass, "getOrderedComponents", "setOrderedComponents");
			properties[0].setDisplayName(getExternalizedString("property.orderedComponents.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.orderedComponents.short_description"));
			properties[0].setHidden(true);

			properties[1] = new PropertyDescriptor("title", beanClass, "getTitle", "setTitle");
			properties[1].setDisplayName(getExternalizedString("property.title.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.title.short_description"));

			properties[2] = new PropertyDescriptor("segment", beanClass, "getSegment", "setSegment");
			properties[2].setDisplayName(getExternalizedString("property.segment.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.segment.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("PropertyWithValidatorEditor")); 
			
			properties[3] = new PropertyDescriptor("scriptContent", beanClass, "getScriptContent", "setScriptContent");
			properties[3].setDisplayName(getExternalizedString("property.scriptContent.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.scriptContent.short_description"));
			properties[3].setHidden(true);
			
            properties[4] = new PropertyDescriptor("inAutoMenu", beanClass, "isInAutoMenu", "setInAutoMenu");
			properties[4].setDisplayName(getExternalizedString("property.inAutoMenu.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.inAutoMenu.short_description"));

			properties[5] = new PropertyDescriptor("isEnabled", beanClass, "isEnabled", "setEnabled");
			properties[5].setDisplayName(getExternalizedString("property.isEnabled.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.isEnabled.short_description"));
			
			properties[6] = new PropertyDescriptor("menu", beanClass, "getMenu", "setMenu");
			properties[6].setDisplayName(getExternalizedString("property.menu.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.menu.short_description"));
			properties[6].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
			properties[7] = new PropertyDescriptor("icon", beanClass, "getIcon", "setIcon");
			properties[7].setDisplayName(getExternalizedString("property.icon.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.icon.short_description"));
			properties[7].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
