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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** A static SvelteKit build prerenders a route with "_" in place of its parameters: /product/_/index.html. */
public class ServletUtilsPlaceholderTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File build(String... pages) throws IOException {
		var root = folder.newFolder("mobile");
		for (var page : pages) {
			var file = new File(root, page);
			file.getParentFile().mkdirs();
			Files.writeString(file.toPath(), page);
		}
		return root;
	}

	@Test
	public void aParameterValueIsServedByThePagePrerenderedWithThePlaceholder() throws IOException {
		var root = build("index.html", "product/_/index.html", "list/index.html", "list/_/index.html", "shop/_/_/index.html");
		assertEquals("/product/_/index.html", ServletUtils.resolvePlaceholderPath(root, "/product/42/index.html"));
		assertEquals("An optional parameter absent", "/list/index.html", ServletUtils.resolvePlaceholderPath(root, "/list/index.html"));
		assertEquals("An optional parameter present", "/list/_/index.html", ServletUtils.resolvePlaceholderPath(root, "/list/3/index.html"));
		assertEquals("/shop/_/_/index.html", ServletUtils.resolvePlaceholderPath(root, "/shop/books/price/index.html"));
	}

	@Test
	public void anExistingSegmentWinsOverThePlaceholder() throws IOException {
		var root = build("product/_/index.html", "product/new/index.html");
		assertEquals("/product/new/index.html", ServletUtils.resolvePlaceholderPath(root, "/product/new/index.html"));
		assertEquals("/product/_/index.html", ServletUtils.resolvePlaceholderPath(root, "/product/old/index.html"));
	}

	@Test
	public void noPageWithoutAMatchingPlaceholder() throws IOException {
		var root = build("index.html", "product/_/index.html");
		assertNull(ServletUtils.resolvePlaceholderPath(root, "/cart/42/index.html"));
		assertNull("One segment more than the route", ServletUtils.resolvePlaceholderPath(root, "/product/42/reviews/index.html"));
		assertNull("Only pages", ServletUtils.resolvePlaceholderPath(root, "/product/42/logo.png"));
		assertNull(ServletUtils.resolvePlaceholderPath(root, "/product/../../secret/index.html"));
	}
}
