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

public class SequenceStepBeanInfo extends MySimpleBeanInfo {
    
	public SequenceStepBeanInfo() {
		try {
			beanClass = SequenceStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.RequestableStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/sequencestep_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/sequencestep_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/SequenceStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[3];
			
            properties[0] = new PropertyDescriptor("projectName", beanClass, "getProjectName", "setProjectName");
			properties[0].setDisplayName(getExternalizedString("property.projectName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.projectName.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("PropertyWithDynamicTagsEditor"));	
			properties[0].setValue(BLACK_LIST_NAME, Boolean.TRUE);
			
            properties[1] = new PropertyDescriptor("sequenceName", beanClass, "getSequenceName", "setSequenceName");
			properties[1].setDisplayName(getExternalizedString("property.sequenceName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.sequenceName.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("PropertyWithDynamicTagsEditor"));
			
            properties[2] = new PropertyDescriptor("inheritTransactionCtx", beanClass, "isInheritTransactionCtx", "setInheritTransactionCtx");
			properties[2].setDisplayName(getExternalizedString("property.inheritTransactionCtx.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.inheritTransactionCtx.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
