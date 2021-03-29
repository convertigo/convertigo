/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

public class UIDynamicMenuItemBeanInfo extends MySimpleBeanInfo {
	
	public UIDynamicMenuItemBeanInfo() {
		try {
			beanClass = UIDynamicMenuItem.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement.class;

			resourceBundle = getResourceBundle("res/UIDynamicMenuItem");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[4];
			
			properties[0] = new PropertyDescriptor("itempage", beanClass, "getItemPage", "setItemPage");
			properties[0].setDisplayName(getExternalizedString("property.itempage.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.itempage.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			
			properties[1] = new PropertyDescriptor("itemtitle", beanClass, "getItemTitle", "setItemTitle");
			properties[1].setDisplayName(getExternalizedString("property.itemtitle.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.itemtitle.short_description"));
			properties[1].setValue("scriptable", Boolean.TRUE);
			
			properties[2] = new PropertyDescriptor("itemicon", beanClass, "getItemIcon", "setItemIcon");
			properties[2].setDisplayName(getExternalizedString("property.itemicon.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.itemicon.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));

			properties[3] = new PropertyDescriptor("itemiconPos", beanClass, "getItemIconPosition", "setItemIconPosition");
			properties[3].setDisplayName(getExternalizedString("property.itemiconPos.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.itemiconPos.short_description"));
			properties[3].setPropertyEditorClass(getEditorClass("StringComboBoxPropertyDescriptor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
