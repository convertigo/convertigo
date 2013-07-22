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

package com.twinsoft.convertigo.beans.sna;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class CommandSNABeanInfo extends MySimpleBeanInfo {

	public CommandSNABeanInfo() {
		try {
			beanClass = CommandSNA.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.common.Command.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/sna/images/command_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/sna/images/command_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/sna/res/CommandSNA");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
            properties[PROPERTY_keywordSeparator] = new PropertyDescriptor ( "keywordSeparator", CommandSNA.class, "getKeywordSeparator", "setKeywordSeparator" );
            properties[PROPERTY_keywordSeparator].setDisplayName ( getExternalizedString("property.keywordSeparator.display_name") );
            properties[PROPERTY_keywordSeparator].setShortDescription ( getExternalizedString("property.keywordSeparator.short_description") );
			
            properties[PROPERTY_labelLocation] = new PropertyDescriptor ( "labelLocation", CommandSNA.class, "getLabelLocation", "setLabelLocation" );
			properties[PROPERTY_labelLocation].setDisplayName ( getExternalizedString("property.labelLocation.display_name") );
			properties[PROPERTY_labelLocation].setShortDescription ( getExternalizedString("property.labelLocation.short_description") );
			properties[PROPERTY_labelLocation].setPropertyEditorClass (getEditorClass("CommandSNALabelLocationEditor"));
			
			properties[PROPERTY_separatorMendatory] = new PropertyDescriptor ( "separatorMendatory", CommandSNA.class, "isSeparatorMendatory", "setSeparatorMendatory" );
            properties[PROPERTY_separatorMendatory].setDisplayName ( getExternalizedString("property.separatorMendatory.display_name") );
            properties[PROPERTY_separatorMendatory].setShortDescription ( getExternalizedString("property.separatorMendatory.short_description") );
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
	
    private static final int PROPERTY_keywordSeparator = 0;
	private static final int PROPERTY_labelLocation = 1;
	private static final int PROPERTY_separatorMendatory = 2;
}

