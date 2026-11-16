/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.GoogleAnalyticsTicketManager;
import com.twinsoft.convertigo.engine.billing.TicketQueue;
import com.twinsoft.convertigo.engine.util.Crypto2;

/** Runs in a fresh JVM for each passphrase; embedded analytics values are never printed. */
public class BillingPassphraseTest {
	private BillingManager billing;
	private static String configuredPassphrase;
	private static String cipherBeforeInitialization;
	private static final String SAMPLE = "hello";

	public static void main(String[] args) {
		System.setProperty("billing.test.customPassphrase", Boolean.toString("custom".equals(args[0])));
		var result = JUnitCore.runClasses(BillingPassphraseTest.class);
		for (var failure : result.getFailures()) {
			System.out.println(failure.getDescription().getMethodName() + "=false");
		}
		System.out.println("billingPassphrase_" + args[0] + "=" + result.wasSuccessful());
		if (!result.wasSuccessful()) {
			throw new AssertionError("Billing passphrase checks failed");
		}
	}

	@BeforeClass
	public static void initialize() throws Exception {
		// This suppresses the analytics manager's background server-address lookup. No ticket is sent.
		System.setProperty("convertigo.cloud.customer_name", "billing-passphrase-test");
		BillingBench.setUpEngine();
		Logger.getRootLogger().setLevel(Level.OFF);
		Engine.logEngine.setLevel(Level.OFF);
		Engine.logBillers.setLevel(Level.OFF);
		if (Boolean.getBoolean("billing.test.customPassphrase")) {
			EnginePropertiesManager.setProperty(PropertyName.CRYPTO_PASSPHRASE, "unit-test-passphrase");
		}
		configuredPassphrase = EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE);
		cipherBeforeInitialization = Crypto2.encodeToHexString(SAMPLE);
	}

	@After
	public void destroy() throws Exception {
		if (billing != null) {
			billing.destroy();
		}
		assertTrue("Configured passphrase must be preserved", configuredPassphrase.equals(
				EnginePropertiesManager.getProperty(PropertyName.CRYPTO_PASSPHRASE)));
		assertTrue("Stored ciphertext must stay compatible", SAMPLE.equals(
				Crypto2.decodeFromHexString(cipherBeforeInitialization)));
	}

	private static Field field(Class<?> type, String name) throws Exception {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private void start(boolean studio, boolean sentinel) throws Exception {
		field(Engine.class, "bStudioMode").setBoolean(null, studio);
		Object builtInValue = field(BillingManager.class, "dkey").get(null);
		assertTrue("Embedded setting must decode independently of the configured passphrase", builtInValue != null);
		EnginePropertiesManager.setProperty(PropertyName.ANALYTICS_GOOGLE_ID,
				sentinel ? (String) builtInValue : "G-SYNTHETIC");
		billing = new BillingManager();
		billing.init();
	}

	@Test
	public void studioInitializesWithoutServerMonitoring() throws Exception {
		start(true, false);
		assertFalse(field(BillingManager.class, "serverMonitor").getBoolean(billing));
		assertFalse(billing.hasActiveManagers());
	}

	@Test
	public void sentinelDisablesServerMonitoringAndCreatesNoManager() throws Exception {
		start(false, true);
		assertFalse(field(BillingManager.class, "serverMonitor").getBoolean(billing));
		assertFalse(billing.hasActiveManagers());
	}

	@Test
	public void serverMonitoringUsesTheSameSettingsWithoutOpeningAnHttpClient() throws Exception {
		start(false, false);
		assertTrue(field(BillingManager.class, "serverMonitor").getBoolean(billing));
		List<?> queues = (List<?>) field(BillingManager.class, "queues").get(billing);
		assertTrue("Only the server analytics manager is configured", queues.size() == 1);
		Object manager = ((TicketQueue) queues.get(0)).getManager();
		assertTrue(manager instanceof GoogleAnalyticsTicketManager);
		Object http = field(GoogleAnalyticsTicketManager.class, "http").get(manager);
		assertTrue("No HTTP request must initialize the lazy client", field(http.getClass(), "httpClient").get(http) == null);
		// Replace it with an empty set before teardown, without ever handing it a ticket.
		billing.useTicketManagers(List.of());
		for (Object queue : queues) {
			((TicketQueue) queue).close(true);
		}
		assertFalse(billing.hasActiveManagers());
	}

	@Test
	public void configuredEncryptionKeepsItsKnownCiphertext() {
		String encoded = Crypto2.encodeToHexString(SAMPLE);
		assertTrue(encoded.equals(Crypto2.encodeToHexString(configuredPassphrase, SAMPLE)));
		assertTrue(SAMPLE.equals(Crypto2.decodeFromHexString(encoded)));
		if (Boolean.getBoolean("billing.test.customPassphrase")) {
			// The same public synthetic vector used by Crypto2Test, never an installed secret.
			assertTrue(encoded.equals("xe86d5ccd43916ea5f793a0c52ef7b171"));
		}
	}
}
