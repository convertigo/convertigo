/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.beans.couchdb;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class JsonIndexBeanInfo extends MySimpleBeanInfo {

	public JsonIndexBeanInfo() {
		try {
			beanClass =  JsonIndex.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.Index.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/couchdb/images/jsonindex_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/couchdb/images/jsonindex_color_32x32.png";
			
			resourceBundle = getResourceBundle("res/JsonIndex");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");	
			
			properties = new PropertyDescriptor[2];
			
			properties[0] = new PropertyDescriptor("fields", beanClass, "getFields", "setFields");
			properties[0].setDisplayName(getExternalizedString("property.fields.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.fields.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("JsonIndexEditor"));
			
			properties[1] = new PropertyDescriptor("ascending", beanClass, "getAscending", "setAscending");
			properties[1].setDisplayName(getExternalizedString("property.ascending.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.ascending.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
