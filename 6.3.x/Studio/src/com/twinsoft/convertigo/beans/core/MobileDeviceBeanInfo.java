/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class MobileDeviceBeanInfo extends MySimpleBeanInfo {
    
	public MobileDeviceBeanInfo() {
		try {
			beanClass = MobileDevice.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/MobileDevice");
			
			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("screenWidth", MobileDevice.class, "getScreenWidth", "setScreenWidth");
			properties[0].setDisplayName(getExternalizedString("property.screenWidth.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.screenWidth.short_description"));
			properties[0].setExpert(false);
			
			properties[1] = new PropertyDescriptor("screenHeight", MobileDevice.class, "getScreenHeight", "setScreenHeight");
			properties[1].setDisplayName(getExternalizedString("property.screenHeight.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.screenHeight.short_description"));
			properties[1].setExpert(false);
			
			properties[2] = new PropertyDescriptor("resourcesPath", MobileDevice.class, "getResourcesPath", "setResourcesPath");
			properties[2].setDisplayName(getExternalizedString("property.resourcesPath.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.resourcesPath.short_description"));
			properties[2].setExpert(false);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
