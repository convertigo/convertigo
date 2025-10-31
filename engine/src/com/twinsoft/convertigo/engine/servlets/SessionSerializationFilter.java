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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.util.HttpServletRequestSessionWrapper;

public class SessionSerializationFilter implements Filter {
	@Override
	public void init(FilterConfig filterConfig) {
		// no-op
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpRequest) {
			var effectiveRequest = httpRequest instanceof HttpServletRequestSessionWrapper
					? httpRequest
					: new HttpServletRequestSessionWrapper(httpRequest);
			chain.doFilter(effectiveRequest, response);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// no-op
	}
}
