package com.twinsoft.convertigo.beans.rest;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.Type;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.Parameter;

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
		
		String h_Accept = request.getHeader(HeaderName.Accept.name());
		
		String targetUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		targetUrl += (targetUrl.endsWith("/") ? "":"/") + "projects/"+ projectName + "/";
		targetUrl += h_Accept.indexOf("application/json") != -1 ? ".json":".pxml";
		
		try {
			PostMethod postMethod = null;
			String responseContenType = null;
			String content = null;
			int statusCode = -1;
			
			// Prepare PostMethod
			try {
				postMethod = new PostMethod(targetUrl);

				/* HEADERS */
				postMethod.setRequestHeader(HeaderName.ContentType.name(), "application/x-www-form-urlencoded;charset=UTF-8");
				
				/* PARAMETERS */
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
				for (String varName: varMap.keySet()) {
					String varValue = varMap.get(varName);
					postMethod.addParameter(varName, varValue);
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
					
					// Retrieve response Content-Type
					Header h_ContentType = postMethod.getResponseHeader(HeaderName.ContentType.name());
					if (h_ContentType != null) {
						responseContenType = h_ContentType.getValue();
					}
					
					// Retrieve response content
					if (statusCode != -1) {
						content = postMethod.getResponseBodyAsString();
					}
					Engine.logBeans.trace("(AbstractRestOperation) \""+ getName() +"\" requestable response content:\n"+ content + "\n");
					
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
			if (responseContenType != null) {
				response.addHeader(HeaderName.ContentType.name(), responseContenType);
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
