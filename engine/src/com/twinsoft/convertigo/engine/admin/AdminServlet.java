/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticationException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.Service;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;

/**
 * Servlet implementation class AdminServlet
 */
public class AdminServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AdminServlet() {
        super();
    }

    @Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String corsOrigin = HttpUtils.applyCorsHeaders(request, response);
		if (corsOrigin != null) {
			Engine.logAdmin.trace("Add CORS header for: " + corsOrigin);
		}
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		boolean show_error = false;
		try {
			show_error = !EnginePropertiesManager.getProperty(PropertyName.HIDING_ERROR_INFORMATION).equals("true");
		} catch (Exception e) {
			Engine.logAdmin.debug("Failed to retrieve property HIDING_ERROR_INFORMATION: " + e.getClass() + " (" + e.getMessage() + ")");
		}
				
		try {
			String serviceName = "";
			String isAdmin = "";
			try {
				response.addHeader("Expires", "-1");
    			response.addHeader("Pragma", "no-cache");
	    		
    			request.setCharacterEncoding("UTF-8");
    			
				String requestURL = request.getRequestURL().toString();
				int i = requestURL.lastIndexOf('/');
				isAdmin = requestURL.substring(0, i).endsWith("/admin/services") ? "admin " : "";
				serviceName = requestURL.substring(i + 1); 
				if (serviceName != null && !serviceName.equals("logs.Get")) {
					Engine.logAdmin.info("Service name: " + serviceName);
				}
				
				String myPackage = this.getClass().getPackage().getName();
				Class<?> serviceClass = Class.forName(myPackage + ".services." + serviceName);
				
				// Check for authentication and roles
				ServiceDefinition serviceDefinition = serviceClass.getAnnotation(ServiceDefinition.class);
				
				if (serviceDefinition == null)
					throw new IllegalArgumentException("The service '" + serviceName + "' has no service definition!");
				
				if (Engine.isCloudMode()) {
					boolean cloud_forbidden = serviceDefinition.cloud_forbidden();
					Engine.logAdmin.debug("Is service forbidden for Cloud ? : " + cloud_forbidden);
					if (cloud_forbidden) {
						throw new EngineException("The service '" + serviceName + "' cannot be acceded on Cloud.");
					}
				}
				
				if (isAdmin.isEmpty() && serviceDefinition.admin()) {
					throw new ClassNotFoundException();
				}
				
				String corsOrigin = HttpUtils.applyCorsHeaders(request, response);
				if (corsOrigin != null) {
					Engine.logAdmin.trace("Add CORS header for: " + corsOrigin);
				}
				
				boolean needsAuthentication = !AuthenticatedSessionManager.hasRole(serviceDefinition.roles(), Role.ANONYMOUS);
				Engine.logAdmin.debug("Needs authentication: " + needsAuthentication);
				if (needsAuthentication) {
					Engine.authenticatedSessionManager.checkRoles(request.getSession(), serviceDefinition.roles());
				}
				
				Service service = (Service) serviceClass.newInstance();
				service.run(serviceName, request, response);
			}
			catch (ClassNotFoundException e) {
				String message = "Unknown " + isAdmin + "service '" + serviceName + "'";
				Engine.logAdmin.error(message);
				if (show_error) {
					ServiceUtils.handleError(message, request, response);
				}
			}
			catch (NoClassDefFoundError e) {
				String message = "Unknown " + isAdmin + "service '" + serviceName + "'";
				Engine.logAdmin.error(message);
				if (show_error) {
					ServiceUtils.handleError(message, request, response);
				}
			}
			catch (AuthenticationException e) {
				String authMessage = e.getMessage();
				Engine.logAdmin.warn(authMessage);
				if (show_error) {
					ServiceUtils.handleError(authMessage, request, response);
				}
			}
			catch (Exception e) {
				Engine.logAdmin.error("Unable to execute the service '"
						+ serviceName + "'", e);
				if (show_error) {
					ServiceUtils.handleError(e, request, response);
				}
			}
			finally {
				response.flushBuffer();
			}
		} catch (Throwable e) {
			if (show_error) {
				throw new ServletException(e);
			}
		}
	}

}
