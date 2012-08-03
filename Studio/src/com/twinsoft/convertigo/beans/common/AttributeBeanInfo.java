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

public class AttributeBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_attributeName = 0;
    private static final int PROPERTY_attributeValue = 1;

    public AttributeBeanInfo() {
		try {
			beanClass =  Attribute.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/tagname_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/tagname_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Attribute");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[2];
			
            properties[PROPERTY_attributeName] = new PropertyDescriptor ( "attributeName", Attribute.class, "getAttributeName", "setAttributeName" );
            properties[PROPERTY_attributeName].setDisplayName ( getExternalizedString("property.attributeName.display_name") );
            properties[PROPERTY_attributeName].setShortDescription ( getExternalizedString("property.attributeName.short_description") );

            properties[PROPERTY_attributeValue] = new PropertyDescriptor ( "attributeValue", Attribute.class, "getAttributeValue", "setAttributeValue" );
            properties[PROPERTY_attributeValue].setDisplayName ( getExternalizedString("property.attributeValue.display_name") );
            properties[PROPERTY_attributeValue].setShortDescription ( getExternalizedString("property.attributeValue.short_description") );

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
