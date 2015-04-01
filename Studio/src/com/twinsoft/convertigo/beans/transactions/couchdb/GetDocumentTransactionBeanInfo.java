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

public class GetDocumentTransactionBeanInfo extends MySimpleBeanInfo {

	public GetDocumentTransactionBeanInfo() {
		try {
			beanClass = GetDocumentTransaction.class;
			additionalBeanClass = AbstractDocumentTransaction.class;

			resourceBundle = getResourceBundle("res/GetDocumentTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/databasetransaction_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/databasetransaction_color_32x32.png";
			
			getPropertyDescriptor("xmlComplexTypeAffectation").setHidden(false);
			
			properties = new PropertyDescriptor[12];
			
			properties[0] = new PropertyDescriptor("q_attachments", beanClass, "getQ_attachments", "setQ_attachments");
			properties[0].setDisplayName(getExternalizedString("property.q_attachments.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.q_attachments.short_description"));
			
			properties[1] = new PropertyDescriptor("q_q_att_encoding_info", beanClass, "getQ_att_encoding_info", "setQ_att_encoding_info");
			properties[1].setDisplayName(getExternalizedString("property.q_att_encoding_info.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.q_att_encoding_info.short_description"));
			
			properties[2] = new PropertyDescriptor("q_atts_since", beanClass, "getQ_atts_since", "setQ_atts_since");
			properties[2].setDisplayName(getExternalizedString("property.q_atts_since.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.q_atts_since.short_description"));
			
			properties[3] = new PropertyDescriptor("q_conflicts", beanClass, "getQ_conflicts", "setQ_conflicts");
			properties[3].setDisplayName(getExternalizedString("property.q_conflicts.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.q_conflicts.short_description"));
			
			properties[4] = new PropertyDescriptor("q_deleted_conflicts", beanClass, "getQ_deleted_conflicts", "setQ_deleted_conflicts");
			properties[4].setDisplayName(getExternalizedString("property.q_deleted_conflicts.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.q_deleted_conflicts.short_description"));
			
			properties[5] = new PropertyDescriptor("q_latest", beanClass, "getQ_latest", "setQ_latest");
			properties[5].setDisplayName(getExternalizedString("property.q_latest.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.q_latest.short_description"));
			
			properties[6] = new PropertyDescriptor("q_local_seq", beanClass, "getQ_local_seq", "setQ_local_seq");
			properties[6].setDisplayName(getExternalizedString("property.q_local_seq.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.q_local_seq.short_description"));
			
			properties[7] = new PropertyDescriptor("q_meta", beanClass, "getQ_meta", "setQ_meta");
			properties[7].setDisplayName(getExternalizedString("property.q_meta.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.q_meta.short_description"));
			
			properties[8] = new PropertyDescriptor("q_open_revs", beanClass, "getQ_open_revs", "setQ_open_revs");
			properties[8].setDisplayName(getExternalizedString("property.q_open_revs.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.q_open_revs.short_description"));
			
			properties[9] = new PropertyDescriptor("q_rev", beanClass, "getQ_rev", "setQ_rev");
			properties[9].setDisplayName(getExternalizedString("property.q_rev.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.q_rev.short_description"));
			
			properties[10] = new PropertyDescriptor("q_revs", beanClass, "getQ_revs", "setQ_revs");
			properties[10].setDisplayName(getExternalizedString("property.q_revs.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.q_revs.short_description"));
			
			properties[11] = new PropertyDescriptor("q_revs_info", beanClass, "getQ_revs_info", "setQ_revs_info");
			properties[11].setDisplayName(getExternalizedString("property.q_revs_info.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.q_revs_info.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
