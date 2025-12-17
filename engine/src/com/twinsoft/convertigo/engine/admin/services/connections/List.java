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

package com.twinsoft.convertigo.engine.admin.services.connections;

import java.util.ArrayList;
import java.util.HashMap;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.redisson.api.RMapCache;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.client.codec.StringCodec;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.RedisClients;
import com.twinsoft.tas.KeyManager;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN, Role.CONNECTIONS_CONFIG, Role.CONNECTIONS_VIEW },
		parameters = {},
		returnValue = ""
	)
public class List extends XmlService{
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String INDEX_SESSIONS = "index:sessions";
	private static final String INDEX_CONTEXTS = "index:contexts";
	private static final String INFLIGHT_CONTEXTS = "inflight:contexts";
	private static final String SESSION_META_CREATION = "__meta:creationTime";
	private static final String SESSION_META_LAST_ACCESS = "__meta:lastAccessedTime";
	private static final String SESSION_META_MAX_INACTIVE = "__meta:maxInactiveInterval";
	private static final String CONTEXT_META_PROJECT = "__meta:projectName";
	private static final String CONTEXT_META_CONNECTOR = "__meta:connectorName";
	private static final String CONTEXT_META_REQUESTED = "__meta:requested";
	private static final String CONTEXT_META_REMOTE_HOST = "__meta:remoteHost";
	private static final String CONTEXT_META_REMOTE_ADDR = "__meta:remoteAddr";
	private static final String CONTEXT_META_USER_AGENT = "__meta:userAgent";
	private static final String CONTEXT_META_WAITING = "__meta:waitingRequests";

	/**
	 * <admin service="Connections">
	 * 		<contextsInUse>1</contextsInUse>
	 * 		<threadsInUse>0</threadsInUse>
	 * 		<threadsNumber>100</threadsNumber>
	 * 		<connections>
	 * 			<connection connected="true" contextName="" project="" connector="" requested="" status="" user="" contextCreationDate="" lastContextAccessDate="" contextInactivityTime="" clientComputer=""/>
	 * 		</connections>
	 * </admin>
	 */
	
	public String formatTime(long time) {
		String s = "";
		long seconds = time % 60;
		long minutes = (time / 60) % 60;
		long hours = (time / 3600) % 24;
		long days = time / 86400; // 86400 = 3600 x 24
		if (days != 0) s += days + "d";
		if (hours != 0) s += hours + "h";
		if (minutes != 0) s += minutes + "min";
		if (seconds != 0) s += seconds + "s";
		return s.isEmpty() ? "0" : s;
	}
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		HttpSession currentSession = request.getSession();
		
		Element rootElement = document.getDocumentElement();
        
        Element connectionsListElement = document.createElement("connections");
        rootElement.appendChild(connectionsListElement);
        
        Element sessionsListElement = document.createElement("sessions");
        rootElement.appendChild(sessionsListElement);
        
        Element contextsInUseElement = document.createElement("contextsInUse");
        rootElement.appendChild(contextsInUseElement);
        
        Element contextsNumberElement = document.createElement("contextsNumber");
        contextsNumberElement.setTextContent("" + EnginePropertiesManager.getProperty(PropertyName.CONVERTIGO_MAX_CONTEXTS));
        rootElement.appendChild(contextsNumberElement);
        
        Element sessionsInUseElement = document.createElement("sessionsInUse");
        rootElement.appendChild(sessionsInUseElement);
        
        Element sessionsIsOverflowElement = document.createElement("sessionsIsOverflow");
        sessionsIsOverflowElement.setTextContent(KeyManager.isOverflow(Session.EmulIDSE) ? "true" : "false");
        rootElement.appendChild(sessionsIsOverflowElement);
        
        Element sessionsNumberElement = document.createElement("sessionsNumber");
        sessionsNumberElement.setTextContent("" + Math.max(0, KeyManager.getMaxCV(Session.EmulIDSE)));
        rootElement.appendChild(sessionsNumberElement);
        
        Element threadsInUseElement = document.createElement("threadsInUse");
        threadsInUseElement.setTextContent("" + Math.max(0, com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads));
        rootElement.appendChild(threadsInUseElement);
        
        Element threadsNumberElement = document.createElement("threadsNumber");
        threadsNumberElement.setTextContent(EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_MAX_WORKER_THREADS));
        rootElement.appendChild(threadsNumberElement);
        
        Element httpTimeoutElement = document.createElement("httpTimeout");
        httpTimeoutElement.setTextContent(formatTime(currentSession.getMaxInactiveInterval()));
        rootElement.appendChild(httpTimeoutElement);
        
        long now = System.currentTimeMillis();
		if (ConvertigoHttpSessionManager.isRedisMode()) {
			var counts = listFromRedis(request, document, currentSession, connectionsListElement, sessionsListElement, now);
			contextsInUseElement.setTextContent(Integer.toString(Math.max(0, counts.contexts)));
			sessionsInUseElement.setTextContent(Integer.toString(Math.max(0, counts.sessions)));
		} else {
			contextsInUseElement.setTextContent("" + Math.max(0, Engine.theApp.contextManager.getNumberOfContexts()));
			sessionsInUseElement.setTextContent("" + HttpSessionListener.countSessions());

			Collection<Context> contexts = null;

			String sessionToFilter = request.getParameter("session");

			if (StringUtils.isNotBlank(sessionToFilter)) {
				HttpSession session = HttpSessionListener.getHttpSession(sessionToFilter);
				if (session != null) {
					contexts = Engine.theApp.contextManager.getContexts(session);
				}
			}
			if (contexts == null) {
				contexts = Engine.theApp.contextManager.getContexts();
			}

			for (Context context : contexts) {
				try {
					String authenticatedUser = null;
					try {
						authenticatedUser = context.getAuthenticatedUser();
					} catch (Exception e) {
						Engine.logAdmin.trace("connection.List failed to get the authenticated user: " + e);
					}

					com.twinsoft.api.Session apiSession = Engine.theApp.sessionManager.getSession(context.contextID);
					boolean bConnected = ((apiSession != null) && apiSession.isConnected());
					Element connectionElement = document.createElement("connection");
					connectionElement.setAttribute("connected", Boolean.toString(bConnected));
					connectionElement.setAttribute("contextName", context.contextID);
					connectionElement.setAttribute("project", context.projectName);
					connectionElement.setAttribute("connector", context.connectorName);
					connectionElement.setAttribute("requested",
							(context.requestedObject instanceof Transaction) ? context.transactionName : context.sequenceName);
					connectionElement.setAttribute("status", (context.requestedObject == null || context.requestedObject.runningThread == null
							? "finished"
							: (context.requestedObject.runningThread.bContinue ? "in progress" : "finished")) + "(" + context.waitingRequests
									+ ")");
					connectionElement.setAttribute("user", authenticatedUser == null ? "" : authenticatedUser);
					connectionElement.setAttribute("contextCreationDate",
							DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.creationTime)));
					connectionElement.setAttribute("lastContextAccessDate",
							DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(context.lastAccessTime)));
					try {
						connectionElement.setAttribute("contextInactivityTime",
								formatTime((now - context.lastAccessTime) / 1000) + " / "
										+ formatTime(Engine.theApp.databaseObjectsManager.getOriginalProjectByName(context.projectName)
												.getContextTimeout()));
					} catch (Exception e) {
						// ignore
					}
					connectionElement.setAttribute("clientComputer",
							context.remoteHost + " (" + context.remoteAddr + "), " + context.userAgent);
					connectionsListElement.appendChild(connectionElement);
				} catch (Exception e) {
					Engine.logAdmin.warn("[connections.List] context list failure: " + e);
				}
			}

			if (!"false".equals(request.getParameter("sessions"))) {
				for (HttpSession session : HttpSessionListener.getSessions()) {
					try {
						Element sessionElement = document.createElement("session");
						java.util.List<Context> ctxs = Engine.theApp.contextManager.getContexts(session);
						sessionElement.setAttribute("sessionID", session.getId());
						sessionElement.setAttribute("authenticatedUser", SessionAttribute.authenticatedUser.string(session));
						sessionElement.setAttribute("contexts", Integer.toString(ctxs == null ? 0 : ctxs.size()));
						sessionElement.setAttribute("clientIP", SessionAttribute.clientIP.string(session));
						sessionElement.setAttribute("deviceUUID", SessionAttribute.deviceUUID.string(session));
						sessionElement.setAttribute("lastSessionAccessDate",
								DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(session.getLastAccessedTime())));
						sessionElement.setAttribute("sessionInactivityTime",
								formatTime((now - session.getLastAccessedTime()) / 1000) + " / " + formatTime(session.getMaxInactiveInterval()));
						Role[] r = (Role[]) session.getAttribute(SessionKey.ADMIN_ROLES.toString());
						sessionElement.setAttribute("adminRoles", Integer.toString(r == null ? 0 : r.length));
						if (session == currentSession) {
							sessionElement.setAttribute("isCurrentSession", "true");
						}
						Set<HttpServletRequest> set = SessionAttribute.fullSyncRequests.get(session);
						sessionElement.setAttribute("isFullSyncActive", Boolean.toString(set != null && !set.isEmpty()));
						sessionsListElement.appendChild(sessionElement);
					} catch (Exception e) {
						Engine.logAdmin.warn("[connections.List] session list failure: " + e);
					}
				}
			}
		}
	}

	private static final class RedisCounts {
		final int sessions;
		final int contexts;

		private RedisCounts(int sessions, int contexts) {
			this.sessions = sessions;
			this.contexts = contexts;
		}
	}

	private RedisCounts listFromRedis(HttpServletRequest request, Document document, HttpSession currentSession, Element connectionsListElement,
			Element sessionsListElement, long now) {
		try {
			var cfg = RedisClients.getConfiguration();
			var client = RedisClients.getClient();

			String sessionsIndexKey = cfg.getContextKeyPrefix() + INDEX_SESSIONS;
			String contextsIndexKey = cfg.getContextKeyPrefix() + INDEX_CONTEXTS;
			RSet<String> sessionsIndex = client.getSet(sessionsIndexKey, StringCodec.INSTANCE);
			RSet<String> contextsIndex = client.getSet(contextsIndexKey, StringCodec.INSTANCE);

			var persistedSessionIds = new HashSet<>(sessionsIndex.readAll());
			var persistedContextIds = new HashSet<>(contextsIndex.readAll());
			var sessionIds = new HashSet<>(persistedSessionIds);
			var contextIds = new HashSet<>(persistedContextIds);

			Map<String, String> inflightSnapshots = java.util.Collections.emptyMap();
			try {
				String inflightKey = cfg.getContextKeyPrefix() + INFLIGHT_CONTEXTS;
				RMapCache<String, String> inflightContexts = client.getMapCache(inflightKey, StringCodec.INSTANCE);
				var read = inflightContexts.readAllMap();
				if (read != null && !read.isEmpty()) {
					inflightSnapshots = read;
					contextIds.addAll(inflightSnapshots.keySet());
					for (var ctxId : inflightSnapshots.keySet()) {
						var sid = extractSessionId(ctxId);
						if (sid != null && !sid.isBlank()) {
							sessionIds.add(sid);
						}
					}
				}
			} catch (Exception ignore) {
				// ignore inflight read failures
			}

			String sessionToFilter = request.getParameter("session");
			String contextPrefixFilter = StringUtils.isNotBlank(sessionToFilter) ? sessionToFilter + "_" : null;

			var staleContexts = new ArrayList<String>();
			var sessionAuthenticatedUsers = new HashMap<String, String>();
			var requiredContextFields = Set.of("name", "contextID", "creationTime", "lastAccessTime", CONTEXT_META_PROJECT,
					CONTEXT_META_CONNECTOR, CONTEXT_META_REQUESTED, CONTEXT_META_REMOTE_HOST, CONTEXT_META_REMOTE_ADDR,
					CONTEXT_META_USER_AGENT, CONTEXT_META_WAITING);

			for (var contextId : contextIds) {
				if (contextPrefixFilter != null && (contextId == null || !contextId.startsWith(contextPrefixFilter))) {
					continue;
				}
				if (contextId == null || contextId.isBlank()) {
					continue;
				}
				boolean isPersisted = persistedContextIds.contains(contextId);
				boolean isInflight = inflightSnapshots.containsKey(contextId);
				var ctxKey = cfg.contextKey(contextId);
				RMap<String, String> ctxMap = client.getMap(ctxKey, StringCodec.INSTANCE);
				Map<String, String> snapshot;
				try {
					snapshot = ctxMap.getAll(requiredContextFields);
				} catch (Exception e) {
					snapshot = null;
				}
				if (snapshot == null || snapshot.isEmpty()) {
					String inflightJson = inflightSnapshots.get(contextId);
					if (inflightJson != null && !inflightJson.isBlank()) {
						snapshot = parseInflightSnapshot(inflightJson, requiredContextFields);
						isInflight = snapshot != null && !snapshot.isEmpty();
					}
				}
				if (snapshot == null || snapshot.isEmpty()) {
					if (isPersisted) {
						staleContexts.add(contextId);
					}
					continue;
				}

				String projectName = jsonText(snapshot.get(CONTEXT_META_PROJECT));
				String connectorName = jsonText(snapshot.get(CONTEXT_META_CONNECTOR));
				String requested = jsonText(snapshot.get(CONTEXT_META_REQUESTED));
				String remoteHost = jsonText(snapshot.get(CONTEXT_META_REMOTE_HOST));
				String remoteAddr = jsonText(snapshot.get(CONTEXT_META_REMOTE_ADDR));
				String userAgent = jsonText(snapshot.get(CONTEXT_META_USER_AGENT));
				int waitingRequests = jsonInt(snapshot.get(CONTEXT_META_WAITING), 0);

				long creationTime = jsonLong(snapshot.get("creationTime"), 0L);
				long lastAccessTime = jsonLong(snapshot.get("lastAccessTime"), 0L);

				String sessionId = extractSessionId(contextId);
				String authenticatedUser = "";
				if (sessionId != null) {
					authenticatedUser = sessionAuthenticatedUsers.computeIfAbsent(sessionId, sid -> {
						try {
							String sessionKey = cfg.getKeyPrefix() + sid;
							RMap<String, String> sessionMap = client.getMap(sessionKey, StringCodec.INSTANCE);
							String user = jsonText(sessionMap.get(SessionAttribute.authenticatedUser.value()));
							return user == null ? "" : user;
						} catch (Exception ignore) {
							return "";
						}
					});
				}

				Element connectionElement = document.createElement("connection");
				connectionElement.setAttribute("connected", Boolean.toString(false));
				connectionElement.setAttribute("contextName", contextId);
				connectionElement.setAttribute("project", projectName != null ? projectName : "");
				connectionElement.setAttribute("connector", connectorName != null ? connectorName : "");
				connectionElement.setAttribute("requested", requested != null ? requested : "");
				connectionElement.setAttribute("status", (isInflight ? "in progress" : "finished") + "(" + waitingRequests + ")");
				connectionElement.setAttribute("user", authenticatedUser != null ? authenticatedUser : "");
				if (creationTime > 0) {
					connectionElement.setAttribute("contextCreationDate",
							DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(creationTime)));
				} else {
					connectionElement.setAttribute("contextCreationDate", "");
				}
				if (lastAccessTime > 0) {
					connectionElement.setAttribute("lastContextAccessDate",
							DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(lastAccessTime)));
				} else {
					connectionElement.setAttribute("lastContextAccessDate", "");
				}

				try {
					if (lastAccessTime > 0 && projectName != null) {
						connectionElement.setAttribute("contextInactivityTime",
								formatTime((now - lastAccessTime) / 1000) + " / "
										+ formatTime(Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName).getContextTimeout()));
					}
				} catch (Exception ignore) {
					// ignore
				}

				var clientComputer = new StringBuilder();
				if (remoteHost != null && !remoteHost.isBlank()) {
					clientComputer.append(remoteHost);
				}
				if (remoteAddr != null && !remoteAddr.isBlank()) {
					if (clientComputer.length() > 0) {
						clientComputer.append(' ');
					}
					clientComputer.append('(').append(remoteAddr).append(')');
				}
				if (userAgent != null && !userAgent.isBlank()) {
					if (clientComputer.length() > 0) {
						clientComputer.append(", ");
					}
					clientComputer.append(userAgent);
				}
				connectionElement.setAttribute("clientComputer", clientComputer.toString());

				connectionsListElement.appendChild(connectionElement);
			}

			if (!staleContexts.isEmpty()) {
				try {
					contextsIndex.removeAll(staleContexts);
					for (var ctxId : staleContexts) {
						contextIds.remove(ctxId);
					}
				} catch (Exception ignore) {
					// ignore
				}
			}

			if (!"false".equals(request.getParameter("sessions"))) {
				var contextsBySession = new HashMap<String, Integer>();
				for (var contextId : contextIds) {
					var sid = extractSessionId(contextId);
					if (sid != null) {
						contextsBySession.merge(sid, 1, Integer::sum);
					}
				}

				String currentSessionId = currentSession != null ? currentSession.getId() : null;
				var staleSessions = new ArrayList<String>();
				var requiredSessionFields = Set.of(SESSION_META_CREATION, SESSION_META_LAST_ACCESS, SESSION_META_MAX_INACTIVE,
						SessionAttribute.authenticatedUser.value(), SessionAttribute.clientIP.value(), SessionAttribute.deviceUUID.value(),
						SessionKey.ADMIN_ROLES.toString());
				for (var sessionId : sessionIds) {
					if (sessionId == null || sessionId.isBlank()) {
						continue;
					}
					boolean isPersisted = persistedSessionIds.contains(sessionId);
					String sessionKey = cfg.getKeyPrefix() + sessionId;
					RMap<String, String> sessionMap = client.getMap(sessionKey, StringCodec.INSTANCE);
					Map<String, String> snapshot;
					try {
						snapshot = sessionMap.getAll(requiredSessionFields);
					} catch (Exception e) {
						snapshot = null;
					}
					if (snapshot == null || snapshot.isEmpty()) {
						if (isPersisted) {
							staleSessions.add(sessionId);
							continue;
						}
						int contextsCount = contextsBySession.getOrDefault(sessionId, 0);
						if (contextsCount == 0) {
							continue;
						}
						Element sessionElement = document.createElement("session");
						sessionElement.setAttribute("sessionID", sessionId);
						sessionElement.setAttribute("authenticatedUser", "");
						sessionElement.setAttribute("contexts", Integer.toString(contextsCount));
						sessionElement.setAttribute("clientIP", "");
						sessionElement.setAttribute("deviceUUID", "");
						sessionElement.setAttribute("lastSessionAccessDate", "");
						sessionElement.setAttribute("sessionInactivityTime", "");
						sessionElement.setAttribute("adminRoles", Integer.toString(0));
						if (sessionId.equals(currentSessionId)) {
							sessionElement.setAttribute("isCurrentSession", "true");
							Set<HttpServletRequest> set = SessionAttribute.fullSyncRequests.get(currentSession);
							sessionElement.setAttribute("isFullSyncActive", Boolean.toString(set != null && !set.isEmpty()));
						} else {
							sessionElement.setAttribute("isFullSyncActive", Boolean.toString(false));
						}
						sessionsListElement.appendChild(sessionElement);
						continue;
					}

					long lastAccessedTime = parseLong(snapshot.get(SESSION_META_LAST_ACCESS), 0L);
					int maxInactive = (int) parseLong(snapshot.get(SESSION_META_MAX_INACTIVE), 0L);
					String authenticatedUser = jsonText(snapshot.get(SessionAttribute.authenticatedUser.value()));
					String clientIP = jsonText(snapshot.get(SessionAttribute.clientIP.value()));
					String deviceUUID = jsonText(snapshot.get(SessionAttribute.deviceUUID.value()));
					int contextsCount = contextsBySession.getOrDefault(sessionId, 0);
					int adminRoles = parseAdminRolesCount(snapshot.get(SessionKey.ADMIN_ROLES.toString()));

					Element sessionElement = document.createElement("session");
					sessionElement.setAttribute("sessionID", sessionId);
					sessionElement.setAttribute("authenticatedUser", authenticatedUser != null ? authenticatedUser : "");
					sessionElement.setAttribute("contexts", Integer.toString(contextsCount));
					sessionElement.setAttribute("clientIP", clientIP != null ? clientIP : "");
					sessionElement.setAttribute("deviceUUID", deviceUUID != null ? deviceUUID : "");
					if (lastAccessedTime > 0) {
						sessionElement.setAttribute("lastSessionAccessDate",
								DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(lastAccessedTime)));
						sessionElement.setAttribute("sessionInactivityTime",
								formatTime((now - lastAccessedTime) / 1000) + " / " + formatTime(maxInactive));
					} else {
						sessionElement.setAttribute("lastSessionAccessDate", "");
						sessionElement.setAttribute("sessionInactivityTime", "");
					}
					sessionElement.setAttribute("adminRoles", Integer.toString(adminRoles));
					if (sessionId.equals(currentSessionId)) {
						sessionElement.setAttribute("isCurrentSession", "true");
						Set<HttpServletRequest> set = SessionAttribute.fullSyncRequests.get(currentSession);
						sessionElement.setAttribute("isFullSyncActive", Boolean.toString(set != null && !set.isEmpty()));
					} else {
						sessionElement.setAttribute("isFullSyncActive", Boolean.toString(false));
					}
					sessionsListElement.appendChild(sessionElement);
				}
				if (!staleSessions.isEmpty()) {
					try {
						sessionsIndex.removeAll(staleSessions);
						for (var sid : staleSessions) {
							sessionIds.remove(sid);
						}
					} catch (Exception ignore) {
						// ignore
					}
				}
			}

			return new RedisCounts(sessionIds.size(), contextIds.size());
		} catch (Exception e) {
			Engine.logAdmin.warn("[connections.List] redis list failure: " + e, e);
			return new RedisCounts(0, 0);
		}
	}

	private static String extractSessionId(String contextId) {
		if (contextId == null) {
			return null;
		}
		int idx = contextId.indexOf('_');
		if (idx <= 0) {
			return null;
		}
		return contextId.substring(0, idx);
	}

	private static String jsonText(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			JsonNode node = JSON.readTree(raw);
			if (node == null || node.isNull()) {
				return null;
			}
			return node.isTextual() ? node.textValue() : node.asText();
		} catch (Exception e) {
			return raw;
		}
	}

	private static long jsonLong(String raw, long defaultValue) {
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}
		try {
			JsonNode node = JSON.readTree(raw);
			if (node == null || node.isNull()) {
				return defaultValue;
			}
			return node.isNumber() ? node.longValue() : node.asLong(defaultValue);
		} catch (Exception e) {
			return parseLong(raw, defaultValue);
		}
	}

	private static int jsonInt(String raw, int defaultValue) {
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}
		try {
			JsonNode node = JSON.readTree(raw);
			if (node == null || node.isNull()) {
				return defaultValue;
			}
			return node.isNumber() ? node.intValue() : node.asInt(defaultValue);
		} catch (Exception e) {
			return (int) parseLong(raw, defaultValue);
		}
	}

	private static long parseLong(String raw, long defaultValue) {
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(raw);
		} catch (Exception ignore) {
			return defaultValue;
		}
	}

	private static int parseAdminRolesCount(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		try {
			JsonNode node = JSON.readTree(raw);
			if (node == null || node.isNull()) {
				return 0;
			}
			if (node.isArray()) {
				return node.size();
			}
			JsonNode value = node.get("value");
			return value != null && value.isArray() ? value.size() : 0;
		} catch (Exception ignore) {
			return 0;
		}
	}

	private static Map<String, String> parseInflightSnapshot(String rawJson, Set<String> requiredFields) {
		if (rawJson == null || rawJson.isBlank() || requiredFields == null || requiredFields.isEmpty()) {
			return null;
		}
		try {
			JsonNode root = JSON.readTree(rawJson);
			if (root == null || root.isNull() || !root.isObject()) {
				return null;
			}
			var map = new HashMap<String, String>(requiredFields.size());
			for (var key : requiredFields) {
				JsonNode value = root.get(key);
				if (value == null || value.isNull()) {
					continue;
				}
				map.put(key, value.toString());
			}
			return map;
		} catch (Exception ignore) {
			return null;
		}
	}
}
