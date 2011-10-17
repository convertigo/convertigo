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

package com.twinsoft.convertigo.beans.connectors;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class ProxyHttpConnectorBeanInfo extends MySimpleBeanInfo {
    
	public ProxyHttpConnectorBeanInfo() {
		try {
			beanClass = ProxyHttpConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.connectors.HttpConnector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/proxyhttpconnector_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/proxyhttpconnector_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/connectors/res/ProxyHttpConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
			properties[0] = new PropertyDescriptor("replacements", beanClass, "getReplacements", "setReplacements");
			properties[0].setDisplayName(getExternalizedString("property.replacements.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.replacements.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("ReplacementsEditor"));
			
			properties[1] = new PropertyDescriptor("dynamicContentFiles", beanClass, "getDynamicContentFiles", "setDynamicContentFiles");
			properties[1].setDisplayName(getExternalizedString("property.dynamicContentFiles.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.dynamicContentFiles.short_description"));
			
			properties[2] = new PropertyDescriptor("removableHeaders", beanClass, "getRemovableHeaders", "setRemovableHeaders");
			properties[2].setDisplayName(getExternalizedString("property.removableHeaders.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.removableHeaders.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("RemovableHeadersEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

