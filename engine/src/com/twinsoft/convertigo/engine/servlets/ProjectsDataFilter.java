/*
 * Copyright (c) 2001-2025 Convertigo SA.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.MDC;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.MinificationManager;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.util.ServletUtils;

public class ProjectsDataFilter implements Filter {
	private static Pattern p_projects = Pattern.compile("/(system/)?projects(/.*)");
	private static Pattern p_forbidden = Pattern.compile("^/(?:([^/]+$)|(?:(.*?)/\\2\\.xml)|(?:(?:.*?)/(?:c8oProject\\.yaml|_c8oProject/.*|libs/.*|\\.git/.*))|(?:(?:.*?)/_private(?:$|(?!/mobile/flashupdate_|/ionic/node_modules/@ionic/).*)))$");

	public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain) throws IOException, ServletException {
		boolean hide_error = EnginePropertiesManager.getProperty( PropertyName.HIDING_ERROR_INFORMATION ).equals( "true" );


		HttpServletRequest request = (HttpServletRequest) _request;
		HttpServletResponse response = (HttpServletResponse) _response;
		String query = request.getQueryString();
		String requestURI = request.getRequestURI();
		
		if (HeaderName.XConvertigoNoLog.has(request) || (query != null && query.matches("(.*&)?__nolog=true(&.*)?")) || requestURI.contains("/system/projects/")) {
			MDC.put("nolog", true);
		} else {
			MDC.remove("nolog");
		}
		
		Engine.logContext.debug("Entering projects data servlet filter");

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

		Engine.logContext.debug("requestURI=" + requestURI);

		// Get a canonicalized form of the request URL, i.e. resolve all ".", "..", "///"...
		try {
			requestURI = new URI(request.getRequestURL().toString()).normalize().getPath();
		} catch (URISyntaxException e) {
			// should never occur
		}

		Matcher m_projects = p_projects.matcher(requestURI);
		String pathInfo = m_projects.find() ? m_projects.group(2) : "";

		String requestedObject;
		if (m_projects.group(1) == null) {
			requestedObject = Engine.PROJECTS_PATH + pathInfo;
			requestedObject = Engine.resolveProjectPath(requestedObject);
		} else {
			requestedObject = Engine.WEBAPP_PATH + "/system/projects/" + pathInfo;
		}
		Engine.logContext.debug("requestedObject=" + requestedObject);

		Matcher m_forbidden = p_forbidden.matcher(pathInfo);
		if (m_forbidden.matches() && (m_forbidden.group(1) == null || !(new File(requestedObject).isDirectory()))) {
			if (m_forbidden.group(1) != null) {
				requestedObject = Engine.WEBAPP_PATH + "/noproject.html";
			} else {
				if (hide_error == false) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
				return;
			}
		}

		if (Pattern.matches(".*?[/\\\\]index\\.jsp", requestedObject)) {
			requestedObject = requestedObject.replaceAll("index.jsp", "index.html");
			Engine.logContext.debug("index.jsp remapped to '" + requestedObject + "'");
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

		// Handle implicit document (index.html)
		if (file.exists() && file.isDirectory()) {
			// Handle ".../projects" requests
			s = file.getCanonicalPath();
			if (s.equals(Engine.PROJECTS_PATH)) {
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

		if (!file.exists() && Engine.theApp != null && Engine.theApp.minificationManager != null) {
			if (Engine.theApp.minificationManager.check(request, response)) {
				return;
			} else {
				File commonResource = MinificationManager.getCommonCssResource(request);
				if (commonResource != null) {
					file = commonResource;
				}
			}
		}
		
		if (GatewayServlet.getDevPort(requestURI) > 0) {
			request.getRequestDispatcher("/gw").forward(request, response);
			return;
		}

		ServletUtils.handleFileFilter(file, request, response, filterConfig, chain);

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
