/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class SqlTransactionBeanInfo extends MySimpleBeanInfo {
	public SqlTransactionBeanInfo() {
		try {
			beanClass = SqlTransaction.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.TransactionWithVariables.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/sqltransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/sqltransaction_color_32x32.png";

			properties = new PropertyDescriptor[4];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/SqlTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("sqlQuery", beanClass, "getSqlQuery", "setSqlQuery");
			properties[0].setDisplayName(getExternalizedString("property.sqlQuery.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.sqlQuery.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("SqlQueryEditor"));
			
			properties[1] = new PropertyDescriptor("xmlOutput", beanClass, "getXmlOutput", "setXmlOutput");
			properties[1].setExpert(true);
			properties[1].setDisplayName(getExternalizedString("property.xmlOutput.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.xmlOutput.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("SqlOutputSelectionEditor"));

			properties[2] = new PropertyDescriptor("xmlGrouping", beanClass, "getXmlGrouping", "setXmlGrouping");
			properties[2].setExpert(true);
			properties[2].setDisplayName(getExternalizedString("property.xmlGrouping.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.xmlGrouping.short_description"));
			//properties[2].setPropertyEditorClass(getPropertyEditorClass("SqlOutputSelectionEditor"));
			
			properties[3] = new PropertyDescriptor("maxResult", beanClass, "getMaxResult", "setMaxResult");
			properties[3].setExpert(true);
			properties[3].setDisplayName(getExternalizedString("property.maxResult.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.maxResult.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
