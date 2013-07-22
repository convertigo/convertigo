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

package com.twinsoft.convertigo.beans.connectors;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class HttpConnectorBeanInfo extends MySimpleBeanInfo {
    
	public HttpConnectorBeanInfo() {
		try {
			beanClass = HttpConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Connector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/httpconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/httpconnector_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/connectors/res/HttpConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[9];
			
			properties[0] = new PropertyDescriptor("baseDir", beanClass, "getBaseDir", "setBaseDir");
			properties[0].setDisplayName(getExternalizedString("property.baseDir.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.baseDir.short_description"));

			properties[1] = new PropertyDescriptor("https", beanClass, "isHttps", "setHttps");
			properties[1].setDisplayName(getExternalizedString("property.https.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.https.short_description"));
			
			properties[2] = new PropertyDescriptor("port", beanClass, "getPort", "setPort");
			properties[2].setDisplayName(getExternalizedString("property.port.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.port.short_description"));
			
			properties[3] = new PropertyDescriptor("server", beanClass, "getServer", "setServer");
			properties[3].setDisplayName(getExternalizedString("property.server.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.server.short_description"));
		
			properties[4] = new PropertyDescriptor("trustAllServerCertificates", beanClass, "isTrustAllServerCertificates", "setTrustAllServerCertificates");
			properties[4].setDisplayName(getExternalizedString("property.trustAllServerCertificates.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.trustAllServerCertificates.short_description"));
			
			properties[5] = new PropertyDescriptor("basicUser", beanClass, "getBasicUser", "setBasicUser");
			properties[5].setDisplayName(getExternalizedString("property.basicUser.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.basicUser.short_description"));
			properties[5].setExpert(true);
			
			properties[6] = new PropertyDescriptor("basicPassword", beanClass, "getBasicPassword", "setBasicPassword");
			properties[6].setDisplayName(getExternalizedString("property.basicPassword.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.basicPassword.short_description"));
			properties[6].setExpert(true);
			
			properties[7] = new PropertyDescriptor("httpHeaderForward", beanClass, "getHttpHeaderForward", "setHttpHeaderForward");
			properties[7].setDisplayName(getExternalizedString("property.httpHeaderForward.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.httpHeaderForward.short_description"));
			properties[7].setExpert(true);
			properties[7].setPropertyEditorClass(getEditorClass("HttpHeaderForwardEditor"));
			
			properties[8] = new PropertyDescriptor("urlEncodingCharset", beanClass, "getUrlEncodingCharset", "setUrlEncodingCharset");
			properties[8].setDisplayName(getExternalizedString("property.urlEncodingCharset.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.urlEncodingCharset.short_description"));
			properties[8].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

