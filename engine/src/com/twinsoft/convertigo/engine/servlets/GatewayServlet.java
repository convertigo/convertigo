package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.http.HttpHost;
import org.apache.tomcat.websocket.server.WsServerContainer;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

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
		doPreserveHost = true;
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
		HttpHost targetHost = null;
		
		if (uri == null && servletRequest.getPathInfo() != null) {
			// enter by the /gw servlet
			var mKey = pKey.matcher(servletRequest.getPathInfo());
			if (!mKey.find()) {
				return;
			}
			targetHost = Engine.theApp.reverseProxyManager.getHttpHost(mKey.group(1));
			if (targetHost == null) {
				return;
			}
			uri = servletRequest.getRequestURI();
			targetUri = servletRequest.getContextPath() + servletRequest.getServletPath();
		} else {
			// enter by the ProjectsDataFilter /DisplayObjects/dev
			targetHost = new HttpHost("localhost", getDevPort(targetUri), "http");
		}
		
		if ("websocket".equals(HeaderName.Upgrade.getHeader(servletRequest))) {
			try {
				var wsContainer = (WsServerContainer) getServletContext()
						.getAttribute("javax.websocket.server.ServerContainer");
				var config = ServerEndpointConfig.Builder.create(WsProxy.class, uri);
				var subprotocols = HeaderName.SecWebSocketProtocol.getHeader(servletRequest);
				var map = new HashMap<String, String>();
				map.put(WSTARGET, targetHost.toURI().replaceFirst("http", "ws") + uri);
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
