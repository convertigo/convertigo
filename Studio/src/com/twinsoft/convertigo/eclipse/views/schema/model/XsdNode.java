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

import org.apache.ws.commons.schema.utils.DOMUtil;
import org.w3c.dom.Element;

public class XsdNode extends TreeParentNode {

	public XsdNode(TreeParentNode parent, Element element) {
		super(parent, element.getTagName(), element);
	}
	
	public boolean useRef() {
		return getObject().hasAttribute("ref");
	}
	
	public boolean useType() {
		return getObject().hasAttribute("type");
	}

	public boolean useBase() {
		return getObject().hasAttribute("base");
	}

	public boolean isComplexType() {
		return getObject().getTagName().endsWith("complexType");
	}
	
	public boolean isSimpleType() {
		return getObject().getTagName().endsWith("simpleType");
	}

	public boolean isComplexContent() {
		return getObject().getTagName().endsWith("complexContent");
	}
	
	public boolean isSimpleContent() {
		return getObject().getTagName().endsWith("simpleContent");
	}

	public boolean hasOccurs() {
		return getObject().hasAttribute("minOccurs") ||
				getObject().hasAttribute("maxOccurs");
	}
	
	@Override
	public String getName() {
		String name = "";
		if (useRef()) {
			name = getObject().getAttribute("ref");
		}
		else {
			name = getObject().getAttribute("name");
			if (name.equals("")) {
				name = getObject().getLocalName();
			}
		}
		return name;
	}

	protected XsdNode createNode(Element element) {
		XsdNode xsdNode = null;
		String tagName = element.getTagName();
		if (tagName.endsWith("schema")) {
			xsdNode = new SchemaNode(this, element);
		}
		else if (tagName.endsWith("import")) {
			xsdNode = new ImportNode(this, element);
		}
		else if (tagName.endsWith("include")) {
			xsdNode = new IncludeNode(this, element);
		}
		else if (tagName.endsWith("annotation")) {
			xsdNode = new AnnotationNode(this, element);
		}
		else if (tagName.endsWith("attribute")) {
			xsdNode = new AttributeNode(this, element);
		}
		else if (tagName.endsWith("element")) {
			xsdNode = new ElementNode(this, element);
		}
		else if (tagName.endsWith("Type")) {
			xsdNode = new TypeNode(this, element);
		}
		else if (tagName.endsWith("group")) {
			xsdNode = new GroupNode(this, element);
		}
		else if (tagName.endsWith("sequence")) {
			xsdNode = new SequenceNode(this, element);
		}
		else {
			xsdNode = new XsdNode(this, element);
		}
		return xsdNode;
	}
	
	protected void handleChidren() {
		Element element = (Element) getObject();
		Element first = DOMUtil.getFirstChildElement(element);
		while (first != null) {
			XsdNode xsdNode = createNode(first);
			xsdNode.handleChidren();
			addChild(xsdNode);
			first = DOMUtil.getNextSiblingElement(first);
		}
	}
}
