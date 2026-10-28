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
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.HttpServletRequestTwsWrapper;

/** A multipart request only creates its upload folder for the items written to disk, and starts no process (#1253). */
public class GenericServletMultipartTest {
	private static final File TMP = new File(System.getProperty("java.io.tmpdir"));

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logContext = Logger.getLogger("generic-servlet-multipart-test");
		EnginePropertiesManager.initProperties();
	}

	@Test public void fieldsAndAnEmptyFileInputCreateNoFolderNorProcess() throws Throwable {
		var multipart = new Multipart().field("name", "value").file("file", "", new byte[0]);
		var before = uploadFolders();
		var starts = processStarts(() -> assertEquals("done", post(multipart, request -> {
			assertEquals("value", request.getParameter("name"));
			assertEquals("", request.getParameter("file"));
			assertEquals(before, uploadFolders());
		})));
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
		var starts = processStarts(() -> assertEquals("done", post(multipart, request -> {
			assertEquals(large, request.getParameter("large"));
			var files = request.getParameterValues("files");
			assertEquals(2, files.length);
			folder[0] = checkUploadedFile(files[0], 1, "small.txt", small);
			assertEquals(folder[0], checkUploadedFile(files[1], 2, "big.bin", big));
		})));
		assertEquals(List.of(), starts);
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
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

	private Object post(Multipart multipart, Consumer<HttpServletRequest> check) throws Exception {
		var requester = new UploadRequester(check);
		var servlet = new GenericServlet() {
			private static final long serialVersionUID = 1L;
			@Override public Requester getRequester() { return requester; }
			@Override public String getDocumentExtension() { return ".xml"; }
			@Override void processRequestEnd(HttpServletRequest request, Requester requester) { }
		};
		return servlet.processRequest(new HttpServletRequestTwsWrapper(multipart.request("/")));
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

		public HttpServletRequest request(String pathInfo) throws IOException {
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
				case "getPathInfo" -> pathInfo;
				case "getContentType" -> "multipart/form-data; boundary=" + BOUNDARY;
				case "getContentLength" -> bytes.length;
				case "getContentLengthLong" -> (long) bytes.length;
				case "getCharacterEncoding" -> "UTF-8";
				case "getInputStream" -> input;
				case "getParameterMap" -> Map.of();
				case "getAttribute" -> attributes.get(args[0]);
				case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
				default -> null;
			});
		}
	}

	/** Checks the parameters and the files of the request, as a requestable would use them. */
	private static class UploadRequester extends Requester {
		private final Consumer<HttpServletRequest> check;
		UploadRequester(Consumer<HttpServletRequest> check) { this.check = check; }
		@Override public Object processRequest(Object inputData) {
			check.accept((HttpServletRequest) inputData);
			return "done";
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
