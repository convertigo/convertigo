package com.twinsoft.convertigo.beans.rest;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
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
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractRestOperation extends UrlMappingOperation {

	private static final long serialVersionUID = 895538076484401562L;

	private transient boolean hasBodyParameter = false;
	
	@Override
	public AbstractRestOperation clone() throws CloneNotSupportedException {
		AbstractRestOperation clonedObject = (AbstractRestOperation) super.clone();
		clonedObject.hasBodyParameter = hasBodyParameter;
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
		if (hasBodyParameter) {
			Type type = parameter.getType();
			if (type != Type.Path && type != Type.Header) {
				if (type == Type.Body)
					throw new EngineException("The REST operation already contains a 'body' parameter");
				else
					throw new EngineException("The REST operation contains a 'body' parameter. You can only add 'header' parameters");
			}
		}
		else if (parameter.getType() == Type.Body) {
			for (UrlMappingParameter param: getParameterList()) {
				Type type = param.getType();
				if (type == Type.Query || type == Type.Form) {
					throw new EngineException("The REST operation contains a '"+type+"' parameter. You can not add a 'body' parameter");
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
	
	
/*
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws EngineException {
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
		
		String h_Accept = request.getHeader(HeaderName.Accept.value());
		
		String targetUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		targetUrl += (targetUrl.endsWith("/") ? "":"/") + "projects/"+ projectName + "/";
		targetUrl += h_Accept.indexOf("application/json") != -1 ? ".json":".pxml";
		
		try {
			PostMethod postMethod = null;
			String responseContentType = null;
			String content = null;
			int statusCode = -1;
			
			// Prepare PostMethod
			try {
				postMethod = new PostMethod(targetUrl);

				postMethod.setRequestHeader(HeaderName.ContentType.value(), "application/x-www-form-urlencoded;charset=UTF-8");
				
				// Add requestable parameter(s)
				if (sequenceName.isEmpty()) {
					postMethod.addParameter(Parameter.Connector.getName(), connectorName);
					postMethod.addParameter(Parameter.Transaction.getName(), transactionName);
				}
				else {
					postMethod.addParameter(Parameter.Sequence.getName(), sequenceName);
				}
				
				// Add path variables parameters
				Map<String, String> varMap = ((UrlMapping)getParent()).getPathVariableValues(request);
				if (varMap != null) {
					for (String varName: varMap.keySet()) {
						String varValue = varMap.get(varName);
						postMethod.addParameter(varName, varValue);
					}
				}
				
				// Add other parameters
				for (UrlMappingParameter param :getParameterList()) {
					String paramName = param.getName();
					Object paramValue = null;
					if (param.getType() == Type.Header) {
						paramValue = request.getHeader(paramName);
					}
					if (param.getType() == Type.Body) {
						if (request.getInputStream() != null) {
							//String contentType = request.getContentType();
							paramValue = IOUtils.toString(request.getInputStream(), "UTF-8");
						}
					}
					if ((param.getType() == Type.Query || param.getType() == Type.Form)) {
						paramValue = request.getParameterValues(paramName);
					}
					
					if (paramValue != null) {
						if (paramValue instanceof String) {
							postMethod.addParameter(paramName, (String)paramValue);
						}
						else if (paramValue instanceof String[]) {
							String[] values = (String[])paramValue;
							for (int i=0; i<values.length; i++) {
								postMethod.addParameter(paramName, values[i]);
							}
						}
					}
					else if (param.isRequired()) {
						Engine.logBeans.warn("(AbstractRestOperation) \""+ getName() +"\" : missing parameter "+ param.getName());
					}
				}
			}
			catch (IOException ioe) {
				Engine.logBeans.error("(AbstractRestOperation) \""+ getName() +"\" : invalid body", ioe);
				throw ioe;
			}
			
			// Execute POST
			if (postMethod != null) {
				try {
					// Set HostConfiguration
					URL url = new URL(targetUrl);
					HostConfiguration hostConfiguration = new HostConfiguration();
					hostConfiguration.setHost(url.getHost());
					
					// Set/Store HttpState
					HttpState httpState = (HttpState) request.getSession().getAttribute("c8o_httpState");
					if (httpState == null) {
						httpState = new HttpState();
						request.getSession().setAttribute("c8o_httpState", httpState);
					}
					
					// Request Headers
					if (Engine.logBeans.isTraceEnabled()) {
						Header[] requestHeaders = postMethod.getRequestHeaders();
						StringBuffer buf = new StringBuffer();
						buf.append("(AbstractRestOperation) \""+ getName() +"\" requestable request headers:\n");
						for (Header header: requestHeaders) {
							buf.append(" " + header.getName() + "=" + header.getValue() + "\n");
						}
						Engine.logBeans.trace(buf.toString());
					}
					
					// Invoke requestable
					Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\" executing requestable \""+ targetRequestableQName +"\"");
					statusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, postMethod, httpState);
					Engine.logBeans.debug("(AbstractRestOperation) \""+ getName() +"\" requestable response status code: "+ statusCode);
					
					// Response Headers
					if (Engine.logBeans.isTraceEnabled()) {
						Header[] responseHeaders = postMethod.getResponseHeaders();
						StringBuffer buf = new StringBuffer();
						buf.append("(AbstractRestOperation) \""+ getName() +"\" requestable response headers:\n");
						for (Header header: responseHeaders) {
							buf.append(" " + header.getName() + "=" + header.getValue() + "\n");
						}
						Engine.logBeans.trace(buf.toString());
					}
					
					// Retrieve response Content-Type
					Header h_ContentType = postMethod.getResponseHeader(HeaderName.ContentType.value());
					if (h_ContentType != null) {
						responseContentType = h_ContentType.getValue();
					}
					
					// Retrieve response content
					if (statusCode != -1) {
						content = postMethod.getResponseBodyAsString();
					}
					Engine.logBeans.trace("(AbstractRestOperation) \""+ getName() +"\" requestable response content:\n"+ content + "\n");
					
				}
				catch (MalformedURLException e) {
					Engine.logBeans.error("(AbstractRestOperation) \""+ getName() +"\" : invalid URL", e);
					throw e;
				} catch (HttpException e) {
					Engine.logBeans.error("(AbstractRestOperation) \""+ getName() +"\" : http invoke failed", e);
					throw e;
				} catch (IOException e) {
					Engine.logBeans.error("(AbstractRestOperation) \""+ getName() +"\" : get response body failed", e);
					throw e;
				}
				finally {
					if (postMethod != null) {
						postMethod.releaseConnection();
					}
				}
			}
			
			//TODO : analyse/modify status/content with Response beans
			
            // Set response status
			response.setStatus(statusCode);
			
			// Set response content-type header
			if (responseContentType != null) {
				response.addHeader(HeaderName.ContentType.value(), responseContentType);
			}
			
			// Set response content
			if (content != null) {
				Writer writer = response.getWriter();
	            writer.write(content);
			}
		}
		catch (Throwable t) {
			throw new EngineException("Operation \""+ getName() +"\" failed to handle request", t);
		}
	}
*/	
	
	@Override
	@SuppressWarnings("deprecation")
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws EngineException {
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
							map.put(parameterName, parameterValue);
						}
					}
				}
				
				String contextName = request.getParameter(Parameter.Context.getName());
				String sessionId = request.getSession().getId();
				
		    	map.put(Parameter.Context.getName(), new String[] { contextName });
		    	map.put(Parameter.SessionId.getName(), new String[] { sessionId });

		    	map.put(Parameter.Project.getName(), new String[] { projectName });
				if (sequenceName.isEmpty()) {
					map.put(Parameter.Connector.getName(), new String[] { connectorName });
					map.put(Parameter.Transaction.getName(), new String[] { transactionName });
				}
				else {
					map.put(Parameter.Sequence.getName(), new String[] { sequenceName });
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
						paramValue = request.getParameterValues(paramName);
					}
					else if (param.getType() == Type.Path) {
						paramValue = varMap.get(param.getName());
					}
					else if (param.getType() == Type.Body) {
						if (request.getInputStream() != null) {
							// Retrieve data
							paramValue = IOUtils.toString(request.getInputStream(), "UTF-8");
			        		
							// Get input content type
			        		DataContent dataInput = param.getInputContent();
			        		if (dataInput.equals(DataContent.useHeader)) {
			        			String requestContentType = request.getContentType();
			        			if (requestContentType == null || requestContentType.indexOf("application/json") != -1) {
			        				dataInput = DataContent.toJson;
			        			}
			        			if (requestContentType == null || requestContentType.indexOf("application/xml") != -1) {
			        				dataInput = DataContent.toXml;
			        			}
			        		}
			        		
			        		// Transform input data
			        		try {
				        		if (dataInput.equals(DataContent.toJson)) {
				        			String objectName = variableName.isEmpty() ? paramName : variableName;
				        			Document doc = XMLUtils.parseDOMFromString("<"+objectName+"/>");
				        			Element root = doc.getDocumentElement();
				        			XMLUtils.JsonToXml(new JSONObject((String)paramValue), root);
				        			paramValue = root.getChildNodes();
				        		}
				        		else if (dataInput.equals(DataContent.toXml)) {
				        			Document doc = XMLUtils.parseDOMFromString((String)paramValue);
				        			paramValue = doc.getDocumentElement();
				        		}
			        		}
			        		catch (Exception e) {
			        			Engine.logBeans.error("(AbstractRestOperation) \""+ getName() +"\" : unable to decode body", e);
			        		}
						}
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
        	InternalRequester internalRequester = new InternalRequester(request);
        	internalRequester.setStrictMode(getProject().isStrictMode());
    		Object result = internalRequester.processRequest(map);
        	if (result != null) {
        		Document xmlHttpDocument = (Document) result;
        		
        		// Get output content type
        		DataContent dataOutput = getOutputContent();
        		if (dataOutput.equals(DataContent.useHeader)) {
            		String h_Accept = request.getHeader(HeaderName.Accept.value());
        			if (h_Accept == null || h_Accept.indexOf("application/xml") != -1) {
        				dataOutput = DataContent.toXml;
        			}
        			if (h_Accept == null || h_Accept.indexOf("application/json") != -1) {
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
            		content = XMLUtils.XmlToJson(xmlHttpDocument.getDocumentElement(), true, useType, jsonRoot);
            		responseContentType = "application/json";
        		}
        		else {
        			content = XMLUtils.prettyPrintDOMWithEncoding(xmlHttpDocument, "UTF-8");
            		responseContentType = "application/xml";
        		}
        	}
        	else {
        		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        	}
			
			// Set response content-type header
			if (responseContentType != null) {
				response.addHeader(HeaderName.ContentType.value(), responseContentType);
			}
			
			// Set response content
			if (content != null) {
				Writer writer = response.getWriter();
	            writer.write(content);
			}
		}
		catch (Throwable t) {
			throw new EngineException("Operation \""+ getName() +"\" failed to handle request", t);
		}
	}
}
