/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;

public class FallbackFilterTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private String previousWebappPath;

	@Before public void setup() {
		previousWebappPath = Engine.WEBAPP_PATH;
		Engine.WEBAPP_PATH = folder.getRoot().getAbsolutePath();
		Engine.logEngine = Engine.logContext = Logger.getLogger("fallback-filter-test");
	}

	@After public void restoreWebappPath() { Engine.WEBAPP_PATH = previousWebappPath; }

	@Test public void redirectsTheLegacyTestPlatformToTheDashboard() throws Exception {
		assertEquals("/convertigo/dashboard/", redirect("GET", "/testplatform.html", null));
		assertEquals("/convertigo/dashboard/", redirect("HEAD", "/testplatform.html", null));
		assertEquals("/convertigo/dashboard/?a=b", redirect("GET", "/testplatform.html", "a=b"));
	}

	@Test public void redirectsTheLegacyAdministrationConsoleToTheAdmin() throws Exception {
		for (var path : List.of("/admin_", "/admin_/", "/admin_/index.html", "/admin_/main.html",
				"/admin_/_utils/", "/admin_/js/jquery/jquery.min.js")) {
			assertEquals(path, "/convertigo/admin/", redirect("GET", path, null));
		}
		assertNull(redirect("GET", "/admin_x/index.html", null));
		assertNull(redirect("POST", "/admin_/main.html", null));
	}

	@Test public void keepsTheExistingLegacyRedirects() throws Exception {
		assertEquals("/convertigo/login/", redirect("GET", "/admin/main.html", null));
		assertEquals("/convertigo/dashboard/", redirect("GET", "/projects/", null));
	}

	@Test public void leavesOtherPathsAndMethodsUntouched() throws Exception {
		for (var path : List.of("/project.html", "/testplatform.html.bak", "/scripts/weblib.js", "/admin/",
				"/admin/services/engine.GetStatus")) {
			assertNull(path, redirect("GET", path, null));
		}
		assertNull(redirect("POST", "/testplatform.html", null));
	}

	/** @return the redirect location, or null when the request goes on to the chain */
	private static String redirect(String verb, String path, String query) throws Exception {
		var location = new String[1];
		var chained = new boolean[1];
		var request = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getMethod" -> verb;
					case "getRequestURI" -> "/convertigo" + path;
					case "getContextPath" -> "/convertigo";
					case "getServletPath" -> path;
					case "getQueryString" -> query;
					default -> throw new UnsupportedOperationException(method.getName());
				});
		var response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
				new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> switch (method.getName()) {
					case "encodeRedirectURL" -> args[0];
					case "sendRedirect" -> { location[0] = (String) args[0]; yield null; }
					default -> throw new UnsupportedOperationException(method.getName());
				});
		new FallbackFilter().doFilter(request, response, (req, res) -> chained[0] = true);
		assertEquals(path, location[0] == null, chained[0]);
		return location[0];
	}
}
