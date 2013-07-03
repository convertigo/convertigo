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

public class XMLSplitStepBeanInfo extends MySimpleBeanInfo {
    
	public XMLSplitStepBeanInfo() {
		try {
			beanClass = XMLSplitStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.XMLElementStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/splitstep_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/splitstep_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/XMLSplitStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[3];

            properties[0] = new PropertyDescriptor("regexp", beanClass, "getRegexp", "setRegexp");
            properties[0].setDisplayName(getExternalizedString("property.regexp.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.regexp.short_description"));
            
            properties[1] = new PropertyDescriptor("keepSeparator", beanClass, "isKeepSeparator", "setKeepSeparator");
            properties[1].setDisplayName(getExternalizedString("property.keepSeparator.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.keepSeparator.short_description"));            

            properties[2] = new PropertyDescriptor("tags", beanClass, "getTags", "setTags");
			properties[2].setExpert(true);
			properties[2].setDisplayName(getExternalizedString("property.tags.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.tags.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("SplitStepTagsEditor"));

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
