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

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class RadioBeanInfo extends MySimpleBeanInfo {

	public RadioBeanInfo() {
		try {
			beanClass = Radio.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/radiobutton_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/radiobutton_32x32.gif";

 			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/common/res/Radio");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
			properties[PROPERTY_options] = new PropertyDescriptor ( "options", Radio.class, "getOptions", "setOptions" );
            properties[PROPERTY_options].setDisplayName ( getExternalizedString("property.options.display_name") );
            properties[PROPERTY_options].setShortDescription ( getExternalizedString("property.options.short_description") );
            properties[PROPERTY_options].setPropertyEditorClass ( getEditorClass("ActionsEditor") );
			
			properties[PROPERTY_separatorChars] = new PropertyDescriptor ( "separatorChars", Radio.class, "getSeparatorChars", "setSeparatorChars" );
			properties[PROPERTY_separatorChars].setDisplayName ( getExternalizedString("property.separatorChars.display_name") );
            properties[PROPERTY_separatorChars].setShortDescription ( getExternalizedString("property.separatorChars.short_description") );
            
			properties[PROPERTY_radioDesc] = new PropertyDescriptor ( "radioDesc", Radio.class, "getRadioDesc", "setRadioDesc" );
			properties[PROPERTY_radioDesc].setDisplayName ( getExternalizedString("property.radioDesc.display_name") );
			properties[PROPERTY_radioDesc].setShortDescription ( getExternalizedString("property.radioDesc.short_description") );
			properties[PROPERTY_radioDesc].setPropertyEditorClass(getEditorClass("ZoneEditor"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
		
	private static final int PROPERTY_options = 0;
	private static final int PROPERTY_separatorChars = 1;
	private static final int PROPERTY_radioDesc = 2;
}

