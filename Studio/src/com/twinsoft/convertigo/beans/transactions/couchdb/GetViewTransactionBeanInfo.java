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

public class GetViewTransactionBeanInfo extends MySimpleBeanInfo {

	public GetViewTransactionBeanInfo() {
		try {
			beanClass = GetViewTransaction.class;
			additionalBeanClass = AbstractDocumentTransaction.class;

			resourceBundle = getResourceBundle("res/GetViewTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/databasetransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/databasetransaction_color_32x32.png";	
			
			properties = new PropertyDescriptor[22];
			
			properties[0] = new PropertyDescriptor("viewname", beanClass, "getViewname", "setViewname");
			properties[0].setDisplayName(getExternalizedString("property.viewname.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.viewname.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
//			properties[0].setPropertyEditorClass(getEditorClass("DesignDocumentViewSelectorEditor"));
            
			properties[1] = new PropertyDescriptor("u_db", beanClass, "getU_db", "setU_db");
			properties[1].setDisplayName(getExternalizedString("property.u_db.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.u_db.short_description"));
			
			properties[2] = new PropertyDescriptor("u_ddoc", beanClass, "getU_ddoc", "setU_ddoc");
			properties[2].setDisplayName(getExternalizedString("property.u_ddoc.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.u_ddoc.short_description"));
			
			properties[3] = new PropertyDescriptor("u_view", beanClass, "getU_view", "setU_view");
			properties[3].setDisplayName(getExternalizedString("property.u_view.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.u_view.short_description"));
			
			properties[4] = new PropertyDescriptor("q_conflicts", beanClass, "getQ_conflicts", "setQ_conflicts");
			properties[4].setDisplayName(getExternalizedString("property.q_conflicts.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.q_conflicts.short_description"));
			
			properties[5] = new PropertyDescriptor("q_descending", beanClass, "getQ_descending", "setQ_descending");
			properties[5].setDisplayName(getExternalizedString("property.q_descending.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.q_descending.short_description"));
			
			properties[6] = new PropertyDescriptor("q_endkey", beanClass, "getQ_endkey", "setQ_endkey");
			properties[6].setDisplayName(getExternalizedString("property.q_endkey.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.q_endkey.short_description"));
			
			properties[7] = new PropertyDescriptor("q_endkey_docid", beanClass, "getQ_endkey_docid", "setQ_endkey_docid");
			properties[7].setDisplayName(getExternalizedString("property.q_endkey_docid.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.q_endkey_docid.short_description"));
			
			properties[8] = new PropertyDescriptor("q_group", beanClass, "getQ_group", "setQ_group");
			properties[8].setDisplayName(getExternalizedString("property.q_group.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.q_group.short_description"));
			
			properties[9] = new PropertyDescriptor("q_group_level", beanClass, "getQ_group_level", "setQ_group_level");
			properties[9].setDisplayName(getExternalizedString("property.q_group_level.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.q_group_level.short_description"));
			
			properties[10] = new PropertyDescriptor("q_include_docs", beanClass, "getQ_include_docs", "setQ_include_docs");
			properties[10].setDisplayName(getExternalizedString("property.q_include_docs.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.q_include_docs.short_description"));
			
			properties[11] = new PropertyDescriptor("q_attachments", beanClass, "getQ_attachments", "setQ_attachments");
			properties[11].setDisplayName(getExternalizedString("property.q_attachments.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.q_attachments.short_description"));
			
			properties[12] = new PropertyDescriptor("q_att_encoding_info", beanClass, "getQ_att_encoding_info", "setQ_att_encoding_info");
			properties[12].setDisplayName(getExternalizedString("property.q_att_encoding_info.display_name"));
			properties[12].setShortDescription(getExternalizedString("property.q_att_encoding_info.short_description"));
			
			properties[13] = new PropertyDescriptor("q_inclusive_end", beanClass, "getQ_inclusive_end", "setQ_inclusive_end");
			properties[13].setDisplayName(getExternalizedString("property.q_inclusive_end.display_name"));
			properties[13].setShortDescription(getExternalizedString("property.q_inclusive_end.short_description"));
			
			properties[14] = new PropertyDescriptor("q_key", beanClass, "getQ_key", "setQ_key");
			properties[14].setDisplayName(getExternalizedString("property.q_key.display_name"));
			properties[14].setShortDescription(getExternalizedString("property.q_key.short_description"));
			
			properties[15] = new PropertyDescriptor("q_limit", beanClass, "getQ_limit", "setQ_limit");
			properties[15].setDisplayName(getExternalizedString("property.q_limit.display_name"));
			properties[15].setShortDescription(getExternalizedString("property.q_limit.short_description"));
			
			properties[16] = new PropertyDescriptor("q_reduce", beanClass, "getQ_reduce", "setQ_reduce");
			properties[16].setDisplayName(getExternalizedString("property.q_reduce.display_name"));
			properties[16].setShortDescription(getExternalizedString("property.q_reduce.short_description"));
			
			properties[17] = new PropertyDescriptor("q_skip", beanClass, "getQ_skip", "setQ_skip");
			properties[17].setDisplayName(getExternalizedString("property.q_skip.display_name"));
			properties[17].setShortDescription(getExternalizedString("property.q_skip.short_description"));
			
			properties[18] = new PropertyDescriptor("q_stale", beanClass, "getQ_stale", "setQ_stale");
			properties[18].setDisplayName(getExternalizedString("property.q_stale.display_name"));
			properties[18].setShortDescription(getExternalizedString("property.q_stale.short_description"));
			
			properties[19] = new PropertyDescriptor("q_startkey", beanClass, "getQ_startkey", "setQ_startkey");
			properties[19].setDisplayName(getExternalizedString("property.q_startkey.display_name"));
			properties[19].setShortDescription(getExternalizedString("property.q_startkey.short_description"));
			
			properties[20] = new PropertyDescriptor("q_startkey_docid", beanClass, "getQ_startkey_docid", "setQ_startkey_docid");
			properties[20].setDisplayName(getExternalizedString("property.q_startkey_docid.display_name"));
			properties[20].setShortDescription(getExternalizedString("property.q_startkey_docid.short_description"));
			
			properties[21] = new PropertyDescriptor("q_update_seq", beanClass, "getQ_update_seq", "setQ_update_seq");
			properties[21].setDisplayName(getExternalizedString("property.q_update_seq.display_name"));
			properties[21].setShortDescription(getExternalizedString("property.q_update_seq.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
