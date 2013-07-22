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

package com.twinsoft.convertigo.engine.migration;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.util.XMLUtils;

public class MigrationUtils {
    
	public static Element findChildElementByTagName(NodeList children, String childTagName) {
    	Node child;
		for (int i = 0 ; i < children.getLength() ; i++) {
			child = children.item(i);
			if ((child.getNodeType() == Node.ELEMENT_NODE) && (childTagName.equals(child.getNodeName()))) {
                return ((Element) child);
			}    	
		}
		return null;
    }
	
    public static Element findChildElementByAttributeValue(NodeList children, String childTagName, String attributeValue) {
    	Node child;
    	String tmp;
		for (int i = 0 ; i < children.getLength() ; i++) {
			child = children.item(i);
			if ((child.getNodeType() == Node.ELEMENT_NODE) && (childTagName.equals(child.getNodeName()))) {
	            tmp = ((Element) child).getAttribute("name");
	            if (attributeValue.equals(tmp)) {
	                return ((Element) child);
	            }
			}    	
		}
		return null;
    }

    public static Element findChildElementByProperty(NodeList children, String childTagName, String propertyName, String propertyValue) {
    	Element childElement, childElementNode;
    	Object childElementValue;
    	Node node;
		for (int i = 0 ; i < children.getLength() ; i++) {
			node = children.item(i);
			if ((node.getNodeType() == Node.ELEMENT_NODE) && (childTagName.equals(node.getNodeName()))) {
				try {
					childElement = MigrationUtils.findChildElementByAttributeValue(node.getChildNodes(), "property", propertyName);
					childElementNode = (Element) XMLUtils.findChildNode(childElement, Node.ELEMENT_NODE);
					childElementValue = XMLUtils.readObjectFromXml(childElementNode);
					if (propertyValue.equals(childElementValue)) {
						return (Element)node;
					}
				}
				catch (Exception e) {
					;
				}
			}    	
		}
    	return null;
    }
}
