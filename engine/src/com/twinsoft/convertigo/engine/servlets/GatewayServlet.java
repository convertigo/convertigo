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
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;

import org.apache.http.HttpHost;
import org.apache.tomcat.websocket.server.WsServerContainer;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.ReverseProxyManager;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.sessions.RedisInstanceDiscovery;

public class GatewayServlet extends org.mitre.dsmiley.httpproxy.ProxyServlet {

	private static final long serialVersionUID = -5125409699734422218L;
	private static final Pattern pKey = Pattern.compile("^/(.*?)(?:/|$)");
	private static final Pattern pDevPort = Pattern.compile(".*/DisplayObjects/dev(\\d+)/");
	private static final String SUBPROTOCOLS = "subprotocols";
	private static final String WSTARGET = "wstarget";

	@Override
	public void init() throws ServletException {
		doLog = false;
		doForwardIP = true;
		doPreserveHost = false;
		doHandleCompression = true;
		doPreserveCookies = true;
		super.init();
	}

	@Override
	protected void initTarget() throws ServletException {
	}

	// npm run ionic:serve --disableHostCheck=true -- --port=5173 --allowed-hosts all
	// http://localhost:28080/convertigo/projects/sampleMobileRetailStore/DisplayObjects/dev5173/Store
	// cssrule = [...document.styleSheets].map(x => [...x.cssRules].find(y => y.selectorText?.startsWith(".class1513949910723"))).filter(x => x)
	
	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		var uri = (String) servletRequest.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
		var targetUri = uri;
		var websocketUri = uri;
		HttpHost targetHost = null;
		
		if (uri == null && servletRequest.getPathInfo() != null) {
			// Direct /gw entry: resolve a local capability, or forward it to its Redis-discovered owner.
			var mKey = pKey.matcher(servletRequest.getPathInfo());
			if (!mKey.find()) {
				servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			var route = resolveRoute(mKey.group(1));
			if (route == null) {
				servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			targetHost = route.host;
			uri = servletRequest.getRequestURI();
			targetUri = (route.basePath == null ? servletRequest.getContextPath() : route.basePath)
					+ servletRequest.getServletPath();
			websocketUri = targetUri + servletRequest.getPathInfo();
		} else {
			// Compatibility entry through ProjectsDataFilter /DisplayObjects/dev<port>.
			var devPort = getDevPort(targetUri);
			if (devPort < 1) {
				servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			targetHost = new HttpHost("127.0.0.1", devPort, "http");
			websocketUri = uri;
		}
		
		if ("websocket".equals(HeaderName.Upgrade.getHeader(servletRequest))) {
			try {
				var wsContainer = (WsServerContainer) getServletContext()
						.getAttribute("jakarta.websocket.server.ServerContainer");
				var config = ServerEndpointConfig.Builder.create(WsProxy.class, uri);
				var subprotocols = HeaderName.SecWebSocketProtocol.getHeader(servletRequest);
				var map = new HashMap<String, String>();
				var query = servletRequest.getQueryString();
				var target = targetHost.toURI().replaceFirst("^http", "ws") + websocketUri
						+ (query == null || query.isBlank() ? "" : "?" + query);
				map.put(WSTARGET, target);
				if (subprotocols != null) {
					config.subprotocols(Arrays.asList(subprotocols.split(", *")));
					map.put(SUBPROTOCOLS, subprotocols);
				}
				wsContainer.upgradeHttpToWebSocket(servletRequest, servletResponse, config.build(), map);
				Engine.logEngine.debug("[GatewayServlet] Upgraded uri " + uri);
			} catch (Exception e) {
				Engine.logEngine.error("[GatewayServlet] Failed to upgrade uri " + uri, e);
			}
		} else {
			servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
			servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
			super.service(servletRequest, servletResponse);
		}
	}

	private static ProxyTarget resolveRoute(String key) {
		var manager = Engine.theApp.reverseProxyManager;
		var local = manager.getHttpHost(key);
		if (local != null) {
			return new ProxyTarget(local, null);
		}
		var instanceId = ReverseProxyManager.getRouteInstanceId(key);
		if (instanceId == null || instanceId.equals(RedisInstanceDiscovery.getLocalInstanceId())) {
			return null;
		}
		try {
			var baseUrl = RedisInstanceDiscovery.resolveBaseUrl(instanceId);
			if (baseUrl == null) {
				return null;
			}
			var remote = URI.create(baseUrl);
			if (remote.getHost() == null || !("http".equalsIgnoreCase(remote.getScheme())
					|| "https".equalsIgnoreCase(remote.getScheme()))) {
				return null;
			}
			var basePath = remote.getPath() == null ? "" : remote.getPath().replaceFirst("/+$", "");
			return new ProxyTarget(new HttpHost(remote.getHost(), remote.getPort(), remote.getScheme()), basePath);
		} catch (Exception e) {
			Engine.logEngine.debug("[GatewayServlet] Unable to resolve route owner for " + key, e);
			return null;
		}
	}

	private static final class ProxyTarget {
		private final HttpHost host;
		private final String basePath;

		private ProxyTarget(HttpHost host, String basePath) {
			this.host = host;
			this.basePath = basePath;
		}
	}
	
	public static int getDevPort(String uri) {
		try {
			var m = pDevPort.matcher(uri);
			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
		} catch (Exception e) {
		}
		return -1;
	}

	static public class WsProxy {
		Session client;
		Session server;

		@OnOpen
		public void onOpen(Session session) {
			server = session;
			Engine.logEngine.trace(
					"[GatewayServlet] Server session open " + session.getId() + " on " + session.getRequestURI());
			var conf = ClientEndpointConfig.Builder.create();
			var map = session.getRequestParameterMap();
			if (map.containsKey(SUBPROTOCOLS)) {
				conf.preferredSubprotocols(Arrays.asList(map.get(SUBPROTOCOLS).get(0).split(", *")));
			}
			try {
				client = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {

					@Override
					public void onOpen(Session session, EndpointConfig config) {
						Engine.logEngine.trace("[GatewayServlet] Client session open " + session.getId());
						session.addMessageHandler(new MessageHandler.Whole<String>() {

							@Override
							public void onMessage(String message) {
								Engine.logEngine.trace("[GatewayServlet] Client onMessage: " + message);
								try {
									WsProxy.this.server.getBasicRemote().sendText(message);
								} catch (IOException e) {
									Engine.logEngine.trace("[GatewayServlet] Failed to sendText on server: " + e);
								}
							}

						});
					}

					@Override
					public void onClose(Session session, CloseReason closeReason) {
						Engine.logEngine.trace("[GatewayServlet] Client session close " + session.getId() + " "
								+ closeReason.getReasonPhrase());
						try {
							server.close(closeReason);
						} catch (IOException e) {
							Engine.logEngine.trace("[GatewayServlet] Failed to close the server: " + e);
						}
						super.onClose(session, closeReason);
					}

					@Override
					public void onError(Session session, Throwable throwable) {
						Engine.logEngine.debug("[GatewayServlet] Client session error " + session.getId() + " " + throwable);
						super.onError(session, throwable);
					}

				}, conf.build(), URI.create(map.get(WSTARGET).get(0)));
			} catch (Exception e) {
				Engine.logEngine.debug("[GatewayServlet] Failed to connectToServer: " + e);
			}
		}

		@OnMessage
		public void onMessage(String message, Session session) {
			Engine.logEngine.trace("[GatewayServlet] Server onMessage: " + message);
			try {
				client.getBasicRemote().sendText(message);
			} catch (IOException e) {
				Engine.logEngine.trace("[GatewayServlet] Failed to sendText on client: " + e);
			}
		}

		@OnClose
		public void onClose(Session session) {
			Engine.logEngine.trace("[GatewayServlet] Server close " + session.getId());
			try {
				client.close();
			} catch (IOException e) {
				Engine.logEngine.trace("[GatewayServlet] Failed to close the client: " + e);
			}
		}

		@OnError
		public void onError(Throwable throwable, Session session) {
			Engine.logEngine.debug("[GatewayServlet] Server error " + session.getId() + ": " + throwable);
		}
	}
}
