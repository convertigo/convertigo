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

import java.util.Collections;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.CachedXPathAPI;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.enums.XPathEngine;

public class TwsCachedXPathAPI {
	protected Node lastContextNode = null;
	protected CachedXPathAPI xpathApi = null;
	protected XPathEngine xpathEngine = XPathEngine.JXPath;
	
	public TwsCachedXPathAPI() {
	}
	
	public TwsCachedXPathAPI(Project project) {
		if (project != null) {
			xpathEngine = project.getXpathEngine();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Node> selectList(Node contextNode, String xpath) {
		@SuppressWarnings("rawtypes")
		List nodes;
		try {
			nodes = JXPathContext.newContext(contextNode).selectNodes(xpath);
			int i = 0;
			for (Object node: nodes) {
				if (node instanceof String) {
					node = contextNode.getOwnerDocument().createTextNode((String) node);
					nodes.set(i, node);
				}
				i++;
			}
		} catch (Exception e) {
			nodes = Collections.emptyList();
		}
		return (List<Node>) nodes;
	}
	
	public Node selectNode(Node contextNode, String xpath) {
		Node node;
		try {
			node = (Node) JXPathContext.newContext(contextNode).selectSingleNode(xpath);
		} catch (Exception e) {
			node = null;
		}
		return node;
	}
	
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
		if (xpathApi == null) {
			xpathApi = new CachedXPathAPI();
		}
		return xpathApi.getXPathContext();
	}

	public NodeIterator selectNodeIterator(Node contextNode, String xpath, Node namespaceNode) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.selectNodeIterator(contextNode, xpath, namespaceNode);
	}

	public NodeIterator selectNodeIterator(Node contextNode, String xpath) throws TransformerException {
		switch (xpathEngine) {
		case Xalan:
			checkContextNode(contextNode);
			return xpathApi.selectNodeIterator(contextNode, xpath);
		}
		return new JXPathNodeIterator(selectList(contextNode, xpath));
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
		switch (xpathEngine) {
		case Xalan:		
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
						Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
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
		return new JXPathNodeList(selectList(contextNode, xpath));

	}

	public Node selectSingleNode(Node contextNode, String xpath, Node namespaceNode) throws TransformerException {
		checkContextNode(contextNode);
		return xpathApi.selectSingleNode(contextNode, xpath, namespaceNode);
	}

	public Node selectSingleNode(Node contextNode, String xpath) throws TransformerException {
		switch (xpathEngine) {
		case Xalan:
			checkContextNode(contextNode);
			return xpathApi.selectSingleNode(contextNode, xpath);
		}
		return selectNode(contextNode, xpath);
	}

	protected void checkContextNode(Node contextNode) {
		if (shouldReset(lastContextNode, contextNode)) {
			// reset the cache
			xpathApi = new CachedXPathAPI();
			lastContextNode = contextNode;
		}
	}
	
	public void release() {
		if (xpathApi != null) {
			XPathContext  xpathContext = xpathApi.getXPathContext();
			if (xpathContext != null) {
				xpathContext.reset();
			}
		}
		resetCache();
	}
	
	public void resetCache() {
		lastContextNode = null;
	}
	
	private boolean shouldReset(Node last, Node current) {
		boolean shouldReset = last == null || xpathApi == null;
		if (!shouldReset) {
		/* Does not work in all cases
			int lastNodeType = last.getNodeType();
			int currentNodeType = current.getNodeType();
			
			if (lastNodeType == Node.DOCUMENT_NODE && currentNodeType == Node.DOCUMENT_NODE) {
				shouldReset = !last.equals(current);
			}
			else if (lastNodeType == Node.DOCUMENT_NODE && currentNodeType != Node.DOCUMENT_NODE) {
				shouldReset = !last.equals(current.getOwnerDocument());
			}
			else if (lastNodeType != Node.DOCUMENT_NODE && currentNodeType == Node.DOCUMENT_NODE) {
				shouldReset = !last.getOwnerDocument().equals(current);
			}
			else {
				shouldReset = !last.getOwnerDocument().equals(current.getOwnerDocument());
			}
		*/
			shouldReset = !last.equals(current);
		}
		return shouldReset;
	}
	
	protected class JXPathNodeList implements NodeList {
		List<Node> nodes;
		
		public JXPathNodeList(List<Node> nodes) {
			this.nodes = nodes;
		}

		@Override
		public int getLength() {
			return nodes.size();
		}

		@Override
		public Node item(int index) {
			return nodes.get(index);
		}
		
	}
	
	protected class JXPathNodeIterator implements NodeIterator {
		List<Node> nodes;
		int i = 0;
		
		public JXPathNodeIterator(List<Node> nodes) {
			this.nodes = nodes;
		}
		
		@Override
		public void detach() {
			nodes = null;
		}

		@Override
		public boolean getExpandEntityReferences() {
			return false;
		}

		@Override
		public NodeFilter getFilter() {
			return new NodeFilter() {
				
				@Override
				public short acceptNode(Node arg0) {
					return 0;
				}
			};
		}

		@Override
		public Node getRoot() {
			return nodes.isEmpty() ? null : nodes.get(0);
		}

		@Override
		public int getWhatToShow() {
			return 0;
		}

		@Override
		public Node nextNode() throws DOMException {
			return i < nodes.size() ? nodes.get(i++) : null;
		}

		@Override
		public Node previousNode() throws DOMException {
			return i > 0 ? nodes.get(--i) : null;
		}
		
	}
}
