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

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.enums.LdapBindingPolicy;

public class LDAPAuthenticationStepBeanInfo extends MySimpleBeanInfo {
    
	public LDAPAuthenticationStepBeanInfo() {
		try {
			beanClass = LDAPAuthenticationStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/setAuthenticatedUser_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/setAuthenticatedUser_32x32.png";
			
			resourceBundle = getResourceBundle("res/LDAPAuthenticationStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[8];

            properties[0] = new PropertyDescriptor("server", beanClass, "getServer", "setServer");
            properties[0].setDisplayName(getExternalizedString("property.server.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.server.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[1] = new PropertyDescriptor("login", beanClass, "getLogin", "setLogin");
            properties[1].setDisplayName(getExternalizedString("property.login.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.login.short_description"));
            properties[1].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[2] = new PropertyDescriptor("password", beanClass, "getPassword", "setPassword");
            properties[2].setDisplayName(getExternalizedString("property.password.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.password.short_description"));
            properties[2].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[3] = new PropertyDescriptor("adminLogin", beanClass, "getAdminLogin", "setAdminLogin");
            properties[3].setDisplayName(getExternalizedString("property.adminLogin.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.adminLogin.short_description"));
            properties[3].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            
            properties[4] = new PropertyDescriptor("adminPassword", beanClass, "getAdminPassword", "setAdminPassword");
            properties[4].setDisplayName(getExternalizedString("property.adminPassword.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.adminPassword.short_description"));
            properties[4].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));

            properties[5] = new PropertyDescriptor("basePath", beanClass, "getBasePath", "setBasePath");
            properties[5].setDisplayName(getExternalizedString("property.basePath.display_name"));
            properties[5].setShortDescription(getExternalizedString("property.basePath.short_description"));
            properties[5].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));

            properties[6] = new PropertyDescriptor("bindingPolicy", beanClass, "getBindingPolicy", "setBindingPolicy");
            properties[6].setDisplayName(getExternalizedString("property.bindingPolicy.display_name"));
            properties[6].setShortDescription(getExternalizedString("property.bindingPolicy.short_description"));
            properties[6].setPropertyEditorClass(LdapBindingPolicy.class);

            properties[7] = new PropertyDescriptor("attributes", beanClass, "getAttributes", "setAttributes");
            properties[7].setDisplayName(getExternalizedString("property.attributes.display_name"));
            properties[7].setShortDescription(getExternalizedString("property.attributes.short_description"));
            properties[7].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
            properties[7].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
