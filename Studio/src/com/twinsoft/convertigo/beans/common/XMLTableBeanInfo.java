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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class XMLTableBeanInfo extends MySimpleBeanInfo {
    
	public XMLTableBeanInfo() {
		try {
			beanClass = XMLTable.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.common.AbstractXMLReferer.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/xmltable_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/xmltable_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/XMLTable");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties = new PropertyDescriptor[4];
			
			properties[0] = new PropertyDescriptor("tagName", beanClass, "getTagName", "setTagName");
			properties[0].setDisplayName(getExternalizedString("property.tagName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.tagName.short_description"));
			properties[0].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
			
			properties[1] = new PropertyDescriptor("description", beanClass, "getDescription", "setDescription");
			properties[1].setDisplayName(getExternalizedString("property.description.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.description.short_description"));
			//properties[1].setPropertyEditorClass(getEditorClass("XMLTableEditor"));
			properties[1].setExpert(true);
			properties[1].setValue("disable", Boolean.TRUE);
			
			properties[2] = new PropertyDescriptor ( "accumulateDataInSameTable", beanClass, "isAccumulateDataInSameTable", "setAccumulateDataInSameTable" );
            properties[2].setDisplayName ( getExternalizedString("property.accumulateDataInSameTable.display_name") );
            properties[2].setShortDescription ( getExternalizedString("property.accumulateDataInSameTable.short_description") );
            
			properties[3] = new PropertyDescriptor ( "flipTable", beanClass, "isFlipTable", "setFlipTable" );
            properties[3].setDisplayName ( getExternalizedString("property.fliptable.display_name") );
            properties[3].setShortDescription ( getExternalizedString("property.fliptable.short_description") );
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
