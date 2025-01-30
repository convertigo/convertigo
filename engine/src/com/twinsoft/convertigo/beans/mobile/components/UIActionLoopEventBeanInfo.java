/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.mobile.components;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class UIActionLoopEventBeanInfo extends MySimpleBeanInfo {

	public UIActionLoopEventBeanInfo() {
		try {
			beanClass = UIActionLoopEvent.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.UIActionEvent.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/mobile/components/images/uiactionloopevent_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/mobile/components/images/uiactionloopevent_color_32x32.png";

			resourceBundle = getResourceBundle("res/UIActionLoopEvent");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[2];

			properties[0] = new PropertyDescriptor("varItemName", beanClass, "getVarItemName", "setVarItemName");
			properties[0].setDisplayName(getExternalizedString("property.varItemName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.varItemName.short_description"));
			properties[0].setValue(CATEGORY, "@Properties");

			properties[1] = new PropertyDescriptor("varIndexName", beanClass, "getVarIndexName", "setVarIndexName");
			properties[1].setDisplayName(getExternalizedString("property.varIndexName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.varIndexName.short_description"));
			properties[1].setValue(CATEGORY, "@Properties");

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
