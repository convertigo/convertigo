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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/XMLErrorStepBeanInfo.java $
 * $Author: nicolasa $
 * $Revision: 34210 $
 * $Date: 2013-05-22 17:57:15 +0200 (mer., 22 mai 2013) $
 */

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class SessionGetStepBeanInfo extends MySimpleBeanInfo {
    
	public SessionGetStepBeanInfo() {
		try {
			beanClass = SessionGetStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.StepWithExpressions.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/sessionGet_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/sessionGet_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/SessionGetStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");    

			properties = new PropertyDescriptor[1];

            properties[0] = new PropertyDescriptor("key", beanClass, "getKey", "setKey");
            properties[0].setDisplayName(getExternalizedString("property.key.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.key.short_description"));
            
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
