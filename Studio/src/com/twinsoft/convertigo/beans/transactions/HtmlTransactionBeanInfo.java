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

public class HtmlTransactionBeanInfo extends MySimpleBeanInfo {
    
	public HtmlTransactionBeanInfo() {
		try {
			beanClass = HtmlTransaction.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.transactions.HttpTransaction.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/htmltransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/htmltransaction_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/HtmlTransaction");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties = new PropertyDescriptor[2];
			
			// Overide variableDefinition with enhanced editor
			//properties[0] = new PropertyDescriptor("variablesDefinition", HttpTransaction.class, "getVariablesDefinition", "setVariablesDefinition");
			//properties[0].setPropertyEditorClass(getEditorClass("HtmlTransactionVariablesEditor"));
			
			properties[0] = new PropertyDescriptor("stateFull", HtmlTransaction.class, "isStateFull", "setStateFull");
			properties[0].setDisplayName(getExternalizedString("property.stateFull.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.stateFull.short_description"));
			properties[0].setExpert(true);
			
			properties[1] = new PropertyDescriptor("trigger", beanClass, "getTrigger", "setTrigger");
			properties[1].setDisplayName(getExternalizedString("property.trigger.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.trigger.short_description"));
			properties[1].setExpert(true);
			properties[1].setPropertyEditorClass(getEditorClass("HttpTriggerEditor"));            
			
			PropertyDescriptor property = getPropertyDescriptor("subDir");
            property.setDisplayName(getExternalizedString("property.subDir.display_name"));
            property.setShortDescription(getExternalizedString("property.subDir.short_description"));
            
            property = getPropertyDescriptor("httpParameters");
            property.setDisplayName(getExternalizedString("property.httpParameters.display_name"));
            property.setShortDescription(getExternalizedString("property.httpParameters.short_description"));
            
            property = getPropertyDescriptor("dataEncoding");
            property.setHidden(true);
			
            property = getPropertyDescriptor("httpInfo");
            property.setHidden(true);
            
            property = getPropertyDescriptor("httpInfoTagName");
            property.setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
