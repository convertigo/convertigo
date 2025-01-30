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

import com.twinsoft.convertigo.beans.core.Project.WsdlStyle;
import com.twinsoft.convertigo.beans.core.Project.XsdForm;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.XPathEngine;

public class ProjectBeanInfo extends MySimpleBeanInfo {
    
	public ProjectBeanInfo() {
		try {
			beanClass = Project.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.core.DatabaseObject.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/core/images/project_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/core/images/project_color_32x32.png";

			resourceBundle = getResourceBundle("res/Project");

			displayName = resourceBundle.getString("display_name");
			shortDescription = resourceBundle.getString("short_description");

			properties = new PropertyDescriptor[13];
			
			properties[0] = new PropertyDescriptor("browserDefinitions", beanClass, "getBrowserDefinitions", "setBrowserDefinitions");
			properties[0].setExpert(true);
			properties[0].setDisplayName(getExternalizedString("property.browserDefinitions.display_name"));
			properties[0].setShortDescription(getExternalizedString("property.browserDefinitions.short_description"));
			properties[0].setPropertyEditorClass(getEditorClass("BrowserDefinitionEditor"));

			properties[1] = new PropertyDescriptor("contextTimeout", beanClass, "getContextTimeout", "setContextTimeout");
			properties[1].setDisplayName(getExternalizedString("property.contextTimeout.display_name"));
			properties[1].setShortDescription(getExternalizedString("property.contextTimeout.short_description"));
			
			properties[2] = new PropertyDescriptor("wsdlStyle", beanClass, "getWsdlStyle", "setWsdlStyle");
			properties[2].setDisplayName(getExternalizedString("property.wsdlStyle.display_name"));
			properties[2].setShortDescription(getExternalizedString("property.wsdlStyle.short_description"));
			properties[2].setPropertyEditorClass(WsdlStyle.class);
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
			properties[5].setPropertyEditorClass(XsdForm.class);
			properties[5].setExpert(true);
			properties[5].setHidden(true);
			
            properties[6] = new PropertyDescriptor("version", beanClass, "getVersion", "setVersion");
            properties[6].setDisplayName(getExternalizedString("property.version.display_name"));
            properties[6].setShortDescription(getExternalizedString("property.version.short_description"));

            properties[7] = new PropertyDescriptor("bStrictMode", beanClass, "isStrictMode", "setStrictMode");
			properties[7].setDisplayName(getExternalizedString("property.bStrictMode.display_name"));
			properties[7].setShortDescription(getExternalizedString("property.bStrictMode.short_description"));
			properties[7].setExpert(true);

            properties[8] = new PropertyDescriptor("jsonOutput", beanClass, "getJsonOutput", "setJsonOutput");
            properties[8].setDisplayName(getExternalizedString("property.jsonOutput.display_name"));
            properties[8].setShortDescription(getExternalizedString("property.jsonOutput.short_description"));
            properties[8].setPropertyEditorClass(JsonOutput.class);
            properties[8].setExpert(true);
            
            properties[9] = new PropertyDescriptor("corsOrigin", beanClass, "getCorsOrigin", "setCorsOrigin");
			properties[9].setDisplayName(getExternalizedString("property.corsOrigin.display_name"));
			properties[9].setShortDescription(getExternalizedString("property.corsOrigin.short_description"));
			properties[9].setExpert(true);
			
            properties[10] = new PropertyDescriptor("jsonRoot", beanClass, "getJsonRoot", "setJsonRoot");
            properties[10].setDisplayName(getExternalizedString("property.jsonRoot.display_name"));
            properties[10].setShortDescription(getExternalizedString("property.jsonRoot.short_description"));
            properties[10].setPropertyEditorClass(JsonRoot.class);
            properties[10].setExpert(true);
			
            properties[11] = new PropertyDescriptor("xpathEngine", beanClass, "getXpathEngine", "setXpathEngine");
            properties[11].setDisplayName(getExternalizedString("property.xpathEngine.display_name"));
            properties[11].setShortDescription(getExternalizedString("property.xpathEngine.short_description"));
            properties[11].setPropertyEditorClass(XPathEngine.class);
            properties[11].setExpert(true);
            
			properties[12] = new PropertyDescriptor("httpSessionTimeout", beanClass, "getHttpSessionTimeout", "setHttpSessionTimeout");
			properties[12].setDisplayName(getExternalizedString("property.httpSessionTimeout.display_name"));
			properties[12].setShortDescription(getExternalizedString("property.httpSessionTimeout.short_description"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}

