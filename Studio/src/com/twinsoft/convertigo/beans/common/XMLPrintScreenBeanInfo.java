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

public class XMLPrintScreenBeanInfo extends MySimpleBeanInfo {
    
	public XMLPrintScreenBeanInfo() {
		try {
			beanClass = XMLPrintScreen.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/xmlprintscreen_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/xmlprintscreen_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/XMLPrintScreen");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties = new PropertyDescriptor[9];
			
			properties[0] = new PropertyDescriptor("tagName", beanClass, "getTagName", "setTagName");
			properties[0].setDisplayName(getExternalizedString("property.tagName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.tagName.short_description"));
			properties[0].setValue(DatabaseObject.PROPERTY_XMLNAME, true);
			
			properties[1] = new PropertyDescriptor("height", beanClass, "getHeight", "setHeight");
			properties[1].setDisplayName(getExternalizedString("property.height.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.height.short_description"));
			
			properties[2] = new PropertyDescriptor("width", beanClass, "getWidth", "setWidth");
			properties[2].setDisplayName(getExternalizedString("property.width.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.width.short_description"));
			
			properties[3] = new PropertyDescriptor("top", beanClass, "getTop", "setTop");
			properties[3].setDisplayName(getExternalizedString("property.top.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.top.short_description"));
			
			properties[4] = new PropertyDescriptor("left", beanClass, "getLeft", "setLeft");
			properties[4].setDisplayName(getExternalizedString("property.left.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.left.short_description"));
			
			properties[5] = new PropertyDescriptor("scale", beanClass, "getScale", "setScale");
			properties[5].setDisplayName(getExternalizedString("property.scale.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.scale.short_description"));
			
			properties[6] = new PropertyDescriptor("imageFormat", beanClass, "getImageFormat", "setImageFormat");
			properties[6].setDisplayName(getExternalizedString("property.imageFormat.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.imageFormat.short_description"));
			properties[6].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			
			properties[7] = new PropertyDescriptor("includeDataUrl", beanClass, "isIncludeDataUrl", "setIncludeDataUrl");
			properties[7].setDisplayName(getExternalizedString("property.includeDataUrl.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.includeDataUrl.short_description"));
			
			properties[8] = new PropertyDescriptor("minDelay", beanClass, "getMinDelay", "setMinDelay");
			properties[8].setDisplayName(getExternalizedString("property.minDelay.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.minDelay.short_description"));
			properties[8].setExpert(true);
			
			PropertyDescriptor property = getPropertyDescriptor("xpath");
			property.setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
