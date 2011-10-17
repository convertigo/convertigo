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

package com.twinsoft.convertigo.engine.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.CachedXPathAPI;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

public class TwsCachedXPathAPI{
	protected Node lastContextNode = null;
	protected CachedXPathAPI xpathApi = new CachedXPathAPI();

	public XObject eval(Node contextNode, String xpath, Node namespaceNode) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.eval(contextNode, xpath, namespaceNode);
	}

	public XObject eval(Node contextNode, String xpath, PrefixResolver namespaceNode) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.eval(contextNode, xpath, namespaceNode);
	}

	public XObject eval(Node contextNode, String xpath) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.eval(contextNode, xpath);
	}

	public XPathContext getXPathContext() {
		return xpathApi.getXPathContext();
	}

	public NodeIterator selectNodeIterator(Node contextNode, String xpath, Node namespaceNode) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.selectNodeIterator(contextNode, xpath, namespaceNode);
	}

	public NodeIterator selectNodeIterator(Node contextNode, String xpath) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.selectNodeIterator(contextNode, xpath);
	}

	public NodeList selectNodeList(Node contextNode, String xpath, Node namespaceNode) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.selectNodeList(contextNode, xpath, namespaceNode);
	}

	/**
	 * Returns a node list for the specified Xpath and context node. When the result is just a string, a NodeList is 
	 * constructed containing one node text containing this string.
	 * 
	 * @param contextNode
	 * @param xpath
	 * @return null is an error occurs
	 * @throws TransformerException
	 */
	public NodeList selectNodeList(Node contextNode, String xpath) throws TransformerException {
		checkContextNode(contextNode);
		XObject Xobj;
		
		Xobj = xpathApi.eval(contextNode, xpath);
		switch (Xobj.getType()) {
			case XObject.CLASS_NODESET:
				return (Xobj.nodelist());
			
			case XObject.CLASS_BOOLEAN:
			case XObject.CLASS_NUMBER:
			case XObject.CLASS_STRING:
				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder documentBuilder = factory.newDocumentBuilder();
					Document doc = documentBuilder.newDocument();
					Element root =  (Element) doc.createElement("root"); 
					doc.appendChild(root);
					root.appendChild(doc.createTextNode(Xobj.str()));
					return (root.getChildNodes());
				} catch (Exception e) {
					return null;
				}
		}
		return null;
	}

	public Node selectSingleNode(Node contextNode, String xpath, Node namespaceNode) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.selectSingleNode(contextNode, xpath, namespaceNode);
	}

	public Node selectSingleNode(Node contextNode, String xpath) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.selectSingleNode(contextNode, xpath);
	}

	protected void checkContextNode(Node contextNode){
		if(contextNode != lastContextNode){
			// reset the cache
			xpathApi = new CachedXPathAPI();
			lastContextNode = contextNode;
		}
	}
	
	public void resetCache(){
		lastContextNode = null;
	}
}
