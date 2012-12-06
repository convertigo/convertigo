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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.core;

import java.beans.PropertyDescriptor;

public class ProjectBeanInfo extends MySimpleBeanInfo {
    
	public ProjectBeanInfo() {
		try {
			beanClass = Project.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/project_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/project_color_32x32.png";

			resourceBundle = java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/beans/core/res/Project");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[8];
			
			properties[0] = new PropertyDescriptor("browserDefinitions", beanClass, "getBrowserDefinitions", "setBrowserDefinitions");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.browserDefinitions.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.browserDefinitions.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("BrowserDefinitionEditor"));

			properties[1] = new PropertyDescriptor("httpSessionTimeout", beanClass, "getHttpSessionTimeout", "setHttpSessionTimeout");
			properties[1].setDisplayName(getExternalizedString("property.httpSessionTimeout.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.httpSessionTimeout.short_description"));
			
			properties[2] = new PropertyDescriptor("wsdlStyle", beanClass, "getWsdlStyle", "setWsdlStyle");
			properties[2].setDisplayName(getExternalizedString("property.wsdlStyle.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.wsdlStyle.short_description"));
			properties[2].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			properties[2].setExpert(true);
			properties[2].setValue(BLACK_LIST_NAME,true);
			
			properties[3] = new PropertyDescriptor("schemaInline", beanClass, "isSchemaInline", "setSchemaInline");
			properties[3].setDisplayName(getExternalizedString("property.schemaInline.display_name"));
			properties[3].setShortDescription(getExternalizedString("property.schemaInline.short_description"));
			properties[3].setExpert(true);
			properties[3].setValue(BLACK_LIST_NAME,true);
			
			properties[4] = new PropertyDescriptor("namespaceUri", beanClass, "getNamespaceUri", "setNamespaceUri");
			properties[4].setDisplayName(getExternalizedString("property.namespaceUri.display_name"));
			properties[4].setShortDescription(getExternalizedString("property.namespaceUri.short_description"));
			properties[4].setPropertyEditorClass(getEditorClass("PropertyWithValidatorEditor")); 
			properties[4].setExpert(true);
			
			properties[5] = new PropertyDescriptor("schemaElementForm", beanClass, "getSchemaElementForm", "setSchemaElementForm");
			properties[5].setDisplayName(getExternalizedString("property.schemaElementForm.display_name"));
			properties[5].setShortDescription(getExternalizedString("property.schemaElementForm.short_description"));
			properties[5].setPropertyEditorClass(getEditorClass("PropertyWithTagsEditorAdvance"));
			properties[5].setExpert(true);
			
            properties[6] = new PropertyDescriptor("version", beanClass, "getVersion", "setVersion");
            properties[6].setDisplayName(getExternalizedString("property.version.display_name"));
            properties[6].setShortDescription(getExternalizedString("property.version.short_description"));

            properties[7] = new PropertyDescriptor("exported", beanClass, "getExportTime", "setExportTime");
            properties[7].setDisplayName(getExternalizedString("property.exported.display_name"));
            properties[7].setShortDescription(getExternalizedString("property.exported.short_description"));
            properties[7].setPropertyEditorClass(getEditorClass("PropertyWithDynamicInfoEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

