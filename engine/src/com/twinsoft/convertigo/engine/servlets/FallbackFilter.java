/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
import com.twinsoft.convertigo.engine.util.Log4jHelper;

public class FallbackFilter implements Filter {
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain filterChain) throws IOException, ServletException {
	    HttpServletRequest request = (HttpServletRequest) _request;
	    HttpServletResponse response = (HttpServletResponse) _response;
		Log4jHelper.mdcClear();
	    var servletPath = request.getServletPath();
	    if (servletPath.endsWith("/")) {
	        servletPath += "index.html";
	    }
	    if (servletPath.endsWith("/index.html") && !new File(Engine.WEBAPP_PATH, servletPath).exists()) {
	        var segments = servletPath.split("/");
	        var currentPath = new StringBuilder();

	        for (var i = 1; i < segments.length; i++) { // skip the first empty "/"
	            currentPath.append("/");
	            
	            if (new File(Engine.WEBAPP_PATH, currentPath.toString() + segments[i]).exists()) {
	            	currentPath.append(segments[i]);
	            } else if (new File(Engine.WEBAPP_PATH, currentPath.toString() + "_").exists()) {
					currentPath.append("_");
				} else {
					currentPath = null;
					break;
	            }
	        }
	        
	        if (currentPath != null && new File(Engine.WEBAPP_PATH, currentPath.toString()).exists()) {
	            request.getRequestDispatcher(currentPath.toString()).forward(request, response);
	            return;
	        }
	    }
	    
	    filterChain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

}
