/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;

public class PacManagerTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Engine previousApp;
	private boolean previousStarted;
	private Logger previousLogger;
	private Logger logger;
	private final List<String> messages = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();
	private ProxyManager manager;
	private PacManager parser;

	@Before public void setup() throws Exception {
		previousApp = Engine.theApp;
		previousStarted = Engine.isStarted;
		previousLogger = Engine.logProxyManager;
		Engine.theApp = null;
		Engine.isStarted = false;
		EnginePropertiesManager.initProperties();
		manager = new ProxyManager();
		logger = Logger.getLogger("pac-manager-test");
		logger.setLevel(Level.DEBUG);
		logger.setAdditivity(false);
		logger.addAppender(new AppenderSkeleton() {
			@Override protected void append(LoggingEvent event) { messages.add(event.getRenderedMessage()); if (event.getLevel() == Level.WARN) warnings.add(event.getRenderedMessage()); }
			@Override public void close() { }
			@Override public boolean requiresLayout() { return false; }
		});
		Engine.logProxyManager = logger;
		parser = new PacManager("unused");
	}

	@After public void restore() {
		logger.removeAllAppenders();
		EnginePropertiesManager.unload();
		Engine.theApp = previousApp;
		Engine.isStarted = previousStarted;
		Engine.logProxyManager = previousLogger;
	}

	private void assertProxy(String response, String host, int port) {
		var info = parser.parsePacResult(response);
		assertNotNull(response, info);
		assertEquals(host, info.getServer());
		assertEquals(port, info.getPort());
	}

	@Test public void preservesAllRoutesIncludingDirectInTheirDeclaredOrder() {
		var routes = parser.parsePacProxies("PROXY first.invalid:8000; DIRECT; PROXY second.invalid:8001");
		assertEquals(3, routes.size());
		assertEquals("first.invalid", ((java.net.InetSocketAddress) routes.get(0).address()).getHostString());
		assertEquals(java.net.Proxy.NO_PROXY, routes.get(1));
		assertEquals("second.invalid", ((java.net.InetSocketAddress) routes.get(2).address()).getHostString());
	}

	@Test public void selectsTheFirstOfFourProxies() {
		assertProxy("PROXY proxy1.example:8000; PROXY proxy4.example:8000; PROXY proxy2.example:8000; PROXY proxy3.example:8000",
				"proxy1.example", 8000);
	}

	@Test public void directStopsTheOrderedSelection() {
		assertNull(parser.parsePacResult("DIRECT"));
		assertNull(parser.parsePacResult(" direct ; PROXY proxy.example:8080"));
		assertProxy("PROXY proxy.example:8080; DIRECT", "proxy.example", 8080);
	}

	@Test public void toleratesWhitespaceAndCase() {
		assertProxy(" ;\tprOxY \t proxy.example : 8000 ; ", "proxy.example", 8000);
		assertNull(parser.parsePacResult("\tDiReCt\r\n"));
	}

	@Test public void skipsUnsupportedAlternativesWithADiagnostic() {
		assertProxy("SOCKS socks.example:1080; HTTPS secure.example:443; PROXY proxy.example:8080; DIRECT",
				"proxy.example", 8080);
		assertTrue(messages.stream().anyMatch(line -> line.contains("unsupported PAC directive: SOCKS")));
		assertTrue(messages.stream().anyMatch(line -> line.contains("unsupported PAC directive: HTTPS")));
		assertNull(parser.parsePacResult("SOCKS socks.example:1080; DIRECT; PROXY proxy.example:8080"));
	}

	@Test public void emptyAndInvalidAnswersHaveNoProxy() {
		for (String result : new String[] {null, "", "  ; ; ", "invalid", "PROXY", "DIRECT unexpected"}) {
			assertNull(result, parser.parsePacResult(result));
		}
	}

	@Test public void skipsInvalidAddressesAndPortsWithoutThrowing() {
		for (String address : List.of("missing-port", ":80", "proxy.example:abc", "proxy.example:-1", "proxy.example:0",
				"proxy.example:65536", "proxy.example:99999999999999999999", "http://proxy.example:80")) {
			assertNull(address, parser.parsePacResult("PROXY " + address));
			assertProxy("PROXY " + address + "; PROXY next.example:3128", "next.example", 3128);
		}
	}

	@Test public void acceptsABracketedIpv6Address() {
		assertProxy("PROXY [::1]:8080", "::1", 8080);
	}

	@Test public void evaluatesRealPacScriptsIncludingNullAnswers() throws Exception {
		var pac = folder.newFile("selection.pac").toPath();
		Files.writeString(pac, "function FindProxyForURL(url, host) { return host == 'direct.example' ? 'DIRECT' : 'PROXY first.example:8000; PROXY second.example:8000'; }");
		var runtime = new PacManager(pac.toUri().toString());
		runtime.start();
		assertEquals("first.example", runtime.getPacInfos("https://target.example/", "target.example").getServer());
		assertNull(runtime.getPacInfos("https://direct.example/", "direct.example"));
		Files.writeString(pac, "function FindProxyForURL(url, host) { return null; }");
		runtime = new PacManager(pac.toString());
		runtime.start();
		assertNull(runtime.getPacInfos("https://target.example/", "target.example"));
		assertNull(org.mozilla.javascript.Context.getCurrentContext());
	}

	@Test public void directClearsAReusedHttpProxyWithoutConfiguringNtlm() throws Exception {
		var pac = folder.newFile("direct.pac").toPath();
		Files.writeString(pac, "function FindProxyForURL(url, host) { return 'DIRECT'; }");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, "auto");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_AUTO, pac.toString());
		manager.getEngineProperties();
		manager.proxyMethod = ProxyMethod.ntlm;
		manager.proxyUser = "username-without-domain";
		var host = new HostConfiguration();
		host.setProxy("previous.example", 8080);
		manager.setProxy(host, new HttpState(), new URI("https://target.example/").toURL());
		assertNull(host.getProxyHost());
	}

	@Test public void manualModeNeverReadsTheConfiguredPac() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, "manual");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_HOST, "manual.example");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_AUTO, folder.getRoot().toPath().resolve("missing.pac").toString());
		for (int i = 0; i < 3; i++) {
			manager.getEngineProperties();
			assertNull(manager.getPacInfos("https://target.example/", "target.example"));
			var host = new HostConfiguration();
			manager.setProxy(host, new HttpState(), new URI("https://target.example/").toURL());
			assertEquals("manual.example", host.getProxyHost());
		}
		assertTrue(messages.stream().noneMatch(line -> line.contains("(PacManager)")));
	}

	@Test public void leavingAutomaticModeReleasesItsPacAndGuardsDirectAccess() throws Exception {
		var pac = folder.newFile("old.pac").toPath();
		Files.writeString(pac, "function FindProxyForURL(url, host) { return 'PROXY automatic.example:8000'; }");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, "auto");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_AUTO, pac.toString());
		manager.getEngineProperties();
		assertNotNull(manager.getPacInfos("https://target.example/", "target.example"));
		manager.proxyMode = ProxyMode.manual;
		assertNull(manager.getPacInfos("https://target.example/", "target.example"));
		Files.delete(pac);
		messages.clear();
		Field field = ProxyManager.class.getDeclaredField("pacUtils");
		field.setAccessible(true);
		for (String mode : List.of("manual", "off")) {
			EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, mode);
			manager.getEngineProperties();
			assertNull(field.get(manager));
			assertNull(manager.getPacInfos("https://target.example/", "target.example"));
		}
		assertTrue(messages.stream().noneMatch(line -> line.contains("(PacManager)")));
	}
	private void automatic(String location) {
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, "auto");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_AUTO, location);
		manager.getEngineProperties();
	}

	@Test public void missingAndEmptyPacLocationsWarnWithoutPublishingAPartialScript() throws Exception {
		for (String location : List.of("", folder.getRoot().toPath().resolve("missing.pac").toString())) {
			warnings.clear();
			automatic(location);
			for (int i = 0; i < 2; i++) assertNull(manager.getPacInfos("https://target.example/", "target.example"));
			assertEquals(1, warnings.size());
			assertTrue(warnings.get(0).contains("PAC is unavailable"));
			assertTrue(warnings.get(0).contains("direct connection"));
		}
	}

	@Test public void failedDownloadWarnsAndAPacCanBeAppliedAgain() throws Exception {
		var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		var status = new java.util.concurrent.atomic.AtomicInteger(404);
		server.createContext("/", exchange -> {
			byte[] body = "function FindProxyForURL(url, host) { return 'PROXY loaded.example:8080'; }".getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status.get(), body.length);
			try (var out = exchange.getResponseBody()) { out.write(body); }
		});
		server.start();
		try {
			String location = "http://127.0.0.1:" + server.getAddress().getPort() + "/proxy.pac";
			automatic(location);
			assertNull(manager.getPacInfos("https://target.example/", "target.example"));
			assertEquals(1, warnings.size());
			status.set(200);
			warnings.clear();
			automatic(location);
			assertEquals("loaded.example", manager.getPacInfos("https://target.example/", "target.example").getServer());
			assertTrue(warnings.isEmpty());
		} finally { server.stop(0); }
	}

	@Test public void invalidScriptsWarnAndDirectIsNotReportedAsUnavailable() throws Exception {
		var file = folder.newFile("invalid.pac").toPath();
		for (String script : List.of("", "function FindProxyForURL( {", "var other = 1;")) {
			Files.writeString(file, script);
			warnings.clear();
			automatic(file.toString());
			assertNull(manager.getPacInfos("https://target.example/", "target.example"));
			assertEquals(1, warnings.size());
			assertNull(org.mozilla.javascript.Context.getCurrentContext());
		}
		Files.writeString(file, "function FindProxyForURL(url, host) { return 'DIRECT'; }");
		warnings.clear();
		automatic(file.toString());
		assertNull(manager.getPacInfos("https://target.example/", "target.example"));
		assertTrue(warnings.isEmpty());
	}

	@Test public void automaticModeWithoutAPacManagerWarns() {
		manager.proxyMode = ProxyMode.auto;
		assertNull(manager.getPacInfos("https://target.example/", "target.example"));
		assertTrue(warnings.stream().anyMatch(line -> line.contains("PAC is unavailable")));
	}

}
