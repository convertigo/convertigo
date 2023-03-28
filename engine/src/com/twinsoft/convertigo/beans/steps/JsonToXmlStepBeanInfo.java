/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

public class JsonToXmlStepBeanInfo extends MySimpleBeanInfo {

	public JsonToXmlStepBeanInfo() {
		try {
			beanClass = JsonToXmlStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/jsontoxml_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/jsontoxml_32x32.png";

			resourceBundle = getResourceBundle("res/JsonToXmlStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[3];

			properties[0] = new PropertyDescriptor("key", beanClass, "getKey", "setKey");
			properties[0].setDisplayName(getExternalizedString("property.key.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.key.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));

			properties[1] = new PropertyDescriptor("jsonObject", beanClass, "getJsonObject", "setJsonObject");
			properties[1].setDisplayName(getExternalizedString("property.jsonObject.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.jsonObject.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("SmartTypeCellEditor"));
			properties[1].setValue(GENERIC_EDITOR_EXTENSION, "json");

			properties[2] = new PropertyDescriptor("jsonSample", beanClass, "getJsonSample", "setJsonSample");
			properties[2].setDisplayName(getExternalizedString("property.jsonSample.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.jsonSample.short_description"));
			properties[2].setValue(GENERIC_EDITOR_EXTENSION, "json");
			properties[2].setValue(MULTILINE, true);
			properties[2].setExpert(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}	
}
