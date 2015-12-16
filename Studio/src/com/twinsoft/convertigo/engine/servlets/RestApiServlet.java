package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.RestApiManager;
import com.twinsoft.convertigo.engine.util.SwaggerUtils;

public class RestApiServlet extends HttpServlet {

	private static final long serialVersionUID = 6926586430359873778L;

	private String buildSwaggerDefinition(String projectName, boolean isYaml) throws EngineException, JsonProcessingException {
		String definition = null;
		
		// Build a given project definition
		if (projectName != null) {
			UrlMapper urlMapper = RestApiManager.getInstance().getUrlMapper(projectName);
			if (urlMapper != null) {
				definition = isYaml ? SwaggerUtils.getYamlDefinition(urlMapper): SwaggerUtils.getJsonDefinition(urlMapper);
			}
			else {
				throw new EngineException("Project \""+projectName+"\" does not contain any UrlMapper.");
			}
		}
		// Build all project definitions
		else {
			Collection<UrlMapper> collection = RestApiManager.getInstance().getUrlMappers();
			definition = isYaml ? SwaggerUtils.getYamlDefinition(collection): SwaggerUtils.getJsonDefinition(collection);
		}
		return definition;
	}
	
	@Override
	public void destroy() {
		super.destroy();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String method = request.getMethod();
		Engine.logEngine.debug("(RestApiServlet) Requested URI: "+ method + " " + request.getRequestURI());
		
		boolean isYaml = request.getParameter("YAML") != null;
		boolean isJson = request.getParameter("JSON") != null;
				
        // Generate YAML/JSON definition (swagger specific)
		if ("GET".equalsIgnoreCase(method) && (isYaml || isJson)) {
    		try {
    			String output = buildSwaggerDefinition(request.getParameter("__project"), isYaml);
                Writer writer = response.getWriter();
                writer.write(output);

                Engine.logEngine.debug("(RestApiServlet) Definition sent :\n"+ output);
    		}
    		catch(Exception e) {
    			throw new ServletException(e);
    		}
		}
		// Handle REST request
		else {
			Collection<UrlMapper> collection = RestApiManager.getInstance().getUrlMappers();
			
			if (collection.size() > 0) {
				if (Engine.logEngine.isDebugEnabled()) {
					StringBuffer buf = new StringBuffer();
					buf.append("(RestApiServlet) Request headers:\n");
					Enumeration<String> headerNames = request.getHeaderNames();
					while (headerNames.hasMoreElements()) {
						String headerName = headerNames.nextElement();
						buf.append(" " + headerName + "=" + request.getHeader(headerName) + "\n");
					}
					Engine.logEngine.debug(buf.toString());
					
					Engine.logEngine.debug("(RestApiServlet) Request parameters: "+ 
												Collections.list(request.getParameterNames()));
				}
				
				// Found a matching operation
				UrlMappingOperation urlMappingOperation = null;
				for (UrlMapper urlMapper : collection) {
					urlMappingOperation = urlMapper.getMatchingOperation(request);
					if (urlMappingOperation != null) {
						break;
					}
				}
				
				if (urlMappingOperation != null) {
					// TODO : Handle request
					StringBuffer buf = new StringBuffer();
					buf.append("Not yet implemented...\n\n");
					buf.append("Found a matching operation for request:\n");
					buf.append(" project: " + urlMappingOperation.getProject().getName() + 
								", mapping: "+ ((UrlMapping) urlMappingOperation.getParent()).getPath() + 
								", operation: "+urlMappingOperation.getName()+"\n");

					// Write response
					Writer writer = response.getWriter();
	                writer.write(buf.toString());
					
					// Request handled
					Engine.logEngine.debug("(RestApiServlet) Request successfully handled");
				}
				else {
					Engine.logEngine.debug("(RestApiServlet) No matching operation for request");
					super.service(request, response);
				}
			}
			else {
				Engine.logEngine.debug("(RestApiServlet) No mapping defined");
				super.service(request, response);
			}
		}
	}
}
