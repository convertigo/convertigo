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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.ServletUtils;

public class CacheControlFilterTest {
	private static final String IMMUTABLE = "public, max-age=31536000, immutable";
	private static final String SHORT = "public, max-age=60, s-maxage=300, must-revalidate";
	private static final String REVALIDATE = "no-cache, must-revalidate";
	private static final long MODIFIED = 1_700_000_000_000L;
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private String previousWebappPath;

	@Before public void setup() throws Exception {
		previousWebappPath = Engine.WEBAPP_PATH;
		Engine.WEBAPP_PATH = folder.getRoot().getAbsolutePath();
		Engine.isStarted = false;
		Engine.logEngine = Engine.logContext = Logger.getLogger("admin-cache-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.NET_ADMIN_STATIC_MAX_AGE, "60");
		EnginePropertiesManager.setProperty(PropertyName.NET_ADMIN_STATIC_SHARED_MAX_AGE, "300");
		EnginePropertiesManager.setProperty(PropertyName.NET_GZIP, "true");
	}

	@After public void restoreWebappPath() { Engine.WEBAPP_PATH = previousWebappPath; }

	@Test public void recognizesActualSvelteBuildHashes() throws Exception {
		for (var path : List.of("/_app/immutable/entry/start.BYXUh3T7.js", "/_app/immutable/nodes/0.Cd5IM2S2.js",
				"/_app/immutable/assets/InputGroup.CYKG1TyG.css", "/_app/immutable/assets/xyflow.DZg-IA-4.css",
				"/_app/immutable/assets/35.WVxOxH_H.css", "/_app/immutable/assets/ibm-plex-sans.BAAhND-U.woff2",
				"/_app/immutable/chunks/utils.Da7iAuY-.js.map", "/icons/icon.058775c0153355e7.svg")) {
			file(path);
			assertEquals(path, IMMUTABLE, policy(path));
		}
	}

	@Test public void revalidatesStableMonacoScriptsAndCachesOnlyHashedChunksImmutably() throws Exception {
		for (var path : List.of("/monaco/vs/loader.js", "/monaco/vs/editor.js", "/monaco/vs/editor/editor.main.js",
				"/monaco/vs/editor/editor.worker.js", "/monaco/vs/basic-languages/json/json.js",
				"/monaco/vs/nls.messages-loader.js", "/monaco/vs/editor-production.js")) {
			file(path);
			assertEquals(path, REVALIDATE, policy(path));
		}
		for (var path : List.of("/monaco/vs/editor-KLE6jdfb.js", "/monaco/vs/toggleHighContrast-qGX7E9o7.js",
				"/monaco/vs/editor-BdtEMBbM.js", "/monaco/vs/assets/editor.worker-_vAIFJDs.js",
				"/monaco/vs/editor-BdtEMBbM.js.map")) {
			file(path);
			assertEquals(path, IMMUTABLE, policy(path));
		}
		for (var path : List.of("/monaco/vs/editor/editor.main.css", "/monaco/vs/base/browser/ui/codicons/codicon/codicon.ttf")) {
			file(path);
			assertEquals(path, SHORT, policy(path));
		}
	}

	@Test public void replacedStableMonacoEntryPointReturnsNewChunkReference() throws Exception {
		var path = "/monaco/vs/editor/editor.main.js";
		var file = file(path);
		var response = new Response();
		new CacheControlFilter().doFilter(request("GET", path, Map.of("If-Modified-Since", Long.toString(MODIFIED))),
				response.proxy(), (req, res) -> ServletUtils.serveFile(file, (HttpServletRequest) req, (HttpServletResponse) res, "text/javascript"));
		assertEquals(304, response.status);
		assertEquals(REVALIDATE, response.header("Cache-Control"));
		Files.writeString(file.toPath(), "define(['../editor-BdtEMBbM'], function () {});");
		assertTrue(file.setLastModified(MODIFIED + 2000));
		response = new Response();
		new CacheControlFilter().doFilter(request("GET", path, Map.of("If-Modified-Since", Long.toString(MODIFIED))),
				response.proxy(), (req, res) -> ServletUtils.serveFile(file, (HttpServletRequest) req, (HttpServletResponse) res, "text/javascript"));
		assertEquals(200, response.status);
		assertEquals(REVALIDATE, response.header("Cache-Control"));
		assertTrue(response.text().contains("editor-BdtEMBbM"));
		assertNull(policy("/monaco/vs/editor-KLE6jdfb.js"));
	}

	@Test public void directoryNameAloneNeverMakesAssetsImmutable() throws Exception {
		for (var path : List.of("/_app/immutable/entry/start.js", "/_app/immutable/assets/site.min.css",
				"/_app/immutable/assets/logo.production.svg", "/_app/immutable/assets/site.longname.css",
				"/fonts/SpaceGrotesk.ttf", "/icons/studio.svg", "/bezels/iPhone-17-Pro-Max.webp",
				"/logo.svg", "/favicon.png", "/_app/immutable/chunks/app.uncertain123.js",
				"/icons/studio.CYKG1TyG.svg")) {
			file(path);
			assertEquals(path, SHORT, policy(path));
		}
	}

	@Test public void revalidatesHtmlAndBootstrapFiles() throws Exception {
		for (var path : List.of("/index.html", "/login/index.html", "/admin/projects/index.html",
				"/admin/fullsync/index.html", "/admin/help.htm", "/manifest.webmanifest",
				"/_app/version.json", "/env.json", "/service-worker.js", "/sw.js", "/chunk-recovery.js")) {
			file(path);
			assertEquals(path, REVALIDATE, policy(path));
		}
	}

	@Test public void htmlAndBootPolicyTakesPriorityOverHashLookingNames() throws Exception {
		file("/_app/immutable/assets/page.CYKG1TyG.html");
		file("/_app/immutable/assets/version.json");
		assertEquals(REVALIDATE, policy("/_app/immutable/assets/page.CYKG1TyG.html"));
		assertEquals(REVALIDATE, policy("/_app/immutable/assets/version.json"));
	}

	@Test public void resolvesDirectoryAndDynamicAdminIndexPages() throws Exception {
		file("/admin/config/_/index.html");
		file("/login/index.html");
		assertEquals(REVALIDATE, policy("/login/"));
		assertEquals(REVALIDATE, policy("/admin/config/my-instance/"));
		assertEquals(REVALIDATE, policy("/admin/config/my-instance/index.html"));
		assertNull(policy("/admin/config/my-instance/missing.js"));
	}

	@Test public void keepsExistingFallbackNoStoreAndRemovesConditionalHeaders() throws Exception {
		file("/login/index.html");
		var response = new Response();
		new CacheControlFilter().doFilter(request("GET", "/login/", Map.of("If-Modified-Since", Long.toString(MODIFIED))),
				response.proxy(), (req, res) -> new FallbackFilter().doFilter(req, res, (innerRequest, innerResponse) -> {
					assertEquals(-1, ((HttpServletRequest) innerRequest).getDateHeader("If-Modified-Since"));
					assertEquals("no-store, no-cache, must-revalidate", ((HttpServletResponse) innerResponse).getHeader("Cache-Control"));
				}));
		assertEquals("no-store, no-cache, must-revalidate", response.header("Cache-Control"));
	}

	@Test public void leavesProjectApisProtectedFilesAndNonGetRequestsUntouched() throws Exception {
		for (var path : List.of("/projects/demo/DisplayObjects/mobile/main.058775c0153355e7.js",
				"/system/projects/demo/index.html", "/admin/services/test.json", "/services/test.json",
				"/api/index.html", "/fullsync/index.html", "/WEB-INF/secret.json", "/META-INF/file.json")) {
			file(path);
			assertNull(path, policy(path));
		}
		file("/logo.svg");
		for (var verb : List.of("POST", "PUT", "DELETE", "OPTIONS")) {
			var response = new Response();
			new CacheControlFilter().doFilter(request(verb, "/logo.svg", Map.of()), response.proxy(), (req, res) -> {});
			assertNull(response.header("Cache-Control"));
		}
	}

	@Test public void preservesExplicitResponsePolicyAndAllowsDownstreamOverride() throws Exception {
		var path = "/_app/immutable/entry/start.BYXUh3T7.js";
		file(path);
		for (var header : List.of("private, no-cache", "no-store")) {
			var response = new Response();
			response.headers.put("Cache-Control", header);
			new CacheControlFilter().doFilter(request("GET", path, Map.of()), response.proxy(), (req, res) -> {});
			assertEquals(header, response.header("Cache-Control"));
		}
		var response = new Response();
		new CacheControlFilter().doFilter(request("GET", path, Map.of()), response.proxy(),
				(req, res) -> ((HttpServletResponse) res).setHeader("Cache-Control", "private, no-store"));
		assertEquals("private, no-store", response.header("Cache-Control"));
	}

	@Test public void appliesIndependentConfigurableTtlsAndClampsNegativeValues() throws Exception {
		file("/fonts/SpaceGrotesk.ttf");
		EnginePropertiesManager.setProperty(PropertyName.NET_APP_STATIC_MAX_AGE, "123");
		EnginePropertiesManager.setProperty(PropertyName.NET_ADMIN_STATIC_MAX_AGE, "20");
		EnginePropertiesManager.setProperty(PropertyName.NET_ADMIN_STATIC_SHARED_MAX_AGE, "120");
		assertEquals("public, max-age=20, s-maxage=120, must-revalidate", policy("/fonts/SpaceGrotesk.ttf"));
		EnginePropertiesManager.setProperty(PropertyName.NET_ADMIN_STATIC_MAX_AGE, "-1");
		EnginePropertiesManager.setProperty(PropertyName.NET_ADMIN_STATIC_SHARED_MAX_AGE, "-2");
		assertEquals("public, max-age=0, s-maxage=0, must-revalidate", policy("/fonts/SpaceGrotesk.ttf"));
	}

	@Test public void missingFilesAndTraversalNeverAcquireImmutableHeaders() throws Exception {
		assertNull(policy("/_app/immutable/entry/missing.BYXUh3T7.js"));
		file("/logo.058775c0153355e7.svg");
		for (var path : List.of("/_app/../logo.058775c0153355e7.svg", "/icons/%2e%2e/logo.058775c0153355e7.svg",
				"/icons/../logo.058775c0153355e7.svg", "/icons/./test.svg")) {
			assertNull(path, policy(path));
		}
		var outside = folder.newFolder("outside").toPath();
		Files.writeString(outside.resolve("icon.058775c0153355e7.svg"), "outside");
		var root = folder.newFolder("webapp");
		Engine.WEBAPP_PATH = root.getAbsolutePath();
		Files.createSymbolicLink(root.toPath().resolve("icons"), outside);
		assertNull(policy("/icons/icon.058775c0153355e7.svg"));
	}

	@Test public void keepsPolicyOnGetHeadAndNotModifiedWithoutAddingEtag() throws Exception {
		var path = "/_app/immutable/entry/start.BYXUh3T7.js";
		var file = file(path);
		for (var verb : List.of("GET", "HEAD")) {
			var response = new Response();
			new CacheControlFilter().doFilter(request(verb, path, Map.of()), response.proxy(),
					(req, res) -> ServletUtils.serveFile(file, (HttpServletRequest) req, (HttpServletResponse) res, "text/javascript"));
			assertEquals(IMMUTABLE, response.header("Cache-Control"));
			assertEquals(200, response.status);
			assertNull(response.header("ETag"));
			// The servlet container suppresses HEAD bodies; this file helper writes to the supplied stream.
			assertEquals("fixture-content", response.text());
		}
		var response = new Response();
		new CacheControlFilter().doFilter(request("GET", path, Map.of("If-Modified-Since", Long.toString(MODIFIED))),
				response.proxy(), (req, res) -> ServletUtils.serveFile(file, (HttpServletRequest) req, (HttpServletResponse) res, "text/javascript"));
		assertEquals(304, response.status);
		assertEquals(IMMUTABLE, response.header("Cache-Control"));
		assertEquals(Long.toString(MODIFIED), response.header("Last-Modified"));
		assertEquals("", response.text());
	}

	@Test public void neverCachesErrorsOrRedirectsAfterExistenceCheck() throws Exception {
		var path = "/_app/immutable/entry/start.BYXUh3T7.js";
		file(path);
		for (var status : List.of(404, 416, 500, 302)) {
			for (var action : List.of("setStatus", "sendError", "sendErrorMessage")) {
				var response = new Response();
				new CacheControlFilter().doFilter(request("GET", path, Map.of()), response.proxy(), (req, res) -> {
					var http = (HttpServletResponse) res;
					if (action.equals("setStatus")) http.setStatus(status);
					else if (action.equals("sendError")) http.sendError(status);
					else http.sendError(status, "failure");
				});
				assertEquals( "no-store", response.header("Cache-Control"));
			}
		}
		var response = new Response();
		new CacheControlFilter().doFilter(request("GET", path, Map.of()), response.proxy(),
				(req, res) -> ((HttpServletResponse) res).sendRedirect("/login/"));
		assertEquals("no-store", response.header("Cache-Control"));
	}

	@Test public void keepsPolicyWithGzipAndIdentityAndVariesOnEncoding() throws Exception {
		var path = "/_app/immutable/entry/start.BYXUh3T7.js";
		var file = file(path);
		for (var encoding : List.of("gzip", "identity")) {
			var response = new Response();
			new CacheControlFilter().doFilter(request("GET", path, Map.of("Accept-Encoding", encoding)), response.proxy(),
					(req, res) -> new CompressionFilter().doFilter(req, res, (innerRequest, innerResponse) ->
							ServletUtils.serveFile(file, (HttpServletRequest) innerRequest, (HttpServletResponse) innerResponse, "text/javascript")));
			assertEquals(IMMUTABLE, response.header("Cache-Control"));
			assertEquals("Accept-Encoding", response.header("Vary"));
			var bytes = response.body.toByteArray();
			if (encoding.equals("gzip")) {
				assertEquals("gzip", response.header("Content-Encoding"));
				bytes = new GZIPInputStream(new ByteArrayInputStream(bytes)).readAllBytes();
			} else assertNull(response.header("Content-Encoding"));
			assertEquals("fixture-content", new String(bytes, StandardCharsets.UTF_8));
		}
	}

	private String policy(String path) throws Exception {
		var response = new Response();
		new CacheControlFilter().doFilter(request("GET", path, Map.of()), response.proxy(), (req, res) -> {});
		return response.header("Cache-Control");
	}

	private File file(String path) throws Exception {
		var file = new File(Engine.WEBAPP_PATH, path.substring(1));
		Files.createDirectories(file.toPath().getParent());
		Files.writeString(file.toPath(), "fixture-content");
		assertTrue(file.setLastModified(MODIFIED));
		return file;
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
					case "getServletPath" -> path;
					case "getQueryString" -> null;
					case "getAttribute" -> attributes.get(args[0]);
					case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
					case "removeAttribute" -> { attributes.remove(args[0]); yield null; }
					case "getHeader" -> headers.get(args[0]);
					case "getHeaders" -> Collections.enumeration(headers.containsKey(args[0]) ? List.of(headers.get(args[0])) : List.of());
					case "getDateHeader" -> headers.containsKey(args[0]) ? Long.parseLong(headers.get(args[0])) : -1L;
					default -> throw new UnsupportedOperationException(method.getName());
				});
	}

	private static class Response {
		final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
						case "setDateHeader" -> { headers.put((String) args[0], args[1].toString()); yield null; }
						case "getHeader" -> headers.get(args[0]);
						case "getHeaders" -> headers.containsKey(args[0]) ? List.of(headers.get(args[0])) : List.of();
						case "getHeaderNames" -> headers.keySet();
						case "containsHeader" -> headers.containsKey(args[0]);
						case "setStatus", "sendError" -> { status = (Integer) args[0]; yield null; }
						case "sendRedirect" -> { status = 302; yield null; }
						case "encodeRedirectURL" -> args[0];
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
