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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.parsers.HtmlParser;
import com.twinsoft.convertigo.engine.util.URLrewriter;

/**
 * 
 * This class simply extract a nodelist.
 *
 */
public class WebClipper2 extends XMLNodeList implements ITagsProperty {

	private static final long serialVersionUID = -6214516945439480891L;
	public static final String mHttptunnelOff = "disable";
	public static final String mHttptunnelOnCache = "cache";
	public static final String mHttptunnelOnNoCache = "no cache";

	/* Properties */
	protected XMLVector<XMLVector<String>> attributes = new XMLVector<XMLVector<String>>();
	protected String	mHttptunnel = mHttptunnelOff;
	
	/* Variables */
	transient private Context 	context 		= null;
	transient private HtmlParser htmlParser		= null;
	
	public WebClipper2() {
		super();
		initAttributes();
	}

	@Override
	public WebClipper2 clone() throws CloneNotSupportedException {
		WebClipper2 webClipper = (WebClipper2)super.clone();
		return webClipper;
	}
	
	private void initAttributes(){
		addAttribute( "src");
		addAttribute( "href");
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
		htmlParser = ((HtmlConnector)context.getConnector()).getHtmlParser();
		xpathApi = context.getXpathApi();
		initUrlRewriter(xmlDom, context.outputDocument);
		
		try {
			Element indoc = htmlParser.getStyledDom(context, xpath).getDocumentElement();
			Element outDoc = context.outputDocument.getDocumentElement();
			
			context.urlRewriter.rewriteURL(indoc);
			context.urlRewriter.rewriteStyleAttr(indoc);
			
			NodeList nodeList = indoc.getChildNodes();
			NodeList originalNodeList = xpathApi.selectNodeList(xmlDom, xpath);
			for(int i=0;i<nodeList.getLength();i++){		
				Element inChild = (Element)nodeList.item(i);
				Element copyChild = (Element)context.outputDocument.importNode(inChild, true);
				generateIds((Element)originalNodeList.item(i), copyChild);
				removeUnwanted(copyChild);
				outDoc.appendChild(copyChild);
			}
		} catch (Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Unexpected exception in WebClipper2.apply()", e);
		}
		return true;
	}
	
	public XMLVector<XMLVector<String>> getAttributes(){
		if(attributes==null) initAttributes();
		return attributes;
	}
	
	public void setAttributes(XMLVector<XMLVector<String>> attributes){
		this.attributes = attributes;
	}	
	
	public String getMHttpTunnel(){
		return mHttptunnel;
	}
	
	public void setMHttpTunnel(String mHttptunnel){
		if(mHttptunnel.equals(mHttptunnelOff)||
			mHttptunnel.equals(mHttptunnelOnCache)||
			mHttptunnel.equals(mHttptunnelOnNoCache)){
				this.mHttptunnel = mHttptunnel;
		}
	}
	
	protected void generateIds(Element original, Element copy){
		String attr = context.getIdToXpathManager().addNode(original);
		
		Engine.logBeans.trace("Added node : "  + original.getNodeName() + " with id :" + attr);
		copy.setAttribute("twsid", attr);
		NodeList original_nodes = original.getChildNodes();
		NodeList copy_nodes = copy.getChildNodes();
		for(int i=0;i<original_nodes.getLength();i++){
			Node node = original_nodes.item(i);
			Node copy_node = copy_nodes.item(i);
			if(node instanceof Element && copy_node instanceof Element)
				generateIds((Element)node, (Element)copy_node);
			else {
				// Engine.logBeans.trace("Add id (not an element "  + node + " copy is : " + copy_node);
				;
			}
		}
	}
	
	protected void removeUnwanted(Element elt){
		try {
			NodeList aElements = xpathApi.selectNodeList(elt,".//*[@href[name(..)=\"A\"]]"); // A with href
			
			NodeList nodes = xpathApi.selectNodeList(elt, ".//SCRIPT"	// remove scripts
				+"|.//@*[starts-with(name(),\"on\")]"					// remove on*** attributes
				+"|.//@action"											// remove action attributes
				+"|.//@href[name(..)=\"A\"]"							// remove href attributes of A elements
				+"|.//@target[name(..)=\"A\"]"							// remove target attributes of A elements
			);
			for(int i=0;i<nodes.getLength();i++){
				Node node = nodes.item(i);
				if(node instanceof Attr){
					Attr at = (Attr)node;
					at.getOwnerElement().removeAttributeNode(at);
				}else if(node!=null && node.getParentNode()!=null)
					node.getParentNode().removeChild(node);
			}
			
			for(int i=0;i<aElements.getLength();i++) // restore fake href
				((Element)aElements.item(i)).setAttribute("href", "#");
			
			xpathApi.resetCache(); // dom change
		} catch (TransformerException e) {
			Engine.logBeans.error("Exception when using xpathAPI on WebClipper.removeUnwanted()");
		}
	}
	
	protected void initUrlRewriter(Document doc, Document outputDoc) {
		try {
			String targetReferer = "";
			
			Node node = xpathApi.selectSingleNode(outputDoc, "//@twsid");
			if(node==null){
				context.idToXpathManager = null;
				
				NodeList currentHeadNodes = xpathApi.selectNodeList(doc, "/HTML/HEAD");
				if(currentHeadNodes.getLength()!=0) {
					Element head = (Element)currentHeadNodes.item(0);

					NodeList baseNl = head.getElementsByTagName("BASE");
					if(baseNl.getLength() > 0){
						Element base = (Element)baseNl.item(0);
						targetReferer = base.getAttribute("href");
						base.getParentNode().removeChild(base);
					}
				}
				context.urlRewriter = new URLrewriter(targetReferer, context, mHttptunnel, makeAttributesXpath());
			}
		} catch (TransformerException e) {
			Engine.logBeans.error("Exception when using xpathAPI on WebClipper.getHead()", e);
		}
	}
	
	protected String makeAttributesXpath(){
		StringBuffer selectXpath = new StringBuffer();
		for(List<String> attribute : attributes)
				selectXpath.append("|.//@" + attribute.get(0));
		return selectXpath.length()==0 ? "" : selectXpath.substring(1);
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("mHttpTunnel")){
			return new String[]{
					mHttptunnelOff,
					mHttptunnelOnCache,
					mHttptunnelOnNoCache
			};
		}
		return new String[0];
	}
}
