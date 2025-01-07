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

public class PostFindTransactionBeanInfo extends MySimpleBeanInfo {

	public PostFindTransactionBeanInfo() {
		try {
			beanClass = PostFindTransaction.class;
			additionalBeanClass = AbstractDatabaseTransaction.class;

			resourceBundle = getResourceBundle("res/PostFindTransaction");

			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			iconNameC16 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postfind_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/transactions/couchdb/images/postfind_color_32x32.png";

			properties = new PropertyDescriptor[11];

			properties[0] = new PropertyDescriptor("p_json_base", beanClass, "getP_json_base", "setP_json_base");
			properties[0].setDisplayName(getExternalizedString("property.p_json_base.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.p_json_base.short_description"));
			properties[0].setExpert(true);

			properties[1] = new PropertyDescriptor("p_selector", beanClass, "getP_selector", "setP_selector");
			properties[1].setDisplayName(getExternalizedString("property.p_selector.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.p_selector.short_description"));
			properties[1].setPropertyEditorClass(getEditorClass("JavascriptTextEditor"));
			properties[1].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[2] = new PropertyDescriptor("p_limit", beanClass, "getP_limit", "setP_limit");
			properties[2].setDisplayName(getExternalizedString("property.p_limit.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.p_limit.short_description"));

			properties[3] = new PropertyDescriptor("p_skip", beanClass, "getP_skip", "setP_skip");
			properties[3].setDisplayName(getExternalizedString("property.p_skip.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.p_skip.short_description"));

			properties[4] = new PropertyDescriptor("p_sort", beanClass, "getP_sort", "setP_sort");
			properties[4].setDisplayName(getExternalizedString("property.p_sort.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.p_sort.short_description"));
			properties[4].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[5] = new PropertyDescriptor("p_fields", beanClass, "getP_fields", "setP_fields");
			properties[5].setDisplayName(getExternalizedString("property.p_fields.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.p_fields.short_description"));
			properties[5].setValue(SCRIPTABLE, Boolean.TRUE);

			properties[6] = new PropertyDescriptor("p_use_index", beanClass, "getP_use_index", "setP_use_index");
			properties[6].setDisplayName(getExternalizedString("property.p_use_index.display_name"));
			properties[6].setShortDescription(getExternalizedString("property.p_use_index.short_description"));
			properties[6].setValue(SCRIPTABLE, Boolean.TRUE);
			properties[6].setExpert(true);

			properties[7] = new PropertyDescriptor("p_bookmark", beanClass, "getP_bookmark", "setP_bookmark");
			properties[7].setDisplayName(getExternalizedString("property.p_bookmark.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.p_bookmark.short_description"));

			properties[8] = new PropertyDescriptor("p_update", beanClass, "getP_update", "setP_update");
			properties[8].setDisplayName(getExternalizedString("property.p_update.display_name"));
			properties[8].setShortDescription(getExternalizedString("property.p_update.short_description"));
			properties[8].setExpert(true);

			properties[9] = new PropertyDescriptor("p_stable", beanClass, "getP_stable", "setP_stable");
			properties[9].setDisplayName(getExternalizedString("property.p_stable.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.p_stable.short_description"));
			properties[9].setExpert(true);

			properties[10] = new PropertyDescriptor("p_execution_stats", beanClass, "getP_execution_stats", "setP_execution_stats");
			properties[10].setDisplayName(getExternalizedString("property.p_execution_stats.display_name"));
			properties[10].setShortDescription(getExternalizedString("property.p_execution_stats.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
