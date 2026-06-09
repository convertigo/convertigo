/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.beans.flow;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class FlowVirtualObjectBeanInfo extends MySimpleBeanInfo {

	public FlowVirtualObjectBeanInfo() {
		try {
			beanClass = FlowVirtualObject.class;
			additionalBeanClass = null;

			iconNameC16 = "/com/twinsoft/convertigo/beans/flow/images/flowvirtualobject_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/flow/images/flowvirtualobject_color_32x32.png";

			resourceBundle = getResourceBundle("res/FlowVirtualObject");
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[5];

			properties[0] = new PropertyDescriptor("virtualKind", beanClass, "getVirtualKind", "setVirtualKind");
			properties[0].setDisplayName(getExternalizedString("property.virtualKind.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.virtualKind.short_description"));

			properties[1] = new PropertyDescriptor("virtualType", beanClass, "getVirtualType", "setVirtualType");
			properties[1].setDisplayName(getExternalizedString("property.virtualType.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.virtualType.short_description"));

			properties[2] = new PropertyDescriptor("virtualPath", beanClass, "getVirtualPath", "setVirtualPath");
			properties[2].setDisplayName(getExternalizedString("property.virtualPath.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.virtualPath.short_description"));

			properties[3] = new PropertyDescriptor("summary", beanClass, "getSummary", "setSummary");
			properties[3].setDisplayName(getExternalizedString("property.summary.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.summary.short_description"));

			properties[4] = new PropertyDescriptor("definition", beanClass, "getDefinition", "setDefinition");
			properties[4].setDisplayName(getExternalizedString("property.definition.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.definition.short_description"));
			properties[4].setHidden(true);

			for (PropertyDescriptor property : properties) {
				property.setValue(CATEGORY, "Information");
				property.setValue(DISABLE, Boolean.TRUE);
			}
		} catch (Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass, e);
		}
	}
}
