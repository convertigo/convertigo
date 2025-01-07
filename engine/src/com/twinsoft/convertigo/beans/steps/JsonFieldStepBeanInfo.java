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

package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.enums.JsonFieldType;

public class JsonFieldStepBeanInfo extends MySimpleBeanInfo {

	public JsonFieldStepBeanInfo() {
		try {
			beanClass = JsonFieldStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/jsonfield_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/jsonfield_32x32.png";

			resourceBundle = getResourceBundle("res/JsonFieldStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[3];

			properties[0] = new PropertyDescriptor("key", beanClass, "getKey", "setKey");
			properties[0].setDisplayName(getExternalizedString("property.key.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.key.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));

			properties[1] = new PropertyDescriptor("value", beanClass, "getValue", "setValue");
			properties[1].setDisplayName(getExternalizedString("property.value.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.value.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));

			properties[2] = new PropertyDescriptor("type", beanClass, "getType", "setType");
			properties[2].setDisplayName(getExternalizedString("property.type.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.type.short_description"));
			properties[2].setPropertyEditorClass(JsonFieldType.class);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
