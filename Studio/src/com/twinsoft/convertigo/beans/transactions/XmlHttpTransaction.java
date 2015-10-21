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

import java.io.File;
import java.nio.charset.Charset;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.apache.commons.lang3.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.IElementRefAffectation;
import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.ErrorType;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class XmlHttpTransaction extends AbstractHttpTransaction implements IElementRefAffectation {

	private String xmlEncoding = "ISO-8859-1";
	
	private String responseElementQName = "";
	
	private XmlQName xmlElementRefAffectation = new XmlQName();
	
	private boolean ignoreSoapEnveloppe = false;
	
	private boolean errorOnSoapFault = true;
	
	private static final long serialVersionUID = 1494278577299328199L;
	
	public XmlHttpTransaction(){
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

	/**
	 * @return the errorOnSoapFault
	 */
	public boolean isErrorOnSoapFault() {
		return errorOnSoapFault;
	}

	/**
	 * @param errorOnSoapFault the errorOnSoapFault to set
	 */
	public void setErrorOnSoapFault(boolean errorOnSoapFault) {
		this.errorOnSoapFault = errorOnSoapFault;
	}

	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		String version = element.getAttribute("version");
		
		if (VersionUtils.compare(version, "7.3.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propVarDom = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "errorOnSoapFault");
			if (propVarDom == null) {
				errorOnSoapFault = false;
				hasChanged = true;
				Engine.logBeans.warn("[Project] Successfully set 'errorOnSoapFault' property for project \""+ getName() +"\" (v 7.3.0)");
			}
		}
		
	}

	@Override
	public void makeDocument(byte[] httpData) throws Exception {
		Engine.logBeans.trace("makeDocument : " + getEncodingCharSet());
		
		Charset charset = XMLUtils.getEncoding(httpData, Charset.forName(xmlEncoding));
		
		String sdata = new String(httpData, charset);
		sdata = sdata.replaceFirst("[\\d\\D]*?<", "<");
		Engine.logBeans.trace("makeDocument afternewString: " + sdata);
		
		Document xmlHttpDocument = requester.parseDOM(sdata);
		if (Engine.logBeans.isTraceEnabled())
			Engine.logBeans.trace("makeDocument after parseDom: " + XMLUtils.prettyPrintDOM(xmlHttpDocument));
		
		// Replace SOAP fault by an c8o error element
		if (isErrorOnSoapFault()) {
			Element soapFaultElement = null;
			soapFaultElement = getSoapFaultElement(xmlHttpDocument);
			if (soapFaultElement != null) {
				String sfm = getSoapFaultMessage(soapFaultElement);
				ConvertigoException ex = new ConvertigoException("The Web Service returned a SOAP Fault",new SOAPException(sfm));
				ConvertigoError err = ConvertigoError.initError(ErrorType.Project, ex);
				Document errDocument = err.buildErrorDocument(getRequester(), context);
				Node error = context.outputDocument.importNode(errDocument.getDocumentElement().getFirstChild(), true);
				context.outputDocument.getDocumentElement().appendChild(error);
				return;
			}
		}
		
		if (getAllowDownloadAttachment()) {
			Element attachmentInfo = (Element) XPathAPI.selectSingleNode(context.outputDocument, "/document/AttachmentInfo");
			
			if (attachmentInfo != null) {
				NodeList nl = XPathAPI.selectNodeList(attachmentInfo, "attachment");
				
				for (int i = 0; i < nl.getLength(); i++) {
					Element attachment = (Element) nl.item(i);
					String cid = attachment.getAttribute("cid");
					
					if (StringUtils.isNotBlank(cid)) {
						Element include = (Element) XPathAPI.selectSingleNode(xmlHttpDocument, "//*[local-name()='Include' and @href='" + cid + "']");
						
						if (include != null) {
							include.appendChild(xmlHttpDocument.importNode(attachment, true));
							XMLUtils.removeNode(attachment);
						}
					}
				}
							
				if (XPathAPI.selectSingleNode(attachmentInfo, "attachment") == null) {
					XMLUtils.removeNode(attachmentInfo);
				}
			}
		}
		
		// Removes soap elements if needed
		if (isIgnoreSoapEnveloppe()) {
			Element soapBodyResponseElement = null;
			soapBodyResponseElement = getSoapBodyResponseElement(xmlHttpDocument.getDocumentElement());
	        if (soapBodyResponseElement != null) {
	        	NamedNodeMap attributes = ((Element)soapBodyResponseElement.getParentNode()).getAttributes();
	        	NodeList childNodes = soapBodyResponseElement.getChildNodes();
	    		int len = childNodes.getLength();
	    		Node child, node;
	            for (int i = 0 ; i < len ; i++) {
	            	node = childNodes.item(i);
	            	if (node instanceof Element) {
	            		//child = importNodeWithNoPrefix(context.outputDocument, node, true);
	            		child = context.outputDocument.importNode(node, true);
	            		// add envelope attributes (e.g namespace declarations to avoid unbound prefixes for XSL transformation)
						for (int j=0; j <attributes.getLength(); j++) {
							Node attr = attributes.item(j);
							((Element)child).setAttributeNode((Attr) context.outputDocument.importNode(attr, true));
						}
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

//	private Node importNodeWithNoPrefix(Document document, Node node, boolean deep) {
//		Node newNode = null;
//		
//		if (node instanceof Element) {
//			String localName = node.getNodeName().substring(node.getNodeName().indexOf(":")+1);
//			newNode = document.createElement(localName);
//
//			NodeList childNodes = node.getChildNodes();
//			int len = childNodes.getLength();
//			Node child;
//	        for (int i = 0 ; i < len ; i++) {
//	        	child = childNodes.item(i);
//	        	if (deep && (child instanceof Element)) {
//	        		newNode.appendChild(importNodeWithNoPrefix(document, child, deep));
//	        	}
//	        	else {
//	        		newNode.appendChild(document.importNode(child, deep));
//	        	}
//	        }
//		}
//			
//		return newNode;
//	}
	
	private String getSoapFaultMessage(Element element) {
		String message = "unknow reason";
		if (element != null) {
    		NodeList childNodes = element.getChildNodes();
    		int len = childNodes.getLength();
    		Node node;
            for (int i = 0 ; i < len ; i++) {
            	node = childNodes.item(i);
            	if (node instanceof Element) {
            		String ename = ((Element)node).getNodeName().toUpperCase();
					if (ename.equals("FAULTSTRING")) { // SOAP 1.1
						return ((Element)node).getTextContent();
					}
					else if (ename.equals("TEXT")) { // SOAP 1.2
						return ((Element)node).getTextContent();
					}
					else if (ename.indexOf(":REASON") != -1) { // SOAP 1.2
						return getSoapFaultMessage((Element)node);
					}
            	}
            }
		}
		return message;
	}

	private Element getSoapFaultElement(Document document) {
		Element soapBodyResponseElement = null;
		soapBodyResponseElement = getSoapBodyResponseElement(document.getDocumentElement());
        if (soapBodyResponseElement != null) {
    		NodeList childNodes = soapBodyResponseElement.getChildNodes();
    		int len = childNodes.getLength();
    		Node node;
            for (int i = 0 ; i < len ; i++) {
            	node = childNodes.item(i);
            	if (node instanceof Element) {
            		String ename = ((Element)node).getNodeName().toUpperCase();
					if (ename.indexOf(":FAULT") != -1) {
						return ((Element)node);
					}
            	}
            }
        }
		return null;
	}

	private Element getSoapBodyResponseElement(Element element) {
		NodeList childNodes = element.getChildNodes();
		int len = childNodes.getLength();
		Element soapBody = null;
		Node node;
        for (int i = 0 ; i < len ; i++) {
        	node = childNodes.item(i);
        	if (node instanceof Element) {
        		String ename = ((Element)node).getNodeName().toUpperCase();
				if ((ename.indexOf("SOAPENV:") != -1) || (ename.indexOf("SOAP-ENV:") != -1) || (ename.indexOf("SOAP:") != -1)) {
					if (ename.indexOf(":BODY") != -1) {
						soapBody = ((Element)node);
					} else {
						soapBody = getSoapBodyResponseElement((Element)node);
					}
				}
        	}
        	if (soapBody != null) {
        		return soapBody;
        	}
        }
        return null;
	}
	
	@Override
	protected String extractXsdType(Document document) throws Exception {
		XmlQName xmlQName = getXmlElementRefAffectation();
    	String reqn = getResponseElementQName();
    	if (!reqn.equals("") || !xmlQName.isEmpty())
    		return generateWsdlType(document);
		return super.extractXsdType(document);
	}

	@Override
	public String generateWsdlType(Document document) throws Exception {
		XmlQName xmlQName = getXmlElementRefAffectation();
    	String reqn = getResponseElementQName();
    	if (!reqn.equals("") || !xmlQName.isEmpty()) {
//    		String opName = getName()+"Response", eltName = "response", eltType = "xsd:string";
//    		boolean useRef = true;
//    		int index, index2;
//    		if ((index = reqn.indexOf(";")) != -1) {
//    			useRef = false;
//    			opName = reqn.substring(0, index);
//    			if ((index2 = reqn.indexOf(";", index+1)) != -1) {
//        			eltName = reqn.substring(index+1,index2);
//        			eltType = reqn.substring(index2+1);
//    			}
//    		}
//    		
//    		String prefix = getXsdTypePrefix();
//    		String xsdType = "";
//    		xsdType += "<xsd:complexType name=\""+ prefix + getName() +"Response" +"\" >\n";
//    		xsdType += "  <xsd:sequence>\n";
//    		if (useRef)
//    			xsdType += "    <xsd:element ref=\""+ reqn +"\"/>\n";
//    		else {
//    			xsdType += "    <xsd:element name=\""+ opName +"\">\n";
//    			xsdType += "      <xsd:complexType>\n";
//    			xsdType += "        <xsd:sequence>\n";
//    			xsdType += "          <xsd:element name=\""+ eltName +"\" type=\""+ eltType +"\"/>\n";
//    			xsdType += "        </xsd:sequence>\n";
//    			xsdType += "      </xsd:complexType>\n";
//    			xsdType += "    </xsd:element>\n";
//    		}
//    		xsdType += "  </xsd:sequence>\n";
//    		xsdType += "</xsd:complexType>\n";
    		String xsdType = "<xsd:complexType name=\""+ getXsdResponseElementName() +"\" />\n";
    		try {
	    		XmlSchema xmlSchema = createSchema();
	    		XmlSchemaComplexType cType = (XmlSchemaComplexType) xmlSchema.getTypeByName(getXsdResponseTypeName());
	    		xsdType = cType.toString("xsd", 0);
    		}
    		catch (Exception e) {
    			e.printStackTrace();
    		}
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
	
	@Override
	public void writeSchemaToFile(String xsdTypes) {
		XmlQName xmlQName = getXmlElementRefAffectation();
		boolean bRPC = getResponseElementQName().indexOf(";") != -1;
		if (!xmlQName.isEmpty() || bRPC) {
			try {
				XmlSchema xmlSchema = createSchema();
				String ns = getElementRefAffectation().getNamespaceURI();
				XmlSchemaCollection collection = Engine.theApp.schemaManager.getSchemasForProject(getProject().getName());
				XmlSchema referenced = collection.schemaForNamespace(ns);
				if (referenced != null) {
					if (ns.equals(xmlSchema.getTargetNamespace())) {
						XmlSchemaInclude xmlSchemaInclude = new XmlSchemaInclude();
						xmlSchemaInclude.setSchemaLocation(referenced.getSourceURI());
						xmlSchemaInclude.setSchema(referenced);
						XmlSchemaUtils.add(xmlSchema, xmlSchemaInclude);
					}
					else {
						XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
						xmlSchemaImport.setNamespace(referenced.getTargetNamespace());
						xmlSchemaImport.setSchemaLocation(referenced.getSourceURI());
						xmlSchemaImport.setSchema(referenced);
						XmlSchemaUtils.add(xmlSchema, xmlSchemaImport);
					}
				}
				new File(getSchemaFileDirPath()).mkdirs();
				SchemaUtils.saveSchema(getSchemaFilePath(), xmlSchema);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			super.writeSchemaToFile(xsdTypes);
		}
	}
	
	
	@Override
	public XmlSchemaInclude getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaInclude xmlSchemaInclude = super.getXmlSchemaObject(collection, schema);	
		
		XmlQName xmlQName = getXmlElementRefAffectation();
		String reqn = getResponseElementQName();
		if (!xmlQName.isEmpty() || !reqn.equals("")) {
			XmlSchema transactionSchema =  createSchema();
			if (transactionSchema != null) {
//				Transformer transformer = TransformerFactory.newInstance().newTransformer();
//				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//				transformer.transform(new DOMSource(transactionSchema.getSchemaDocument()), new StreamResult(System.out));
				xmlSchemaInclude.setSchema(transactionSchema);
			}
		}
		return xmlSchemaInclude;
	}

	@Override
	protected XmlSchemaComplexType addSchemaResponseDataType(XmlSchema xmlSchema) {
		XmlSchemaComplexType xmlSchemaComplexType = super.addSchemaResponseDataType(xmlSchema);
		XmlQName xmlQName = getXmlElementRefAffectation();
		boolean bRPC = getResponseElementQName().indexOf(";") != -1;
		if (!xmlQName.isEmpty() || bRPC) {
			XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence)xmlSchemaComplexType.getParticle();
			if (xmlSchemaSequence == null) xmlSchemaSequence = new XmlSchemaSequence();
			XmlSchemaElement xmlSchemaElement = new XmlSchemaElement();
			if (bRPC) {
				// response is based on an element defined with a type
	    		String reqn = getResponseElementQName();// operationResponse element name; element name; element type
				String opName = getName()+"Response", eltName = "response", eltType = "{"+Constants.URI_2001_SCHEMA_XSD+"}string";
	    		QName typeName = Constants.XSD_STRING;
				int i, j, k, z;
	    		if ((i = reqn.indexOf(";")) != -1) {
	    			opName = reqn.substring(0, i);
	    			if ((j = reqn.indexOf(";", i+1)) != -1) {
	        			eltName = reqn.substring(i+1,j);
	        			eltType = reqn.substring(j+1);
	        			if ((k = eltType.indexOf("{")) != -1) {
		        			if ((z = eltType.indexOf("}")) != -1) {
		        				String ns = eltType.substring(k+1, z);
		        				String local = eltType.substring(z+1);
		        				typeName = new QName(ns,local);
		        			}
	        			}
	    			}
	    		}
				
				xmlSchemaElement.setName(opName);
				XmlSchemaComplexType cType = new XmlSchemaComplexType(xmlSchema);
				XmlSchemaSequence seq = new XmlSchemaSequence();
				XmlSchemaElement elem = new XmlSchemaElement();
				elem.setName(eltName);
				elem.setSchemaTypeName(typeName);
				seq.getItems().add(elem);
				cType.setParticle(seq);
				xmlSchemaElement.setSchemaType(cType);
			}
			else {
				xmlSchemaElement.setName("");
				xmlSchemaElement.setRefName(xmlQName.getQName());
			}
			
			if (isErrorOnSoapFault()) {
				xmlSchemaElement.setMinOccurs(0);
			}
			xmlSchemaSequence.getItems().add(xmlSchemaElement);
			xmlSchemaComplexType.setParticle(xmlSchemaSequence);
		}
		return xmlSchemaComplexType;
	}

	public XmlQName getXmlElementRefAffectation() {
		return xmlElementRefAffectation;
	}

	public void setXmlElementRefAffectation(XmlQName xmlComplexTypeAffectation) {
		this.xmlElementRefAffectation = xmlComplexTypeAffectation;
	}

	public QName getElementRefAffectation() {
		return getXmlElementRefAffectation().getQName();
	}

}
