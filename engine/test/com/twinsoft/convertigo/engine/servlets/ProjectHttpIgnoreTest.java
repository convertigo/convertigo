/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.startup.Tomcat;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** Actual HTTP requests through Tomcat and ProjectsDataFilter, no running engine required. */
public class ProjectHttpIgnoreTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Tomcat tomcat;
	private Path project;
	private String previousProjects;
	private String previousWebapp;
	private String base;
	private final HttpClient client = HttpClient.newHttpClient();

	@Before public void setup() throws Exception {
		previousProjects = Engine.PROJECTS_PATH;
		previousWebapp = Engine.WEBAPP_PATH;
		Engine.logContext = Engine.logEngine = Engine.logSiteClipper = Logger.getLogger("metadata-http-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.PROJECTS_DATA_COMPATIBILITY_MODE, "false");
		Engine.PROJECTS_PATH = folder.newFolder("projects").getAbsolutePath();
		Engine.WEBAPP_PATH = folder.newFolder("webapp").getAbsolutePath();
		project = Files.createDirectory(Path.of(Engine.PROJECTS_PATH, "Demo"));
		for (String path : List.of(".project", "nested/.project", "public.txt", "nested/public.txt",
				".project.json", ".project-backup", ".settings.json", ".settings-backup",
				".settings/editor.prefs", "nested/.settings/editor.prefs", "blocked.txt", "allowed.txt")) {
			Path file = project.resolve(path);
			Files.createDirectories(file.getParent());
			Files.writeString(file, "content:" + path);
		}
		tomcat = new Tomcat();
		tomcat.setBaseDir(folder.newFolder("tomcat").getAbsolutePath());
		tomcat.setPort(0);
		tomcat.getConnector().setProperty("address", "127.0.0.1");
		var context = tomcat.addContext("/convertigo", Engine.WEBAPP_PATH);
		var filter = new FilterDef();
		filter.setFilterName("projects");
		filter.setFilter(new ProjectsDataFilter());
		context.addFilterDef(filter);
		var mapping = new FilterMap();
		mapping.setFilterName("projects");
		mapping.addURLPattern("/projects/*");
		context.addFilterMap(mapping);
		Tomcat.addServlet(context, "fallback", new HttpServlet() {
			private static final long serialVersionUID = 1L;
			@Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
				response.sendError(404);
			}
		});
		context.addServletMappingDecoded("/", "fallback");
		tomcat.start();
		base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/convertigo/projects/Demo/";
	}

	@After public void cleanup() throws Exception {
		try {
			if (tomcat != null) { tomcat.stop(); tomcat.destroy(); }
		} finally {
			Engine.PROJECTS_PATH = previousProjects;
			Engine.WEBAPP_PATH = previousWebapp;
		}
	}

	@Test public void hidesDescriptionsWithoutHttpIgnoreAndKeepsOtherResources() throws Exception {
		assertFalse(Files.exists(project.resolve(".httpignore")));
		for (String path : List.of(".project", "nested/.project", "%2eproject", "nested/../.project", ".httpignore",
				".settings", ".settings/", ".settings/editor.prefs", "nested/.settings/editor.prefs",
				"%2esettings/editor.prefs", "nested/../.settings/editor.prefs")) {
			assertEquals(path, 404, get(path).statusCode());
		}
		for (String path : List.of("public.txt", "nested/public.txt", ".project.json", ".project-backup", ".settings.json", ".settings-backup")) {
			assertEquals(path, 200, get(path).statusCode());
			assertEquals("content:" + path, get(path).body());
		}
		for (String path : List.of(".project", ".settings/editor.prefs", "nested/.settings/editor.prefs")) {
			var head = client.send(HttpRequest.newBuilder(URI.create(base + path)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
			assertEquals(path, 404, head.statusCode());
			assertEquals("", head.body());
		}
	}

	@Test public void keepsExistingRulesNegationsAndCacheReloads() throws Exception {
		Path ignore = Files.writeString(project.resolve(".httpignore"), "*.txt\n!allowed.txt\n!.project\n!nested/.project\n!.settings/\n!.settings/**\n!nested/.settings/\n!nested/.settings/**\n");
		for (String path : List.of(".project", "nested/.project", ".httpignore", "blocked.txt", ".settings",
				".settings/", ".settings/editor.prefs", "nested/.settings/editor.prefs")) {
			assertEquals(path, 404, get(path).statusCode());
		}
		assertEquals(200, get("allowed.txt").statusCode());
		Files.writeString(ignore, "public.txt\n");
		Files.setLastModifiedTime(ignore, FileTime.fromMillis(System.currentTimeMillis() + 2000));
		assertEquals(404, get("public.txt").statusCode());
		assertEquals(200, get("blocked.txt").statusCode());
		Files.delete(ignore);
		assertEquals(200, get("public.txt").statusCode());
		assertEquals(404, get(".project").statusCode());
		assertEquals(404, get(".settings/editor.prefs").statusCode());
	}

	@Test public void hidesDescriptionsAtAnyPathSegment() throws Exception {
		for (String path : List.of("/.project/", "nested/.project/child", "nested\\.project", "nested/.httpignore/child",
				"/.settings/", "nested/.settings/child", "nested\\.settings\\child")) {
			assertTrue(path, ProjectHttpIgnore.isIgnored(project.toFile(), path, false));
		}
		assertFalse(ProjectHttpIgnore.isIgnored(project.toFile(), "nested/.project.json", false));
		assertFalse(ProjectHttpIgnore.isIgnored(project.toFile(), "nested/.settings.json", false));
	}

	private HttpResponse<String> get(String path) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
	}
}
