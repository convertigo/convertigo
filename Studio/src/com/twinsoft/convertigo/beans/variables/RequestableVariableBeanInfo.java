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

public class RequestableVariableBeanInfo extends MySimpleBeanInfo {
    
	public RequestableVariableBeanInfo() {
		try {
			beanClass = RequestableVariable.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Variable.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/variables/images/variable_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/variables/images/variable_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/variables/res/RequestableVariable");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[4];
			
            properties[0] = new PropertyDescriptor("wsdl", beanClass, "isWsdl", "setWsdl");
			properties[0].setDisplayName(getExternalizedString("property.wsdl.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.wsdl.short_description"));
			properties[0].setExpert(true);
			properties[0].setValue(BLACK_LIST_NAME,true);			
			
            properties[1] = new PropertyDescriptor("personalizable", beanClass, "isPersonalizable", "setPersonalizable");
			properties[1].setDisplayName(getExternalizedString("property.personalizable.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.personalizable.short_description"));
			properties[1].setExpert(true);
			
            properties[2] = new PropertyDescriptor("cachedKey", beanClass, "isCachedKey", "setCachedKey");
			properties[2].setDisplayName(getExternalizedString("property.cachedKey.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.cachedKey.short_description"));
			properties[2].setExpert(true);
			
            properties[3] = new PropertyDescriptor("schemaType", beanClass, "getSchemaType", "setSchemaType");
			properties[3].setDisplayName(getExternalizedString("property.schemaType.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.schemaType.short_description"));
			properties[3].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			properties[3].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
