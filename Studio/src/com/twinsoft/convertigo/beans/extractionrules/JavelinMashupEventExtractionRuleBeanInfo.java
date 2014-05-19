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

package com.twinsoft.convertigo.beans.extractionrules;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class JavelinMashupEventExtractionRuleBeanInfo extends MySimpleBeanInfo {
    
	public JavelinMashupEventExtractionRuleBeanInfo() {
		try {
			beanClass = JavelinMashupEventExtractionRule.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			resourceBundle = getResourceBundle("res/JavelinMashupEventExtractionRule");

			properties = new PropertyDescriptor[1];
			
            properties[0] = new PropertyDescriptor("mashupEventName", beanClass, "getMashupEventName", "setMashupEventName");
            properties[0].setDisplayName(getExternalizedString("property.mashupEventName.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.mashupEventName.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("JavelinMashupEventEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
