/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReverseProxyManagerTest {
	@Test
	public void managedRouteKeyRoundTripsItsOwner() {
		var key = ReverseProxyManager.createManagedRouteKey("studio-local");

		assertTrue(ReverseProxyManager.isManagedRouteKey(key));
		assertEquals("studio-local", ReverseProxyManager.getRouteInstanceId(key));
	}

	@Test
	public void malformedManagedRouteKeysAreRejected() {
		assertFalse(ReverseProxyManager.isManagedRouteKey("studio-local"));
		assertNull(ReverseProxyManager.getRouteInstanceId("studio-local.invalid"));
		assertNull(ReverseProxyManager.getRouteInstanceId(null));
	}

	@Test
	public void explicitRoutesCanBeRemoved() throws Exception {
		var manager = new ReverseProxyManager();
		manager.addReverseProxyHttp("preview", "http://127.0.0.1:5173");

		assertEquals(5173, manager.getHttpHost("preview").getPort());
		assertTrue(manager.removeReverseProxyHttp("preview"));
		assertNull(manager.getHttpHost("preview"));
		assertFalse(manager.removeReverseProxyHttp("preview"));
	}
}
