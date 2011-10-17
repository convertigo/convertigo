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

package com.twinsoft.convertigo.engine.admin.services.configuration;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ComboEnum;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyCategory;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyType;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class List extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		
		Element rootElement = document.getDocumentElement();

		for (PropertyCategory propertyCategory : PropertyCategory.values()) {
			if (propertyCategory.isVisible()) {
	            Element elementCategory = document.createElement("category");
	            elementCategory.setAttribute("name", propertyCategory.toString());
	            elementCategory.setAttribute("displayName", propertyCategory.getDisplayName());
	            rootElement.appendChild(elementCategory);
			}
		}
        
        NodeList categories = document.getElementsByTagName("category");

        for (PropertyName property : PropertyName.values()) {
        	if (property.isVisible()) {
	        	Element categoryElement = (Element) XMLUtils.findNodeByAttributeValue(categories, "name", property.getCategory().name());
	        	if (categoryElement != null) {
	        		String value = EnginePropertiesManager.getProperty(property);
	        		String originalValue = EnginePropertiesManager.getOriginalProperty(property);
	        		switch (property.getType()) {
	        		case PasswordHash:
	        		case PasswordPlain:
	        			originalValue = value = "????????????????";
	        			break;
	        		}
	        		Element propertyElement = document.createElement("property");
	        		propertyElement.setAttribute("name", property.name());
	        		propertyElement.setAttribute("type", property.getType().name());
	        		propertyElement.setAttribute("description", property.getDescription());
	        		propertyElement.setAttribute("value", value);
	        		propertyElement.setAttribute("originalValue", originalValue);
	        		propertyElement.setAttribute("isAdvanced", Boolean.toString(property.isAdvance()));
	
	
	        		categoryElement.appendChild(propertyElement);
	
	        		if (property.getType() == PropertyType.Combo) {
	        			for (ComboEnum ce : property.getCombo()) {
	        				Element comboValueElement = document.createElement("item");
	        				comboValueElement.setAttribute("value", ce.getValue());
	        				Text comboValueText = document.createTextNode(ce.getDisplay());
	        				comboValueElement.appendChild(comboValueText);
	        				propertyElement.appendChild(comboValueElement);
	        			}
	        		}
	        	}
	        }
        }
	}
}
