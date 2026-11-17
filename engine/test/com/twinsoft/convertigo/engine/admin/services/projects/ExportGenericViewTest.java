/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.admin.services.projects;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;

public class ExportGenericViewTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void archiveServiceFinishesWithALongFlow() throws Exception {
		var previousApp = Engine.theApp;
		var previousProjects = Engine.PROJECTS_PATH;
		try {
			Engine.logEngine = Engine.logBeans = Engine.logAdmin = Logger.getLogger("export-generic-view-test");
			Engine.PROJECTS_PATH = folder.newFolder("projects").getAbsolutePath();
			var project = new Project();
			project.setName("GenericExport");
			var directory = Path.of(Engine.PROJECTS_PATH, project.getName());
			Files.createDirectories(directory);
			Files.writeString(directory.resolve("c8oProject.yaml"), "project fixture\n");
			project.add(new FlowEngine());
			Files.createDirectories(directory.resolve("_flow"));
			Files.writeString(directory.resolve("_flow/dependencies.json"), "{\"existing\":true}\n");
			var flow = new Flow();
			flow.setName("Slow");
			project.add(flow);
			// a certain header is written as the runtime would have synchronized it (Public differs from the Private default)
			flow.setFlowSource("const _flow = { accessibility: 'Public', authenticatedContextRequired: true };\n" + "x += 1;\n".repeat(100_000));
			// an uncertain header is shown closed in a view, but the archive keeps the stored property
			var uncertain = new Flow();
			uncertain.setName("Uncertain");
			project.add(uncertain);
			uncertain.setAccessibility(com.twinsoft.convertigo.engine.enums.Accessibility.Hidden);
			uncertain.setFlowSource("const a = 1; const _flow = { accessibility: 'Public' };\n" + "x += 1;\n".repeat(100_000));
			try (var view = com.twinsoft.convertigo.engine.flow.FlowGenericView.open()) {
				assertEquals(com.twinsoft.convertigo.engine.enums.Accessibility.Private, uncertain.getAccessibility());
				assertTrue(uncertain.getAuthenticatedContextRequired());
			}
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override public Project getOriginalProjectByName(String name) { return project; }
			};
			var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) ->
						"getParameter".equals(method.getName()) ?
							"projectName".equals(args[0]) ? project.getName() :
							"exportOptions".equals(args[0]) ? "{\"includeTestCase\":false}" : null : null);
			var bytes = new ByteArrayOutputStream();
			var output = new ServletOutputStream() {
				@Override public void write(int value) { bytes.write(value); }
				@Override public boolean isReady() { return true; }
				@Override public void setWriteListener(WriteListener listener) { }
			};
			var response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {HttpServletResponse.class}, (proxy, method, args) ->
						"getOutputStream".equals(method.getName()) ? output : null);
			long started = System.nanoTime();
			long bridgeCalls = com.twinsoft.convertigo.engine.flow.FlowEngineBridge.syncInputsAttempts();
			long runtimeCalls = com.twinsoft.convertigo.engine.flow.FlowBridgeProbe.calls();
			new Export().writeResponseResult(request, response);
			assertEquals(bridgeCalls, com.twinsoft.convertigo.engine.flow.FlowEngineBridge.syncInputsAttempts());
			assertEquals(runtimeCalls, com.twinsoft.convertigo.engine.flow.FlowBridgeProbe.calls());
			assertTrue("export took over one second", System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1));
			assertTrue(bytes.size() > 0);
			String yaml = null;
			String flowYaml = null;
			String uncertainYaml = null;
			String dependencies = null;
			try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
				java.util.zip.ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					if (entry.getName().endsWith("/c8oProject.yaml")) yaml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
					if (entry.getName().endsWith("/sequences/Slow.yaml")) flowYaml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
					if (entry.getName().endsWith("/sequences/Uncertain.yaml")) uncertainYaml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
					if (entry.getName().endsWith("/_flow/dependencies.json")) dependencies = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
				}
			}
			assertNotNull("archive has c8oProject.yaml", yaml);
			assertTrue("project YAML does not reference the Flow", yaml.contains("sequences/Slow.yaml"));
			assertNotNull("Flow YAML missing from archive", flowYaml);
			assertTrue("declared accessibility missing from archive: " + flowYaml, flowYaml.contains("accessibility: Public"));
			assertTrue("declared authentication missing from archive: " + flowYaml, flowYaml.contains("authenticatedContextRequired: true"));
			assertNotNull("uncertain Flow YAML missing from archive", uncertainYaml);
			assertTrue("stored accessibility missing from archive: " + uncertainYaml, uncertainYaml.contains("accessibility: Hidden"));
			assertFalse("a closed view value was archived: " + uncertainYaml, uncertainYaml.contains("accessibility: Private"));
			assertFalse("a closed view value was archived: " + uncertainYaml, uncertainYaml.contains("authenticatedContextRequired: true"));
			assertEquals("{\"existing\":true}\n", dependencies);
			assertEquals("{\"existing\":true}\n", Files.readString(directory.resolve("_flow/dependencies.json")));
			assertFalse(com.twinsoft.convertigo.engine.flow.FlowGenericView.active());
		} finally {
			Engine.PROJECTS_PATH = previousProjects;
			Engine.theApp = previousApp;
		}
	}
}
