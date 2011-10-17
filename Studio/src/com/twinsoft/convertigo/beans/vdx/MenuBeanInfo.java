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

package com.twinsoft.convertigo.beans.vdx;

import java.beans.*;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class MenuBeanInfo extends MySimpleBeanInfo {
    
	  private static final int PROPERTY_startLine = 0;
	  private static final int PROPERTY_menuWidth = 1;
	  private static final int PROPERTY_charType = 2;

    public MenuBeanInfo() {
		try {
			beanClass =  Menu.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/menu_color_16x16.gif";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/menu_color_32x32.gif";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/vdx/res/Menu");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[3];
			
			properties[PROPERTY_startLine] = new PropertyDescriptor ( "startLine", Menu.class, "getStartLine", "setStartLine" );
			properties[PROPERTY_menuWidth] = new PropertyDescriptor ( "menuWidth", Menu.class, "getMenuWidth", "setMenuWidth" );
			properties[PROPERTY_charType] = new PropertyDescriptor ( "charType", Menu.class, "getCharType", "setCharType" );
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
