/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.common;

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class TrimBlockBeanInfo extends MySimpleBeanInfo {
    
    public TrimBlockBeanInfo() {
		try {
			beanClass =  TrimBlock.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/trimblock_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/trimblock_color_32x32.png";

			resourceBundle = getResourceBundle("res/TrimBlock");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[2];
			
			properties[PROPERTY_TrimRight] = new PropertyDescriptor ( "bTrimRight", TrimBlock.class, "isBTrimRight", "setBTrimRight" );
			properties[PROPERTY_TrimRight].setDisplayName ( getExternalizedString("property.bTrimRight.display_name") );
			properties[PROPERTY_TrimRight].setShortDescription ( getExternalizedString("property.bTrimRight.short_description") );
			
			properties[PROPERTY_TrimLeft] = new PropertyDescriptor ( "bTrimLeft", TrimBlock.class, "isBTrimLeft", "setBTrimLeft" );
			properties[PROPERTY_TrimLeft].setDisplayName ( getExternalizedString("property.bTrimLeft.display_name") );
			properties[PROPERTY_TrimLeft].setShortDescription ( getExternalizedString("property.bTrimLeft.short_description") );
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

    private static final int PROPERTY_TrimRight	= 0;
	private static final int PROPERTY_TrimLeft 	= 1;
	
}
