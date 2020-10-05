/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.beans.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataContent;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataType;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.Type;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractRestOperation extends UrlMappingOperation {

	private static final long serialVersionUID = 895538076484401562L;

	private transient boolean hasBodyParameter = false;
	
	private boolean terminateSession = true;
	
	@Override
	public AbstractRestOperation clone() throws CloneNotSupportedException {
		AbstractRestOperation clonedObject = (AbstractRestOperation) super.clone();
		clonedObject.hasBodyParameter = false;
		return clonedObject;
	}
	
	@Override
	protected boolean canAddParameter(UrlMappingParameter parameter) {
		Type type = parameter.getType();
		String method = getMethod();
		if (method.equalsIgnoreCase(HttpMethodType.GET.name()) ||
			method.equalsIgnoreCase(HttpMethodType.HEAD.name()) ||
			method.equalsIgnoreCase(HttpMethodType.DELETE.name())) {
			if (type != Type.Path && type != Type.Query && type != Type.Header) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected void addParameter(UrlMappingParameter parameter) throws EngineException {
		List<UrlMappingParameter> l = getParameterList();
		if (hasBodyParameter) {
			Type type = parameter.getType();
			if (type != Type.Path && type != Type.Header) {
				if (type == Type.Body) {
					throw new EngineException("The REST operation already contains a 'body' parameter");
				}
				else {
					if (!isChangeTo || (isChangeTo && l.size() != 1)) {
						throw new EngineException("The REST operation contains a 'body' parameter. You can only add 'header' parameters");
					}
				}
			}
		}
		else if (parameter.getType() == Type.Body) {
			for (UrlMappingParameter param: l) {
				Type type = param.getType();
				if (type == Type.Query || type == Type.Form) {
					if (!isChangeTo || (isChangeTo && l.size() != 1)) {
						throw new EngineException("The REST operation contains a '"+type+"' parameter. You can not add a 'body' parameter");
					}
				}
			}
		}
		super.addParameter(parameter);
		if (!hasBodyParameter && parameter.getType() == Type.Body) {
			hasBodyParameter = true;
		}
	}


	@Override
	protected void removeParameter(UrlMappingParameter parameter) throws EngineException {
		super.removeParameter(parameter);
		if (hasBodyParameter && parameter.getType() == Type.Body) {
			hasBodyParameter = false;
		}
	}

	private DataContent outputContent = DataContent.toJson;
	
	public DataContent getOutputContent() {
		return outputContent;
	}

	public void setOutputContent(DataContent outputContent) {
		this.outputContent = outputContent;
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public String handleRequest(HttpServletRequest request, HttpServletResponse response) throws EngineException {
		String targetRequestableQName = getTargetRequestable();
		if (targetRequestableQName.isEmpty()) {
			throw new EngineException("Mapping operation \""+ getName() +"\" has no target requestable defined");
		}
		
		StringTokenizer st = new StringTokenizer(targetRequestableQName,".");
		int count = st.countTokens();
		String projectName = st.nextToken();
		String sequenceName = count == 2 ? st.nextToken():"";
		String connectorName = count == 3 ? st.nextToken():"";
		String transactionName = count == 3 ? st.nextToken():"";
		
		try {
			
			Map<String, Object> map = new HashMap<String, Object>();
			String responseContentType = null;
			String content = null;
			
			try {
				
				// Check multipart request
				if (ServletFileUpload.isMultipartContent(request)) {
					Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\" Multipart resquest");
		
					// Create a factory for disk-based file items
					DiskFileItemFactory factory = new DiskFileItemFactory();
		
					// Set factory constraints
					factory.setSizeThreshold(1000);
		
					File temporaryFile = File.createTempFile("c8o-multipart-files", ".tmp");
					int cptFile = 0;
					temporaryFile.delete();
					temporaryFile.mkdirs();
					factory.setRepository(temporaryFile);
					Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\" Temporary folder for upload is : " + temporaryFile.getAbsolutePath());
		
					// Create a new file upload handler
					ServletFileUpload upload = new ServletFileUpload(factory);
		
					// Set overall request size constraint
					upload.setSizeMax(EnginePropertiesManager.getPropertyAsLong(PropertyName.FILE_UPLOAD_MAX_REQUEST_SIZE));
					upload.setFileSizeMax(EnginePropertiesManager.getPropertyAsLong(PropertyName.FILE_UPLOAD_MAX_FILE_SIZE));
		
					// Parse the request
					List<FileItem> items = GenericUtils.cast(upload.parseRequest(request));
					for (FileItem fileItem : items) {
						String parameterName = fileItem.getFieldName();
						String parameterValue;
						if (fileItem.isFormField()) {
							parameterValue = fileItem.getString();
							Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\"  Value for field '" + parameterName + "' : " + parameterValue);
						} else {
							String name = fileItem.getName().replaceFirst("^.*(?:\\\\|/)(.*?)$", "$1");
							if (name.length() > 0) {
								File wDir = new File(temporaryFile, "" + (++cptFile));
								wDir.mkdirs();
								File wFile = new File(wDir, name);
								fileItem.write(wFile);
								fileItem.delete();
								parameterValue = wFile.getAbsolutePath();
								Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\" Temporary uploaded file for field '" + parameterName + "' : " + parameterValue);
							} else {
								Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\" No temporary uploaded file for field '" + parameterName + "', empty name");
								parameterValue = "";
							}
						}
		
						if (parameterValue != null && !parameterValue.isEmpty()) {
							UrlMappingParameter param = null;
							try {
								param = getParameterByName(parameterName);
							} catch (Exception e) {}
							if (param != null) {
								String variableName = param.getMappedVariableName();
								if (!variableName.isEmpty()) {
									parameterName = variableName;
								}
							}
							
							Object mapValue = map.get(parameterName);
							if (mapValue == null) {
								map.put(parameterName, parameterValue);
							} else {
								List<String> values = new ArrayList<String>();
								if (mapValue instanceof String) {
									values.add((String)mapValue);
								}
								else if (mapValue instanceof List) {
									values.addAll(GenericUtils.cast(mapValue));
								}
								values.add(parameterValue);
								map.put(parameterName, values);
							}
						}
					}
				}
				
				String contextName = request.getParameter(Parameter.Context.getName());
				
		    	map.put(Parameter.Context.getName(), new String[] { contextName });

		    	map.put(Parameter.Project.getName(), new String[] { projectName });
				if (sequenceName.isEmpty()) {
					map.put(Parameter.Connector.getName(), new String[] { connectorName });
					map.put(Parameter.Transaction.getName(), new String[] { transactionName });
				}
				else {
					map.put(Parameter.Sequence.getName(), new String[] { sequenceName });
					map.put(Parameter.RemoveContext.getName(), new String[] { "" });
					map.put(Parameter.RemoveSession.getName(), new String[] { "" });
				}
				
				// Add path variables parameters
				Map<String, String> varMap = ((UrlMapping)getParent()).getPathVariableValues(request);
				for (String varName: varMap.keySet()) {
					String varValue = varMap.get(varName);
					map.put(varName, varValue);
				}
				
				// Add other parameters
				for (UrlMappingParameter param :getParameterList()) {
					String paramName = param.getName();
					String variableName = param.getMappedVariableName();
					
					Object paramValue = null;
					if (param.getType() == Type.Header) {
						paramValue = request.getHeader(paramName);
					}
					else if ((param.getType() == Type.Query || param.getType() == Type.Form)) {
						String[] pvalues = request.getParameterValues(paramName);
						if (pvalues != null) {
							paramValue = pvalues;
						}
					}
					else if (param.getType() == Type.Path) {
						String varValue = varMap.get(param.getName());
						paramValue = varValue;
					}
					else if (param.getType() == Type.Body) {
						if (request.getInputStream() != null) {
							// Retrieve data
							paramValue = IOUtils.toString(request.getInputStream(), "UTF-8");
			        		
							// Get input content type
			        		DataContent dataInput = param.getInputContent();
			        		if (dataInput.equals(DataContent.useHeader)) {
			        			String requestContentType = request.getContentType();
			        			if (requestContentType == null || MimeType.Xml.is(requestContentType)) {
			        				dataInput = DataContent.toXml;
			        			} else if (MimeType.Json.is(requestContentType)) {
			        				dataInput = DataContent.toJson;
			        			}
			        		}
			        		
			        		// Transform input data
			        		try {
				        		if (dataInput.equals(DataContent.toJson)) {
				        			//String modelName = param instanceof IMappingRefModel ? ((IMappingRefModel)param).getModelReference() : "";
				        			//String objectName = modelName.isEmpty() ? paramName : modelName;
				        			//Document doc = XMLUtils.parseDOMFromString("<"+objectName+"/>");
				        			Document doc = XMLUtils.parseDOMFromString("<"+paramName+"/>");
				        			Element root = doc.getDocumentElement();
				        			JSONObject json = new JSONObject((String) paramValue);
				        			XMLUtils.jsonToXml(json, root);
				        			paramValue = root.getChildNodes();
				        		}
				        		else if (dataInput.equals(DataContent.toXml)) {
				        			//Document doc = XMLUtils.parseDOMFromString((String)paramValue);
				        			//paramValue = doc.getDocumentElement();
				        			Document xml = XMLUtils.parseDOMFromString((String)paramValue);
				        			if (xml.getDocumentElement().getTagName().equals(paramName)) {
				        				paramValue = xml.getDocumentElement();
				        			} else {
					        			NodeList nl = xml.getDocumentElement().getChildNodes();
					        			Document doc = XMLUtils.parseDOMFromString("<"+paramName+"/>");
					        			Element root = doc.getDocumentElement();
					        			for (int i = 0 ; i < nl.getLength() ; i++) {
					        				Node node = nl.item(i);
					        				if (node.getNodeType() == Node.ELEMENT_NODE) {
					        					root.appendChild(doc.adoptNode(node));
					        				}
					        			}
					        			paramValue = doc.getDocumentElement();
				        			}
				        		}
			        		}
			        		catch (Exception e) {
			        			Engine.logBeans.error("(AbstractRestOperation) \""+ getName() +"\" : unable to decode body", e);
			        		}
						}
					}
					
					// retrieve default value if necessary
					if (paramValue == null) {
						paramValue = param.getValueOrNull();
					}
					
					if (paramValue != null) {
						// map parameter to variable
						if (!variableName.isEmpty()) {
							paramName = variableName;
						}
						
						// add parameter with value to input map
						if (paramValue instanceof String) {
							map.put(paramName, new String[] { paramValue.toString() });
						}
						else if (paramValue instanceof String[]) {
							String[] values = (String[])paramValue;
							map.put(paramName, values);
						}
						else {
							map.put(paramName, paramValue);
						}
					}
					else if (param.isRequired()) {
						if (param.getType() == Type.Path) {
							// ignore : already handled
						}
						else if (param.getDataType().equals(DataType.File)) {
							// ignore : already handled
						}
						else {
							Engine.logBeans.warn("(AbstractRestOperation) \""+ getName() +"\" : missing parameter "+ param.getName());
						}
					}
				}
			}
			catch (IOException ioe) {
				Engine.logBeans.error("(AbstractRestOperation) \""+ getName() +"\" : invalid body", ioe);
				throw ioe;
			}
			
			// Execute requestable
			Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\" executing requestable \""+ targetRequestableQName +"\"");
        	InternalRequester internalRequester = new InternalRequester(map, request);
			request.setAttribute("convertigo.requester", internalRequester);
    		Object result = internalRequester.processRequest();
    		String encoding = "UTF-8";
    		if (result != null) {
        		Document xmlHttpDocument = (Document) result;
        		
				// Extract the encoding Char Set from PI
    			Node firstChild = xmlHttpDocument.getFirstChild();
    			if ((firstChild.getNodeType() == Document.PROCESSING_INSTRUCTION_NODE)
    					&& (firstChild.getNodeName().equals("xml"))) {
    				String piValue = firstChild.getNodeValue();
    				int encodingOffset = piValue.indexOf("encoding=\"");
    				if (encodingOffset != -1) {
    					encoding = piValue.substring(encodingOffset + 10);
    					encoding = encoding.substring(0, encoding.length() - 1);
    				}
    			}
        		
        		// Get output content type
        		DataContent dataOutput = getOutputContent();
        		if (dataOutput.equals(DataContent.useHeader)) {
            		String h_Accept = HeaderName.Accept.getHeader(request);
            		if (MimeType.Xml.is(h_Accept)) {
            			dataOutput = DataContent.toXml;
        			} else if (h_Accept == null || MimeType.Json.is(h_Accept)) {
        				dataOutput = DataContent.toJson;
        			}
        		}
        		
        		// Modify status according to XPath condition of Response beans        		
        		int statusCode = HttpServletResponse.SC_OK;
        		String statusText = "";
        		if (RequestAttribute.responseStatus.get(request) == null) {
	        		for (UrlMappingResponse umr : getResponseList()) {
	        			if (umr instanceof OperationResponse) {
	        				OperationResponse or = (OperationResponse)umr;
	        				if (or.isMatching(xmlHttpDocument)) {
	        					try {
	        						statusCode = Integer.valueOf(or.getStatusCode(),10);
	        						statusText = or.getStatusText();
	        					}
	        					catch (Exception e) {}
	        					break;
	        				}
	        			}
	        		}
        		}
        		if (statusText.isEmpty()) response.setStatus(statusCode);
        		else response.setStatus(statusCode, statusText);
        		
        		// Transform XML data
        		if (dataOutput.equals(DataContent.toJson)) {
        			JsonRoot jsonRoot = getProject().getJsonRoot();
        			boolean useType = getProject().getJsonOutput() == JsonOutput.useType;
        			Document document = useType ? Engine.theApp.schemaManager.makeXmlRestCompliant(xmlHttpDocument) : xmlHttpDocument;
    				XMLUtils.logXml(document, Engine.logContext, "Generated Rest XML (useType="+ useType +")");
       				content = XMLUtils.XmlToJson(document.getDocumentElement(), true, useType, jsonRoot);
            		responseContentType = MimeType.Json.value();
        		}
        		else {
        			content = XMLUtils.prettyPrintDOMWithEncoding(xmlHttpDocument, "UTF-8");
            		responseContentType = MimeType.Xml.value();
        		}
        	}
        	else {
        		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        	}
			
			// Set response content-type header
			if (responseContentType != null) {
				HeaderName.ContentType.addHeader(response, responseContentType);
			}
			
			// Set response content
			if (content != null) {
				response.setCharacterEncoding(encoding);
				if (Engine.logContext.isInfoEnabled()) {
					try {
						String json = new JSONObject(content).toString(1);
						int len = json.length();
						if (len > 5000) {
							String txt = json.substring(0, 5000) + "\n... (see the complete message in DEBUG log level)";
							Engine.logContext.info("Generated REST Json:\n"+ txt);
							Engine.logContext.debug("Generated REST Json:\n"+ json);
						} else {
							Engine.logContext.info("Generated REST Json:\n"+ json);
						}
					} catch (Exception e ) {}
				}
			}
			return content;
		}
		catch (Throwable t) {
			throw new EngineException("Operation \""+ getName() +"\" failed to handle request", t);
		} finally {
			if (terminateSession) {
				request.setAttribute("convertigo.requireEndOfContext", true);
			}
		}
	}

	public boolean isTerminateSession() {
		return terminateSession;
	}

	public void setTerminateSession(boolean terminateSession) {
		this.terminateSession = terminateSession;
	}
}
