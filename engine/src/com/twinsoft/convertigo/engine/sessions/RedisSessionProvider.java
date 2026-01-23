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

package com.twinsoft.convertigo.engine.sessions;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.Engine;

final class RedisSessionProvider implements SessionProvider {
	private final SessionStore store;
	private final RedisSessionConfiguration configuration;
	private final SessionCookieHelper cookieHelper = new SessionCookieHelper();
	private static final String REQUEST_SESSION_OBJECT_ATTR = "convertigo.redis.session";
	private final ThreadLocal<HashMap<String, RedisHttpSession>> threadSessions = ThreadLocal.withInitial(HashMap::new);

	RedisSessionProvider() {
		configuration = RedisSessionConfiguration.fromProperties();
		store = new RedisSessionStore(configuration);
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
		SessionStoreMeta meta = null;
		if (sessionId != null) {
			meta = store.readMeta(sessionId);
			debug("Read session meta " + sessionId + ": " + (meta != null ? "hit" : "miss"));
		}
		RedisHttpSession session = null;
		if (meta != null) {
			session = RedisHttpSession.fromMeta(store, request.getServletContext(), configuration, sessionId, meta);
		}
		if (session == null) {
			if (!create) {
				debug("No existing session and create=false");
				return null;
			}
			sessionId = cookieHelper.generateSessionId();
			session = RedisHttpSession.newSession(store, request.getServletContext(), configuration, sessionId);
			debug("Created new session " + sessionId + " [redis-hash]");
		}
		session.markAccessed();
		threadSessions.get().put(session.getId(), session);
		cookieHelper.ensureCookie(request, response, sessionId, configuration.getCookieName());
		request.setAttribute(REQUEST_SESSION_OBJECT_ATTR, session);
		return session;
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				store.shutdown();
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

	@Override
	public void flushBuffers() {
		var sessions = threadSessions.get();
		if (sessions.isEmpty()) {
			return;
		}
		try {
			for (var session : sessions.values()) {
				try {
					session.flush();
				} catch (Exception e) {
					debug("flush session failed: " + e.getMessage());
				}
			}
		} finally {
			sessions.clear();
		}
	}

	@Override
	public boolean terminateSession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return false;
		}
		try {
			store.delete(sessionId);
			return true;
		} catch (Exception e) {
			debug("terminateSession failed: " + e.getMessage());
			return false;
		}
	}

	@Override
	public int terminateAllSessions() {
		if (!(store instanceof RedisSessionStore redisStore)) {
			return 0;
		}
		int count = 0;
		try {
			for (var sessionId : redisStore.readSessionIds()) {
				if (sessionId == null || sessionId.isBlank()) {
					continue;
				}
				try {
					store.delete(sessionId);
					count++;
				} catch (Exception e) {
					debug("terminateAllSessions delete failed: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			debug("terminateAllSessions failed: " + e.getMessage());
		}
		return count;
	}
}
