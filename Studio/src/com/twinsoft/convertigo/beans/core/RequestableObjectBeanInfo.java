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

public class RequestableObjectBeanInfo extends MySimpleBeanInfo {
    
	public RequestableObjectBeanInfo() {
		try {
			beanClass =  RequestableObject.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/RequestableObject");
			
			properties = new PropertyDescriptor[7];
			
			properties[0] = new PropertyDescriptor("publicMethod", beanClass, "isPublicMethod", "setPublicMethod");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.publicMethod.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.publicMethod.short_description"));
			properties[0].setValue(BLACK_LIST_NAME,true);
			
			properties[1] = new PropertyDescriptor("sheetLocation", beanClass, "getSheetLocation", "setSheetLocation");
			properties[1].setDisplayName(getExternalizedString("property.sheetLocation.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.sheetLocation.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("SheetLocationEditor"));

			properties[2] = new PropertyDescriptor("responseTimeout", beanClass, "getResponseTimeout", "setResponseTimeout");
			properties[2].setDisplayName(getExternalizedString("property.responseTimeout.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.responseTimeout.short_description"));

			properties[3] = new PropertyDescriptor("encodingCharSet", beanClass, "getEncodingCharSet", "setEncodingCharSet");
			properties[3].setExpert(true);
			properties[3].setDisplayName(getExternalizedString("property.encodingCharSet.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.encodingCharSet.short_description"));

			properties[4] = new PropertyDescriptor("responseExpiryDate", beanClass, "getResponseExpiryDate", "setResponseExpiryDate");
			properties[4].setExpert(true);
			properties[4].setDisplayName(getExternalizedString("property.responseExpiryDate.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.responseExpiryDate.short_description"));

			properties[5] = new PropertyDescriptor("billable", beanClass, "isBillable", "setBillable");
			properties[5].setExpert(true);
			properties[5].setDisplayName(getExternalizedString("property.billable.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.billable.short_description"));

			properties[6] = new PropertyDescriptor("clientCachable", beanClass, "isClientCachable", "setClientCachable");
			properties[6].setExpert(true);
			properties[6].setDisplayName(getExternalizedString("property.clientCachable.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.clientCachable.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
