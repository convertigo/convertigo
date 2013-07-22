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

public class CredentialsStatementBeanInfo extends MySimpleBeanInfo {

	public CredentialsStatementBeanInfo() {
		try {
			beanClass = CredentialsStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Statement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/credentials_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/credentials_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/statements/res/CredentialsStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[3];

            properties[0] = new PropertyDescriptor("user", beanClass, "getUser", "setUser");
            properties[0].setDisplayName(getExternalizedString("property.user.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.user.short_description"));
            properties[0].setValue("scriptable", Boolean.TRUE);

            properties[1] = new PropertyDescriptor("password", beanClass, "getPassword", "setPassword");
            properties[1].setDisplayName(getExternalizedString("property.password.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.password.short_description"));
            properties[1].setValue("scriptable", Boolean.TRUE);
            
            properties[2] = new PropertyDescriptor("forceBasic", beanClass, "isForceBasic", "setForceBasic");
            properties[2].setDisplayName(getExternalizedString("property.forcebasic.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.forcebasic.short_description"));
            properties[2].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
		
	}
}
