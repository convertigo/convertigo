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
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;

public class HttpTransactionBeanInfo extends MySimpleBeanInfo {
    
	public HttpTransactionBeanInfo() {
		try {
			beanClass = HttpTransaction.class;
			additionalBeanClass = TransactionWithVariables.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/transaction_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/transaction_color_32x32.gif";

			properties = new PropertyDescriptor[6];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/HttpTransaction");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("subDir", HttpTransaction.class, "getSubDir", "setSubDir");
			properties[0].setDisplayName(getExternalizedString("property.subDir.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.subDir.short_description"));

			properties[1] = new PropertyDescriptor("handleCookie", HttpTransaction.class, "isHandleCookie", "setHandleCookie");
			properties[1].setDisplayName(getExternalizedString("property.handleCookie.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.handleCookie.short_description"));
			properties[1].setExpert(true);

			properties[2] = new PropertyDescriptor("httpParameters", HttpTransaction.class, "getHttpParameters", "setHttpParameters");
			properties[2].setDisplayName(getExternalizedString("property.httpParameters.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.httpParameters.short_description"));
			properties[2].setExpert(true);
			properties[2].setPropertyEditorClass(getEditorClass("HttpParametersEditor"));

			// Overide variableDefinition with enhanced editor
			//properties[3] = new PropertyDescriptor("variablesDefinition", TransactionWithVariables.class, "getVariablesDefinition", "setVariablesDefinition");
			//properties[3].setPropertyEditorClass(getEditorClass("HttpTransactionVariablesEditor"));
			
			properties[3] = new PropertyDescriptor("requestTemplate", HttpTransaction.class, "getRequestTemplate", "setRequestTemplate");
			properties[3].setDisplayName(getExternalizedString("property.requestTemplate.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.requestTemplate.short_description"));
			properties[3].setExpert(true);

			properties[4] = new PropertyDescriptor("httpVerb", HttpTransaction.class, "getHttpVerb", "setHttpVerb");
			properties[4].setDisplayName(getExternalizedString("property.httpVerb.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.httpVerb.short_description"));
			properties[4].setPropertyEditorClass(getEditorClass("HttpVerbEditor"));

			properties[5] = new PropertyDescriptor("dataEncoding", HttpTransaction.class, "getDataEncoding", "setDataEncoding");
			properties[5].setDisplayName(getExternalizedString("property.dataEncoding.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.dataEncoding.short_description"));
			properties[5].setPropertyEditorClass(getEditorClass("HttpDataEncodingEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

