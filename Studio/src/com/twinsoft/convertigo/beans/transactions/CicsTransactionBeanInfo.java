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

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class CicsTransactionBeanInfo extends MySimpleBeanInfo {
    
	public CicsTransactionBeanInfo() {
		try {
			beanClass = CicsTransaction.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Transaction.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/cicstransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/cicstransaction_color_32x32.png";

			properties = new PropertyDescriptor[7];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/CicsTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("program", beanClass, "getProgram", "setProgram");
			properties[0].setDisplayName(getExternalizedString("property.program.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.program.short_description"));
			
			properties[1] = new PropertyDescriptor("transactionId", beanClass, "getTransactionId", "setTransactionId");
			properties[1].setDisplayName(getExternalizedString("property.transactionId.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.transactionId.short_description"));

			properties[2] = new PropertyDescriptor("inputMap", beanClass, "getInputMap", "setInputMap");
			properties[2].setExpert(true);
			properties[2].setDisplayName(getExternalizedString("property.inputMap.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.inputMap.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("CicsInputMapEditor"));
			
			properties[3] = new PropertyDescriptor("outputMap", beanClass, "getOutputMap", "setOutputMap");
			properties[3].setExpert(true);
			properties[3].setDisplayName(getExternalizedString("property.outputMap.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.outputMap.short_description"));
			properties[3].setPropertyEditorClass(getEditorClass("CicsOutputMapEditor"));

			properties[4] = new PropertyDescriptor("userId", beanClass, "getUserId", "setUserId");
			properties[4].setDisplayName(getExternalizedString("property.userId.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.userId.short_description"));

			properties[5] = new PropertyDescriptor("userPassword", beanClass, "getUserPassword", "setUserPassword");
			properties[5].setDisplayName(getExternalizedString("property.userPassword.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.userPassword.short_description"));

			properties[6] = new PropertyDescriptor("inputMapSize", beanClass, "getInputMapSize", "setInputMapSize");
			properties[6].setExpert(true);
			properties[6].setDisplayName(getExternalizedString("property.inputMapSize.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.inputMapSize.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
