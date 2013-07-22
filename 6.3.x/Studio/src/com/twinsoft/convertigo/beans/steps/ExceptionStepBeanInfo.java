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

public class ExceptionStepBeanInfo extends MySimpleBeanInfo {
    
	public ExceptionStepBeanInfo() {
		try {
			beanClass = ExceptionStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.SimpleStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/exception_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/exception_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/ExceptionStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[1];
			
	        properties[0] = new PropertyDescriptor("details", beanClass, "getDetails", "setDetails");
	        properties[0].setDisplayName(getExternalizedString("property.details.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.details.short_description"));
	        properties[0].setValue("scriptable", Boolean.TRUE);
	        properties[0].setPropertyEditorClass(getEditorClass("JavascriptTextEditor"));

	        PropertyDescriptor property = getPropertyDescriptor("expression");
	        property.setDisplayName(getExternalizedString("property.message.display_name"));
	        property.setShortDescription(getExternalizedString("property.message.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
