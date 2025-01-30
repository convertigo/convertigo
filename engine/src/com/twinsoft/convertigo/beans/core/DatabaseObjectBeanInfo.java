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

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class DatabaseObjectBeanInfo extends MySimpleBeanInfo {

	public DatabaseObjectBeanInfo() {
		try {
			beanClass = DatabaseObject.class;
			additionalBeanClass = null;

			resourceBundle = getResourceBundle("res/DatabaseObject");

			properties = new PropertyDescriptor[2];

			properties[0] = new PropertyDescriptor("comment", beanClass, "getComment", "setComment");
			properties[0].setDisplayName(getExternalizedString("property.comment.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.comment.short_description"));
			properties[0].setValue(GENERIC_EDITOR_EXTENSION, "md");
			properties[0].setValue(MULTILINE, Boolean.TRUE);

			properties[1] = new PropertyDescriptor("name", beanClass, "getName", "setName");
			properties[1].setDisplayName(getExternalizedString("property.name.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.name.short_description"));
			properties[1].setHidden(true);
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
