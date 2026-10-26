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

public class JsonFormatCellsStepBeanInfo extends MySimpleBeanInfo {

	public JsonFormatCellsStepBeanInfo() {
		try {
			beanClass = JsonFormatCellsStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/jsonformatcells_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/jsonformatcells_32x32.png";

			resourceBundle = getResourceBundle("res/JsonFormatCellsStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[9];

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

			properties[3] = new PropertyDescriptor("schema", beanClass, "getSchema", "setSchema");
			properties[3].setDisplayName(getExternalizedString("property.schema.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.schema.short_description"));
			properties[3].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[3], "JsonExpressionEditor", "Input");

			properties[4] = new PropertyDescriptor("columns", beanClass, "getColumns", "setColumns");
			properties[4].setDisplayName(getExternalizedString("property.columns.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.columns.short_description"));
			properties[4].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[4], "JsonColumnsEditor", "Columns");

			properties[5] = new PropertyDescriptor("hiddenColumns", beanClass, "getHiddenColumns", "setHiddenColumns");
			properties[5].setDisplayName(getExternalizedString("property.hiddenColumns.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.hiddenColumns.short_description"));
			properties[5].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[5], "JsonColumnsEditor", "Columns");

			properties[6] = new PropertyDescriptor("valuesOnly", beanClass, "getValuesOnly", "setValuesOnly");
			properties[6].setDisplayName(getExternalizedString("property.valuesOnly.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.valuesOnly.short_description"));
			properties[6].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[6], "StringComboBoxPropertyDescriptor", "Format");

			properties[7] = new PropertyDescriptor("defaultTimezone", beanClass, "getDefaultTimezone", "setDefaultTimezone");
			properties[7].setDisplayName(getExternalizedString("property.defaultTimezone.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.defaultTimezone.short_description"));
			properties[7].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[7], "StringComboBoxPropertyDescriptor", "Format");

			properties[8] = new PropertyDescriptor("fieldsVariableName", beanClass, "getFieldsVariableName", "setFieldsVariableName");
			properties[8].setDisplayName(getExternalizedString("property.fieldsVariableName.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.fieldsVariableName.short_description"));
			setStudioEditor(properties[8], null, "Output");

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
