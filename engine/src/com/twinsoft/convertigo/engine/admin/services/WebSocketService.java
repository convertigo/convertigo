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

package com.twinsoft.convertigo.engine.admin.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.tomcat.websocket.server.WsServerContainer;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public abstract class WebSocketService implements Service {
	private static int cID = 0;
	private static Map<String, WsEntry> entries = new HashMap<>();

	Session session;

	@Override
	public void run(String serviceName, HttpServletRequest request, HttpServletResponse response)
			throws ServiceException {
		if ("websocket".equals(HeaderName.Upgrade.getHeader(request))) {
			var uri = request.getRequestURI();
			try {
				var wsContainer = (WsServerContainer) request.getServletContext()
						.getAttribute("javax.websocket.server.ServerContainer");
				var config = ServerEndpointConfig.Builder.create(WsProxy.class, uri);
				var id = "" + cID++;
				var map = new HashMap<String, String>();
				map.put("id", id);
				synchronized (entries) {
					entries.put(id, new WsEntry(id, this));
					Engine.logAdmin.warn("add id " + id + " [now " + entries.size() + " entries]");
				}
				wsContainer.setDefaultMaxTextMessageBufferSize(100000);
				wsContainer.upgradeHttpToWebSocket(request, response, config.build(), map);
				Engine.logAdmin.debug("[WebSocketService] Upgraded uri " + uri);
			} catch (Exception e) {
				Engine.logAdmin.error("[WebSocketService] Failed to upgrade uri " + uri, e);
			}
		}
	}

	public void onOpen(Session session) {
	}

	public void onMessage(String message, Session session) {
	}

	public void onClose(Session session) {
	}

	public void onError(Throwable throwable, Session session) {
	}

	public void send(String text) throws IOException {
		if (session.isOpen()) {
			var remote = session.getBasicRemote();
			remote.sendText(text);
		}
	}

	static class WsEntry {
		String id;
		Session proxy;
		WebSocketService service;

		WsEntry(String id, WebSocketService service) {
			this.id = id;
			this.service = service;
		}

	}

	static public class WsProxy {
		WsEntry entry;

		@OnOpen
		public void onOpen(Session session) {
			var map = session.getRequestParameterMap();
			entry = entries.get(map.get("id").get(0));
			entry.proxy = session;
			entry.service = entry.service;
			entry.service.session = session;
			entry.service.onOpen(session);
		}

		@OnMessage
		public void onMessage(String message, Session session) {
			entry.service.onMessage(message, session);
		}

		@OnClose
		public void onClose(Session session) {
			synchronized (entries) {
				entries.remove(entry.id);
				Engine.logAdmin.warn("remove id " + entry.id + " [now " + entries.size() + " entries]");
			}
			entry.service.onClose(session);
		}

		@OnError
		public void onError(Throwable throwable, Session session) {
			entry.service.onError(throwable, session);
		}
	}
}
