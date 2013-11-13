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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/beans/transactions/HttpTransactionBeanInfo.java $
 * $Author: elodiee $
 * $Revision: 33010 $
 * $Date: 2012-12-14 15:07:12 +0100 (ven., 14 déc. 2012) $
 */

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;

public class AbstractHttpTransactionBeanInfo extends MySimpleBeanInfo {
    
	public AbstractHttpTransactionBeanInfo() {
		try {
			beanClass = AbstractHttpTransaction.class;
			additionalBeanClass = TransactionWithVariables.class;

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/AbstractHttpTransaction");

			properties = new PropertyDescriptor[8];
			
			properties[0] = new PropertyDescriptor("subDir", AbstractHttpTransaction.class, "getSubDir", "setSubDir");
			properties[0].setDisplayName(getExternalizedString("property.subDir.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.subDir.short_description"));

			properties[1] = new PropertyDescriptor("handleCookie", AbstractHttpTransaction.class, "isHandleCookie", "setHandleCookie");
			properties[1].setDisplayName(getExternalizedString("property.handleCookie.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.handleCookie.short_description"));
			properties[1].setExpert(true);

			properties[2] = new PropertyDescriptor("httpParameters", AbstractHttpTransaction.class, "getHttpParameters", "setHttpParameters");
			properties[2].setDisplayName(getExternalizedString("property.httpParameters.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.httpParameters.short_description"));
			properties[2].setExpert(true);
			properties[2].setPropertyEditorClass(getEditorClass("HttpParametersEditor"));

			properties[3] = new PropertyDescriptor("requestTemplate", AbstractHttpTransaction.class, "getRequestTemplate", "setRequestTemplate");
			properties[3].setDisplayName(getExternalizedString("property.requestTemplate.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.requestTemplate.short_description"));
			properties[3].setExpert(true);

			properties[4] = new PropertyDescriptor("httpVerb", AbstractHttpTransaction.class, "getHttpVerb", "setHttpVerb");
			properties[4].setDisplayName(getExternalizedString("property.httpVerb.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.httpVerb.short_description"));
			properties[4].setPropertyEditorClass(getEditorClass("HttpVerbEditor"));
			
			properties[5] = new PropertyDescriptor("httpInfo", AbstractHttpTransaction.class, "getHttpInfo", "setHttpInfo");
			properties[5].setDisplayName(getExternalizedString("property.httpInfo.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.httpInfo.short_description"));
			properties[5].setExpert(true);
			
			properties[6] = new PropertyDescriptor("httpInfoTagName", AbstractHttpTransaction.class, "getHttpInfoTagName", "setHttpInfoTagName");
			properties[6].setDisplayName(getExternalizedString("property.httpInfoTagName.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.httpInfoTagName.short_description"));
            properties[6].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
			properties[6].setExpert(true);
			
			properties[7] = new PropertyDescriptor("urlEncodingCharset", beanClass, "getUrlEncodingCharset", "setUrlEncodingCharset");
			properties[7].setDisplayName(getExternalizedString("property.urlEncodingCharset.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.urlEncodingCharset.short_description"));
			properties[7].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

