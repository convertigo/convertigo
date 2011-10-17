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

public class TransactionBeanInfo extends MySimpleBeanInfo {
    
	public TransactionBeanInfo() {
		try {
			beanClass =  Transaction.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.RequestableObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/transaction_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/transaction_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/Transaction");
			
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("sheetLocation", beanClass, "getSheetLocation", "setSheetLocation");
			properties[0].setPropertyEditorClass(getEditorClass("TransactionSheetLocationEditor"));

			properties[1] = new PropertyDescriptor("includedTagAttributes", beanClass, "getIncludedTagAttributes", "setIncludedTagAttributes");
			properties[1].setDisplayName(getExternalizedString("property.includedTagAttributes.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.includedTagAttributes.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("IncludedTagAttributesEditor"));

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
