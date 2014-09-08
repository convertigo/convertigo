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

import com.twinsoft.convertigo.beans.common.WebClipper.HttpTunnel;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class WebClipperBeanInfo extends MySimpleBeanInfo {
    
	public WebClipperBeanInfo() {
		try {
			beanClass = WebClipper.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/webclipper_color_16x16.png";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/webclipper_color_32x32.png";

		    properties = new PropertyDescriptor[5];
		    
			resourceBundle = getResourceBundle("res/WebClipper");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("attributes", beanClass, "getAttributes", "setAttributes");
			properties[0].setDisplayName(getExternalizedString("property.attributes.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.attributes.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("WebClipperAttributesEditor"));
			
			properties[1] = new PropertyDescriptor("bHttpTunnel", beanClass, "getBHttpTunnel", "setBHttpTunnel");
			properties[1].setDisplayName(getExternalizedString("property.bHttpTunnel.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.bHttpTunnel.short_description"));
			properties[1].setHidden(true);
			
			properties[2] = new PropertyDescriptor("transactionName", beanClass, "getTransactionName", "setTransactionName");
			properties[2].setDisplayName(getExternalizedString("property.transactionName.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.transactionName.short_description"));
			properties[2].setHidden(true);
			
			properties[3] = new PropertyDescriptor("mHttpTunnel", beanClass, "getMHttpTunnel", "setMHttpTunnel");
			properties[3].setDisplayName(getExternalizedString("property.mHttpTunnel.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.mHttpTunnel.short_description"));
			properties[3].setPropertyEditorClass(HttpTunnel.class);
			
			properties[4] = new PropertyDescriptor("extractParent", beanClass, "getExtractParent", "setExtractParent");
			properties[4].setDisplayName(getExternalizedString("property.extractParent.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.extractParent.short_description"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
