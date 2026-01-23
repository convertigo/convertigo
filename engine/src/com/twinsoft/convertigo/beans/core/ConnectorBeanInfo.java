/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class ConnectorBeanInfo extends MySimpleBeanInfo {
    
	public ConnectorBeanInfo() {
		try {
			beanClass = Connector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			resourceBundle = getResourceBundle("res/Connector");

			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("endTransactionName", Connector.class, "getEndTransactionName", "setEndTransactionName");
			properties[0].setDisplayName(getExternalizedString("property.endTransactionName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.endTransactionName.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("PropertyWithDynamicTagsEditor"));
			properties[0].setExpert(true);
			
			properties[1] = new PropertyDescriptor("isTasAuthenticationRequired", Connector.class, "isTasAuthenticationRequired", "setTasAuthenticationRequired");
			properties[1].setDisplayName(getExternalizedString("property.isTasAuthenticationRequired.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.isTasAuthenticationRequired.short_description"));
			properties[1].setExpert(true);
			
			// TODO: combo box to choose the billing class from a configuration file
			properties[2] = new PropertyDescriptor("billingClassName", Connector.class, "getBillingClassName", "setBillingClassName");
			properties[2].setDisplayName(getExternalizedString("property.billingClassName.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.billingClassName.short_description"));
			properties[2].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

