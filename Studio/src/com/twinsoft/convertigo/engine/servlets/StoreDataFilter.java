package com.twinsoft.convertigo.engine.servlets;

import java.io.File;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ServletUtils;

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

    	String suffix = request.getRequestURI().replaceFirst(".*?(/store)", "$1");
    	
    	File file = getStoreFile(Engine.USER_WORKSPACE_PATH + suffix);
    	
    	if (!file.exists()) {
    		file = getStoreFile(Engine.WEBAPP_PATH + "/WEB-INF" + suffix);
    	}

    	ServletUtils.handleFileFilter(file, request, response, filterConfig, chain);
    }
    
    private File getStoreFile(String requestedObject) {
    	if (requestedObject.endsWith("/")) {
    		requestedObject += "index.html";
    	}
    	
    	return new File(requestedObject);
    }
}
