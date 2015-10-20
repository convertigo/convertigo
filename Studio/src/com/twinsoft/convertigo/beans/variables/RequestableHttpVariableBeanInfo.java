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

package com.twinsoft.convertigo.beans.variables;

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.enums.DoFileUploadMode;


public class RequestableHttpVariableBeanInfo extends MySimpleBeanInfo {
    
	public RequestableHttpVariableBeanInfo() {
		try {
			beanClass = RequestableHttpVariable.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.variables.RequestableVariable.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/variables/images/variable_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/variables/images/variable_color_32x32.png";

			resourceBundle = getResourceBundle("res/RequestableHttpVariable");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
            properties[0] = new PropertyDescriptor("httpMethod", beanClass, "getHttpMethod", "setHttpMethod");
			properties[0].setDisplayName(getExternalizedString("property.httpMethod.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.httpMethod.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			
            properties[1] = new PropertyDescriptor("httpName", beanClass, "getHttpName", "setHttpName");
			properties[1].setDisplayName(getExternalizedString("property.httpName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.httpName.short_description"));
			
            properties[2] = new PropertyDescriptor("doFileUploadMode", beanClass, "getDoFileUploadMode", "setDoFileUploadMode");
			properties[2].setDisplayName(getExternalizedString("property.doFileUploadMode.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.doFileUploadMode.short_description"));
			properties[2].setPropertyEditorClass(DoFileUploadMode.class);
			properties[2].setExpert(true);
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
