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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/beans/transactions/XmlHttpTransactionBeanInfo.java $
 * $Author: elodiee $
 * $Revision: 33010 $
 * $Date: 2012-12-14 15:07:12 +0100 (ven., 14 déc. 2012) $
 */

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class JsonHttpTransactionBeanInfo extends MySimpleBeanInfo {
    
	public JsonHttpTransactionBeanInfo() {
		try {
			beanClass = JsonHttpTransaction.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.transactions.HttpTransaction.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/jsonhttptransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/jsonhttptransaction_color_32x32.png";

			properties = new PropertyDescriptor[2];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/JsonHttpTransaction");
			
			properties[0] = new PropertyDescriptor("jsonEncoding", JsonHttpTransaction.class, "getJsonEncoding", "setJsonEncoding");
			properties[0].setDisplayName(getExternalizedString("property.jsonEncoding.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.jsonEncoding.short_description"));
			properties[0].setExpert(true);
			
			properties[1] = new PropertyDescriptor("includeDataType", JsonHttpTransaction.class, "getIncludeDataType", "setIncludeDataType");
			properties[1].setDisplayName(getExternalizedString("property.includeDataType.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.includeDataType.short_description"));
			properties[1].setExpert(true);

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
