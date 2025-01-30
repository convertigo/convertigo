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

public class OperationResponseBeanInfo extends MySimpleBeanInfo {

	public OperationResponseBeanInfo() {
		try {
			beanClass = OperationResponse.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.rest.AbstractRestResponse.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/rest/images/operationresponse_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/rest/images/operationresponse_color_32x32.png";

			resourceBundle = getResourceBundle("res/OperationResponse");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[1];
			
            properties[0] = new PropertyDescriptor("xpath", beanClass, "getXpath", "setXpath");
            properties[0].setDisplayName(getExternalizedString("property.xpath.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.xpath.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
