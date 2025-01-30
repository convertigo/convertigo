/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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
import com.eviware.soapui.settings.ProxySettings;
import com.eviware.soapui.settings.WsdlSettings;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.InputVariablesStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.PacManager.PacInfos;
import com.twinsoft.convertigo.engine.Version;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

public class WsReference {
	public record CreateSequenceOptions(Accessibility accessiblity, boolean authenticationRequired) {};
	
	public static CreateSequenceOptions nextCreateSequences = null;
	
	private RemoteFileReference reference = null;
	private CreateSequenceOptions createSequences = nextCreateSequences;
	
	protected WsReference(WebServiceReference reference) {
		this.reference = reference;
	}

	protected WsReference(RestServiceReference reference) {
		this.reference = reference;
	}

	private RemoteFileReference getReference() {
		return reference;
	}
	
	protected HttpConnector importInto(Project project) throws Exception {
		boolean needAuthentication = false;
		HttpConnector httpConnector = null;
		try {
			if (project != null) {
				RemoteFileReference wsReference = getReference();
				if (wsReference != null) {
					
					String wsdlUrl = wsReference.getUrlpath();
					
					// test for valid URL
					new URL(wsdlUrl);
					
					needAuthentication = wsReference.needAuthentication();
					
					// SOAP web service
					if (wsReference instanceof WebServiceReference) {
						try {
							// Configure SoapUI settings
							configureSoapUI(wsdlUrl);
							
							// Authenticate
							if (needAuthentication) {
								String login = wsReference.getAuthUser();
								String password = wsReference.getAuthPassword();
								try {
									//We add login/password into the connection
									System.setProperty("soapui.loader.username", login);
									System.setProperty("soapui.loader.password", password);
									
									tryAuthentication(wsReference);
								} catch (Exception e) {
									throw new Exception ("Authentication failure !", e);
								}
							}
							
							WebServiceReference soapServiceReference = (WebServiceReference)wsReference;
							httpConnector = importSoapWebService(project, soapServiceReference);
						} finally {
							if (needAuthentication) {
								//We clear login/password
								System.setProperty("soapui.loader.username", "");
								System.setProperty("soapui.loader.password", "");
							}
						}
					}
					
					// REST web service
					else if (wsReference instanceof RestServiceReference) {
						RestServiceReference restServiceReference = (RestServiceReference)wsReference;
						httpConnector = importRestWebService(project, restServiceReference);
					}
				}
			}
			
			if (createSequences != null && httpConnector != null) {
				var prefix = project.getName() + TransactionStep.SOURCE_SEPARATOR + httpConnector.getName() + TransactionStep.SOURCE_SEPARATOR;
				for (var tr: httpConnector.getTransactionsList()) {
					var seq = new GenericSequence();
					seq.setAccessibility(createSequences.accessiblity());
					seq.setAuthenticatedContextRequired(createSequences.authenticationRequired());
					seq.setName(tr.getName());
					var call = new TransactionStep();
					call.setSourceTransaction(prefix + tr.getName());
					seq.add(new InputVariablesStep());
					seq.add(call);
					var copy = new XMLCopyStep();
					var source = new XMLVector<String>();
					source.add(Long.toString(call.priority));
					source.add("./document/*[name() != 'HttpInfo']");
					copy.setSourceDefinition(source);
					seq.add(copy);
					project.add(seq);
					call.importVariableDefinition();
					call.exportVariableDefinition();
				}
			}
			
		} catch (Throwable t) {
			throw new EngineException("Unable to import the web service reference", t);
		}
		return httpConnector;
	}
	
	static public int getTotalTaskNumber() {
		return 9;
	}
	
	private static void tryAuthentication(RemoteFileReference wsReference) throws Exception {
		URL urlToConnect = wsReference.getUrl();
		String wsdlUrl = wsReference.getUrlpath();
		String username = wsReference.getAuthUser();
		String password = wsReference.getAuthPassword();
		
        HttpClient client = new HttpClient();

		client.getState().setCredentials(
				new AuthScope(urlToConnect.getHost(), urlToConnect.getPort()),
				new UsernamePasswordCredentials(username, password)
		);
        
        GetMethod get = new GetMethod(wsdlUrl);
        get.setDoAuthentication( true );
        
        int statuscode = client.executeMethod(get);
        
        if (statuscode == HttpStatus.SC_UNAUTHORIZED) {
        	throw new Exception(HttpStatus.SC_UNAUTHORIZED + " - Unauthorized connection!");
        }
	}
	
	private static void configureSoapUI(String wsdlUrl) throws Exception {
		boolean soapuiSettingsChanged = false;
		Settings settings = SoapUI.getSettings();
		if (settings != null) {
			// WSDL
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
			
			// PROXY
			ProxyMode proxyMode = Engine.theApp.proxyManager.proxyMode;
			String proxyExcludes = StringUtils.join(Engine.theApp.proxyManager.getBypassDomains(), ",");
			String proxyServer = Engine.theApp.proxyManager.getProxyServer();
			int proxyPort = Engine.theApp.proxyManager.getProxyPort();
			String proxyUser = Engine.theApp.proxyManager.getProxyUser();
			String proxyPwd = Engine.theApp.proxyManager.getProxyPassword();
			
			boolean enableProxy = Engine.theApp.proxyManager.isEnabled();
			if (enableProxy) {
				if (proxyMode == ProxyMode.auto) {
					try {
						URL url = new URL(wsdlUrl);
						PacInfos pacInfos = Engine.theApp.proxyManager.getPacInfos(url.toString(), url.getHost());
						if (pacInfos != null) {
							proxyServer = pacInfos.getServer();
							proxyPort = pacInfos.getPort();
						}
					}
					catch (Exception e) {}
				}
				
				if (!settings.getBoolean(ProxySettings.ENABLE_PROXY)) {
					settings.setBoolean(ProxySettings.ENABLE_PROXY, true);
					soapuiSettingsChanged = true;
				}
				if (!proxyExcludes.equals(settings.getString(ProxySettings.EXCLUDES, null))) {
					settings.setString(ProxySettings.EXCLUDES, proxyExcludes);
					soapuiSettingsChanged = true;
				}
				if (!proxyServer.equals(settings.getString(ProxySettings.HOST, null))) {
					settings.setString(ProxySettings.HOST, proxyServer);
					soapuiSettingsChanged = true;
				}
				if (!String.valueOf(proxyPort).equals(settings.getString(ProxySettings.PORT, null))) {
					settings.setString(ProxySettings.PORT, String.valueOf(proxyPort));
					soapuiSettingsChanged = true;
				}
				if (!proxyUser.equals(settings.getString(ProxySettings.USERNAME, null))) {
					settings.setString(ProxySettings.USERNAME, proxyUser);
					soapuiSettingsChanged = true;
				}
				if (!proxyPwd.equals(settings.getString(ProxySettings.PASSWORD, null))) {
					settings.setString(ProxySettings.PASSWORD, proxyPwd);
					soapuiSettingsChanged = true;
				}
			}
			else {
				if (settings.getBoolean(ProxySettings.ENABLE_PROXY)) {
					settings.setBoolean(ProxySettings.ENABLE_PROXY, false);
					soapuiSettingsChanged = true;
				}
			}
		}
		if (soapuiSettingsChanged)
			SoapUI.saveSettings();
	}
	
	private static HttpConnector importRestWebService(Project project, RestServiceReference restServiceReference) throws Exception
	{
		try {
			HttpConnector httpConnector = null;
			
			if (restServiceReference == null) {
				throw new Exception("Reference is null");
			}
			
			Object restApi = null;
			
			String urlPath = restServiceReference.getUrlpath();
			if (urlPath.startsWith("http")) {
				// Try to parse as a Swagger definition (oas2)
				restApi = new SwaggerParser().read(urlPath);
				
				// Try to parse as an OpenAPI definition (oas3)
				if (restApi == null) {
					restApi = new OpenAPIV3Parser().read(urlPath);
				}
			}
			else if (urlPath.startsWith("file")) {
				URL url = new URL(urlPath);
				File f = FileUtils.toFile(url);
				String filePath = f.getAbsolutePath();
				
				// Try to parse as a Swagger definition (oas2)
				restApi = new SwaggerParser().read(filePath);
				
				// Try to parse as an OpenAPI definition (oas3)
				if (restApi == null) {
					restApi = new OpenAPIV3Parser().read(filePath);
				}
			}
				
			if (restApi != null) {
				httpConnector = createRestConnector(restApi);
			}
			// Try to parse as a ? definition
			else {
				//String jsonString = readFromURL(restServiceReference.getUrl());
				//httpConnector = createRestConnector(new JSONObject(jsonString));
				
				throw new Exception("Invalid REST definition");
			}
			
			if (httpConnector != null) {
				project.add(httpConnector);
				if (project.getDefaultConnector() == null) {
					project.setDefaultConnector(httpConnector);
				}
				project.hasChanged = true;
			}
			
			return httpConnector;
		}
		catch (Throwable t) {
			throw new Exception("Unable to import REST service", t);
		}
	}
	
	@SuppressWarnings("unused")
	private static String readFromURL(URL url) throws Exception {
		String line;
		StringBuffer buf = new StringBuffer("");
        BufferedReader in = null;
        try {
    		Boolean isFileUrl = url.getProtocol().equals("file");
	        in = new BufferedReader(new InputStreamReader(isFileUrl ? new FileInputStream(url.getFile()):url.openStream()));
	        while ((line = in.readLine()) != null) {
				buf.append(line);
				buf.append("\n");
	        }
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception("Unable to read from URL:"+ url, e);
		} finally {
    		try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {}
        }
		return buf.toString();
	}
		
	private static HttpConnector createRestConnector(Object object) throws Exception {
		if (object instanceof Swagger) {
			return SwaggerUtils.createRestConnector((Swagger)object);
		} else if (object instanceof OpenAPI) {
			return OpenApiUtils.createRestConnector((OpenAPI)object);
		} else {
			throw new Exception("Unsupported REST definition format");
		}
	}
	
	private static HttpConnector importSoapWebService(Project project, WebServiceReference soapServiceReference) throws Exception
	{
		List<HttpConnector> connectors = new ArrayList<HttpConnector>();
	   	HttpConnector firstConnector = null;

		String wsdlUrl = soapServiceReference.getUrlpath();
		
	   	WsdlProject wsdlProject = new WsdlProject();
	   	WsdlInterface[] wsdls = WsdlImporter.importWsdl(wsdlProject, wsdlUrl); 	
	   	
	   	int len = wsdls.length;
	   	if (len>0) {
	   		WsdlInterface iface = wsdls[len-1];
	   		if (iface != null) {
	   			// Retrieve definition name or first service name
	   			String definitionName = null;
	   			try {
			   		Definition definition = iface.getWsdlContext().getDefinition();
			   		QName qname = definition.getQName();
			   		qname = (qname == null ? (QName) definition.getAllServices().keySet().iterator().next() : qname);
			   		definitionName = qname.getLocalPart();
	   			}
	   			catch (Exception e1) {
	   				throw new Exception("No service found !");
	   			}
		   		
	   			// Modify reference's name
		   		if (soapServiceReference.bNew) {
		   			// Note : new reference may have already been added to the project (new object wizard)
		   			// its name must be replaced with a non existing one !
	   				String newDatabaseObjectName = project.getChildBeanName(project.getReferenceList(), 
	   													StringUtils.normalize("Import_WS_" + definitionName), true);
	   				soapServiceReference.setName(newDatabaseObjectName);
		   		}
		   		
		   		// Retrieve directory for WSDLs to download
				File exportDir = null;
				/* For further use...
				if (!webServiceReference.getFilepath().isEmpty()) {	// for update case
					try {
						exportDir = webServiceReference.getFile().getParentFile();
						if (exportDir.exists()) {
							File destDir = exportDir;
					   		for (int index = 0; destDir.exists(); index++) {
					   			destDir = new File(exportDir.getPath()+ "/v" + index);
					   		}
							Collection<File> files = GenericUtils.cast(FileUtils.listFiles(exportDir, null, false));
							for (File file: files) {
								FileUtils.copyFileToDirectory(file, destDir);
							}
						}
					} catch (Exception ex) {}
				}*/
				if (soapServiceReference.bNew || exportDir == null) {	// for other cases
					String projectDir = project.getDirPath();
					exportDir = new File(projectDir + "/wsdl/" + definitionName);
			   		for (int index = 1; exportDir.exists(); index++) {
			   			exportDir = new File(projectDir + "/wsdl/" + definitionName + index);
			   		}
				}
				
				// Download all needed WSDLs (main one and imported/included ones)
		   		String wsdlPath = iface.getWsdlContext().export(exportDir.getPath());
		   		
		   		// Modify reference's filePath : path to local main WSDL
		   		String wsdlUriPath = new File(wsdlPath).toURI().getPath();
		   		String wsdlLocalPath = ".//" + wsdlUriPath.substring(wsdlUriPath.indexOf("/wsdl") + 1);
		   		soapServiceReference.setFilepath(wsdlLocalPath);
		   		soapServiceReference.hasChanged = true;
		   		
				// Add reference to project
				if (soapServiceReference.getParent() == null) {
					project.add(soapServiceReference);
				}
			   	
		   		// create an HTTP connector for each binding
		   		if (soapServiceReference.bNew) {
				   	for (int i=0; i<wsdls.length; i++) {
					   	iface = wsdls[i];
					   	if (iface != null) {
						   	Definition definition = iface.getWsdlContext().getDefinition();
						   	XmlSchemaCollection xmlSchemaCollection = WSDLUtils.readSchemas(definition);
						   	XmlSchema xmlSchema = xmlSchemaCollection.schemaForNamespace(definition.getTargetNamespace());
						   	
					   		HttpConnector httpConnector = createSoapConnector(iface);
					   		if (httpConnector != null) {
					   			String bindingName = iface.getBindingName().getLocalPart();
				   				String newDatabaseObjectName = project.getChildBeanName(project.getConnectorsList(), 
											StringUtils.normalize(bindingName), true);
					   	   		httpConnector.setName(newDatabaseObjectName);
					   			
					   		   	boolean hasDefaultTransaction = false;
							   	for (int j=0; j<iface.getOperationCount(); j++) {
							   		WsdlOperation wsdlOperation = (WsdlOperation)iface.getOperationAt(j);
								   	XmlHttpTransaction xmlHttpTransaction = createSoapTransaction(xmlSchema, iface, wsdlOperation, project, httpConnector);
						   			// Adds transaction
							   		if (xmlHttpTransaction != null) {
							   			httpConnector.add(xmlHttpTransaction);
							   			if (!hasDefaultTransaction) {
							   				xmlHttpTransaction.setByDefault();
							   				hasDefaultTransaction = true;
							   			}
							   		}
							   	}
							   	
					   	   		connectors.add(httpConnector);
					   		}
					   	}
				   	}
				   	
				   	// add connector(s) to project
				   	for (HttpConnector httpConnector : connectors) {
			   			project.add(httpConnector);
				   		if (firstConnector == null) {
				   			firstConnector = httpConnector;
				   		}
				   	}
				   	
		   		}
	   		}
	   	}
	   	else {
	   		throw new Exception("No interface found !");
	   	}
	   	
	   	return firstConnector;
	}
	
	private static HttpConnector createSoapConnector(WsdlInterface iface) throws Exception {
   		HttpConnector httpConnector = null;
   		if (iface != null) {
   		   	String[] endPoints = iface.getEndpoints();
   		   	if (endPoints.length > 0) {
   	   	   		String comment = iface.getDescription();
   	   	   		try {comment = (comment.equals("")? iface.getBinding().getDocumentationElement().getTextContent():comment);} catch (Exception e) {}

   	   	   		String endPoint = endPoints[0];
	   		   	
	   		   	String host, server, port, baseDir;
	   		   	boolean isHttps = endPoint.startsWith("https://");
	   		   	if (endPoint.indexOf("://") != -1) {
		   		   	int beginIndex = endPoint.indexOf("://") + "://".length();
		   		   	int endIndex = endPoint.indexOf("/", beginIndex);
		   		   	if (endIndex != -1) {
		   		   		host = endPoint.substring(beginIndex, endIndex);
		   		   		baseDir = endPoint.substring(endIndex+1);
		   		   	}
		   		   	else {
		   		   		host = endPoint.substring(beginIndex);
		   		   		baseDir = "";
		   		   	}
		   		   	
		   		   	int middleIndex = host.indexOf(":");
		   		   	if (middleIndex != -1) {
		   		   		server = host.substring(0, middleIndex);
		   		   		port = host.substring(middleIndex+1);
		   		   	}
		   		   	else {
		   		   		server = host;
		   		   		port = isHttps ? "443":"80";
		   		   	}
	   		   	}
	   		   	else {
	   		   		server = endPoint;
	   		   		port = isHttps ? "443":"80";
	   		   		baseDir = "";
	   		   	}
	   		   	
   	   			httpConnector = new HttpConnector();
   	   	   		httpConnector.bNew = true;
   	   	   		
   	   	   		httpConnector.setComment(comment);
	   		   	httpConnector.setHttps(isHttps);
	   		   	httpConnector.setServer(server);
	   		   	httpConnector.setPort(Integer.valueOf(port).intValue());
			   	httpConnector.setBaseDir("/"+baseDir);
	   		   	httpConnector.hasChanged = true;
   		   	}
   		}
	   	return httpConnector;
	}
	
	private static XmlHttpTransaction createSoapTransaction(XmlSchema xmlSchema, WsdlInterface iface, WsdlOperation operation, Project project, HttpConnector httpConnector) throws ParserConfigurationException, SAXException, IOException, EngineException {
		XmlHttpTransaction xmlHttpTransaction = null;
	   	WsdlRequest request;
	   	String requestXml;
	   	String transactionName, comment;
	   	String operationName;
	   	
	   	if (operation != null) {
	   		comment = operation.getDescription();
	   		try {comment = (comment.equals("") ? operation.getBindingOperation().getDocumentationElement().getTextContent():comment);} catch (Exception e) {}
	   		operationName = operation.getName();
		   	transactionName = StringUtils.normalize("C"+operationName);
	   		xmlHttpTransaction = new XmlHttpTransaction();
	   		xmlHttpTransaction.bNew = true;
	   		xmlHttpTransaction.setHttpVerb(HttpMethodType.POST);
	   		xmlHttpTransaction.setName(transactionName);
	   		xmlHttpTransaction.setComment(comment);
	   		
	   		// Set encoding (UTF-8 by default)
	   		xmlHttpTransaction.setEncodingCharSet("UTF-8");
	   		xmlHttpTransaction.setXmlEncoding("UTF-8");
	   		
	   		// Ignore SOAP elements in response
	   		xmlHttpTransaction.setIgnoreSoapEnveloppe(true);
	   		
   			// Adds parameters
   			XMLVector<XMLVector<String>> parameters = new XMLVector<XMLVector<String>>();
   			XMLVector<String> xmlv;
   			xmlv = new XMLVector<String>();
   			xmlv.add(HeaderName.ContentType.value());
   			xmlv.add(MimeType.TextXml.value());
   			parameters.add(xmlv);
   			
   			xmlv = new XMLVector<String>();
   			xmlv.add("Host");
   			xmlv.add(httpConnector.getServer());
   			parameters.add(xmlv);

   			xmlv = new XMLVector<String>();
   			xmlv.add("SOAPAction");
   			xmlv.add(""); // fix #4215 - SOAPAction header must be empty
   			parameters.add(xmlv);

   			xmlv = new XMLVector<String>();
   			xmlv.add("user-agent");
   			xmlv.add("Convertigo EMS "+ Version.fullProductVersion);
   			parameters.add(xmlv);
   			
   			xmlHttpTransaction.setHttpParameters(parameters);
   			
   			QName qname = null;
   			boolean bRPC = false;
   			String style = operation.getStyle();
   			if (style.toUpperCase().equals("RPC")) bRPC = true;
   			
   			// Set SOAP response element
   			if (bRPC) {
   				try {
					MessagePart[] parts = operation.getDefaultResponseParts();
					if (parts.length>0) {
						String ename = parts[0].getName();
						if (parts[0].getPartType().name().equals("CONTENT")) {
							MessagePart.ContentPart mpcp = (MessagePart.ContentPart)parts[0];
							qname = mpcp.getSchemaType().getName();
							if (qname != null) {
								// response is based on an element defined with a type
								// operationResponse element name; element name; element type
								String responseQName = operationName + "Response;" + ename + ";" + "{"+qname.getNamespaceURI()+"}" + qname.getLocalPart();
								xmlHttpTransaction.setResponseElementQName(responseQName);
							}
						}
					}
   				}
   				catch (Exception e) {}
   			}
   			else {
				try {
					qname = operation.getResponseBodyElementQName();
					if (qname != null) {
						QName refName = new QName(qname.getNamespaceURI(),qname.getLocalPart());
						xmlHttpTransaction.setXmlElementRefAffectation(new XmlQName(refName));
					}
				}
				catch (Exception e) {}
			}
			
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
		   	
			Element enveloppe = requestDoc.getDocumentElement();
			String soapenvNamespace = enveloppe.getNamespaceURI();
			
			// Retrieve variables
		   	Element header = (Element)requestDoc.getDocumentElement().getElementsByTagNameNS(soapenvNamespace,"Header").item(0);
			Element body = (Element)requestDoc.getDocumentElement().getElementsByTagNameNS(soapenvNamespace,"Body").item(0);
			
			//System.out.println(XMLUtils.prettyPrintDOM(requestDoc));
			
			// Extract variables
			List<RequestableHttpVariable> variables = new ArrayList<RequestableHttpVariable>();
			extractSoapVariables(xmlSchema, variables, header, null, false, null);
			extractSoapVariables(xmlSchema, variables, body, null, false, null);
			
			// Serialize request/response into template xml files
			String projectName = project.getName();
			String connectorName = httpConnector.getName();
			String templateDir = Engine.projectDir(projectName) + "/soap-templates/" + connectorName;
			File dir = new File(templateDir);
			if (!dir.exists())
				dir.mkdirs();
			
			String requestTemplateName = "/soap-templates/" + connectorName + "/" + xmlHttpTransaction.getName() + ".xml";
			String requestTemplate = Engine.PROJECTS_PATH + "/"+ projectName + requestTemplateName;
			
			xmlHttpTransaction.setRequestTemplate(requestTemplateName);
			saveTemplate(requestDoc, requestTemplate);
			
   			// Adds variables
   			for (RequestableHttpVariable variable: variables) {
   				//System.out.println("adding "+ variable.getName());
   				xmlHttpTransaction.add(variable);
   			}
			
			xmlHttpTransaction.hasChanged = true;
	   	}
		
		return xmlHttpTransaction;
	}
	
	private static void saveTemplate(Document doc, String templateDir) throws EngineException {
		try {
			XMLUtils.saveXml(doc, templateDir);
        } catch (Exception e) {
        	throw new EngineException("Unable to create template file \""+templateDir+"\"", e);
        }
	}

	private static void extractSoapVariables(XmlSchema xmlSchema, List<RequestableHttpVariable> variables, Node node, String longName, boolean isMulti, QName variableType) throws EngineException {
		if (node == null) return;
		int type = node.getNodeType();
		
		if (type == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			if (element != null) {
				String elementName = element.getLocalName();
				if (longName != null)
					elementName = longName + "_" + elementName;
				
				if (!element.getAttribute("soapenc:arrayType").equals("") && !element.hasChildNodes()) {
					String avalue = element.getAttribute("soapenc:arrayType");
					element.setAttribute("soapenc:arrayType", avalue.replaceAll("\\[\\]", "[1]"));
					
					Element child = element.getOwnerDocument().createElement("item");
					String atype = avalue.replaceAll("\\[\\]", "");
					child.setAttribute("xsi:type", atype);
					if (atype.startsWith("xsd:")) {
						String variableName = elementName + "_item";
						child.appendChild(element.getOwnerDocument().createTextNode("$("+ variableName.toUpperCase() +")"));
						RequestableHttpVariable httpVariable = createHttpVariable(true, variableName, new QName(Constants.URI_2001_SCHEMA_XSD,atype.split(":")[1]));
						variables.add(httpVariable);
					}
					element.appendChild(child);
				}
				
				// extract from attributes
				NamedNodeMap map = element.getAttributes();
				for (int i=0; i<map.getLength(); i++) {
					Node child = map.item(i);
					if (child.getNodeName().equals("soapenc:arrayType")) continue;
					if (child.getNodeName().equals("xsi:type")) continue;
					if (child.getNodeName().equals("soapenv:encodingStyle")) continue;
					
					String variableName = getVariableName(variables, elementName + "_" + child.getLocalName());
					
					child.setNodeValue("$("+ variableName.toUpperCase() +")");
					
					RequestableHttpVariable httpVariable = createHttpVariable(false, variableName, Constants.XSD_STRING);
					variables.add(httpVariable);
				}
				
				// extract from children nodes
				boolean multi = false;
				QName qname = Constants.XSD_STRING;
				NodeList children = element.getChildNodes();
				if (children.getLength() > 0) {
					Node child = element.getFirstChild();
					while (child != null) {
						if (child.getNodeType() == Node.COMMENT_NODE) {
							String value = child.getNodeValue();
							if (value.startsWith("type:")) {
								String schemaType = child.getNodeValue().substring("type:".length()).trim();
								qname = getVariableSchemaType(xmlSchema, schemaType);
							}
							if (value.indexOf("repetitions:") != -1) {
								multi = true;
							}
						}
						else if (child.getNodeType() == Node.TEXT_NODE) {
							String value = child.getNodeValue().trim();
							if (value.equals("?") || !value.equals("")) {
								String variableName = getVariableName(variables, elementName);
								
								child.setNodeValue("$("+ variableName.toUpperCase() +")");
								
								RequestableHttpVariable httpVariable = createHttpVariable(isMulti, variableName, variableType);
								variables.add(httpVariable);
							}
						}
						else if (child.getNodeType() == Node.ELEMENT_NODE) {
							extractSoapVariables(xmlSchema, variables, child, elementName, multi, qname);
							multi = false;
							qname = Constants.XSD_STRING;
						}
						
						child = child.getNextSibling();
					}
				}
				
			}
		}
	}
	
	private static String getVariableName(List<RequestableHttpVariable> variables, String name) {
		String variableName = StringUtils.normalize(name);
		int index = 1;
		for (RequestableHttpVariable variable: variables) {
			if (variable.getName().equals(variableName)) {
				variableName += "_"+ index;
				index++;
				continue;
			}
		}
		return variableName;
	}
	
	private static QName getVariableSchemaType(XmlSchema xmlSchema, String schemaType) {
		String local = "string";
		String nsuri = Constants.URI_2001_SCHEMA_XSD;
		try {
			if (!schemaType.equals("")) {
				if (schemaType.startsWith("{")) {
					int i = schemaType.indexOf("}");
					if (i  != -1) {
						nsuri = schemaType.substring(1, i);
						local = schemaType.substring(i+1);
						int j = local.indexOf(" - "); // there's an info
						if (j != -1) {
							local = local.substring(0, j);
						}
					}
				}
				else if (schemaType.indexOf(":") != -1) {
					String[] qinfos = schemaType.split(":");
					nsuri = xmlSchema.getNamespaceContext().getNamespaceURI(qinfos[0]);
					local = qinfos[1];
				}
			}
		
			QName qname = new QName(nsuri,local);
			return qname;
		}
		catch (Exception e) {
			return Constants.XSD_STRING;
		}
	}
	
	private static RequestableHttpVariable createHttpVariable(boolean multi, String variableName, QName schemaTypeName) throws EngineException {
		RequestableHttpVariable httpVariable = (multi ? new RequestableHttpMultiValuedVariable():new RequestableHttpVariable());
		httpVariable.setName(variableName);
		httpVariable.setDescription(variableName);
		httpVariable.setWsdl(Boolean.TRUE);
		httpVariable.setPersonalizable(Boolean.FALSE);
		httpVariable.setCachedKey(Boolean.TRUE);
		httpVariable.setHttpMethod("POST");
		httpVariable.setHttpName(variableName.toUpperCase());
		httpVariable.setXmlTypeAffectation(new XmlQName(schemaTypeName));
		httpVariable.bNew = true;
		return httpVariable;
	}
	
}
