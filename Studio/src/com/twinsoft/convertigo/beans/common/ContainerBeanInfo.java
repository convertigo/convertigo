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

public class ContainerBeanInfo extends MySimpleBeanInfo {

	public ContainerBeanInfo() {
		try {
			beanClass = Container.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/container_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/container_color_32x32.png";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Container");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[2];
			
            properties[PROPERTY_TagName] = new PropertyDescriptor ( "tagName", Container.class, "getTagName", "setTagName" );
            properties[PROPERTY_TagName].setDisplayName ( getExternalizedString("property.tagName.display_name") );
            properties[PROPERTY_TagName].setShortDescription ( getExternalizedString("property.tagName.short_description") );
            properties[PROPERTY_TagName].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
            
            properties[PROPERTY_ContainerDesc] = new PropertyDescriptor ( "containerDesc", Container.class, "getContainerDesc", "setContainerDesc" );
			properties[PROPERTY_ContainerDesc].setDisplayName ( getExternalizedString("property.containerDesc.display_name") );
			properties[PROPERTY_ContainerDesc].setShortDescription ( getExternalizedString("property.containerDesc.short_description") );
			properties[PROPERTY_ContainerDesc].setPropertyEditorClass(getEditorClass("ZoneEditor"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
    private static final int PROPERTY_TagName 		= 0;
    private static final int PROPERTY_ContainerDesc = 1;
    
}

