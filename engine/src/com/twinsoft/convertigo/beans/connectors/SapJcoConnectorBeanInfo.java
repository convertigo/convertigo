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

public class SapJcoConnectorBeanInfo extends MySimpleBeanInfo {
	public SapJcoConnectorBeanInfo() {
		try {
			beanClass = SapJcoConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Connector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/sapconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/sapconnector_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/connectors/res/SapJcoConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[10];
			
			properties[0] = new PropertyDescriptor("ashost", beanClass, "getAsHost", "setAsHost");
			properties[0].setDisplayName(getExternalizedString("property.ashost.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.ashost.short_description"));
			
			properties[1] = new PropertyDescriptor("systemNumber", beanClass, "getSystemNumber", "setSystemNumber");
			properties[1].setDisplayName(getExternalizedString("property.systemnumber.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.systemnumber.short_description"));
			
			properties[2] = new PropertyDescriptor("client", beanClass, "getClient", "setClient");
			properties[2].setDisplayName(getExternalizedString("property.client.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.client.short_description"));
			
			properties[3] = new PropertyDescriptor("user", beanClass, "getUser", "setUser");
			properties[3].setDisplayName(getExternalizedString("property.user.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.user.short_description"));

			properties[4] = new PropertyDescriptor("password", beanClass, "getPassword", "setPassword");
			properties[4].setDisplayName(getExternalizedString("property.password.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.password.short_description"));

			properties[5] = new PropertyDescriptor("language", beanClass, "getLanguage", "setLanguage");
			properties[5].setDisplayName(getExternalizedString("property.language.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.language.short_description"));

			properties[6] = new PropertyDescriptor("systemId", beanClass, "getSystemId", "setSystemId");
			properties[6].setDisplayName(getExternalizedString("property.systemId.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.systemId.short_description"));

			properties[7] = new PropertyDescriptor("msService", beanClass, "getMsService", "setMsService");
			properties[7].setDisplayName(getExternalizedString("property.msService.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.msService.short_description"));

			properties[8] = new PropertyDescriptor("msHost", beanClass, "getMsHost", "setMsHost");
			properties[8].setDisplayName(getExternalizedString("property.msHost.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.msHost.short_description"));

			properties[9] = new PropertyDescriptor("group", beanClass, "getGroup", "setGroup");
			properties[9].setDisplayName(getExternalizedString("property.group.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.group.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
