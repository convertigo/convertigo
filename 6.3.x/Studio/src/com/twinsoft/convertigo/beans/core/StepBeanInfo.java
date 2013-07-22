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

public class StepBeanInfo extends MySimpleBeanInfo {
    
	public StepBeanInfo() {
		try {
			beanClass = Step.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/Step");
			
			properties = new PropertyDescriptor[3];
			
            properties[0] = new PropertyDescriptor("isEnable", beanClass, "isEnable", "setEnable");
			properties[0].setDisplayName(getExternalizedString("property.isEnabled.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.isEnabled.short_description"));

			properties[1] = new PropertyDescriptor("output", beanClass, "isOutput", "setOutput");
			properties[1].setDisplayName(getExternalizedString("property.output.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.output.short_description"));
			properties[1].setExpert(true);
			properties[1].setValue(BLACK_LIST_NAME,true);
			
            properties[2] = new PropertyDescriptor("schemaDataType", beanClass, "getSchemaDataType", "setSchemaDataType");
			properties[2].setDisplayName(getExternalizedString("property.schemaDataType.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.schemaDataType.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			properties[2].setExpert(true);
			properties[2].setHidden(true);	
			properties[2].setValue(BLACK_LIST_NAME,true);	
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
