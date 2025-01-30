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

package com.twinsoft.convertigo.beans.connectors;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class CouchDbConnectorBeanInfo extends MySimpleBeanInfo {
    
	public CouchDbConnectorBeanInfo() {
		try {
			beanClass = CouchDbConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Connector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/couchdbconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/couchdbconnector_color_32x32.png";

			resourceBundle = getResourceBundle("res/CouchDbConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[8];
			
			properties[0] = new PropertyDescriptor("databaseName", beanClass, "getDatabaseName", "setDatabaseName");
			properties[0].setDisplayName(getExternalizedString("property.databaseName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.databaseName.short_description"));

			properties[1] = new PropertyDescriptor("https", beanClass, "isHttps", "setHttps");
			properties[1].setDisplayName(getExternalizedString("property.https.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.https.short_description"));
			
			properties[2] = new PropertyDescriptor("port", beanClass, "getPort", "setPort");
			properties[2].setDisplayName(getExternalizedString("property.port.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.port.short_description"));
			
			properties[3] = new PropertyDescriptor("server", beanClass, "getServer", "setServer");
			properties[3].setDisplayName(getExternalizedString("property.server.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.server.short_description"));
			
			properties[4] = new PropertyDescriptor("couchUsername", beanClass, "getCouchUsername", "setCouchUsername");
			properties[4].setDisplayName(getExternalizedString("property.couchUsername.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.couchUsername.short_description"));
			
			properties[5] = new PropertyDescriptor("couchPassword", beanClass, "getCouchPassword", "setCouchPassword");
			properties[5].setDisplayName(getExternalizedString("property.couchPassword.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.couchPassword.short_description"));
			
			properties[6] = new PropertyDescriptor("jsonUseType", beanClass, "isJsonUseType", "setJsonUseType");
			properties[6].setDisplayName(getExternalizedString("property.jsonUseType.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.jsonUseType.short_description"));
			
			properties[7] = new PropertyDescriptor("secureDatabase", beanClass, "isSecureDatabase", "setSecureDatabase");
			properties[7].setDisplayName(getExternalizedString("property.secureDatabase.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.secureDatabase.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

