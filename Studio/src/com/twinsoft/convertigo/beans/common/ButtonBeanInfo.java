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

public class ButtonBeanInfo extends MySimpleBeanInfo {

	public ButtonBeanInfo() {
		try {
			beanClass = Button.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/button_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/button_color_32x32.png";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Button");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[6];
			
            properties[PROPERTY_Label] = new PropertyDescriptor ( "Label", Button.class, "getLabel", "setLabel" );
            properties[PROPERTY_Label].setDisplayName ( getExternalizedString("property.label.display_name") );
            properties[PROPERTY_Label].setShortDescription ( getExternalizedString("property.label.short_description") );
			
            properties[PROPERTY_Action] = new PropertyDescriptor ( "Action", Button.class, "getAction", "setAction" );
			properties[PROPERTY_Action].setDisplayName ( getExternalizedString("property.action.display_name") );
			properties[PROPERTY_Action].setShortDescription ( getExternalizedString("property.action.short_description") );
			
			properties[PROPERTY_DoTransaction] = new PropertyDescriptor ( "DoTransaction", Button.class, "isDoTransaction", "setDoTransaction" );
			properties[PROPERTY_DoTransaction].setDisplayName ( getExternalizedString("property.dotransaction.display_name") );
			properties[PROPERTY_DoTransaction].setShortDescription ( getExternalizedString("property.dotransaction.short_description") );
			
			properties[PROPERTY_ButtonDesc] = new PropertyDescriptor ( "buttonDesc", Button.class, "getButtonDesc", "setButtonDesc" );
			properties[PROPERTY_ButtonDesc].setDisplayName ( getExternalizedString("property.buttondesc.display_name") );
			properties[PROPERTY_ButtonDesc].setShortDescription ( getExternalizedString("property.buttondesc.short_description") );
			properties[PROPERTY_ButtonDesc].setPropertyEditorClass(getEditorClass("ZoneEditor"));
			
			properties[PROPERTY_startPattern] = new PropertyDescriptor ( "startPattern", Button.class, "getStartPattern", "setStartPattern" );
            properties[PROPERTY_startPattern].setDisplayName ( getExternalizedString("property.startPattern.display_name") );
            properties[PROPERTY_startPattern].setShortDescription ( getExternalizedString("property.startPattern.short_description") );
            
            properties[PROPERTY_endPattern] = new PropertyDescriptor ( "endPattern", Button.class, "getEndPattern", "setEndPattern" );
            properties[PROPERTY_endPattern].setDisplayName ( getExternalizedString("property.endPattern.display_name") );
            properties[PROPERTY_endPattern].setShortDescription ( getExternalizedString("property.endPattern.short_description") );
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
    private static final int PROPERTY_Label 		= 0;
	private static final int PROPERTY_Action 		= 1;
	private static final int PROPERTY_DoTransaction = 2;
	private static final int PROPERTY_ButtonDesc 	= 3;
	private static final int PROPERTY_startPattern 	= 4;
	private static final int PROPERTY_endPattern 	= 5;
}

