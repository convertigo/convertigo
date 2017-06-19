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
import com.twinsoft.convertigo.engine.enums.LocalCachePolicy;

public class UIControlCallSequenceBeanInfo extends MySimpleBeanInfo {
	
	public UIControlCallSequenceBeanInfo() {
		try {
			beanClass = UIControlCallSequence.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.UIControlCallAction.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/uicontrolcallsequence_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/uicontrolcallsequence_color_32x32.png";

			resourceBundle = getResourceBundle("res/UIControlCallSequence");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[2];
			
            properties[0] = new PropertyDescriptor("cachePolicy", beanClass, "getCachePolicy", "setCachePolicy");
            properties[0].setDisplayName(getExternalizedString("property.cachePolicy.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.cachePolicy.short_description"));  
            properties[0].setPropertyEditorClass(LocalCachePolicy.class);
            
            properties[1] = new PropertyDescriptor("cacheTtl", beanClass, "getCacheTimeToLive", "setCacheTimeToLive");
            properties[1].setDisplayName(getExternalizedString("property.cacheTtl.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.cacheTtl.short_description"));  
            
            
			getPropertyDescriptor("target").setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
