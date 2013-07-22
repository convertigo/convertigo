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

public class RemoveBlocksBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_length = 0;
    private static final int PROPERTY_regularExpression = 1;
    private static final int PROPERTY_blockTag = 2;

    public RemoveBlocksBeanInfo() {
		try {
			beanClass =  RemoveBlocks.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/removeblocks_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/removeblocks_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/RemoveBlocks");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
            properties[PROPERTY_length] = new PropertyDescriptor ( "length", RemoveBlocks.class, "getLength", "setLength" );
            properties[PROPERTY_length].setDisplayName ( getExternalizedString("property.lenght.display_name") );
            properties[PROPERTY_length].setShortDescription ( getExternalizedString("property.lenght.short_description") );
            
            properties[PROPERTY_regularExpression] = new PropertyDescriptor ( "regularExpression", RemoveBlocks.class, "getRegularExpression", "setRegularExpression" );
            properties[PROPERTY_regularExpression].setDisplayName ( getExternalizedString("property.regularExpression.display_name") );
            properties[PROPERTY_regularExpression].setShortDescription ( getExternalizedString("property.regularExpression.short_description") );

            properties[PROPERTY_blockTag] = new PropertyDescriptor ( "blockTag", RemoveBlocks.class, "getBlockTag", "setBlockTag" );
            properties[PROPERTY_blockTag].setDisplayName ( getExternalizedString("property.blockTag.display_name") );
            properties[PROPERTY_blockTag].setShortDescription ( getExternalizedString("property.blockTag.short_description") );
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
