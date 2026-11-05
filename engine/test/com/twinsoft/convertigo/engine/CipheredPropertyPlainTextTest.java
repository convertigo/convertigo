package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.Crypto2;

public class CipheredPropertyPlainTextTest {
	private static final String KEY = PropertyName.SESSION_REDIS_PASSWORD.getKey();

	@BeforeClass public static void init() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = null;
	}

	private static String captured(Runnable run) {
		var out = System.out;
		var buffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
		try {
			run.run();
		} finally {
			System.setOut(out);
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}

	@Test public void cipheredValueIsDeciphered() {
		String ciphered = Crypto2.encodeToHexString("Ciphered-Secret-1");
		String log = captured(() -> assertEquals("Ciphered-Secret-1", EnginePropertiesManager.decipherPropertyValue(KEY, ciphered)));
		assertFalse(log.contains("not ciphered"));
	}

	@Test public void plainTextValueIsKeptWithAWarningWithoutTheValue() {
		for (String plain : new String[] { "secret", "MyRedisP@ss2024", "redis-password-123", "Aa1!" }) {
			String log = captured(() -> assertEquals(plain, EnginePropertiesManager.decipherPropertyValue(KEY, plain)));
			assertTrue(log, log.contains("'" + KEY + "' is not ciphered"));
			assertFalse(log, log.contains(plain));
		}
	}

	@Test public void hexadecimalPlainTextValueIsKept() {
		for (String plain : new String[] { "abcdef12", "x0123", "deadbeefcafe0123" }) {
			String log = captured(() -> assertEquals(plain, EnginePropertiesManager.decipherPropertyValue(KEY, plain)));
			assertTrue(log, log.contains("'" + KEY + "' is not ciphered"));
		}
	}

	@Test public void emptyValueStaysEmpty() {
		String log = captured(() -> assertEquals("", EnginePropertiesManager.decipherPropertyValue(KEY, "")));
		assertFalse(log.contains("not ciphered"));
	}

	@Test public void reloadAcceptsAPlainTextValue() throws Exception {
		EnginePropertiesManager.syncProperties(KEY + "=Plain-Reload-2\n");
		assertEquals("Plain-Reload-2", EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_PASSWORD));
		EnginePropertiesManager.syncProperties(KEY + "=" + Crypto2.encodeToHexString("Ciphered-Reload-3") + "\n");
		assertEquals("Ciphered-Reload-3", EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_PASSWORD));
		EnginePropertiesManager.syncProperties(KEY + "=\n");
		assertEquals("", EnginePropertiesManager.getProperty(PropertyName.SESSION_REDIS_PASSWORD));
	}
}
