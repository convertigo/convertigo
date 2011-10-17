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

public class CheckBoxBeanInfo extends MySimpleBeanInfo {

	public CheckBoxBeanInfo() {
		try {
			beanClass = CheckBox.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/checkbox_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/checkbox_32x32.gif";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/CheckBox");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[4];
			
			properties[PROPERTY_checkPattern] = new PropertyDescriptor( "checkPattern", CheckBox.class, "getCheckPattern", "setCheckPattern" );
			properties[PROPERTY_checkPattern].setDisplayName( getExternalizedString("property.checkPattern.display_name") );
			properties[PROPERTY_checkPattern].setShortDescription( getExternalizedString("property.checkPattern.short_description") );
			
			properties[PROPERTY_uncheckPattern] = new PropertyDescriptor( "uncheckPattern", CheckBox.class, "getUncheckPattern", "setUncheckPattern" );
			properties[PROPERTY_uncheckPattern].setDisplayName( getExternalizedString("property.uncheckPattern.display_name") );
			properties[PROPERTY_uncheckPattern].setShortDescription( getExternalizedString("property.uncheckPattern.short_description") );
			
			properties[PROPERTY_checkBoxDesc] = new PropertyDescriptor ( "checkBoxDesc", CheckBox.class, "getCheckBoxDesc", "setCheckBoxDesc" );
			properties[PROPERTY_checkBoxDesc].setDisplayName ( getExternalizedString("property.checkBoxDesc.display_name") );
			properties[PROPERTY_checkBoxDesc].setShortDescription ( getExternalizedString("property.checkBoxDesc.short_description") );
			properties[PROPERTY_checkBoxDesc].setPropertyEditorClass(getEditorClass("ZoneEditor"));
			
			properties[PROPERTY_Label] = new PropertyDescriptor ( "Label", CheckBox.class, "getLabel", "setLabel" );
            properties[PROPERTY_Label].setDisplayName ( getExternalizedString("property.label.display_name") );
            properties[PROPERTY_Label].setShortDescription ( getExternalizedString("property.label.short_description") );
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
	private static final int PROPERTY_checkPattern 		= 0;
	private static final int PROPERTY_uncheckPattern 	= 1;
	private static final int PROPERTY_checkBoxDesc 		= 2;
	private static final int PROPERTY_Label 			= 3;
}

