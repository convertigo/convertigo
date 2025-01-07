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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class SequenceBeanInfo extends MySimpleBeanInfo {
    
	public SequenceBeanInfo() {
		try {
			beanClass =  Sequence.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.RequestableObject.class;

			resourceBundle = getResourceBundle("res/Sequence");
			
			properties = new PropertyDescriptor[2];
            
            properties[0] = new PropertyDescriptor("includeResponseElement", beanClass, "isIncludeResponseElement", "setIncludeResponseElement");
            properties[0].setDisplayName(getExternalizedString("property.includeResponseElement.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.includeResponseElement.short_description"));
            properties[0].setExpert(true);
           
            properties[1] = new PropertyDescriptor("autoStart", beanClass, "isAutoStart", "setAutoStart");
            properties[1].setDisplayName(getExternalizedString("property.autoStart.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.autoStart.short_description"));
            properties[1].setExpert(true);
            
            PropertyDescriptor property = getPropertyDescriptor("sheetLocation");
			property.setPropertyEditorClass(getEditorClass("SequenceSheetLocationEditor"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
