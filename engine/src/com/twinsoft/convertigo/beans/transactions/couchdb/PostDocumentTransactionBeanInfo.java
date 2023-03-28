/*
 * Copyright (c) 2001-2023 Convertigo SA.
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
import com.twinsoft.convertigo.engine.enums.CouchPostDocumentPolicy;
import com.twinsoft.convertigo.engine.enums.FullSyncAclPolicy;

public class PostDocumentTransactionBeanInfo extends MySimpleBeanInfo {

	public PostDocumentTransactionBeanInfo() {
		try {
			beanClass = PostDocumentTransaction.class;
			additionalBeanClass = AbstractDatabaseTransaction.class;

			resourceBundle = getResourceBundle("res/PostDocumentTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postdocument_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postdocument_color_32x32.png";
			
			properties = new PropertyDescriptor[6];

			properties[0] = new PropertyDescriptor("policy", beanClass, "getPolicy", "setPolicy");
			properties[0].setDisplayName(getExternalizedString("property.policy.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.policy.short_description"));  
			properties[0].setPropertyEditorClass(CouchPostDocumentPolicy.class);

			properties[1] = new PropertyDescriptor("p_json_base", beanClass, "getP_json_base", "setP_json_base");
			properties[1].setDisplayName(getExternalizedString("property.p_json_base.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.p_json_base.short_description"));

			properties[2] = new PropertyDescriptor("q_batch", beanClass, "getQ_batch", "setQ_batch");
			properties[2].setDisplayName(getExternalizedString("property.q_batch.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.q_batch.short_description"));

			properties[3] = new PropertyDescriptor("useHash", beanClass, "isUseHash", "setUseHash");
			properties[3].setDisplayName(getExternalizedString("property.useHash.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.useHash.short_description"));
			properties[3].setValue(BLACK_LIST_PARENT_CLASS, "com.twinsoft.convertigo.beans.connectors.CouchDbConnector");

			properties[4] = new PropertyDescriptor("fullSyncAclPolicy", beanClass, "getFullSyncAclPolicy", "setFullSyncAclPolicy");
			properties[4].setDisplayName(getExternalizedString("property.fullSyncAclPolicy.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.fullSyncAclPolicy.short_description"));
			properties[4].setPropertyEditorClass(FullSyncAclPolicy.class);
			properties[4].setValue(BLACK_LIST_PARENT_CLASS, "com.twinsoft.convertigo.beans.connectors.CouchDbConnector");

			properties[5] = new PropertyDescriptor("p_merge", beanClass, "getP_merge", "setP_merge");
			properties[5].setDisplayName(getExternalizedString("property.p_merge.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.p_merge.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
