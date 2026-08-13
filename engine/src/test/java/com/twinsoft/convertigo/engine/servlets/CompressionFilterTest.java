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

public class CompressionFilterTest {
	private final CompressionFilter filter = new CompressionFilter();

	@Test
	public void includesModernApplicationTextAssets() {
		assertTrue(filter.isCompressionCandidate("/projects/sample/DisplayObjects/mobile/chunk.mjs"));
		assertTrue(filter.isCompressionCandidate("/projects/sample/DisplayObjects/mobile/icon.svg"));
		assertTrue(filter.isCompressionCandidate("/projects/sample/DisplayObjects/mobile/manifest.webmanifest"));
		assertTrue(filter.isCompressionCandidate("/projects/sample/DisplayObjects/mobile/route-without-extension"));
	}

	@Test
	public void excludesAlreadyCompressedAndDownloadResponses() {
		assertFalse(filter.isCompressionCandidate("/projects/sample/DisplayObjects/mobile/font.woff2"));
		assertFalse(filter.isCompressionCandidate("/projects/sample/DisplayObjects/mobile/icon.png"));
		assertFalse(filter.isCompressionCandidate("/admin/services/projects.Export"));
	}
}
