/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.settings.ProxySettings;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.ProxyManager;

public class WsReferenceProxyTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Engine previousApp;
	private boolean previousStarted;
	private Logger previousLogger;
	private Logger previousEngineLogger;
	private final Map<String, Object> values = new HashMap<>();
	private Settings settings;

	@Before public void setup() throws Exception {
		previousApp = Engine.theApp;
		previousStarted = Engine.isStarted;
		previousLogger = Engine.logProxyManager;
		previousEngineLogger = Engine.logEngine;
		Engine.logEngine = Logger.getLogger("ws-reference-proxy-test");
		Engine.theApp = null;
		Engine.isStarted = false;
		EnginePropertiesManager.initProperties();
		settings = (Settings) Proxy.newProxyInstance(Settings.class.getClassLoader(), new Class<?>[] {Settings.class}, (proxy, method, args) -> {
			switch (method.getName()) {
			case "getBoolean": return values.getOrDefault(args[0], false);
			case "getString": return values.getOrDefault(args[0], args[1]);
			case "setBoolean", "setString":
				values.put((String) args[0], args[1]);
				return method.getReturnType() == void.class ? null : proxy;
			default: throw new AssertionError("Unexpected settings operation: " + method.getName());
			}
		});
	}

	@After public void restore() {
		EnginePropertiesManager.unload();
		Engine.theApp = previousApp;
		Engine.isStarted = previousStarted;
		Engine.logProxyManager = previousLogger;
		Engine.logEngine = previousEngineLogger;
	}

	private void configure(String mode, String response) throws Exception {
		var pac = folder.newFile("proxy.pac").toPath();
		Files.writeString(pac, "function FindProxyForURL(url, host) { return '" + response + "'; }");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_MODE, mode);
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_HOST, "manual.example");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_PORT, "3128");
		EnginePropertiesManager.setProperty(PropertyName.PROXY_SETTINGS_AUTO, pac.toString());
		var manager = new ProxyManager();
		manager.getEngineProperties();
		Engine.theApp = new Engine();
		Engine.theApp.proxyManager = manager;
	}

	@Test public void directDisablesPreviouslyConfiguredSoapProxy() throws Exception {
		configure("auto", "DIRECT; PROXY unused.example:8000");
		values.put(ProxySettings.ENABLE_PROXY, true);
		values.put(ProxySettings.HOST, "previous.example");
		assertTrue(WsReference.configureProxy(settings, "https://target.example/service.wsdl"));
		assertEquals(false, values.get(ProxySettings.ENABLE_PROXY));
	}

	@Test public void automaticModeUsesTheFirstSupportedProxy() throws Exception {
		configure("auto", "SOCKS socks.example:1080; PROXY first.example:8000; PROXY second.example:8000");
		assertTrue(WsReference.configureProxy(settings, "https://target.example/service.wsdl"));
		assertEquals(true, values.get(ProxySettings.ENABLE_PROXY));
		assertEquals("first.example", values.get(ProxySettings.HOST));
		assertEquals("8000", values.get(ProxySettings.PORT));
	}

	@Test public void manualModeKeepsItsConfiguredProxy() throws Exception {
		configure("manual", "DIRECT");
		assertTrue(WsReference.configureProxy(settings, "https://target.example/service.wsdl"));
		assertEquals(true, values.get(ProxySettings.ENABLE_PROXY));
		assertEquals("manual.example", values.get(ProxySettings.HOST));
		assertEquals("3128", values.get(ProxySettings.PORT));
	}
}
