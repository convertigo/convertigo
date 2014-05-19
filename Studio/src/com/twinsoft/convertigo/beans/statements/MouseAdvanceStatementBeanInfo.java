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

public class MouseAdvanceStatementBeanInfo extends MySimpleBeanInfo {
    
	public MouseAdvanceStatementBeanInfo() {
		try {
			beanClass = MouseAdvanceStatement.class;
			additionalBeanClass = MouseStatement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/statements/images/mouseadvance_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/statements/images/mouseadvance_32x32.png";
			
			resourceBundle = getResourceBundle("res/MouseAdvanceStatement");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[9];

            properties[0] = new PropertyDescriptor("screenX", beanClass, "getScreenX", "setScreenX");
            properties[0].setDisplayName(getExternalizedString("property.screenx.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.screenx.short_description"));
            properties[0].setValue("scriptable", Boolean.TRUE);
            
            properties[1] = new PropertyDescriptor("screenY", beanClass, "getScreenY", "setScreenY");
            properties[1].setDisplayName(getExternalizedString("property.screeny.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.screeny.short_description"));
            properties[1].setValue("scriptable", Boolean.TRUE);

            properties[2] = new PropertyDescriptor("clientX", beanClass, "getClientX", "setClientX");
            properties[2].setDisplayName(getExternalizedString("property.clientx.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.clientx.short_description"));
            properties[2].setValue("scriptable", Boolean.TRUE);

            properties[3] = new PropertyDescriptor("clientY", beanClass, "getClientY", "setClientY");
            properties[3].setDisplayName(getExternalizedString("property.clienty.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.clienty.short_description"));
            properties[3].setValue("scriptable", Boolean.TRUE);
            
            properties[4] = new PropertyDescriptor("altKey", beanClass, "getAltKey", "setAltKey");
            properties[4].setDisplayName(getExternalizedString("property.altkey.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.altkey.short_description"));
            properties[4].setValue("scriptable", Boolean.TRUE);
            
            properties[5] = new PropertyDescriptor("button", beanClass, "getButton", "setButton");
            properties[5].setDisplayName(getExternalizedString("property.button.display_name"));
            properties[5].setShortDescription(getExternalizedString("property.button.short_description"));
            properties[5].setValue("scriptable", Boolean.TRUE);
            
            properties[6] = new PropertyDescriptor("ctrlKey", beanClass, "getCtrlKey", "setCtrlKey");
            properties[6].setDisplayName(getExternalizedString("property.ctrlkey.display_name"));
            properties[6].setShortDescription(getExternalizedString("property.ctrlkey.short_description"));
            properties[6].setValue("scriptable", Boolean.TRUE);
            
            properties[7] = new PropertyDescriptor("metKey", beanClass, "getMetKey", "setMetKey");
            properties[7].setDisplayName(getExternalizedString("property.metkey.display_name"));
            properties[7].setShortDescription(getExternalizedString("property.metkey.short_description"));
            properties[7].setValue("scriptable", Boolean.TRUE);
            
            properties[8] = new PropertyDescriptor("shiftKey", beanClass, "getShiftKey", "setShiftKey");
            properties[8].setDisplayName(getExternalizedString("property.shiftkey.display_name"));
            properties[8].setShortDescription(getExternalizedString("property.shiftkey.short_description"));
            properties[8].setValue("scriptable", Boolean.TRUE);  
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
