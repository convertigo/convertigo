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
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class CacheControlFilter implements Filter {

	private static final String CACHE_CONTROL_IMMUTABLE = "public, max-age=31536000, immutable";
	private static final String CACHE_CONTROL_REVALIDATE = "no-cache, must-revalidate";
	private static final Pattern WEBAPP_IMMUTABLE_PATH = Pattern.compile("^/_app/immutable/.+", Pattern.CASE_INSENSITIVE);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			var httpRequest = (HttpServletRequest) request;
			var httpResponse = (HttpServletResponse) response;

			if (shouldHandle(httpRequest) && !HeaderName.CacheControl.has(httpResponse)) {
				var path = getPath(httpRequest);
				var cacheControl = getCacheControl(path);
				if (cacheControl != null) {
					HeaderName.CacheControl.setHeader(httpResponse, cacheControl);
				}
			}
		}

		chain.doFilter(request, response);
	}

	private static String getCacheControl(String path) {
		if ("/manifest.webmanifest".equals(path) || resolvesToIndexHtml(path)) {
			return CACHE_CONTROL_REVALIDATE;
		}
		if (WEBAPP_IMMUTABLE_PATH.matcher(path).matches() && webappFile(path).isFile()) {
			return CACHE_CONTROL_IMMUTABLE;
		}
		return null;
	}

	private static String getPath(HttpServletRequest request) {
		var uri = request.getRequestURI();
		var contextPath = request.getContextPath();
		var path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
		if (path == null || path.isEmpty()) {
			return "/";
		}
		return path.startsWith("/") ? path : "/" + path;
	}

	private static boolean resolvesToIndexHtml(String path) {
		var servletPath = path.endsWith("/") ? path + "index.html" : path;
		var file = webappFile(servletPath);
		if (isIndexHtml(file)) {
			return true;
		}
		if (!servletPath.endsWith("/index.html")) {
			return false;
		}

		var segments = servletPath.split("/");
		var resolved = new StringBuilder();
		for (var i = 1; i < segments.length; i++) {
			resolved.append("/");
			var direct = webappFile(resolved + segments[i]);
			if (direct.exists()) {
				resolved.append(segments[i]);
				continue;
			}
			var fallback = webappFile(resolved + "_");
			if (fallback.exists()) {
				resolved.append("_");
				continue;
			}
			return false;
		}

		return isIndexHtml(webappFile(resolved.toString()));
	}

	private static boolean isIndexHtml(File file) {
		return file.isFile() && "index.html".equalsIgnoreCase(file.getName());
	}

	private static File webappFile(CharSequence path) {
		var relativePath = path.toString();
		while (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		return new File(Engine.WEBAPP_PATH, relativePath);
	}

	private static boolean shouldHandle(HttpServletRequest request) {
		var method = request.getMethod();
		return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
	}
}
