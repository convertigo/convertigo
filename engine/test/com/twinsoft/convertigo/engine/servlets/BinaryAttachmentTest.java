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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.AttachmentManager.AttachmentDetails;
import com.twinsoft.convertigo.engine.AttachmentManagerTest;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.PluginsManager;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.requesters.BinaryServletRequester;

public class BinaryAttachmentTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private final Map<String, Object> attributes = new HashMap<>();
	private boolean finalized;

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logContext = Logger.getLogger("binary-attachment-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.pluginsManager = new PluginsManager();
	}

	@Test public void streamsOneGiBThroughTheBinaryRequesterAndServletWithA128MiBHeap() throws Exception {
		var path = folder.newFile().toPath();
		long length = 1024L * 1024 * 1024;
		try (var file = new RandomAccessFile(path.toFile(), "rw")) {
			file.setLength(length);
			file.write(new byte[] { 1, 2, 3, 4 });
			file.seek(length - 4);
			file.write(new byte[] { 5, 6, 7, 8 });
		}
		var xml = AttachmentManagerTest.attachment();
		xml.setAttribute("local-url", path.toString());
		xml.setAttribute("content-length", "123");
		var attachment = new BinaryServletRequester().postGetDocument(xml.getOwnerDocument());
		var sink = new Response();
		serve(attachment, sink, Map.of());
		assertEquals(length, sink.count);
		assertEquals(Long.toString(length), sink.headers.get("Content-Length"));
		assertEquals("attachment; filename=\"file.bin\"", sink.headers.get("Content-Disposition"));
		assertEquals("application/octet-stream", sink.headers.get("Content-Type"));
		var expected = MessageDigest.getInstance("SHA-256");
		try (var input = java.nio.file.Files.newInputStream(path)) {
			var buffer = new byte[64 * 1024];
			for (int n; (n = input.read(buffer)) != -1;) expected.update(buffer, 0, n);
		}
		assertArrayEquals(expected.digest(), sink.digest.digest());
		assertTrue(finalized);
		assertTrue(sink.flushed);
		assertFalse(sink.closed); // The servlet container owns the response stream.
	}

	@Test public void base64ResponseAndCustomHeadersStatusAndContentTypeArePreserved() throws Exception {
		var bytes = new byte[] { 0, 1, -1, 13, 10 };
		var xml = AttachmentManagerTest.attachment();
		xml.setAttribute("encoding", "base64");
		xml.setTextContent(Base64.encodeBase64String(bytes));
		RequestAttribute.responseStatus.set(request(Map.of()), Map.of(201, ""));
		RequestAttribute.responseHeader.set(request(Map.of()), Map.of("X-Example", "kept"));
		var sink = new Response();
		serve(new BinaryServletRequester().postGetDocument(xml.getOwnerDocument()), sink,
				Map.of("__content_type", new String[] { "application/pdf" }));
		assertEquals(bytes.length, sink.count);
		assertEquals(201, sink.status);
		// A caller-provided content type outside the whitelist is ignored: the declared type is served.
		assertEquals("application/octet-stream", sink.headers.get("Content-Type"));
		assertEquals("kept", sink.headers.get("X-Example"));
		assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes), sink.digest.digest());
	}

	@Test public void whitelistedCallerContentTypeOverridesTheDeclaredOne() throws Exception {
		var bytes = new byte[] { 0, 1, -1, 13, 10 };
		var xml = AttachmentManagerTest.attachment();
		xml.setAttribute("encoding", "base64");
		xml.setTextContent(Base64.encodeBase64String(bytes));
		var sink = new Response();
		serve(new BinaryServletRequester().postGetDocument(xml.getOwnerDocument()), sink,
				Map.of("__content_type", new String[] { "text/plain" }));
		assertEquals("text/plain", sink.headers.get("Content-Type"));
		assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes), sink.digest.digest());
	}

	@Test public void streamingNeverCallsGetDataAndClosesInputEvenWhenClientDisconnects() throws Exception {
		for (boolean fail : new boolean[] { false, true }) {
			boolean[] closed = { false };
			AttachmentDetails attachment = new AttachmentDetails() {
				public byte[] getData() { throw new AssertionError("Streaming must not materialize the payload"); }
				public long getLength() { return (long) Integer.MAX_VALUE + 9; }
				public String getName() { return "stream.bin"; }
				public String getContentType() { return "application/octet-stream"; }
				public InputStream getInputStream() {
					return new InputStream() {
						private final InputStream delegate = new ByteArrayInputStream(new byte[20000]);
						@Override public int read() throws IOException { return delegate.read(); }
						@Override public int read(byte[] bytes, int off, int len) throws IOException {
							return delegate.read(bytes, off, Math.min(len, 3));
						}
						@Override public void close() { closed[0] = true; }
					};
				}
			};
			var sink = new Response();
			sink.fail = fail;
			serve(attachment, sink, Map.of());
			assertTrue(closed[0]);
			assertTrue(finalized);
			assertEquals("2147483656", sink.headers.get("Content-Length"));
			if (!fail) assertEquals(20000, sink.count);
		}
	}

	private void serve(Object attachment, Response response, Map<String, String[]> parameters) throws Exception {
		var servlet = new BinaryServlet() {
			@Override public Object processRequest(HttpServletRequest request) {
				request.setAttribute("convertigo.isErrorDocument", false);
				return attachment;
			}
			@Override void onFinally(HttpServletRequest request) { finalized = true; }
			@Override public void processException(HttpServletRequest request, HttpServletResponse response, Exception e) {
				throw new AssertionError(e);
			}
		};
		servlet.doRequest(request(parameters), response.proxy());
	}

	private HttpServletRequest request(Map<String, String[]> parameters) {
		return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
			case "getMethod" -> "GET";
			case "getScheme" -> "http";
			case "getServerName" -> "localhost";
			case "getServerPort" -> 18082;
			case "getRequestURI" -> "/convertigo/projects/AttachmentTest/.bin";
			case "getCharacterEncoding" -> "UTF-8";
			case "getParameterMap" -> parameters;
			case "getParameter" -> parameters.containsKey(args[0]) ? parameters.get(args[0])[0] : null;
			case "getAttribute" -> attributes.get(args[0]);
			case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
			case "removeAttribute" -> { attributes.remove(args[0]); yield null; }
			default -> null;
		});
	}

	private static class Response extends ServletOutputStream {
		final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		final MessageDigest digest;
		long count;
		int status = 200;
		boolean fail, flushed, closed;
		Response() throws Exception { digest = MessageDigest.getInstance("SHA-256"); }
		@Override public void write(int b) throws IOException { write(new byte[] { (byte) b }, 0, 1); }
		@Override public void write(byte[] b, int off, int len) throws IOException {
			if (fail) throw new IOException("Test client disconnected");
			digest.update(b, off, len);
			count += len;
		}
		@Override public void flush() { flushed = true; }
		@Override public void close() { closed = true; }
		@Override public boolean isReady() { return true; }
		@Override public void setWriteListener(WriteListener listener) { }
		HttpServletResponse proxy() {
			return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> switch (method.getName()) {
				case "setHeader", "addHeader" -> { headers.put((String) args[0], (String) args[1]); yield null; }
				case "setContentType" -> { headers.put("Content-Type", (String) args[0]); yield null; }
				case "getContentType" -> headers.get("Content-Type");
				case "setStatus" -> { status = (Integer) args[0]; yield null; }
				case "getHeaderNames" -> headers.keySet();
				case "getOutputStream" -> this;
				default -> null;
			});
		}
	}
}
