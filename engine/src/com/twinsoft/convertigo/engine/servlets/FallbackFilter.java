/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

public class FallbackFilter implements Filter {
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) _request;
		HttpServletResponse response = (HttpServletResponse) _response;
		var servletPath = request.getServletPath();
		if (servletPath.endsWith("/")) {
			servletPath += "index.html";
		}
		if (servletPath.endsWith("/index.html")) {
			var f = new File(Engine.WEBAPP_PATH, servletPath);
			if (!f.exists()) {
				var path = !File.separator.equals("/") ? f.getAbsolutePath().replace(File.separator, "/") : f.getAbsolutePath();
				var split = path.split("/");
				for (var i = split.length - 2; i >= 0; i--) {
					var part = split[i];
					split[i] = "_";
					var fallback = new File(String.join("/", split));
					split[i] = part;
					if (fallback.exists()) {
						request.getRequestDispatcher(fallback.getPath().substring(Engine.WEBAPP_PATH.length())).forward(request, response);
						return;
					}
				}
			}
		}
		filterChain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

}
