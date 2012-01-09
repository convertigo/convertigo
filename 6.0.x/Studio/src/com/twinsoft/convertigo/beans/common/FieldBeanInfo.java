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

public class FieldBeanInfo extends MySimpleBeanInfo {

	public FieldBeanInfo() {
		try {
			beanClass = Field.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/editfield_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/editfield_color_32x32.gif";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Field");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[5];
			
            properties[PROPERTY_Value] = new PropertyDescriptor ( "Value", Field.class, "getFieldValue", "setFieldValue" );
            properties[PROPERTY_Value].setDisplayName ( getExternalizedString("property.value.display_name") );
            properties[PROPERTY_Value].setShortDescription ( getExternalizedString("property.value.short_description") );
			
            properties[PROPERTY_Type] = new PropertyDescriptor ( "Type", Field.class, "getFieldType", "setFieldType" );
			properties[PROPERTY_Type].setDisplayName ( getExternalizedString("property.type.display_name") );
			properties[PROPERTY_Type].setShortDescription ( getExternalizedString("property.type.short_description") );
			
			properties[PROPERTY_Name] = new PropertyDescriptor ( "Name", Field.class, "getFieldName", "setFieldName" );
			properties[PROPERTY_Name].setDisplayName ( getExternalizedString("property.name.display_name") );
			properties[PROPERTY_Name].setShortDescription ( getExternalizedString("property.name.short_description") );
			
			properties[PROPERTY_fieldDesc] = new PropertyDescriptor ( "fieldDesc", Field.class, "getFieldDesc", "setFieldDesc" );
			properties[PROPERTY_fieldDesc].setDisplayName ( getExternalizedString("property.fielddesc.display_name") );
			properties[PROPERTY_fieldDesc].setShortDescription ( getExternalizedString("property.fielddesc.short_description") );
			properties[PROPERTY_fieldDesc].setPropertyEditorClass(getEditorClass("ZoneEditor"));
			
			properties[PROPERTY_fieldAttrb] = new PropertyDescriptor ( "fieldAttrb", Field.class, "getFieldAttrb", "setFieldAttrb" );
			properties[PROPERTY_fieldAttrb].setDisplayName ( getExternalizedString("property.fieldattrb.display_name") );
			properties[PROPERTY_fieldAttrb].setShortDescription ( getExternalizedString("property.fieldattrb.short_description") );
			properties[PROPERTY_fieldAttrb].setPropertyEditorClass(getEditorClass("JavelinAttributeEditor"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
    private static final int PROPERTY_Value 		= 0;
	private static final int PROPERTY_Type 			= 1;
	private static final int PROPERTY_Name 			= 2;
	private static final int PROPERTY_fieldDesc 	= 3;
	private static final int PROPERTY_fieldAttrb 	= 4;
	
}

