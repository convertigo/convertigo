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

package com.twinsoft.convertigo.beans.transactions;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class SapJcoTransactionBeanInfo extends MySimpleBeanInfo {
	public SapJcoTransactionBeanInfo() {
		try {
			beanClass = SapJcoTransaction.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.TransactionWithVariables.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/images/saptransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/images/saptransaction_color_32x32.png";

			properties = new PropertyDescriptor[1];
			
			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/transactions/res/SapTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
			
			properties[0] = new PropertyDescriptor("bapiName", beanClass, "getBapiName", "setBapiName");
			properties[0].setDisplayName(getExternalizedString("property.bapiname.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.bapiname.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}

