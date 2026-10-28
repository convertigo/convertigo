/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.rest;

import static com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.checkUploadedFile;
import static com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.uploadFolders;
import static com.twinsoft.convertigo.engine.util.FileUtilsDeleteDirectoryTest.processStarts;
import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.rest.AbstractRestOperation.OutputContent;
import com.twinsoft.convertigo.engine.AttachmentManager.AttachmentDetails;
import com.twinsoft.convertigo.engine.AttachmentManagerTest;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.requesters.UrlMapperRequester;
import com.twinsoft.convertigo.engine.requesters.UrlMapperRequester.ResponseMode;
import com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Multipart;
import com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Response;
import com.twinsoft.convertigo.engine.servlets.RestApiServlet;
import com.twinsoft.convertigo.engine.util.JakartaServletFileUploadSupport;

/**
 * A multipart REST request only creates its upload folder for the items written to disk, and the folder is removed
 * once the response is written, as the REST servlet does at the end of the request: a binary response can stream an
 * uploaded file after the operation returns (#1253).
 */
public class RestOperationMultipartTest {
	private final Project project = new Project();
	private PostOperation operation;
	/** The requestable: returns the path of the file it answers as its top-level attachment, or null. */
	private Function<Map<String, Object>, String> requestable;

	@Before public void setup() throws Exception {
		Engine.logEngine = Engine.logContext = Engine.logBeans = Logger.getLogger("rest-operation-multipart-test");
		EnginePropertiesManager.initProperties();
		project.setName("RestUpload");
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override public Project getOriginalProjectByName(String name) {
				return project.getName().equals(name) ? project : null;
			}
		};
		var mapper = new UrlMapper();
		project.add(mapper);
		var mapping = new PathMapping();
		mapping.setPath("/upload");
		mapper.add(mapping);
		operation = new PostOperation() {
			private static final long serialVersionUID = 1L;
			@Override UrlMapperRequester createUrlMapperRequester(Map<String, Object> map, HttpServletRequest request, OutputContent outputContent) throws EngineException {
				var mode = outputContent == OutputContent.toBinary ? ResponseMode.binary : ResponseMode.defaultContent;
				return new UrlMapperRequester(map, request, mode) {
					@Override public Object processRequest() throws Exception {
						var path = requestable.apply(map);
						if (path == null) {
							return null;
						}
						context = new Context("rest-operation-multipart-test");
						var attachment = AttachmentManagerTest.attachment();
						attachment.setAttribute("local-url", path);
						return postGetDocument(attachment.getOwnerDocument());
					}
				};
			}
		};
		operation.setTargetRequestable("RestUpload.Echo");
		mapping.add(operation);
		for (var name : List.of("name", "file")) {
			var parameter = new FormParameter();
			parameter.setName(name);
			operation.add(parameter);
		}
	}

	@Test public void fieldsAndAnEmptyFileInputCreateNoFolderNorProcess() throws Throwable {
		var multipart = new Multipart().field("name", "value").file("file", "", new byte[0]);
		var before = uploadFolders();
		requestable = map -> {
			assertArrayEquals(new String[] { "value" }, (String[]) map.get("name"));
			assertNull(map.get("file"));
			assertEquals(before, uploadFolders());
			return null;
		};
		var request = multipart.request("/upload");
		assertEquals(List.of(), processStarts(() -> {
			assertNull(operation.handleRequest(request, response()));
			JakartaServletFileUploadSupport.removeUploadFolder(request);
		}));
		assertEquals(before, uploadFolders());
	}

	@Test public void filesAndALargeFieldAreKeptUntilTheResponseIsWrittenAndRemovedWithoutProcess() throws Throwable {
		// over the size threshold, written to disk by the factory
		var large = "x".repeat(5000);
		var small = "small".getBytes(StandardCharsets.UTF_8);
		var big = new byte[2 * 1024 * 1024];
		Arrays.fill(big, (byte) 7);
		var multipart = new Multipart().field("name", large).file("file", "small.txt", small).file("file", "big.bin", big);
		var before = uploadFolders();
		var folder = new File[1];
		requestable = map -> {
			assertArrayEquals(new String[] { large }, (String[]) map.get("name"));
			var files = (List<?>) map.get("file");
			assertEquals(2, files.size());
			folder[0] = checkUploadedFile((String) files.get(0), 1, "small.txt", small);
			assertEquals(folder[0], checkUploadedFile((String) files.get(1), 2, "big.bin", big));
			return null;
		};
		var request = multipart.request("/upload");
		assertEquals(List.of(), processStarts(() -> {
			assertNull(operation.handleRequest(request, response()));
			assertTrue(folder[0].exists());
			JakartaServletFileUploadSupport.removeUploadFolder(request);
		}));
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	@Test public void aFailingRequestableLeavesTheFolderToTheEndOfTheRequest() throws Throwable {
		var content = "content".getBytes(StandardCharsets.UTF_8);
		var multipart = new Multipart().file("file", "file.txt", content);
		var before = uploadFolders();
		var folder = new File[1];
		requestable = map -> {
			folder[0] = checkUploadedFile(((String[]) map.get("file"))[0], 1, "file.txt", content);
			throw new IllegalStateException("requestable failure");
		};
		var request = multipart.request("/upload");
		var error = assertThrows(EngineException.class, () -> operation.handleRequest(request, response()));
		assertEquals("requestable failure", error.getCause().getMessage());
		JakartaServletFileUploadSupport.removeUploadFolder(request);
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	@Test public void anUploadedFileAnsweredAsABinaryResponseIsStreamedThenRemoved() throws Throwable {
		var content = new byte[3 * 1024 * 1024];
		new Random(1253).nextBytes(content);
		var multipart = new Multipart().file("file", "image.png", content);
		operation.setOutputContent(OutputContent.toBinary);
		var before = uploadFolders();
		var folder = new File[1];
		requestable = map -> {
			var path = ((String[]) map.get("file"))[0];
			folder[0] = checkUploadedFile(path, 1, "image.png", content);
			// the sequence answers the uploaded file as its attachment, read once the response is written
			return path;
		};
		var request = multipart.request("/upload");
		var response = new Response();
		assertEquals(List.of(), processStarts(() -> {
			var attachment = operation.handleRequest(request, response.proxy());
			assertTrue(attachment instanceof AttachmentDetails);
			assertTrue(folder[0].exists());
			writeResponseContent(request, response.proxy(), attachment);
			JakartaServletFileUploadSupport.removeUploadFolder(request);
		}));
		assertArrayEquals(content, response.body.toByteArray());
		assertEquals(Integer.toString(content.length), response.headers.get("Content-Length"));
		assertEquals("attachment; filename=\"file.bin\"", response.headers.get("Content-Disposition"));
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	/** Writes the content returned by the operation, as the REST servlet does before the end of the request. */
	private static void writeResponseContent(HttpServletRequest request, HttpServletResponse response, Object content) throws Exception {
		var method = RestApiServlet.class.getDeclaredMethod("writeResponseContent", HttpServletRequest.class,
				HttpServletResponse.class, Object.class);
		method.setAccessible(true);
		try {
			method.invoke(new RestApiServlet(), request, response, content);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception cause) throw cause;
			throw e;
		}
	}

	private static HttpServletResponse response() {
		return (HttpServletResponse) Proxy.newProxyInstance(RestOperationMultipartTest.class.getClassLoader(),
				new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> null);
	}
}
