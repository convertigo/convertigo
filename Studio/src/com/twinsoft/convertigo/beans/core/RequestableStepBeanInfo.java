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

public class RequestableStepBeanInfo extends MySimpleBeanInfo {
    
	public RequestableStepBeanInfo() {
		try {
			beanClass = RequestableStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			resourceBundle = getResourceBundle("res/RequestableStep");
			
			properties = new PropertyDescriptor[3];
			
            properties[0] = new PropertyDescriptor("contextName", beanClass, "getStepContextName", "setStepContextName");
			properties[0].setDisplayName(getExternalizedString("property.contextName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.contextName.short_description"));
			properties[0].setValue("scriptable", Boolean.TRUE);

			properties[1] = new PropertyDescriptor("orderedVariables", beanClass, "getOrderedVariables", "setOrderedVariables");
			properties[1].setDisplayName(getExternalizedString("property.orderedVariables.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.orderedVariables.short_description"));
			//properties[1].setPropertyEditorClass(getEditorClass("TransactionStepVariablesEditor"));
			//properties[1].setExpert(true);
			properties[1].setHidden(true);
			
            properties[2] = new PropertyDescriptor("bInternalInvoke", beanClass, "isInternalInvoke", "setInternalInvoke");
			properties[2].setDisplayName(getExternalizedString("property.bInternalInvoke.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.bInternalInvoke.short_description"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
