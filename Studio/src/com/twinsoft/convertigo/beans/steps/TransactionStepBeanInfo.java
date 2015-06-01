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

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/transactionstep_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/transactionstep_32x32.png";
			
			resourceBundle = getResourceBundle("res/TransactionStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("sourceTransaction", beanClass, "getSourceTransaction", "setSourceTransaction");
			properties[0].setDisplayName(getExternalizedString("property.sourceTransaction.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.sourceTransaction.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
            properties[1] = new PropertyDescriptor("connectionStringDefinition", beanClass, "getConnectionStringDefinition", "setConnectionStringDefinition");
			properties[1].setDisplayName(getExternalizedString("property.connectionStringDefinition.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.connectionStringDefinition.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			properties[1].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
