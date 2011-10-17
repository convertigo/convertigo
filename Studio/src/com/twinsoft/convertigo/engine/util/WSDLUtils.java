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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.wsdl.DefinitionImpl;
import com.ibm.wsdl.extensions.PopulatedExtensionRegistry;
import com.ibm.wsdl.extensions.schema.SchemaConstants;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPOperationImpl;
import com.ibm.wsdl.util.xml.DOM2Writer;
import com.ibm.wsdl.xml.WSDLReaderImpl;
import com.ibm.wsdl.xml.WSDLWriterImpl;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class WSDLUtils {

	public static final String WSDL_STYLE_ALL = "ALL";
	public static final String WSDL_STYLE_DOC = "DOC/LITERAL";
	public static final String WSDL_STYLE_RPC = "RPC";
	
	protected String applicationServerUrl;
	
	protected WSDLUtils(){
		applicationServerUrl = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
	}
	
	public static WSDL getWSDL(String wsdlURI) throws WSDLException {
		WSDL wsdl = new WSDLUtils(). new WSDL(wsdlURI);
		return wsdl;
	}
	
	public static WSDL createWSDL(String projectName, String wsdlURI) throws Exception {
		return createWSDL(projectName, wsdlURI, WSDL_STYLE_ALL);
	}
	
	public static WSDL createWSDL(String projectName, String wsdlURI, String wsdlStyle) throws Exception {
		WSDL wsdl = new WSDLUtils(). new WSDL();
		wsdl.create(projectName, wsdlURI, wsdlStyle);
		return wsdl;
	}
	
	public class WSDL {
		private String wsdlURI = null;
		private Definition definition = null;
		private boolean bModified = false;
		private String wsdlStyle = WSDL_STYLE_DOC;
		
		protected WSDL() {
			
		}
		
		protected WSDL(String wsdlURI) throws WSDLException {
			this.wsdlURI = wsdlURI;
			this.wsdlStyle = WSDL_STYLE_ALL;
			this.definition = new WSDLReaderImpl().readWSDL(wsdlURI);
			
			String targetNamespace = definition.getTargetNamespace();
			String projectName = targetNamespace.replaceFirst("http://www.convertigo.com/convertigo/projects/", "");
			Service service = definition.getService(new QName(targetNamespace, projectName));
			if (service != null) {
				try {
					Port portDoc = getPort(service, projectName + "SOAP");
					Port portRpc = getPort(service, projectName + "SOAP_RPC");
					if ((portDoc != null) && (portRpc == null)) this.wsdlStyle = WSDL_STYLE_DOC;
					if ((portDoc == null) && (portRpc != null)) this.wsdlStyle = WSDL_STYLE_RPC;
				} catch (Exception e) {
				}
			}
		}
		
		protected void debugWsdlSchemas(Definition definition) {
			Types types = definition.getTypes();
			List<?> list = types.getExtensibilityElements();
			Iterator<?> iterator = list.iterator();
			while (iterator.hasNext()) {
				ExtensibilityElement extensibilityElement = (ExtensibilityElement)iterator.next();
				if (extensibilityElement instanceof UnknownExtensibilityElement) {
					Element element = ((UnknownExtensibilityElement)extensibilityElement).getElement();
					StringWriter sw = new StringWriter();
					DOM2Writer.serializeAsXML(element, sw);
					System.out.println("\nDebug ExtensibilityElement :");
					System.out.println(sw.toString());
					
					if (element.getTagName().indexOf(":schema") != -1) {
						XmlSchemaCollection schemaCol = new XmlSchemaCollection();
						schemaCol.setBaseUri(definition.getDocumentBaseURI());
						schemaCol.read(element);
						
						HashMap<String, String> options = new HashMap<String, String>();
						options.put(OutputKeys.OMIT_XML_DECLARATION, "no");
						options.put(OutputKeys.INDENT, "yes");

						XmlSchema[] schemas = schemaCol.getXmlSchemas();
						for (int i=0; i<schemas.length; i++) {
							XmlSchema xmlSchema = schemas[i];
							try {
								System.out.println("\nDebug XMLSchema "+ xmlSchema.toString() +":");
								xmlSchema.write(System.out, options);
							}
							catch (Exception e) {
								Engine.logEngine.error("Unexpected exception", e);
							}
						}
					}
				}
			}
		}
		
		public String getWsdlUri() {
			return wsdlURI;
		}
		
		public Definition getDefinition() {
			return definition;
		}
		
		public void addNamespaceDeclaration(String prefix, String ns) {
			definition.addNamespace(prefix, ns);
			bModified = true;
		}
		
		public void save() throws WSDLException, IOException {
			if (bModified) {
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(wsdlURI);
					WSDLWriterImpl wsdlWriter = new WSDLWriterImpl();
					wsdlWriter.writeWSDL(definition, fos);
				}
				finally {
					if (fos != null)
						fos.close();
					bModified = false;
				}
			}
		}
		
		public void addOperation(String projectName, String operationName, String operationComment) throws Exception {
			removeOperation(projectName, operationName);
			
			if ((projectName != null) && (!projectName.equals(""))) {
				
				if ((operationName == null) || (operationName.equals("")))
					throw new Exception("operationName parameter is invalid");
				
				String targetNamespace = definition.getTargetNamespace();
				
				// Checks service exist
				QName serviceQName = new QName(targetNamespace, projectName);
				Service service = getService(serviceQName);
				if (service == null) {
					throw new Exception("Corrupted file : missing service definition");
				}
				
				if ((wsdlStyle == WSDL_STYLE_ALL) || (wsdlStyle == WSDL_STYLE_DOC))
					addDocOperation(service, projectName, operationName, operationComment);
				if ((wsdlStyle == WSDL_STYLE_ALL) || (wsdlStyle == WSDL_STYLE_RPC))
					addRpcOperation(service, projectName, operationName, operationComment);
			}
			else {
				throw new Exception("projectName parameter is invalid");
			}
		}
		
		private void addDocOperation(Service service, String projectName, String operationName, String operationComment) throws Exception {
			String targetNamespace = definition.getTargetNamespace();
			
			String portName = projectName + "SOAP";
			Port port = getPort(service, portName);
			if (port == null) {
				throw new Exception("Corrupted file : missing port definition");
			}
			
			// Adds messages
			QName requestQName = new QName(targetNamespace, operationName + "Request");
			QName partElementRequestQName = new QName(targetNamespace, operationName);
			addDocMessage(requestQName, partElementRequestQName);

			QName responseQName = new QName(targetNamespace, operationName + "Response");
			QName partElementResponseQName = new QName(targetNamespace, operationName + "Response");
			addDocMessage(responseQName, partElementResponseQName);
			
			// Adds portType operation
			QName portTypeQName = new QName(targetNamespace, projectName + "PortType");
			PortType portType = getPortType(portTypeQName);
			Operation operation = addDocPortTypeOperation(portType, operationName, requestQName, responseQName);
			operation.setDocumentationElement(createDocumentationElement(operationComment));
			
			// Adds binding operation
			QName bindingQName = new QName(targetNamespace, projectName + "SOAPBinding");
			Binding binding = getBinding(bindingQName);
			BindingOperation bindingOperation = addDocBindingOperation(binding, projectName, operationName);
			bindingOperation.setDocumentationElement(createDocumentationElement(operationComment));
		}
		
		private void addRpcOperation(Service service, String projectName, String operationName, String operationComment) throws Exception {
			String targetNamespace = definition.getTargetNamespace();
			
			String portName = projectName + "SOAP_RPC";
			Port port = getPort(service, portName);
			if (port == null) {
				throw new Exception("Corrupted file : missing port definition");
			}
			
			// Adds messages
			QName requestQName = new QName(targetNamespace, operationName + "Request_RPC");
			QName partElementRequestQName = new QName(targetNamespace, operationName+ "RequestData");
			addRpcMessage(requestQName, partElementRequestQName);

			QName responseQName = new QName(targetNamespace, operationName + "Response_RPC");
			QName partElementResponseQName = new QName(targetNamespace, operationName + "ResponseData");
			addRpcMessage(responseQName, partElementResponseQName);
			
			// Adds portType operation
			QName portTypeQName = new QName(targetNamespace, projectName + "PortType_RPC");
			PortType portType = getPortType(portTypeQName);
			Operation operation = addRpcPortTypeOperation(portType, operationName, requestQName, responseQName);
			operation.setDocumentationElement(createDocumentationElement(operationComment));
			
			// Adds binding operation
			QName bindingQName = new QName(targetNamespace, projectName + "SOAPBinding_RPC");
			Binding binding = getBinding(bindingQName);
			BindingOperation bindingOperation = addRpcBindingOperation(binding, projectName, operationName);
			bindingOperation.setDocumentationElement(createDocumentationElement(operationComment));
		}

		public void updateRequestMessage(String projectName, String operationName, String xsdTypes) throws Exception {
			if ((projectName != null) && (!projectName.equals(""))) {
				
				if ((operationName == null) || (operationName.equals("")))
					throw new Exception("operationName parameter is invalid");
				
				String targetNamespace = definition.getTargetNamespace();
				
				// Update Request RPC message
				if ((wsdlStyle == WSDL_STYLE_ALL) || (wsdlStyle == WSDL_STYLE_RPC)) {
					QName requestQName = new QName(targetNamespace, operationName + "Request_RPC");
					QName partElementRequestQName = new QName(targetNamespace, operationName+ "RequestData");
					removeMessage(requestQName);
					addRpcMessage(requestQName, partElementRequestQName, xsdTypes);
				}
			}
			else {
				throw new Exception("projectName parameter is invalid");
			}
		}
		
		public void removeOperation(String projectName, String operationName) throws Exception {
			if ((projectName != null) && (!projectName.equals(""))) {
				
				if ((operationName == null) || (operationName.equals("")))
					throw new Exception("operationName parameter is invalid");
				
				String targetNamespace = definition.getTargetNamespace();
				
				// Checks service exist
				QName serviceQName = new QName(targetNamespace, projectName);
				Service service = getService(serviceQName);
				if (service == null) {
					throw new Exception("Corrupted file : missing service definition");
				}
				
				if ((wsdlStyle == WSDL_STYLE_ALL) || (wsdlStyle == WSDL_STYLE_DOC))
					removeDocOperation(service, projectName, operationName);
				if ((wsdlStyle == WSDL_STYLE_ALL) || (wsdlStyle == WSDL_STYLE_RPC))
					removeRpcOperation(service, projectName, operationName);
			}
			else {
				throw new Exception("projectName parameter is invalid");
			}
		}
		
		private void removeDocOperation(Service service, String projectName, String operationName) throws Exception {
			String targetNamespace = definition.getTargetNamespace();
			
			String portName = projectName + "SOAP";
			Port port = getPort(service, portName);
			if (port == null) {
				throw new Exception("Corrupted file : missing port definition");
			}
			
			// Removes messages
			QName requestQName = new QName(targetNamespace, operationName + "Request");
			removeMessage(requestQName);

			QName responseQName = new QName(targetNamespace, operationName + "Response");
			removeMessage(responseQName);
			
			// Removes portType operation
			QName portTypeQName = new QName(targetNamespace, projectName + "PortType");
			PortType portType = getPortType(portTypeQName);
			removePortTypeOperation(portType, operationName);
			
			// Removes binding operation
			QName bindingQName = new QName(targetNamespace, projectName + "SOAPBinding");
			Binding binding = getBinding(bindingQName);
			removeBindingOperation(binding, operationName);
		}
		
		private void removeRpcOperation(Service service, String projectName, String operationName) throws Exception {
			String targetNamespace = definition.getTargetNamespace();
			
			String portName = projectName + "SOAP_RPC";
			Port port = getPort(service, portName);
			if (port == null) {
				throw new Exception("Corrupted file : missing port definition");
			}
			
			// Removes messages
			QName requestQName = new QName(targetNamespace, operationName + "Request_RPC");
			removeMessage(requestQName);

			QName responseQName = new QName(targetNamespace, operationName + "Response_RPC");
			removeMessage(responseQName);
			
			// Removes portType operation
			QName portTypeQName = new QName(targetNamespace, projectName + "PortType_RPC");
			PortType portType = getPortType(portTypeQName);
			removePortTypeOperation(portType, operationName);
			
			// Removes binding operation
			QName bindingQName = new QName(targetNamespace, projectName + "SOAPBinding_RPC");
			Binding binding = getBinding(bindingQName);
			removeBindingOperation(binding, operationName);
		}

		/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * Any modification of above code needs to reflected into convertigo projects templates
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
		private void create(String projectName, String wsdlURI, String wsdlStyle) throws Exception {
			String targetNamespace = "http://www.convertigo.com/convertigo/projects/"+projectName;
			this.wsdlURI = wsdlURI;
			this.definition = new DefinitionImpl();
			this.wsdlStyle = wsdlStyle.equals("") ? WSDL_STYLE_DOC:wsdlStyle;
			
			definition.setExtensionRegistry(new PopulatedExtensionRegistry());
			definition.setTargetNamespace(targetNamespace);
			definition.setQName(new QName(targetNamespace, projectName));
			definition.addNamespace("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
			definition.addNamespace("soapenc", "http://schemas.xmlsoap.org/soap/encoding/");
			definition.addNamespace("wsdl", "http://schemas.xmlsoap.org/wsdl/");
			definition.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
			definition.addNamespace(projectName+"_ns", targetNamespace);
			bModified = true;
			
			addTypes(projectName);
			
			Service service = createService(new QName(targetNamespace, projectName));
			if ((wsdlStyle == WSDL_STYLE_ALL) || (wsdlStyle == WSDL_STYLE_DOC))
				addDocServicePort(service, projectName);
			if ((wsdlStyle == WSDL_STYLE_ALL) || (wsdlStyle == WSDL_STYLE_RPC))
				addRpcServicePort(service, projectName);

			save();
		}
		
		private void addDocServicePort(Service service, String projectName) throws Exception {
			String targetNamespace = definition.getTargetNamespace();
			
			QName portTypeQName = new QName(targetNamespace, projectName + "PortType");
			PortType portType = addDocPortType(portTypeQName);
			
			QName bindingQName = new QName(targetNamespace, projectName + "SOAPBinding");
			Binding binding = addDocBinding(bindingQName);
			binding.setPortType(portType);
			
			String portName = projectName + "SOAP";
			Port port = addDocPort(projectName, portName);
			port.setBinding(binding);
			
			service.addPort(port);
		}
		
		private void addRpcServicePort(Service service, String projectName) throws Exception {
			String targetNamespace = definition.getTargetNamespace();
			
			QName portTypeQName = new QName(targetNamespace, projectName + "PortType_RPC");
			PortType portType = addRpcPortType(portTypeQName);
			
			QName bindingQName = new QName(targetNamespace, projectName + "SOAPBinding_RPC");
			Binding binding = addRpcBinding(bindingQName);
			binding.setPortType(portType);
			
			String portName = projectName + "SOAP_RPC";
			Port port = addRpcPort(projectName, portName);
			port.setBinding(binding);
			
			service.addPort(port);
		}

		private Message addDocMessage(QName messageQName, QName partElementQName) throws Exception {
			if (messageQName != null) {
				if (partElementQName == null)
					throw new Exception("partElementQName parameter is invalid");
				
				Message message = createMessage(messageQName);
				Part part = definition.createPart();
				part.setName("parameters");
				part.setElementName(partElementQName);
				
				message.addPart(part);
				bModified = true;
				return message;
			}
			else {
				throw new Exception("messageQName parameter is invalid");
			}
		}
		
		private Message addRpcMessage(QName messageQName, QName partElementQName) throws Exception {
			if (messageQName != null) {
				if (partElementQName == null)
					throw new Exception("partElementQName parameter is invalid");
				
				Message message = createMessage(messageQName);
				if (messageQName.getLocalPart().indexOf("Request") != -1) {
					
					String filePath = wsdlURI.substring(0, wsdlURI.indexOf(".wsdl")) + ".xsd";
					InputStream is = null;
					try {
						is = new FileInputStream(filePath);
						Document doc = XMLUtils.parseDOM(is);
						addRpcMessagePart(doc, message, partElementQName.getLocalPart());
					}
					finally {
						if (is != null)
							is.close();
					}
				}
				else {
					Part part = definition.createPart();
					part.setName("response");
					part.setTypeName(partElementQName);
					
					message.addPart(part);
					bModified = true;
				}
				return message;
			}
			else {
				throw new Exception("messageQName parameter is invalid");
			}
		}
		
		private Message addRpcMessage(QName messageQName, QName partElementQName, String xsdTypes) throws Exception {
			if (messageQName != null) {
				if (partElementQName == null)
					throw new Exception("partElementQName parameter is invalid");
				
				Message message = createMessage(messageQName);
				if (messageQName.getLocalPart().indexOf("Request") != -1) {
					if ((xsdTypes != null) && (!xsdTypes.equals(""))) {
						InputStream is = null;
						try {
							String s = "<document>"+xsdTypes+"</document>";
							is = new ByteArrayInputStream(s.getBytes());
							Document doc = XMLUtils.parseDOM(is);
							addRpcMessagePart(doc, message, partElementQName.getLocalPart());
						}
						finally {
							if (is != null)
								is.close();
						}
					}
				}
				return message;
			}
			else {
				throw new Exception("messageQName parameter is invalid");
			}
		}

		private void addRpcMessagePart(Document doc, Message message, String typeName) {
			if (doc != null) {
				String elementName, elementType, paramNs, paramType;
				StringTokenizer tokenizer;
				Element complex;
				int tokens;
				
				complex = null;
				NodeList list = doc.getElementsByTagName("xsd:complexType");
				for (int i=0; i<list.getLength(); i++) {
					if (((Element)list.item(i)).getAttribute("name").equals(typeName)) {
						complex = (Element)list.item(i);
						break;
					}
				}
				
				if (complex != null) {
					NodeList clist = complex.getChildNodes();
					if (clist!=null) {
						for (int i=0; i<clist.getLength(); i++) {
							Node cnode = clist.item(i);
							if (cnode.getNodeType()== Node.ELEMENT_NODE) {
								Element celement = (Element)cnode;
								if (celement.getNodeName().equals("xsd:sequence")) {
									NodeList slist = celement.getChildNodes();
									if (slist != null) {
										for (int j=0; j<slist.getLength(); j++) {
											Node snode = slist.item(j);
											if (snode.getNodeType()== Node.ELEMENT_NODE) {
												Element selement = (Element)snode;
												elementName = selement.getAttribute("name");
												elementType = selement.getAttribute("type");
												if (elementType.equals("")) { // multivalued
													NodeList selements = selement.getElementsByTagName("xsd:element");
													Element item = (Element)selements.item(0);
													String itemType = item.getAttribute("type");
													elementType = definition.getPrefix(definition.getTargetNamespace());
													elementType += ":ArrayOf_" + itemType.replaceAll(":", "_");
												}
												
												paramNs = "xsd";
												paramType = "string";
												tokenizer = new StringTokenizer(elementType, ":", false);
												tokens = tokenizer.countTokens();
												if (tokens == 1) {
													paramType = tokenizer.nextToken();
												}
												else if (tokens == 2) {
													paramNs = tokenizer.nextToken();
													paramType = tokenizer.nextToken();
												}
												
												if (message.getPart(elementName) == null) {
													Part part = definition.createPart();
													part.setName(elementName);
													part.setTypeName(new QName(definition.getNamespace(paramNs),paramType));
													
													message.addPart(part);
													bModified = true;
												}												
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		private Message createMessage(QName messageQName) throws Exception {
			if (messageQName != null) {
				Message message = definition.createMessage();
				message.setQName(messageQName);
				message.setUndefined(false);
				
				definition.addMessage(message);
				bModified = true;
				return message;
			}
			else {
				throw new Exception("messageQName parameter is invalid");
			}
		}
		
		private void removeMessage(QName messageQName) throws Exception {
			if (messageQName != null) {
				Message message = definition.getMessage(messageQName);
				if (message != null) {
					definition.removeMessage(messageQName);
					bModified = true;
				}
			}
			else {
				throw new Exception("messageQName parameter is invalid");
			}
		}

		private Operation addDocPortTypeOperation(PortType portType, String operationName, QName inputQName, QName ouptutQName) throws Exception {
			if (portType != null) {
					Operation operation = createPortTypeOperation(operationName, inputQName, ouptutQName);
					portType.addOperation(operation);
					return operation;
			}
			else {
				throw new Exception("portType parameter is invalid");
			}
		}
		private Operation addRpcPortTypeOperation(PortType portType, String operationName, QName inputQName, QName ouptutQName) throws Exception {
			if (portType != null) {
				Operation operation = createPortTypeOperation(operationName, inputQName, ouptutQName);
				portType.addOperation(operation);
				return operation;
			}
			else {
				throw new Exception("portType parameter is invalid");
			}
		}
		
		private Operation createPortTypeOperation(String operationName, QName inputQName, QName ouptutQName) throws Exception {
			if ((operationName == null) || (operationName.equals("")))
				throw new Exception("operationName parameter is invalid");
			if (inputQName == null)
				throw new Exception("operationName parameter is invalid");
			if (ouptutQName == null)
				throw new Exception("operationName parameter is invalid");
			
			Message inputMessage = definition.createMessage();
			inputMessage.setQName(inputQName);
			Input input = definition.createInput();
			input.setMessage(inputMessage);
			
			Message outpuMessage = definition.createMessage();
			outpuMessage.setQName(ouptutQName);
			Output output = definition.createOutput();
			output.setMessage(outpuMessage);
			
			Operation operation = definition.createOperation();
			operation.setName(operationName);
			operation.setInput(input);
			operation.setOutput(output);
			operation.setUndefined(false);
			
			bModified = true;
			return operation;
		}

		private void removePortTypeOperation(PortType portType, String operationName) throws Exception {
			if (portType != null) {
				if ((operationName == null) || (operationName.equals("")))
					throw new Exception("operationName parameter is invalid");
				Operation operation = portType.getOperation(operationName, null, null);
				if (operation != null) {
					portType.getOperations().remove(operation);
					bModified = true;
				}
			}
			else {
				throw new Exception("portType parameter is invalid");
			}
		}

		private BindingOperation addDocBindingOperation(Binding binding, String projectName, String operationName) throws Exception {
			if (binding != null) {
				if ((projectName == null) || (projectName.equals("")))
					throw new Exception("projectName parameter is invalid");
				
				SOAPBodyImpl soapInputBody = new SOAPBodyImpl();
				soapInputBody.setUse("literal");
				BindingInput bindingInput = definition.createBindingInput();
				bindingInput.addExtensibilityElement(soapInputBody);
				
				SOAPBodyImpl soapOutputBody = new SOAPBodyImpl();
				soapOutputBody.setUse("literal");
				BindingOutput bindingOutput = definition.createBindingOutput();
				bindingOutput.addExtensibilityElement(soapOutputBody);

				SOAPOperationImpl soapOperation = new SOAPOperationImpl();
				soapOperation.setSoapActionURI(projectName + "?" + operationName);
				
				BindingOperation bindingOperation = createBindingOperation(operationName);
				bindingOperation.addExtensibilityElement(soapOperation);
				bindingOperation.setBindingInput(bindingInput);
				bindingOperation.setBindingOutput(bindingOutput);
				
				binding.addBindingOperation(bindingOperation);
				bModified = true;
				return bindingOperation;
			}
			else {
				throw new Exception("binding parameter is invalid");
			}
		}
		
		private BindingOperation addRpcBindingOperation(Binding binding, String projectName, String operationName) throws Exception {
			if (binding != null) {
				if ((projectName == null) || (projectName.equals("")))
					throw new Exception("projectName parameter is invalid");
				
				String targetNamespace = definition.getTargetNamespace();
				List<String> list = Arrays.asList(new String[]{"http://schemas.xmlsoap.org/soap/encoding/"});
				
				SOAPBodyImpl soapInputBody = new SOAPBodyImpl();
				soapInputBody.setUse("encoded");
				soapInputBody.setNamespaceURI(targetNamespace);
				soapInputBody.setEncodingStyles(list);
				BindingInput bindingInput = definition.createBindingInput();
				bindingInput.addExtensibilityElement(soapInputBody);
				
				SOAPBodyImpl soapOutputBody = new SOAPBodyImpl();
				soapOutputBody.setUse("encoded");
				soapOutputBody.setNamespaceURI(targetNamespace);
				soapOutputBody.setEncodingStyles(list);
				BindingOutput bindingOutput = definition.createBindingOutput();
				bindingOutput.addExtensibilityElement(soapOutputBody);

				SOAPOperationImpl soapOperation = new SOAPOperationImpl();
				soapOperation.setSoapActionURI(projectName + "?" + operationName);
				
				BindingOperation bindingOperation = createBindingOperation(operationName);
				bindingOperation.addExtensibilityElement(soapOperation);
				bindingOperation.setBindingInput(bindingInput);
				bindingOperation.setBindingOutput(bindingOutput);
				
				binding.addBindingOperation(bindingOperation);
				bModified = true;
				return bindingOperation;
			}
			else {
				throw new Exception("binding parameter is invalid");
			}
		}
		
		private BindingOperation createBindingOperation(String operationName) throws Exception {
			if ((operationName != null) && (!operationName.equals(""))) {
				BindingOperation bindingOperation = definition.createBindingOperation();
				bindingOperation.setName(operationName);
				bModified = true;
				return bindingOperation;
			}
			else {
				throw new Exception("operationName parameter is invalid");
			}
		}
		
		private void removeBindingOperation(Binding binding, String operationName) throws Exception {
			if (binding != null) {
				if ((operationName == null) || (operationName.equals("")))
					throw new Exception("operationName parameter is invalid");
				
				BindingOperation bindingOperation = binding.getBindingOperation(operationName, null, null);
				if (bindingOperation != null) {
					binding.getBindingOperations().remove(bindingOperation);
					bModified = true;
				}
			}
			else {
				throw new Exception("binding parameter is invalid");
			}
		}

		/*private Types getTypes() {
			return definition.getTypes();
		}*/
		
		private Document getDocument() throws WSDLException {
			WSDLWriterImpl wsdlWriter = new WSDLWriterImpl();
			return wsdlWriter.getDocument(definition);
			
		}
		
		private Element createDocumentationElement(String nodeValue) throws WSDLException {
			Document doc = getDocument();
			Element element = doc.createElementNS("http://schemas.xmlsoap.org/wsdl/","documentation");
			element.setPrefix("wsdl");
			String cdataValue = nodeValue;
			cdataValue = cdataValue.replaceAll("<!\\[CDATA\\[", "&lt;!\\[CDATA\\[");
			cdataValue = cdataValue.replaceAll("\\]\\]>", "\\]\\]&gt;");
			element.appendChild(doc.createCDATASection(cdataValue));
			return element;
		}
		
		private Types addTypes(String projectName) throws WSDLException {
			Document doc = getDocument();
			Element include = doc.createElement("xsd:include");
			//include.setAttribute("schemaLocation",  applicationServerUrl + "/projects/"+projectName+"/"+projectName + ".xsd");
			include.setAttribute("schemaLocation", projectName + ".xsd");
			Element schema = doc.createElement("xsd:schema");
			schema.setAttribute("targetNamespace", definition.getTargetNamespace());
			schema.appendChild(include);
			
			Schema _schema = (Schema)definition.getExtensionRegistry().createExtension(Types.class, SchemaConstants.Q_ELEM_XSD_2001);
			_schema.setElement(schema);
			
			Types types = createTypes();
			types.addExtensibilityElement(_schema);
			bModified = true;
			return types;
		}
		
		private Types createTypes() throws WSDLException {
			Types types = definition.createTypes();
			definition.setTypes(types);
			bModified = true;
			return types;
		}

		public void addSchemaTypes(Document xsdTypesDoc) {
			List<?> _elements = definition.getTypes().getExtensibilityElements();
			if (_elements.size() > 0) {
				Schema _schema = (Schema)_elements.get(0);
				Element schema = _schema.getElement();
				Document doc = schema.getOwnerDocument();
				if (xsdTypesDoc != null) {
					Element root = xsdTypesDoc.getDocumentElement();
					NodeList list = root.getChildNodes();
					for (int i=0; i<list.getLength(); i++) {
						Node node = list.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							Element element = (Element)node;
							if (!existElementWithName(schema.getChildNodes(),element.getAttribute("name"))) {
								schema.appendChild(doc.importNode(element, true));
								bModified = true;
							}
						}
					}
				}
			}
		}
		
	    private boolean existElementWithName(NodeList nodeList, String name) {
	        Node node;
	        if ((name != null) && (!name.equals(""))) {
		        int len = nodeList.getLength();
		        for (int i = 0 ; i < len ; i++) {
		        	node = nodeList.item(i);
		        	if (node.getNodeType() == Node.ELEMENT_NODE) {
			            if (name.equals(((Element)node).getAttribute("name"))) {
			                return true;
			            }
		        	}
		        }
	        }
	        return false;
	    }
		
		private PortType getPortType(QName portTypeQName) throws Exception {
			if (portTypeQName != null) {
				return definition.getPortType(portTypeQName);
			}
			else {
				throw new Exception("portTypeQName parameter is invalid");
			}
		}
		
		private PortType addDocPortType(QName portTypeQName) throws Exception {
			return createPortType(portTypeQName);
		}
		
		private PortType addRpcPortType(QName portTypeQName) throws Exception {
			return createPortType(portTypeQName);
		}
		
		private PortType createPortType(QName portTypeQName) throws Exception {
			if (portTypeQName != null) {
				PortType portType = definition.createPortType();
				portType.setQName(portTypeQName);
				portType.setUndefined(false);
				definition.addPortType(portType);
				bModified = true;
				return portType;
			}
			else {
				throw new Exception("portTypeQName parameter is invalid");
			}
		}

		private Binding getBinding(QName bindingQName) throws Exception {
			if (bindingQName != null) {
				return definition.getBinding(bindingQName);
			}
			else {
				throw new Exception("bindingQName parameter is invalid");
			}
		}
		
		private Binding addDocBinding(QName bindingQName) throws Exception {
			SOAPBindingImpl soapBinding = new SOAPBindingImpl();
			soapBinding.setTransportURI("http://schemas.xmlsoap.org/soap/http");
			soapBinding.setStyle("document");
			
			Binding binding = createBinding(bindingQName);
			binding.addExtensibilityElement(soapBinding);
			return binding;
		}
		
		private Binding addRpcBinding(QName bindingQName) throws Exception {
			SOAPBindingImpl soapBinding = new SOAPBindingImpl();
			soapBinding.setTransportURI("http://schemas.xmlsoap.org/soap/http");
			soapBinding.setStyle("rpc");
			
			Binding binding = createBinding(bindingQName);
			binding.addExtensibilityElement(soapBinding);
			return binding;
		}

		private Binding createBinding(QName bindingQName) throws Exception {
			if (bindingQName != null) {
				Binding binding = definition.createBinding();
				binding.setQName(bindingQName);
				binding.setUndefined(false);
				definition.addBinding(binding);
				bModified = true;
				return binding;
			}
			else {
				throw new Exception("bindingQName parameter is invalid");
			}
		}

		private Service getService(QName serviceQName) throws Exception {
			if (serviceQName != null) {
				return definition.getService(serviceQName);
			}
			else {
				throw new Exception("serviceQName parameter is invalid");
			}
		}
		
		private Service createService(QName serviceQName) throws Exception {
			if (serviceQName != null) {
				Service service = definition.createService();
				service.setQName(serviceQName);
				definition.addService(service);
				bModified = true;
				return service;
			}
			else {
				throw new Exception("serviceQName parameter is invalid");
			}
		}
		
		private Port getPort(Service service, String portName) throws Exception {
			if ((portName != null) && (!portName.equals(""))) {
				if (service == null)
					throw new Exception("service parameter is invalid");
				return service.getPort(portName);
			}
			else {
				throw new Exception("portName parameter is invalid");
			}
		}
		
		private Port addDocPort(String projectName, String portName) throws Exception {
			if ((projectName != null) && (!projectName.equals(""))) {
				SOAPAddress soapAddress = new SOAPAddressImpl();
				soapAddress.setLocationURI(applicationServerUrl + "/projects/"+ projectName +"/.wsl");
				
				Port port = createPort(portName);
				port.addExtensibilityElement(soapAddress);
				return port;
			}
			else {
				throw new Exception("projectName parameter is invalid");
			}
		}
		
		private Port addRpcPort(String projectName, String portName) throws Exception {
			if ((projectName != null) && (!projectName.equals(""))) {
				SOAPAddress soapAddress = new SOAPAddressImpl();
				soapAddress.setLocationURI(applicationServerUrl + "/projects/"+ projectName +"/.ws");
				
				Port port = createPort(portName);
				port.addExtensibilityElement(soapAddress);
				return port;
			}
			else {
				throw new Exception("projectName parameter is invalid");
			}
		}
		
		private Port createPort(String portName) throws Exception {
			if ((portName != null) && (!portName.equals(""))) {
				Port port = definition.createPort();
				port.setName(portName);
				bModified = true;
				return port;
			}
			else {
				throw new Exception("portName parameter is invalid");
			}
		}
		
		public HashMap<String, String> dumpSchemas(String schemasDir) {
			HashMap<String, String> nsmap = new HashMap<String, String>();
			Definition definition = getDefinition();
			if (definition != null) {
				// Create schemas directory
				File dir = new File(schemasDir);
				if (!dir.exists())
					dir.mkdirs();
				
				// Get types
				Types types = definition.getTypes();
				Iterator<?> exs = types.getExtensibilityElements().iterator();
				while (exs.hasNext()) {
					ExtensibilityElement ee = (ExtensibilityElement)exs.next();
					if (ee instanceof Schema) {
						Schema schema = (Schema)ee;
						
						XmlSchemaCollection schemaCol = new XmlSchemaCollection();
						schemaCol.setBaseUri(definition.getDocumentBaseURI());
						schemaCol.read(schema.getElement());
						
						HashMap<String, String> options = new HashMap<String, String>();
						options.put(OutputKeys.OMIT_XML_DECLARATION, "no");
						options.put(OutputKeys.INDENT, "yes");

						XmlSchema[] schemas = schemaCol.getXmlSchemas();
						for (int i=0; i<schemas.length; i++) {
							// Retrieve schema
							XmlSchema xmlSchema = schemas[i];
							
							// Set new location for imported schema
							Iterator<?> it = xmlSchema.getItems().getIterator();
							while (it.hasNext()) {
								XmlSchemaObject ob = (XmlSchemaObject)it.next();
								if (ob instanceof XmlSchemaImport) {
									XmlSchemaImport xmlSchemaImport = ((XmlSchemaImport)ob);
									String xmlSchemaImportNs = xmlSchemaImport.getNamespace();
									if (xmlSchemaImportNs != null)
										xmlSchemaImport.setSchemaLocation(StringUtils.normalize(xmlSchemaImportNs) + ".xsd");
								}
							}
							
							// Write schema to file
							String ns = xmlSchema.getTargetNamespace();
							if ((ns!=null) && !ns.equals("")) {
								String xsdFileName = StringUtils.normalize(ns) + ".xsd";
								String xsdFilePath = schemasDir + "/"+ xsdFileName;
								FileOutputStream fos = null;
								try {
									fos = new FileOutputStream(xsdFilePath);
									xmlSchema.write(fos, options);
									fos.close();
									
									String prefix = xmlSchema.getNamespaceContext().getPrefix(ns);
									nsmap.put(prefix, ns);
								}
								catch (Exception e) {
									if (fos != null) {
										try {
											fos.close();
										} catch (IOException e1) {}
									}
									
									File file = new File(xsdFilePath);
									if (file.exists()) {
										try {
											//boolean bDeleted = file.delete();
											//System.out.println("file deleted \""+xsdFilePath+"\"" + (bDeleted ? " OK":" KO"));
										} catch (Exception e2) {}
									}
								}
								finally {
									if (fos != null) {
										try {
											fos.close();
										} catch (IOException e1) {}
									}
								}
							}
						}
					}
				}
			}
			return nsmap;
		}
	}
	
/*
	private static void testCreateWSDL() {
		String projectName = "statsDB";
		String operationName = "Stats01";
		try {
			WSDL wsdl = createWSDL(projectName, "C:/Development/SVN/Convertigo4.4.3/tomcat/webapps/convertigo/projects/statsDB/statsDB.wsdl");
			
			//WSDL wsdl = getWSDL("C:/Development/SVN/Convertigo4.4.3/tomcat/webapps/convertigo/projects/statsDB/statsDB.wsdl");
			wsdl.addOperation(projectName, operationName);
			wsdl.save();
			
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}
	
	private static void testImportWSDL() {
		try {
			WSDL wsdl = getWSDL("http://localhost:18080/convertigo/projects/statsDB/.wsl?WSDL");
			Definition definition = wsdl.getDefinition();
			if (definition != null) {
				System.out.println("--------------------------------------------------");
				// Get types
				Types types = definition.getTypes();
				Iterator exs = types.getExtensibilityElements().iterator();
				while (exs.hasNext()) {
					ExtensibilityElement ee = (ExtensibilityElement)exs.next();
					if (ee instanceof Schema) {
						Schema schema = (Schema)ee;
						
						XmlSchemaCollection schemaCol = new XmlSchemaCollection();
						schemaCol.setBaseUri(definition.getDocumentBaseURI());
						schemaCol.read(schema.getElement());
						
						HashMap options = new HashMap();
						options.put(OutputKeys.OMIT_XML_DECLARATION, "no");
						options.put(OutputKeys.INDENT, "yes");

						XmlSchema[] schemas = schemaCol.getXmlSchemas();
						for (int i=0; i<schemas.length; i++) {
							XmlSchema xmlSchema = schemas[i];
							try {
								String uri = xmlSchema.getSourceURI();
								System.out.println("\nDebug XMLSchema ("+ xmlSchema.toString() + ") " + uri +":");
								//xmlSchema.write(System.out, options);
								if (uri != null) {
									SchemaTypeLoader loader = XmlBeans.loadXsd(new XmlObject[] { XmlObject.Factory.parse(new URL(uri))}, new XmlOptions());
									SchemaType t = loader.findDocumentType(new QName("http://www.convertigo.com/convertigo/projects/statsDB", "Stats01"));
									System.out.println("Type=" + t.toString());
								}
							}
							catch (Exception e) {
								Engine.logEngine.error("Unexpected exception", e);
							}
						}
					}
				}
				
				System.out.println("--------------------------------------------------");
				// Get service
				Service service = (Service)definition.getServices().values().iterator().next();
				System.out.println("Service: " + service.getQName().getLocalPart());
				
				// Get service ports
				Iterator ports = service.getPorts().values().iterator();
				while (ports.hasNext()) {
					System.out.println("--------------------------------------------------");
					
					Port port = (Port)ports.next();
					System.out.println("Port: " + port.getName());
					
					Binding binding = port.getBinding();
					System.out.println("Binding: " + binding.getQName().getLocalPart());
					
					PortType portType = binding.getPortType();
					System.out.println("PortType: " + portType.getQName().getLocalPart());
					
					//Iterator operations = binding.getBindingOperations().iterator();
					//while (operations.hasNext()) {
					//	BindingOperation bindingOperation = (BindingOperation)operations.next();
					//	System.out.println("Operation: " + bindingOperation.getName());
					//}

					Iterator operations = portType.getOperations().iterator();
					while (operations.hasNext()) {
						Operation operation = (Operation)operations.next();
						String opname = operation.getName();
						System.out.println("Operation: " + opname);
						
						BindingOperation bindingOperation = binding.getBindingOperation(opname, null, null);
						Iterator exelems = bindingOperation.getExtensibilityElements().iterator();
						while (exelems.hasNext()) {
							ExtensibilityElement ee = (ExtensibilityElement)exelems.next();
							if (ee instanceof SOAPOperation)
								System.out.println("SOAP action: " + ((SOAPOperation)ee).getSoapActionURI());
						}
						
						Input input = operation.getInput();
						Message message = input.getMessage();
						System.out.println("Input: " + message.getQName().getLocalPart());

						Iterator parts = message.getParts().values().iterator();
						while (parts.hasNext()) {
							Part part = (Part)parts.next();
							System.out.println("Part: " + part.getName() + ":" + part.getElementName() + ":" + part.getTypeName());
						}
					}
				}
			}
			
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}

	private static void testDumpSchemas(String wsdlUri, String schemasDir) {
		try {
			WSDL wsdl = getWSDL(wsdlUri);
			wsdl.dumpSchemas(schemasDir);
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}

	public static void main(String[] args) {
		// testCreateWSDL();
		//testImportWSDL();
		//testDumpSchemas("http://localhost:18080/convertigo/enterprise.wsdl","C:/Temp/Schemas");
	}
*/
}
