/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.junit.Test;

public class GatewayServletTest {
	@Test
	public void aTargetListensOnlyWhileItsPortAcceptsConnections() throws Exception {
		int port;
		try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			port = server.getLocalPort();
			assertTrue(GatewayServlet.isListening(new HttpHost("127.0.0.1", port, "http")));
		}
		assertFalse(GatewayServlet.isListening(new HttpHost("127.0.0.1", port, "http")));
	}

	@Test
	public void aWebsocketToADevelopmentServerThatDoesNotListenIsRefused() throws Exception {
		// Vite reloads its page as soon as a websocket to its server opens: refused, it keeps polling
		int port;
		try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			port = server.getLocalPort();
		}
		var errors = new ArrayList<Integer>();
		var response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
					if ("sendError".equals(method.getName())) {
						errors.add((Integer) args[0]);
					}
					return null;
				});
		assertTrue(GatewayServlet.refusedUntilListening(new HttpHost("127.0.0.1", port, "http"), response));
		assertEquals(List.of(HttpServletResponse.SC_SERVICE_UNAVAILABLE), errors);
	}
}
