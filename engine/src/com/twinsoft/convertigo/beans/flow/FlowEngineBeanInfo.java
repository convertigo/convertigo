/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.flow;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class FlowEngineBeanInfo extends MySimpleBeanInfo {

	public FlowEngineBeanInfo() {
		try {
			beanClass = FlowEngine.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/flow/images/flowengine_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/flow/images/flowengine_color_32x32.png";

			resourceBundle = getResourceBundle("res/FlowEngine");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[2];

			properties[0] = new PropertyDescriptor("engineQName", beanClass, "getEngineQName", "setEngineQName");
			properties[0].setDisplayName(getExternalizedString("property.engineQName.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.engineQName.short_description"));

			properties[1] = new PropertyDescriptor("engineSource", beanClass, "getEngineSource", "setEngineSource");
			properties[1].setDisplayName(getExternalizedString("property.engineSource.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.engineSource.short_description"));
			properties[1].setValue(MULTILINE, Boolean.TRUE);
			properties[1].setValue(GENERIC_EDITOR_EXTENSION, "yaml");
		} catch (Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
