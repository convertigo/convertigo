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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/XMLAttributeStepBeanInfo.java $
 * $Author: fabienb $
 * $Revision: 29732 $
 * $Date: 2012-02-20 18:05:49 +0100 (lun., 20 févr. 2012) $
 */

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class AttributeStepBeanInfo extends MySimpleBeanInfo {
    
	public AttributeStepBeanInfo() {
		try {
			beanClass = AttributeStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/jattrib_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/jattrib_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/steps/res/AttributeStep");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[5];

            properties[0] = new PropertyDescriptor("nodeName", beanClass, "getNodeName", "setNodeName");
            properties[0].setDisplayName(getExternalizedString("property.nodeName.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.nodeName.short_description"));
            properties[0].setValue("normalizable", Boolean.TRUE);
            properties[0].setValue(BLACK_LIST_NAME, Boolean.TRUE);
            
            properties[1] = new PropertyDescriptor("nodeText", beanClass, "getNodeText", "setNodeText");
            properties[1].setDisplayName(getExternalizedString("property.nodeText.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.nodeText.short_description")); 
            properties[1].setValue(BLACK_LIST_NAME, Boolean.TRUE);

            properties[2] = new PropertyDescriptor("expression", beanClass, "getExpression", "setExpression");
            properties[2].setDisplayName(getExternalizedString("property.expression.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.expression.short_description"));
            properties[2].setValue("scriptable", Boolean.TRUE);
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
