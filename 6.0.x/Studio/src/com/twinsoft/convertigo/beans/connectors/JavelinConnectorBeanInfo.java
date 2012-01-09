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

public class JavelinConnectorBeanInfo extends MySimpleBeanInfo {
    
    public JavelinConnectorBeanInfo() {
		try {
	    	beanClass = JavelinConnector.class;
	    	additionalBeanClass = com.twinsoft.convertigo.beans.core.Connector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/javelinconnector_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/javelinconnector_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/connectors/res/JavelinConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[8];
			
			properties[0] = new PropertyDescriptor("serviceCode", beanClass, "getServiceCode", "setServiceCode");
			properties[0].setDisplayName(getExternalizedString("property.serviceCode.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.serviceCode.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("CariocaServiceCodeEditor"));
			
			properties[1] = new PropertyDescriptor("virtualServer", beanClass, "getVirtualServer", "setVirtualServer");
			properties[1].setDisplayName(getExternalizedString("property.virtualServer.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.virtualServer.short_description"));
			properties[1].setExpert(true);
			
			properties[2] = new PropertyDescriptor("emulatorTechnology", beanClass, "getEmulatorTechnology", "setEmulatorTechnology");
			properties[2].setDisplayName(getExternalizedString("property.emulatorTechnology.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.emulatorTechnology.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("EmulatorTechnologyEditor"));
			
			properties[3] = new PropertyDescriptor("connectionSyncCode", beanClass, "getConnectionSyncCode", "setConnectionSyncCode");
			properties[3].setDisplayName(getExternalizedString("property.connectionSyncCode.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.connectionSyncCode.short_description"));
			properties[3].setExpert(true);
            properties[3].setPropertyEditorClass(getEditorClass("JavascriptTextEditor"));
			
			properties[4] = new PropertyDescriptor("javelinLanguage", beanClass, "getJavelinLanguage", "setJavelinLanguage");
			properties[4].setDisplayName(getExternalizedString("property.javelinLanguage.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.javelinLanguage.short_description"));
			properties[4].setExpert(true);
            properties[4].setPropertyEditorClass(getEditorClass("JavelinLanguageEditor"));
			
			properties[5] = new PropertyDescriptor("sslEnabled", beanClass, "isSslEnabled", "setSslEnabled");
			properties[5].setDisplayName(getExternalizedString("property.sslEnabled.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.sslEnabled.short_description"));
			properties[5].setExpert(true);
			
			properties[6] = new PropertyDescriptor("sslTrustAllServerCertificates", beanClass, "isSslTrustAllServerCertificates", "setSslTrustAllServerCertificates");
			properties[6].setDisplayName(getExternalizedString("property.sslTrustAllServerCertificates.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.sslTrustAllServerCertificates.short_description"));
			properties[6].setExpert(true);
			
			properties[7] = new PropertyDescriptor("ibmTerminalType", beanClass, "getIbmTerminalType", "setIbmTerminalType");
			properties[7].setDisplayName(getExternalizedString("property.ibmTerminalType.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.ibmTerminalType.short_description"));
			properties[7].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
    }

}
