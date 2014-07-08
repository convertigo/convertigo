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

package com.twinsoft.convertigo.engine.servlets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.wsdl.WSDLElement;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.dom.TextImpl;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.ibm.wsdl.DefinitionImpl;
import com.ibm.wsdl.extensions.PopulatedExtensionRegistry;
import com.ibm.wsdl.extensions.schema.SchemaConstants;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPOperationImpl;
import com.ibm.wsdl.xml.WSDLWriterImpl;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.requesters.WebServiceServletRequester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SOAPUtils;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;

public class WebServiceServlet extends GenericServlet {

	public static final String REQUEST_MESSAGE_ATTRIBUTE = "com.twinsoft.convertigo.engine.requesters.WebServiceServletRequester.requestMessage";
	
	private static final long serialVersionUID = -3070056458702585103L;

	public String getName() {
    	return "WebServiceServlet";
    }
    
	@Override
	public Object processRequest(HttpServletRequest request) throws Exception {
		getSOAPMessage(request);
		return super.processRequest(request);
	}
	


	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        if (Engine.theApp == null) throw new ServletException("Unable to process the request: the Convertigo engine is not started!");

        String queryString = request.getQueryString();
        Engine.logEngine.debug("(WebServiceServlet) Query string: " + queryString);

        if ("wsdl".equalsIgnoreCase(queryString) || "xwsdl".equalsIgnoreCase(queryString)) {
    		response.addHeader("Expires", "-1");
    		response.addHeader("Pragma", "no-cache");
    		response.addHeader("Cache-control", "no-cache");
			response.setContentType(getDefaultContentType());

    		try {
                String wsdl = generateWsdl(request);
                Writer output = response.getWriter();
                output.write(wsdl);

                Engine.logEngine.debug("(WebServiceServlet) WSDL sent :\n"+ wsdl);
    		}
    		catch(Exception e) {
    			throw new ServletException(e);
    		}
        }
        else {
        	throw new ServletException("Unknown GET command! (query string: " + queryString + ")");
        }
    }

	@Override
    public void processException(HttpServletRequest request, HttpServletResponse response, Exception e) throws ServletException {
		boolean bThrowHTTP500 = Boolean.parseBoolean(EnginePropertiesManager
				.getProperty(EnginePropertiesManager.PropertyName.THROW_HTTP_500_SOAP_FAULT));

		if (bThrowHTTP500) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Engine.logEngine.debug("(WebServiceServlet) Requested HTTP 500 status code");
		}
		try {
			String soapFault = SOAPUtils.writeSoapFault(e, "UTF-8");
			response.getWriter().print(soapFault);
			Engine.logEngine.debug("(WebServiceServlet) SOAP fault response:\n"+ soapFault);
		} catch (IOException e1) {
			throw new ServletException(e);
		}
    }
    
	@Override
    public String getDefaultContentType() {
    	return "text/xml; charset=\"UTF-8\"";
    }

    public Requester getRequester() {
		return new WebServiceServletRequester();
    }

    public String getDocumentExtension() {
        return ".ws";
    }
    
    @Override
    public String getServletInfo() {
        return "TWinSoft Convertigo web service provider";
    }
	
    protected String generateWsdl(HttpServletRequest request) throws EngineException {
        Engine.logEngine.debug("(WebServiceServlet) WSDL required");
        
		String servletPath = request.getServletPath();
		Engine.logEngine.debug("(WebServiceServlet) Servlet path: " + servletPath);
		
        String servletURI =
            request.getScheme() + "://" +
            request.getServerName() + ":" +
            request.getServerPort() +
            request.getRequestURI();
        Engine.logEngine.debug("(WebServiceServlet) Servlet uri: " + servletURI);
        
		try {
			int projectNameStartIndex = servletPath.indexOf("/projects/") + 10; 
			int slashIndex = servletPath.indexOf("/", projectNameStartIndex);

			String projectName = servletPath.substring(projectNameStartIndex, slashIndex);
			Engine.logEngine.debug("(WebServiceServlet) Project name: " + projectName);

			if (servletPath.endsWith(".wsl") || servletPath.endsWith(".ws") || servletPath.endsWith(".wsr")) {
				return generateWsdlForDocLiteral(servletURI, projectName);
			}
			throw new EngineException("Unhandled SOAP method (RPC or literal accepted)");
		}
		catch(StringIndexOutOfBoundsException e) {
			throw new EngineException("Unable to find the project name into the provided URL (\"" + servletPath + "\").");
		}
    }
    
    protected static String encode(String source, String encoding) throws UnsupportedEncodingException {
    	String encoded = new String(source.getBytes(encoding));
    	return encoded;
    }
    
    public static String generateWsdlForDocLiteral(String servletURI, String projectName) throws EngineException {
    	Engine.logEngine.debug("(WebServiceServlet) Generating WSDL...");
    	
    	String locationPath = servletURI.substring(0,servletURI.indexOf("/.w"));
    	String targetNamespace = Project.CONVERTIGO_PROJECTS_NAMESPACEURI + projectName;
    	
    	Project project = null;
		// server mode
		if (Engine.isEngineMode()) {
			project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			targetNamespace = project.getTargetNamespace();
		}
		// studio mode
		else {
			project = Engine.objectsProvider.getProject(projectName);
			targetNamespace = project.getTargetNamespace();
		}

		// Create WSDL definition
		Definition definition = new DefinitionImpl();
		definition.setExtensionRegistry(new PopulatedExtensionRegistry());
		definition.setTargetNamespace(targetNamespace);
		definition.setQName(new QName(targetNamespace, projectName));
		definition.addNamespace("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
		definition.addNamespace("soapenc", "http://schemas.xmlsoap.org/soap/encoding/");
		definition.addNamespace("wsdl", "http://schemas.xmlsoap.org/wsdl/");
		definition.addNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
		definition.addNamespace(projectName+"_ns", targetNamespace);
		
		// Add WSDL Types
		Types types = definition.createTypes();
		definition.setTypes(types);
		
		// Add WSDL service
		Service service = definition.createService();
		service.setQName(new QName(targetNamespace, projectName));
		definition.addService(service);
		
		// Add WSDL binding
		PortType portType = definition.createPortType();
		portType.setQName(new QName(targetNamespace, projectName + "PortType"));
		portType.setUndefined(false);
		definition.addPortType(portType);
		
		SOAPBindingImpl soapBinding = new SOAPBindingImpl();
		soapBinding.setTransportURI("http://schemas.xmlsoap.org/soap/http");
		soapBinding.setStyle("document");
		
		Binding binding = definition.createBinding();
		binding.setQName(new QName(targetNamespace, projectName + "SOAPBinding"));
		binding.setUndefined(false);
		definition.addBinding(binding);
		binding.addExtensibilityElement(soapBinding);
		binding.setPortType(portType);
		
		// Add WSDL port
		SOAPAddress soapAddress = new SOAPAddressImpl();
		soapAddress.setLocationURI(locationPath +"/.wsl");
		
		Port port = definition.createPort();
		port.setName(projectName + "SOAP");
		port.addExtensibilityElement(soapAddress);
		port.setBinding(binding);
		
		service.addPort(port);
    	
		// Add all WSDL operations
		List<QName> partElementQNames = new ArrayList<QName>(); // remember qnames for accessibility
		if (project != null) {
			for (Connector connector : project.getConnectorsList()) {
				for (Transaction transaction : connector.getTransactionsList()) {
					if (transaction.isPublicAccessibility()) {
						addWsdlOperation(definition, transaction, partElementQNames);
					}
				}
			}
			for (Sequence sequence : project.getSequencesList()) {
				if (sequence.isPublicAccessibility()) {
					addWsdlOperation(definition, sequence, partElementQNames);
				}
			}
		}
		
		// Add all schemas of project under WSDL Types
		try {
			// Retrieve the only needed schema objects map
			XmlSchemaCollection xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName);
			XmlSchema projectSchema = xmlSchemaCollection.getXmlSchema(targetNamespace)[0];
			LinkedHashMap<QName, XmlSchemaObject> map = new LinkedHashMap<QName, XmlSchemaObject>();
			XmlSchemaWalker dw = XmlSchemaWalker.newDependencyWalker(map, true, true);
			for (QName qname: partElementQNames) {
				dw.walkByElementRef(projectSchema, qname);
			}
				if (Engine.logEngine.isTraceEnabled()) {
					String message = "";
					for (QName qname: map.keySet()) {
						message += "\n\t"+ qname.toString();
					}
					Engine.logEngine.trace("(WebServiceServlet) needed schema objects :"+ message);
				}

			// Read schemas into a new Collection in order to modify them
			XmlSchemaCollection wsdlSchemaCollection = new XmlSchemaCollection();
			String baseURI = Engine.PROJECTS_PATH + "/" + projectName + "/xsd";
			if (baseURI != null) wsdlSchemaCollection.setBaseUri(baseURI);
			for (XmlSchema xmlSchema: xmlSchemaCollection.getXmlSchemas()) {
				String tns = xmlSchema.getTargetNamespace();
				if (tns.equals(Constants.URI_2001_SCHEMA_XSD)) continue;
				if (tns.equals(SchemaUtils.URI_SOAP_ENC)) continue;
				
				if (wsdlSchemaCollection.schemaForNamespace(tns) == null) {
					wsdlSchemaCollection.read(xmlSchema.getSchemaDocument().getDocumentElement());
				}
			}
			
			// Modify schemas and add them
			Map<String, Schema> schemaMap = new HashMap<String, Schema>();
			for (XmlSchema xmlSchema: wsdlSchemaCollection.getXmlSchemas()) {
				if (xmlSchema.getTargetNamespace().equals(Constants.URI_2001_SCHEMA_XSD)) continue;
				if (xmlSchema.getTargetNamespace().equals(SchemaUtils.URI_SOAP_ENC)) continue;
				
				String tns = xmlSchema.getTargetNamespace();
				
				// Reduce schema to needed objects
				reduceSchema(xmlSchema, map.keySet());
				
				// Retrieve schema Element
				Schema wsdlSchema = schemaMap.get(tns);
				if (wsdlSchema == null) {
					wsdlSchema = (Schema)definition.getExtensionRegistry().createExtension(Types.class, SchemaConstants.Q_ELEM_XSD_2001);
					Element schemaElt = xmlSchema.getSchemaDocument().getDocumentElement();
					
					// Remove 'schemaLocation' attribute on imports
					NodeList importList = schemaElt.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD, "import");
					if (importList.getLength() > 0) {
						for (int i=0; i < importList.getLength(); i++) {
							Element importElt = (Element)importList.item(i);
							importElt.removeAttribute("schemaLocation");
						}
					}
					// Remove includes
					NodeList includeList = schemaElt.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD, "include");
					if (includeList.getLength() > 0) {
						for (int i=0; i < includeList.getLength(); i++) {
							schemaElt.removeChild(includeList.item(i));
						}
					}
					
					// Add schema Element
					schemaMap.put(tns, wsdlSchema);
					wsdlSchema.setElement(schemaElt);
					types.addExtensibilityElement(wsdlSchema);
				}
				else { // case of schema include (same targetNamespace) or same schema
					Element schemaElt = wsdlSchema.getElement();
					
					// Add missing attributes
					NamedNodeMap attributeMap = xmlSchema.getSchemaDocument().getDocumentElement().getAttributes();
					for (int i=0; i< attributeMap.getLength(); i++) {
						Node node = attributeMap.item(i);
						if (schemaElt.getAttributes().getNamedItem(node.getNodeName()) == null) {
							schemaElt.setAttribute(node.getNodeName(), node.getNodeValue());
						}
					}
					
					// Add children
					NodeList children = xmlSchema.getSchemaDocument().getDocumentElement().getChildNodes();
					for (int i=0; i< children.getLength(); i++) {
						Node node = children.item(i);
						// Special cases
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							Element el = (Element)node;
							// Do not add include
							if (el.getTagName().endsWith("include"))
								continue;
							// Add import at first
							if (el.getTagName().endsWith("import")) {
								String ins = el.getAttribute("namespace");
								if (!tns.equals(ins) && el.hasAttribute("schemaLocation")) {
									if (!hasElement(schemaElt, el.getLocalName(), ins))
										schemaElt.insertBefore(schemaElt.getOwnerDocument().importNode(el, true), schemaElt.getFirstChild());
									continue;
								}
							}
							// Else
							if (!hasElement(schemaElt, el.getLocalName(), el.getAttribute("name")))
								schemaElt.appendChild(schemaElt.getOwnerDocument().importNode(el, true));
						}
						else {
							// Others
							schemaElt.appendChild(schemaElt.getOwnerDocument().importNode(node, true));
						}
					}
				}
			}
			
		} catch (Exception e1) {
			Engine.logEngine.error("An error occured while adding schemas for WSDL", e1);
		}
		
		// Write WSDL to string
		WSDLWriter wsdlWriter = new WSDLWriterImpl();
		StringWriter sw = new StringWriter();
		try {
			wsdlWriter.writeWSDL(definition, sw);
		} catch (WSDLException e) {
			Engine.logEngine.error("An error occured while generating WSDL", e);
		}
		String wsdl = sw.toString();
        return wsdl;
    }
    
    private static boolean hasElement(Element schemaElt, String localName, String attrValue) {
    	NodeList list = schemaElt.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD, localName);
		if (list.getLength() > 0) {
			for (int i=0; i < list.getLength(); i++) {
				Element el = (Element)list.item(i);
				if ("include".equals(localName) || "import".equals(localName)) {
					if (el.getAttribute("namespace").equals(attrValue)) {
						return true;
					}
				}
				else {
					if (el.getAttribute("name").equals(attrValue)) {
						return true;
					}
				}
			}
		}
    	return false;
    }
    
	private static void reduceSchema(XmlSchema xmlSchema, Set<QName> qnames) {
		if (qnames == null)
			return;
		if (xmlSchema == null)
			return;
		
		XmlSchemaObject ob = null;
		QName qname = null;
		
		String tns = xmlSchema.getTargetNamespace();
		
		String message = "";
		Iterator<XmlSchemaObject> it = GenericUtils.cast(xmlSchema.getItems().getIterator());
		while (it.hasNext()) {
			ob = it.next();
			if (ob instanceof XmlSchemaType) {
				qname = ((XmlSchemaType)ob).getQName();
				if (qname == null) {
					qname = new QName(tns, ((XmlSchemaType)ob).getName());
				}
			}
			else if (ob instanceof XmlSchemaElement) {
				qname = ((XmlSchemaElement)ob).getQName();
				if (qname == null) {
					qname = new QName(tns, ((XmlSchemaElement)ob).getName());
				}
			}
			else if (ob instanceof XmlSchemaGroup) {
				qname = ((XmlSchemaGroup)ob).getName();
			}
			else if (ob instanceof XmlSchemaAttribute) {
				qname = ((XmlSchemaAttribute)ob).getQName();
				if (qname == null) {
					qname = new QName(tns, ((XmlSchemaAttribute)ob).getName());
				}
			}
			else {
				qname = null;
			}
			
			if (qname!=null) {
				if (!qnames.contains(qname)) {
					it.remove();
					message += "\n\tremoved: "+qname.toString();
				}
				else {
					message += "\n\tkept: "+qname.toString();
				}
			}
			else {
				message += "\n\tfound: "+ob.toString();
			}
		}
		
		if (Engine.logEngine.isTraceEnabled()) {
			Engine.logEngine.trace("(WebServiceServlet) reduceSchema for "+ xmlSchema.getTargetNamespace() + message);
			//xmlSchema.write(System.out, options);
		}
	}
    
    private static void addPartElementQName(List<QName> qnames, QName qname) {
    	if (qname == null)
    		return;
    	if (!qnames.contains(qname))
    		qnames.add(qname);
    }
    
    private static void addWsdlOperation(Definition definition, RequestableObject requestable, List<QName> qnames) {
    	String targetNamespace = definition.getTargetNamespace();
    	String projectName = requestable.getProject().getName();
    	String operationName = requestable.getXsdTypePrefix() + requestable.getName();
    	String operationComment = requestable.getComment();
    	
		// Adds messages
		QName requestQName = new QName(targetNamespace, operationName + "Request");
		QName partElementRequestQName = new QName(targetNamespace, operationName);
		addWsdlMessage(definition, requestQName, partElementRequestQName);
		addPartElementQName(qnames, partElementRequestQName);

		QName responseQName = new QName(targetNamespace, operationName + "Response");
		QName partElementResponseQName = new QName(targetNamespace, operationName + "Response");
		addWsdlMessage(definition, responseQName, partElementResponseQName);
		addPartElementQName(qnames, partElementResponseQName);
		
		// Adds portType operation
		QName portTypeQName = new QName(targetNamespace, projectName + "PortType");
		addWsdlPortTypeOperation(definition, portTypeQName, operationName, operationComment, requestQName, responseQName);
		
		// Adds binding operation
		QName bindingQName = new QName(targetNamespace, projectName + "SOAPBinding");
		addWsdlBindingOperation(definition, bindingQName, projectName, operationName, operationComment);
    }
    
    private static void addWsdlMessage(Definition definition, QName messageQName, QName partElementQName) {
		Message message = definition.createMessage();
		message.setQName(messageQName);
		message.setUndefined(false);
		Part part = definition.createPart();
		part.setName("parameters");
		part.setElementName(partElementQName);
		message.addPart(part);
		definition.addMessage(message);
    }
    
    private static void addWsdlPortTypeOperation(Definition definition, QName portTypeQName, String operationName, String operationComment, QName inputQName, QName ouptutQName) {
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
		addWsdLDocumentation(definition, operation, operationComment);
		
		PortType portType = definition.getPortType(portTypeQName);
		portType.addOperation(operation);
    }
    
    private static void addWsdlBindingOperation(Definition definition, QName bindingQName, String projectName, String operationName, String operationComment) {
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
		
		BindingOperation bindingOperation = definition.createBindingOperation();
		bindingOperation.setName(operationName);
		bindingOperation.addExtensibilityElement(soapOperation);
		bindingOperation.setBindingInput(bindingInput);
		bindingOperation.setBindingOutput(bindingOutput);
		addWsdLDocumentation(definition, bindingOperation, operationComment);
		
		Binding binding = definition.getBinding(bindingQName);
		binding.addBindingOperation(bindingOperation);
    }
    
    private static void addWsdLDocumentation(Definition definition, WSDLElement wsdlElement, String documentation) {
		if (documentation.equals(""))
			return;
		
    	try {
			Document doc = new WSDLWriterImpl().getDocument(definition);
			Element element = doc.createElementNS("http://schemas.xmlsoap.org/wsdl/","documentation");
			element.setPrefix("wsdl");
			String cdataValue = documentation;
			cdataValue = cdataValue.replaceAll("<!\\[CDATA\\[", "&lt;!\\[CDATA\\[");
			cdataValue = cdataValue.replaceAll("\\]\\]>", "\\]\\]&gt;");
			element.appendChild(doc.createCDATASection(cdataValue));
			wsdlElement.setDocumentationElement(element);
		} catch (WSDLException e) {
			e.printStackTrace();
		}
    }
    
    private SOAPMessage getSOAPMessage(HttpServletRequest request) throws SOAPException, IOException {
		
		SOAPMessage requestMessage = (SOAPMessage) request.getAttribute(REQUEST_MESSAGE_ATTRIBUTE);
		if (requestMessage == null) {
			boolean bAddXmlEncodingCharSet = new Boolean(EnginePropertiesManager.getProperty(PropertyName.SOAP_REQUEST_ADD_XML_ENCODING_CHARSET)).booleanValue();

			String contentType = request.getContentType();
			
			boolean isMultipart = contentType.toLowerCase().indexOf("multipart/related") >= 0;
			boolean isXOP = contentType.toLowerCase().indexOf("application/xop+xml") >= 0;
			boolean isMTOM = isMultipart && isXOP;
			
			MessageFactory messageFactory = MessageFactory.newInstance();
			if (messageFactory instanceof org.apache.axis2.saaj.MessageFactoryImpl) {
				((org.apache.axis2.saaj.MessageFactoryImpl)messageFactory).setProcessMTOM(true);
			}
			
			MimeHeaders mimeHeaders = new MimeHeaders();
			mimeHeaders.setHeader("Content-Type", isMTOM ? contentType:"text/xml; charset=\"UTF-8\"");
			
			InputStream is = null;
			if (isMTOM) {
				is = request.getInputStream();
			}
			else {
				StringBuffer requestAsString = new StringBuffer("");
				if (bAddXmlEncodingCharSet)
					requestAsString.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				
				BufferedReader br = new BufferedReader(request.getReader());
				String line;
				while ((line = br.readLine()) != null) {
					requestAsString.append(line + "\n");
				}
		
				String sRequest = requestAsString.toString();
				Engine.logEngine.trace("(WebServiceServlet) input:\n" + sRequest);
				
				is = new ByteArrayInputStream(sRequest.getBytes("UTF-8"));
			}
			
			// Create the SOAP request message
			requestMessage = messageFactory.createMessage(mimeHeaders, is);
			
			// Handle MTOM uploads
			if (isMTOM) {
				handleMTOMUploads(request.getSession().getId(), requestMessage);
			}
			
			// Remove all attachments
			requestMessage.removeAllAttachments();
			requestMessage.saveChanges();
			
			// Store the request message for later use
			request.setAttribute(REQUEST_MESSAGE_ATTRIBUTE, requestMessage);
		}
		return requestMessage;
	}
	
	private void handleMTOMUploads(String sessionID, SOAPMessage requestMessage) throws SOAPException, FileNotFoundException, IOException {
		SOAPPart sp = requestMessage.getSOAPPart();
		SOAPEnvelope se = sp.getEnvelope();
		SOAPBody sb = se.getBody();
		handleMTOMUploads(sessionID, sb.getChildElements());
	}
	
	private void handleMTOMUploads(String sessionID, Iterator<?> iterator) throws FileNotFoundException, IOException, SOAPException {
		while (iterator.hasNext()) {
			Object element = iterator.next();
			if (element instanceof SOAPElement) {
				handleMTOMUploads(sessionID, (SOAPElement)element);
				handleMTOMUploads(sessionID, ((SOAPElement)element).getChildElements());
			}
		}
	}
	
	private void handleMTOMUploads(String sessionID, SOAPElement soapElement) throws FileNotFoundException, IOException, SOAPException {
		if (soapElement instanceof org.apache.axis2.saaj.SOAPElementImpl) {
			org.apache.axis2.saaj.SOAPElementImpl el = (org.apache.axis2.saaj.SOAPElementImpl)soapElement;
			final OMNode firstOMChild = el.getElement().getFirstOMChild();
			if (firstOMChild instanceof TextImpl) {
	        	TextImpl ti = ((TextImpl)firstOMChild);
	        	boolean isBinary = ti.isBinary();
	        	boolean isOptimized = ti.isOptimized();
	            String contentID = ti.getContentID();
	            if (isBinary && isOptimized && contentID != null) {
	            	// Write file
	            	javax.activation.DataHandler dh = (javax.activation.DataHandler)ti.getDataHandler();
	            	String filePath = getUploadFilePath(sessionID, el.getLocalName(), dh.getContentType());
	            	dh.writeTo(new FileOutputStream(new File(filePath)));
					
	            	// Modify value in soap envelope (replace the base64 encoded value with the filepath value)
	            	ti.detach();
					el.addTextNode(filePath);
					
					Engine.logEngine.trace("(WebServiceServlet) File successfully uploaded :"+ filePath);
	            }
			}
		}
	}
	
	private String getUploadFilePath(String sessionID, String fileName, String mimeType) throws IOException {
		File uploadsDir = new File(Engine.USER_WORKSPACE_PATH + "/uploads");
		if (!uploadsDir.exists()) {
			uploadsDir.mkdir();
		}
		String cpUploadsDir = uploadsDir.getCanonicalPath();

		String[] mimeTypeParts = mimeType.split(";");

		/*
		 * Computing the file extension:
		 * - if present in the header, use this one
		 * - otherwise, we will use "xxx"
		 */
		String fileExt = MimeType.parse(mimeTypeParts[0]).getExtensions()[0];
		if (fileExt.equals("")) {
			fileExt = "xxx";
		}

		/*
		 * Computing the file name:
		 * - if present in the header, use this one
		 * - otherwise, we create the filename with the node name
		 */
		fileName = fileName + "." + fileExt;
		for (int i = 0; i < mimeTypeParts.length; i++) {
			if (mimeTypeParts[i].trim().startsWith("name=")) {
				fileName = mimeTypeParts[i].trim().substring("name=".length());
				break;
			}
		}

		/*
		 * This filename should be unique accross all C8O contexts. So, first,
		 * we add the session ID, and second, if it already exists, we try to
		 * compute a new unique filename by adding an incremental number.
		 */
		String uniqueFileName = sessionID + "_" + fileName;
		int i = 1;
		File file = new File(cpUploadsDir, uniqueFileName);
		if (file.exists()) {
			Engine.logEngine.warn("MTOM upload filename (\"" + file.toString()
					+ "\") already exists, computing a new unique one...");
			do {
				i++;
				uniqueFileName = sessionID + "_" + i + "_" + fileName;
				file = new File(cpUploadsDir, uniqueFileName);
			} while (file.exists());
		}

		fileName = file.toString();
		Engine.logEngine.debug("MTOM upload file name: " + fileName);
		return fileName;
	}
}
