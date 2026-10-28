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
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Multipart;

/** A multipart REST request only creates its upload folder for the items written to disk, and removes it at its end (#1253). */
public class RestOperationMultipartTest {
	private final Project project = new Project();
	private PostOperation operation;
	private Consumer<Map<String, Object>> requestable;

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
			@Override InternalRequester newInternalRequester(Map<String, Object> map, HttpServletRequest request) throws EngineException {
				return new InternalRequester(map, request) {
					@Override public Object processRequest() {
						requestable.accept(map);
						return null;
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
		};
		assertEquals(List.of(), processStarts(() -> assertNull(operation.handleRequest(multipart.request("/upload"), response()))));
		assertEquals(before, uploadFolders());
	}

	@Test public void filesAndALargeFieldAreReadableDuringTheCallAndRemovedWithoutProcess() throws Throwable {
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
		};
		assertEquals(List.of(), processStarts(() -> assertNull(operation.handleRequest(multipart.request("/upload"), response()))));
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	@Test public void aFailingRequestableStillRemovesTheFolder() throws Throwable {
		var content = "content".getBytes(StandardCharsets.UTF_8);
		var multipart = new Multipart().file("file", "file.txt", content);
		var before = uploadFolders();
		var folder = new File[1];
		requestable = map -> {
			folder[0] = checkUploadedFile(((String[]) map.get("file"))[0], 1, "file.txt", content);
			throw new IllegalStateException("requestable failure");
		};
		var error = assertThrows(EngineException.class, () -> operation.handleRequest(multipart.request("/upload"), response()));
		assertEquals("requestable failure", error.getCause().getMessage());
		assertFalse(folder[0].exists());
		assertEquals(before, uploadFolders());
	}

	private static HttpServletResponse response() {
		return (HttpServletResponse) Proxy.newProxyInstance(RestOperationMultipartTest.class.getClassLoader(),
				new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> null);
	}
}
