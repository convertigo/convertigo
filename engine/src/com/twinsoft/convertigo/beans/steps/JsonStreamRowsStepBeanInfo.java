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

public class JsonStreamRowsStepBeanInfo extends MySimpleBeanInfo {

	public JsonStreamRowsStepBeanInfo() {
		try {
			beanClass = JsonStreamRowsStep.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.steps.JsonToXmlStep.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/steps/images/jsontoxml_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/steps/images/jsontoxml_32x32.png";

			resourceBundle = getResourceBundle("res/JsonStreamRowsStep");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[20];

			properties[0] = new PropertyDescriptor("streamMode", beanClass, "getStreamMode", "setStreamMode");
			properties[0].setDisplayName(getExternalizedString("property.streamMode.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.streamMode.short_description"));
			properties[0].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[1] = new PropertyDescriptor("rows", beanClass, "getRows", "setRows");
			properties[1].setDisplayName(getExternalizedString("property.rows.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.rows.short_description"));
			properties[1].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[2] = new PropertyDescriptor("firstPage", beanClass, "getFirstPage", "setFirstPage");
			properties[2].setDisplayName(getExternalizedString("property.firstPage.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.firstPage.short_description"));
			properties[2].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[3] = new PropertyDescriptor("nextUrl", beanClass, "getNextUrl", "setNextUrl");
			properties[3].setDisplayName(getExternalizedString("property.nextUrl.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.nextUrl.short_description"));
			properties[3].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[4] = new PropertyDescriptor("baseUrl", beanClass, "getBaseUrl", "setBaseUrl");
			properties[4].setDisplayName(getExternalizedString("property.baseUrl.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.baseUrl.short_description"));
			properties[4].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[5] = new PropertyDescriptor("authorization", beanClass, "getAuthorization", "setAuthorization");
			properties[5].setDisplayName(getExternalizedString("property.authorization.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.authorization.short_description"));
			properties[5].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[6] = new PropertyDescriptor("maxRows", beanClass, "getMaxRows", "setMaxRows");
			properties[6].setDisplayName(getExternalizedString("property.maxRows.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.maxRows.short_description"));
			properties[6].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[7] = new PropertyDescriptor("schema", beanClass, "getSchema", "setSchema");
			properties[7].setDisplayName(getExternalizedString("property.schema.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.schema.short_description"));
			properties[7].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[8] = new PropertyDescriptor("columns", beanClass, "getColumns", "setColumns");
			properties[8].setDisplayName(getExternalizedString("property.columns.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.columns.short_description"));
			properties[8].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[9] = new PropertyDescriptor("hiddenColumns", beanClass, "getHiddenColumns", "setHiddenColumns");
			properties[9].setDisplayName(getExternalizedString("property.hiddenColumns.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.hiddenColumns.short_description"));
			properties[9].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[10] = new PropertyDescriptor("valuesOnly", beanClass, "getValuesOnly", "setValuesOnly");
			properties[10].setDisplayName(getExternalizedString("property.valuesOnly.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.valuesOnly.short_description"));
			properties[10].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[11] = new PropertyDescriptor("defaultTimezone", beanClass, "getDefaultTimezone", "setDefaultTimezone");
			properties[11].setDisplayName(getExternalizedString("property.defaultTimezone.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.defaultTimezone.short_description"));
			properties[11].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[12] = new PropertyDescriptor("distinctColumns", beanClass, "getDistinctColumns", "setDistinctColumns");
			properties[12].setDisplayName(getExternalizedString("property.distinctColumns.display_name"));
			properties[12].setShortDescription(getExternalizedString("property.distinctColumns.short_description"));
			properties[12].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[13] = new PropertyDescriptor("firstColumns", beanClass, "getFirstColumns", "setFirstColumns");
			properties[13].setDisplayName(getExternalizedString("property.firstColumns.display_name"));
			properties[13].setShortDescription(getExternalizedString("property.firstColumns.short_description"));
			properties[13].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[14] = new PropertyDescriptor("outputColumns", beanClass, "getOutputColumns", "setOutputColumns");
			properties[14].setDisplayName(getExternalizedString("property.outputColumns.display_name"));
			properties[14].setShortDescription(getExternalizedString("property.outputColumns.short_description"));
			properties[14].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[15] = new PropertyDescriptor("columnsOrder", beanClass, "getColumnsOrder", "setColumnsOrder");
			properties[15].setDisplayName(getExternalizedString("property.columnsOrder.display_name"));
			properties[15].setShortDescription(getExternalizedString("property.columnsOrder.short_description"));
			properties[15].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[16] = new PropertyDescriptor("failPage", beanClass, "getFailPage", "setFailPage");
			properties[16].setDisplayName(getExternalizedString("property.failPage.display_name"));
			properties[16].setShortDescription(getExternalizedString("property.failPage.short_description"));
			properties[16].setValue(SCRIPTABLE, Boolean.TRUE);
			properties[16].setExpert(true);

			properties[17] = new PropertyDescriptor("fieldsVariableName", beanClass, "getFieldsVariableName", "setFieldsVariableName");
			properties[17].setDisplayName(getExternalizedString("property.fieldsVariableName.display_name"));
			properties[17].setShortDescription(getExternalizedString("property.fieldsVariableName.short_description"));

			properties[18] = new PropertyDescriptor("errorVariableName", beanClass, "getErrorVariableName", "setErrorVariableName");
			properties[18].setDisplayName(getExternalizedString("property.errorVariableName.display_name"));
			properties[18].setShortDescription(getExternalizedString("property.errorVariableName.short_description"));

			properties[19] = new PropertyDescriptor("restartPageSize", beanClass, "getRestartPageSize", "setRestartPageSize");
			properties[19].setDisplayName(getExternalizedString("property.restartPageSize.display_name"));
			properties[19].setShortDescription(getExternalizedString("property.restartPageSize.short_description"));
			properties[19].setValue(SCRIPTABLE, Boolean.TRUE);

		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
