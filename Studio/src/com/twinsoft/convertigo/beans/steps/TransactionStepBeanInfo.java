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

public class TransactionStepBeanInfo extends MySimpleBeanInfo {
    
	public TransactionStepBeanInfo() {
		try {
			beanClass = TransactionStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.RequestableStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/transactionstep_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/transactionstep_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/TransactionStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[4];
			
			properties[0] = new PropertyDescriptor("projectName", beanClass, "getProjectName", "setProjectName");
			properties[0].setDisplayName(getExternalizedString("property.projectName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.projectName.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			properties[0].setValue(BLACK_LIST_NAME, Boolean.TRUE);
			
            properties[1] = new PropertyDescriptor("connectorName", beanClass, "getConnectorName", "setConnectorName");
			properties[1].setDisplayName(getExternalizedString("property.connectorName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.connectorName.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			
            properties[2] = new PropertyDescriptor("transactionName", beanClass, "getTransactionName", "setTransactionName");
			properties[2].setDisplayName(getExternalizedString("property.transactionName.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.transactionName.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			
            properties[3] = new PropertyDescriptor("connectionStringDefinition", beanClass, "getConnectionStringDefinition", "setConnectionStringDefinition");
			properties[3].setDisplayName(getExternalizedString("property.connectionStringDefinition.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.connectionStringDefinition.short_description"));
			properties[3].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			properties[3].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
