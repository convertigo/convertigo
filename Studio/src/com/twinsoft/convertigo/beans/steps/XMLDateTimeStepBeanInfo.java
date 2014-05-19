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

public class XMLDateTimeStepBeanInfo extends MySimpleBeanInfo {
    
	public XMLDateTimeStepBeanInfo() {
		try {
			beanClass = XMLDateTimeStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.XMLConcatStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/datetime_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/datetime_32x32.png";
			
			resourceBundle = getResourceBundle("res/XMLDateTimeStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
		
			properties = new PropertyDescriptor[4];

            properties[0] = new PropertyDescriptor("inputFormat", beanClass, "getInputFormat", "setInputFormat");
            properties[0].setDisplayName(getExternalizedString("property.inputFormat.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.inputFormat.short_description"));
            properties[0].setExpert(true);

            properties[1] = new PropertyDescriptor("inputLocale", beanClass, "getInputLocale", "setInputLocale");
            properties[1].setDisplayName(getExternalizedString("property.inputLocale.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.inputLocale.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            properties[1].setExpert(true);
            
            properties[2] = new PropertyDescriptor("outputFormat", beanClass, "getOutputFormat", "setOutputFormat");
            properties[2].setDisplayName(getExternalizedString("property.outputFormat.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.outputFormat.short_description"));
            properties[2].setExpert(true);
            
            properties[3] = new PropertyDescriptor("outputLocale", beanClass, "getOutputLocale", "setOutputLocale");
            properties[3].setDisplayName(getExternalizedString("property.outputLocale.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.outputLocale.short_description"));
            properties[3].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            properties[3].setExpert(true);
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
