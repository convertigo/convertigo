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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MobileApplication.FlashUpdateBuildMode;
import com.twinsoft.convertigo.beans.core.MobileApplication.SplashRemoveMode;
import com.twinsoft.convertigo.engine.enums.Accessibility;

public class MobileApplicationBeanInfo extends MySimpleBeanInfo {

	public MobileApplicationBeanInfo() {
		try {
			beanClass = MobileApplication.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/mobileapplication_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/mobileapplication_color_32x32.png";

			resourceBundle = getResourceBundle("res/MobileApplication");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[20];

			properties[0] = new PropertyDescriptor("enableFlashUpdate", MobileApplication.class, "getEnableFlashUpdate", "setEnableFlashUpdate");
			properties[0].setDisplayName(getExternalizedString("property.enableFlashUpdate.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.enableFlashUpdate.short_description"));

			properties[1] = new PropertyDescriptor("buildMode", MobileApplication.class, "getBuildMode", "setBuildMode");
			properties[1].setDisplayName(getExternalizedString("property.buildMode.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.buildMode.short_description"));
			properties[1].setPropertyEditorClass(FlashUpdateBuildMode.class);
			properties[1].setExpert(true);

			properties[2] = new PropertyDescriptor("requireUserConfirmation", MobileApplication.class, "getRequireUserConfirmation", "setRequireUserConfirmation");
			properties[2].setDisplayName(getExternalizedString("property.requireUserConfirmation.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.requireUserConfirmation.short_description"));
			properties[2].setExpert(true);

			properties[3] = new PropertyDescriptor("applicationId", MobileApplication.class, "getApplicationId", "setApplicationId");
			properties[3].setDisplayName(getExternalizedString("property.applicationId.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.applicationId.short_description"));

			properties[4] = new PropertyDescriptor("endpoint", MobileApplication.class, "getEndpoint", "setEndpoint");
			properties[4].setDisplayName(getExternalizedString("property.endpoint.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.endpoint.short_description"));
			properties[4].setPropertyEditorClass(getEditorClass("MobileApplicationEndpointEditor"));

			properties[5] = new PropertyDescriptor("applicationName", MobileApplication.class, "getApplicationName", "setApplicationName");
			properties[5].setDisplayName(getExternalizedString("property.applicationName.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.applicationName.short_description"));

			properties[6] = new PropertyDescriptor("applicationDescription", MobileApplication.class, "getApplicationDescription", "setApplicationDescription");
			properties[6].setDisplayName(getExternalizedString("property.applicationDescription.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.applicationDescription.short_description"));

			properties[7] = new PropertyDescriptor("applicationAuthorName", MobileApplication.class, "getApplicationAuthorName", "setApplicationAuthorName");
			properties[7].setDisplayName(getExternalizedString("property.applicationAuthorName.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.applicationAuthorName.short_description"));

			properties[8] = new PropertyDescriptor("applicationAuthorEmail", MobileApplication.class, "getApplicationAuthorEmail", "setApplicationAuthorEmail");
			properties[8].setDisplayName(getExternalizedString("property.applicationAuthorEmail.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.applicationAuthorEmail.short_description"));

			properties[9] = new PropertyDescriptor("applicationAuthorSite", MobileApplication.class, "getApplicationAuthorSite", "setApplicationAuthorSite");
			properties[9].setDisplayName(getExternalizedString("property.applicationAuthorSite.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.applicationAuthorSite.short_description"));

			properties[10] = new PropertyDescriptor("flashUpdateTimeout", MobileApplication.class, "getFlashUpdateTimeout", "setFlashUpdateTimeout");
			properties[10].setDisplayName(getExternalizedString("property.flashUpdateTimeout.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.flashUpdateTimeout.short_description"));
			properties[10].setExpert(true);

			properties[11] = new PropertyDescriptor("authenticationToken", MobileApplication.class, "getAuthenticationToken", "setAuthenticationToken");
			properties[11].setDisplayName(getExternalizedString("property.authenticationToken.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.authenticationToken.short_description"));
			properties[11].setExpert(true);

			properties[12] = new PropertyDescriptor("accessibility", MobileApplication.class, "getAccessibility", "setAccessibility");
			properties[12].setDisplayName(getExternalizedString("property.accessibility.display_name"));
			properties[12].setShortDescription(getExternalizedString("property.accessibility.short_description"));
			properties[12].setPropertyEditorClass(Accessibility.class);
			properties[12].setExpert(true);

			properties[13] = new PropertyDescriptor("applicationVersion", MobileApplication.class, "getApplicationVersion", "setApplicationVersion");
			properties[13].setDisplayName(getExternalizedString("property.applicationVersion.display_name"));
			properties[13].setShortDescription(getExternalizedString("property.applicationVersion.short_description"));

			properties[14] = new PropertyDescriptor("splashRemoveMode", MobileApplication.class, "getSplashRemoveMode", "setSplashRemoveMode");
			properties[14].setDisplayName(getExternalizedString("property.splashRemoveMode.display_name"));
			properties[14].setShortDescription(getExternalizedString("property.splashRemoveMode.short_description"));
			properties[14].setPropertyEditorClass(SplashRemoveMode.class);

			properties[15] = new PropertyDescriptor("fsConnector", MobileApplication.class, "getFsConnector", "setFsConnector");
			properties[15].setDisplayName(getExternalizedString("property.fsConnector.display_name"));
			properties[15].setShortDescription(getExternalizedString("property.fsConnector.short_description"));
			properties[15].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			properties[15].setHidden(true);

			properties[16] = new PropertyDescriptor("fsDesignDocument", MobileApplication.class, "getFsDesignDocument", "setFsDesignDocument");
			properties[16].setDisplayName(getExternalizedString("property.fsDesignDocument.display_name"));
			properties[16].setShortDescription(getExternalizedString("property.fsDesignDocument.short_description"));
			properties[16].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			properties[16].setHidden(true);
			
			properties[17] = new PropertyDescriptor("applicationIcons", beanClass, "getApplicationIcons", "setApplicationIcons");
			properties[17].setDisplayName(getExternalizedString("property.applicationIcons.display_name"));
			properties[17].setShortDescription(getExternalizedString("property.applicationIcons.short_description"));
			properties[17].setPropertyEditorClass(getEditorClass("ApplicationIconsEditor"));
			
			properties[18] = new PropertyDescriptor("applicationBgColor", beanClass, "getApplicationBgColor", "setApplicationBgColor");
			properties[18].setDisplayName(getExternalizedString("property.applicationBgColor.display_name"));
			properties[18].setShortDescription(getExternalizedString("property.applicationBgColor.short_description"));
			
			properties[19] = new PropertyDescriptor("applicationThemeColor", beanClass, "getApplicationThemeColor", "setApplicationThemeColor");
			properties[19].setDisplayName(getExternalizedString("property.applicationThemeColor.display_name"));
			properties[19].setShortDescription(getExternalizedString("property.applicationThemeColor.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
