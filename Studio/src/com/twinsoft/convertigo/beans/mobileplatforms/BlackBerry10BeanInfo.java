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
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/mobiledevices/BlackBerry6BeanInfo.java $
 * $Author: julienda $
 * $Revision: 36011 $
 * $Date: 2013-12-19 10:29:48 +0100 (jeu., 19 déc. 2013) $
 */

package com.twinsoft.convertigo.beans.mobileplatforms;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class BlackBerry10BeanInfo extends MySimpleBeanInfo {

	public BlackBerry10BeanInfo() {
		try {
			beanClass = BlackBerry10.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.MobilePlatform.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/mobileplatforms/images/blackberry10_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/mobileplatforms/images/blackberry10_color_32x32.png";

			resourceBundle = getResourceBundle("res/BlackBerry10");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
		
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("bbKeyTitle", BlackBerry10.class, "getBbKeyTitle", "setBbKeyTitle");
			properties[0].setDisplayName(getExternalizedString("property.bbKeyTitle.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.bbKeyTitle.short_description"));
			properties[0].setExpert(true);
			
			properties[1] = new PropertyDescriptor("bbKeyPw", BlackBerry10.class, "getBbKeyPw", "setBbKeyPw");
			properties[1].setDisplayName(getExternalizedString("property.bbKeyPw.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.bbKeyPw.short_description"));
			properties[1].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
