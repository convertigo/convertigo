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

public class LegacyScreenCriteriaBeanInfo extends MySimpleBeanInfo {
    
	public LegacyScreenCriteriaBeanInfo() {
		try {
			beanClass = LegacyScreenCriteria.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Criteria.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/legacyScreenCriteria_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/legacyScreenCriteria_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/LegacyScreenCriteria");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("attribute", FindString.class, "getAttribute", "setAttribute");
			properties[0].setDisplayName(getExternalizedString("property.attribute.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.attribute.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("JavelinAttributeEditor"));
			
			properties[1] = new PropertyDescriptor("x", FindString.class, "getX", "setX");
			properties[1].setDisplayName(getExternalizedString("property.x.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.x.short_description"));
			
			properties[2] = new PropertyDescriptor("y", FindString.class, "getY", "setY");
			properties[2].setDisplayName(getExternalizedString("property.y.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.y.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
