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

package com.twinsoft.convertigo.beans.mobileplatforms;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class BlackBerryBeanInfo extends MySimpleBeanInfo {

	public BlackBerryBeanInfo() {
		try {
			beanClass = BlackBerry.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.MobilePlatform.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/mobileplatforms/images/blackberry_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/mobileplatforms/images/blackberry_color_32x32.png";

			resourceBundle = getResourceBundle("res/BlackBerry");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
		
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("bbKeyTitle", BlackBerry.class, "getBbKeyTitle", "setBbKeyTitle");
			properties[0].setDisplayName(getExternalizedString("property.bbKeyTitle.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.bbKeyTitle.short_description"));
			properties[0].setExpert(true);
			
			properties[1] = new PropertyDescriptor("bbKeyPw", BlackBerry.class, "getBbKeyPw", "setBbKeyPw");
			properties[1].setDisplayName(getExternalizedString("property.bbKeyPw.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.bbKeyPw.short_description"));
			properties[1].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
