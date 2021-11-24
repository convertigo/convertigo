/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.engine.requesters;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.servlets.DelegateServlet;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.tas.TASException;

/**
 * This class is a workaround class, allowing to detect HTTP session
 * start and end, because of lack of specification from the Servlet
 * 2.2 API.
 */
public class HttpSessionListener implements HttpSessionBindingListener {
	private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss.SSS");
	private static final Map<String, HttpSession> httpSessions = new ConcurrentHashMap<>();
	private static final Set<String> devices = Collections.synchronizedSet(new HashSet<>());

	public void valueBound(HttpSessionBindingEvent event) {
		try {
			Engine.logEngine.debug("HTTP session starting...");
			HttpSession httpSession = event.getSession();
			String httpSessionID = httpSession.getId();
			httpSessions.put(httpSessionID, httpSession);
			Engine.logEngine.debug("HTTP session started [" + httpSessionID + "]");

			if (Engine.isEngineMode() && !devices.contains(httpSessionID)) {
				int maxCV = KeyManager.getMaxCV(Session.EmulIDSE);
				int currentCV = countSessions();
				if (currentCV > maxCV) {
					if (KeyManager.hasExpired(Session.EmulIDSE)) {
						Engine.logEngine.warn("The Standard Edition key is expired");
					} else if (KeyManager.isOverflow(Session.EmulIDSE)) {
						String line = dateFormat.format(new Date()) + "\t" + maxCV + "\t" + currentCV + "\n";
						try {
							FileUtils.write(new File(Engine.LOG_PATH + "/Session License exceeded.log"), line, "UTF-8", true);
						} catch (IOException e1) {
							Engine.logEngine.error("Failed to write the 'Session License exceeded.log' file", e1);
						}
						return;
					}
					Engine.logEngine.error("No more HTTP session available for this Standard Edition.");
					if (DelegateServlet.canDelegate()) {
						JSONObject json = new JSONObject();
						json.put("action", "maxSessionExceeded");
						json.put("currentCV", currentCV);
						json.put("maxCV", maxCV);
						DelegateServlet.delegate(json);
					}
					SessionAttribute.exception.set(event.getSession(), new TASException("Max number of sessions exceeded for " + KeyManager.getEmulatorName(Session.EmulIDSE), false, currentCV, maxCV));
					HttpUtils.terminateSession(event.getSession());
				}
			}
		} catch(Exception e) {
			Engine.logEngine.error("Exception during binding HTTP session listener", e);
		}
	}

	@Override
	public void valueUnbound(HttpSessionBindingEvent event) {
		try {
			Engine.logContext.debug("HTTP session stopping...");
			HttpSession httpSession = event.getSession();
			String httpSessionID = httpSession.getId();

			if (Engine.theApp != null) Engine.theApp.contextManager.removeAll(httpSession);
			removeSession(httpSessionID);
			
			Engine.logContext.debug("HTTP session stopped [" + httpSessionID + "]");
		} catch(Exception e) {
			Engine.logContext.error("Exception during unbinding HTTP session listener", e);
		}
	}

	static public void terminateSession(String httpSessionID) {
		HttpSession session = httpSessions.get(httpSessionID);
		if (session != null) {
			HttpUtils.terminateSession(session);
		}
	}

	static public void removeSession(String httpSessionID) {
		HttpSession httpSession = httpSessions.remove(httpSessionID);
		if (httpSession != null) {
			try {
				Set<File> files = GenericUtils.cast(httpSession.getAttribute("fileToDeleteAtEndOfContext"));
				if (files != null) {
					for (File file: files) {
						FileUtils.deleteQuietly(file);
					}
				}
			} catch (Exception e) {
			}
		}
	}

	static public HttpSession getHttpSession(String sessionID) {
		return httpSessions.get(sessionID);
	}

	static public void removeAllSession() {
		for (Entry<String, HttpSession> entry: httpSessions.entrySet()) {
			HttpUtils.terminateSession(entry.getValue());
		}
	}

	static public void checkSession(HttpServletRequest request) throws TASException {
		HttpSession httpSession = request.getSession(true);
		SessionAttribute.clientIP.set(httpSession, request.getRemoteAddr());
		String uuid = request.getParameter(Parameter.DeviceUUID.getName());
		if (StringUtils.isNotBlank(uuid)) {
			SessionAttribute.deviceUUID.set(httpSession, uuid);
			if (Engine.isCloudMode() && !uuid.startsWith("web-")) {
				devices.add(httpSession.getId());
			}
		}
		if (!SessionAttribute.sessionListener.has(httpSession)) {
			Engine.logContext.trace("Inserting HTTP session listener into the HTTP session");
			SessionAttribute.sessionListener.set(httpSession, new HttpSessionListener());
			Object t;
			if ((t = SessionAttribute.exception.get(httpSession)) != null) {
				if (t instanceof Throwable) {
					((Throwable) t).setStackTrace(new StackTraceElement[0]);
					if (t instanceof TASException) {
						throw (TASException) t;
					}
					throw new RuntimeException((Throwable) t);
				}
			}
		}
	}

	static public Collection<HttpSession> getSessions() {
		return new ArrayList<>(httpSessions.values());
	}

	static public int countSessions() {
		return httpSessions.size() - devices.size();
	}
}
