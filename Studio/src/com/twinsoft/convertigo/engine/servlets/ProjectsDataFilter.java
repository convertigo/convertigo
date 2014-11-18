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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.ResourceCompressorManager;

public class ProjectsDataFilter implements Filter {
	private static Pattern p_projects = Pattern.compile("/projects(/.*)");
	private static Pattern p_forbidden = Pattern.compile("^/(?:([^/]+$)|(?:(.*?)/\\2\\.xml)|(?:(?:.*?)/_private(?:$|(?!/mobile/flashupdate_).*)))$");
	
    public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain) throws IOException, ServletException {
    	boolean hide_error = EnginePropertiesManager.getProperty( PropertyName.HIDING_ERROR_INFORMATION ).equals( "true" );
    	Engine.logContext.debug("Entering projects data servlet filter");

		HttpServletRequest request = (HttpServletRequest) _request;
    	HttpServletResponse response = (HttpServletResponse) _response;

    	if (SiteClipperConnector.handleRequest(request, response)) {
    		return;
    	}
    	
		boolean bProjectsDataCompatibilityMode = Boolean.parseBoolean(EnginePropertiesManager
				.getProperty(PropertyName.PROJECTS_DATA_COMPATIBILITY_MODE));

    	Engine.logContext.debug("bProjectsDataCompatibilityMode=" + bProjectsDataCompatibilityMode);
    	
    	if (bProjectsDataCompatibilityMode) {
        	Engine.logContext.debug("Projects data compatibility mode => follow the normal filter chain");
        	chain.doFilter(request, response);
        	return;
    	}
    	
    	ServletContext servletContext = filterConfig.getServletContext();
    	
    	String requestURI = request.getRequestURI();
    	Engine.logContext.debug("requestURI=" + requestURI);
    	
    	// Get a canonicalized form of the request URL, i.e. resolve all ".", "..", "///"...
    	URL url = new URL(request.getRequestURL().toString());
    	
    	try {
    		requestURI = url.toURI().normalize().getPath();
    	} catch (URISyntaxException e) {
    		// should never occur
    	}
    	
    	Matcher m_projects = p_projects.matcher(requestURI);
    	String pathInfo = (m_projects.find()) ? m_projects.group(1) : "";

    	String requestedObject = Engine.PROJECTS_PATH + pathInfo;
    	Engine.logContext.debug("requestedObject=" + requestedObject);
    	
    	Matcher m_forbidden = p_forbidden.matcher(pathInfo); 
    	if (m_forbidden.matches() && (m_forbidden.group(1) == null || !(new File(requestedObject).isDirectory()))) {
    		if (hide_error == false) 
    			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
    	}
    	
    	if (requestedObject.endsWith("/index.jsp")) {
    		requestedObject = requestedObject.replaceAll("index.jsp", "index.html");
        	Engine.logContext.debug("index.jps remapped to '" + requestedObject + "'");
    	}
    	else if (requestedObject.endsWith(".jsp")) {
    		if (hide_error == false) {
    			Engine.logContext.error("JSP required, but not allowed! (" + requestURI + ")");
    			response.sendError(HttpServletResponse.SC_FORBIDDEN, requestURI + ": JSP is not allowed!");
    		}
    		return;
    	}
    	
    	File file = new File(requestedObject);
    	
    	String s = file.getCanonicalPath();
    	if (!s.startsWith(Engine.PROJECTS_PATH)) {
    		if (hide_error == false) {
    			Engine.logContext.error(requestURI + ": access to directories outside the projects repository is not allowed!");
    			response.sendError(HttpServletResponse.SC_FORBIDDEN, requestURI + ": access to directories outside the projects repository is not allowed!");
    		}
    		return;
    	}
    	
    	// Handle implicit document (index.html)
    	if (file.exists() && file.isDirectory()) {
    		// Handle ".../projects" requests
    		s = file.getCanonicalPath();
    		if (s.endsWith("projects") || s.equals(Engine.PROJECTS_PATH)) {
    			if (requestURI.endsWith("/")) {
			    	Engine.logContext.debug("Send redirect to: '../index.html'");
					response.sendRedirect("../index.html");
    			} else {
			    	Engine.logContext.debug("Send redirect to: 'index.html'");
					response.sendRedirect("index.html");
    			}
    			return;
    		}
    		else {
    			if (requestURI.endsWith("/"))
	    			file = new File(requestedObject + "/index.html");
				else {
					String redirect = requestURI + "/index.html";
			    	Engine.logContext.debug("Send redirect to: '" + redirect + "'");
					response.sendRedirect(redirect);
    				return;
				}
    		}
    	}
    	
    	if (!file.exists() && Engine.theApp != null && Engine.theApp.resourceCompressorManager != null) {
    		if (Engine.theApp.resourceCompressorManager.check(request, response)) {
    			return;
    		} else {
    			File commonResource = ResourceCompressorManager.getCommonCssResource(request);
    			if (commonResource != null) {
    				file = commonResource;
    			}
    		}
    	}
    	
    	if (file.exists()) {
        	Engine.logContext.debug("Static file");
        	
        	// Warning date comparison: 'If-Modified-Since' header precision is second,
        	// although file date precision is milliseconds on Windows
        	long clientDate = request.getDateHeader("If-Modified-Since") / 1000;
    		Engine.logContext.debug("If-Modified-Since: " + clientDate);
    		long fileDate = file.lastModified() / 1000;
    		Engine.logContext.debug("File date: " + fileDate);
        	if (clientDate == fileDate) {
        		Engine.logContext.debug("Returned HTTP 304 Not Modified");
        		response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        	} else {
	    		// Serve static files if they exist in the projects repository.
	    		String mimeType = servletContext.getMimeType(requestURI);
	        	Engine.logContext.debug("Found MIME type: " + mimeType);
	    		response.setHeader("Content-type", mimeType);
	    		response.setHeader("Cache-Control", "public");
	    		response.setDateHeader("Last-Modified", file.lastModified());
	
	    		FileInputStream fileInputStream = null;
	    		OutputStream output = response.getOutputStream();
	    		try {
	        		fileInputStream = new FileInputStream(file);
	        		IOUtils.copy(fileInputStream, output);
	    		}
	    		finally {
	    			if (fileInputStream != null) {
	    				fileInputStream.close();
	    			}
	    		}
        	}
    	} else {
    	    Engine.logContext.debug("Convertigo request => follow the normal filter chain");
    	    chain.doFilter(request, response);
    	}

    	Engine.logContext.debug("Exiting projects data filter");
    }

    private FilterConfig filterConfig = null;

	public void destroy() {
		this.filterConfig = null;
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		System.out.println("Projects data filter has been initialized");
	}
}
