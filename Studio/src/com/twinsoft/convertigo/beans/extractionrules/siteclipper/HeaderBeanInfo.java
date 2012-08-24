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

package com.twinsoft.convertigo.beans.extractionrules.siteclipper;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class HeaderBeanInfo extends MySimpleBeanInfo {

	public HeaderBeanInfo() {
		try {
			beanClass = Header.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.siteclipper.BaseRule.class;

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/extractionrules/siteclipper/res/Header");

			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("headerName", beanClass, "getHeaderName", "setHeaderName" );
			properties[0].setDisplayName(getExternalizedString("property.headerName.display_name") );
			properties[0].setShortDescription(getExternalizedString("property.headerName.short_description") );
			
			properties[1] = new PropertyDescriptor("headerValue", beanClass, "getHeaderValue", "setHeaderValue" );
			properties[1].setDisplayName(getExternalizedString("property.headerValue.display_name") );
			properties[1].setShortDescription(getExternalizedString("property.headerValue.short_description") );
			properties[1].setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
