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

public class SliderBeanInfo extends MySimpleBeanInfo {

	public SliderBeanInfo() {
		try {
			beanClass = Slider.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/slider_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/slider_32x32.png";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Slider");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[4];
			
            properties[PROPERTY_startPattern] = new PropertyDescriptor ( "startPattern", Slider.class, "getStartPattern", "setStartPattern" );
            properties[PROPERTY_startPattern].setDisplayName ( getExternalizedString("property.startPattern.display_name") );
            properties[PROPERTY_startPattern].setShortDescription ( getExternalizedString("property.startPattern.short_description") );
            
            properties[PROPERTY_endPattern] = new PropertyDescriptor ( "endPattern", Slider.class, "getEndPattern", "setEndPattern" );
            properties[PROPERTY_endPattern].setDisplayName ( getExternalizedString("property.endPattern.display_name") );
            properties[PROPERTY_endPattern].setShortDescription ( getExternalizedString("property.endPattern.short_description") );
            
            properties[PROPERTY_screenShowedPattern] = new PropertyDescriptor ( "screenShowedPattern", Slider.class, "getScreenShowedPattern", "setScreenShowedPattern" );
            properties[PROPERTY_screenShowedPattern].setDisplayName ( getExternalizedString("property.screenShowedPattern.display_name") );
            properties[PROPERTY_screenShowedPattern].setShortDescription ( getExternalizedString("property.screenShowedPattern.short_description") );
            
            properties[PROPERTY_screenHiddenPattern] = new PropertyDescriptor ( "screenHiddenPattern", Slider.class, "getScreenHiddenPattern", "setScreenHiddenPattern" );
            properties[PROPERTY_screenHiddenPattern].setDisplayName ( getExternalizedString("property.screenHiddenPattern.display_name") );
            properties[PROPERTY_screenHiddenPattern].setShortDescription ( getExternalizedString("property.screenHiddenPattern.short_description") );
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
    private static final int PROPERTY_startPattern 			= 0;
	private static final int PROPERTY_endPattern 			= 1;
	private static final int PROPERTY_screenShowedPattern 	= 2;
	private static final int PROPERTY_screenHiddenPattern 	= 3;
}

