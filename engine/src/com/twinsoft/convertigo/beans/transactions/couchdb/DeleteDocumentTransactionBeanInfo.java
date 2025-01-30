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

package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class DeleteDocumentTransactionBeanInfo extends MySimpleBeanInfo {

	public DeleteDocumentTransactionBeanInfo() {
		try {
			beanClass = DeleteDocumentTransaction.class;
			additionalBeanClass = AbstractDocumentTransaction.class;

			resourceBundle = getResourceBundle("res/DeleteDocumentTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/deletedocument_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/deletedocument_color_32x32.png";
			
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("q_rev", beanClass, "getQ_rev", "setQ_rev");
			properties[0].setDisplayName(getExternalizedString("property.q_rev.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.q_rev.short_description"));
			
			properties[1] = new PropertyDescriptor("q_batch", beanClass, "getQ_batch", "setQ_batch");
			properties[1].setDisplayName(getExternalizedString("property.q_batch.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.q_batch.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
