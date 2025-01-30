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

package com.twinsoft.convertigo.beans.rest;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class AbstractRestResponseBeanInfo extends MySimpleBeanInfo {

	public AbstractRestResponseBeanInfo() {
		try {
			beanClass = AbstractRestResponse.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.UrlMappingResponse.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/rest/images/abstractrestresponse_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/rest/images/abstractrestresponse_color_32x32.png";

			resourceBundle = getResourceBundle("res/AbstractRestResponse");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[3];
			
            properties[0] = new PropertyDescriptor("statusCode", beanClass, "getStatusCode", "setStatusCode");
            properties[0].setDisplayName(getExternalizedString("property.statusCode.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.statusCode.short_description"));
			
            properties[1] = new PropertyDescriptor("statusText", beanClass, "getStatusText", "setStatusText");
            properties[1].setDisplayName(getExternalizedString("property.statusText.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.statusText.short_description"));
            
            properties[2] = new PropertyDescriptor("modelReference", beanClass, "getModelReference", "setModelReference");
            properties[2].setDisplayName(getExternalizedString("property.modelReference.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.modelReference.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("ModelObjectEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
