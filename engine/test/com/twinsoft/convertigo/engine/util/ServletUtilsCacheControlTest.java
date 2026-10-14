/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.servlets.CompressionFilter;

public class ServletUtilsCacheControlTest {
	private static final String MOBILE = "/projects/demo/DisplayObjects/mobile/";
	private static final String IMMUTABLE = "public, max-age=31536000, immutable";
	private static final String REVALIDATE = "no-cache, must-revalidate";
	private static final String SHORT = "public, max-age=60, s-maxage=300, must-revalidate";
	private static final long MODIFIED = 1_700_000_000_000L;
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void initEngineProperties() throws Exception {
		Engine.isStarted = false;
		Engine.logEngine = Engine.logContext = Logger.getLogger("static-cache-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.NET_MAX_AGE, "10");
		EnginePropertiesManager.setProperty(PropertyName.NET_APP_STATIC_MAX_AGE, "60");
		EnginePropertiesManager.setProperty(PropertyName.NET_APP_STATIC_SHARED_MAX_AGE, "300");
		EnginePropertiesManager.setProperty(PropertyName.NET_GZIP, "true");
	}

	@Test
	public void recognizesGeneratedAngularBundlesAndLegacyHexHashes() {
		for (var name : List.of("main-GZ62CSQV.js", "chunk-IGCSZ4TO.js", "styles-L7DE6WN3.css",
				"main-YHEEUCBR.js", "styles-YQXXZ2NK.css",
				"polyfills-EXCRSS4S.js", "scripts-4CHYFCAL.js", "runtime-ABCD2345.mjs",
				"main-GZ62CSQV.js.map", "6521.05b42b408ccf3ab0.js", "styles.16f3035d63813619.css",
				"open-sans.058775c0153355e7.woff", "main.05b42b408ccf3ab0.js.map")) {
			assertEquals(name, IMMUTABLE, policy(name));
		}
	}

	@Test
	public void recognizesAngularNamedChunksAndAdditionalGlobalBundles() {
		for (var name : List.of("feature-page-component-IGCSZ4TO.js", "custom-scripts-4CHYFCAL.js",
				"custom-theme-L7DE6WN3.css", "feature-page-component-IGCSZ4TO.js.map")) {
			assertEquals(name, IMMUTABLE, policy(name));
		}
	}

	@Test
	public void recognizesFingerprintedAngularMediaReferencedByC8OformsCss() {
		for (var name : List.of("media/saira-latin-400-normal-OG7TRS4Z.woff2",
				"media/inter-latin-400-normal-FR5HOXA4.woff", "media/codicon-LCPAQIGT.ttf",
				"media/layers-55W3Q4RM.png", "media/marker-icon-2x-SP63CLEO.png",
				"media/marker-shadow-MEXDWUU7.png")) {
			assertEquals(name, IMMUTABLE, policy(name));
			assertEquals(name, IMMUTABLE, ServletUtils.getStaticCacheControl(
					"/projects/demo/DisplayObjects/pwas/sample/" + name,
					name.substring(name.lastIndexOf('/') + 1), 60, 300));
		}
	}

	@Test
	public void copiedAssetsAndNonHashedMediaKeepShortTtls() {
		for (var name : List.of("assets/icon/favicon.ico", "assets/css/animate.min.css",
				"assets/logo-ABCDEFGH.svg", "assets/custom-ABCDEFGH.js",
				"media/logo-production.svg", "media/icon-arrow-right.svg", "media/font.woff2")) {
			assertEquals(name, SHORT, policy(name));
		}
	}

	@Test
	public void fingerprintedAngularMediaKeepsItsPolicyWhenServed() throws Exception {
		var file = file("media/saira-latin-400-normal-OG7TRS4Z.woff2", "font-content");
		var response = serve(file, request("GET", MOBILE + "media/" + file.getName(), Map.of()));
		assertEquals(IMMUTABLE, response.header("Cache-Control"));
		assertEquals("font-content", response.text());
	}

	@Test
	public void keepsHyphenatedNamesAndUncertainHashesOnShortTtls() {
		for (var name : List.of("logo-production.svg", "icon-arrow-right.svg", "main-production.js",
				"main-ABC_def123.js", "animate.min.css", "cordova.js", "assets/config.json")) {
			assertEquals(name, SHORT, policy(name));
		}
	}

	@Test
	public void revalidatesAllHtmlAndRuntimeUpdateFiles() {
		for (var name : List.of("index.html", "route/index.html", "prerendered.html", "PAGE.HTM",
				"env.json", "version.json", "manifest.json", "manifest.webmanifest", "asset-manifest.json",
				"ngsw.json", "ngsw-worker.js", "service-worker.js", "safety-worker.js", "sw.js")) {
			assertEquals(name, REVALIDATE, policy(name));
		}
	}

	@Test
	public void recognizesSvelteImmutablePathsAndPwaApplicationRoots() {
		assertEquals(IMMUTABLE, policy("_app/immutable/entry/start.BYXUh3T7.js"));
		assertEquals(REVALIDATE, policy("_app/version.json"));
		assertEquals(IMMUTABLE, ServletUtils.getStaticCacheControl(
				"/system/projects/demo/DisplayObjects/pwas/sample/main-GZ62CSQV.js", "main-GZ62CSQV.js", 60, 300));
	}

	@Test
	public void leavesAdminApisAndDevelopmentPathsToTheirExistingPolicy() {
		for (var path : List.of("/admin/index.html", "/api/main-GZ62CSQV.js", "/projects/demo/.json",
				"/projects/demo/DisplayObjects/dev47447/main-GZ62CSQV.js")) {
			assertNull(path, ServletUtils.getStaticCacheControl(path, "main-GZ62CSQV.js", 60, 300));
		}
	}

	@Test
	public void boundsConfiguredTtlsAtZeroAndAllowsIndependentLifetimes() {
		assertEquals("public, max-age=20, s-maxage=120, must-revalidate",
				ServletUtils.getStaticCacheControl(MOBILE + "logo.svg", "logo.svg", 20, 120));
		assertEquals("public, max-age=0, s-maxage=0, must-revalidate",
				ServletUtils.getStaticCacheControl(MOBILE + "logo.svg", "logo.svg", -1, -100));
	}

	@Test
	public void servesTheImmutableBodyWithoutOverwritingItsPolicy() throws Exception {
		var file = file("main-GZ62CSQV.js", "compiled-content");
		var response = serve(file, request("GET", MOBILE + file.getName(), Map.of()));
		assertEquals(IMMUTABLE, response.header("Cache-Control"));
		assertEquals("compiled-content", response.text());
		assertEquals(MODIFIED, response.dates.get("Last-Modified").longValue());
		assertEquals("bytes", response.header("Accept-Ranges"));
		assertNull(response.header("ETag"));
	}

	@Test
	public void appliesTheConfiguredShortTtlsWhenServingAnAsset() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.NET_APP_STATIC_MAX_AGE, "20");
		EnginePropertiesManager.setProperty(PropertyName.NET_APP_STATIC_SHARED_MAX_AGE, "120");
		var file = file("logo.svg", "image-content");
		var response = serve(file, request("GET", MOBILE + file.getName(), Map.of()));
		assertEquals("public, max-age=20, s-maxage=120, must-revalidate", response.header("Cache-Control"));
	}

	@Test
	public void includesThePolicyAndLastModifiedOnServeFile304() throws Exception {
		var file = file("main-GZ62CSQV.js", "compiled-content");
		var response = serve(file, request("GET", MOBILE + file.getName(),
				Map.of("If-Modified-Since", Long.toString(MODIFIED))));
		assertEquals(304, response.status);
		assertEquals(IMMUTABLE, response.header("Cache-Control"));
		assertEquals(MODIFIED, response.dates.get("Last-Modified").longValue());
		assertEquals(0, response.body.size());
	}

	@Test
	public void includesTheRevalidationPolicyOnFilteredHtml304() throws Exception {
		var file = file("index.html", "<html>entry</html>");
		var response = new Response();
		ServletUtils.handleFileFilter(file, request("GET", MOBILE + "index.html",
				Map.of("If-Modified-Since", Long.toString(MODIFIED))), response.proxy(), config(), unexpectedChain());
		assertEquals(304, response.status);
		assertEquals(REVALIDATE, response.header("Cache-Control"));
		assertEquals(MODIFIED, response.dates.get("Last-Modified").longValue());
		assertEquals(0, response.body.size());
	}

	@Test
	public void includesThePolicyOnHeadRequests() throws Exception {
		var file = file("main-GZ62CSQV.js", "compiled-content");
		assertEquals(IMMUTABLE, serve(file, request("HEAD", MOBILE + file.getName(), Map.of())).header("Cache-Control"));
	}

	@Test
	public void preservesExplicitPoliciesAndTheLegacyGlobalLifetime() throws Exception {
		var file = file("logo.svg", "image-content");
		var response = new Response();
		response.headers.put("Cache-Control", "private, no-store");
		ServletUtils.serveFile(file, request("GET", MOBILE + file.getName(), Map.of()), response.proxy(), "image/svg+xml");
		assertEquals("private, no-store", response.header("Cache-Control"));
		assertEquals("max-age=10", serve(file, request("GET", "/admin/logo.svg", Map.of())).header("Cache-Control"));
	}

	@Test
	public void leavesMissingChunksTo404HandlingInsteadOfServingHtml() throws Exception {
		var index = file("index.html", "<html>entry</html>");
		var missing = new File(index.getParentFile(), "chunk-ABCD2345.js");
		var response = new Response();
		var called = new boolean[1];
		ServletUtils.handleFileFilter(missing, request("GET", MOBILE + missing.getName(), Map.of()),
				response.proxy(), config(), (req, res) -> { called[0] = true; ((HttpServletResponse) res).sendError(404); });
		assertTrue(called[0]);
		assertEquals(404, response.status);
		assertNull(response.header("Cache-Control"));
		assertEquals(0, response.body.size());
	}

	@Test
	public void fallbackRoutesUseTheHtmlPolicyAndKeepBaseHrefRewriting() throws Exception {
		var index = file("index.html", "<html><base href=\"./\"></html>");
		file("env.json", "{\"appTemplateVersion\":\"8.4.0\"}");
		var response = new Response();
		ServletUtils.handleFileFilter(new File(index.getParentFile(), "route/deep"),
				request("GET", MOBILE + "route/deep", Map.of()), response.proxy(), config(), unexpectedChain());
		assertEquals(REVALIDATE, response.header("Cache-Control"));
		assertTrue(response.text(), response.text().contains("href=\"../\""));
		assertTrue(response.text().contains("data-c8o-mode=\"web\""));
	}

	@Test
	public void servesRangesWithTheSamePolicyEvenWithIfModifiedSince() throws Exception {
		var file = file("main-GZ62CSQV.js", "0123456789");
		var response = new Response();
		ServletUtils.handleFileFilter(file, request("GET", MOBILE + file.getName(),
				Map.of("Range", "bytes=2-4", "If-Modified-Since", Long.toString(MODIFIED))),
				response.proxy(), config(), unexpectedChain());
		assertEquals(206, response.status);
		assertEquals(IMMUTABLE, response.header("Cache-Control"));
		assertEquals("bytes 2-4/10", response.header("Content-Range"));
		assertEquals("234", response.text());
	}

	@Test
	public void doesNotCacheUnsatisfiableRangesAsImmutableResources() throws Exception {
		for (var content : List.of("0123456789", "")) {
			var file = file("main-GZ62CSQV.js", content);
			var response = serve(file, request("GET", MOBILE + file.getName(), Map.of("Range", "bytes=100-200")));
			assertEquals(416, response.status);
			assertEquals("no-store", response.header("Cache-Control"));
			assertEquals("bytes */" + content.length(), response.header("Content-Range"));
			assertEquals(0, response.body.size());
		}
	}

	@Test
	public void gzipAndIdentityVariantsKeepTheirPolicyAndVary() throws Exception {
		var file = file("main-GZ62CSQV.js", "compiled-content");
		for (var encoding : List.of("identity", "gzip")) {
			var response = new Response();
			response.headers.put("Vary", "Origin");
			new CompressionFilter().doFilter(request("GET", MOBILE + file.getName(), Map.of("Accept-Encoding", encoding)),
					response.proxy(), (req, res) -> ServletUtils.serveFile(file, (HttpServletRequest) req,
							(HttpServletResponse) res, "text/javascript"));
			assertEquals(IMMUTABLE, response.header("Cache-Control"));
			assertEquals("Origin, Accept-Encoding", response.header("Vary"));
			var bytes = response.body.toByteArray();
			if (encoding.equals("gzip")) {
				assertEquals("gzip", response.header("Content-Encoding"));
				bytes = new GZIPInputStream(new ByteArrayInputStream(bytes)).readAllBytes();
			} else {
				assertNull(response.header("Content-Encoding"));
			}
			assertArrayEquals("compiled-content".getBytes(StandardCharsets.UTF_8), bytes);
		}
	}

	@Test
	public void rangeResponsesAreNotGzipped() throws Exception {
		var file = file("main-GZ62CSQV.js", "0123456789");
		var response = new Response();
		new CompressionFilter().doFilter(request("GET", MOBILE + file.getName(),
				Map.of("Accept-Encoding", "gzip", "Range", "bytes=2-4")), response.proxy(),
				(req, res) -> ServletUtils.serveFile(file, (HttpServletRequest) req, (HttpServletResponse) res, "text/javascript"));
		assertEquals(206, response.status);
		assertNull(response.header("Content-Encoding"));
		assertEquals("234", response.text());
		assertEquals(IMMUTABLE, response.header("Cache-Control"));
	}

	@Test
	public void doesNotDuplicateVaryOrChangeTheDevelopmentGateway() throws Exception {
		for (var vary : List.of("Origin, accept-encoding", "*")) {
			var response = new Response();
			response.headers.put("Vary", vary);
			new CompressionFilter().doFilter(request("GET", MOBILE + "main.js", Map.of()), response.proxy(), (req, res) -> {});
			assertEquals(vary, response.header("Vary"));
		}
		var response = new Response();
		new CompressionFilter().doFilter(request("GET", "/projects/demo/DisplayObjects/dev47447/main.js",
				Map.of("Accept-Encoding", "gzip")), response.proxy(), (req, res) -> {});
		assertNull(response.header("Vary"));
		assertNull(response.header("Content-Encoding"));
	}

	private static String policy(String name) {
		return ServletUtils.getStaticCacheControl(MOBILE + name, name.substring(name.lastIndexOf('/') + 1), 60, 300);
	}

	private File file(String name, String content) throws Exception {
		var file = new File(folder.getRoot(), "projects/demo/DisplayObjects/mobile/" + name);
		Files.createDirectories(file.toPath().getParent());
		Files.writeString(file.toPath(), content);
		assertTrue(file.setLastModified(MODIFIED));
		return file;
	}

	private static Response serve(File file, HttpServletRequest request) throws Exception {
		var response = new Response();
		ServletUtils.serveFile(file, request, response.proxy(), "text/javascript");
		return response;
	}

	private static FilterChain unexpectedChain() {
		return (req, res) -> { throw new AssertionError("Existing file unexpectedly delegated"); };
	}

	private static FilterConfig config() {
		var context = (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
				new Class<?>[] { ServletContext.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getMimeType" -> args[0].toString().endsWith(".html") ? "text/html" : "text/javascript";
					default -> throw new UnsupportedOperationException(method.getName());
				});
		return (FilterConfig) Proxy.newProxyInstance(FilterConfig.class.getClassLoader(), new Class<?>[] { FilterConfig.class },
				(proxy, method, args) -> method.getName().equals("getServletContext") ? context : null);
	}

	private static HttpServletRequest request(String verb, String path, Map<String, String> requestHeaders) {
		var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		headers.putAll(requestHeaders);
		var attributes = new HashMap<String, Object>();
		return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getMethod" -> verb;
					case "getRequestURI" -> "/convertigo" + path;
					case "getContextPath" -> "/convertigo";
					case "getHeader" -> headers.get(args[0]);
					case "getHeaders" -> Collections.enumeration(headers.containsKey(args[0]) ? List.of(headers.get(args[0])) : List.of());
					case "getDateHeader" -> headers.containsKey(args[0]) ? Long.parseLong(headers.get(args[0])) : -1L;
					case "getAttribute" -> attributes.get(args[0]);
					case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
					case "removeAttribute" -> { attributes.remove(args[0]); yield null; }
					default -> throw new UnsupportedOperationException(method.getName());
				});
	}

	private static class Response {
		final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		final Map<String, Long> dates = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		final ByteArrayOutputStream body = new ByteArrayOutputStream();
		int status = 200;
		String header(String name) { return headers.get(name); }
		String text() { return body.toString(StandardCharsets.UTF_8); }
		HttpServletResponse proxy() {
			var output = new ServletOutputStream() {
				@Override public void write(int value) { body.write(value); }
				@Override public boolean isReady() { return true; }
				@Override public void setWriteListener(WriteListener listener) { throw new UnsupportedOperationException(); }
			};
			return (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
					new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> switch (method.getName()) {
						case "setHeader" -> { headers.put((String) args[0], (String) args[1]); yield null; }
						case "addHeader" -> { headers.merge((String) args[0], (String) args[1], (a, b) -> a + ", " + b); yield null; }
						case "setDateHeader" -> { dates.put((String) args[0], (Long) args[1]); headers.put((String) args[0], args[1].toString()); yield null; }
						case "getHeader" -> headers.get(args[0]);
						case "getHeaders" -> headers.containsKey(args[0]) ? List.of(headers.get(args[0])) : List.of();
						case "getHeaderNames" -> new ArrayList<>(headers.keySet());
						case "containsHeader" -> headers.containsKey(args[0]);
						case "setStatus", "sendError" -> { status = (Integer) args[0]; yield null; }
						case "getStatus" -> status;
						case "getOutputStream" -> output;
						case "getCharacterEncoding" -> "UTF-8";
						case "isCommitted" -> false;
						case "flushBuffer" -> null;
						default -> throw new UnsupportedOperationException(method.getName());
					});
		}
	}
}
