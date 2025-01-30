/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.util.Collections;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.dom.events.MutationEventImpl;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.CachedXPathAPI;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.enums.XPathEngine;

public class TwsCachedXPathAPI implements EventListener {
	private static TwsCachedXPathAPI instance;
	
	protected DocumentImpl lastDocument = null;
	protected CachedXPathAPI xpathApi = null;
	private XPathEngine xpathEngine = XPathEngine.JXPath;
	
	public static TwsCachedXPathAPI getInstance() {
		synchronized (TwsCachedXPathAPI.class) {
			if (instance == null) {
				instance = new TwsCachedXPathAPI();
			}
			return instance;
		}
	}
	
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
				if (node instanceof Number) {
					Number num = (Number) node;
					if (num.intValue() == num.doubleValue()) {
						node = Integer.toString(num.intValue());
					} else {
						node = Double.toString(num.doubleValue());
					}
				} else if (node instanceof Boolean) {
					node = node.toString();
				}
				
				if (node instanceof String) {
					Document doc =  contextNode instanceof Document ? (Document)contextNode : contextNode.getOwnerDocument();
					node = doc.createTextNode((String) node);
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
		default:
			return new JXPathNodeIterator(selectList(contextNode, xpath));
		}
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
		default:
			return new JXPathNodeList(selectList(contextNode, xpath));
		}

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
		default:
			return selectNode(contextNode, xpath);
		}
	}

	private void checkContextNode(Node contextNode) {
		if (shouldReset(contextNode)) {
			// reset the cache
			xpathApi = new CachedXPathAPI();
			resetCache();
			Document doc = contextNode instanceof Document ? (Document) contextNode : contextNode.getOwnerDocument();
			if (doc instanceof DocumentImpl) {
				lastDocument = (DocumentImpl) doc;
				lastDocument.addEventListener(MutationEventImpl.DOM_SUBTREE_MODIFIED, this, false);
				lastDocument.addEventListener(MutationEventImpl.DOM_NODE_INSERTED, this, false);
				lastDocument.addEventListener(MutationEventImpl.DOM_NODE_REMOVED, this, false);
				lastDocument.addEventListener(MutationEventImpl.DOM_ATTR_MODIFIED, this, false);
				lastDocument.addEventListener(MutationEventImpl.DOM_CHARACTER_DATA_MODIFIED, this, false);
			}
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
		if (lastDocument != null) {
			lastDocument.removeEventListener(MutationEventImpl.DOM_SUBTREE_MODIFIED, this, false);
			lastDocument.removeEventListener(MutationEventImpl.DOM_NODE_INSERTED, this, false);
			lastDocument.removeEventListener(MutationEventImpl.DOM_NODE_REMOVED, this, false);
			lastDocument.removeEventListener(MutationEventImpl.DOM_ATTR_MODIFIED, this, false);
			lastDocument.removeEventListener(MutationEventImpl.DOM_CHARACTER_DATA_MODIFIED, this, false);
			lastDocument = null;
		}
	}
	
	private boolean shouldReset(Node current) {
		boolean shouldReset = lastDocument == null || xpathApi == null;
		if (!shouldReset) {
			Document doc = current instanceof Document ? (Document) current : current.getOwnerDocument();
			shouldReset = !lastDocument.equals(doc);
		}
		return shouldReset;
	}
	
	@Override
	public void handleEvent(Event arg0) {
		resetCache();
	}
	
	private class JXPathNodeList implements NodeList {
		private List<Node> nodes;
		
		private JXPathNodeList(List<Node> nodes) {
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
	
	private class JXPathNodeIterator implements NodeIterator {
		private List<Node> nodes;
		private int i = 0;
		
		private JXPathNodeIterator(List<Node> nodes) {
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
