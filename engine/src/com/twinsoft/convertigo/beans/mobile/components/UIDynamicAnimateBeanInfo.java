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
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicAnimate.ApplyMode;

public class UIDynamicAnimateBeanInfo extends MySimpleBeanInfo {

	public UIDynamicAnimateBeanInfo() {
		try {
			beanClass = UIDynamicAnimate.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction.class;

			resourceBundle = getResourceBundle("res/UIDynamicAnimate");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[2];

			properties[0] = new PropertyDescriptor("identifiable", beanClass, "getIdentifiable", "setIdentifiable");
			properties[0].setDisplayName(getExternalizedString("property.identifiable.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.identifiable.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("NamedSourceSelectorEditor"));
			properties[0].setValue(CATEGORY, "@Component");

			properties[1] = new PropertyDescriptor("mode", beanClass, "getMode", "setMode");
			properties[1].setDisplayName(getExternalizedString("property.mode.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.mode.short_description"));
			properties[1].setPropertyEditorClass(ApplyMode.class);
			properties[1].setValue(CATEGORY, "@Component");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
