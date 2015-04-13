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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.engine.enums.CouchPostDocumentPolicy;

public class PostBulkDocumentsTransactionBeanInfo extends MySimpleBeanInfo {

	public PostBulkDocumentsTransactionBeanInfo() {
		try {
			beanClass = PostBulkDocumentsTransaction.class;
			additionalBeanClass = AbstractDatabaseTransaction.class;

			resourceBundle = getResourceBundle("res/PostBulkDocumentsTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postbulkdocuments_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postbulkdocuments_color_32x32.png";
			
			properties = new PropertyDescriptor[3];

            properties[0] = new PropertyDescriptor("policy", beanClass, "getPolicy", "setPolicy");
            properties[0].setDisplayName(getExternalizedString("property.policy.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.policy.short_description"));  
            properties[0].setPropertyEditorClass(CouchPostDocumentPolicy.class);
            
            properties[1] = new PropertyDescriptor("p_all_or_nothing", beanClass, "getP_all_or_nothing", "setP_all_or_nothing");
            properties[1].setDisplayName(getExternalizedString("property.p_all_or_nothing.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.p_all_or_nothing.short_description"));
            
            properties[2] = new PropertyDescriptor("p_new_edits", beanClass, "getP_new_edits", "setP_new_edits");
            properties[2].setDisplayName(getExternalizedString("property.p_new_edits.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.p_new_edits.short_description"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
