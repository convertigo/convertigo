package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.message.BasicHttpRequest;
import org.junit.Test;

public class HttpUtilsProxyHeadersTest {
	private HttpClientContext context(boolean secure) {
		var context = HttpClientContext.create();
		context.setAttribute(HttpClientContext.HTTP_ROUTE, new HttpRoute(
				new HttpHost("origin.example", secure ? 443 : 80, secure ? "https" : "http"),
				null, new HttpHost("proxy.example", 3128), secure));
		return context;
	}

	@Test public void originRequestInATunnelKeepsOnlyOriginAuthorization() {
		var request = new BasicHttpRequest("GET", "/models");
		request.setHeader("Proxy-Authorization", "fixture-proxy");
		request.setHeader("Authorization", "fixture-origin");
		HttpUtils.removeTunnelProxyAuthorization(request, context(true));
		assertFalse(request.containsHeader("Proxy-Authorization"));
		assertEquals("fixture-origin", request.getFirstHeader("Authorization").getValue());
	}

	@Test public void connectRequestKeepsItsProxyAuthorization() {
		var request = new BasicHttpRequest("CONNECT", "origin.example:443");
		request.setHeader("Proxy-Authorization", "fixture-proxy");
		HttpUtils.removeTunnelProxyAuthorization(request, context(true));
		assertTrue(request.containsHeader("Proxy-Authorization"));
	}

	@Test public void plainProxyRequestKeepsItsProxyAuthorization() {
		var request = new BasicHttpRequest("GET", "http://origin.example/models");
		request.setHeader("Proxy-Authorization", "fixture-proxy");
		HttpUtils.removeTunnelProxyAuthorization(request, context(false));
		assertTrue(request.containsHeader("Proxy-Authorization"));
	}
}
