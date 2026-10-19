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

public class JsonDistinctStepBeanInfo extends MySimpleBeanInfo {

	public JsonDistinctStepBeanInfo() {
		try {
			beanClass = JsonDistinctStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Step.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/jsondistinct_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/jsondistinct_32x32.png";

			resourceBundle = getResourceBundle("res/JsonDistinctStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[4];

			properties[0] = new PropertyDescriptor("rows", beanClass, "getRows", "setRows");
			properties[0].setDisplayName(getExternalizedString("property.rows.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.rows.short_description"));
			properties[0].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[0], "JsonExpressionEditor", "Input");

			properties[1] = new PropertyDescriptor("variableName", beanClass, "getVariableName", "setVariableName");
			properties[1].setDisplayName(getExternalizedString("property.variableName.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.variableName.short_description"));
			setStudioEditor(properties[1], null, "Output");

			properties[2] = new PropertyDescriptor("columns", beanClass, "getColumns", "setColumns");
			properties[2].setDisplayName(getExternalizedString("property.columns.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.columns.short_description"));
			properties[2].setValue(SCRIPTABLE, Boolean.TRUE);
			setStudioEditor(properties[2], "JsonColumnsEditor", "Columns");

			properties[3] = new PropertyDescriptor("valueKey", beanClass, "getValueKey", "setValueKey");
			properties[3].setDisplayName(getExternalizedString("property.valueKey.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.valueKey.short_description"));
			setStudioEditor(properties[3], null, "Columns");

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
