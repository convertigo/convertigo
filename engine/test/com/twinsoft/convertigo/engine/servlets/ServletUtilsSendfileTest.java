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
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.ServletUtils;

public class ServletUtilsSendfileTest {
	private static final String MOBILE = "/projects/demo/DisplayObjects/mobile/";
	private static final String PREFIX = "org.apache.tomcat.sendfile.";
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Path asset;

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logContext = Logger.getLogger("sendfile-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.NET_GZIP, "true");
		asset = folder.getRoot().toPath().resolve("DisplayObjects/mobile/main-ABCDEFGH.js");
		Files.createDirectories(asset.getParent());
		Files.write(asset, new byte[128 * 1024]);
	}

	private HttpServletRequest supported(String verb, String name, Map<String, String> headers) {
		var request = request(verb, MOBILE + name, headers);
		request.setAttribute(PREFIX + "support", Boolean.TRUE);
		return request;
	}

	private void serve(Path file, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ServletUtils.handleFileFilter(file.toFile(), request, response, config(),
				(req, res) -> { throw new AssertionError("An existing asset must be served"); });
	}

	@Test public void delegatesLargeImmutableAssetsWithoutOpeningTheResponseBody() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of());
		var response = new Response();
		serve(asset, request, response.proxy());
		assertEquals(asset.toRealPath().toString(), request.getAttribute(PREFIX + "filename"));
		assertEquals(0L, request.getAttribute(PREFIX + "start"));
		assertEquals(Files.size(asset), request.getAttribute(PREFIX + "end"));
		assertEquals(Long.toString(Files.size(asset)), response.header("Content-Length"));
		assertEquals(0, response.outputRequests);
		assertEquals(0, response.body.size());
	}

	@Test public void flushesSessionAndCookieBeforePublishingTheTransfer() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of());
		var response = new Response();
		var saved = new AtomicInteger();
		var wrapper = new SessionCookieResponseWrapper(response.proxy(), "JSESSIONID", () -> {
			assertNull(request.getAttribute(PREFIX + "filename"));
			saved.incrementAndGet();
		});
		wrapper.addCookie(new Cookie("JSESSIONID", "synthetic-session"));
		serve(asset, request, wrapper);
		assertNotNull(request.getAttribute(PREFIX + "filename"));
		assertEquals(1, saved.get());
		assertEquals(1, response.cookies);
		assertEquals(0, response.outputRequests);
	}

	@Test public void sessionFailureCannotPublishAPartialTransfer() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of());
		var response = new Response();
		var wrapper = new SessionCookieResponseWrapper(response.proxy(), "JSESSIONID", () -> {
			throw new IllegalStateException("Session store unavailable");
		});
		assertThrows(IllegalStateException.class, () -> serve(asset, request, wrapper));
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertNull(request.getAttribute(PREFIX + "start"));
		assertNull(request.getAttribute(PREFIX + "end"));
		assertEquals(0, response.body.size());
	}

	@Test public void unsupportedConnectorsSmallMutableAndHeadResponsesKeepTheCopyPath() throws Exception {
		for (String name : List.of("small-ABCDEFGH.js", "env.json", asset.getFileName().toString())) {
			var file = asset.getParent().resolve(name);
			if (!file.equals(asset)) Files.write(file, new byte[name.startsWith("small") ? 1024 : 128 * 1024]);
			var request = supported(name.equals(asset.getFileName().toString()) ? "HEAD" : "GET", name, Map.of());
			var response = new Response(); serve(file, request, response.proxy());
			assertNull(request.getAttribute(PREFIX + "filename"));
		}
		var request = supported("GET", asset.getFileName().toString(), Map.of());
		request.removeAttribute(PREFIX + "support");
		var response = new Response(); serve(asset, request, response.proxy());
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertEquals(Files.size(asset), response.body.size());
	}

	@Test public void rangesAndConditionalResponsesKeepTheirExistingBodiesAndHeaders() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of("Range", "bytes=3-9"));
		var response = new Response(); serve(asset, request, response.proxy());
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertEquals(206, response.status); assertEquals(7, response.body.size());
		request = supported("GET", asset.getFileName().toString(),
				Map.of("If-Modified-Since", Long.toString(Files.getLastModifiedTime(asset).toMillis())));
		response = new Response(); serve(asset, request, response.proxy());
		assertEquals(304, response.status); assertEquals(0, response.body.size());
		assertNull(request.getAttribute(PREFIX + "filename"));
	}

	@Test public void unknownResponseWrappersAndExistingEncodingsCannotBeBypassed() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of());
		var response = new Response();
		serve(asset, request, new HttpServletResponseWrapper(response.proxy()));
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertEquals(Files.size(asset), response.body.size());
		request = supported("GET", asset.getFileName().toString(), Map.of());
		response = new Response(); response.headers.put("Content-Encoding", "identity");
		serve(asset, request, response.proxy());
		assertNull(request.getAttribute(PREFIX + "filename"));
	}

	@Test public void aMutableFileCannotBecomeEligibleThroughAnExistingCacheHeader() throws Exception {
		var file = asset.resolveSibling("env.json"); Files.write(file, new byte[128 * 1024]);
		var request = supported("GET", "env.json", Map.of());
		var response = new Response(); response.headers.put("Cache-Control", "public, max-age=31536000, immutable");
		serve(file, request, response.proxy());
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertEquals(Files.size(file), response.body.size());
	}

	@Test public void nestedUnknownWrapperDoesNotRunTheSessionHookTwice() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of());
		var response = new Response(); var saved = new AtomicInteger();
		var wrapper = new SessionCookieResponseWrapper(new HttpServletResponseWrapper(response.proxy()),
				"JSESSIONID", saved::incrementAndGet);
		serve(asset, request, wrapper);
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertEquals(1, saved.get());
		assertEquals(Files.size(asset), response.body.size());
	}

	@Test public void compressionStillProducesAnEncodedBodyInsteadOfSendfile() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of("Accept-Encoding", "gzip"));
		var response = new Response();
		new CompressionFilter().doFilter(request, response.proxy(), (req, res) ->
				ServletUtils.handleFileFilter(asset.toFile(), (HttpServletRequest) req, (HttpServletResponse) res,
						config(), (r, s) -> { throw new AssertionError(); }));
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertEquals("gzip", response.header("Content-Encoding"));
		try (var input = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(response.body.toByteArray()))) {
			assertArrayEquals(Files.readAllBytes(asset), input.readAllBytes());
		}
	}

	@Test public void genericFileDeliveryKeepsTemporaryFilesOnTheCopyPath() throws Exception {
		var request = supported("GET", asset.getFileName().toString(), Map.of());
		var response = new Response();
		ServletUtils.serveFile(asset.toFile(), request, response.proxy(), "application/javascript");
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertEquals(Files.size(asset), response.body.size());
		var second = new Response();
		ServletUtils.servePath(asset, request, second.proxy(), "application/javascript");
		assertNull(request.getAttribute(PREFIX + "filename"));
		assertArrayEquals(response.body.toByteArray(), second.body.toByteArray());
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
		int outputRequests;
		int cookies;
		boolean committed;
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
						case "getOutputStream" -> { outputRequests++; yield output; }
						case "addCookie" -> { cookies++; yield null; }
						case "getCharacterEncoding" -> "UTF-8";
						case "isCommitted" -> committed;
						case "flushBuffer" -> null;
						default -> throw new UnsupportedOperationException(method.getName());
					});
		}
	}
}
