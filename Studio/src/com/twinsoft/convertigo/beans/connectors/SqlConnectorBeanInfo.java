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

public class SqlConnectorBeanInfo extends MySimpleBeanInfo {
	public SqlConnectorBeanInfo() {
		try {
			beanClass = SqlConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Connector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/sqlconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/sqlconnector_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/connectors/res/SqlConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[10];
			
			properties[0] = new PropertyDescriptor("jdbcDriverClassName", beanClass, "getJdbcDriverClassName", "setJdbcDriverClassName");
			properties[0].setDisplayName(getExternalizedString("property.jdbcDriverClassName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.jdbcDriverClassName.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			
			properties[1] = new PropertyDescriptor("jdbcURL", beanClass, "getJdbcURL", "setJdbcURL");
			properties[1].setDisplayName(getExternalizedString("property.jdbcURL.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.jdbcURL.short_description"));
			
			properties[2] = new PropertyDescriptor("jdbcUserName", beanClass, "getJdbcUserName", "setJdbcUserName");
			properties[2].setDisplayName(getExternalizedString("property.jdbcUserName.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.jdbcUserName.short_description"));
			
			properties[3] = new PropertyDescriptor("jdbcUserPassword", beanClass, "getJdbcUserPassword", "setJdbcUserPassword");
			properties[3].setDisplayName(getExternalizedString("property.jdbcUserPassword.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.jdbcUserPassword.short_description"));

			properties[4] = new PropertyDescriptor("jdbcMaxConnection", beanClass, "getJdbcMaxConnection", "setJdbcMaxConnection");
			properties[4].setDisplayName(getExternalizedString("property.jdbcMaxConnection.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.jdbcMaxConnection.short_description"));

			properties[5] = new PropertyDescriptor("systemTablesQuery", beanClass, "getSystemTablesQuery", "setSystemTablesQuery");
			properties[5].setDisplayName(getExternalizedString("property.systemTablesQuery.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.systemTablesQuery.short_description"));
			properties[5].setExpert(true);
			
			properties[6] = new PropertyDescriptor("keepConnectionAliveAfterTransaction", beanClass, "isKeepConnectionAliveAfterTransaction", "setKeepConnectionAliveAfterTransaction");
			properties[6].setDisplayName(getExternalizedString("property.keepConnectionAliveAfterTransaction.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.keepConnectionAliveAfterTransaction.short_description"));
			properties[6].setExpert(true);
			
			properties[7] = new PropertyDescriptor("idleConnectionTestTime", beanClass, "getIdleConnectionTestTime", "setIdleConnectionTestTime");
			properties[7].setDisplayName(getExternalizedString("property.idleConnectionTestTime.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.idleConnectionTestTime.short_description"));
			properties[7].setExpert(true);
			
			properties[8] = new PropertyDescriptor("connectionPool", beanClass, "getConnectionPool", "setConnectionPool");
			properties[8].setDisplayName(getExternalizedString("property.connectionPool.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.connectionPool.short_description"));
			properties[8].setExpert(true);
			
			properties[9] = new PropertyDescriptor("testOnBorrow", beanClass, "getTestOnBorrow", "setTestOnBorrow");
			properties[9].setDisplayName(getExternalizedString("property.testOnBorrow.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.testOnBorrow.short_description"));
			properties[9].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
