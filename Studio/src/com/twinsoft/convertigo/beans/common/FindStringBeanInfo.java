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

package com.twinsoft.convertigo.beans.common;

import java.beans.PropertyDescriptor;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class FindStringBeanInfo extends MySimpleBeanInfo {
	
	private static final int PROPERTY_string = 0;
    
	public FindStringBeanInfo() {
		try {
			beanClass = FindString.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.common.LegacyScreenCriteria.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/findstring_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/findstring_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/FindString");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[1];
			
			properties[PROPERTY_string] = new PropertyDescriptor("string", FindString.class, "getString", "setString");
			properties[PROPERTY_string].setDisplayName(getExternalizedString("property.string.display_name"));
			properties[PROPERTY_string].setShortDescription(getExternalizedString("property.string.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
