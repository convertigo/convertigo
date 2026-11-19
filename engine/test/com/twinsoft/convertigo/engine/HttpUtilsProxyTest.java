package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.PacManager.PacInfos;
import com.twinsoft.convertigo.engine.util.HttpUtils;

public class HttpUtilsProxyTest {
	private Engine previous;
	private ProxyManager manager;
	private PacManager pac;
	private final List<HttpServer> servers = new ArrayList<>();

	@Before
	public void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logEngine = Engine.logProxyManager = Logger.getLogger("http-proxy-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		manager = new ProxyManager() {
			@Override
			public PacInfos getPacInfos(String url, String host) {
				return pac.getPacInfos(url, host);
			}
		};
		manager.proxyMode = ProxyMode.manual;
		manager.proxyMethod = ProxyMethod.anonymous;
		manager.setBypassDomains("");
		manager.setProxyUser("BENCH\\proxy-user");
		manager.setProxyPassword("dummy-proxy-password");
		Engine.theApp.proxyManager = manager;
	}

	@After
	public void restore() {
		servers.forEach(server -> server.stop(0));
		Engine.theApp = previous;
	}

	private HttpServer server(HttpHandler handler) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", handler);
		server.start();
		servers.add(server);
		return server;
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length);
		try (var stream = exchange.getResponseBody()) {
			stream.write(bytes);
		}
	}

	private static String proxy(HttpServer server) {
		return "PROXY 127.0.0.1:" + server.getAddress().getPort();
	}

	private void manual(HttpServer server) {
		manager.proxyMode = ProxyMode.manual;
		manager.setProxyServer("127.0.0.1");
		manager.setProxyPort(server.getAddress().getPort());
	}

	private void automatic(String script) {
		pac = new PacManager("") {
			@Override
			public String getScriptContent() {
				return "function FindProxyForURL(url, host) { " + script + " }";
			}
		};
		pac.start();
		manager.proxyMode = ProxyMode.auto;
	}

	private String get(String url) throws IOException {
		return get(url, null);
	}

	private String get(String url, org.apache.http.HttpHost explicitProxy) throws IOException {
		HttpGet request = new HttpGet(url);
		request.setConfig(RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000).setProxy(explicitProxy).build());
		try (var client = HttpUtils.makeHttpClient(false); var response = client.execute(request)) {
			assertEquals(200, response.getStatusLine().getStatusCode());
			return EntityUtils.toString(response.getEntity());
		} finally {
			request.releaseConnection();
		}
	}

	@Test
	public void manualProxyReceivesTheRequest() throws Exception {
		var proxy = server(exchange -> respond(exchange, 200, exchange.getRequestURI().toString()));
		manual(proxy);
		assertEquals("http://service.invalid/metadata?version=1", get("http://service.invalid/metadata?version=1"));
	}

	@Test
	public void bypassAndOffUseTheOrigin() throws Exception {
		var origin = server(exchange -> respond(exchange, 200, "direct"));
		var proxy = server(exchange -> respond(exchange, 502, "unexpected proxy"));
		manual(proxy);
		manager.setBypassDomains("127.0.0.1");
		String url = "http://127.0.0.1:" + origin.getAddress().getPort() + "/metadata";
		assertEquals("direct", get(url));
		manager.setBypassDomains("");
		manager.proxyMode = ProxyMode.off;
		assertEquals("direct", get(url));
	}

	@Test
	public void pacSelectsFirstProxyAndResolvesRedirectsUsingTheFullUrl() throws Exception {
		var firstCalls = new AtomicInteger();
		var secondCalls = new AtomicInteger();
		var second = server(exchange -> {
			secondCalls.incrementAndGet();
			respond(exchange, 200, "redirected");
		});
		var first = server(exchange -> {
			firstCalls.incrementAndGet();
			exchange.getResponseHeaders().set("Location", "http://second.invalid/final?version=2");
			respond(exchange, 302, "redirect");
		});
		automatic("if (url === 'http://first.invalid/start?version=1') return '" + proxy(first)
				+ "; PROXY unavailable.invalid:3128; DIRECT'; "
				+ "if (host === 'second.invalid' && url === 'http://second.invalid/final?version=2') return '"
				+ proxy(second) + "; DIRECT'; return 'PROXY unavailable.invalid:3128';");
		assertEquals("redirected", get("http://first.invalid/start?version=1"));
		assertEquals(1, firstCalls.get());
		assertEquals(1, secondCalls.get());
	}

	@Test
	public void directPacEntryPrecedesLaterProxy() throws Exception {
		var origin = server(exchange -> respond(exchange, 200, "direct"));
		automatic("return 'DIRECT; PROXY unavailable.invalid:3128';");
		assertEquals("direct", get("http://127.0.0.1:" + origin.getAddress().getPort() + "/metadata"));
	}

	@Test
	public void basicAuthenticationWorksInManualAndPacModes() throws Exception {
		String expected = "Basic " + Base64.getEncoder().encodeToString("proxy-user:dummy-proxy-password".getBytes(StandardCharsets.UTF_8));
		var authenticated = new AtomicInteger();
		var proxy = server(exchange -> {
			if (expected.equals(exchange.getRequestHeaders().getFirst("Proxy-Authorization"))) {
				authenticated.incrementAndGet();
				respond(exchange, 200, "authenticated");
			} else {
				exchange.getResponseHeaders().set("Proxy-Authenticate", "Basic realm=\"bench\"");
				respond(exchange, 407, "proxy authentication required");
			}
		});
		manager.proxyMethod = ProxyMethod.basic;
		manager.setProxyUser("proxy-user");
		manual(proxy);
		assertEquals("authenticated", get("http://service.invalid/metadata"));
		automatic("return '" + proxy(proxy) + "; DIRECT';");
		assertEquals("authenticated", get("http://service.invalid/metadata"));
		assertEquals(2, authenticated.get());
	}

	@Test
	public void explicitPacProxyAuthenticatesOnTheFirstRequest() throws Exception {
		var authenticated = new AtomicInteger();
		String expected = "Basic " + Base64.getEncoder().encodeToString("proxy-user:dummy-proxy-password".getBytes(StandardCharsets.UTF_8));
		var proxy = server(exchange -> {
			if (expected.equals(exchange.getRequestHeaders().getFirst("Proxy-Authorization"))) {
				authenticated.incrementAndGet();
				respond(exchange, 200, "authenticated");
			} else {
				exchange.getResponseHeaders().set("Proxy-Authenticate", "Basic realm=\"bench\"");
				respond(exchange, 407, "proxy authentication required");
			}
		});
		manager.proxyMethod = ProxyMethod.basic;
		manager.setProxyUser("proxy-user");
		automatic("return '" + proxy(proxy) + "';");
		var request = new HttpGet("http://service.invalid/metadata");
		request.setConfig(RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000)
				.setProxy(new org.apache.http.HttpHost("127.0.0.1", proxy.getAddress().getPort())).build());
		try (var client = HttpUtils.makeHttpClient(false); var response = client.execute(request)) {
			assertEquals(200, response.getStatusLine().getStatusCode());
		}
		assertEquals(1, authenticated.get());
	}

	@Test
	public void unrelatedExplicitProxyDoesNotReceiveEngineCredentials() throws Exception {
		var credentialHeaders = new AtomicInteger();
		var other = server(exchange -> {
			if (exchange.getRequestHeaders().containsKey("Proxy-Authorization")) credentialHeaders.incrementAndGet();
			exchange.getResponseHeaders().set("Proxy-Authenticate", "Basic realm=\"other\"");
			respond(exchange, 407, "proxy authentication required");
		});
		manager.proxyMethod = ProxyMethod.basic;
		manager.setProxyUser("proxy-user");
		automatic("return 'PROXY configured.example:3128';");
		var request = new HttpGet("http://service.invalid/metadata");
		request.setConfig(RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000)
				.setProxy(new org.apache.http.HttpHost("127.0.0.1", other.getAddress().getPort())).build());
		try (var client = HttpUtils.makeHttpClient(false); var response = client.execute(request)) {
			assertEquals(407, response.getStatusLine().getStatusCode());
		}
		assertEquals(0, credentialHeaders.get());
	}

	private static String ntlmField(byte[] message, int offset) {
		var buffer = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN);
		int length = Short.toUnsignedInt(buffer.getShort(offset));
		return new String(message, buffer.getInt(offset + 4), length, StandardCharsets.UTF_16LE);
	}

	@Test
	public void ntlmHandshakeWorksInManualAndPacModes() throws Exception {
		// Permissive NTLM fixture: validate identity and message types, never an AD password.
		String challenge = "TlRMTVNTUAACAAAAEgASADAAAAAFgoigVElHTzEyMzQAAAAAAAAAACgAKABCAAAAVABJAEcATwBQAFIATwBYAFkAAgASAFQASQBHAE8AUABSAE8AWABZAAEACgBCAEUATgBDAEgAAAAAAA==";
		var typeOne = new AtomicInteger();
		var typeThree = new AtomicInteger();
		var proxy = server(exchange -> {
			String authorization = exchange.getRequestHeaders().getFirst("Proxy-Authorization");
			if (authorization != null && authorization.startsWith("NTLM ")) {
				byte[] message = Base64.getDecoder().decode(authorization.substring(5));
				int type = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN).getInt(8);
				if (type == 1) {
					typeOne.incrementAndGet();
					exchange.getResponseHeaders().set("Proxy-Authenticate", "NTLM " + challenge);
					respond(exchange, 407, "continue NTLM");
					return;
				} else if (type == 3 && "BENCH".equals(ntlmField(message, 28)) && "proxy-user".equals(ntlmField(message, 36))) {
					typeThree.incrementAndGet();
					respond(exchange, 200, "ntlm authenticated");
					return;
				}
			}
			exchange.getResponseHeaders().set("Proxy-Authenticate", "NTLM");
			respond(exchange, 407, "proxy authentication required");
		});
		manager.proxyMethod = ProxyMethod.ntlm;
		manual(proxy);
		assertEquals("ntlm authenticated", get("http://service.invalid/metadata"));
		automatic("return '" + proxy(proxy) + "; DIRECT';");
		assertEquals("ntlm authenticated", get("http://service.invalid/metadata"));
		assertEquals("ntlm authenticated", get("http://service.invalid/metadata",
				new org.apache.http.HttpHost("127.0.0.1", proxy.getAddress().getPort())));
		assertEquals(3, typeOne.get());
		assertEquals(3, typeThree.get());
	}

	@Test
	public void rejectsNtlmIdentityWithoutDomain() throws Exception {
		manual(server(exchange -> respond(exchange, 502, "unexpected request")));
		manager.proxyMethod = ProxyMethod.ntlm;
		manager.setProxyUser("proxy-user");
		var error = assertThrows(IOException.class, () -> get("http://service.invalid/metadata"));
		assertTrue(error.getCause().getMessage().contains("domain\\user"));
	}

	@Test
	public void doesNotSendProxyCredentialsToAnOriginChallenge() throws Exception {
		var credentialHeaders = new AtomicInteger();
		var origin = server(exchange -> {
			if (exchange.getRequestHeaders().containsKey("Authorization") || exchange.getRequestHeaders().containsKey("Proxy-Authorization")) {
				credentialHeaders.incrementAndGet();
			}
			exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"origin\"");
			respond(exchange, 401, "origin authentication required");
		});
		manual(server(exchange -> respond(exchange, 502, "unexpected proxy")));
		manager.proxyMethod = ProxyMethod.basic;
		manager.setBypassDomains("127.0.0.1");
		var request = new HttpGet("http://127.0.0.1:" + origin.getAddress().getPort() + "/metadata");
		try (var client = HttpUtils.makeHttpClient(false); var response = client.execute(request)) {
			assertEquals(401, response.getStatusLine().getStatusCode());
		}
		assertEquals(0, credentialHeaders.get());
	}
	@Test
	public void pooledClientUsesPacSettingsAppliedAfterItsFirstRequest() throws Exception {
		var origin = server(exchange -> respond(exchange, 200, "direct"));
		var proxy = server(exchange -> respond(exchange, 200, "proxied"));
		manager.proxyMode = ProxyMode.off;
		try (var client = HttpUtils.makeHttpClient(true)) {
			var request = new HttpGet("http://127.0.0.1:" + origin.getAddress().getPort() + "/metadata");
			request.setConfig(RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000).build());
			try (var response = client.execute(request)) {
				assertEquals("direct", EntityUtils.toString(response.getEntity()));
			}
			automatic("return '" + proxy(proxy) + "';");
			try (var response = client.execute(request)) {
				assertEquals("proxied", EntityUtils.toString(response.getEntity()));
			}
			automatic("return 'DIRECT';");
			try (var response = client.execute(request)) {
				assertEquals("direct", EntityUtils.toString(response.getEntity()));
			}
		}
	}

	@Test
	public void httpsUsesAConnectTunnelToThePacProxy() throws Exception {
		try (var listener = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
			listener.setSoTimeout(4000);
			var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
			try {
				var received = executor.submit(() -> {
					try (var socket = listener.accept()) {
						socket.setSoTimeout(4000);
						var in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
						String firstLine = in.readLine();
						for (String line; (line = in.readLine()) != null && !line.isEmpty();) { }
						socket.getOutputStream().write("HTTP/1.1 502 Test tunnel refused\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
						return firstLine;
					}
				});
				automatic("return 'PROXY 127.0.0.1:" + listener.getLocalPort() + "';");
				var request = new HttpGet("https://service.invalid/metadata");
				request.setConfig(RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000).build());
				try (var client = HttpUtils.makeHttpClient(false); var response = client.execute(request)) {
					assertEquals(502, response.getStatusLine().getStatusCode());
				}
				assertEquals("CONNECT service.invalid:443 HTTP/1.1", received.get(5, java.util.concurrent.TimeUnit.SECONDS));
			} finally { executor.shutdownNow(); }
		}
	}

}
