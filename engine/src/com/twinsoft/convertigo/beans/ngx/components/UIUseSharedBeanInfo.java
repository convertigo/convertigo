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

package com.twinsoft.convertigo.beans.ngx.components;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class UIUseSharedBeanInfo extends MySimpleBeanInfo {

	public UIUseSharedBeanInfo() {
		try {
			beanClass = UIUseShared.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.ngx.components.UIElement.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/ngx/components/images/uiuseshared_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/ngx/components/images/uiuseshared_32x32.png";

			resourceBundle = getResourceBundle("res/UIUseShared");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[1];

			properties[0] = new PropertyDescriptor("sharedcomponent", beanClass, "getSharedComponentQName", "setSharedComponentQName");
			properties[0].setDisplayName(getExternalizedString("property.sharedcomponent.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.sharedcomponent.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			properties[0].setValue(CATEGORY, "@Component");

			getPropertyDescriptor("tagName").setHidden(true);
			getPropertyDescriptor("selfClose").setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
