/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the  Free Software Foundation;  either
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

package com.twinsoft.convertigo.engine.sessions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.Engine;

final class RedisSessionProvider implements SessionProvider {
	private final SessionStore store;
	private final BufferedSessionStore bufferedStore;
	private final RedisSessionConfiguration configuration;
	private final SessionCookieHelper cookieHelper = new SessionCookieHelper();
	private static final String REQUEST_SESSION_OBJECT_ATTR = "convertigo.redis.session";

	RedisSessionProvider() {
		configuration = RedisSessionConfiguration.fromProperties();
		store = new RedisHashSessionStore(configuration);
		bufferedStore = new BufferedSessionStore(store);
		registerShutdownHook();
	}

	public HttpSession getSession(HttpServletRequest request, boolean create) {
		if (request == null) {
			return null;
		}

		var cachedSession = (HttpSession) request.getAttribute(REQUEST_SESSION_OBJECT_ATTR);
		if (cachedSession instanceof RedisHttpSession redisSession) {
			if (!redisSession.isInvalidatedInternal()) {
				return redisSession;
			}
			// Invalidation occurred in this request: drop and recreate.
			request.removeAttribute(REQUEST_SESSION_OBJECT_ATTR);
		}

		var response = (HttpServletResponse) request.getAttribute("response");
		// Reuse session created earlier in the same request to avoid double-creation.
		var sessionId = cookieHelper.resolveSessionId(request, configuration.getCookieName());
		debug("Incoming sessionId=" + sessionId + ", create=" + create);
		SessionData data = null;
		if (sessionId != null) {
			data = currentStore().read(sessionId);
			debug("Read session " + sessionId + ": " + (data != null ? "hit" : "miss"));
		}
		if (data == null) {
			if (!create) {
				debug("No existing session and create=false");
				return null;
			}
			sessionId = cookieHelper.generateSessionId();
			data = SessionData.newSession(sessionId, configuration.getDefaultTtlSeconds());
			// Touch & persist immediately to avoid losing the first attribute write on a fresh session.
			data.touch();
			currentStore().save(data);
			debug("Created new session " + sessionId + " (touched+saved) [redis-hash]");
		}
		cookieHelper.ensureCookie(request, response, sessionId, configuration.getCookieName());
		var session = new RedisHttpSession(currentStore(), data, request.getServletContext(), configuration);
		session.markAccessed();
		request.setAttribute(REQUEST_SESSION_OBJECT_ATTR, session);
		return session;
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				currentStore().shutdown();
			} catch (Exception e) {
				if (Engine.logEngine != null) {
					Engine.logEngine.error("(RedisSessionProvider) Failed to shutdown cleanly", e);
				}
			}
		}, "convertigo-redis-session-shutdown"));
	}

	private void debug(String message) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(RedisSessionProvider) " + message);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	private SessionStore currentStore() {
		return bufferedStore;
	}

	@Override
	public void flushBuffers() {
		if (bufferedStore != null) {
			bufferedStore.flushThread();
		}
	}
}
