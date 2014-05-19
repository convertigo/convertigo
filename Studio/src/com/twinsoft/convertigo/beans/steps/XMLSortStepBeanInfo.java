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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/XMLCopyStepBeanInfo.java $
 * $Author: laetitiam $
 * $Revision: 31301 $
 * $Date: 2012-08-03 17:52:41 +0200 (ven., 03 août 2012) $
 */

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class XMLSortStepBeanInfo extends MySimpleBeanInfo {
    
	public XMLSortStepBeanInfo() {
		try {
			beanClass = XMLSortStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/sortstep_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/sortstep_32x32.png";
			
			resourceBundle = getResourceBundle("res/XMLSortStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[5];

            properties[0] = new PropertyDescriptor("sourceDefinition", beanClass, "getSourceDefinition", "setSourceDefinition");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.sourceDefinition.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.sourceDefinition.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			properties[0].setValue(BLACK_LIST_NAME, Boolean.TRUE);
            
			properties[1] = new PropertyDescriptor("sortXPATHDefinition", beanClass, "getSortXPATHDefinition", "setSortXPATHDefinition");
			properties[1].setExpert(true);
			properties[1].setDisplayName(getExternalizedString("property.sortXPATHDefinition.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.sortXPATHDefinition.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("TextEditor"));
			properties[1].setValue(BLACK_LIST_NAME, Boolean.TRUE);            
			
			properties[2] = new PropertyDescriptor("orderSort", beanClass, "getOrderSort", "setOrderSort");
			properties[2].setExpert(true);
			properties[2].setDisplayName(getExternalizedString("property.orderSort.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.orderSort.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			properties[2].setValue(BLACK_LIST_NAME, Boolean.TRUE);   
            
			properties[3] = new PropertyDescriptor("optionSort", beanClass, "getOptionSort", "setOptionSort");
			properties[3].setExpert(true);
			properties[3].setDisplayName(getExternalizedString("property.optionSort.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.optionSort.short_description"));
            properties[3].setValue("scriptable", Boolean.TRUE);
			properties[3].setValue(BLACK_LIST_NAME, Boolean.TRUE);   
            
			properties[4] = new PropertyDescriptor("typeSort", beanClass, "getTypeSort", "setTypeSort");
			properties[4].setExpert(true);
			properties[4].setDisplayName(getExternalizedString("property.typeSort.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.typeSort.short_description"));
            properties[4].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			properties[4].setValue(BLACK_LIST_NAME, Boolean.TRUE);   
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
