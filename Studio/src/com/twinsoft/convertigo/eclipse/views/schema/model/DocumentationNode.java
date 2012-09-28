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

package com.twinsoft.convertigo.eclipse.views.schema.model;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DocumentationNode extends XsdNode {

	public DocumentationNode(TreeParentNode parent, Element object) {
		super(parent, object);
	}

	@Override
	protected void handleChidren() {
		Element element = (Element) getObject();
		NodeList childrenList = element.getChildNodes();
		for (int i=0; i<childrenList.getLength(); i++) {
			Node node = childrenList.item(i);
			if (node.getNodeType() == Node.TEXT_NODE) {
				addChild(new TextNode(this, node.getTextContent()));
			}
		}
		super.handleChidren();
	}

	
}
