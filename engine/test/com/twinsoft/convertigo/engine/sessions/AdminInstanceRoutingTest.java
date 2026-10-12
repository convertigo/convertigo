/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.sessions;

import static org.junit.Assert.*;

import org.junit.Test;

public class AdminInstanceRoutingTest {
	private static final String ROUTE = "route-fdd3ac11-c252-41d0-a0cd-ce1497c7d1b0";
	private static final String RECORD = "{\"instanceId\":\"convertigo-pod-a\",\"routingId\":\"" + ROUTE
			+ "\",\"baseUrl\":\"http://10.0.0.2:28080/convertigo/\",\"startedAt\":12,\"lastHeartbeat\":34}";

	@Test
	public void thePublicAddressDoesNotContainTheTechnicalIdentityAndIsStable() {
		String route = RedisInstanceDiscovery.getLocalRoutingId();
		assertTrue(route.matches("route-[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"));
		assertEquals(route, RedisInstanceDiscovery.getLocalRoutingId());
	}

	@Test
	public void theOpaqueAddressResolvesToThePodAndItsInternalEndpoint() {
		var info = RedisInstanceDiscovery.resolveRoutingInfo(ROUTE, RECORD, RECORD);
		assertNotNull(info);
		assertEquals("convertigo-pod-a", info.instanceId);
		assertEquals(ROUTE, info.routingId);
		assertEquals("http://10.0.0.2:28080/convertigo", info.baseUrl);
		assertEquals(12, info.startedAt);
		assertEquals(34, info.lastHeartbeat);
	}

	@Test
	public void anOldAddressDoesNotSelectAPodRestartedUnderTheSameName() {
		String restarted = RECORD.replace(ROUTE, "route-1cb08122-2c28-4220-9b46-302b6aba958b");
		assertNull(RedisInstanceDiscovery.resolveRoutingInfo(ROUTE, RECORD, restarted));
		assertNull(RedisInstanceDiscovery.resolveRoutingInfo(ROUTE, RECORD, null));
	}

	@Test
	public void malformedOrUnknownAddressesAreNotResolved() {
		assertNull(RedisInstanceDiscovery.resolveRoutingInfo("route-unknown", RECORD, RECORD));
		assertNull(RedisInstanceDiscovery.resolveRoutingInfo(ROUTE, "invalid json", RECORD));
		assertNull(RedisInstanceDiscovery.resolveRoutingInfo(ROUTE, RECORD, "invalid json"));
	}

	@Test
	public void oldDiscoveryRecordsKeepTheirInternalIdentityForRollingUpgrades() {
		var info = RedisInstanceDiscovery.parseInfo("old-pod", "{\"baseUrl\":\"http://10.0.0.3:28080/convertigo\"}");
		assertEquals("old-pod", info.instanceId);
		assertNull(info.routingId);
		assertNotNull(info.baseUrl);
		assertNull(RedisInstanceDiscovery.resolveRoutingInfo(ROUTE, RECORD,
				"{\"baseUrl\":\"http://10.0.0.3:28080/convertigo\"}"));
	}
}
