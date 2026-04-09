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
import java.util.Collections;
import java.util.Enumeration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

public class FallbackFilter implements Filter {
	private static String resolveFallbackPath(String servletPath) {
		if (!servletPath.endsWith("/index.html")) {
			return null;
		}

		if (new File(Engine.WEBAPP_PATH, servletPath).exists()) {
			return servletPath;
		}

		var segments = servletPath.split("/");
		var currentPath = new StringBuilder();

		for (var i = 1; i < segments.length; i++) { // skip the first empty "/"
			currentPath.append("/");

			if (new File(Engine.WEBAPP_PATH, currentPath.toString() + segments[i]).exists()) {
				currentPath.append(segments[i]);
			} else if (new File(Engine.WEBAPP_PATH, currentPath.toString() + "_").exists()) {
				currentPath.append("_");
			} else {
				return null;
			}
		}

		var fallbackPath = currentPath.toString();
		return new File(Engine.WEBAPP_PATH, fallbackPath).exists() ? fallbackPath : null;
	}

	private static HttpServletRequest withoutConditionalCaching(HttpServletRequest request) {
		return new HttpServletRequestWrapper(request) {
			@Override
			public String getHeader(String name) {
				return isConditionalCacheHeader(name) ? null : super.getHeader(name);
			}

			@Override
			public Enumeration<String> getHeaders(String name) {
				return isConditionalCacheHeader(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
			}

			@Override
			public long getDateHeader(String name) {
				return isConditionalCacheHeader(name) ? -1 : super.getDateHeader(name);
			}
		};
	}

	private static boolean isConditionalCacheHeader(String name) {
		return HeaderName.IfNoneMatch.is(name) || HeaderName.IfModifiedSince.is(name);
	}

	private static void applyNoStore(HttpServletResponse response) {
		HeaderName.CacheControl.setHeader(response, "no-store, no-cache, must-revalidate");
		HeaderName.Pragma.setHeader(response, "no-cache");
		HeaderName.Expires.setHeader(response, "0");
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain filterChain) throws IOException, ServletException {
	    HttpServletRequest request = (HttpServletRequest) _request;
	    HttpServletResponse response = (HttpServletResponse) _response;
		Log4jHelper.mdcClear();
	    var servletPath = request.getServletPath();

		if (!servletPath.endsWith("/") && ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
			var slashServletPath = servletPath + "/";
			if (resolveFallbackPath(slashServletPath + "index.html") != null) {
				var location = new StringBuilder(request.getContextPath()).append(slashServletPath);
				if (request.getQueryString() != null) {
					location.append("?").append(request.getQueryString());
				}
				response.sendRedirect(response.encodeRedirectURL(location.toString()));
				return;
			}
		}

		if (servletPath.endsWith("/")) {
			servletPath += "index.html";
		}

		var fallbackPath = resolveFallbackPath(servletPath);
		if (fallbackPath != null) {
			request = withoutConditionalCaching(request);
			applyNoStore(response);
		}
		if (fallbackPath != null && !fallbackPath.equals(servletPath)) {
			request.getRequestDispatcher(fallbackPath).forward(request, response);
			return;
		}
	    
	    filterChain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

}
