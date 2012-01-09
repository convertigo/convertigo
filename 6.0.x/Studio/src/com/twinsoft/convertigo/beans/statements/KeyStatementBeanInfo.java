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

package com.twinsoft.convertigo.beans.statements;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class KeyStatementBeanInfo extends MySimpleBeanInfo {
    
	public KeyStatementBeanInfo() {
		try {
			beanClass = KeyStatement.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.statements.SimpleEventStatement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/key_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/key_32x32.gif";
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/statements/res/KeyStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[6];
			
            properties[0] = new PropertyDescriptor("keyCode", beanClass, "getKeyCode", "setKeyCode");
            properties[0].setDisplayName(getExternalizedString("property.keycode.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.keycode.short_description"));
            
            properties[1] = new PropertyDescriptor("charCode", beanClass, "getCharCode", "setCharCode");
            properties[1].setDisplayName(getExternalizedString("property.charcode.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.charcode.short_description"));
			
            properties[2] = new PropertyDescriptor("altKey", beanClass, "getAltKey", "setAltKey");
            properties[2].setDisplayName(getExternalizedString("property.altkey.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.altkey.short_description"));
            
            properties[3] = new PropertyDescriptor("ctrlKey", beanClass, "getCtrlKey", "setCtrlKey");
            properties[3].setDisplayName(getExternalizedString("property.ctrlkey.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.ctrlkey.short_description"));
            
            properties[4] = new PropertyDescriptor("metKey", beanClass, "getMetKey", "setMetKey");
            properties[4].setDisplayName(getExternalizedString("property.metkey.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.metkey.short_description"));
            
            properties[5] = new PropertyDescriptor("shiftKey", beanClass, "getShiftKey", "setShiftKey");
            properties[5].setDisplayName(getExternalizedString("property.shiftkey.display_name"));
            properties[5].setShortDescription(getExternalizedString("property.shiftkey.short_description")); 			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
