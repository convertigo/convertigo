/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ServletUtilsCacheControlTest {
	private static final String MOBILE = "/projects/sample/DisplayObjects/mobile/";
	private static final String IMMUTABLE = "public, max-age=31536000, immutable";
	private static final String REVALIDATE = "no-cache, must-revalidate";
	private static final String STATIC = "public, max-age=2592000, must-revalidate";

	@Test
	public void cachesAngularFingerprintedBundlesAndFontsForever() {
		assertEquals(IMMUTABLE, policy("6521.05b42b408ccf3ab0.js"));
		assertEquals(IMMUTABLE, policy("styles.16f3035d63813619.css"));
		assertEquals(IMMUTABLE, policy("open-sans.058775c0153355e7.woff"));
		assertEquals(IMMUTABLE, policy("main-ABC_def123.js"));
		assertEquals(IMMUTABLE, policy("main.05b42b408ccf3ab0.js.map"));
	}

	@Test
	public void cachesSvelteKitImmutableTreeForever() {
		var path = MOBILE + "_app/immutable/entry/start.BYXUh3T7.js";
		assertEquals(IMMUTABLE, ServletUtils.getStaticCacheControl(path, "start.BYXUh3T7.js", 2592000));
	}

	@Test
	public void revalidatesEntryRuntimeConfigurationAndUpdateFiles() {
		assertEquals(REVALIDATE, policy("index.html"));
		assertEquals(REVALIDATE, policy("env.json"));
		assertEquals(REVALIDATE, ServletUtils.getStaticCacheControl(MOBILE + "_app/version.json", "version.json", 2592000));
		assertEquals(REVALIDATE, policy("manifest.webmanifest"));
		assertEquals(REVALIDATE, policy("ngsw.json"));
		assertEquals(REVALIDATE, policy("service-worker.js"));
	}

	@Test
	public void givesUnversionedApplicationAssetsABoundedConfigurableLifetime() {
		assertEquals(STATIC, policy("animate.min.css"));
		assertEquals(STATIC, policy("logo.svg"));
		assertEquals("public, max-age=60, must-revalidate", ServletUtils.getStaticCacheControl(MOBILE + "cordova.js", "cordova.js", 60));
	}

	@Test
	public void leavesNonApplicationsToTheirExistingPolicy() {
		assertNull(ServletUtils.getStaticCacheControl("/admin/index.html", "index.html", 2592000));
	}

	private static String policy(String fileName) {
		return ServletUtils.getStaticCacheControl(MOBILE + fileName, fileName, 2592000);
	}
}
