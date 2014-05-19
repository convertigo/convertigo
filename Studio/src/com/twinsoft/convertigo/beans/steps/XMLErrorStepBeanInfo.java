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

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class XMLErrorStepBeanInfo extends MySimpleBeanInfo {
    
	public XMLErrorStepBeanInfo() {
		try {
			beanClass = XMLErrorStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.StepWithExpressions.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/xmlErrorStructure_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/xmlErrorStructure_32x32.png";
			
			resourceBundle = getResourceBundle("res/XMLErrorStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[3];

            properties[0] = new PropertyDescriptor("message", beanClass, "getMessage", "setMessage");
            properties[0].setDisplayName(getExternalizedString("property.message.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.message.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[1] = new PropertyDescriptor("details", beanClass, "getDetails", "setDetails");
            properties[1].setDisplayName(getExternalizedString("property.details.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.details.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[2] = new PropertyDescriptor("code", beanClass, "getCode", "setCode");
            properties[2].setDisplayName(getExternalizedString("property.code.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.code.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
