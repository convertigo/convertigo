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

public class StyleBeanInfo extends MySimpleBeanInfo {

	public StyleBeanInfo() {
		try {
			beanClass = Style.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/changelettercase_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/changelettercase_color_32x32.gif";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Style");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[11];
			
			properties[PROPERTY_fontSize] = new PropertyDescriptor( "fontSize", Style.class, "getFontSize", "setFontSize" );
			properties[PROPERTY_fontSize].setDisplayName( getExternalizedString("property.fontSize.display_name") );
			properties[PROPERTY_fontSize].setShortDescription( getExternalizedString("property.fontSize.short_description") );
			
			properties[PROPERTY_fontName] = new PropertyDescriptor ( "fontName", Style.class, "getFontName", "setFontName" );
			properties[PROPERTY_fontName].setDisplayName ( getExternalizedString("property.fontName.display_name") );
			properties[PROPERTY_fontName].setShortDescription ( getExternalizedString("property.fontName.short_description") );
			properties[PROPERTY_fontName].setPropertyEditorClass( getEditorClass("FontEditor") );
			
			properties[PROPERTY_bold] = new PropertyDescriptor ( "bold", Style.class, "isBold", "setBold" );
			properties[PROPERTY_bold].setDisplayName ( getExternalizedString("property.bold.display_name") );
			properties[PROPERTY_bold].setShortDescription ( getExternalizedString("property.bold.short_description") );
			
			properties[PROPERTY_italic] = new PropertyDescriptor ( "italic", Style.class, "isItalic", "setItalic" );
			properties[PROPERTY_italic].setDisplayName ( getExternalizedString("property.italic.display_name") );
			properties[PROPERTY_italic].setShortDescription ( getExternalizedString("property.italic.short_description") );
			
			properties[PROPERTY_underlined] = new PropertyDescriptor ( "underlined", Style.class, "isUnderlined", "setUnderlined" );
			properties[PROPERTY_underlined].setDisplayName ( getExternalizedString("property.underlined.display_name") );
			properties[PROPERTY_underlined].setShortDescription ( getExternalizedString("property.underlined.short_description") );
			
			properties[PROPERTY_color] = new PropertyDescriptor ( "color", Style.class, "getColor", "setColor" );
			properties[PROPERTY_color].setDisplayName ( getExternalizedString("property.color.display_name") );
			properties[PROPERTY_color].setShortDescription ( getExternalizedString("property.color.short_description") );
			properties[PROPERTY_color].setPropertyEditorClass( getEditorClass("ColorEditor") );
			
			properties[PROPERTY_bgColor] = new PropertyDescriptor ( "bgColor", Style.class, "getBgColor", "setBgColor" );
			properties[PROPERTY_bgColor].setDisplayName ( getExternalizedString("property.bgColor.display_name") );
			properties[PROPERTY_bgColor].setShortDescription ( getExternalizedString("property.bgColor.short_description") );
			properties[PROPERTY_bgColor].setPropertyEditorClass( getEditorClass("ColorEditor") );
			
			properties[PROPERTY_freeStyle] = new PropertyDescriptor ( "freeStyle", Style.class, "getFreeStyle", "setFreeStyle" );
			properties[PROPERTY_freeStyle].setDisplayName ( getExternalizedString("property.freeStyle.display_name") );
			properties[PROPERTY_freeStyle].setShortDescription ( getExternalizedString("property.freeStyle.short_description") );
			
			properties[PROPERTY_border] = new PropertyDescriptor ( "border", Style.class, "isBorder", "setBorder" );
			properties[PROPERTY_border].setDisplayName ( getExternalizedString("property.border.display_name") );
			properties[PROPERTY_border].setShortDescription ( getExternalizedString("property.border.short_description") );
			
			properties[PROPERTY_borderWidth] = new PropertyDescriptor ( "borderWidth", Style.class, "getBorderWidth", "setBorderWidth" );
			properties[PROPERTY_borderWidth].setDisplayName ( getExternalizedString("property.borderWidth.display_name") );
			properties[PROPERTY_borderWidth].setShortDescription ( getExternalizedString("property.borderWidth.short_description") );
			
			properties[PROPERTY_borderColor] = new PropertyDescriptor ( "borderColor", Style.class, "getBorderColor", "setBorderColor" );
			properties[PROPERTY_borderColor].setDisplayName ( getExternalizedString("property.borderColor.display_name") );
			properties[PROPERTY_borderColor].setShortDescription ( getExternalizedString("property.borderColor.short_description") );
			properties[PROPERTY_borderColor].setPropertyEditorClass( getEditorClass("ColorEditor") );
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
	private static final int PROPERTY_fontSize 		= 0;
	private static final int PROPERTY_fontName 		= 1;
	private static final int PROPERTY_bold 			= 2;
	private static final int PROPERTY_italic 		= 3;
	private static final int PROPERTY_underlined 	= 4;
	private static final int PROPERTY_color 		= 5;
	private static final int PROPERTY_bgColor 		= 6;
	private static final int PROPERTY_freeStyle 	= 7;
	private static final int PROPERTY_border 		= 8;
	private static final int PROPERTY_borderWidth 	= 9;
	private static final int PROPERTY_borderColor 	= 10;
	
}

