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

public class XMLTransformStepBeanInfo extends MySimpleBeanInfo {
    
	public XMLTransformStepBeanInfo() {
		try {
			beanClass = XMLTransformStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.XMLElementStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/transformstep_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/transformstep_32x32.png";
			
			resourceBundle = getResourceBundle("res/XMLTransformStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[1];

            properties[0] = new PropertyDescriptor("replacements", beanClass, "getReplacements", "setReplacements");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.replacements.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.replacements.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("TransformStepReplacementEditor"));

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
