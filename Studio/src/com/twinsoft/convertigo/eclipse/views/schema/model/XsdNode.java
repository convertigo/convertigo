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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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

	public XsdNode handleNode() {
		XsdNode nodeCreated = createNode(getObject());
		nodeCreated.handleChidren();
		return nodeCreated;
	}
	
	public SchemaNode getSchemaNode() {
		TreeParentNode parent = getParent();
		while (parent!= null) {
			if (parent instanceof SchemaNode)
				return (SchemaNode)parent;
			parent = parent.getParent();
		}
		return null;
	}
	
	public String getTargetNamespace() {
		Document doc = ((Element) getObject()).getOwnerDocument();
		return doc.getDocumentElement().getAttribute("targetNamespace");
	}
	
	public String getNamespaceURI(String prefix) {
		if (prefix.equals(""))
			return getTargetNamespace();
		
		Document doc = ((Element) getObject()).getOwnerDocument();
		NamedNodeMap map = doc.getDocumentElement().getAttributes();
		for (int i=0; i < map.getLength(); i++) {
			Node node = map.item(i);
			String nodeName = node.getNodeName();
			if (nodeName.equals("xmlns:"+prefix))
				return node.getNodeValue();
		}
		return null;
	}
	
	public String findNamespaceURI(String qname) {
		if ((qname != null) && (!qname.equals(""))) {
			String[] qn = qname.split(":");
			String ns = qn.length > 1 ? getNamespaceURI(qn[0]):getNamespaceURI("");
			return ns;
		}
		return null;
	}
	
	public String findLocalName(String qname) {
		if ((qname != null) && (!qname.equals(""))) {
			String[] qn = qname.split(":");
			String ns = qn.length > 1 ? qn[1]:qn[0];
			return ns;
		}
		return null;
	}

	protected XsdNode createNode(Element element) {
		XsdNode xsdNode = null;
		String tagName = element.getTagName();
		if (tagName.endsWith("all")) {
			xsdNode = new AllNode(this, element);
		}
		else if (tagName.endsWith("annotation")) {
			xsdNode = new AnnotationNode(this, element);
		}
		else if (tagName.endsWith("attribute")) {
			xsdNode = new AttributeNode(this, element);
		}
		else if (tagName.endsWith("attributeGroup")) {
			xsdNode = new AttributeGroupNode(this, element);
		}
		else if (tagName.endsWith("choice")) {
			xsdNode = new ChoiceNode(this, element);
		}
		else if (tagName.endsWith("Content")) {
			xsdNode = new ContentNode(this, element);
		}
		else if (tagName.endsWith("documentation")) {
			xsdNode = new DocumentationNode(this, element);
		}
		else if (tagName.endsWith("element")) {
			xsdNode = new ElementNode(this, element);
		}
		else if (tagName.endsWith("extension")) {
			xsdNode = new ExtensionNode(this, element);
		}
		else if (tagName.endsWith("group")) {
			xsdNode = new GroupNode(this, element);
		}
		else if (tagName.endsWith("include")) {
			xsdNode = new IncludeNode(this, element);
		}
		else if (tagName.endsWith("import")) {
			xsdNode = new ImportNode(this, element);
		}
		else if (tagName.endsWith("key")) {
			xsdNode = new KeyNode(this, element);
		}
		else if (tagName.endsWith("list")) {
			xsdNode = new ListNode(this, element);
		}
		else if (tagName.endsWith("redefine")) {
			xsdNode = new RedefineNode(this, element);
		}
		else if (tagName.endsWith("restriction")) {
			xsdNode = new RestrictionNode(this, element);
		}
		else if (tagName.endsWith("schema")) {
			xsdNode = new SchemaNode(this, element);
		}
		else if (tagName.endsWith("sequence")) {
			xsdNode = new SequenceNode(this, element);
		}
		else if (tagName.endsWith("Type")) {
			xsdNode = new TypeNode(this, element);
		}
		else if (tagName.endsWith("unique")) {
			xsdNode = new UniqueNode(this, element);
		}
		else if (tagName.endsWith("union")) {
			xsdNode = new UnionNode(this, element);
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
