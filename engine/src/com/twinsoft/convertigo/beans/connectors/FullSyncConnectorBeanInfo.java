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

package com.twinsoft.convertigo.beans.connectors;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.enums.FullSyncAnonymousReplication;

public class FullSyncConnectorBeanInfo extends MySimpleBeanInfo {
    
	public FullSyncConnectorBeanInfo() {
		try {
			beanClass = FullSyncConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.connectors.CouchDbConnector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_32x32.png";

			resourceBundle = getResourceBundle("res/FullSyncConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[1];
			
			properties[0] = new PropertyDescriptor("anonymousReplication", beanClass, "getAnonymousReplication", "setAnonymousReplication");
	        properties[0].setDisplayName(getExternalizedString("property.anonymousReplication.display_name"));
	        properties[0].setShortDescription(getExternalizedString("property.anonymousReplication.short_description"));
	        properties[0].setPropertyEditorClass(FullSyncAnonymousReplication.class);

			getPropertyDescriptor("databaseName").setHidden(true);
			getPropertyDescriptor("https").setHidden(true);
			getPropertyDescriptor("port").setHidden(true);
			getPropertyDescriptor("server").setHidden(true);
			getPropertyDescriptor("couchUsername").setHidden(true);
			getPropertyDescriptor("couchPassword").setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

