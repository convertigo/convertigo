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

public class JavelinExtractionRuleBeanInfo extends MySimpleBeanInfo {
    
	public JavelinExtractionRuleBeanInfo() {
		try {
			beanClass = JavelinExtractionRule.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.ExtractionRule.class;

			resourceBundle = getResourceBundle("res/JavelinExtractionRule");

			properties = new PropertyDescriptor[4];
			
            properties[0] = new PropertyDescriptor("selectionAttribute", JavelinExtractionRule.class, "getSelectionAttribute", "setSelectionAttribute");
            properties[0].setDisplayName(getExternalizedString("property.selectionAttribute.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.selectionAttribute.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("JavelinAttributeEditor"));
            properties[0].setExpert(true);

            properties[1] = new PropertyDescriptor("isFinal", JavelinExtractionRule.class, "isFinal", "setFinal");
            properties[1].setDisplayName(getExternalizedString("property.isFinal.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.isFinal.short_description"));

            properties[2] = new PropertyDescriptor("selectionType", JavelinExtractionRule.class, "getSelectionType", "setSelectionType");
            properties[2].setDisplayName(getExternalizedString("property.selectionType.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.selectionType.short_description"));
            properties[2].setExpert(true);

            properties[3] = new PropertyDescriptor("selectionScreenZone", JavelinExtractionRule.class, "getSelectionScreenZone", "setSelectionScreenZone");
            properties[3].setDisplayName(getExternalizedString("property.selectionScreenZone.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.selectionScreenZone.short_description"));
            properties[3].setPropertyEditorClass(getEditorClass("ZoneEditor"));
            properties[3].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
