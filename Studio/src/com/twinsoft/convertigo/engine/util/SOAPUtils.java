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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.dom4j.io.DocumentSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.steps.StepException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.util.Log;

public class SOAPUtils {

	private static ThreadLocal<SOAPFactory> defaultSOAPFactory = new ThreadLocal<SOAPFactory>() {
		@Override
		protected SOAPFactory initialValue() {
			try {
				return SOAPFactory.newInstance();
			} catch (SOAPException e) {
				e.printStackTrace();
				return null;
			}
		}
	};

	/**
	 * SOAP to DOM transformer.
	 */
	public static Object getDOM(SOAPPart soapPart) throws TransformerException, SOAPException
	{
		Object ob = null;
		
		Source sc = soapPart.getContent();
		
		if (sc instanceof DocumentSource) {
			ob = ((DocumentSource) sc).getDocument();
		}
		else {
			// Create a transformer
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();

			// Create an instance of Result for a DOM tree
			DOMResult domResult = new DOMResult();

			// Transform !!
			transformer.transform(sc, domResult);
			ob = (Document)domResult.getNode();
		}

		return ob;
	}
	
	public static String toString(SOAPMessage soapMessage, String encoding) throws TransformerConfigurationException, TransformerException, SOAPException, IOException, ParserConfigurationException, SAXException {	
		soapMessage.saveChanges();
		
		if (encoding == null) { // #3803
			Engine.logEngine.warn("(SOAPUtils) encoding is null. Set encoding to UTF-8 for toString.");
			encoding = "UTF-8";
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		soapMessage.writeTo(out);
        String s = new String(out.toByteArray(), encoding); 

        s = XMLUtils.prettyPrintDOMWithEncoding(s, encoding);
        
		// Ticket #2678: fix empty "xmlns"
		s = s.replaceAll("\\sxmlns=\"\"", "");
		
		return s;
    }
    
    public static String writeSoapFault(Throwable e, String encoding) throws IOException {
		String faultString = null;
		StepException stepException = null;
		
		if (e instanceof SOAPException) {
			Throwable cause = ((SOAPException) e).getCause();
			Engine.logEngine.error("A SOAP error has occured while processing the web service request.", e);
			Engine.logEngine.error("Cause:", cause);
			faultString = "A SOAP error has occured while processing the web service request: " + cause.getMessage();
		}
		else {
			if (System.getProperty("java.specification.version").compareTo("1.4") >= 0) {
				Throwable eCause = e;
				while ((eCause = eCause.getCause()) != null)  {
					if (eCause instanceof StepException) {
						stepException = (StepException) eCause;
						break;
					}
				}				
			}
		
			if (stepException == null) {
				Engine.logEngine.error("Unable to analyze or execute the web service.", e);
				faultString = "Unable to analyze or execute the web service.";
			}
		}
		
		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage faultMessage = messageFactory.createMessage();
		
			SOAPPart sp = faultMessage.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			SOAPBody sb = se.getBody();
			SOAPFault fault = sb.addFault();
	
			fault.setFaultString(stepException == null ? faultString : stepException.getErrorMessage());
			
			fault.setFaultCode("soapenv:Server");

			SOAPFactory soapFactory = defaultSOAPFactory.get();
			Name name;
			DetailEntry detailEntry;

			if (stepException == null) {
				Detail detail = fault.addDetail();
				String faultDetail = e.getMessage();
				if (faultDetail == null) faultDetail = "";
		
				name = soapFactory.createName(e.getClass().getName());
				detailEntry = detail.addDetailEntry(name);
				detailEntry.addTextNode(faultDetail == null ? "(no more information)" : faultDetail);
				
				if (System.getProperty("java.specification.version").compareTo("1.4") >= 0) {
					Throwable eCause = e;
					while ((eCause = eCause.getCause()) != null)  {
						faultDetail = eCause.getMessage();
						name = soapFactory.createName(eCause.getClass().getName());
						detailEntry = detail.addDetailEntry(name);
						detailEntry.addTextNode(faultDetail == null ? "(no more information)" : faultDetail);
					}				
				}
				
	 			name = soapFactory.createName("moreinfo");
				detailEntry = detail.addDetailEntry(name);
				detailEntry.addTextNode("See the Convertigo engine log files for more details...");
			}
			else {
				// Details property of ExceptionStep is not empty
				String details = stepException.getErrorDetails();
				if (!(("").equals(details) || details.startsWith("org.mozilla.javascript.Undefined"))) {
					Detail detail = fault.addDetail();
					// If step's exception detail is an XML document, insert it in the detail SOAP part
					try {
						InputStream inputStream = (InputStream) new ByteArrayInputStream(details.getBytes("UTF-8"));
						DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
						documentBuilderFactory.setNamespaceAware(true);
						documentBuilderFactory.setValidating(true);
						DocumentBuilder docbuilder = documentBuilderFactory.newDocumentBuilder();
						Document domDetails = docbuilder.parse(inputStream);
						addDetails(detail, domDetails.getDocumentElement());
					} catch (Exception ee) {
						// Probably not an XML DOM, insert as CDATA
						name = soapFactory.createName("content");
						detailEntry = detail.addDetailEntry(name);
						detailEntry.addTextNode(details);
					}
				}
			}
			
			faultMessage.saveChanges();
			
			String sResponseMessage = "";
			sResponseMessage = SOAPUtils.toString(faultMessage, encoding);
			
			if (Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("SOAP response:\n" + sResponseMessage);
			}
			
			return sResponseMessage;

		} catch(Throwable ee) {
			Engine.logEngine.error("Unable to send the SOAP FAULT message.", ee);
			String response = "";
			response += Log.getStackTrace(ee);
			response += "SOAP error: " + faultString + "\n";
			response += "Initial exception:\n";
			response += Log.getStackTrace(e);
			if (e instanceof SOAPException) {
				Throwable cause = ((SOAPException) e).getCause();
				response += "SOAP Cause:\n";
				response += Log.getStackTrace(cause);
			}
			return response;
		}
	}
    
    private static void addDetails(SOAPElement detailEntry, Node node) throws SOAPException {
    	String prefix = node.getPrefix();
		String namespace = node.getNamespaceURI();
		String localname = node.getLocalName();
		
		QName qname = (prefix == null ? new QName(localname) : new QName(namespace, localname, prefix));
		SOAPElement childEntry = detailEntry.addChildElement(qname);

		// Add the attributes
		NamedNodeMap attributes =  node.getAttributes();
		int nAttributes = attributes.getLength();
		for (int i = 0; i < nAttributes; i++) {
			Attr attribute = (Attr) attributes.item(i);
			localname = attribute.getLocalName();
			prefix = attribute.getPrefix();
			namespace = attribute.getNamespaceURI();
			if (prefix == null) qname = new QName(localname);
			else qname = new QName(namespace, localname, prefix);
			String value = attribute.getNodeValue();
			childEntry.addAttribute(qname, value);
		}
		
		// Add sub nodes
		NodeList childNodes = node.getChildNodes();
		int nChildNodes = childNodes.getLength();
		for (int i = 0 ; i < nChildNodes; i++) {
			Node childNode = childNodes.item(i);
			if (childNode instanceof Text) {
				// Add the text content
				childEntry.addTextNode(childNode.getTextContent());
			}
			else {
				addDetails(childEntry, childNode);
			}
		}
    }
    
}