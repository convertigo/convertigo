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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class PoolBeanInfo extends MySimpleBeanInfo {
    
	public PoolBeanInfo() {
		try {
			beanClass = Pool.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/pool_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/pool_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/Pool");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[6];
			
            properties[0] = new PropertyDescriptor("startTransaction", Pool.class, "getStartTransaction", "setStartTransaction");
            properties[0].setDisplayName(getExternalizedString("property.startTransaction.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.startTransaction.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("PropertyWithDynamicTagsEditor"));

            properties[1] = new PropertyDescriptor("startTransactionVariables", Pool.class, "getStartTransactionVariables", "setStartTransactionVariables");
            properties[1].setDisplayName(getExternalizedString("property.startTransactionVariables.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.startTransactionVariables.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("StartTransactionVariablesEditor"));

            properties[2] = new PropertyDescriptor("connectionsParameter", Pool.class, "getConnectionsParameter", "setConnectionsParameter");
            properties[2].setExpert(true);
            properties[2].setDisplayName(getExternalizedString("property.connectionsParameter.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.connectionsParameter.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("ConnectionsParameterEditor"));
    		properties[2].setHidden(true);

            properties[3] = new PropertyDescriptor("numberOfContexts", Pool.class, "getNumberOfContexts", "setNumberOfContexts");
            properties[3].setDisplayName(getExternalizedString("property.numberOfContexts.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.numberOfContexts.short_description"));
            
            properties[4] = new PropertyDescriptor("initialScreenClass", Pool.class, "getInitialScreenClass", "setInitialScreenClass");
            properties[4].setDisplayName(getExternalizedString("property.initialScreenClass.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.initialScreenClass.short_description"));
            properties[4].setPropertyEditorClass(getEditorClass("PropertyWithDynamicTagsEditor"));

            properties[5] = new PropertyDescriptor("serviceCode", Pool.class, "getServiceCode", "setServiceCode");
			properties[5].setDisplayName(getExternalizedString("property.serviceCode.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.serviceCode.short_description"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
