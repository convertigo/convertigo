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

public class AllDocsTransactionBeanInfo extends MySimpleBeanInfo {

	public AllDocsTransactionBeanInfo() {
		try {
			beanClass = AllDocsTransaction.class;
			additionalBeanClass = AbstractDatabaseTransaction.class;

			resourceBundle = getResourceBundle("res/AllDocsTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/alldocs_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/alldocs_color_32x32.png";
			
			properties = new PropertyDescriptor[16];
			
			properties[0] = new PropertyDescriptor("q_startkey", beanClass, "getQ_startkey", "setQ_startkey");
			properties[0].setDisplayName(getExternalizedString("property.q_startkey.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.q_startkey.short_description"));
			
			properties[1] = new PropertyDescriptor("q_startkey_docid", beanClass, "getQ_startkey_docid", "setQ_startkey_docid");
			properties[1].setDisplayName(getExternalizedString("property.q_startkey_docid.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.q_startkey_docid.short_description"));
			
			properties[2] = new PropertyDescriptor("q_update_seq", beanClass, "getQ_update_seq", "setQ_update_seq");
			properties[2].setDisplayName(getExternalizedString("property.q_update_seq.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.q_update_seq.short_description"));
			
			properties[3] = new PropertyDescriptor("q_stale", beanClass, "getQ_stale", "setQ_stale");
			properties[3].setDisplayName(getExternalizedString("property.q_stale.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.q_stale.short_description"));
			
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
			
			properties[8] = new PropertyDescriptor("q_skip", beanClass, "getQ_skip", "setQ_skip");
			properties[8].setDisplayName(getExternalizedString("property.q_skip.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.q_skip.short_description"));
			
			properties[9] = new PropertyDescriptor("q_limit", beanClass, "getQ_limit", "setQ_limit");
			properties[9].setDisplayName(getExternalizedString("property.q_limit.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.q_limit.short_description"));
			
			properties[10] = new PropertyDescriptor("q_include_docs", beanClass, "getQ_include_docs", "setQ_include_docs");
			properties[10].setDisplayName(getExternalizedString("property.q_include_docs.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.q_include_docs.short_description"));
			
			properties[11] = new PropertyDescriptor("q_inclusive_end", beanClass, "getQ_inclusive_end", "setQ_inclusive_end");
			properties[11].setDisplayName(getExternalizedString("property.q_inclusive_end.display_name"));
			properties[11].setShortDescription(getExternalizedString("property.q_inclusive_end.short_description"));
			
			properties[12] = new PropertyDescriptor("q_sorted", beanClass, "getQ_sorted", "setQ_sorted");
			properties[12].setDisplayName(getExternalizedString("property.q_sorted.display_name"));
			properties[12].setShortDescription(getExternalizedString("property.q_sorted.short_description"));
			
			properties[13] = new PropertyDescriptor("q_stable", beanClass, "getQ_stable", "setQ_stable");
			properties[13].setDisplayName(getExternalizedString("property.q_stable.display_name"));
			properties[13].setShortDescription(getExternalizedString("property.q_stable.short_description"));
			
			properties[14] = new PropertyDescriptor("q_key", beanClass, "getQ_key", "setQ_key");
			properties[14].setDisplayName(getExternalizedString("property.q_key.display_name"));
			properties[14].setShortDescription(getExternalizedString("property.q_key.short_description"));
			
			properties[15] = new PropertyDescriptor("q_keys", beanClass, "getQ_keys", "setQ_keys");
			properties[15].setDisplayName(getExternalizedString("property.q_keys.display_name"));
			properties[15].setShortDescription(getExternalizedString("property.q_keys.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
