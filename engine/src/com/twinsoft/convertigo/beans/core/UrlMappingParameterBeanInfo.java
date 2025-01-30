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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataContent;

public class UrlMappingParameterBeanInfo extends MySimpleBeanInfo {

	public UrlMappingParameterBeanInfo() {
		try {
			beanClass = UrlMappingParameter.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/urlmappingparameter_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/urlmappingparameter_color_32x32.png";

			resourceBundle = getResourceBundle("res/UrlMappingParameter");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[8];
			properties[0] = new PropertyDescriptor("required", beanClass, "isRequired", "setRequired");
			properties[0].setDisplayName(getExternalizedString("property.required.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.required.short_description"));

			properties[1] = new PropertyDescriptor("multiValued", beanClass, "isMultiValued", "setMultiValued");
			properties[1].setDisplayName(getExternalizedString("property.multiValued.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.multiValued.short_description"));

			properties[2] = new PropertyDescriptor("mappedVariableName", beanClass, "getMappedVariableName", "setMappedVariableName");
			properties[2].setDisplayName(getExternalizedString("property.mappedVariableName.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.mappedVariableName.short_description"));

			properties[3] = new PropertyDescriptor("inputContent", beanClass, "getInputContent", "setInputContent");
			properties[3].setDisplayName(getExternalizedString("property.inputContent.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.inputContent.short_description"));
			properties[3].setPropertyEditorClass(DataContent.class);
			properties[3].setHidden(true);

			properties[4] = new PropertyDescriptor("inputType", beanClass, "getInputType", "setInputType");
			properties[4].setDisplayName(getExternalizedString("property.inputType.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.inputType.short_description"));
			properties[4].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));

			properties[5] = new PropertyDescriptor("array", beanClass, "isArray", "setArray");
			properties[5].setDisplayName(getExternalizedString("property.array.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.array.short_description"));

			properties[6] = new PropertyDescriptor("exposed", beanClass, "isExposed", "setExposed");
			properties[6].setDisplayName(getExternalizedString("property.exposed.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.exposed.short_description"));

			properties[7] = new PropertyDescriptor("value", beanClass, "getDefaultValue", "setDefaultValue");
			properties[7].setDisplayName(getExternalizedString("property.value.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.value.short_description"));
			properties[7].setValue(NILLABLE, Boolean.TRUE);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
