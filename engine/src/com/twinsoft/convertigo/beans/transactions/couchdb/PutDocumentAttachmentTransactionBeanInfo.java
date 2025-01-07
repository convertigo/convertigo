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

public class PutDocumentAttachmentTransactionBeanInfo extends MySimpleBeanInfo {

	public PutDocumentAttachmentTransactionBeanInfo() {
		try {
			beanClass = PutDocumentAttachmentTransaction.class;
			additionalBeanClass = AbstractDocumentTransaction.class;

			resourceBundle = getResourceBundle("res/PutDocumentAttachmentTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/putdocumentattachment_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/putdocumentattachment_color_32x32.png";
			
			properties = new PropertyDescriptor[5];
			
			properties[0] = new PropertyDescriptor("p_attname", beanClass, "getP_attname", "setP_attname");
			properties[0].setDisplayName(getExternalizedString("property.p_attname.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.p_attname.short_description"));
			
			properties[1] = new PropertyDescriptor("p_attpath", beanClass, "getP_attpath", "setP_attpath");
			properties[1].setDisplayName(getExternalizedString("property.p_attpath.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.p_attpath.short_description"));
			
			properties[2] = new PropertyDescriptor("q_rev", beanClass, "getQ_rev", "setQ_rev");
			properties[2].setDisplayName(getExternalizedString("property.q_rev.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.q_rev.short_description"));
			
			properties[3] = new PropertyDescriptor("p_attbase64", beanClass, "getP_attbase64", "setP_attbase64");
			properties[3].setDisplayName(getExternalizedString("property.p_attbase64.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.p_attbase64.short_description"));
			
			properties[4] = new PropertyDescriptor("p_attcontent_type", beanClass, "getP_attcontent_type", "setP_attcontent_type");
			properties[4].setDisplayName(getExternalizedString("property.p_attcontent_type.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.p_attcontent_type.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
