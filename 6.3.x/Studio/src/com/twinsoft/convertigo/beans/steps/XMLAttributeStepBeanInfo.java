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

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class XMLAttributeStepBeanInfo extends MySimpleBeanInfo {
    
	public XMLAttributeStepBeanInfo() {
		try {
			beanClass = XMLAttributeStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/attrib_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/attrib_32x32.png";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/XMLAttributeStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[5];

            properties[0] = new PropertyDescriptor("nodeName", beanClass, "getNodeName", "setNodeName");
            properties[0].setDisplayName(getExternalizedString("property.nodeName.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.nodeName.short_description"));
            properties[0].setValue(DatabaseObject.PROPERTY_XMLNAME, Boolean.TRUE);
            properties[0].setValue(BLACK_LIST_NAME, Boolean.TRUE);
            
            properties[1] = new PropertyDescriptor("nodeText", beanClass, "getNodeText", "setNodeText");
            properties[1].setDisplayName(getExternalizedString("property.nodeText.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.nodeText.short_description")); 
            properties[1].setValue(BLACK_LIST_NAME, Boolean.TRUE);

            properties[2] = new PropertyDescriptor("sourceDefinition", beanClass, "getSourceDefinition", "setSourceDefinition");
			properties[2].setExpert(true);
			properties[2].setDisplayName(getExternalizedString("property.sourceDefinition.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.sourceDefinition.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("StepSourceEditor"));
			properties[2].setValue(BLACK_LIST_NAME, Boolean.TRUE);
			
            properties[3] = new PropertyDescriptor("nodeNameSpace", beanClass, "getNodeNameSpace", "setNodeNameSpace");
            properties[3].setDisplayName(getExternalizedString("property.nodeNameSpace.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.nodeNameSpace.short_description")); 
            properties[3].setValue(BLACK_LIST_NAME, Boolean.FALSE);
			
            properties[4] = new PropertyDescriptor("nodeNameSpaceURI", beanClass, "getNodeNameSpaceURI", "setNodeNameSpaceURI");
            properties[4].setDisplayName(getExternalizedString("property.nodeNameSpaceURI.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.nodeNameSpaceURI.short_description")); 
            properties[4].setValue(BLACK_LIST_NAME, Boolean.FALSE);

			getPropertyDescriptor("schemaDataType").setHidden(false);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}	
}
