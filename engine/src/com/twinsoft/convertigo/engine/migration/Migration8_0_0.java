/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.migration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent.ViewEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class Migration8_0_0 {
	
	public static Element migrate(Document document, Element projectNode) throws EngineException {
		try {
			NodeList properties = document.getDocumentElement().getElementsByTagName("property");
			
			Element property;
			for (int i = 0; i < properties.getLength(); i++) {
				property = (Element) properties.item(i);
				
				// migration of pageWillUnload to pageDidLeave for NGX sharedComponent only !
				if ("viewEvent".equals(property.getAttribute("name"))) {
					Element propElement = property;
					if (propElement != null) {
						Element topParent = (Element) propElement.getParentNode().getParentNode();
						if (topParent != null) {
							String topClassname = topParent.getAttribute("classname");
							if ("com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent".equals(topClassname)) {
								Element valueElement = (Element) XMLUtils.findChildNode(propElement, Node.ELEMENT_NODE);
								if (valueElement != null) {
									Object content = XMLUtils.readObjectFromXml(valueElement);
									if (content instanceof String) {
										if (ViewEvent.onWillUnload.name().equals(content)) {
											Element newValueElement = (Element)XMLUtils.writeObjectToXml(document, ViewEvent.onDidLeave.name());
											propElement.replaceChild(newValueElement, valueElement);
											document.setUserData("needExport", "true", null);
											
											String sharedCompName = "";
											try { sharedCompName += XMLUtils.findPropertyValue(topParent, "name");} catch (Exception e) {}
											Engine.logBeans.warn("(UIPageEvent) For sharedComponent '"+sharedCompName+"': pageEvent 'willUnload' has been updated to pageEvent 'didLeave'");
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
	    	throw new EngineException("[Migration 8.0.0] Unable to migrate project",e);
	    }
			
		return projectNode;
	}
}
