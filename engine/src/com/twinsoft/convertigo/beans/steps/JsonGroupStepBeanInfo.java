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


package com.twinsoft.convertigo.beans.steps;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class JsonGroupStepBeanInfo extends MySimpleBeanInfo {

	public JsonGroupStepBeanInfo() {
		try {
			beanClass = JsonGroupStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/jsongroup_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/jsongroup_32x32.png";

			resourceBundle = getResourceBundle("res/JsonGroupStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[8];

			properties[0] = new PropertyDescriptor("rows", beanClass, "getRows", "setRows");
			properties[0].setDisplayName(getExternalizedString("property.rows.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.rows.short_description"));
			properties[0].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[0], "JsonExpressionEditor", "Input");

			properties[1] = new PropertyDescriptor("variableName", beanClass, "getVariableName", "setVariableName");
			properties[1].setDisplayName(getExternalizedString("property.variableName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.variableName.short_description"));
			setStudioEditor(properties[1], null, "Output");

			properties[2] = new PropertyDescriptor("format", beanClass, "getFormat", "setFormat");
			properties[2].setDisplayName(getExternalizedString("property.format.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.format.short_description"));
			setStudioEditor(properties[2], "StringComboBoxPropertyDescriptor", "Format");

			properties[3] = new PropertyDescriptor("groupColumns", beanClass, "getGroupColumns", "setGroupColumns");
			properties[3].setDisplayName(getExternalizedString("property.groupColumns.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.groupColumns.short_description"));
			properties[3].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[3], "JsonColumnsEditor", "Columns");

			properties[4] = new PropertyDescriptor("aggregations", beanClass, "getAggregations", "setAggregations");
			properties[4].setDisplayName(getExternalizedString("property.aggregations.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.aggregations.short_description"));
			properties[4].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[4], "JsonAggregationsEditor", "Columns");

			properties[5] = new PropertyDescriptor("schema", beanClass, "getSchema", "setSchema");
			properties[5].setDisplayName(getExternalizedString("property.schema.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.schema.short_description"));
			properties[5].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[5], "JsonExpressionEditor", "Input");

			properties[6] = new PropertyDescriptor("valueKey", beanClass, "getValueKey", "setValueKey");
			properties[6].setDisplayName(getExternalizedString("property.valueKey.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.valueKey.short_description"));
			setStudioEditor(properties[6], null, "Columns");

			properties[7] = new PropertyDescriptor("errorVariableName", beanClass, "getErrorVariableName", "setErrorVariableName");
			properties[7].setDisplayName(getExternalizedString("property.errorVariableName.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.errorVariableName.short_description"));
			setStudioEditor(properties[7], null, "Output");

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
