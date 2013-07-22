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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

/**
 * 
 * This class simply extract a nodelist.
 *
 */
public class XMLSplitNodes extends HtmlExtractionRule {

	private static final long serialVersionUID = -4286418144685038971L;

	private String resultXpath= "";
	private String regExp = "";
	private boolean keepSeparator = true;
	
	public XMLSplitNodes() {
		super();
	}

	@Override
	public XMLSplitNodes clone() throws CloneNotSupportedException {
		XMLSplitNodes splitNodes = (XMLSplitNodes)super.clone();
		return splitNodes;
	}
	
	@Override
	public boolean apply(Document xmlDom, Context context) {
		xpathApi = context.getXpathApi();
		return super.apply(xmlDom, context);
	}
	
	@Override
	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element doc = outputDom.getDocumentElement();
		
		try {
			NodeList liste = xpathApi.selectNodeList(doc, resultXpath);
			 
			for (int i = 0 ; i < liste.getLength() ; i++) {
				if (!isRequestedObjectRunning()) break;
				
			 	Node treatedNode = liste.item(i); 
			 	// look for the text node to modify under treated node
			 	NodeList itemNodes = treatedNode.getChildNodes();
			 	Node tmpNode = null;
				for (int j = 0 ; j < itemNodes.getLength() ; j++) {
					tmpNode = itemNodes.item(j);
					if (tmpNode.getNodeType() == Node.TEXT_NODE)
						break;
				}
				if (tmpNode == null)
					continue;
				
				// we found a text node under treated node
				String texte = tmpNode.getNodeValue();
				Pattern myPattern = Pattern.compile(regExp);
				Matcher myMatcher = myPattern.matcher(texte);
				List<String> splitString = new LinkedList<String>();
				int beginIndex = 0, startIndex, endIndex, j=0;
				while (myMatcher.find()) {
					startIndex = myMatcher.start();
					endIndex = myMatcher.end();
					splitString.add(texte.substring(beginIndex, startIndex));
					if (keepSeparator)
						splitString.add(texte.substring(startIndex, endIndex));
					beginIndex = endIndex;
				}
				if (beginIndex != texte.length())
					splitString.add(texte.substring(beginIndex, texte.length()));
				
				// for all split string : create a node and add it
				for(String tmp : splitString){
					Element elem = outputDom.createElement("item_" + (j++)); 
					Node text = outputDom.createTextNode(tmp);
					elem.appendChild(text);
					treatedNode.appendChild(elem);
				}
				// remove text node value now it has been split	
				tmpNode.setNodeValue(""); 
			} 
		} catch(TransformerException e) {
			Engine.logBeans.debug("Xpath error : no node list found for " + resultXpath);
		}
	}

	public boolean isKeepSeparator() {
		return keepSeparator;
	}

	public void setKeepSeparator(boolean keepSeparator) {
		this.keepSeparator = keepSeparator;
	}

	public String getRegExp() {
		return regExp;
	}

	public void setRegExp(String regExp) {
		this.regExp = regExp;
	}

	public String getResultXpath() {
		return resultXpath;
	}

	public void setResultXpath(String xpath) {
		this.resultXpath = xpath;
	}

	public String getSchema(String tns) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSchemaElementNSType(String tns) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSchemaElementName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSchemaElementType() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
