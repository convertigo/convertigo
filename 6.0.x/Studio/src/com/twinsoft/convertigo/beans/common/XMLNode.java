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

import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.engine.Engine;

/**
 * 
 * This class simply extract a node.
 *
 */
public class XMLNode extends HtmlExtractionRule {

	private static final long serialVersionUID = -636829907688699693L;
	
	public XMLNode() {
		super();
	}

	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element elt, doc;
		Node node = null;
		int length = nodeList.getLength();
		
		doc = outputDom.getDocumentElement();
		if (length >= 1) {
			node = nodeList.item(0);
			elt = (Element)outputDom.importNode(node,true);
			doc.appendChild(elt);
			Engine.logBeans.trace("node '" + node.getNodeName() + "' added to result document.");
		}
	}
	
	public String getSchema(String tns) {
		return "<xsd:any minOccurs=\"0\" maxOccurs=\"1\" namespace=\"##any\"/>\n";
	}

	public String getSchemaElementName() {
		return "";
	}

	public String getSchemaElementType() {
		return "";
	}
	
	public String getSchemaElementNSType(String tns) {
		return "";
	}
}
