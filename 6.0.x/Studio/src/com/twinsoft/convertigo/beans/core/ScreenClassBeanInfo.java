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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class ScreenClassBeanInfo extends MySimpleBeanInfo {
    
	public ScreenClassBeanInfo() {
		try {
			beanClass = ScreenClass.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/screenclass_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/screenclass_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/ScreenClass");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[2];

            properties[0] = new PropertyDescriptor("orderedCriterias", ScreenClass.class, "getOrderedCriterias", "setOrderedCriterias");
            properties[0].setDisplayName(getExternalizedString("property.orderedCriterias.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.orderedCriterias.short_description"));
            properties[0].setHidden(true);

            properties[1] = new PropertyDescriptor("orderedExtractionRules", ScreenClass.class, "getOrderedExtractionRules", "setOrderedExtractionRules");
            properties[1].setDisplayName(getExternalizedString("property.orderedExtractionRules.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.orderedExtractionRules.short_description"));
            properties[1].setHidden(true);
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
