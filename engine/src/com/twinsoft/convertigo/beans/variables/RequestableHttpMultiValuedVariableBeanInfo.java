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

package com.twinsoft.convertigo.beans.variables;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class RequestableHttpMultiValuedVariableBeanInfo extends MySimpleBeanInfo {
    
	public RequestableHttpMultiValuedVariableBeanInfo() {
		try {
			beanClass = RequestableHttpMultiValuedVariable.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.variables.RequestableHttpVariable.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/variables/images/multivaluedvariable_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/variables/images/multivaluedvariable_color_32x32.png";

			resourceBundle = getResourceBundle("res/RequestableHttpMultiValuedVariable");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			getPropertyDescriptor("bSoapArray").setHidden(false);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
