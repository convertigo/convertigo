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

public class SequenceBeanInfo extends MySimpleBeanInfo {
    
	public SequenceBeanInfo() {
		try {
			beanClass =  Sequence.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.RequestableObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/sequence_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/sequence_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/Sequence");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("orderedVariables", beanClass, "getOrderedVariables", "setOrderedVariables");
			properties[0].setDisplayName(getExternalizedString("property.orderedVariables.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.orderedVariables.short_description"));
			//properties[0].setPropertyEditorClass(getEditorClass("SequenceVariablesEditor"));
			//properties[0].setExpert(true);
			properties[0].setHidden(true);

            properties[1] = new PropertyDescriptor("orderedSteps", beanClass, "getOrderedSteps", "setOrderedSteps");
            properties[1].setDisplayName(getExternalizedString("property.orderedSteps.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.orderedSteps.short_description"));
            properties[1].setHidden(true);
            
            properties[2] = new PropertyDescriptor("includeResponseElement", beanClass, "isIncludeResponseElement", "setIncludeResponseElement");
            properties[2].setDisplayName(getExternalizedString("property.includeResponseElement.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.includeResponseElement.short_description"));
            properties[2].setExpert(true);
           
            PropertyDescriptor property = getPropertyDescriptor("sheetLocation");
			property.setPropertyEditorClass(getEditorClass("SequenceSheetLocationEditor"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
