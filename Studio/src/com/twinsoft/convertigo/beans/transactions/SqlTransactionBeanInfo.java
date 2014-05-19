/*
 * Copyright (c) 2009-2014 Convertigo. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of  Convertigo  or in accordance  with  the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo makes  no  representations  or  warranties  about  the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
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

			properties = new PropertyDescriptor[7];
			
			resourceBundle = getResourceBundle("res/SqlTransaction");
			
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
			
			properties[4] = new PropertyDescriptor("autoCommit", beanClass, "getAutoCommit", "setAutoCommit");
			properties[4].setExpert(true);
			properties[4].setDisplayName(getExternalizedString("property.autoCommit.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.autoCommit.short_description"));
			properties[4].setPropertyEditorClass(getEditorClass("SqlCommitSelectionEditor"));
			
			properties[5] = new PropertyDescriptor("xmlDefaultRowTagname", beanClass, "getXmlDefaultRowTagname", "setXmlDefaultRowTagname");
			properties[5].setDisplayName(getExternalizedString("property.xmlDefaultRowTagname.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.xmlDefaultRowTagname.short_description"));
			
			properties[6] = new PropertyDescriptor("xmlDefaultColumnTagname", beanClass, "getXmlDefaultColumnTagname", "setXmlDefaultColumnTagname");
			properties[6].setDisplayName(getExternalizedString("property.xmlDefaultColumnTagname.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.xmlDefaultColumnTagname.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
