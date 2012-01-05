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

import java.beans.*;

public class VariableBeanInfo extends MySimpleBeanInfo {
    
	public VariableBeanInfo() {
		try {
			beanClass = Variable.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/variable_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/variable_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/Variable");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[4];
			
            properties[0] = new PropertyDescriptor("description", beanClass, "getDescription", "setDescription");
			properties[0].setDisplayName(getExternalizedString("property.description.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.description.short_description"));
			
            properties[1] = new PropertyDescriptor("value", beanClass, "getDefaultValue", "setDefaultValue");
			properties[1].setDisplayName(getExternalizedString("property.value.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.value.short_description"));
			properties[1].setValue("nillable", Boolean.TRUE);
			
            properties[2] = new PropertyDescriptor("visibility", beanClass, "getVisibility", "setVisibility");
			properties[2].setDisplayName(getExternalizedString("property.visibility.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.visibility.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("VisibilityEditor"));
			properties[2].setValue(BLACK_LIST_NAME,true);
			
			properties[3] = new PropertyDescriptor("bSoapArray", beanClass, "isSoapArray", "setSoapArray");
			properties[3].setDisplayName(getExternalizedString("property.bsoaparray.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.bsoaparray.short_description"));
			properties[3].setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

	
}
