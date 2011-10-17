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

public class SplitFieldsBeanInfo extends MySimpleBeanInfo {
	
	public SplitFieldsBeanInfo() {
		try {
			beanClass = SplitFields.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

		    iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/table_color_16x16.gif";
		    iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/table_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/SplitFields");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[9];
			
			properties[0] = new PropertyDescriptor("separatorTableStart", SplitFields.class, "getSeparatorTableStart", "setSeparatorTableStart");
			properties[0].setDisplayName(getExternalizedString("property.separatorTableStart.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.separatorTableStart.short_description"));

			properties[1] = new PropertyDescriptor("separatorTableEnd", SplitFields.class, "getSeparatorTableEnd", "setSeparatorTableEnd");
			properties[1].setDisplayName(getExternalizedString("property.separatorTableEnd.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.separatorTableEnd.short_description"));
			
			properties[2] = new PropertyDescriptor("separatorRowStart", SplitFields.class, "getSeparatorRowStart", "setSeparatorRowStart");
			properties[2].setDisplayName(getExternalizedString("property.separatorRowStart.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.separatorRowStart.short_description"));
			
			properties[3] = new PropertyDescriptor("separatorRowEnd", SplitFields.class, "getSeparatorRowEnd", "setSeparatorRowEnd");
			properties[3].setDisplayName(getExternalizedString("property.separatorRowEnd.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.separatorRowEnd.short_description"));
			
			properties[4] = new PropertyDescriptor("separatorCelStart", SplitFields.class, "getSeparatorCelStart", "setSeparatorCelStart");
			properties[4].setDisplayName(getExternalizedString("property.separatorCelStart.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.separatorCelStart.short_description"));
			
			properties[5] = new PropertyDescriptor("separatorCelEnd", SplitFields.class, "getSeparatorCelEnd", "setSeparatorCelEnd");
			properties[5].setDisplayName(getExternalizedString("property.separatorCelEnd.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.separatorCelEnd.short_description"));
			
			properties[6] = new PropertyDescriptor("columns", SplitFields.class, "getColumns", "setColumns");
			properties[6].setDisplayName(getExternalizedString("property.columns.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.columns.short_description"));
			properties[6].setPropertyEditorClass (getEditorClass("SplitFieldsEditor"));

			properties[7] = new PropertyDescriptor("tagTable", SplitFields.class, "getTagTable", "setTagTable");
			properties[7].setDisplayName(getExternalizedString("property.tagTable.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.tagTable.short_description"));

			properties[8] = new PropertyDescriptor ( "doNotAccumulate", SplitFields.class, "isDoNotAccumulate", "setDoNotAccumulate" );
			properties[8].setDisplayName ( getExternalizedString("property.doNotAccumulate.display_name") );			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
