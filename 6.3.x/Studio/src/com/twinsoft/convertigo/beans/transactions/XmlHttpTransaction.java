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

package com.twinsoft.convertigo.beans.transactions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XmlHttpTransaction extends AbstractHttpTransaction {

	private String xmlEncoding = "ISO-8859-1";
	
	private String responseElementQName = "";
	
	private boolean ignoreSoapEnveloppe = false;
	
	private static final long serialVersionUID = 1494278577299328199L;

	public XmlHttpTransaction()
	{
		super();
	}
	
	public String getXmlEncoding() {
		return xmlEncoding;
	}

	public void setXmlEncoding(String xmlEncoding) {
		this.xmlEncoding = xmlEncoding;
	}
	
	/**
	 * @return the responseElementQName
	 */
	public String getResponseElementQName() {
		return responseElementQName;
	}

	/**
	 * @param responseElementQName the responseElementQName to set
	 */
	public void setResponseElementQName(String responseElementQName) {
		this.responseElementQName = responseElementQName;
	}

	/**
	 * @return the ignoreSoapEnveloppe
	 */
	public boolean isIgnoreSoapEnveloppe() {
		return ignoreSoapEnveloppe;
	}

	/**
	 * @param ignoreSoapEnveloppe the ignoreSoapEnveloppe to set
	 */
	public void setIgnoreSoapEnveloppe(boolean ignoreSoapEnveloppe) {
		this.ignoreSoapEnveloppe = ignoreSoapEnveloppe;
	}

	@Override
	public void makeDocument(byte[] httpData) throws Exception {
		Engine.logBeans.trace("makeDocument : " + getEncodingCharSet());
		
		String sdata = new String(httpData, xmlEncoding);
		Engine.logBeans.trace("makeDocument afternewString: " + sdata);

		
		Document xmlHttpDocument = requester.parseDOM(sdata);
		if (Engine.logBeans.isTraceEnabled())
			Engine.logBeans.trace("makeDocument after parseDom: " + XMLUtils.prettyPrintDOM(xmlHttpDocument));
		
		// Removes soap elements if needed
		if (isIgnoreSoapEnveloppe()) {
			Element soapBodyResponseElement = null;
			soapBodyResponseElement = getSoapBodyResponseElement(xmlHttpDocument.getDocumentElement());
	        if (soapBodyResponseElement != null) {
	        	NodeList childNodes = soapBodyResponseElement.getChildNodes();
	    		int len = childNodes.getLength();
	    		Node child, node;
	            for (int i = 0 ; i < len ; i++) {
	            	node = childNodes.item(i);
	            	if (node instanceof Element) {
	            		child = importNodeWithNoPrefix(context.outputDocument, node, true);
	            		context.outputDocument.getDocumentElement().appendChild(child);
	            	}
	            }
	        }
	        else {
	        	XMLUtils.copyDocument(xmlHttpDocument, context.outputDocument);
	        }
		}
		// Normal case
		else
			XMLUtils.copyDocument(xmlHttpDocument, context.outputDocument);
    }

	private Node importNodeWithNoPrefix(Document document, Node node, boolean deep) {
		Node newNode = null;
		
		if (node instanceof Element) {
			String localName = node.getNodeName().substring(node.getNodeName().indexOf(":")+1);
			newNode = document.createElement(localName);

			NodeList childNodes = node.getChildNodes();
			int len = childNodes.getLength();
			Node child;
	        for (int i = 0 ; i < len ; i++) {
	        	child = childNodes.item(i);
	        	if (deep && (child instanceof Element)) {
	        		newNode.appendChild(importNodeWithNoPrefix(document, child, deep));
	        	}
	        	else {
	        		newNode.appendChild(document.importNode(child, deep));
	        	}
	        }
		}
			
		return newNode;
	}
	
	private Element getSoapBodyResponseElement(Element element) {
		NodeList childNodes = element.getChildNodes();
		int len = childNodes.getLength();
		Node node;
        for (int i = 0 ; i < len ; i++) {
        	node = childNodes.item(i);
        	if (node instanceof Element) {
        		String ename = ((Element)node).getNodeName().toUpperCase();
				if ((ename.indexOf("SOAPENV:") != -1) || (ename.indexOf("SOAP-ENV:") != -1) || (ename.indexOf("SOAP:") != -1)) {
					if (ename.indexOf(":BODY") != -1) {
						return ((Element)node);
					}
					else {
						return getSoapBodyResponseElement((Element)node);
					}
				}
        	}
        }
        return null;
	}
	
	@Override
	protected String extractXsdType(Document document) throws Exception {
    	String reqn = getResponseElementQName();
    	if (!reqn.equals("")) return generateWsdlType(document);
		return super.extractXsdType(document);
	}

	@Override
	public String generateWsdlType(Document document) throws Exception {
    	String reqn = getResponseElementQName();
    	if (!reqn.equals("")) {
    		String opName = getName()+"Response", eltName = "response", eltType = "xsd:string";
    		boolean useRef = true;
    		int index, index2;
    		if ((index = reqn.indexOf(";")) != -1) {
    			useRef = false;
    			opName = reqn.substring(0, index);
    			if ((index2 = reqn.indexOf(";", index+1)) != -1) {
        			eltName = reqn.substring(index+1,index2);
        			eltType = reqn.substring(index2+1);
    			}
    		}
    		
    		String prefix = getXsdTypePrefix();
    		String xsdType = "";
    		xsdType += "<xsd:complexType name=\""+ prefix + getName() +"Response" +"\" >\n";
    		xsdType += "  <xsd:sequence>\n";
    		if (useRef)
    			xsdType += "    <xsd:element ref=\""+ reqn +"\"/>\n";
    		else {
    			xsdType += "    <xsd:element name=\""+ opName +"\">\n";
    			xsdType += "      <xsd:complexType>\n";
    			xsdType += "        <xsd:sequence>\n";
    			xsdType += "          <xsd:element name=\""+ eltName +"\" type=\""+ eltType +"\"/>\n";
    			xsdType += "        </xsd:sequence>\n";
    			xsdType += "      </xsd:complexType>\n";
    			xsdType += "    </xsd:element>\n";
    		}
    		xsdType += "  </xsd:sequence>\n";
    		xsdType += "</xsd:complexType>\n";
    		return xsdType;
    	}
    	return super.generateWsdlType(document);
    }
	
	/*
	private void dumpByteArray(byte[] data)
	{
		byte	c[] = new byte[1];
		
		for (int j=0; j <data.length; j+= 80) {
			String	displayH ="";
			String	displayC ="";
			
			for (int i=0; i < 80; i++) {
				if ((i+j) < data.length) {
					byte b = data[i+j];
					if (b < 16) {
						displayH += "0" + Integer.toHexString(b) + " ";
					} else {
						displayH += Integer.toHexString(b) + " ";;
					}
					c[0] = b;
					displayC += new String(c) + "  ";
				}
			}
			Engine.logBeans.debug(displayH);
			Engine.logBeans.debug(displayC);
		}
	}
	*/
}

