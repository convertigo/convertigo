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

package com.twinsoft.convertigo.beans.sna;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class NptuiBeanInfo extends MySimpleBeanInfo {    

	public NptuiBeanInfo() {
		try {
			beanClass = Nptui.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/sna/images/nptui_color_16x16.gif";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/sna/images/nptui_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/sna/res/Nptui");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[7];
			
            properties[PROPERTY_window] = new PropertyDescriptor ( "bWindow", Nptui.class, "isBWindow", "setBWindow" );
            properties[PROPERTY_window].setDisplayName ( getExternalizedString("property.window.display_name") );
            properties[PROPERTY_window].setShortDescription ( getExternalizedString("property.window.short_description") );
            
            properties[PROPERTY_choice] = new PropertyDescriptor ( "bChoice", Nptui.class, "isBChoice", "setBChoice" );
            properties[PROPERTY_choice].setDisplayName ( getExternalizedString("property.choice.display_name") );
            properties[PROPERTY_choice].setShortDescription ( getExternalizedString("property.choice.short_description") );
            
            properties[PROPERTY_scrollBar] = new PropertyDescriptor ( "bScrollBar", Nptui.class, "isBScrollBar", "setBScrollBar" );
            properties[PROPERTY_scrollBar].setDisplayName ( getExternalizedString("property.scrollBar.display_name") );
            properties[PROPERTY_scrollBar].setShortDescription ( getExternalizedString("property.scrollBar.short_description") );
            
            properties[PROPERTY_button] = new PropertyDescriptor ( "bButton", Nptui.class, "isBButton", "setBButton" );
            properties[PROPERTY_button].setDisplayName ( getExternalizedString("property.button.display_name") );
            properties[PROPERTY_button].setShortDescription ( getExternalizedString("property.button.short_description") );
            
            properties[PROPERTY_checkbox] = new PropertyDescriptor ( "bCheckbox", Nptui.class, "isBCheckbox", "setBCheckbox" );
            properties[PROPERTY_checkbox].setDisplayName ( getExternalizedString("property.checkbox.display_name") );
            properties[PROPERTY_checkbox].setShortDescription ( getExternalizedString("property.checkbox.short_description") );
            
            properties[PROPERTY_menu] = new PropertyDescriptor ( "bMenu", Nptui.class, "isBMenu", "setBMenu" );
            properties[PROPERTY_menu].setDisplayName ( getExternalizedString("property.menu.display_name") );
            properties[PROPERTY_menu].setShortDescription ( getExternalizedString("property.menu.short_description") );
            
            properties[PROPERTY_radio] = new PropertyDescriptor ( "bRadio", Nptui.class, "isBRadio", "setBRadio" );
            properties[PROPERTY_radio].setDisplayName ( getExternalizedString("property.radio.display_name") );
            properties[PROPERTY_radio].setShortDescription ( getExternalizedString("property.radio.short_description") );
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

    private static final int PROPERTY_window 		= 0;
    private static final int PROPERTY_choice 		= 1;
    private static final int PROPERTY_scrollBar 	= 2;
    private static final int PROPERTY_button 		= 3;
    private static final int PROPERTY_checkbox 		= 4;
    private static final int PROPERTY_menu 			= 5;
    private static final int PROPERTY_radio 		= 6;
    
}

