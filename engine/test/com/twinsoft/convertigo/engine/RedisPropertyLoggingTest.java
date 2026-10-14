/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;
import static com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.*;

import java.util.Properties;

import org.junit.Test;

public class RedisPropertyLoggingTest {
	@Test
	public void masksPasswordsInSystemAndEngineDumpsAndConfigurationUpdates() {
		var properties = new Properties();
		for (var property : new EnginePropertiesManager.PropertyName[] { SESSION_REDIS_PASSWORD,
				SESSION_REDIS_SENTINEL_PASSWORD, SESSION_REDIS_SSL_TRUSTSTORE_PASSWORD,
				SESSION_REDIS_SSL_KEYSTORE_PASSWORD, ADMIN_PASSWORD }) {
			properties.setProperty(property.getKey(), "test-secret");
			properties.setProperty("convertigo.engine." + property.getKey(), "test-secret");
			assertEquals("(hidden)", EnginePropertiesManager.getPropertyValueForLogging(property, "test-secret"));
			assertEquals("(empty)", EnginePropertiesManager.getPropertyValueForLogging(property, ""));
		}
		properties.setProperty("session.redis.sentinel.master", "mymaster");
		var dump = EnginePropertiesManager.getPropertiesAsString("Redis configuration", properties);
		assertFalse(dump.contains("test-secret"));
		assertTrue(dump.contains("session.redis.sentinel.master=mymaster"));
		assertTrue(dump.contains("convertigo.engine.session.redis.sentinel.password=(hidden)"));
		assertEquals("mymaster", EnginePropertiesManager.getPropertyValueForLogging(SESSION_REDIS_SENTINEL_MASTER, "mymaster"));
	}
}
