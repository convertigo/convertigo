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

package com.twinsoft.convertigo.beans.common;

import org.w3c.dom.*;
import com.twinsoft.convertigo.engine.util.*;

public class XMLRectangle extends java.awt.Rectangle implements XMLizable {

	private static final long serialVersionUID = -6954051606166034538L;

	public XMLRectangle() {
        super();
    }
    
    public XMLRectangle(int left, int top, int width, int height) {
        super(left, top, width, height);
    }
    
    public void readXml(Node node) throws Exception {
        NodeList nl = node.getChildNodes();
        int len = nl.getLength();
        
        Object object;
        Node objectNode;
        for (int i = 0 ; i < len ; i++) {
            objectNode = nl.item(i);
            if (objectNode.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = objectNode.getNodeName();
                object = XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(objectNode, Node.ELEMENT_NODE));
                int integer = ((Integer) object).intValue();
                if (nodeName.equals("x")) {
                    x = integer;
                }
                else if (nodeName.equals("y")) {
                    y = integer;
                }
                else if (nodeName.equals("width")) {
                    width = integer;
                }
                else if (nodeName.equals("height")) {
                    height = integer;
                }
            }
        }
    }    

    public Node writeXml(Document document) throws Exception {
        Element element = document.createElement(getClass().getName());
        Element propertyElement;
        
        propertyElement = document.createElement("x");
        propertyElement.appendChild(XMLUtils.writeObjectToXml(document, new Integer(x)));
        element.appendChild(propertyElement);

        propertyElement = document.createElement("y");
        propertyElement.appendChild(XMLUtils.writeObjectToXml(document, new Integer(y)));
        element.appendChild(propertyElement);

        propertyElement = document.createElement("width");
        propertyElement.appendChild(XMLUtils.writeObjectToXml(document, new Integer(width)));
        element.appendChild(propertyElement);

        propertyElement = document.createElement("height");
        propertyElement.appendChild(XMLUtils.writeObjectToXml(document, new Integer(height)));
        element.appendChild(propertyElement);

        return element;
    }
    
    public String toString() {
    	return "[x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }
}
