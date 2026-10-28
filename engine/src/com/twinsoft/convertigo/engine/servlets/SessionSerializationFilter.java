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

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.SessionStoreUnavailableException;
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
			var redisMode = ConvertigoHttpSessionManager.isRedisMode();
			if (redisMode) {
				ContextManager.initRequestContextCache(effectiveRequest);
			}
			if (response instanceof HttpServletResponse httpResponse) {
				var cookieName = EnginePropertiesManager.getProperty(PropertyName.SESSION_COOKIE_NAME);
				// Redis: the session changes are written before the response can reach the client, the end of the
				// request writes only what changes after
				Runnable beforeOutput = redisMode
						? () -> ConvertigoHttpSessionManager.getInstance().flushRequestSession(effectiveRequest)
						: null;
				var responseWrapper = (cookieName == null || cookieName.isEmpty()) && beforeOutput == null
						? httpResponse
						: new SessionCookieResponseWrapper(httpResponse, cookieName, beforeOutput);
				try {
					chain.doFilter(effectiveRequest, responseWrapper);
				} catch (IOException | ServletException | RuntimeException e) {
					if (!SessionStoreUnavailableException.respond(httpResponse, e)) {
						throw e;
					}
				} finally {
					// Redis: the contexts of the request were saved before their lock was released, they are only
					// evicted here, before the last write of the session, which saves the contexts still held only
					if (ConvertigoHttpSessionManager.isRedisMode()) {
						try {
							ContextManager.flushRequestContextCache(effectiveRequest);
						} catch (Exception ignored) {
							// ignore
						}
					}
					try {
						ConvertigoHttpSessionManager.getInstance().flushBuffers();
					} catch (Exception ignored) {
						// ignore
					}
					if (responseWrapper instanceof SessionCookieResponseWrapper wrapper) {
						wrapper.flushSessionCookie();
					}
				}
			} else {
				chain.doFilter(effectiveRequest, response);
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// no-op
	}
}
