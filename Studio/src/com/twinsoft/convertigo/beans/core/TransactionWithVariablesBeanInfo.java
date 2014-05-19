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

public class TransactionWithVariablesBeanInfo extends MySimpleBeanInfo {
    
	public TransactionWithVariablesBeanInfo() {
		try {
			beanClass =  TransactionWithVariables.class;
			additionalBeanClass = Transaction.class;

			resourceBundle = getResourceBundle("res/TransactionWithVariables");

			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("orderedVariables", TransactionWithVariables.class, "getOrderedVariables", "setOrderedVariables");
			properties[0].setDisplayName(getExternalizedString("property.orderedVariables.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.orderedVariables.short_description"));
			//properties[0].setPropertyEditorClass(getEditorClass("TransactionVariablesEditor"));
			//properties[0].setExpert(true);
			properties[0].setHidden(true);
			
			properties[1] = new PropertyDescriptor("bIncludeCertificateGroup", TransactionWithVariables.class, "includeCertificateGroup", "setIncludeCertificateGroup");
			properties[1].setExpert(true);
			properties[1].setDisplayName(getExternalizedString("property.bIncludeCertificateGroup.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.bIncludeCertificateGroup.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
