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

package com.twinsoft.convertigo.beans.references;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class RemoteFileReferenceBeanInfo extends MySimpleBeanInfo {
	public RemoteFileReferenceBeanInfo() {
		try {
			beanClass = RemoteFileReference.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.references.FileReference.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/references/images/RemoteFileReference_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/references/images/RemoteFileReference_32x32.png";
			
			resourceBundle = getResourceBundle("res/RemoteFileReference");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[4];

            properties[0] = new PropertyDescriptor("urlpath", beanClass, "getUrlpath", "setUrlpath");
            properties[0].setDisplayName(getExternalizedString("property.urlpath.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.urlpath.short_description"));
            
            properties[1] = new PropertyDescriptor("needAuthentication", beanClass, "needAuthentication", "setNeedAuthentication");
            properties[1].setDisplayName(getExternalizedString("property.needAuthentication.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.needAuthentication.short_description"));
            properties[1].setExpert(true);
            
            properties[2] = new PropertyDescriptor("authUser", beanClass, "getAuthUser", "setAuthUser");
            properties[2].setDisplayName(getExternalizedString("property.authUser.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.authUser.short_description"));
            properties[2].setExpert(true);
            
            properties[3] = new PropertyDescriptor("authPassword", beanClass, "getAuthPassword", "setAuthPassword");
            properties[3].setDisplayName(getExternalizedString("property.authPassword.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.authPassword.short_description"));
            properties[3].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
