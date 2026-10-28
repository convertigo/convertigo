/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static com.twinsoft.convertigo.engine.util.FileUtilsDeleteDirectoryTest.processStarts;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AttachmentManager;
import com.twinsoft.convertigo.engine.AttachmentManagerTest;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.PluginsManager;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.translators.Translator;

/**
 * A multipart request only creates its upload folder for the items written to disk, starts no process, and removes the
 * folder once its response is written, the response being able to stream an uploaded file (#1253).
 */
public class GenericServletMultipartTest {
	private static final File TMP = new File(System.getProperty("java.io.tmpdir"));

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logContext = Logger.getLogger("generic-servlet-multipart-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.pluginsManager = new PluginsManager();
	}

	@Test public void fieldsAndAnEmptyFileInputCreateNoFolderNorProcess() throws Throwable {
		var multipart = new Multipart().field("name", "value").file("file", "", new byte[0]);
		var before = uploadFolders();
		var response = new Response();
		var starts = processStarts(() -> post(multipart, response, request -> {
			assertEquals("value", request.getParameter("name"));
			assertEquals("", request.getParameter("file"));
			assertEquals(before, uploadFolders());
			return "done";
		}));
		assertEquals("done", response.text());
		assertEquals(List.of(), starts);
		assertEquals(before, uploadFolders());
	}

	@Test public void filesAndALargeFieldAreGivenInNumberedFoldersAndRemovedWithoutProcess() throws Throwable {
		var large = "é".repeat(3000);
		var small = "small".getBytes(StandardCharsets.UTF_8);
		var big = new byte[2 * 1024 * 1024];
		Arrays.fill(big, (byte) 7);
		var multipart = new Multipart().field("large", large).file("files", "small.txt", small)
				.file("files", "C:\\path\\big.bin", big);
		var before = uploadFolders();
		var folder = new File[1];
		var response = new Response();
		var starts = processStarts(() -> post(multipart, response, request -> {
			assertEquals(large, request.getParameter("large"));
			var files = request.getParameterValues("files");
			assertEquals(2, files.length);
			folder[0] = checkUploadedFile(files[0], 1, "small.txt", small);
			assertEquals(folder[0], checkUploadedFile(files[1], 2, "big.bin", big));
			return "done";
		}));
		assertEquals("done", response.text());
		assertEquals(List.of(), starts);
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	@Test public void anUploadedFileAnsweredAsABinaryAttachmentIsStreamedBeforeItsRemoval() throws Throwable {
		var content = new byte[3 * 1024 * 1024];
		new java.util.Random(1253).nextBytes(content);
		var multipart = new Multipart().file("file", "image.png", content);
		var before = uploadFolders();
		var folder = new File[1];
		var response = new Response();
		var starts = processStarts(() -> post(multipart, response, request -> {
			var path = request.getParameter("file");
			folder[0] = checkUploadedFile(path, 1, "image.png", content);
			// the sequence answers the uploaded file: the attachment reads it once the response is written
			return attachment(path);
		}));
		assertEquals(List.of(), starts);
		assertArrayEquals(content, response.body.toByteArray());
		assertEquals(Integer.toString(content.length), response.headers.get("Content-Length"));
		assertEquals("attachment; filename=\"file.bin\"", response.headers.get("Content-Disposition"));
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	@Test public void theFolderIsRemovedWhenTheClientDisconnectsDuringTheBinaryResponse() throws Throwable {
		var content = "content".getBytes(StandardCharsets.UTF_8);
		var multipart = new Multipart().file("file", "file.txt", content);
		var before = uploadFolders();
		var folder = new File[1];
		var response = new Response();
		response.fail = true;
		post(multipart, response, request -> {
			var path = request.getParameter("file");
			folder[0] = checkUploadedFile(path, 1, "file.txt", content);
			return attachment(path);
		});
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	/** The attachment of a sequence answering a file of the server, as a binary response streams it. */
	static Object attachment(String path) {
		try {
			var xml = AttachmentManagerTest.attachment();
			xml.setAttribute("local-url", path);
			return AttachmentManager.getAttachment(xml);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	/** Checks an uploaded file given to the requestable, and returns the upload folder of the request. */
	public static File checkUploadedFile(String path, int number, String name, byte[] content) {
		var file = new File(path);
		assertEquals(name, file.getName());
		assertEquals(Integer.toString(number), file.getParentFile().getName());
		try {
			assertArrayEquals(content, Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		var folder = file.getParentFile().getParentFile();
		assertTrue(folder.getName().startsWith("c8o-multipart-files"));
		return folder;
	}

	/** The upload folders in the temporary directory. */
	public static Set<String> uploadFolders() {
		return Arrays.stream(TMP.list()).filter(name -> name.startsWith("c8o-multipart-files")).collect(Collectors.toSet());
	}

	/** Serves the request as a binary request of a project, the requestable answering what the function returns. */
	private void post(Multipart multipart, Response response, Function<HttpServletRequest, Object> requestable) throws Exception {
		var requester = new UploadRequester(requestable);
		var servlet = new BinaryServlet() {
			private static final long serialVersionUID = 1L;
			@Override public Requester getRequester() { return requester; }
			@Override void processRequestEnd(HttpServletRequest request, Requester requester) {
				request.setAttribute("convertigo.isErrorDocument", false);
				request.setAttribute("convertigo.charset", "UTF-8");
			}
			@Override void onFinally(HttpServletRequest request) { }
			@Override public void processException(HttpServletRequest request, HttpServletResponse response, Exception e) {
				throw new AssertionError(e);
			}
		};
		servlet.doRequest(multipart.request("/convertigo/projects/Upload/.bin"), response.proxy());
	}

	/** A multipart/form-data POST request, as a browser sends it. */
	public static class Multipart {
		private static final String BOUNDARY = "c8o-multipart-test";
		private final ByteArrayOutputStream body = new ByteArrayOutputStream();

		public Multipart field(String name, String value) throws IOException {
			return part("form-data; name=\"" + name + "\"", null, value.getBytes(StandardCharsets.UTF_8));
		}

		public Multipart file(String name, String filename, byte[] content) throws IOException {
			return part("form-data; name=\"" + name + "\"; filename=\"" + filename + "\"", "application/octet-stream", content);
		}

		private Multipart part(String disposition, String contentType, byte[] content) throws IOException {
			var headers = "--" + BOUNDARY + "\r\nContent-Disposition: " + disposition + "\r\n"
					+ (contentType == null ? "" : "Content-Type: " + contentType + "\r\n") + "\r\n";
			body.write(headers.getBytes(StandardCharsets.UTF_8));
			body.write(content);
			body.write("\r\n".getBytes(StandardCharsets.UTF_8));
			return this;
		}

		/** The request, with the path info of a REST request or the URI of a project request. */
		public HttpServletRequest request(String path) throws IOException {
			body.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
			var bytes = body.toByteArray();
			var input = new ServletInputStream() {
				private final ByteArrayInputStream delegate = new ByteArrayInputStream(bytes);
				@Override public int read() { return delegate.read(); }
				@Override public int read(byte[] b, int off, int len) { return delegate.read(b, off, len); }
				@Override public boolean isFinished() { return delegate.available() == 0; }
				@Override public boolean isReady() { return true; }
				@Override public void setReadListener(ReadListener listener) { }
			};
			var attributes = new HashMap<String, Object>();
			return (HttpServletRequest) Proxy.newProxyInstance(Multipart.class.getClassLoader(),
					new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getMethod" -> "POST";
				case "getPathInfo" -> path;
				case "getRequestURI" -> path;
				case "getScheme" -> "http";
				case "getServerName" -> "localhost";
				case "getServerPort" -> 18080;
				case "getContentType" -> "multipart/form-data; boundary=" + BOUNDARY;
				case "getContentLength" -> bytes.length;
				case "getContentLengthLong" -> (long) bytes.length;
				case "getCharacterEncoding" -> "UTF-8";
				case "getInputStream" -> input;
				case "getParameterMap" -> Map.of();
				case "getAttribute" -> attributes.get(args[0]);
				case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
				case "removeAttribute" -> { attributes.remove(args[0]); yield null; }
				default -> null;
			});
		}
	}

	/** The response of a request, as the client receives it. */
	public static class Response extends ServletOutputStream {
		public final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		public final ByteArrayOutputStream body = new ByteArrayOutputStream();
		/** true: the client disconnects at the first write */
		public boolean fail;
		@Override public void write(int b) throws IOException { write(new byte[] { (byte) b }, 0, 1); }
		@Override public void write(byte[] b, int off, int len) throws IOException {
			if (fail) throw new IOException("Test client disconnected");
			body.write(b, off, len);
		}
		@Override public boolean isReady() { return true; }
		@Override public void setWriteListener(WriteListener listener) { }
		public String text() { return body.toString(StandardCharsets.UTF_8); }
		public HttpServletResponse proxy() {
			var writer = new PrintWriter(new OutputStreamWriter(this, StandardCharsets.UTF_8));
			return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> switch (method.getName()) {
				case "setHeader", "addHeader" -> { headers.put((String) args[0], (String) args[1]); yield null; }
				case "setContentType" -> { headers.put("Content-Type", (String) args[0]); yield null; }
				case "getContentType" -> headers.get("Content-Type");
				case "getHeaderNames" -> headers.keySet();
				case "getOutputStream" -> this;
				case "getWriter" -> writer;
				default -> null;
			});
		}
	}

	/** Gives the parameters and the files of the request to the requestable, as a requester does. */
	private static class UploadRequester extends Requester {
		private final Function<HttpServletRequest, Object> requestable;
		UploadRequester(Function<HttpServletRequest, Object> requestable) { this.requestable = requestable; }
		@Override public Object processRequest(Object inputData) {
			return requestable.apply((HttpServletRequest) inputData);
		}
		@Override public String getName() { return "multipart-test"; }
		@Override public Context getContext() { return null; }
		@Override public void initContext(Context context) { }
		@Override public Document createDOM(String encodingCharSet) { return null; }
		@Override public Document parseDOM(String xml) { return null; }
		@Override public Translator getTranslator() { return null; }
		@Override public void makeInputDocument() { }
		@Override public Document getDocument() { return null; }
		@Override public void checkAccessibility() { }
		@Override public void checkSecuredConnection() { }
		@Override public void checkAuthenticatedContext() { }
		@Override public void checkParentContext() { }
	}
}
