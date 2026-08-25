/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CacheControlFilterTest {
	@Test
	public void recognizesAdminImmutableResourceTrees() {
		assertTrue(CacheControlFilter.isImmutableWebappPath("/_app/immutable/entry/app.hash.js"));
		assertTrue(CacheControlFilter.isImmutableWebappPath("/fonts/inter.woff2"));
		assertTrue(CacheControlFilter.isImmutableWebappPath("/icons/studio.svg"));
		assertTrue(CacheControlFilter.isImmutableWebappPath("/bezels/phone.png"));
		assertFalse(CacheControlFilter.isImmutableWebappPath("/manifest.webmanifest"));
		assertFalse(CacheControlFilter.isImmutableWebappPath("/admin/index.html"));
	}
}
