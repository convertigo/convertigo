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
public class XMLDeleteNodes extends XMLNodeList {

	private static final long serialVersionUID = -8237610895917114200L;

	public XMLDeleteNodes() {
		super();
	}

	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element doc;
		NodeList ndList = null;
		Node node, parent = null;
				
		doc = outputDom.getDocumentElement();
		if (xpathApi != null) {
			try {
				ndList = xpathApi.selectNodeList(doc, getXpath());
			}
			catch (TransformerException e) {
				Engine.logBeans.debug("XMLDeleteNodes : Exception when applying Xpath '" + getXpath() + "' on output document.");
			}
			int length = ndList.getLength();
			for (int i = 0 ; i < length ; i++) {
				if (!isRequestedObjectRunning()) break;
				
				node = ndList.item(i);
				parent = node.getParentNode();
				if (parent != null)
					try {
						parent.removeChild(node);
						Engine.logBeans.debug("XMLDeleteNodes : node '" + node.getNodeName() + "' deleted from output document.");
					} catch (DOMException e) {
						Engine.logBeans.debug("XMLDeleteNodes : Exception when deleting node '" + node.getNodeName() + "' from output document.");
					}
			}
		}
	}

}
