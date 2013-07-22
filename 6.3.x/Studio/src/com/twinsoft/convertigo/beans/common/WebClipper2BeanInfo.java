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

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class WebClipper2BeanInfo extends MySimpleBeanInfo {
    
	public WebClipper2BeanInfo() {
		try {
			beanClass = WebClipper2.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/webclipper2_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/webclipper2_color_32x32.png";

		    properties = new PropertyDescriptor[2];
		    
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/WebClipper2");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("attributes", beanClass, "getAttributes", "setAttributes");
			properties[0].setDisplayName(getExternalizedString("property.attributes.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.attributes.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("WebClipperAttributesEditor"));
			
			properties[1] = new PropertyDescriptor("mHttpTunnel", beanClass, "getMHttpTunnel", "setMHttpTunnel");
			properties[1].setDisplayName(getExternalizedString("property.mHttpTunnel.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.mHttpTunnel.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
