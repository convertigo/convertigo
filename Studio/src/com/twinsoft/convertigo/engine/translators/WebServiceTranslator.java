/*
 * Copyright (c) 2001-2012 Convertigo SA.
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

package com.twinsoft.convertigo.engine.translators;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.AttachmentManager;
import com.twinsoft.convertigo.engine.AttachmentManager.AttachmentDetails;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.requesters.WebServiceServletRequester;
import com.twinsoft.convertigo.engine.servlets.WebServiceServlet;
import com.twinsoft.convertigo.engine.util.Base64v21;
import com.twinsoft.convertigo.engine.util.SOAPUtils;

public class WebServiceTranslator implements Translator {
	
	private WebServiceServletRequester webServiceServletRequester;
	
	public WebServiceTranslator(WebServiceServletRequester webServiceServletRequester) {
		this.webServiceServletRequester = webServiceServletRequester;
	}

	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logBeans.debug("[WebServiceTranslator] Making input document");

        HttpServletRequest request = (HttpServletRequest) inputData;

		SOAPMessage requestMessage = (SOAPMessage) request.getAttribute(WebServiceServlet.REQUEST_MESSAGE_ATTRIBUTE);
		
		SOAPPart sp = requestMessage.getSOAPPart();
		SOAPEnvelope se = sp.getEnvelope();
		SOAPBody sb = se.getBody();

		Iterator<?> iterator = sb.getChildElements();
		SOAPElement method, parameter;
		String methodName;

		InputDocumentBuilder inputDocumentBuilder = new InputDocumentBuilder(context);
		
		while (iterator.hasNext()) {
			List<RequestableVariable> variableList = null; // jmc 12/06/26
			
			Object element = iterator.next();
			if (element instanceof SOAPElement) {
				method = (SOAPElement) element;

				methodName = method.getElementName().getLocalName();
				Engine.logBeans.debug("[WebServiceTranslator] Requested web service name: " + methodName);

				int i = methodName.indexOf("__");
				
				// for statefull transaction, don't replace the project
				if(context.project==null || !context.project.getName().equals(context.projectName)) {
					context.project = Engine.theApp.databaseObjectsManager.getProjectByName(context.projectName);
				}

				String connectorName = null;
				if (i == -1) {
					context.connectorName = null;
					context.sequenceName = methodName;
				}
				else {
					connectorName = methodName.substring(0, i);
					context.transactionName = methodName.substring(i + 2);
				}

				if ((connectorName != null) && (!connectorName.equals(context.connectorName))) {
			        Engine.logBeans.debug("Connector name differs from previous one; requiring new session");
					context.isNewSession = true;
					context.connectorName = connectorName;
					Engine.logBeans.debug("[WebServiceTranslator] The connector is overridden to \"" + context.connectorName + "\".");
				}
				
				Engine.logBeans.debug("[WebServiceTranslator] Connector: " + (context.connectorName == null ? "(default)" : context.connectorName));
				Engine.logBeans.debug("[WebServiceTranslator] Transaction: " + context.transactionName);

				//Connector connector = (context.connectorName == null ? context.project.getDefaultConnector() : context.project.getConnectorByName(context.connectorName));
				//Transaction transaction = (context.transactionName == null ? connector.getDefaultTransaction() : connector.getTransactionByName(context.transactionName));
				RequestableObject requestable = null;
				if (context.sequenceName != null) {
					requestable = context.project.getSequenceByName(context.sequenceName);
					variableList = ((Sequence) requestable).getVariablesList();
				}
				else if (context.connectorName != null) {
					if (context.transactionName != null) {
						requestable = context.project.getConnectorByName(context.connectorName).getTransactionByName(context.transactionName);
						if (requestable instanceof TransactionWithVariables) {
							variableList = ((TransactionWithVariables) requestable).getVariablesList();
						}
					}									
				}
				
				Iterator<?> iterator2 = method.getChildElements();
				String parameterName, parameterValue;

				while (iterator2.hasNext()) {
					element = iterator2.next();
					if (element instanceof SOAPElement) {
						parameter = (SOAPElement) element;
						parameterName = parameter.getElementName().getLocalName();
						parameterValue = parameter.getValue();
						if (parameterValue == null) {
							parameterValue = "";
						}
						
						if (variableList != null) {		// jmc 12/06/26 hide hidden variables in sequences
							String str = (String) Visibility.Logs.replaceVariables(variableList, "" + parameterName + "=\"" + parameterValue + "\"") ;
							Engine.logBeans.debug("   Parameter: " + str);
						}
						else
							Engine.logBeans.debug("   Parameter: " + parameterName + "=\"" + parameterValue + "\"");
						
						// Handle convertigo parameters
						if (parameterName.startsWith("__")) {
							webServiceServletRequester.handleParameter(parameterName, parameterValue);
						}
						
						// Common parameter handling
						if (inputDocumentBuilder.handleSpecialParameter(parameterName, parameterValue)) {
							// handled
						}
						// Compatibility for Convertigo 2.x
						else if (parameterName.equals("context")) {
							// Just ignore it
						}
						else {
							SOAPElement soapArrayElement = null;
							Iterator<?> iterator3;

							String href = parameter.getAttributeValue(se.createName("href"));
							
							String arrayType = parameter.getAttributeValue(se.createName("soapenc:arrayType"));
							if (arrayType == null) {
								iterator3 = parameter.getAllAttributes();
								while (iterator3.hasNext()) {
									element = iterator3.next();
									if (element instanceof Name) {
										String s = ((Name) element).getQualifiedName();
										if (s.equals("soapenc:arrayType")) {
											arrayType = s;
											break;
										}
									}
								}
							}

							// Array (Microsoft .net)
							if (href != null) {
								Engine.logBeans.debug("Deserializing Microsoft .net array");
								iterator3 = sb.getChildElements();
								while (iterator3.hasNext()) {
									element = iterator3.next();
									if (element instanceof SOAPElement) {
										soapArrayElement = (SOAPElement) element;
										String elementId = soapArrayElement.getAttributeValue(se.createName("id"));
										if (elementId != null) {
											if (href.equals("#" + elementId)) {
												iterator3 = soapArrayElement.getChildElements();
												while (iterator3.hasNext()) {
													element = iterator3.next();
													if (element instanceof SOAPElement) {
														break;
													}
												}
												break;
											}
										}
									}
								}
								
								// Find the element with href id
								iterator3 = sb.getChildElements();
								while (iterator3.hasNext()) {
									element = iterator3.next();
									if (element instanceof SOAPElement) {
										soapArrayElement = (SOAPElement) element;
										String elementId = soapArrayElement.getAttributeValue(se.createName("id"));
										if (elementId != null) {
											if (href.equals("#" + elementId)) {
												break;
											}
										}
									}
								}
							}
							// Array (Java/Axis)
							else if (arrayType != null) {
								Engine.logBeans.debug("Deserializing Java/Axis array");
								soapArrayElement = parameter;
							}
							// If the node has children nodes, we assume it is an array.
							else if (parameter.getChildElements().hasNext()) {
								if (isSoapArray((IVariableContainer) requestable, parameterName)) {
									Engine.logBeans.debug("Deserializing array");
									soapArrayElement = parameter;
								}
							}

							// Deserializing array
							if (soapArrayElement != null) {
								iterator3 = soapArrayElement.getChildElements();
								while (iterator3.hasNext()) {
									element = iterator3.next();
									if (element instanceof SOAPElement) {
										soapArrayElement = (SOAPElement) element;
										parameterValue = soapArrayElement.getValue();
										if (parameterValue == null) parameterValue = "";
										handleSimpleVariable(context.inputDocument, soapArrayElement, parameterName, parameterValue, inputDocumentBuilder.transactionVariablesElement);
									}
								}
							}
							// Deserializing simple variable
							else {
								handleSimpleVariable(context.inputDocument, parameter, parameterName, parameterValue, inputDocumentBuilder.transactionVariablesElement);
							}
						}
					}
				}
				
				if (Engine.logBeans.isDebugEnabled()) {
					String soapMessage = SOAPUtils.toString(requestMessage, request.getCharacterEncoding());
					
					if (requestable instanceof TransactionWithVariables)
						Engine.logBeans.debug("[WebServiceTranslator] SOAP message received:\n" + Visibility.Logs.replaceVariables(((TransactionWithVariables)(requestable)).getVariablesList(), request));
					else
					if (requestable instanceof Sequence)
						Engine.logBeans.debug("[WebServiceTranslator] SOAP message received:\n" + Visibility.Logs.replaceVariables(((Sequence)(requestable)).getVariablesList(), request));
					else
						Engine.logBeans.debug("[WebServiceTranslator] SOAP message received:\n" + soapMessage);
				}
				
				break;
			}
		}

		Engine.logBeans.debug("[WebServiceTranslator] SOAP message analyzed");

		Engine.logBeans.debug("[WebServiceTranslator] Input document created");
    }
    
	private void handleSimpleVariable(Document inputDocument, SOAPElement parameter, String parameterName, String parameterValue, Element transactionVariablesElement) {	
		Element item = inputDocument.createElement("variable");
		item.setAttribute("name", parameterName);
		
		// Structured value?
		if (parameter.hasChildNodes() && (parameter.getChildNodes().getLength() > 1 || parameter.getFirstChild().getNodeType() == Node.ELEMENT_NODE)) {
			appendNodes(parameter.getChildNodes(), item);
			Engine.logBeans.debug("   Adding structured requestable variable '" + parameterName + "'");
		}
		else {
			item.setAttribute("value", parameterValue);
			Engine.logBeans.debug("   Adding requestable variable '" + parameterName + "' = '" + Visibility.maskValue(parameterValue) +"'");
		}
		transactionVariablesElement.appendChild(item);
	}
	
	private void appendNodes(NodeList nodes, Node destination) {
		int nodesLen = nodes.getLength();
		
		for (int i = 0; i < nodesLen; i++) {
			Node node = nodes.item(i);
			copyNode(node, destination);
		}
	}
	
	private void copyNode(Node sourceNode, Node destinationNode) {
		Document destinationDoc = destinationNode.getOwnerDocument();

		switch (sourceNode.getNodeType()) {
		case Node.TEXT_NODE:
			Text text = destinationDoc.createTextNode(sourceNode.getNodeValue());
			destinationNode.appendChild(text);
			break;
		case Node.ELEMENT_NODE:
			Element element = destinationDoc.createElement(sourceNode.getNodeName());
			destinationNode.appendChild(element);
			
			element.setTextContent(sourceNode.getNodeValue());
			
			// Copy attributes
			NamedNodeMap attributes = sourceNode.getAttributes();
			int nbAttributes = attributes.getLength();
			
			for (int i = 0; i < nbAttributes; i++) {
				Node attribute = attributes.item(i);
				element.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
			}
			
			// Copy children nodes
			NodeList children = sourceNode.getChildNodes();
			int nbChildren = children.getLength();
			for (int i = 0; i < nbChildren; i++) {
				Node child = children.item(i);
				copyNode(child, element);
			}
		}
	}

    private boolean isSoapArray(IVariableContainer requestable, String parameterName) {
		int len = requestable.numberOfVariables();
		for (int j = 0 ; j < len ; j++) {
			RequestableVariable variable = (RequestableVariable) requestable.getVariable(parameterName);
			if (variable != null) {
				return variable.isSoapArray();
			}
		}
		return false;
    }

    /* Old builOutputData method */
	public Object __buildOutputData(Context context, Object convertigoResponse) throws Exception {
        Engine.logBeans.debug("[WebServiceTranslator] Encoding the SOAP response...");
        
        SOAPMessage responseMessage = null;
        String sResponseMessage = "";
		String encodingCharSet = "UTF-8";
		if (context.requestedObject != null)
			encodingCharSet = context.requestedObject.getEncodingCharSet();
        
        if (convertigoResponse instanceof Document) {
			Engine.logBeans.debug("[WebServiceTranslator] The Convertigo response is a XML document.");
            Document document = (Document) convertigoResponse;

    		//MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            MessageFactory messageFactory = MessageFactory.newInstance();
            responseMessage = messageFactory.createMessage();
            
            responseMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, encodingCharSet);

            SOAPPart sp = responseMessage.getSOAPPart();
            SOAPEnvelope se = sp.getEnvelope();
            SOAPBody sb = se.getBody();

            sb.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");

    		String targetNamespace = context.project.getTargetNamespace();
    		String prefix = getPrefix(context.projectName, targetNamespace);
        	
        	//se.addNamespaceDeclaration(prefix, targetNameSpace);
            se.addNamespaceDeclaration("soapenc", "http://schemas.xmlsoap.org/soap/encoding/");
            se.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            se.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
            
            // Remove header as it not used. Seems that empty headers causes the WS client of Flex 4 to fail 
            se.getHeader().detachNode();
            
            // Add the method response element
            SOAPElement soapMethodResponseElement = null;
            String soapElementName = context.sequenceName != null ? context.sequenceName:context.connectorName + "__" +context.transactionName;
            soapElementName += "Response";
            
			soapMethodResponseElement = sb.addChildElement(se.createName(soapElementName, prefix, targetNamespace));

        	if (Project.XSD_FORM_QUALIFIED.equals(context.project.getSchemaElementForm())) {
            	soapMethodResponseElement.addAttribute(se.createName("xmlns"), targetNamespace);
            }
            
            // Add a 'response' root child element or not
            SOAPElement soapResponseElement;
            if (context.sequenceName != null) {
            	Sequence sequence = (Sequence) context.requestedObject;
            	if (sequence.isIncludeResponseElement()) {
                    soapResponseElement = soapMethodResponseElement.addChildElement("response");
            	}
            	else {
                    soapResponseElement = soapMethodResponseElement;
            	}
            }
            else {
            	soapResponseElement = soapMethodResponseElement.addChildElement("response");
            }
            
            if (soapResponseElement.getLocalName().equals("response")) {
        		if (document.getDocumentElement().hasAttributes()) {
        			addAttributes(responseMessage, se, context, document.getDocumentElement().getAttributes(), soapResponseElement);
        		}
            }
            
            NodeList childNodes = document.getDocumentElement().getChildNodes();
            int len = childNodes.getLength();
			org.w3c.dom.Node node;
            for (int i = 0 ; i < len ; i++) {
            	node = childNodes.item(i);
            	if (node instanceof Element) {
    				addElement(responseMessage, se, context, (Element) node, soapResponseElement);
            	}
            }

    		sResponseMessage = SOAPUtils.toString(responseMessage, encodingCharSet);

            // Correct missing "xmlns" (Bug AXA POC client .NET)
    		//sResponseMessage = sResponseMessage.replaceAll("<soapenv:Envelope", "<soapenv:Envelope xmlns=\""+targetNameSpace+"\"");
        }
        else {
			Engine.logBeans.debug("[WebServiceTranslator] The Convertigo response is not a XML document.");
        	sResponseMessage = convertigoResponse.toString();
        }
        
        if (Engine.logBeans.isDebugEnabled()) {
			Engine.logBeans.debug("[WebServiceTranslator] SOAP response:\n" + sResponseMessage);
        }
        
        return responseMessage == null ? sResponseMessage.getBytes(encodingCharSet) : responseMessage; 
	}

	private SOAPElement addSoapElement(Context context, SOAPEnvelope se, SOAPElement soapParent, Node node) throws Exception {
		String prefix = node.getPrefix();
		String namespace = node.getNamespaceURI();
		String nodeName = node.getNodeName();
		String localName = node.getLocalName();
		String value = node.getNodeValue();
		
		boolean includeResponseElement = true;
		if (context.sequenceName != null) {
			includeResponseElement = ((Sequence) context.requestedObject).isIncludeResponseElement();
		}
		
		SOAPElement soapElement = null;
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			
			boolean toAdd = true;
			if (!includeResponseElement && localName.equalsIgnoreCase("response")) {
				toAdd = false;
			}
			if ("http://schemas.xmlsoap.org/soap/envelope/".equals(element.getParentNode().getNamespaceURI()) ||
				"http://schemas.xmlsoap.org/soap/envelope/".equals(namespace) ||
				nodeName.toLowerCase().endsWith(":envelope") ||
				nodeName.toLowerCase().endsWith(":body")
				//element.getParentNode().getNodeName().toUpperCase().indexOf("NS0:") != -1 ||
				//nodeName.toUpperCase().indexOf("NS0:") != -1
				)
			{
				toAdd = false;
			}
			
			if (toAdd) {
				if (prefix == null || prefix.equals("")) {
					soapElement = soapParent.addChildElement(nodeName);
				}
				else {
					soapElement = soapParent.addChildElement(se.createName(localName, prefix, namespace));
				}
			}
			else {
				soapElement = soapParent;
			}
			
			if (soapElement != null) {
				if (soapParent.equals(se.getBody()) && !soapParent.equals(soapElement)) {
		        	if (Project.XSD_FORM_QUALIFIED.equals(context.project.getSchemaElementForm())) {
		        		if (soapElement.getAttribute("xmlns") == null) {
		        			soapElement.addAttribute(se.createName("xmlns"), context.project.getTargetNamespace());
		        		}
		        	}
				}
				
				if (element.hasAttributes()) {
					String attrType = element.getAttribute("type");
	            	if (("attachment").equals(attrType)) {
	    				if (context.requestedObject instanceof AbstractHttpTransaction){
	    					AttachmentDetails attachment = AttachmentManager.getAttachment(element);
	    					if (attachment != null){
	    						byte[] raw = attachment.getData();
	    						if(raw != null) soapElement.addTextNode(Base64v21.encodeBytes(raw));
	    					}
	    					
	    					/* DON'T WORK YET *\
	    					AttachmentPart ap = responseMessage.createAttachmentPart(new ByteArrayInputStream(raw), element.getAttribute("content-type"));
	    					ap.setContentId(key);
	    					ap.setContentLocation(element.getAttribute("url"));
	    					responseMessage.addAttachmentPart(ap);
	    					\* DON'T WORK YET */
	    				}
	    			}
					
					NamedNodeMap attributes = element.getAttributes();
		            int len = attributes.getLength();
		            for (int i = 0 ; i < len ; i++) {
		            	Node item = attributes.item(i);
		            	addSoapElement(context, se, soapElement, item);
		            }
				}
				
				if (element.hasChildNodes()) {
		            NodeList childNodes = element.getChildNodes();
		            int len = childNodes.getLength();
		            for (int i = 0 ; i < len ; i++) {
		            	Node item = childNodes.item(i);
						switch (item.getNodeType()) {
							case Node.ELEMENT_NODE:
								addSoapElement(context, se, soapElement, item);
								break;
							case Node.CDATA_SECTION_NODE:
							case Node.TEXT_NODE:
								String text = item.getNodeValue();
								text = (text == null) ? "":text;
								soapElement.addTextNode(text);
								break;
							default:
								break;
						}
		            }
				}
			}
		}
		else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			if (prefix == null || prefix.equals("")) {
				soapElement = soapParent.addAttribute(se.createName(nodeName), value);
			}
			else {
				soapElement = soapParent.addAttribute(se.createName(localName, prefix, namespace), value);
			}
		}
		return soapElement;
	}
	
	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
        Engine.logBeans.debug("[WebServiceTranslator] Encoding the SOAP response...");
        
        SOAPMessage responseMessage = null;
        String sResponseMessage = "";
		String encodingCharSet = "UTF-8";
		if (context.requestedObject != null)
			encodingCharSet = context.requestedObject.getEncodingCharSet();
        
        if (convertigoResponse instanceof Document) {
			Engine.logBeans.debug("[WebServiceTranslator] The Convertigo response is a XML document.");
            Document document = Engine.theApp.schemaManager.makeResponse((Document) convertigoResponse);
            
    		//MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            MessageFactory messageFactory = MessageFactory.newInstance();
            responseMessage = messageFactory.createMessage();
            
            responseMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, encodingCharSet);

            SOAPPart sp = responseMessage.getSOAPPart();
            SOAPEnvelope se = sp.getEnvelope();
            SOAPBody sb = se.getBody();

            sb.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");

        	//se.addNamespaceDeclaration(prefix, targetNameSpace);
            se.addNamespaceDeclaration("soapenc", "http://schemas.xmlsoap.org/soap/encoding/");
            se.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            se.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
            
            // Remove header as it not used. Seems that empty headers causes the WS client of Flex 4 to fail 
            se.getHeader().detachNode();
            
            addSoapElement(context, se, sb, document.getDocumentElement());
            
    		sResponseMessage = SOAPUtils.toString(responseMessage, encodingCharSet);

            // Correct missing "xmlns" (Bug AXA POC client .NET)
    		//sResponseMessage = sResponseMessage.replaceAll("<soapenv:Envelope", "<soapenv:Envelope xmlns=\""+targetNameSpace+"\"");
        }
        else {
			Engine.logBeans.debug("[WebServiceTranslator] The Convertigo response is not a XML document.");
        	sResponseMessage = convertigoResponse.toString();
        }
        
        if (Engine.logBeans.isDebugEnabled()) {
			Engine.logBeans.debug("[WebServiceTranslator] SOAP response:\n" + sResponseMessage);
        }
        
        return responseMessage == null ? sResponseMessage.getBytes(encodingCharSet) : responseMessage; 
	}

	private void addAttributes(SOAPMessage responseMessage, SOAPEnvelope soapEnvelope, Context context, NamedNodeMap attributes, SOAPElement soapElement) throws SOAPException {
		SOAPElement soapMethodResponseElement = (SOAPElement)soapEnvelope.getBody().getFirstChild();
		String targetNamespace = soapMethodResponseElement.getNamespaceURI();
		String prefix = soapMethodResponseElement.getPrefix();
		
		int len = attributes.getLength();
		Attr attribute;
		for (int i = 0 ; i < len ; i++) {
			attribute = (Attr) attributes.item(i);
			String attributeName = attribute.getName();
			String attributeValue = attribute.getNodeValue();
			String attributeNsUri = attribute.getNamespaceURI();
			String attributePrefix = getPrefix(context.projectName, attributeNsUri);
			
			XmlSchemaAttribute xmlSchemaAttribute = getXmlSchemaAttributeByName(context.projectName, attributeName);
			boolean isGlobal = xmlSchemaAttribute != null;
			if (isGlobal) {
				attributeNsUri = xmlSchemaAttribute.getQName().getNamespaceURI();
				attributePrefix = getPrefix(context.projectName, attributeNsUri);
			}
			
			if (Project.XSD_FORM_QUALIFIED.equals(context.project.getSchemaElementForm()) || isGlobal) {
				if (attributePrefix == null) {
					soapElement.addAttribute(
							soapEnvelope.createName(attributeName, prefix, targetNamespace),
							attributeValue);
				}
				else {
					soapElement.addAttribute(
							soapEnvelope.createName(attributeName, attributePrefix, attributeNsUri),
							attributeValue);
				}
			}
			else {
				soapElement.addAttribute(soapEnvelope.createName(attributeName), attributeValue);
			}
		}
	}
	
	private void addElement(SOAPMessage responseMessage, SOAPEnvelope soapEnvelope, Context context, Element elementToAdd, SOAPElement soapElement) throws SOAPException {
		SOAPElement soapMethodResponseElement = (SOAPElement)soapEnvelope.getBody().getFirstChild();
		String targetNamespace = soapMethodResponseElement.getNamespaceURI();
		String prefix = soapMethodResponseElement.getPrefix();
    	
		String nodeType = elementToAdd.getAttribute("type");
		SOAPElement childSoapElement = soapElement;
		
		boolean elementAdded = true;
		boolean bTable = false;
			
		if (nodeType.equals("table")) {
			bTable = true;
			/*childSoapElement = soapElement.addChildElement("ArrayOf" + context.transactionName + "_" + tableName + "_Row", "");

            if (!context.httpServletRequest.getServletPath().endsWith(".wsl")) {
            	childSoapElement.addAttribute(soapEnvelope.createName("xsi:type"), "soapenc:Array");
            }*/
			childSoapElement = soapElement.addChildElement(elementToAdd.getNodeName());
		}
		else if (nodeType.equals("row")) {
			/*String elementType = context.transactionName + "_" + tableName + "_Row";
			childSoapElement = soapElement.addChildElement(elementType, "");*/
			childSoapElement = soapElement.addChildElement(elementToAdd.getNodeName());
		}
		else if (nodeType.equals("attachment")){
			childSoapElement = soapElement.addChildElement(elementToAdd.getNodeName());
			
			if(context.requestedObject instanceof AbstractHttpTransaction){
				AttachmentDetails attachment = AttachmentManager.getAttachment(elementToAdd);
				if(attachment!=null){
					byte[] raw = attachment.getData();
					if(raw != null) childSoapElement.addTextNode(Base64v21.encodeBytes(raw));
				}
				
				/* DON'T WORK YET *\
				AttachmentPart ap = responseMessage.createAttachmentPart(new ByteArrayInputStream(raw), elementToAdd.getAttribute("content-type"));
				ap.setContentId(key);
				ap.setContentLocation(elementToAdd.getAttribute("url"));
				responseMessage.addAttachmentPart(ap);
				\* DON'T WORK YET */
			}
		}
		else {
			String elementNodeName = elementToAdd.getNodeName();
			String elementNodeNsUri = elementToAdd.getNamespaceURI();
			String elementNodePrefix = getPrefix(context.projectName, elementNodeNsUri);
			
			XmlSchemaElement xmlSchemaElement = getXmlSchemaElementByName(context.projectName, elementNodeName);
			boolean isGlobal =  xmlSchemaElement != null;
			if (isGlobal) {
				elementNodeNsUri = xmlSchemaElement.getQName().getNamespaceURI();
				elementNodePrefix = getPrefix(context.projectName, elementNodeNsUri);
			}
			
			// ignore original SOAP message response elements
//			if ((elementNodeName.toUpperCase().indexOf("SOAP-ENV:") != -1) || ((elementToAdd.getParentNode().getNodeName().toUpperCase().indexOf("SOAP-ENV:") != -1)) ||
//				(elementNodeName.toUpperCase().indexOf("SOAPENV:") != -1) || ((elementToAdd.getParentNode().getNodeName().toUpperCase().indexOf("SOAPENV:") != -1)) ||
//				(elementNodeName.toUpperCase().indexOf("NS0:") != -1) || ((elementToAdd.getParentNode().getNodeName().toUpperCase().indexOf("NS0:") != -1))) {
//				elementAdded = false;
//			}
			if ("http://schemas.xmlsoap.org/soap/envelope/".equals(elementToAdd.getNamespaceURI()) ||
				"http://schemas.xmlsoap.org/soap/envelope/".equals(elementToAdd.getParentNode().getNamespaceURI()) ||
				elementToAdd.getParentNode().getNodeName().toUpperCase().indexOf("NS0:") != -1 ||
				elementNodeName.toUpperCase().indexOf("NS0:") != -1) {
				elementAdded = false;
			}
			else {
				if (Project.XSD_FORM_QUALIFIED.equals(context.project.getSchemaElementForm()) || isGlobal) {
					if (elementNodePrefix == null) {
						childSoapElement = soapElement.addChildElement(soapEnvelope.createName(elementNodeName, prefix, targetNamespace));
					}
					else {
						childSoapElement = soapElement.addChildElement(soapEnvelope.createName(elementNodeName, elementNodePrefix, elementNodeNsUri));
					}
				}
				else {
					childSoapElement = soapElement.addChildElement(elementNodeName);
				}
			}
		}
		
		if (elementAdded && elementToAdd.hasAttributes()) {
			addAttributes(responseMessage, soapEnvelope, context, elementToAdd.getAttributes(), childSoapElement);
		}

		if (elementToAdd.hasChildNodes()) {
			NodeList childNodes = elementToAdd.getChildNodes();
			int len = childNodes.getLength();
			
			if (bTable) {
	            /*if (!context.httpServletRequest.getServletPath().endsWith(".wsl")) {
	            	childSoapElement.addAttribute(soapEnvelope.createName("soapenc:arrayType"), context.projectName+"_ns:" + context.transactionName + "_" + tableName + "_Row[" + (len - 1) + "]");
	            }*/
			}
			
			org.w3c.dom.Node node;
			Element childElement;
			for (int i = 0 ; i < len ; i++) {
				node = childNodes.item(i);
				switch (node.getNodeType()) {
					case org.w3c.dom.Node.ELEMENT_NODE:
						childElement = (Element) node;
						addElement(responseMessage, soapEnvelope, context, childElement, childSoapElement);
						break;
					case org.w3c.dom.Node.CDATA_SECTION_NODE:
					case org.w3c.dom.Node.TEXT_NODE:
						String text = node.getNodeValue();
						text = (text == null) ? "":text;
						childSoapElement.addTextNode(text);
						break;
					default:
						break;
				}
			}

			/*org.w3c.dom.Node node;
			Element childElement;
			for (int i = 0 ; i < len ; i++) {
				node = childNodes.item(i);
				if (node instanceof Element) {
					childElement = (Element) node;
					addElement(responseMessage, soapEnvelope, context, childElement, childSoapElement);
				}
				else if (node instanceof CDATASection) {
					Node textNode = XMLUtils.findChildNode(elementToAdd, org.w3c.dom.Node.CDATA_SECTION_NODE);
					String text = textNode.getNodeValue();
					if (text == null) {
						text = "";
					}
					childSoapElement.addTextNode(text);
				}
				else {
					Node textNode = XMLUtils.findChildNode(elementToAdd, org.w3c.dom.Node.TEXT_NODE);
					if (textNode != null) {
						String text = textNode.getNodeValue();
						if (text == null) {
							text = "";
						}
						childSoapElement.addTextNode(text);
					}
				}
			}*/
		}
    } 

	public String getContextName(byte[] data) throws Exception {
		throw new EngineException("The WebServiceTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The WebServiceTranslator translator does not support the getProjectName() method");
	}

	private static XmlSchemaElement getXmlSchemaElementByName(String projectName, String name) {
		XmlSchemaElement xmlSchemaElement = null;
		XmlSchemaCollection xmlSchemaCollection;
		try {
			xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName);
			for (XmlSchema xmlSchema : xmlSchemaCollection.getXmlSchemas()) {
				xmlSchemaElement = xmlSchema.getElementByName(name);
				if (xmlSchemaElement != null)
					return xmlSchemaElement;
			}
		} catch (Exception e) {}
		return null;
	}

	private static XmlSchemaAttribute getXmlSchemaAttributeByName(String projectName, String name) {
		XmlSchemaAttribute xmlSchemaAttribute = null;
		XmlSchemaCollection xmlSchemaCollection;
		try {
			xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName);
			for (XmlSchema xmlSchema : xmlSchemaCollection.getXmlSchemas()) {
				xmlSchemaAttribute = xmlSchema.getAttributeByName(new QName(xmlSchema.getTargetNamespace(), name));
				if (xmlSchemaAttribute != null)
					return xmlSchemaAttribute;
			}
		} catch (Exception e) {}
		return null;
	}
	
	private static String getPrefix(String projectName, String namespaceUri) {
		String prefix = "";
		try {
			prefix = Engine.theApp.schemaManager.getSchemasForProject(projectName).getNamespaceContext().getPrefix(namespaceUri);
		} catch (Exception e) {}
		return prefix;
	}
}