/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

public class GetServerDatabasesTransactionBeanInfo extends MySimpleBeanInfo {

	public GetServerDatabasesTransactionBeanInfo() {
		try {
			beanClass = GetServerDatabasesTransaction.class;
			additionalBeanClass = AbstractCouchDbTransaction.class;

			resourceBundle = getResourceBundle("res/GetServerDatabasesTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/getserverdatabases_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/getserverdatabases_color_32x32.png";
			
			properties = new PropertyDescriptor[5];
			
			properties[0] = new PropertyDescriptor("q_startkey", beanClass, "getQ_startkey", "setQ_startkey");
			properties[0].setDisplayName(getExternalizedString("property.q_startkey.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.q_startkey.short_description"));
			
			properties[1] = new PropertyDescriptor("q_descending", beanClass, "getQ_descending", "setQ_descending");
			properties[1].setDisplayName(getExternalizedString("property.q_descending.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.q_descending.short_description"));
			
			properties[2] = new PropertyDescriptor("q_endkey", beanClass, "getQ_endkey", "setQ_endkey");
			properties[2].setDisplayName(getExternalizedString("property.q_endkey.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.q_endkey.short_description"));
			
			properties[3] = new PropertyDescriptor("q_skip", beanClass, "getQ_skip", "setQ_skip");
			properties[3].setDisplayName(getExternalizedString("property.q_skip.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.q_skip.short_description"));
			
			properties[4] = new PropertyDescriptor("q_limit", beanClass, "getQ_limit", "setQ_limit");
			properties[4].setDisplayName(getExternalizedString("property.q_limit.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.q_limit.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
