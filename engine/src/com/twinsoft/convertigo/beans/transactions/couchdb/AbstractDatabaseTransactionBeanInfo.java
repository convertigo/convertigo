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

public class AbstractDatabaseTransactionBeanInfo extends MySimpleBeanInfo {

	public AbstractDatabaseTransactionBeanInfo() {
		try {
			beanClass = AbstractDatabaseTransaction.class;
			additionalBeanClass = AbstractCouchDbTransaction.class;

			resourceBundle = getResourceBundle("res/AbstractDatabaseTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");
            
			properties = new PropertyDescriptor[1];
			
			properties[0] = new PropertyDescriptor("p_db", beanClass, "getP_db", "setP_db");
			properties[0].setDisplayName(getExternalizedString("property.p_db.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.p_db.short_description"));
			properties[0].setValue(BLACK_LIST_PARENT_CLASS, "com.twinsoft.convertigo.beans.connectors.FullSyncConnector");
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
