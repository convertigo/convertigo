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

public class SeparatorBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_separatorCharacters = 0;
    private static final int PROPERTY_tagName = 1;
    private static final int PROPERTY_nbOccurrences = 2;

    public SeparatorBeanInfo() {
		try {
			beanClass =  Separator.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/separator_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/separator_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Separator");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
            properties[PROPERTY_separatorCharacters] = new PropertyDescriptor ( "separatorCharacters", Separator.class, "getSeparatorCharacters", "setSeparatorCharacters" );
            properties[PROPERTY_separatorCharacters].setDisplayName ( getExternalizedString("property.separatorChars.display_name") );
            properties[PROPERTY_separatorCharacters].setShortDescription ( getExternalizedString("property.separatorChars.short_description") );
            
            properties[PROPERTY_tagName] = new PropertyDescriptor ( "tagName", Separator.class, "getTagName", "setTagName" );
            properties[PROPERTY_tagName].setDisplayName ( getExternalizedString("property.tagName.display_name") );
            properties[PROPERTY_tagName].setShortDescription ( getExternalizedString("property.tagName.short_description") );
            properties[PROPERTY_tagName].setValue("normalizable", Boolean.TRUE);
            
            properties[PROPERTY_nbOccurrences] = new PropertyDescriptor ( "nbOccurrences", Separator.class, "getNbOccurrences", "setNbOccurrences" );
            properties[PROPERTY_nbOccurrences].setDisplayName ( getExternalizedString("property.nbOccurrences.display_name") );
            properties[PROPERTY_nbOccurrences].setShortDescription ( getExternalizedString("property.nbOccurrences.short_description") );
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
