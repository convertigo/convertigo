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

import java.beans.*;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class CicsConnectorBeanInfo extends MySimpleBeanInfo {
    
    public CicsConnectorBeanInfo() {
		try {
	    	beanClass = CicsConnector.class;
	    	additionalBeanClass = com.twinsoft.convertigo.beans.core.Connector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/cicsconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/cicsconnector_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/connectors/res/CicsConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[5];
			
			properties[0] = new PropertyDescriptor("mainframeName", beanClass, "getMainframeName", "setMainframeName");
			properties[0].setDisplayName(getExternalizedString("property.mainframeName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.mainframeName.short_description"));
			
			properties[1] = new PropertyDescriptor("server", beanClass, "getServer", "setServer");
			properties[1].setDisplayName(getExternalizedString("property.server.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.server.short_description"));
			
			properties[2] = new PropertyDescriptor("port", beanClass, "getPort", "setPort");
			properties[2].setDisplayName(getExternalizedString("property.port.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.port.short_description"));
			
			properties[3] = new PropertyDescriptor("userId", beanClass, "getUserId", "setUserId");
			properties[3].setDisplayName(getExternalizedString("property.userId.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.userId.short_description"));

			properties[4] = new PropertyDescriptor("userPassword", beanClass, "getUserPassword", "setUserPassword");
			properties[4].setDisplayName(getExternalizedString("property.userPassword.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.userPassword.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
    }

}
