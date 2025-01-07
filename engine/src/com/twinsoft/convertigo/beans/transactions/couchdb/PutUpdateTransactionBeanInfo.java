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

public class PutUpdateTransactionBeanInfo extends MySimpleBeanInfo {

	public PutUpdateTransactionBeanInfo() {
		try {
			beanClass = PutUpdateTransaction.class;
			additionalBeanClass = AbstractDocumentTransaction.class;

			resourceBundle = getResourceBundle("res/PutUpdateTransaction");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/putupdate_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/putupdate_color_32x32.png";
			
			properties = new PropertyDescriptor[4];

            properties[0] = new PropertyDescriptor("updatename", beanClass, "getUpdatename", "setUpdatename");
            properties[0].setDisplayName(getExternalizedString("property.updatename.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.updatename.short_description")); 
            properties[0].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
            
            properties[1] = new PropertyDescriptor("p_ddoc", beanClass, "getP_ddoc", "setP_ddoc");
            properties[1].setDisplayName(getExternalizedString("property.p_ddoc.display_name"));
            properties[1].setShortDescription(getExternalizedString("property.p_ddoc.short_description")); 
            
            properties[2] = new PropertyDescriptor("p_func", beanClass, "getP_func", "setP_func");
            properties[2].setDisplayName(getExternalizedString("property.p_func.display_name"));
            properties[2].setShortDescription(getExternalizedString("property.p_func.short_description"));
            
            properties[3] = new PropertyDescriptor("p_json_base", beanClass, "getP_json_base", "setP_json_base");
            properties[3].setDisplayName(getExternalizedString("property.p_json_base.display_name"));
            properties[3].setShortDescription(getExternalizedString("property.p_json_base.short_description"));
            
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
