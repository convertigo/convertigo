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

package com.twinsoft.convertigo.beans.variables;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class StepMultiValuedVariableBeanInfo extends MySimpleBeanInfo {
    
	public StepMultiValuedVariableBeanInfo() {
		try {
			beanClass = StepMultiValuedVariable.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.variables.StepVariable.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/variables/images/stepmultivaluedvariable_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/variables/images/stepmultivaluedvariable_color_32x32.png";

			resourceBundle = getResourceBundle("res/StepMultiValuedVariable");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties = new PropertyDescriptor[1];
			
            properties[0] = new PropertyDescriptor("subXPath", beanClass, "getSubXPath", "setSubXPath");
			properties[0].setDisplayName(getExternalizedString("property.subXPath.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.subXPath.short_description"));
			properties[0].setExpert(true);
			
			getPropertyDescriptor("bSoapArray").setHidden(false);
			
			PropertyDescriptor property = getPropertyDescriptor("sourceDefinition");
			property.setDisplayName(getExternalizedString("property.sourceDefinition.display_name"));
			property.setShortDescription(getExternalizedString("property.sourceDefinition.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
