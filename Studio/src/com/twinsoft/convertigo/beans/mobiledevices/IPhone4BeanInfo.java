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

package com.twinsoft.convertigo.beans.mobiledevices;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MobileDevice;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class IPhone4BeanInfo extends MySimpleBeanInfo {

	public IPhone4BeanInfo() {
		try {
			beanClass = IPhone4.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.MobileDevice.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/mobiledevices/images/apple_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/mobiledevices/images/apple_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/mobiledevices/res/IPhone4");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("iOSCertificateTitle", IPhone4.class, "getiOSCertificateTitle", "setiOSCertificateTitle");
			properties[0].setDisplayName(getExternalizedString("property.iOSCertificateTitle.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.iOSCertificateTitle.short_description"));
			properties[0].setExpert(false);
			
			properties[1] = new PropertyDescriptor("iOSCertificatePw", IPhone4.class, "getiOSCertificatePw", "setiOSCertificatePw");
			properties[1].setDisplayName(getExternalizedString("property.iOSCertificatePw.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.iOSCertificatePw.short_description"));
			properties[1].setExpert(false);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
