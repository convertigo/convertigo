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

import com.twinsoft.convertigo.beans.connectors.HtmlConnector.ParseMode;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class HtmlConnectorBeanInfo extends MySimpleBeanInfo {
    
	public HtmlConnectorBeanInfo() {
		try {
			beanClass = HtmlConnector.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.connectors.HttpConnector.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/connectors/images/htmlconnector_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/connectors/images/htmlconnector_color_32x32.png";

			resourceBundle = getResourceBundle("res/HtmlConnector");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("ignoreEmptyAttributes", beanClass, "getIgnoreEmptyAttributes", "setIgnoreEmptyAttributes");
			properties[0].setDisplayName(getExternalizedString("property.ignoreEmptyAttributes.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.ignoreEmptyAttributes.short_description"));
			properties[0].setExpert(true);
			
            properties[1] = new PropertyDescriptor("parseMode", beanClass, "getParseMode", "setParseMode");
            properties[1].setDisplayName(getExternalizedString("property.parsemode.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.parsemode.short_description"));
            properties[1].setPropertyEditorClass(ParseMode.class);
            properties[1].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

