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
import java.util.Set;

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
		configuration = RedisClients.getConfiguration();
		store = new RedisSessionStore(configuration);
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
		Engine.logRedis.debug("(RedisSessionProvider) Incoming sessionId=" + sessionId + ", create=" + create);
		SessionStoreMeta meta = null;
		if (sessionId != null) {
			meta = store.readMeta(sessionId);
			Engine.logRedis.debug("(RedisSessionProvider) Read session meta " + sessionId + ": "
					+ (meta != null ? "hit" : "miss"));
		}
		RedisHttpSession session = null;
		if (meta != null) {
			session = RedisHttpSession.fromMeta(store, request.getServletContext(), configuration, sessionId, meta);
		}
		if (session == null) {
			if (!create) {
				Engine.logRedis.debug("(RedisSessionProvider) No existing session and create=false");
				return null;
			}
			sessionId = cookieHelper.generateSessionId();
			session = RedisHttpSession.newSession(store, request.getServletContext(), configuration, sessionId);
			Engine.logRedis.debug("(RedisSessionProvider) Created new session " + sessionId + " [redis-hash]");
		}
		session.markAccessed();
		threadSessions.get().put(session.getId(), session);
		cookieHelper.ensureCookie(request, response, sessionId, configuration.getCookieName());
		request.setAttribute(REQUEST_SESSION_OBJECT_ATTR, session);
		return session;
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
					Engine.logRedis.debug("(RedisSessionProvider) flush session failed: " + e.getMessage());
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
			Engine.logRedis.debug("(RedisSessionProvider) terminateSession failed: " + e.getMessage());
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
					Engine.logRedis.debug("(RedisSessionProvider) terminateAllSessions delete failed: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			Engine.logRedis.debug("(RedisSessionProvider) terminateAllSessions failed: " + e.getMessage());
		}
		return count;
	}

	@Override
	public int purgeAllSessions(String sessionIdToKeep) {
		if (!(store instanceof RedisSessionStore redisStore)) {
			return 0;
		}
		int count = 0;
		try {
			for (var sessionId : redisStore.readSessionIds()) {
				if (sessionId == null || sessionId.isBlank() || sessionId.equals(sessionIdToKeep)) {
					continue;
				}
				try {
					store.delete(sessionId);
					count++;
				} catch (Exception e) {
					Engine.logRedis.debug("(RedisSessionProvider) purgeAllSessions delete failed: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			Engine.logRedis.debug("(RedisSessionProvider) purgeAllSessions failed: " + e.getMessage());
		}
		return count;
	}

	@Override
	public int estimateCountedSessions() {
		return store instanceof RedisSessionStore redisStore ? redisStore.estimateCountedSessions() : 0;
	}

	@Override
	public int countCountedSessions() {
		return store instanceof RedisSessionStore redisStore ? redisStore.countCountedSessions() : 0;
	}

	@Override
	public Set<String> readCountedSessionIds() {
		return store instanceof RedisSessionStore redisStore ? redisStore.readCountedSessionIds() : Set.of();
	}

	@Override
	public boolean upsertCountedSessionBillingSnapshot(HttpSession session) {
		if (store instanceof RedisSessionStore redisStore) {
			return redisStore.upsertCountedSessionBillingSnapshot(session);
		}
		return false;
	}

	@Override
	public void syncCountedSessionBillingAuthenticatedUser(HttpSession session) {
		if (store instanceof RedisSessionStore redisStore) {
			redisStore.syncCountedSessionBillingAuthenticatedUser(session);
		}
	}
}
