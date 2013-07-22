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

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class TabBoxBeanInfo extends MySimpleBeanInfo {

	public TabBoxBeanInfo() {
		try {
			beanClass = TabBox.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/tabbox_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/tabbox_color_32x32.png";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/TabBox");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[1];
			
			properties[PROPERTY_ExtendedMode] = new PropertyDescriptor ( "extendedMode", TabBox.class, "isExtendedMode", "setExtendedMode" );
			properties[PROPERTY_ExtendedMode].setDisplayName ( getExternalizedString("property.extendedMode.display_name") );
			properties[PROPERTY_ExtendedMode].setShortDescription ( getExternalizedString("property.extendedMode.short_description") );
					
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

	private static final int PROPERTY_ExtendedMode = 0;

}

