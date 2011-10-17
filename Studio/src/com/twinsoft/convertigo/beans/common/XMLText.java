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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Engine;

/**
 * 
 * This class simply extract text nodes from single node.
 *
 */
public class XMLText extends AbstractXMLReferer {

	private static final long serialVersionUID = -636829907688699693L;
	
	private String tagName = null;
	
	private boolean recurse = false;
	
	public XMLText() {
		super();
		tagName = "XMLText";
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	/**
	 * @return the recurse
	 */
	public boolean isRecurse() {
		return recurse;
	}

	/**
	 * @param recurse the recurse to set
	 */
	public void setRecurse(boolean recurse) {
		this.recurse = recurse;
	}

	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element elt, doc;
		String eltTagName;
		Text text;
		Node node = null;
		
		int length = nodeList.getLength();
		
		doc = outputDom.getDocumentElement();
		if (length >= 1) {
			node = nodeList.item(0);
			eltTagName = (tagName.equals("") ? name:tagName);
			elt = outputDom.createElement(eltTagName);
			text = outputDom.createTextNode(getStringValue(node, recurse));
			elt.appendChild(text);
			if (isDisplayReferer())
				addReferer(elt);
			doc.appendChild(elt);
			Engine.logBeans.trace("XMLText '" + node.getNodeName() + "' added to result document.");
		}
	}

	public String getSchema(String tns) {
		String textName, textTypeName, textType;
		String textSchema, textTypeSchema;
		
		textName = getSchemaElementName();
		textTypeName = getSchemaElementType();
		textType = tns+":"+ textTypeName;
		
		textTypeSchema = "<xsd:complexType name=\""+ textTypeName +"\">\n";
		textTypeSchema += "<xsd:simpleContent>\n";
		textTypeSchema += "<xsd:extension base=\"xsd:string\">\n";
		textTypeSchema += "<xsd:attribute name=\"referer\" type=\"xsd:string\" use=\"optional\" />\n";
		textTypeSchema += "</xsd:extension>\n";
		textTypeSchema += "</xsd:simpleContent>\n";
		textTypeSchema += "</xsd:complexType>\n";
		addWsType(textTypeName, textTypeSchema);
		
		textSchema = "<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\""+ textName + "\" type=\"" + textType + "\" />\n";
		return textSchema;
	}
	
	public String getSchemaElementName() {
		return (tagName.equals("") ? name:tagName);
	}

	public String getSchemaElementType() {
		return getSchemaElementName() + "TextType";
	}
	
	public String getSchemaElementNSType(String tns) {
		return tns+":"+ getSchemaElementType();
	}
}
