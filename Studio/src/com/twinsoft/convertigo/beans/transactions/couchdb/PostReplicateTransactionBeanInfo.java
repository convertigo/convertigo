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

public class PostReplicateTransactionBeanInfo extends MySimpleBeanInfo {

	public PostReplicateTransactionBeanInfo() {
		try {
			beanClass = PostReplicateTransaction.class;
			additionalBeanClass = AbstractDatabaseTransaction.class;

			resourceBundle = getResourceBundle("res/PostReplicateTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postreplicate_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postreplicate_color_32x32.png";
			
			properties = new PropertyDescriptor[7];
			
            properties[0] = new PropertyDescriptor("p_cancel", beanClass, "getP_cancel", "setP_cancel");
            properties[0].setDisplayName(getExternalizedString("property.p_cancel.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.p_cancel.short_description"));
            
            properties[1] = new PropertyDescriptor("p_continuous", beanClass, "getP_continuous", "setP_continuous");
            properties[1].setDisplayName(getExternalizedString("property.p_continuous.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.p_continuous.short_description"));
            
            properties[2] = new PropertyDescriptor("p_create_target", beanClass, "getP_target", "setP_target");
            properties[2].setDisplayName(getExternalizedString("property.p_create_target.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.p_create_target.short_description"));
            
            properties[3] = new PropertyDescriptor("p_doc_ids", beanClass, "getP_doc_ids", "setP_doc_ids");
            properties[3].setDisplayName(getExternalizedString("property.p_doc_ids.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.p_doc_ids.short_description"));
            
            properties[4] = new PropertyDescriptor("p_proxy", beanClass, "getP_proxy", "setP_proxy");
            properties[4].setDisplayName(getExternalizedString("property.p_proxy.display_name"));
            properties[4].setShortDescription(getExternalizedString("property.p_proxy.short_description"));
            
            properties[5] = new PropertyDescriptor("p_source", beanClass, "getP_source", "setP_source");
            properties[5].setDisplayName(getExternalizedString("property.p_source.display_name"));
            properties[5].setShortDescription(getExternalizedString("property.p_source.short_description"));
            
            properties[6] = new PropertyDescriptor("p_target", beanClass, "getP_target", "setP_target");
            properties[6].setDisplayName(getExternalizedString("property.p_target.display_name"));
            properties[6].setShortDescription(getExternalizedString("property.p_target.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
