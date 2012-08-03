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

public class PanelBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_bottom = 0;
    private static final int PROPERTY_lowerLeft = 1;
    private static final int PROPERTY_right = 2;
    private static final int PROPERTY_lowerRight = 3;
    private static final int PROPERTY_upperRight = 4;
    private static final int PROPERTY_top = 5;
    private static final int PROPERTY_upperLeft = 6;
    private static final int PROPERTY_left = 7;
    private static final int PROPERTY_minsides = 8;
    private static final int PROPERTY_removeBlocksInBorder = 9;
    private static final int PROPERTY_titleAttribute = 10;
    
    public PanelBeanInfo() {
		try {
			beanClass =  Panel.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/panel_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/panel_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Panel");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[11];
			
            properties[PROPERTY_bottom] = new PropertyDescriptor ( "bottom", Panel.class, "getBottom", "setBottom" );
            properties[PROPERTY_bottom].setDisplayName ( getExternalizedString("property.bottom.display_name") );
            properties[PROPERTY_bottom].setShortDescription ( getExternalizedString("property.bottom.short_description") );

            properties[PROPERTY_lowerLeft] = new PropertyDescriptor ( "lowerLeft", Panel.class, "getLowerLeft", "setLowerLeft" );
            properties[PROPERTY_lowerLeft].setDisplayName ( getExternalizedString("property.LowerLeft.display_name") );
            properties[PROPERTY_lowerLeft].setShortDescription ( getExternalizedString("property.lowerLeft.short_description") );
            
            properties[PROPERTY_right] = new PropertyDescriptor ( "right", Panel.class, "getRight", "setRight" );
            properties[PROPERTY_right].setDisplayName ( getExternalizedString("property.right.display_name") );
            properties[PROPERTY_right].setShortDescription ( getExternalizedString("property.right.short_description") );
            
            properties[PROPERTY_lowerRight] = new PropertyDescriptor ( "lowerRight", Panel.class, "getLowerRight", "setLowerRight" );
            properties[PROPERTY_lowerRight].setDisplayName ( getExternalizedString("property.lowerRight.display_name") );
            properties[PROPERTY_lowerRight].setShortDescription ( getExternalizedString("property.lowerRight.short_description") );
            
            properties[PROPERTY_upperRight] = new PropertyDescriptor ( "upperRight", Panel.class, "getUpperRight", "setUpperRight" );
            properties[PROPERTY_upperRight].setDisplayName ( getExternalizedString("property.upperRight.display_name") );
            properties[PROPERTY_upperRight].setShortDescription ( getExternalizedString("property.upperRight.short_description") );
            
            properties[PROPERTY_top] = new PropertyDescriptor ( "top", Panel.class, "getTop", "setTop" );
            properties[PROPERTY_top].setDisplayName ( getExternalizedString("property.top.display_name") );
            properties[PROPERTY_top].setShortDescription ( getExternalizedString("property.top.short_description") );
            
            properties[PROPERTY_upperLeft] = new PropertyDescriptor ( "upperLeft", Panel.class, "getUpperLeft", "setUpperLeft" );
            properties[PROPERTY_upperLeft].setDisplayName ( getExternalizedString("property.upperLeft.display_name") );
            properties[PROPERTY_upperLeft].setShortDescription ( getExternalizedString("property.upperLeft.short_description") );
            
            properties[PROPERTY_left] = new PropertyDescriptor ( "left", Panel.class, "getLeft", "setLeft" );
            properties[PROPERTY_left].setDisplayName ( getExternalizedString("property.left.display_name") );
            properties[PROPERTY_left].setShortDescription ( getExternalizedString("property.left.short_description") );
            
            properties[PROPERTY_minsides] = new PropertyDescriptor ( "minsides", Panel.class, "getMinSides", "setMinSides" );
            properties[PROPERTY_minsides].setDisplayName ( getExternalizedString("property.minsides.display_name") );
            properties[PROPERTY_minsides].setShortDescription ( getExternalizedString("property.minsides.short_description") );
         
            properties[PROPERTY_removeBlocksInBorder] = new PropertyDescriptor ( "removeBlocksInBorder", Panel.class, "isRemoveBlocksInBorder", "setRemoveBlocksInBorder" );
            properties[PROPERTY_removeBlocksInBorder].setDisplayName ( getExternalizedString("property.removeBlocksInBorder.display_name") );
            properties[PROPERTY_removeBlocksInBorder].setShortDescription ( getExternalizedString("property.removeBlocksInBorder.short_description") );
            
            properties[PROPERTY_titleAttribute] = new PropertyDescriptor("titleAttribute", Panel.class, "getTitleAttribute", "setTitleAttribute");
            properties[PROPERTY_titleAttribute].setDisplayName(getExternalizedString("property.titleAttribute.display_name"));
            properties[PROPERTY_titleAttribute].setShortDescription(getExternalizedString("property.titleAttribute.short_description"));
            properties[PROPERTY_titleAttribute].setPropertyEditorClass(getEditorClass("JavelinAttributeEditor"));
            properties[PROPERTY_titleAttribute].setExpert(true);
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
