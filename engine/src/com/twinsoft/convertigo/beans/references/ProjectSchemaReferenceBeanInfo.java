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

package com.twinsoft.convertigo.beans.references;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class ProjectSchemaReferenceBeanInfo extends MySimpleBeanInfo {
	public ProjectSchemaReferenceBeanInfo() {
		try {
			beanClass = ProjectSchemaReference.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.references.ImportXsdSchemaReference.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/references/images/ProjectSchemaReference_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/references/images/ProjectSchemaReference_32x32.png";

			resourceBundle = getResourceBundle("res/ProjectSchemaReference");
			
			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");
			
			properties = new PropertyDescriptor[1];

            properties[0] = new PropertyDescriptor("projectName", beanClass, "getProjectName", "setProjectName");
            properties[0].setDisplayName(getExternalizedString("property.projectName.display_name"));
            properties[0].setShortDescription(getExternalizedString("property.projectName.short_description"));
            properties[0].setPropertyEditorClass(getEditorClass("ProjectReferenceEditor"));
            
	        PropertyDescriptor filepathProperty = getPropertyDescriptor("filepath");
	        filepathProperty.setHidden(true);

	        PropertyDescriptor urlpathProperty = getPropertyDescriptor("urlpath");
	        urlpathProperty.setHidden(true);
			
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}
}
