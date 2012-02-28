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

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.requesters.WebServiceServletRequester;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.SOAPUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XSDUtils;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSD;
import com.twinsoft.util.StringEx;

public class WebServiceServlet extends GenericServlet {

	private static final long serialVersionUID = -3070056458702585103L;

	public String getName() {
    	return "WebServiceServlet";
    }
    
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        if (Engine.theApp == null) throw new ServletException("Unable to process the request: the Convertigo engine is not started!");

        String queryString = request.getQueryString();
        Engine.logEngine.debug("(WebServiceServlet) Query string: " + queryString);

        if ("wsdl".equalsIgnoreCase(queryString)) {
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
				.getProperty(EnginePropertiesManager.PropertyName.THROW_HTTP_500));

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
		        return getWsdlFromFile(servletURI, projectName);
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
    
    public static String getWsdlFromFile(String servletURI, String projectName) throws EngineException {
		try {
			Project project;
			String wsdlFilePath, xsdFilePath;
			
			// server mode
			if (Engine.isEngineMode()) {
				project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
				wsdlFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".wsdl";
				xsdFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xsd";
			}
			// studio mode
			else {
				project = Engine.objectsProvider.getProject(projectName);
				wsdlFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.wsdl";
				xsdFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.xsd";
			}
			
			Document dom = XMLUtils.loadXml(wsdlFilePath);
			String path = servletURI.substring(0,servletURI.indexOf("/.w"));
			
			StringEx sx = new StringEx(XMLUtils.prettyPrintDOMWithEncoding(dom, "UTF-8"));
			sx.replaceAll("schemaLocation=\""+projectName+".xsd", "schemaLocation=\""+path+"/"+projectName+".xsd");
			sx.replaceAll("schemaLocation=\""+projectName+".temp.xsd", "schemaLocation=\""+path+"/"+projectName+".temp.xsd");
			String wsdl = sx.toString();
			wsdl = wsdl.replaceAll("soap:address location=\".*/\\.ws\"", "soap:address location=\""+path+"/\\.ws\"");
			wsdl = wsdl.replaceAll("soap:address location=\".*/\\.wsl\"", "soap:address location=\""+path+"/\\.wsl\"");
			
			if (project.isSchemaInline()) {
				try  {
					XSD xsd = XSDUtils.getXSD(xsdFilePath);
					ProjectUtils.RemoveUselessObjects(xsd, project);
					
					if (xsd != null) {
						int start = wsdl.indexOf("<xsd:schema ");
						if (start != -1) {
							int index = wsdl.indexOf(".xsd\"/>", start);
							int end = wsdl.indexOf("</xsd:schema>", start);
							if (end != -1) {
								char c;
								int i, j, k, z, x;
								String prefix, schema, schemas = "", types = "";
								
								// stores inline types if exists (for RPC)
								if (index != -1) {
									types = wsdl.substring(index + ".xsd\"/>".length()+1, end);
								}
								
								Document[] docs = xsd.getAllSchemas();
								for (i=0; i<docs.length; i++) {
									Document doc = docs[i];
									if (doc != null) {
										schema = XMLUtils.prettyPrintDOMWithEncoding(doc, "UTF-8");
										try {
											prefix = "";
											x = schema.indexOf("=\"http://www.w3.org/2001/XMLSchema\"");
											while ((x>1) && ((c = schema.charAt(x-1))!= ':')) {
												prefix = c + prefix;
												if (prefix.indexOf(' ')!=-1) {
													prefix = "";
													break;
												}
												x--;
											}
										}
										catch (Exception e) {prefix= "";}
										
										prefix = prefix.equals("") ? prefix: prefix+":";
										
										if ((j=schema.indexOf("?>"))!=-1)
											schema = schema.substring(j+2);

										if ((j=schema.indexOf("<"+prefix+"schema "))!=-1)
											schema = schema.substring(j);
										
										while ((j = schema.indexOf(" schemaLocation=\"")) != -1) {
											k = schema.indexOf("\"", j+ " schemaLocation=\"".length()+1);
											if (k != -1) {
												schema = schema.substring(0,j) + schema.substring(k+1);
											}
										}
										
										String projectTargetNamespace = project.getTargetNamespace();
										if (schema.indexOf("targetNamespace=\""+projectTargetNamespace+"\"")!=-1) {
											z = schema.indexOf("<"+prefix+"complexType ");
											if (z != -1) {
												schema = schema.substring(0,z) + types + schema.substring(z);
											}
										}
										schemas += schema;
									}
								}
								//System.out.println(schemas);
								
								wsdl = wsdl.substring(0, start) + schemas + wsdl.substring(end+"</xsd:schema>".length()+1);
							}
						}
					}
				}
				catch (Exception e) {
					Engine.logEngine.error("Unable to include schema in WSDL for project \""+ projectName +"\"", e);
				}
			}
			
			return wsdl;
			
		} catch (Exception e) {
			throw new EngineException("Unable to retrieve WSDL file for project \""+ projectName +"\"",e);
		}
    }
    
    public static String generateWsdl(boolean bRPC, String servletURI, Project project) throws EngineException {
    	Engine.logEngine.debug("(WebServiceServlet) Generating WSDL...");
    	
    	String encoding = "UTF-8";
    	String projectName = project.getName();
    	String targetNameSpace = project.getTargetNamespace();
    	
        String wsdl = "";
		try {
			wsdl = new String(wsdl.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e1) {
		}
        wsdl += "<?xml version=\"1.0\" encoding=\""+ encoding +"\"?>\n";
        wsdl += "<definitions name=\"" + projectName + "\" " +
                "targetNamespace=\"" + targetNameSpace + "\" " +
                "xmlns:p_ns=\"" + targetNameSpace + "\" " +
                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" " +
                "xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" " +
                "xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
                "xmlns=\"http://schemas.xmlsoap.org/wsdl/\">\n\n";


		List<Connector> vConnectors = project.getConnectorsList();

		//RequestableObject requestable;
		String requestableName;
        
		boolean bArrayOfStringToBeDefined = false;

		// Retrieve all sequences
		List<RequestableObject> vRequestables = new LinkedList<RequestableObject>();
		vRequestables.addAll(project.getSequencesList());
		
		// Retrieve all transactions
		for (Connector connector : vConnectors)
			vRequestables.addAll(connector.getTransactionsList());

		
    	Engine.logEngine.debug("(WebServiceServlet) Generating types...");
		wsdl += "    <types>\n\n";
		if (bRPC) {
			//wsdl += "        <xsd:schema targetNamespace=\"" + targetNameSpace + "\">\n";
			wsdl += "        <xsd:schema  targetNamespace=\"" + targetNameSpace + "\" xmlns=\"http://www.w3.org/2001/XMLSchema\">\n";
			wsdl += "        <xsd:import  schemaLocation=\"http://schemas.xmlsoap.org/soap/encoding/\"  namespace=\"http://schemas.xmlsoap.org/soap/encoding/\"/>";
			
		}
		else {
			wsdl += "        <xsd:schema  elementFormDefault=\"qualified\" targetNamespace=\"" + targetNameSpace + "\">\n";
			wsdl += "        <xsd:import  schemaLocation=\"http://schemas.xmlsoap.org/soap/encoding/\"  namespace=\"http://schemas.xmlsoap.org/soap/encoding/\"/>";
		}

		for (RequestableObject requestable : vRequestables) {
			requestableName = requestable.getName();
			Connector connector = requestable.getConnector();
			String prefix = (connector == null) ? "": connector.getName()+ "__";

			if (StringUtils.normalize(requestable.getName(), true).equals(requestableName)) {
				if (requestable.isPublicMethod()) {
					Engine.logEngine.debug("(WebServiceServlet) Generating input type for requestable '" + requestableName + "'");
					
					if (!bRPC) {
						wsdl += "<xsd:element name=\"" + prefix + requestableName + "\">\n";
						wsdl += "<xsd:complexType>\n";
						wsdl += "<xsd:annotation>\n";
						wsdl += "<xsd:documentation>"+ requestable.getComment() +"</xsd:documentation>\n";
						wsdl += "</xsd:annotation>\n";
						wsdl += "<xsd:sequence>\n";
					}

					IVariableContainer container = (IVariableContainer) requestable;
					int len = container.numberOfVariables();
					RequestableVariable variable;
					String variableName;
					for (int j = 0 ; j < len ; j++) {
						variable = (RequestableVariable)container.getVariable(j);
						if (variable != null) {
							variableName = variable.getName();
							
							// Include in WSDL?
							if (variable.isWsdl().booleanValue()) {
								// Multivalued?
								if (variable.isMultiValued()) {
									bArrayOfStringToBeDefined = true;
								}

								if (!bRPC) {
									if (variable.isMultiValued()) {
										wsdl += "<xsd:element minOccurs=\"1\" maxOccurs=\"1\" name=\"" + variableName + "\" type=\"p_ns:ArrayOfString\">\n";
									}
									else {
										wsdl += "<xsd:element minOccurs=\"1\" maxOccurs=\"1\" name=\"" + variableName + "\" type=\""+ variable.getSchemaType()+"\">\n";
									}
									wsdl += "<xsd:annotation>\n";
									wsdl += "<xsd:documentation>"+ variable.getDescription() +"</xsd:documentation>\n";
									wsdl += "</xsd:annotation>\n";
									wsdl += "</xsd:element>\n";
								}
							}
						}
					}

					if (!bRPC) {
						wsdl += "</xsd:sequence>\n";
						wsdl += "</xsd:complexType>\n";
						wsdl += "</xsd:element>\n";
					}
					
					Engine.logEngine.debug("(WebServiceServlet) Generating output type for requestable '" + requestableName + "'");
					if (!bRPC) {
						wsdl += "<xsd:element name=\"" + prefix + requestableName + "Response\">\n";
						wsdl += "<xsd:complexType>\n";
						wsdl += "<xsd:sequence>\n";
						wsdl += "<xsd:element name=\"response\" type=\"p_ns:" + prefix + requestableName + "Data\"/>\n";
						wsdl += "</xsd:sequence>\n";
						wsdl += "</xsd:complexType>\n";
						wsdl += "</xsd:element>\n";
					}

					if (!(requestable instanceof HtmlTransaction)) {
						if ((requestable.wsdlType != null) && (requestable.wsdlType.length() != 0)) {
							wsdl += requestable.wsdlType + "\n";
						}
					}
				}
			}
			// import wsdlType for ALL HTML transaction!
			if (requestable instanceof HtmlTransaction) {
				if ((requestable.wsdlType != null) && (requestable.wsdlType.length() != 0)) {
					wsdl += requestable.wsdlType + "\n";
				}
			}
		}
		
		if ((bArrayOfStringToBeDefined)) {
			wsdl += getWsdlTypeForArray(bRPC) + "\n";
		}		

		wsdl += Engine.getExceptionSchema() + "\n";
		wsdl += "        </xsd:schema>\n\n";
		wsdl += "    </types>\n";

		
    	Engine.logEngine.debug("(WebServiceServlet) Generating messages...");

		for (RequestableObject requestable : vRequestables) {			
			if (requestable.isPublicMethod() && (requestable instanceof IVariableContainer)) {
				requestableName = StringUtils.normalize(requestable.getName(), true);
				Connector connector = requestable.getConnector();
				String prefix = (connector == null) ? "": connector.getName()+ "__";
				if (requestableName.equals(requestable.getName())) {
			    	Engine.logEngine.debug("(WebServiceServlet) Generating message: '" + prefix + requestableName + "SoapRequest'");
					wsdl += "    <message name=\"" + prefix + requestableName + "SoapRequest\">\n";

					if (bRPC) {
						// Including requestable variables
						IVariableContainer container = (IVariableContainer) requestable;
						int len = container.numberOfVariables();
						RequestableVariable variable;
						String variableName;
						for (int j = 0 ; j < len ; j++) {
							variable = (RequestableVariable)container.getVariable(j);
							if (variable != null) {
								variableName = variable.getName();
								// Include in WSDL?
								if (variable.isWsdl().booleanValue()) {
									// Multivalued?
									if (variable.isMultiValued()) {
										wsdl += "        <part name=\"" + variableName + "\" type=\"p_ns:ArrayOfString\"/>\n";
									}
									else {
										wsdl += "        <part name=\"" + variableName + "\" type=\""+ variable.getSchemaType() +"\"/>\n";
									}
								}
							}
						}
					}
					else {
						wsdl += "        <part name=\"parameters\" element=\"p_ns:" + prefix + requestableName + "\"/>\n";
					}

					wsdl += "    </message>\n";
			    	Engine.logEngine.debug("(WebServiceServlet) Generating message: '" + prefix + requestableName + "SoapResponse'");
					wsdl += "    <message name=\"" + prefix + requestableName + "SoapResponse\">\n";
					if (bRPC) {
						if ((requestable.wsdlType != null) && (requestable.wsdlType.length() != 0)) {
							wsdl += "        <part name=\"response\" type=\"p_ns:" + prefix + requestableName + "Response\"/>\n";
						}
						else {
							wsdl += "        <part name=\"response\" type=\"xsd:string\"/>\n";
						}
					}
					else {
						wsdl += "        <part name=\"parameters\" element=\"p_ns:" + prefix + requestableName + "Response\"/>\n";
					}
					wsdl += "    </message>\n\n";
				}
				else {
					Engine.logEngine.warn("(WebServiceServlet) Ignoring the requestable '" + requestable.getName() + "' because its name is not valid for web service.");
				}
			}
		}

    	Engine.logEngine.debug("(WebServiceServlet) Generating operations...");
        wsdl += "    <portType name=\"" + projectName + "SoapPortType\">\n";

        for (RequestableObject requestable : vRequestables) {
            Connector connector = requestable.getConnector();
            String prefix = (connector == null) ? "": connector.getName()+ "__";
            if (requestable.isPublicMethod()) {
                requestableName = StringUtils.normalize(requestable.getName(), true);
                if (requestableName.equals(requestable.getName())) {
			    	Engine.logEngine.debug("(WebServiceServlet) Generating operation: '" + prefix + requestableName + "'");
	                wsdl += "        <operation name=\"" + prefix + requestableName + "\">\n";
	                wsdl += "            <input message=\"p_ns:" + prefix + requestableName + "SoapRequest\"/>\n";
	                wsdl += "            <output message=\"p_ns:" + prefix + requestableName + "SoapResponse\"/>\n";
	                wsdl += "        </operation>\n";
				}
            }
        }

		wsdl += "    </portType>\n\n";

    	Engine.logEngine.debug("(WebServiceServlet) Generating bindings...");
        wsdl += "    <binding name=\"" + projectName + "SoapBinding\" type=\"p_ns:" + projectName + "SoapPortType\">\n\n";

        if (bRPC) {
            wsdl += "        <soap:binding style=\"rpc\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n";
        }
        else {
            wsdl += "        <soap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n";
        }

        for (RequestableObject requestable : vRequestables) {
            Connector connector = requestable.getConnector();
            String prefix = (connector == null) ? "": connector.getName()+ "__";
            if (requestable.isPublicMethod()) {
                requestableName = StringUtils.normalize(requestable.getName(), true);
				if (requestableName.equals(requestable.getName())) {
	                Engine.logEngine.debug("(WebServiceServlet) Generating binding: '" + prefix + requestableName + "'");
	                wsdl += "        <operation name=\"" + prefix + requestableName + "\">\n";
	                if (bRPC) {
	                	wsdl += "            <soap:operation soapAction=\"" + targetNameSpace + "?" + prefix + requestableName + "\"/>\n";
	                }
	                else {
	                	wsdl += "            <soap:operation soapAction=\"" + projectName + "?" + prefix + requestableName + "\"/>\n";
	                }
	                wsdl += "            <input>\n";
	                if (bRPC) {
		                wsdl += "                <soap:body use=\"encoded\" namespace=\"" + targetNameSpace + "\" encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"/>\n";
	                }
	                else {
		                wsdl += "                <soap:body use=\"literal\"/>\n";
	                }
	                wsdl += "            </input>\n";
	                wsdl += "            <output>\n";
	                if (bRPC) {
		                wsdl += "                <soap:body use=\"encoded\" namespace=\"" + targetNameSpace + "\" encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"/>\n";
	                }
	                else {
		                wsdl += "                <soap:body use=\"literal\"/>\n";
	                }
	                wsdl += "            </output>\n";
	                wsdl += "        </operation>\n";
				}
            }
        }

        wsdl += "    </binding>\n";

    	Engine.logEngine.debug("(WebServiceServlet) Generating services...");
        wsdl += "    <service name=\"" + projectName + "\">\n";
        wsdl += "        <port name=\"" + projectName + "Soap\" binding=\"p_ns:" + projectName + "SoapBinding\">\n";
        wsdl += "            <soap:address location=\"" + servletURI + "\"/>\n";
        wsdl += "        </port>\n";
        wsdl += "    </service>\n\n";

        wsdl += "</definitions>\n";

        // Update types
		if (!bRPC) {
			try {
				NodeList complexTypes;
				Element element;

				Document document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(wsdl)));

	    		complexTypes = document.getElementsByTagName("xsd:complexType");

				for (RequestableObject requestable : vRequestables) {
					requestableName = requestable.getName();
					Connector connector = requestable.getConnector();
					String prefix = (connector == null) ? "": connector.getName()+ "__";

					if (StringUtils.normalize(requestable.getName(), true).equals(requestableName)) {
						if (requestable.isPublicMethod()) {
							Engine.logEngine.debug("(WebServiceServlet) [document/literal mode] Updating input type for requestable '" + requestableName + "'");

							element = (Element) XMLUtils.findNodeByAttributeValue(complexTypes, "name", prefix + requestableName + "Response");
							
							if (element == null) {
								throw new EngineException("The requestable \"" + requestableName + "\" has been declared as public method but does not provide WSDL types.");
							}
							
							element.setAttribute("name", prefix + requestableName + "Data");
							
							//if (!(requestable instanceof HtmlTransaction)) {
							//	Node schemaNode = document.getElementsByTagName("xsd:schema").item(0);
							//	NodeList nodeList = XMLUtils.findChildNode(element, Node.ELEMENT_NODE).getChildNodes();
							//	int len = nodeList.getLength();
							//	for (int i = 0; i < len; i++) {
							//		if (nodeList.item(i) instanceof Element) {
							//			element = (Element) nodeList.item(i);
							//			String att = element.getAttribute("type");
							//			if (att.startsWith("tns")) {
							//				// Update type
							//				element.setAttribute("type", att + "_Literal");
							//				
							//				// Remove RPC type
							//				Node node = XMLUtils.findNodeByAttributeValue(complexTypes, "name", att.substring(4));
							//				schemaNode.removeChild(node);
							//			}
							//		}
							//	}
							//}
						}
					}
				}

				wsdl = XMLUtils.prettyPrintDOMWithEncoding(document,encoding);
			}
			catch(Exception e) {
				Engine.logEngine.error("(WebServiceServlet) Unable to update types for document/literal WSDL", e);
				throw new EngineException("Unable to update types for document/literal WSDL", e);
			}
		}
        
        Engine.logEngine.debug("(WebServiceServlet) WSDL generated");
        
        return wsdl;
    }
    
    private static String getWsdlTypeForArray(boolean bRPC) {
    	String wsdlType = "";

    	wsdlType += "<xsd:complexType name=\"ArrayOfString\">\n";
    	if (bRPC) {
    	/*
	    	wsdlType += "<xsd:complexContent mixed=\"false\">\n";
	    	wsdlType += "<xsd:restriction base=\"soapenc:Array\">\n";
	    	wsdlType += "<xsd:attribute d7p1:arrayType=\"xsd:string[]\" ref=\"soapenc:arrayType\" xmlns:d7p1=\"http://schemas.xmlsoap.org/wsdl/\" />";
	    	wsdlType += "</xsd:restriction>\n";
	    	wsdlType += "</xsd:complexContent>\n";
	    */
	    	wsdlType += "<xsd:complexContent>\n";
	    	wsdlType += "<xsd:restriction base=\"soapenc:Array\">\n";
	    	wsdlType += "<xsd:attribute ref=\"soapenc:arrayType\" wsdl:arrayType=\"xsd:string[]\"/>";
	    	wsdlType += "</xsd:restriction>\n";
	    	wsdlType += "</xsd:complexContent>\n";
    	}
    	else {
	    	wsdlType += "<xsd:sequence>\n";
	    	wsdlType += "<xsd:element minOccurs=\"0\" maxOccurs=\"unbounded\" name=\"item\" type=\"xsd:string\" />";
	    	wsdlType += "</xsd:sequence>\n";
    	}
    	wsdlType += "</xsd:complexType>\n";

    	return wsdlType;
    }
}
