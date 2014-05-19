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

public class XMLGenerateDatesStepBeanInfo extends MySimpleBeanInfo {
    
	public XMLGenerateDatesStepBeanInfo() {
		try {
			beanClass = XMLGenerateDatesStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.XMLGenerateStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/generatedates_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/generatedates_32x32.png";
			
			resourceBundle = getResourceBundle("res/XMLGenerateDatesStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[9];
			
            properties[0] = new PropertyDescriptor("startDefinition", beanClass, "getStartDefinition", "setStartDefinition");
			properties[0].setDisplayName(getExternalizedString("property.startDefinition.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.startDefinition.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			properties[0].setExpert(true);
			
            properties[1] = new PropertyDescriptor("stopDefinition", beanClass, "getStopDefinition", "setStopDefinition");
			properties[1].setDisplayName(getExternalizedString("property.stopDefinition.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.stopDefinition.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			properties[1].setExpert(true);

            properties[2] = new PropertyDescriptor("daysDefinition", beanClass, "getDaysDefinition", "setDaysDefinition");
			properties[2].setDisplayName(getExternalizedString("property.daysDefinition.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.daysDefinition.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			properties[2].setExpert(true);
			
			properties[3] = new PropertyDescriptor("inputFormat", beanClass, "getInputFormat", "setInputFormat");
            properties[3].setDisplayName(getExternalizedString("property.inputFormat.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.inputFormat.short_description"));
            properties[3].setExpert(true);
            
            properties[4] = new PropertyDescriptor("inputLocale", beanClass, "getInputLocale", "setInputLocale");
            properties[4].setDisplayName(getExternalizedString("property.inputLocale.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.inputLocale.short_description"));
            properties[4].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            properties[4].setExpert(true);
			
			properties[5] = new PropertyDescriptor("outputFormat", beanClass, "getOutputFormat", "setOutputFormat");
            properties[5].setDisplayName(getExternalizedString("property.outputFormat.display_name"));
            properties[5].setShortDescription(getExternalizedString("property.outputFormat.short_description"));
            properties[5].setExpert(true);
            
            properties[6] = new PropertyDescriptor("outputLocale", beanClass, "getOutputLocale", "setOutputLocale");
            properties[6].setDisplayName(getExternalizedString("property.outputLocale.display_name"));
            properties[6].setShortDescription(getExternalizedString("property.outputLocale.short_description"));
            properties[6].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            properties[6].setExpert(true);

            properties[7] = new PropertyDescriptor("split", beanClass, "isSplit", "setSplit");
            properties[7].setDisplayName(getExternalizedString("property.split.display_name"));
            properties[7].setShortDescription(getExternalizedString("property.split.short_description"));
            properties[7].setExpert(true);

            properties[8] = new PropertyDescriptor("calendarCompatibility", beanClass, "isCalendarCompatibility", "setCalendarCompatibility");
            properties[8].setDisplayName(getExternalizedString("property.calendarCompatibility.display_name"));
            properties[8].setShortDescription(getExternalizedString("property.calendarCompatibility.short_description"));
            properties[8].setExpert(true);
            
            getPropertyDescriptor("xmlSimpleTypeAffectation").setHidden(false);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
