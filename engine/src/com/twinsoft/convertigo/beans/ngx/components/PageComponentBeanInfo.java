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

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class PageComponentBeanInfo extends MySimpleBeanInfo {
	
	public PageComponentBeanInfo() {
		try {
			beanClass = PageComponent.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.ngx.components.MobileComponent.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/ngx/components/images/pagecomponent_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/ngx/components/images/pagecomponent_color_32x32.png";

			resourceBundle = getResourceBundle("res/PageComponent");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[12];

			properties[0] = new PropertyDescriptor("iconPosition", beanClass, "getIconPosition", "setIconPosition");
			properties[0].setDisplayName(getExternalizedString("property.iconPosition.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.iconPosition.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));

			properties[1] = new PropertyDescriptor("title", beanClass, "getTitle", "setTitle");
			properties[1].setDisplayName(getExternalizedString("property.title.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.title.short_description"));

			properties[2] = new PropertyDescriptor("segment", beanClass, "getSegment", "setSegment");
			properties[2].setDisplayName(getExternalizedString("property.segment.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.segment.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("PropertyWithValidatorEditor")); 
			properties[2].setExpert(true);
			
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
			
			properties[6] = new PropertyDescriptor("startMenu", beanClass, "getStartMenu", "setStartMenu");
			properties[6].setDisplayName(getExternalizedString("property.startMenu.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.startMenu.short_description"));
			properties[6].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
			properties[7] = new PropertyDescriptor("endMenu", beanClass, "getEndMenu", "setEndMenu");
			properties[7].setDisplayName(getExternalizedString("property.endMenu.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.endMenu.short_description"));
			properties[7].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
			properties[8] = new PropertyDescriptor("icon", beanClass, "getIcon", "setIcon");
			properties[8].setDisplayName(getExternalizedString("property.icon.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.icon.short_description"));
			properties[8].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
			
			properties[9] = new PropertyDescriptor("preloadPriority", beanClass, "getPreloadPriority", "setPreloadPriority");
			properties[9].setDisplayName(getExternalizedString("property.preloadPriority.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.preloadPriority.short_description"));
			properties[9].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
			properties[9].setExpert(true);
			
			properties[10] = new PropertyDescriptor("defaultHistory", beanClass, "getDefaultHistory", "setDefaultHistory");
			properties[10].setDisplayName(getExternalizedString("property.defaultHistory.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.defaultHistory.short_description"));
			properties[10].setExpert(true);
			properties[10].setHidden(true);
			
			properties[11] = new PropertyDescriptor("changeDetection", beanClass, "getChangeDetection", "setChangeDetection");
			properties[11].setDisplayName(getExternalizedString("property.changeDetection.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.changeDetection.short_description"));
			properties[11].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
			properties[11].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
