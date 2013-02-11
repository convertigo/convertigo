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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/core/MobilityBeanInfo.java $
 * $Author: laetitiam $
 * $Revision: 31301 $
 * $Date: 2012-08-03 17:52:41 +0200 (ven., 03 août 2012) $
 */

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class MobileApplicationBeanInfo extends MySimpleBeanInfo {
    
	public MobileApplicationBeanInfo() {
		try {
			beanClass = MobileApplication.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/mobileapplication_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/mobileapplication_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/MobileApplication");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[5];
			
            properties[0] = new PropertyDescriptor("enableFlashUpdate", MobileApplication.class, "getEnableFlashUpdate", "setEnableFlashUpdate");
            properties[0].setDisplayName(getExternalizedString("property.enableFlashUpdate.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.enableFlashUpdate.short_description"));

            properties[1] = new PropertyDescriptor("buildMode", MobileApplication.class, "getBuildMode", "setBuildMode");
            properties[1].setDisplayName(getExternalizedString("property.buildMode.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.buildMode.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("FlashUpdateBuildModeEditor"));

            properties[2] = new PropertyDescriptor("requireUserConfirmation", MobileApplication.class, "getRequireUserConfirmation", "setRequireUserConfirmation");
            properties[2].setDisplayName(getExternalizedString("property.requireUserConfirmation.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.requireUserConfirmation.short_description"));

            properties[3] = new PropertyDescriptor("applicationId", MobileApplication.class, "getApplicationId", "setApplicationId");
            properties[3].setDisplayName(getExternalizedString("property.applicationId.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.applicationId.short_description"));
            
            properties[4] = new PropertyDescriptor("endpoint", MobileApplication.class, "getEndpoint", "setEndpoint");
            properties[4].setDisplayName(getExternalizedString("property.endpoint.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.endpoint.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
