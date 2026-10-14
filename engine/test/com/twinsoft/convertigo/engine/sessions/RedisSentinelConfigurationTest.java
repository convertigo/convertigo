/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.sessions;

import static org.junit.Assert.*;
import static com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.*;

import org.junit.Before;
import org.junit.Test;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class RedisSentinelConfigurationTest {
	@Before
	public void defaults() throws Exception {
		Engine.isStarted = false;
		EnginePropertiesManager.initProperties();
		for (var property : EnginePropertiesManager.PropertyName.values()) {
			if (property.name().startsWith("SESSION_REDIS_")) {
				EnginePropertiesManager.setProperty(property, property.getDefaultValue());
			}
		}
	}

	private RedisSessionConfiguration sentinel() {
		EnginePropertiesManager.setProperty(SESSION_REDIS_TOPOLOGY, "sentinel");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_MASTER, "mymaster");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_NODES, "sentinel-a:26379, sentinel-b:26379\n[::1]:26379 sentinel-a:26379");
		return RedisSessionConfiguration.fromProperties();
	}

	@Test
	public void singleIsBackwardCompatible() {
		var cfg = RedisSessionConfiguration.fromProperties();
		assertEquals(RedisTopology.single, cfg.getTopology());
		var redisson = RedisClients.createConfig(cfg);
		assertEquals("redis://localhost:6379", redisson.useSingleServer().getAddress());
		assertEquals(128, redisson.useSingleServer().getConnectionPoolSize());
		assertEquals(32, redisson.useSingleServer().getConnectionMinimumIdleSize());
	}

	@Test
	public void sentinelUsesMasterForReadsAndSubscriptionsAndExistingClientSettings() {
		sentinel();
		EnginePropertiesManager.setProperty(SESSION_REDIS_DATABASE, "3");
		EnginePropertiesManager.setProperty(SESSION_REDIS_TIMEOUT, "1234");
		EnginePropertiesManager.setProperty(SESSION_REDIS_CONNECTION_POOL_SIZE, "12");
		EnginePropertiesManager.setProperty(SESSION_REDIS_CONNECTION_MINIMUM_IDLE_SIZE, "4");
		EnginePropertiesManager.setProperty(SESSION_REDIS_USERNAME, "data-user");
		EnginePropertiesManager.setProperty(SESSION_REDIS_PASSWORD, "data-secret");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_USERNAME, "sentinel-user");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_PASSWORD, "sentinel-secret");
		var cfg = RedisSessionConfiguration.fromProperties();
		var redisson = RedisClients.createConfig(cfg);
		var servers = redisson.useSentinelServers();
		assertEquals("mymaster", servers.getMasterName());
		assertEquals(3, servers.getSentinelAddresses().size());
		assertEquals(ReadMode.MASTER, servers.getReadMode());
		assertEquals(SubscriptionMode.MASTER, servers.getSubscriptionMode());
		assertEquals(3, servers.getDatabase());
		assertEquals(1234, servers.getTimeout());
		assertEquals(12, servers.getMasterConnectionPoolSize());
		assertEquals(4, servers.getMasterConnectionMinimumIdleSize());
		assertEquals("data-user", redisson.getUsername());
		assertEquals("data-secret", redisson.getPassword());
		assertEquals("sentinel-user", servers.getSentinelUsername());
		assertEquals("sentinel-secret", servers.getSentinelPassword());
		assertFalse(cfg.describe().contains("data-secret"));
		assertFalse(cfg.describe().contains("sentinel-secret"));
		assertFalse(cfg.diagnostics().toString().contains("secret"));
		assertEquals("sentinel", cfg.diagnostics().get("topology"));
		assertFalse(cfg.describe().contains("localhost:6379"));
	}

	@Test
	public void tlsAppliesToSentinelAndDataNodes() {
		sentinel();
		EnginePropertiesManager.setProperty(SESSION_REDIS_SSL, "true");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SSL_TRUSTSTORE, "/tmp/ca.p12");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SSL_KEYSTORE, "/tmp/client.p12");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SSL_KEYSTORE_TYPE, "PKCS12");
		var cfg = RedisSessionConfiguration.fromProperties();
		for (var address : cfg.getSentinelAddresses()) {
			assertTrue(address.startsWith("rediss://"));
		}
		var redisson = RedisClients.createConfig(cfg);
		assertEquals("file:/tmp/ca.p12", redisson.getSslTruststore().toString());
		assertEquals("file:/tmp/client.p12", redisson.getSslKeystore().toString());
	}

	@Test
	public void rejectsIncompleteOrUnsafeConfigurationWithoutEchoingCredentials() {
		EnginePropertiesManager.setProperty(SESSION_REDIS_TOPOLOGY, "cluster");
		assertThrows(IllegalArgumentException.class, RedisSessionConfiguration::fromProperties);
		EnginePropertiesManager.setProperty(SESSION_REDIS_TOPOLOGY, "sentinel");
		assertThrows(IllegalArgumentException.class, RedisSessionConfiguration::fromProperties);
		sentinel();
		for (var node : new String[] { "host", "host:0", "host:65536", "redis://user:secret@host:26379",
				"http://host:26379", "rediss://host:26379", "host:26379/path", "host:26379?password=secret" }) {
			EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_NODES, node);
			var error = assertThrows(IllegalArgumentException.class, RedisSessionConfiguration::fromProperties);
			assertFalse(error.getMessage().contains("secret"));
		}
	}

	@Test
	public void makesTheRedissonSentinelOnlyAuthenticationLimitationExplicit() {
		var cfg = sentinel();
		assertNotNull(RedisClients.createConfig(cfg)); // Both endpoints without authentication are supported.
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_PASSWORD, "sentinel-secret");
		var authenticated = RedisSessionConfiguration.fromProperties();
		var error = assertThrows(IllegalArgumentException.class, () -> RedisClients.createConfig(authenticated));
		assertTrue(error.getMessage().contains("no Redis data password"));
		assertFalse(error.getMessage().contains("sentinel-secret"));
	}

	@Test
	public void failedReloadKeepsTheEffectiveClientAndConfiguration() throws Exception {
		var original = sentinel();
		var clientField = RedisClients.class.getDeclaredField("client");
		var configurationField = RedisClients.class.getDeclaredField("configuration");
		clientField.setAccessible(true);
		configurationField.setAccessible(true);
		var previousClient = clientField.get(null);
		var previousConfiguration = configurationField.get(null);
		var fakeClient = (org.redisson.api.RedissonClient) java.lang.reflect.Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[] { org.redisson.api.RedissonClient.class },
				(proxy, method, args) -> { throw new AssertionError("Unexpected client call: " + method.getName()); });
		try {
			clientField.set(null, fakeClient);
			configurationField.set(null, original);
			EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_PASSWORD, "sentinel-secret");
			assertThrows(IllegalArgumentException.class, RedisClients::reload);
			assertSame(original, RedisClients.getConfiguration());
			assertSame(fakeClient, RedisClients.getClient());
		} finally {
			clientField.set(null, previousClient);
			configurationField.set(null, previousConfiguration);
		}
	}

	@Test
	public void reloadDetectsSentinelSettingsAndDoesNotExposeMutableAddresses() {
		var original = sentinel();
		assertEquals(original, RedisSessionConfiguration.fromProperties());
		assertEquals(original.hashCode(), RedisSessionConfiguration.fromProperties().hashCode());
		var addresses = original.getSentinelAddresses();
		addresses[0] = "redis://changed:26379";
		assertEquals("redis://sentinel-a:26379", original.getSentinelAddresses()[0]);
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_PASSWORD, "changed");
		assertNotEquals(original, RedisSessionConfiguration.fromProperties());
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_PASSWORD, "");
		EnginePropertiesManager.setProperty(SESSION_REDIS_SENTINEL_MASTER, "another-master");
		assertNotEquals(original, RedisSessionConfiguration.fromProperties());
	}
}
