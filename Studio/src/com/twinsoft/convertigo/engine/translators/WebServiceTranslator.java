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

package com.twinsoft.convertigo.engine.translators;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.AttachmentManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.AttachmentManager.AttachmentDetails;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.requesters.WebServiceServletRequester;
import com.twinsoft.convertigo.engine.util.Base64v21;
import com.twinsoft.convertigo.engine.util.SOAPUtils;

public class WebServiceTranslator implements Translator {
	
	public void buildInputDocument(Context context, Object inputData) throws Exception {
        Engine.logBeans.debug("[WebServiceTranslator] Making input document");

        HttpServletRequest request = (HttpServletRequest) inputData;

		SOAPMessage requestMessage = (SOAPMessage) request.getAttribute(WebServiceServletRequester.REQUEST_MESSAGE_ATTRIBUTE);
		
		
		if (Engine.logBeans.isDebugEnabled()) {
			String soapMessage = SOAPUtils.toString(requestMessage.getSOAPPart(),"ISO-8859-1");
			Engine.logBeans.debug("[WebServiceTranslator] SOAP message received:\n" + soapMessage);
		}
		
		SOAPPart sp = requestMessage.getSOAPPart();
		SOAPEnvelope se = sp.getEnvelope();
		SOAPBody sb = se.getBody();

		Iterator<?> iterator = sb.getChildElements();
		SOAPElement method, parameter;
		Object element;
		String methodName;

		Element root = context.inputDocument.createElement("input");
		Element transactionVariablesElement = context.inputDocument.createElement("transaction-variables");

		context.inputDocument.appendChild(root);
		root.appendChild(transactionVariablesElement);
		
		Element item;

		while (iterator.hasNext()) {
			element = iterator.next();
			if (element instanceof SOAPElement) {
				method = (SOAPElement) element;

				methodName = method.getElementName().getLocalName();
				Engine.logBeans.debug("[WebServiceTranslator] Requested web service name: " + methodName);

				int i = methodName.indexOf("__");
				
				// for statefull transaction, don't replace the project
				if(context.project==null || !context.project.getName().equals(context.projectName)) context.project = Engine.theApp.databaseObjectsManager.getProjectByName(context.projectName);

				String connectorName = null;
				if (i == -1) {
					/*if (context.project != null) {
						String defaultConnectorName = context.project.getDefaultConnector().getName();
						if (!defaultConnectorName.equals(context.connectorName)) {
							context.isNewSession = true;
							context.connectorName = defaultConnectorName;
						}
					}
					else {
						context.connectorName = null;
					}
					context.transactionName = methodName;*/
					
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
				}
				else if (context.connectorName != null) {
					if (context.transactionName != null) {
						requestable = context.project.getConnectorByName(context.connectorName).getTransactionByName(context.transactionName);
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
						if (parameterValue == null) parameterValue = "";
						Engine.logBeans.debug("   Parameter: " + parameterName + "=\"" + parameterValue + "\"");

						if (Parameter.Context.getName().equalsIgnoreCase(parameterName)) {
							// Already handled!							
						}
						else if (Parameter.CariocaSesskey.getName().equalsIgnoreCase(parameterName)) {
							context.tasSessionKey = parameterValue;
							Engine.logBeans.debug("Carioca session key: " + context.tasSessionKey);
						}
						// This is the overidden service code
						else if (parameterName.equals(Parameter.CariocaService.getName())) {
							if ((context.tasServiceCode == null) || (!context.tasServiceCode.equalsIgnoreCase(parameterValue))) {
								context.isNewSession = true;
								context.tasServiceCode = parameterValue;
								Engine.logBeans.debug("The service code is overidden to \"" + parameterValue + "\".");
							}
						}
						// Carioca trusted request
						else if (parameterName.equals(Parameter.Carioca.getName())) {
							if ((parameterValue != null) && (parameterValue.length() > 0)) {
								context.isTrustedRequest = (parameterValue.equalsIgnoreCase("true") ? true : false);
								Engine.logBeans.debug("Is Carioca trusted request: " + parameterValue);
							}
						}
						else if (Parameter.CariocaUser.getName().equalsIgnoreCase(parameterName)) {
							context.tasUserName = parameterValue;
							Engine.logBeans.debug("Tas user name: " + context.tasUserName);
						}
						else if (Parameter.CariocaPassword.getName().equalsIgnoreCase(parameterName)) {
							context.tasUserPassword = parameterValue;
							Engine.logBeans.debug("Tas user password: " + context.tasUserPassword);
						}
						// VIC trusted request
						else if (parameterName.equals(Parameter.Vic.getName())) {
							if ((parameterValue != null) && (parameterValue.length() > 0)) {
								context.isTrustedRequest = (parameterValue.equalsIgnoreCase("true") ? true : false);
								Engine.logBeans.debug("Is VIC trusted request: " + parameterValue);
							}
						}
						// This is the VIC group
						else if (parameterName.equals(Parameter.VicGroup.getName())) {
							if ((parameterValue != null) && (parameterValue.length() > 0)) {
								context.isRequestFromVic = true;
								int index = parameterValue.indexOf('@');
								if (index == -1) {
									context.tasUserGroup = parameterValue;
									context.tasVirtualServerName = "";
								}
								else {
									context.tasUserGroup = parameterValue.substring(0, index);
									context.tasVirtualServerName = parameterValue.substring(index + 1);
								}
								Engine.logBeans.debug("The VIC group is \"" + context.tasUserGroup + "\".");
								Engine.logBeans.debug("The VIC virtual server is \"" + context.tasVirtualServerName + "\".");
							}
						}
						// This is the VIC service code
						else if (parameterName.equals(Parameter.VicServiceCode.getName())) {
							if ((parameterValue != null) && (parameterValue.length() > 0)) {
								context.isRequestFromVic = true;
								context.isNewSession = true;
								context.tasServiceCode = parameterValue;
								Engine.logBeans.debug("The VIC service code is \"" + parameterValue + "\".");
							}
						}
						// This is the VIC dte address
						else if (parameterName.equals(Parameter.VicDteAddress.getName())) {
							if ((parameterValue != null) && (parameterValue.length() > 0)) {
								context.isRequestFromVic = true;
								context.tasDteAddress = parameterValue;
								Engine.logBeans.debug("The VIC dte address is \"" + parameterValue + "\".");
							}
						}
						// This is the VIC comm device
						else if (parameterName.equals(Parameter.VicCommDevice.getName())) {
							if ((parameterValue != null) && (parameterValue.length() > 0)) {
								context.isRequestFromVic = true;
								context.tasCommDevice = parameterValue;
								Engine.logBeans.debug("The VIC comm device is \"" + parameterValue + "\".");
							}
						}
						else if (parameterName.startsWith(Parameter.NoCache.getName())) {
							context.noCache = (parameterValue.equalsIgnoreCase("true") ? true : false);
							Engine.logBeans.debug("Ignoring cache required: " + parameterValue);
						}
						else if (parameterName.startsWith(Parameter.Testcase.getName())) {
							item = context.inputDocument.createElement("variable");
							item.setAttribute("name", parameterName);
							item.setAttribute("value", parameterValue);
							Engine.logBeans.debug("   Adding test case = '" + parameterValue + "'");
							transactionVariablesElement.appendChild(item);
						}
						else if (parameterName.startsWith("__")) {
							Engine.logBeans.debug("Convertigo internal variable ignored! (not handled)");
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
								if (isMultivaluedParameter((IVariableContainer) requestable, parameterName)) {
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
										item = context.inputDocument.createElement("variable");
										item.setAttribute("name", parameterName);
										item.setAttribute("value", parameterValue);
										Engine.logBeans.debug("   Adding requestable variable '" + parameterName + "' = '" + Visibility.maskValue(parameterValue) +"'");
										transactionVariablesElement.appendChild(item);
									}
								}
							}
							// Deserializing simple variable
							else {
								item = context.inputDocument.createElement("variable");
								item.setAttribute("name", parameterName);
								item.setAttribute("value", parameterValue);
								Engine.logBeans.debug("   Adding requestable variable '" + parameterName + "' = '" + Visibility.maskValue(parameterValue) +"'");
								transactionVariablesElement.appendChild(item);
							}
						}
					}
				}
				
				break;
			}
		}

		Engine.logBeans.debug("[WebServiceTranslator] SOAP message analyzed");

		Engine.logBeans.debug("[WebServiceTranslator] Input document created");
    }
    
    private boolean isMultivaluedParameter(IVariableContainer requestable, String parameterName) {
		int len = requestable.numberOfVariables();
		RequestableVariable variable;
		for (int j = 0 ; j < len ; j++) {
			variable = (RequestableVariable)requestable.getVariable(parameterName);
			if (variable != null) {
				return variable.isMultiValued();
			}
		}
		return false;
    }

	public Object buildOutputData(Context context, Object convertigoResponse) throws Exception {
        Engine.logBeans.debug("[WebServiceTranslator] Encoding the SOAP response...");
        
        String sResponseMessage = "";
		String encodingCharSet = "UTF-8";
		if (context.requestedObject != null)
			encodingCharSet = context.requestedObject.getEncodingCharSet();
        
        if (convertigoResponse instanceof Document) {
			Engine.logBeans.debug("[WebServiceTranslator] The Convertigo response is a XML document.");
            Document document = (Document) convertigoResponse;

    		//MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage responseMessage = messageFactory.createMessage();

            SOAPPart sp = responseMessage.getSOAPPart();
            SOAPEnvelope se = sp.getEnvelope();
            SOAPBody sb = se.getBody();

            sb.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");

        	String targetNameSpace = context.project.getTargetNamespace();

        	se.addNamespaceDeclaration(context.projectName+"_ns", targetNameSpace);
            se.addNamespaceDeclaration("soapenc", "http://schemas.xmlsoap.org/soap/encoding/");
            se.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            se.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
            
            // remove header as it not used. Seems that empty headers causes the WS client of Flex 4 to fail 
            se.getHeader().detachNode();
            
            SOAPElement soapElement = null;
            if (context.sequenceName != null)
            	soapElement = sb.addChildElement(context.sequenceName + "Response");
            else
            	soapElement = sb.addChildElement(context.connectorName+ "__" +context.transactionName + "Response");
            
            if (context.httpServletRequest.getServletPath().endsWith(".wsl")) {
            	soapElement.addAttribute(se.createName("xmlns"), targetNameSpace);// don't work! TODO: correct
            }
            SOAPElement soapElementResponse = soapElement.addChildElement("response");
            /*if (!context.httpServletRequest.getServletPath().endsWith(".wsl")) {
            	soapElementResponse.addAttribute(se.createName("xsi:type"), "xsd:string");
            }*/
            
            NodeList childNodes = document.getDocumentElement().getChildNodes();
            int len = childNodes.getLength();
			org.w3c.dom.Node node;
            for (int i = 0 ; i < len ; i++) {
            	node = childNodes.item(i);
            	if (node instanceof Element) {
    				addElement(responseMessage, se, context, (Element) node, soapElementResponse);
            	}
            }

            //TODO: correct missing "xmlns" (Bug AXA POC client .NET)
    		sResponseMessage = SOAPUtils.toString(sp,encodingCharSet);
    		sResponseMessage = sResponseMessage.replaceAll("<soapenv:Envelope", "<soapenv:Envelope xmlns=\""+targetNameSpace+"\"");
    		
            if (Engine.logBeans.isDebugEnabled()) {
    			Engine.logBeans.debug("[WebServiceTranslator] SOAP response:\n" + sResponseMessage);
            }
        }
        else {
			Engine.logBeans.debug("[WebServiceTranslator] The Convertigo response is not a XML document.");
        	sResponseMessage = convertigoResponse.toString();
        }
        
        return sResponseMessage.getBytes(encodingCharSet);
	}

	private void addElement(SOAPMessage responseMessage, SOAPEnvelope soapEnvelope, Context context, Element elementToAdd, SOAPElement soapElement) throws SOAPException {
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
			
			if(context.requestedObject instanceof HttpTransaction){
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
			// ignore original SOAP message response elements
			if ((elementToAdd.getNodeName().toUpperCase().indexOf("SOAP-ENV:") != -1) || ((elementToAdd.getParentNode().getNodeName().toUpperCase().indexOf("SOAP-ENV:") != -1)) ||
				(elementToAdd.getNodeName().toUpperCase().indexOf("SOAPENV:") != -1) || ((elementToAdd.getParentNode().getNodeName().toUpperCase().indexOf("SOAPENV:") != -1)) ||
				(elementToAdd.getNodeName().toUpperCase().indexOf("NS0:") != -1) || ((elementToAdd.getParentNode().getNodeName().toUpperCase().indexOf("NS0:") != -1))) {
				elementAdded = false;
			}
			else {
				childSoapElement = soapElement.addChildElement(elementToAdd.getNodeName());
			}
		}
		
		if (elementAdded && elementToAdd.hasAttributes()) {
			NamedNodeMap attributes = elementToAdd.getAttributes();
			int len = attributes.getLength();
			Attr attribute;
			for (int i = 0 ; i < len ; i++) {
				attribute = (Attr) attributes.item(i);
				// TODO: delete attributes for InfoPath
				childSoapElement.addAttribute(soapEnvelope.createName(attribute.getNodeName()), attribute.getNodeValue());
			}
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

//    private void addElement(SOAPEnvelope soapEnvelope, Context context, Element elementToAdd, SOAPElement soapElement) throws SOAPException {
//    	String nodeType = elementToAdd.getAttribute("type");
//		SOAPElement childSoapElement;
//		SOAPElement arrayTypeElement = null;
//
//		if (nodeType.equals("table")) {
//			index++;
//			tableName = elementToAdd.getNodeName();
//			childSoapElement = soapElement.addChildElement("ArrayOf" + context.transactionName + "_" + tableName + "_Row", "");
//			childSoapElement.addAttribute(soapEnvelope.createName("href"), "#id" + index);
//			arrayTypeElement = soapElement.getParentElement().getParentElement().addChildElement("soapenc:Array", "");
//			arrayTypeElement.addAttribute(soapEnvelope.createName("id"), "id" + index);
//		}
//		else if (nodeType.equals("row")) {
//			String elementName = "tns:" + context.transactionName + "_" + tableName + "_Row";
//			childSoapElement = soapElement.getParentElement().getParentElement().addChildElement(elementName, "");
//			childSoapElement.addAttribute(soapEnvelope.createName("id"), "id" + index);
//			childSoapElement.addAttribute(soapEnvelope.createName("xsi:type"), elementName);
//		}
//		else {
//			childSoapElement = soapElement.addChildElement(elementToAdd.getNodeName());
//			childSoapElement.addAttribute(soapEnvelope.createName("xsi:type"), "xsd:string");
//		}
//		
////		if (elementToAdd.hasAttributes()) {
////			NamedNodeMap attributes = elementToAdd.getAttributes();
////			int len = attributes.getLength();
////			Attr attribute;
////			for (int i = 0 ; i < len ; i++) {
////				attribute = (Attr) attributes.item(i);
////				childSoapElement.addAttribute(soapEnvelope.createName(attribute.getNodeName()), attribute.getNodeValue());
////			}
////		}
//
//		if (elementToAdd.hasChildNodes()) {
//			NodeList childNodes = elementToAdd.getChildNodes();
//			int len = childNodes.getLength();
//			
//			if (arrayTypeElement != null) {
//				arrayTypeElement.addAttribute(soapEnvelope.createName("soapenc:arrayType"), "tns:" + context.transactionName + "_" + elementToAdd.getNodeName() + "_Row[" + (len - 1) + "]");
//			}
//
//			org.w3c.dom.Node node;
//			Element childElement;
//			for (int i = 0 ; i < len ; i++) {
//				node = childNodes.item(i);
//				if (node instanceof Element) {
//					childElement = (Element) node;
//					if (arrayTypeElement == null) {
//						addElement(soapEnvelope, context, childElement, childSoapElement);
//					}
//					else {
//						index++;
//						childSoapElement = arrayTypeElement.addChildElement("Item", "");
//						childSoapElement.addAttribute(soapEnvelope.createName("href"), "#id" + index);
//						addElement(soapEnvelope, context, childElement, soapElement);
//					}
//				}
//				else if (node instanceof CDATASection) {
//					Node textNode = XMLUtils.findChildNode(elementToAdd, org.w3c.dom.Node.CDATA_SECTION_NODE);
//					String text = textNode.getNodeValue();
//					if (text == null) {
//						text = "";
//					}
//					childSoapElement.addTextNode(text);
//				}
//				else {
//					Node textNode = XMLUtils.findChildNode(elementToAdd, org.w3c.dom.Node.TEXT_NODE);
//					if (textNode != null) {
//						String text = textNode.getNodeValue();
//						if (text == null) {
//							text = "";
//						}
//						childSoapElement.addTextNode(text);
//					}
//				}
//			}
//		}
//    } 

	public String getContextName(byte[] data) throws Exception {
		throw new EngineException("The WebServiceTranslator translator does not support the getContextName() method");
	}

	public String getProjectName(byte[] data) throws Exception {
		throw new EngineException("The WebServiceTranslator translator does not support the getProjectName() method");
	}

}