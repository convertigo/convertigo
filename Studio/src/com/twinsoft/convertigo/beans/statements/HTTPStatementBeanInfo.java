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

package com.twinsoft.convertigo.beans.statements;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;


public class HTTPStatementBeanInfo extends MySimpleBeanInfo {
    
	public HTTPStatementBeanInfo() {
		try {
			beanClass = HTTPStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Statement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/http_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/http_32x32.png";
			
			properties = new PropertyDescriptor[10];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/statements/res/HTTPStatement");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
//			properties[0] = new PropertyDescriptor("methodType", beanClass, "getMethodType", "setMethodType");
//			properties[0].setDisplayName(getExternalizedString("property.methodType.display_name"));
//			properties[0].setShortDescription(getExternalizedString("property.methodType.short_description"));
//			//properties[0].setPropertyEditorClass(getEditorClass("HttpMethodEditor")); //since #169 Bug with http statement
//			properties[0].setHidden(true);
			properties[0] = new PropertyDescriptor("httpVerb", beanClass, "getHttpVerb", "setHttpVerb");
			properties[0].setDisplayName(getExternalizedString("property.httpVerb.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.httpVerb.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("HttpVerbEditor"));

			properties[1] = new PropertyDescriptor("https", beanClass, "isHttps", "setHttps");
			properties[1].setDisplayName(getExternalizedString("property.https.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.https.short_description"));
			
			properties[2] = new PropertyDescriptor("port", beanClass, "getPort", "setPort");
			properties[2].setDisplayName(getExternalizedString("property.port.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.port.short_description"));
			
			properties[3] = new PropertyDescriptor("host", beanClass, "getHost", "setHost");
			properties[3].setDisplayName(getExternalizedString("property.host.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.host.short_description"));
			
			properties[4] = new PropertyDescriptor("requestUri", beanClass, "getRequestUri", "setRequestUri");
			properties[4].setDisplayName(getExternalizedString("property.requestUri.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.requestUri.short_description"));
			properties[4].setValue("scriptable", Boolean.TRUE);
			
			properties[5] = new PropertyDescriptor("httpVersion", beanClass, "getHttpVersion", "setHttpVersion");
			properties[5].setDisplayName(getExternalizedString("property.httpVersion.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.httpVersion.short_description"));
			properties[5].setHidden(true);
			
			properties[6] = new PropertyDescriptor("headers", beanClass, "getHeaders", "setHeaders");
			properties[6].setDisplayName(getExternalizedString("property.headers.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.headers.short_description"));
			properties[6].setExpert(true);
			properties[6].setPropertyEditorClass(getEditorClass("HttpParametersEditor"));
			
			properties[7] = new PropertyDescriptor("orderedVariables", beanClass, "getOrderedVariables", "setOrderedVariables");
			properties[7].setDisplayName(getExternalizedString("property.orderedVariables.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.orderedVariables.short_description"));
			//properties[7].setPropertyEditorClass(getEditorClass("HttpStatementVariablesEditor"));
			//properties[7].setExpert(true);
			properties[7].setHidden(true);
			
			properties[8] = new PropertyDescriptor("trigger", beanClass, "getTrigger", "setTrigger");
			properties[8].setDisplayName(getExternalizedString("property.trigger.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.trigger.short_description"));
			properties[8].setExpert(true);
			properties[8].setPropertyEditorClass(getEditorClass("HttpTriggerEditor"));
			
			properties[9] = new PropertyDescriptor("urlEncodingCharset", beanClass, "getUrlEncodingCharset", "setUrlEncodingCharset");
			properties[9].setDisplayName(getExternalizedString("property.urlEncodingCharset.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.urlEncodingCharset.short_description"));
			properties[9].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}