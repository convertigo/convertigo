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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlImporter;
import com.eviware.soapui.model.iface.MessagePart;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.settings.WsdlSettings;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.Version;
import com.twinsoft.convertigo.engine.util.WSDLUtils.WSDL;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSD;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSDException;

public class WsReference {
	private String wsdlURL;
	
	public WsReference(String wsdlURL) {
		this.wsdlURL = wsdlURL;
	}

	protected HttpConnector importInto(Project project) throws Exception {
		HttpConnector httpConnector = null;
		try {
			if (project != null) {
				setTaskLabel("Importing from \""+wsdlURL+"\"...");
				updateSchema(wsdlURL, project);
				httpConnector = importReference(wsdlURL, project);
			}
		}
		catch (Exception e) {
			if (httpConnector != null) {
				try {
					project.remove(httpConnector);
					httpConnector = null;
				} catch (EngineException e1) {
				}
			}
			throw new Exception(e);
		}
		return httpConnector;
	}
	
	public void setTaskLabel(String text) {
		
	}
	
	private void updateSchema(String wsdlUrl, Project project) throws WSDLException, XSDException {
		String projectName = project.getName();
		String projectDir = Engine.PROJECTS_PATH + "/"+ projectName;
		
		// Dump schemas to project directory
		setTaskLabel("Dumping schemas ...");
		WSDL wsdl = WSDLUtils.getWSDL(wsdlURL);
		HashMap<String, String> nsmap = wsdl.dumpSchemas(projectDir);
		
		// Add namespaces into project's xsd file
		setTaskLabel("Adding schema namespace ...");
		String filePath = projectDir + "/" + projectName + ".temp.xsd";
		if (!new File(filePath).exists())
			filePath = projectDir + "/" + projectName + ".xsd";
		XSD xsd = XSDUtils.getXSD(filePath);
		xsd.addNamespaces(nsmap);
		
		// Add imports into project's xsd file
		setTaskLabel("Adding schema imports ...");
		HashMap<String, String> immap = new HashMap<String, String>();
		Iterator<String> it = nsmap.keySet().iterator();
		while (it.hasNext()) {
			String prefix = (String)it.next();
			String ns = (String)nsmap.get(prefix);
			String location = StringUtils.normalize(ns) + ".xsd";
			immap.put(ns, location);
		}
		xsd.addImportObjects(immap);
		
		// Save xsd
		xsd.save();
	}
	
	private HttpConnector importReference(String wsdlUrl, Project project) throws Exception
	{
		// Configure SoapUI settings
		boolean soapuiSettingsChanged = false;
		Settings settings = SoapUI.getSettings();
		if (settings != null) {
			if (!settings.getBoolean(WsdlSettings.XML_GENERATION_ALWAYS_INCLUDE_OPTIONAL_ELEMENTS)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_ALWAYS_INCLUDE_OPTIONAL_ELEMENTS, true);
				soapuiSettingsChanged = true;
			}
			if (!settings.getBoolean(WsdlSettings.XML_GENERATION_TYPE_COMMENT_TYPE)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_TYPE_COMMENT_TYPE, true);
				soapuiSettingsChanged = true;
			}
			if (!settings.getBoolean(WsdlSettings.XML_GENERATION_TYPE_EXAMPLE_VALUE)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_TYPE_EXAMPLE_VALUE, true);
				soapuiSettingsChanged = true;
			}
			if (settings.getBoolean(WsdlSettings.XML_GENERATION_SKIP_COMMENTS)) {
				settings.setBoolean(WsdlSettings.XML_GENERATION_SKIP_COMMENTS, false);
				soapuiSettingsChanged = true;
			}
		}
		if (soapuiSettingsChanged)
			SoapUI.saveSettings();
		
		// Import WSDL using SoapUI
		HttpConnector httpConnector = null;
		
	   	WsdlProject wsdlProject = new WsdlProject();
	   	WsdlInterface[] wsdls = WsdlImporter.importWsdl( wsdlProject, wsdlUrl);
	   	//Definition definition = WsdlUtils.readDefinition( wsdlUrl );
	   	
	   	boolean hasDefaultTransaction;
	   	WsdlInterface iface;
	   	for (int i=0; i<wsdls.length; i++) {
		   	iface = wsdls[i];
		   	if (iface != null) {
		   		httpConnector = createConnector(iface);
		   		if (httpConnector != null) {
		   		   	hasDefaultTransaction = false;
		   			project.add(httpConnector);
				   	for (int j=0; j<iface.getOperationCount(); j++) {
				   		WsdlOperation wsdlOperation = (WsdlOperation)iface.getOperationAt(j);
				   		Set<RequestableHttpVariable> variables = new HashSet<RequestableHttpVariable>();
				   		XmlHttpTransaction xmlHttpTransaction = createTransaction(iface, wsdlOperation, variables, project, httpConnector);
				   		if (xmlHttpTransaction != null) {
				   			httpConnector.add(xmlHttpTransaction);
				   			if (!hasDefaultTransaction) {
				   				xmlHttpTransaction.setByDefault();
				   				hasDefaultTransaction = true;
				   			}
				   			// Adds variables
				   			for (RequestableHttpVariable variable: variables) {
				   				xmlHttpTransaction.add(variable);
				   			}
				   		}
				   	}
		   		}
		   	}
	   	}
	   	return httpConnector;
	}
	
	private HttpConnector createConnector(WsdlInterface iface) throws EngineException {
   		HttpConnector httpConnector = null;
   		if (iface != null) {
   			httpConnector = new HttpConnector();
   	   		httpConnector.bNew = true;

   	   		String comment = iface.getDescription();
   	   		try {comment = (comment.equals("")? iface.getBinding().getDocumentationElement().getTextContent():comment);} catch (Exception e) {}
   	   		httpConnector.setComment(comment);
   	   		
   	   		String connectorName = StringUtils.normalize(iface.getBindingName().getLocalPart());
   	   		httpConnector.setName(connectorName);
   		   	setTaskLabel("Creating http connector \""+connectorName+"\"...");
   		   	
   		   	String[] endPoints = iface.getEndpoints();
   		   	String endPoint = endPoints[0];
   		   	String host, server, port, baseDir;
   		   	
   		   	boolean isHttps = endPoint.indexOf("https://") != -1;
   		   	httpConnector.setHttps(isHttps);
   		   	
   		   	int beginIndex = endPoint.indexOf("://") + "://".length();
   		   	int endIndex = endPoint.indexOf ("/", beginIndex);
   		   	host = endPoint.substring(beginIndex, endIndex);
   		   	
   		   	int middleIndex = host.indexOf(":");
   		   	if (middleIndex != -1) {
   		   		server = host.substring(0, middleIndex);
   		   		port = host.substring(middleIndex+1);
   		   	}
   		   	else {
   		   		server = host;
   		   		port = isHttps ? "443":"80";
   		   	}
   		   	httpConnector.setServer(server);
   		   	httpConnector.setPort(Integer.valueOf(port).intValue());
   		   	
   		   	baseDir = endPoint.substring(endIndex+1);
   		   	httpConnector.setBaseDir("/"+baseDir);
   		   	httpConnector.hasChanged = true;
   		}
	   	return httpConnector;
	}
	
	private XmlHttpTransaction createTransaction(WsdlInterface iface, WsdlOperation operation, Set<RequestableHttpVariable> variables, Project project, HttpConnector httpConnector) throws ParserConfigurationException, SAXException, IOException, EngineException {
		XmlHttpTransaction xmlHttpTransaction = null;
	   	WsdlRequest request;
	   	//String responseXml;
	   	String requestXml;
	   	String transactionName, comment;
	   	String actionName, operationName;
	   	
	   	if (operation != null) {
	   		actionName = operation.getAction();
	   		comment = operation.getDescription();
	   		try {comment = (comment.equals("") ? operation.getBindingOperation().getDocumentationElement().getTextContent():comment);} catch (Exception e) {}
	   		operationName = operation.getName();
		   	transactionName = StringUtils.normalize("C"+operationName);
	   		xmlHttpTransaction = new XmlHttpTransaction();
	   		xmlHttpTransaction.bNew = true;
	   		xmlHttpTransaction.setHttpVerb(HttpTransaction.HTTP_VERB_POST);
	   		xmlHttpTransaction.setName(transactionName);
	   		xmlHttpTransaction.setComment(comment);
	   		setTaskLabel("Creating transaction \""+transactionName+"\"...");
	   		
	   		// Set encoding (UTF-8 by default)
	   		xmlHttpTransaction.setEncodingCharSet("UTF-8");
	   		xmlHttpTransaction.setXmlEncoding("UTF-8");
	   		
	   		// Ignore SOAP elements in response
	   		xmlHttpTransaction.setIgnoreSoapEnveloppe(true);
	   		
   			// Adds parameters
	   		setTaskLabel("Setting http parameters...");
   			XMLVector<XMLVector<String>> parameters = new XMLVector<XMLVector<String>>();
   			XMLVector<String> xmlv;
   			xmlv = new XMLVector<String>();
   			xmlv.addElement("Content-Type");
   			xmlv.addElement("text/xml");
   			parameters.addElement(xmlv);
   			
   			xmlv = new XMLVector<String>();
   			xmlv.addElement("Host");
   			xmlv.addElement(httpConnector.getServer());
   			parameters.addElement(xmlv);

   			xmlv = new XMLVector<String>();
   			xmlv.addElement("SOAPAction");
   			xmlv.addElement(actionName.equals("") ? operationName:actionName);
   			parameters.addElement(xmlv);

   			xmlv = new XMLVector<String>();
   			xmlv.addElement("user-agent");
   			xmlv.addElement("Convertigo EMS "+ Version.fullProductVersion);
   			parameters.addElement(xmlv);
   			
   			xmlHttpTransaction.setHttpParameters(parameters);
   			
   			// Set SOAP response element
	   		String responseQName = "", qprefix = "", qlocal = "";
   			QName qname = null;
   			boolean bRPC = false;
   			
   			String style = operation.getStyle();
   			if (style.toUpperCase().equals("RPC")) bRPC = true;
   			if (bRPC) {
				MessagePart[] parts = operation.getDefaultResponseParts();
				if (parts.length>0) {
					String ename = parts[0].getName();
					if (parts[0].getPartType().name().equals("CONTENT")) {
						MessagePart.ContentPart mpcp = (MessagePart.ContentPart)parts[0];
					   	try {
							qname = mpcp.getSchemaType().getName();
							qprefix = iface.getDefinitionContext().getDefinition().getPrefix(qname.getNamespaceURI());
							qlocal = qname.getLocalPart();
						} catch (Exception e) {}
						
						if ((qname != null)&&(qprefix != null)) {
							// response is based on an element defined with a type
							// operationResponse element name; element name; element type
							responseQName = operationName + "Response;" + ename + ";" + qprefix + ":" + qlocal;
						}
					}
				}
   			}
   			else {
   			   	try {
   					qname = operation.getResponseBodyElementQName();
   					qprefix = iface.getDefinitionContext().getDefinition().getPrefix(qname.getNamespaceURI());
   					qlocal = qname.getLocalPart();
   				} catch (Exception e) {}
   				
   				if ((qname != null)&&(qprefix != null)) {
   					// response is based on a referenced element
   					// element type
   					responseQName = qprefix + ":" + qlocal;
   				}
   			}
			xmlHttpTransaction.setResponseElementQName(responseQName);
   			
   			// Create request/response
		   	request = operation.addNewRequest("Test"+transactionName);
		   	requestXml = operation.createRequest(true);
		   	request.setRequestContent(requestXml);
		   	//responseXml = operation.createResponse(true);
		   	
		   	
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware( true );
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document requestDoc = db.parse(new InputSource(new StringReader(requestXml)));
			//Document responseDoc = db.parse(new InputSource(new StringReader(responseXml)));
		   	
			// Retrieve variables
		   	Element header = (Element)requestDoc.getDocumentElement().getElementsByTagName("soapenv:Header").item(0);
		   	if (header == null) header = (Element)requestDoc.getDocumentElement().getElementsByTagName("soap:Header").item(0);
		   	
			Element body = (Element)requestDoc.getDocumentElement().getElementsByTagName("soapenv:Body").item(0);
			if (body == null) body = (Element)requestDoc.getDocumentElement().getElementsByTagName("soap:Body").item(0);
			
			extractVariables(header.getChildNodes(), variables);
			extractVariables(body.getChildNodes(), variables);
			
			// Serialize request/response into template xml files
			String projectName = project.getName();
			String connectorName = httpConnector.getName();
			String templateDir = Engine.PROJECTS_PATH + "/"+ projectName + "/soap-templates/" + connectorName;
			File dir = new File(templateDir);
			if (!dir.exists())
				dir.mkdirs();
			
			String requestTemplateName = "/soap-templates/" + connectorName + "/" + xmlHttpTransaction.getName() + ".xml";
			String requestTemplate = Engine.PROJECTS_PATH + "/"+ projectName + requestTemplateName;
			//String responseTemplateName = "/soap-templates/" + connectorName + "/" + xmlHttpTransaction.getName() + "-Response.xml";
			//String responseTemplate = Engine.PROJECTS_DIRECTORY + "/"+ projectName + responseTemplateName;
			
			xmlHttpTransaction.setRequestTemplate(requestTemplateName);
			createTemplate(requestDoc, requestTemplate);
			//createTemplate(responseDoc, responseTemplate);
			
			xmlHttpTransaction.hasChanged = true;
	   	}
		
		return xmlHttpTransaction;
	}
	
	private void createTemplate(Document doc, String templateDir) throws EngineException {
		setTaskLabel("Creating template \""+templateDir+"\"...");
		try {
            Source source = new DOMSource(doc);
            File file = new File(templateDir);
            Result result = new StreamResult(new FileOutputStream(file));
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (Exception e) {
        	throw new EngineException("Unable to create template file \""+templateDir+"\"", e);
        }
	}
	
	private void extractVariables(NodeList list, Set<RequestableHttpVariable> variables) throws EngineException {
		String nodeValue, variableName, schemaType="";
		Element parent, element, child;
		Node node, prev;
		int index;
		boolean multi;
		for (int k=0; k<list.getLength(); k++) {
			node = list.item(k);
			int type = node.getNodeType(); 
			if (type == Node.ELEMENT_NODE) {
				element = (Element)node;
				if (!element.getAttribute("soapenc:arrayType").equals("") && !element.hasChildNodes()) {
					child = element.getOwnerDocument().createElement("item");
					child.setAttribute("xsi:type", "xsd:string");
					child.appendChild(element.getOwnerDocument().createTextNode("?"));
					element.appendChild(child);
				}
				extractVariables(element.getChildNodes(), variables);
			}
			else if (type == Node.TEXT_NODE) {
				parent = (Element)node.getParentNode();
				nodeValue = node.getNodeValue().trim();
				
				// this is a variable
				if (nodeValue.equals("?") || !nodeValue.equals("")) {
					multi = parent.getNodeName().equalsIgnoreCase("item") ||
							(parent.getNodeName().indexOf(":item")!=-1) ? true:false;

					variableName = getVariableName(parent,parent.getNodeName());
					if (variableName == null) continue;
					
					variableName = variableName.replaceAll(":", "_");
					
					index=1;
					while (variables.contains(variableName)) {
						variableName += "_"+ index;
					}
					
					variableName = StringUtils.normalize(variableName);
					
					node.setNodeValue("$("+ variableName.toUpperCase() +")");
					
					// Retrieve schema type only for singlevalued variable!
					if (!multi) {
						try {
							// For Rpc style search for xsi:type attribute
							schemaType = parent.getAttribute("xsi:type");
							// For Doc/Lit style search for comment (<!--type: ???-->)
							if (schemaType.equals("")) {
								prev = parent.getPreviousSibling();
								while ((prev != null) && (prev.getNodeType()!= Node.ELEMENT_NODE)) {
									if (prev.getNodeType() == Node.COMMENT_NODE) {
										if (prev.getNodeValue().startsWith("type:")) {
											schemaType = prev.getNodeValue().substring("type:".length()).trim();
											if (!schemaType.equals("")) {
												if (schemaType.indexOf(":")<0) schemaType = "xsd:"+ schemaType;
												else if (schemaType.indexOf("xsd:")<0) schemaType = ""; // not supported
											}
											break;
										}
									}
									prev = prev.getPreviousSibling();
								}
							}
						}
						catch (Exception e) {
							schemaType = "";
						}
					}
					schemaType = schemaType.equals("") ? "xsd:string":schemaType;
					
					setTaskLabel("Creating variable \""+variableName+"\"...");
					RequestableHttpVariable httpVariable = createVariable(multi,variableName);
					httpVariable.setSchemaType(schemaType);
					variables.add(httpVariable);
				}
			}
		}
	}
	
	private String getVariableName(Element element, String name) {
		Element p, pp;
		try {
			p = (Element)element.getParentNode();
			if ((p.getNodeName().indexOf(":Header")!=-1) ||
				(p.getNodeName().indexOf(":Body")!=-1)) {
				if (name.endsWith("_item"))
					name = name.substring(0, name.lastIndexOf("_item"));
				return name;
			}
			pp = (Element)p.getParentNode();
			if (pp.getNodeName().indexOf(":Body")!=-1) {
				if (name.endsWith("_item"))
					name = name.substring(0, name.lastIndexOf("_item"));
				return name;
			}
			name = p.getNodeName() + "_" + name;
			return getVariableName(p,name);
		}
		catch (Exception e) {
			return null;
		}
	}

	private RequestableHttpVariable createVariable(boolean multi, String variableName) throws EngineException {
		RequestableHttpVariable httpVariable = (multi ? new RequestableHttpMultiValuedVariable():new RequestableHttpVariable());
		httpVariable.setName(variableName);
		httpVariable.setDescription(variableName);
		httpVariable.setWsdl(Boolean.TRUE);
		httpVariable.setPersonalizable(Boolean.FALSE);
		httpVariable.setCachedKey(Boolean.TRUE);
		httpVariable.setHttpMethod("POST");
		httpVariable.setHttpName(variableName.toUpperCase());
		httpVariable.bNew = true;
		return httpVariable;
	}
	
}
