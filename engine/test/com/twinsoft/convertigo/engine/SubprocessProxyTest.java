/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.junit.*;

import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.proxy.ntlm.NtlmConnectProxyBridge;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

public class SubprocessProxyTest {
	private Engine previous;
	private ProxyManager manager;
	private final List<Closeable> resources = new ArrayList<>();
	private final List<HttpServer> httpServers = new ArrayList<>();

	@Before public void setup() throws Exception {
		previous = Engine.theApp;
		Engine.logEngine = Engine.logProxyManager = Logger.getLogger("subprocess-proxy-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		manager = new ProxyManager();
		manager.proxyMode = ProxyMode.manual;
		manager.proxyMethod = ProxyMethod.anonymous;
		manager.setBypassDomains("");
		manager.setProxyUser("DOMAIN\\fixture");
		manager.setProxyPassword("test-only-password");
		Engine.theApp.proxyManager = manager;
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, "manual");
	}

	@After public void cleanup() throws Exception {
		NtlmConnectProxyBridge.closeShared();
		for (var resource : resources) resource.close();
		httpServers.forEach(server -> server.stop(0));
		Engine.theApp = previous;
		EnginePropertiesManager.unload();
	}

	private void pac(String body) throws Exception {
		PacManager pac = new PacManager("") {
			@Override public String getScriptContent() { return "function FindProxyForURL(url,host){" + body + "}"; }
		};
		pac.start();
		var field = ProxyManager.class.getDeclaredField("pacUtils");
		field.setAccessible(true); field.set(manager, pac);
		manager.proxyMode = ProxyMode.auto;
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, "auto");
	}

	private URI localProxy() throws IOException { return URI.create(NtlmConnectProxyBridge.getLocalProxyUrl()); }
	private static String header(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (out.size() < 65536) {
			int value = in.read(); if (value < 0) throw new EOFException(); out.write(value);
			if (out.toString(StandardCharsets.ISO_8859_1).endsWith("\r\n\r\n")) return out.toString(StandardCharsets.ISO_8859_1);
		}
		throw new IOException("Header too large");
	}
	private ServerSocket echoProxy(AtomicInteger calls, boolean requireBasic) throws IOException {
		ServerSocket server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress()); resources.add(server);
		Thread thread = new Thread(() -> {
			while (!server.isClosed()) try (Socket client = server.accept()) {
				String request = header(client.getInputStream());
				calls.incrementAndGet();
				String expected = Base64.getEncoder().encodeToString("DOMAIN\\fixture:test-only-password".getBytes(StandardCharsets.ISO_8859_1));
				if (requireBasic && !request.contains("Proxy-Authorization: Basic " + expected)) {
					client.getOutputStream().write("HTTP/1.1 407 Proxy Authentication Required\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)); continue;
				}
				client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
				client.getInputStream().transferTo(client.getOutputStream());
			} catch (IOException ignored) { }
		}); thread.setDaemon(true); thread.start(); return server;
	}
	private byte[] throughTunnel(String host, int port) throws IOException {
		URI proxy = localProxy();
		try (Socket client = new Socket(proxy.getHost(), proxy.getPort())) {
			client.setSoTimeout(3000);
			client.getOutputStream().write(("CONNECT " + host + ":" + port + " HTTP/1.1\r\nHost: " + host + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
			assertTrue(header(client.getInputStream()).startsWith("HTTP/1.1 200"));
			byte[] payload = new byte[32768]; Arrays.fill(payload, (byte) 71);
			client.getOutputStream().write(payload); client.shutdownOutput();
			byte[] actual = client.getInputStream().readAllBytes(); assertArrayEquals(payload, actual); return actual;
		}
	}
	private void manual(int port) { manager.proxyMode = ProxyMode.manual; manager.setProxyServer("127.0.0.1"); manager.setProxyPort(port); }

	@Test(timeout = 15000) public void connectSupportsAnonymousAndBasicWithoutExposingCredentials() throws Exception {
		var calls = new AtomicInteger(); var proxy = echoProxy(calls, false); manual(proxy.getLocalPort());
		throughTunnel("service.invalid", 443);
		manager.proxyMethod = ProxyMethod.basic; proxy = echoProxy(calls, true); manual(proxy.getLocalPort());
		throughTunnel("service.invalid", 443); assertEquals(2, calls.get());
		assertNull(localProxy().getUserInfo());
	}

	@Test(timeout = 15000) public void pacUsesDestinationOnEveryRequestAndKeepsTheLocalPort() throws Exception {
		var a = new AtomicInteger(); var b = new AtomicInteger();
		var first = echoProxy(a, false); var second = echoProxy(b, false);
		pac("return host === 'one.invalid' ? 'PROXY 127.0.0.1:" + first.getLocalPort() + "' : 'PROXY 127.0.0.1:" + second.getLocalPort() + "';");
		URI before = localProxy(); throughTunnel("one.invalid", 443); throughTunnel("two.invalid", 443);
		assertEquals(1, a.get()); assertEquals(1, b.get()); assertEquals(before, localProxy());
		manual(first.getLocalPort()); assertEquals(before, localProxy());
	}

	@Test(timeout = 15000) public void triesOrderedPacAlternativesWithoutAddingDirect() throws Exception {
		var calls = new AtomicInteger(); var good = echoProxy(calls, false);
		int closed; try (var socket = new ServerSocket(0)) { closed = socket.getLocalPort(); }
		pac("return 'PROXY 127.0.0.1:" + closed + "; PROXY 127.0.0.1:" + good.getLocalPort() + "';");
		throughTunnel("service.invalid",443); assertEquals(1,calls.get());
		pac("return 'PROXY 127.0.0.1:" + closed + "';");
		try (Socket client = new Socket(localProxy().getHost(),localProxy().getPort())) {
			client.setSoTimeout(3000); client.getOutputStream().write("CONNECT service.invalid:443 HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
			assertTrue(header(client.getInputStream()).startsWith("HTTP/1.1 502"));
		}
	}

	@Test(timeout = 15000) public void directConnectTransfersBothDirectionsAndHalfCloses() throws Exception {
		var server = new ServerSocket(0,10,InetAddress.getLoopbackAddress()); resources.add(server);
		Thread thread = new Thread(() -> { try (Socket socket = server.accept()) { socket.getInputStream().transferTo(socket.getOutputStream()); } catch (IOException ignored) { } }); thread.setDaemon(true); thread.start();
		int closed; try (var socket = new ServerSocket(0)) { closed = socket.getLocalPort(); }
		pac("return 'PROXY 127.0.0.1:" + closed + "; DIRECT';"); throughTunnel("127.000.000.001",server.getLocalPort());
	}

	@Test(timeout = 15000) public void plainHttpUsesTheFullUrlAndPreservesPostBodiesAndOriginAuthorization() throws Exception {
		var seen = new ArrayList<String>(); var body = new ArrayList<String>();
		HttpServer proxy = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
		proxy.createContext("/", exchange -> {
			if (exchange.getRequestHeaders().getFirst("Proxy-Authorization") == null) {
				exchange.getResponseHeaders().set("Proxy-Authenticate", "Basic realm=fixture"); exchange.sendResponseHeaders(407,-1); exchange.close(); return;
			}
			seen.add(exchange.getRequestURI().toString()); body.add(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
			assertEquals("Bearer origin-only",exchange.getRequestHeaders().getFirst("Authorization"));
			exchange.sendResponseHeaders(200,2); exchange.getResponseBody().write(new byte[]{'O','K'}); exchange.close();
		});proxy.start();httpServers.add(proxy);manual(proxy.getAddress().getPort());manager.proxyMethod=ProxyMethod.basic;
		int closed; try (var socket = new ServerSocket(0)) { closed = socket.getLocalPort(); }
		pac("return 'PROXY 127.0.0.1:" + closed + "; PROXY 127.0.0.1:" + proxy.getAddress().getPort() + "';");
		URI local=localProxy();
		try(var client=HttpClients.custom().setProxy(new org.apache.http.HttpHost(local.getHost(),local.getPort())).setDefaultRequestConfig(org.apache.http.client.config.RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(3000).build()).build()) {
			var request=new HttpPost("http://service.invalid/path?version=1"); request.setHeader("Authorization","Bearer origin-only"); request.setEntity(new ByteArrayEntity("message".getBytes(StandardCharsets.UTF_8)));
			try(var response=client.execute(request)) { assertEquals(200,response.getStatusLine().getStatusCode()); assertEquals("OK",EntityUtils.toString(response.getEntity())); }
		}
		assertEquals(List.of("http://service.invalid/path?version=1"),seen);assertEquals(List.of("message"),body);
	}

	@Test(timeout = 15000) public void everyProcessBuilderUsesTheRelayAndLoopbackExclusions() throws Exception {
		manual(3128);EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS,"internal.invalid,localhost");
		for (String mode : List.of("manual","auto")) for (ProxyMethod auth : ProxyMethod.values()) {
			EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE,mode);manager.proxyMethod=auth;
			var environment=ProcessUtils.getProcessBuilder(null,new ArrayList<>(List.of("python","--version"))).environment();
			URI proxy=URI.create(environment.get("HTTPS_PROXY")); assertEquals("127.0.0.1",proxy.getHost());assertNull(proxy.getUserInfo());
			for(String key:List.of("HTTP_PROXY","http_proxy","https_proxy"))assertEquals(proxy.toString(),environment.get(key));
			for(String host:List.of("internal.invalid","localhost","127.0.0.1","::1"))assertTrue(environment.get("NO_PROXY").contains(host));
			assertEquals(environment.get("NO_PROXY"),environment.get("no_proxy"));assertEquals("1",environment.get("NODE_USE_ENV_PROXY"));
			assertFalse(environment.get("HTTPS_PROXY").contains(manager.getProxyPassword()));
		}
	}

	@Test(timeout = 15000) public void offLeavesAnExplicitCallerEnvironmentUnchanged() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE,"off");
		var environment=new HashMap<>(Map.of("HTTP_PROXY","http://caller.invalid:3128"));
		ProcessUtils.configureProxyEnvironment(environment); assertEquals(Map.of("HTTP_PROXY","http://caller.invalid:3128"),environment);
	}

	@Test(timeout = 15000) public void plainHttpPreservesHeadResponsesAndDecodesChunkedBodies() throws Exception {
		var body = new ArrayList<String>();
		HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
		origin.createContext("/", exchange -> {
			assertNull(exchange.getRequestHeaders().getFirst("Proxy-Authorization"));
			if ("HEAD".equals(exchange.getRequestMethod())) {
				exchange.getResponseHeaders().set("Content-Length", "123"); exchange.sendResponseHeaders(200,-1);
			} else {
				body.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
				exchange.sendResponseHeaders(201,0); exchange.getResponseBody().write("chunked-reply".getBytes(StandardCharsets.UTF_8));
			}
			exchange.close();
		}); origin.start(); httpServers.add(origin);
		String url="http://127.0.0.1:"+origin.getAddress().getPort()+"/";
		URI proxy=localProxy();
		try(var client=HttpClients.custom().setProxy(new org.apache.http.HttpHost(proxy.getHost(),proxy.getPort())).build()) {
			try(var response=client.execute(new HttpHead(url))) {
				assertEquals(200,response.getStatusLine().getStatusCode());assertEquals("123",response.getFirstHeader("Content-Length").getValue());assertNull(response.getEntity());
			}
			var request=new HttpPost(url);var entity=new ByteArrayEntity("chunked-input".getBytes(StandardCharsets.UTF_8)); entity.setChunked(true); request.setEntity(entity);
			request.setHeader("Proxy-Authorization","Basic local-only");
			try(var response=client.execute(request)) {assertEquals(201,response.getStatusLine().getStatusCode());assertEquals("chunked-reply",EntityUtils.toString(response.getEntity()));}
		}
		assertEquals(List.of("chunked-input"),body);
	}

	@Test(timeout = 15000) public void rejectsAuthenticationWithoutFallingBackToDirect() throws Exception {
		var calls=new AtomicInteger();var proxy=echoProxy(calls,true);manager.proxyMethod=ProxyMethod.anonymous;
		pac("return 'PROXY 127.0.0.1:"+proxy.getLocalPort()+"; DIRECT';");
		try(var client=new Socket(localProxy().getHost(),localProxy().getPort())) {
			client.setSoTimeout(3000);client.getOutputStream().write("CONNECT service.invalid:443 HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
			assertTrue(header(client.getInputStream()).startsWith("HTTP/1.1 502"));
		}
		assertEquals(1,calls.get());
	}

	@Test public void processDiscoveryStillWorksBeforeEngineInitialization() throws Exception {
		EnginePropertiesManager.unload();
		assertNotNull(ProcessUtils.getProcessBuilder(null,new ArrayList<>(List.of("java","-version"))));
	}

	@Test public void configuredBypassAndLoopbackRemainDirect() throws Exception {
		manual(3128);manager.setBypassDomains("internal.invalid");
		for(String host:List.of("internal.invalid","localhost","127.0.0.1","[::1]")) assertEquals(List.of(Proxy.NO_PROXY),manager.selectProxies(URI.create("http://"+host+"/")));
	}

	@Test(timeout = 15000) public void destroyClosesTheSharedListener() throws Exception {
		manual(3128);URI local=localProxy();manager.destroy();
		try(Socket ignored=new Socket(local.getHost(),local.getPort())) { fail("Listener should be closed"); } catch(ConnectException expected) { }
	}
}
