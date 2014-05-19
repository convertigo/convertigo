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

public class ReadCSVStepBeanInfo extends MySimpleBeanInfo{
	
	public ReadCSVStepBeanInfo() {
		try {
			beanClass = ReadCSVStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.ReadFileStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/csvR_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/csvR_32x32.png";
			
			resourceBundle = getResourceBundle("res/ReadCSVStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");	          
			
			properties = new PropertyDescriptor[6];
					
	        
	        properties[0] = new PropertyDescriptor("separator", beanClass, "getSeparator", "setSeparator");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.separator.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.separator.short_description"));            
			
	        properties[1] = new PropertyDescriptor("tagLineName", beanClass, "getTagLineName", "setTagLineName");
			properties[1].setDisplayName(getExternalizedString("property.tagLineName.display_name"));
	        properties[1].setShortDescription(getExternalizedString("property.tagLineName.short_description")); 
	        
	        properties[2] = new PropertyDescriptor("tagColName", beanClass, "getTagColName", "setTagColName");
			properties[2].setDisplayName(getExternalizedString("property.tagColName.display_name"));
	        properties[2].setShortDescription(getExternalizedString("property.tagColName.short_description")); 
	        
	        properties[3] = new PropertyDescriptor("titleLine", beanClass, "isTitleLine", "setTitleLine");
	        properties[3].setExpert(true);
			properties[3].setDisplayName(getExternalizedString("property.titleLine.display_name"));
	        properties[3].setShortDescription(getExternalizedString("property.titleLine.short_description")); 
	        
	        properties[4] = new PropertyDescriptor("verticalDirection", beanClass, "isVerticalDirection", "setVerticalDirection");
	        properties[4].setExpert(true);
			properties[4].setDisplayName(getExternalizedString("property.verticalDirection.display_name"));
	        properties[4].setShortDescription(getExternalizedString("property.verticalDirection.short_description"));
	    
	        properties[5] = new PropertyDescriptor("encoding", beanClass, "getEncoding", "setEncoding");
			properties[5].setDisplayName(getExternalizedString("property.encoding.display_name"));
	        properties[5].setShortDescription(getExternalizedString("property.encoding.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
