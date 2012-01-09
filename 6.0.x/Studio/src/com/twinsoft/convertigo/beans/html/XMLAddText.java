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

package com.twinsoft.convertigo.beans.html;

import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLNodeList;
import com.twinsoft.convertigo.engine.Engine;

/**
 * 
 * This class simply removes nodes from ouputDom.
 *
 */
public class XMLAddText extends XMLNodeList {

	private static final long serialVersionUID = -4219403874543488602L;
	
	protected String text = null;
	
	public XMLAddText() {
		super();
		text = "";
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element doc;
		NodeList ndList = null;
		Node node = null;
				
		doc = outputDom.getDocumentElement();
		if (xpathApi != null) {
			try {
				ndList = xpathApi.selectNodeList(doc, getXpath());
			}
			catch (TransformerException e) {
				Engine.logBeans.debug("XMLAddText : Exception when applying Xpath '" + getXpath() + "' on output document.");
			}
						
			int length = ndList.getLength();
			if (length > 0) {
				Element elem = null;
				// HTML text generation : P element with text inside
				try {
					elem = generateContent(outputDom);
					
					// add of element under the first node addressed by the Xpath
					node = ndList.item(0);
					try {
						node.appendChild(elem);
						Engine.logBeans.debug("XMLAddText : element added under node '" + node.getNodeName() + "' in output document.");
					} catch (DOMException e) {
						Engine.logBeans.debug("XMLAddText : Exception when adding text under node '" + node.getNodeName() + "' in output document.");
					}
				} catch (DOMException e) {
					Engine.logBeans.debug("XMLAddText : Exception when creating P element with text '" + getText() + "'.");
				}
			}
		}
	}

	protected Element generateContent(Document dom) {
		// P element generation
		Element pElem = null;
		pElem = dom.createElement("P");
		// add of text content
		pElem.setTextContent(getText());
		return pElem;
	}
	
}
