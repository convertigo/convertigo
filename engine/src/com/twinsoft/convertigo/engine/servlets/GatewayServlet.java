package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

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

import org.apache.tomcat.websocket.server.WsServerContainer;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;

public class GatewayServlet extends org.mitre.dsmiley.httpproxy.ProxyServlet {

	private static final long serialVersionUID = -5125409699734422218L;
	private static final Pattern pKey = Pattern.compile("^/(.*?)(?:/|$)");
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

	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		var mKey = pKey.matcher(servletRequest.getPathInfo());
		if (!mKey.find()) {
			return;
		}
		var targetHost = Engine.theApp.reverseProxyManager.getHttpHost(mKey.group(1));
		if (targetHost == null) {
			return;
		}
		String uri = servletRequest.getRequestURI();
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
				System.out.println("[GatewayServlet] Upgraded uri " + uri);
			} catch (Exception e) {
				System.err.println(e);
			}
		} else {
			servletRequest.setAttribute(ATTR_TARGET_URI,
					servletRequest.getContextPath() + servletRequest.getServletPath());
			servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
			super.service(servletRequest, servletResponse);
//			System.out.println(uri + " : " + servletResponse.getStatus());
		}
	}

	static public class WsProxy {
		Session client;
		Session server;

		@OnOpen
		public void onOpen(Session session) {
			server = session;
			System.out.println(
					"[GatewayServlet] Server session open " + session.getId() + " on " + session.getRequestURI());
			var conf = ClientEndpointConfig.Builder.create();
			var map = session.getRequestParameterMap();
			if (map.containsKey("subprotocols")) {
				conf.preferredSubprotocols(Arrays.asList(map.get(SUBPROTOCOLS).get(0).split(", *")));
			}
			try {
				client = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {

					@Override
					public void onOpen(Session session, EndpointConfig config) {
						System.out.println("[GatewayServlet] Client session open " + session.getId());
						session.addMessageHandler(new MessageHandler.Whole<String>() {

							@Override
							public void onMessage(String message) {
								System.out.println("[GatewayServlet] Client onMessage: " + message);
								try {
									WsProxy.this.server.getBasicRemote().sendText(message);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

						});
					}

					@Override
					public void onClose(Session session, CloseReason closeReason) {
						System.out.println("[GatewayServlet] Client session close " + session.getId() + " "
								+ closeReason.getReasonPhrase());
						try {
							server.close(closeReason);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						super.onClose(session, closeReason);
					}

					@Override
					public void onError(Session session, Throwable throwable) {
						System.out
								.println("[GatewayServlet] Client session error " + session.getId() + " " + throwable);
						super.onError(session, throwable);
					}

				}, conf.build(), URI.create(map.get(WSTARGET).get(0)));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}

		@OnMessage
		public void onMessage(String message, Session session) {
			System.out.println("[GatewayServlet] Server onMessage: " + message);
			try {
				client.getBasicRemote().sendText(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@OnClose
		public void onClose(Session session) {
			System.out.println("[GatewayServlet] Server close " + session.getId());
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@OnError
		public void onError(Throwable throwable, Session session) {
			System.out.println("[GatewayServlet] Server error " + session.getId());
			throwable.printStackTrace();
		}
	}
}
