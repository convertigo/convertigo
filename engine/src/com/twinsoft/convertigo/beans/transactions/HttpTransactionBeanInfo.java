/*
 * Copyright (c) 2001-2023 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class HttpTransactionBeanInfo extends MySimpleBeanInfo {
	
	public HttpTransactionBeanInfo() {
		try {
			beanClass = HttpTransaction.class;
			additionalBeanClass = AbstractHttpTransaction.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/httptransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/httptransaction_color_32x32.png";

			properties = new PropertyDescriptor[3];
			
			resourceBundle = getResourceBundle("res/HttpTransaction");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("dataEncoding", beanClass, "getDataEncoding", "setDataEncoding");
			properties[0].setDisplayName(getExternalizedString("property.dataEncoding.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.dataEncoding.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("HttpDataEncodingEditor"));
			
			properties[1] = new PropertyDescriptor("dataStringCharset", beanClass, "getDataStringCharset", "setDataStringCharset");
			properties[1].setDisplayName(getExternalizedString("property.dataStringCharset.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.dataStringCharset.short_description"));
			
			properties[2] = new PropertyDescriptor("responseInCDATA", beanClass, "isResponseInCDATA", "setResponseInCDATA");
			properties[2].setDisplayName(getExternalizedString("property.responseInCDATA.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.responseInCDATA.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

