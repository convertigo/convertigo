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

import java.util.List;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.URLrewriter;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * 
 * This class simply extract a nodelist.
 *
 */
public class WebClipper extends XMLNodeList implements ITagsProperty {

	private static final long serialVersionUID = -6214516945439480891L;
	public static final String mHttptunnelOff = "disable";
	public static final String mHttptunnelOnCache = "cache";
	public static final String mHttptunnelOnNoCache = "no cache";

	/* Properties */
	protected XMLVector<XMLVector<String>> attributes = new XMLVector<XMLVector<String>>();
	protected boolean bHttpTunnel = false;
	protected String transactionName = "";
	protected String	mHttptunnel = mHttptunnelOff;
	protected boolean extractParent = false;

	/* Variables */
	transient private Context context = null;
	transient private Element head = null;
	transient private boolean mustRewriteHead = false;
	
	public WebClipper() {
		super();
		initAttributes();
	}

	@Override
	public WebClipper clone() throws CloneNotSupportedException {
		WebClipper webClipper = (WebClipper) super.clone();
		return webClipper;
	}
	
	private void initAttributes() {
		addAttribute("src");
		addAttribute("href");
		addAttribute("background");
		addAttribute("action");
		addAttribute("cite");
		addAttribute("classid");
		addAttribute("codebase");
		addAttribute("data");
		addAttribute("longdesc");
		addAttribute("usemap");		
	}
	
	private void addAttribute(String attr) {
		XMLVector<String> xmlv = new XMLVector<String>();
		xmlv.add(attr);
		attributes.add(xmlv);
	}
	
	@Override
	public boolean apply(Document xmlDom, Context context) {
		this.context = context;
		xpathApi = context.getXpathApi();
		getHead(xmlDom, context.outputDocument);
		context.getIdToXpathManager().setTransaction(transactionName);
		return super.apply(xmlDom, context);
	}
	
	@Override
	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element doc = outputDom.getDocumentElement();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (!isRequestedObjectRunning()) break;
			
			Element elt = importElements(doc, (Element) nodeList.item(i), false);
			context.urlRewriter.rewriteURL(elt);
			context.urlRewriter.rewriteStyle(elt);
			context.urlRewriter.rewriteStyleAttr(elt);
		}
		if (mustRewriteHead && isRequestedObjectRunning()) {
			head = importElements(outputDom.getDocumentElement(), head, true);
			context.urlRewriter.rewriteURL(head);
			context.urlRewriter.rewriteStyle(head);
			context.urlRewriter.rewriteStyleAttr(head);
		}
	}
	
	public XMLVector<XMLVector<String>> getAttributes() {
		if (attributes==null) {
			initAttributes();
		}
		return attributes;
	}
	
	public void setAttributes(XMLVector<XMLVector<String>> attributes) {
		this.attributes = attributes;
	}
	
	public boolean getBHttpTunnel() {
		return bHttpTunnel;
	}
	
	public void setBHttpTunnel(boolean bHttpTunnel) {
		this.bHttpTunnel = bHttpTunnel;
	}
	
	public String getMHttpTunnel() {
		return mHttptunnel;
	}
	
	public void setMHttpTunnel(String mHttptunnel) {
		if (mHttptunnel.equals(mHttptunnelOff) ||
			mHttptunnel.equals(mHttptunnelOnCache) ||
			mHttptunnel.equals(mHttptunnelOnNoCache)) {
				this.mHttptunnel = mHttptunnel;
		}
	}

	public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}
	
	protected void generateIds(Element original, Element copy) {
		String attr = context.getIdToXpathManager().addNode(original);
		
		Engine.logBeans.trace("Added node : "  + original.getNodeName() + " with id :" + attr);
		copy.setAttribute("twsid", attr);
		
		NodeList original_nodes = original.getChildNodes();
		NodeList copy_nodes = copy.getChildNodes();
		for (int i = 0; i < original_nodes.getLength(); i++) {
			Node node = original_nodes.item(i);
			Node copy_node = copy_nodes.item(i);
			if (node instanceof Element && copy_node instanceof Element) {
				generateIds((Element)node, (Element)copy_node);
			}
		}
	}
	
	protected Element getFirstTagName(Element from, String tagname) {
		if (from.getTagName().equals(tagname)) {
			return from;
		}
		NodeList lst = from.getElementsByTagName(tagname);
		return (Element) (lst.getLength() > 0 ? lst.item(0) : null);
	}
	
	protected Element importElements(Element parent, Element child, boolean atFirst) {
		Document owner = parent.getOwnerDocument();
		Element copyChild = (Element) owner.importNode(child, true);
		generateIds(child, copyChild);
		
		try {
			if (copyChild.getTagName().equals("HEAD") || xpathApi.selectNodeList(copyChild, "//HEAD[not(ancestor::IFRAME)][not(ancestor::FRAME)]").getLength() > 0) {
				// HEAD found in nodes that are copied => don't re-copy HEAD after
				mustRewriteHead = false;
			}
		} catch (TransformerException e) {
			Engine.logBeans.error("(WebClipper) Exception when using xpathAPI on WebClipper.importElements()", e);
		}
		
		removeUnwanted(copyChild);
		
		if (extractParent) {
			/**
			 * Add all parents (without root node > HTML) to clipped part
			 */
			Node upParent = child.getParentNode();
			while (upParent != null && !(upParent.getParentNode() instanceof Document)) {
				Node copyUpParent = owner.importNode(upParent, false);
				copyUpParent.appendChild(copyChild);
				upParent = upParent.getParentNode();
				copyChild = (Element) copyUpParent;
			}		
		}
		
		/**
		 * Replace FRAMESET
		 */
		Element elt;
		while ((elt = getFirstTagName(copyChild, "FRAMESET")) != null) {
			if (elt.hasAttribute("cols") || elt.hasAttribute("rows")) {
				owner.renameNode(elt, null, "TABLE");
				elt.setAttribute("border", "2");
				NodeList nl = elt.getChildNodes();
				Node[] frames = new Node[nl.getLength()];
				for (int i = 0; i < frames.length; i++) {
					frames[i] = nl.item(i);
				}
				if (elt.hasAttribute("cols")) {
					Element tr = owner.createElement("TR");
					for (int i = 0; i < frames.length; i++) {
						Element td = owner.createElement("TD");
						td.appendChild(elt.removeChild(frames[i]));
						tr.appendChild(td);
					}
					elt.appendChild(tr);
				} else {
					for (int i = 0; i < frames.length; i++) {
						Element tr = owner.createElement("TR");
						Element td = owner.createElement("TD");
						td.appendChild(elt.removeChild(frames[i]));
						tr.appendChild(td);
						elt.appendChild(tr);
					}
				}
			} else {
				owner.renameNode(elt, null, "DIV");
			}
			elt = getFirstTagName(copyChild, "FRAMESET");
		}

		while ((elt = getFirstTagName(copyChild, "FRAME")) != null) {
			owner.renameNode(elt, null, "DIV");
		}
		while ((elt = getFirstTagName(copyChild, "IFRAME")) != null) {
			owner.renameNode(elt, null, "DIV");
		}
		
		if (extractParent) {
			/**
			 * Find better parent in outputdom
			 */
			try {
				Node nodeToInsert = copyChild;
				Node insertInto = parent;
				Node insertIntoNext = insertInto.getFirstChild();
				while (insertIntoNext != null) {
					boolean ok = true;
					if (insertIntoNext.getNodeName().equals(nodeToInsert.getNodeName())) {
						NamedNodeMap iinAttrs = insertIntoNext.getAttributes();
						NamedNodeMap ntiAttrs = nodeToInsert.getAttributes();
						if (iinAttrs.getLength() == ntiAttrs.getLength()) {
							for (int i = 0; ok && i < ntiAttrs.getLength(); i++) {
								ok = (iinAttrs.item(i).getNodeValue() == null ?
											ntiAttrs.item(i).getNodeValue() == null :
											iinAttrs.item(i).getNodeValue().equals(ntiAttrs.item(i).getNodeValue()))
										&& (iinAttrs.item(i).getNodeName() == null ?
											ntiAttrs.item(i).getNodeName() == null :
											iinAttrs.item(i).getNodeName().equals(ntiAttrs.item(i).getNodeName()));
							}
						}
					} else {
						ok = false;
					}
					if (ok) {
						insertInto = insertIntoNext;
						insertIntoNext = insertInto.getFirstChild();
						nodeToInsert = nodeToInsert.getFirstChild();
					} else {
						insertIntoNext = insertIntoNext.getNextSibling();
					}
				}
				if (nodeToInsert == null) {
					return copyChild;
				}
				parent = (Element) insertInto;
				copyChild = (Element) nodeToInsert;
			} catch(Exception e) {
				Engine.logBeans.error("(WebClipper) unable to retrieve existing parent", e);
			}
		}
		
		if (atFirst) {
			parent.insertBefore(copyChild, parent.getFirstChild());
		} else {
			parent.appendChild(copyChild);
		}		
		
		xpathApi.resetCache(); // dom change
		return copyChild;
	}
	
	protected void removeUnwanted(Element elt) {
		try {
			NodeList aElements = xpathApi.selectNodeList(elt,".//*[@href[name(..)=\"A\"]]"); // A with href
			
			// recopy 'href' from 'A' elements in 'original_url' attribute
			for (int i = 0; i < aElements.getLength(); i++) { 
				Element aTmp = (Element) aElements.item(i);
				aTmp.setAttribute("original_url", aTmp.getAttribute("href"));
			}
			
			NodeList nodes = xpathApi.selectNodeList(elt, ".//SCRIPT" // remove scripts
				+ "|.//@*[starts-with(name(),\"on\")]"                        // remove on*** attributes
				+ "|.//@action"                                                      // remove action attributes
				+ "|.//@href[name(..)=\"A\"]"                                  // remove href attributes of A elements
				+ "|.//@target[name(..)=\"A\"]"                               // remove target attributes of A elements
			);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node instanceof Attr) {
					Attr at = (Attr) node;
					at.getOwnerElement().removeAttributeNode(at);
				} else if(node != null && node.getParentNode() != null) {
					node.getParentNode().removeChild(node);
				}
			}
			
			for (int i = 0; i < aElements.getLength(); i++) {// restore fake href
				((Element) aElements.item(i)).setAttribute("href", "#");
			}
			
			xpathApi.resetCache(); // dom change
		} catch (TransformerException e) {
			Engine.logBeans.error("Exception when using xpathAPI on WebClipper.removeUnwanted()", e);
		}
	}
	
	protected void getHead(Document doc, Document outputDoc) {
		try {
			String targetReferer = "";
			
			NodeList outputHeadNodes = xpathApi.selectNodeList(outputDoc, "//HEAD");
			if (outputHeadNodes.getLength() > 0) { 
				//already head
				head = (Element) outputHeadNodes.item(0);
			} else {
				context.idToXpathManager = null;
				
				NodeList currentHeadNodes = xpathApi.selectNodeList(doc, "/HTML/HEAD");
				if (currentHeadNodes.getLength() == 0) {
					//no head !
					context.urlRewriter = new URLrewriter(targetReferer, context, mHttptunnel, makeAttributesXpath());
					return;
				}
				mustRewriteHead = true;
				head = (Element) currentHeadNodes.item(0).cloneNode(true);
			}
			NodeList baseNl = head.getElementsByTagName("BASE");
			if (baseNl.getLength() > 0) {
				Element base = (Element) baseNl.item(0);
				targetReferer = base.getAttribute("href");
				base.getParentNode().removeChild(base);
			}
			context.urlRewriter = new URLrewriter(targetReferer, context, mHttptunnel, makeAttributesXpath());
			
		} catch (TransformerException e) {
			Engine.logBeans.error("Exception when using xpathAPI on WebClipper.getHead()", e);
		}
	}
	
	protected String makeAttributesXpath() {
		StringBuffer selectXpath = new StringBuffer();
		for (List<String> attribute : attributes) {
			selectXpath.append("|.//@" + attribute.get(0));
		}
		return selectXpath.length() == 0 ? "" : selectXpath.substring(1);
	}
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
        String version = element.getAttribute("version");
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        if (VersionUtils.compare(version, "4.2.1") < 0) {
        	mHttptunnel = (bHttpTunnel)?mHttptunnelOnCache:mHttptunnelOff;
			hasChanged = true;
			Engine.logBeans.warn("[HttpStatement] The object \"" + getName()+ "\" has been updated to version 4.2.1");
        }
	}

	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("mHttpTunnel")) {
			return new String[] {
				mHttptunnelOff,
				mHttptunnelOnCache,
				mHttptunnelOnNoCache
			};
		}
		return new String[0];
	}
	
	public boolean getExtractParent() {
		return extractParent;
	}

	public void setExtractParent(boolean extractParent) {
		this.extractParent = extractParent;
	}
}