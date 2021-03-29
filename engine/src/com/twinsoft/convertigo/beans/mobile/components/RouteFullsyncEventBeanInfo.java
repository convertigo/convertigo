/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

public class RouteFullsyncEventBeanInfo extends MySimpleBeanInfo {
	
	public RouteFullsyncEventBeanInfo() {
		try {
			beanClass = RouteFullsyncEvent.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/routefullsyncevent_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/routefullsyncevent_color_32x32.png";

			resourceBundle = getResourceBundle("res/RouteFullsyncEvent");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[1];
			
			properties[0] = new PropertyDescriptor("verb", beanClass, "getVerb", "setVerb");
			properties[0].setDisplayName(getExternalizedString("property.verb.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.verb.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
			
			getPropertyDescriptor("source").setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
