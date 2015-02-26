package com.twinsoft.convertigo.engine.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.Engine;

public class StoreDataFilter implements Filter {
    private FilterConfig filterConfig = null;
    
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		System.out.println("Store data filter has been initialized");
	}
	
	public void destroy() {
		this.filterConfig = null;
	}
	
    public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) _request;
    	HttpServletResponse response = (HttpServletResponse) _response;
    	
    	String requestedObject = Engine.USER_WORKSPACE_PATH + request.getRequestURI().replaceFirst(".*?(/store)", "$1");
    	if (requestedObject.endsWith("/")) {
    		requestedObject += "index.html";
    	}
    	
    	File file = new File(requestedObject);
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
	    		String mimeType = filterConfig.getServletContext().getMimeType(file.getName());
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
    	}
    	else {
    	    Engine.logContext.debug("Convertigo request => follow the normal filter chain");
    	    chain.doFilter(request, response);
    	}
    }
}
