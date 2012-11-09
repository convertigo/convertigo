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

package com.twinsoft.convertigo.beans.references;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class WsdlSchemaReferenceBeanInfo extends MySimpleBeanInfo {
	public WsdlSchemaReferenceBeanInfo() {
		try {
			beanClass = WsdlSchemaReference.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.references.RemoteFileReference.class;

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/references/res/WsdlSchemaReference");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
	        PropertyDescriptor filepathProperty = getPropertyDescriptor("filepath");
	        filepathProperty.setDisplayName(getExternalizedString("property.filepath.display_name"));
	        filepathProperty.setShortDescription(getExternalizedString("property.filepath.short_description"));

	        PropertyDescriptor urlpathProperty = getPropertyDescriptor("urlpath");
	        urlpathProperty.setDisplayName(getExternalizedString("property.urlpath.display_name"));
	        urlpathProperty.setShortDescription(getExternalizedString("property.urlpath.short_description"));
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
