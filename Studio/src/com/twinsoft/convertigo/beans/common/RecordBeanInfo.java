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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class RecordBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_eorType = 0;
    private static final int PROPERTY_borType = 1;
    private static final int PROPERTY_perPage = 2;
    private static final int PROPERTY_eor = 3;
    private static final int PROPERTY_bor = 4;
    private static final int PROPERTY_tagName = 5;

    public RecordBeanInfo() {
		try {
			beanClass =  Record.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/record_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/record_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Record");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[6];
			
            properties[PROPERTY_eorType] = new PropertyDescriptor ( "eorType", Record.class, "getEorType", "setEorType" );
            properties[PROPERTY_eorType].setDisplayName ( getExternalizedString("property.eorType.display_name") );
            properties[PROPERTY_eorType].setShortDescription ( getExternalizedString("property.eorType.short_description") );
            
            properties[PROPERTY_borType] = new PropertyDescriptor ( "borType", Record.class, "getBorType", "setBorType" );
            properties[PROPERTY_borType].setDisplayName ( getExternalizedString("property.borType.display_name") );
            properties[PROPERTY_borType].setShortDescription ( getExternalizedString("property.borType.short_description") );
            
            properties[PROPERTY_perPage] = new PropertyDescriptor ( "perPage", Record.class, "isPerPage", "setPerPage" );
            properties[PROPERTY_perPage].setDisplayName ( getExternalizedString("property.perPage.display_name") );
            properties[PROPERTY_perPage].setShortDescription ( getExternalizedString("property.perPage.short_description") );
            
            properties[PROPERTY_eor] = new PropertyDescriptor ( "eor", Record.class, "getEor", "setEor" );
            properties[PROPERTY_eor].setDisplayName ( getExternalizedString("property.eor.display_name") );
            properties[PROPERTY_eor].setShortDescription ( getExternalizedString("property.eor.short_description") );
            
            properties[PROPERTY_bor] = new PropertyDescriptor ( "bor", Record.class, "getBor", "setBor" );
            properties[PROPERTY_bor].setDisplayName ( getExternalizedString("property.bor.display_name") );
            properties[PROPERTY_bor].setShortDescription ( getExternalizedString("property.bor.short_description") );
            
            properties[PROPERTY_tagName] = new PropertyDescriptor ( "tagName", Record.class, "getTagName", "setTagName" );
            properties[PROPERTY_tagName].setDisplayName ( getExternalizedString("property.tagName.display_name") );
            properties[PROPERTY_tagName].setShortDescription ( getExternalizedString("property.tagName.short_description") );
            properties[PROPERTY_tagName].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
