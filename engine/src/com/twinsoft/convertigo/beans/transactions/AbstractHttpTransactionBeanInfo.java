/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.HttpPool;

public class AbstractHttpTransactionBeanInfo extends MySimpleBeanInfo {
    
	public AbstractHttpTransactionBeanInfo() {
		try {
			beanClass = AbstractHttpTransaction.class;
			additionalBeanClass = TransactionWithVariables.class;

			resourceBundle = getResourceBundle("res/AbstractHttpTransaction");

			properties = new PropertyDescriptor[12];
			
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
			properties[4].setPropertyEditorClass(HttpMethodType.class);
			
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
			
			properties[8] = new PropertyDescriptor("customHttpVerb", beanClass, "getCustomHttpVerb", "setCustomHttpVerb");
			properties[8].setDisplayName(getExternalizedString("property.customHttpVerb.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.customHttpVerb.short_description"));
			properties[8].setExpert(true);
			
			properties[9] = new PropertyDescriptor("httpPool", beanClass, "getHttpPool", "setHttpPool");
			properties[9].setDisplayName(getExternalizedString("property.httpPool.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.httpPool.short_description"));
			properties[9].setPropertyEditorClass(HttpPool.class);
			properties[9].setExpert(true);
			
			properties[10] = new PropertyDescriptor("allowDownloadAttachment", beanClass, "getAllowDownloadAttachment", "setAllowDownloadAttachment");
			properties[10].setDisplayName(getExternalizedString("property.allowDownloadAttachment.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.allowDownloadAttachment.short_description"));
			properties[10].setExpert(true);
			
			properties[11] = new PropertyDescriptor("followRedirect", beanClass, "isFollowRedirect", "setFollowRedirect");
			properties[11].setDisplayName(getExternalizedString("property.followRedirect.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.followRedirect.short_description"));
			properties[11].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

