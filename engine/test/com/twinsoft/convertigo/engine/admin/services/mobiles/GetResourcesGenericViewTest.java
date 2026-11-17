/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.admin.services.mobiles;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.mobileplatforms.Android;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;

public class GetResourcesGenericViewTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void resourcesServiceFinishesWithALongFlowInTheProject() throws Exception {
		var previousApp = Engine.theApp;
		var previousProjects = Engine.PROJECTS_PATH;
		try {
			Engine.logBeans = Engine.logAdmin = Engine.logEngine = Logger.getLogger("mobile-generic-view-test");
			Engine.PROJECTS_PATH = folder.newFolder("projects").getAbsolutePath();
			var project = new Project();
			project.setName("GenericMobile");
			Files.createDirectories(Path.of(Engine.PROJECTS_PATH, project.getName()));
			var flow = new Flow();
			flow.setName("Slow");
			project.add(flow);
			flow.setFlowSource("const _flow = { accessibility: 'Private' };\n" + "x += 1;\n".repeat(100_000));
			var mobile = new MobileApplication();
			mobile.setEnableFlashUpdate(false);
			mobile.setEndpoint("http://localhost");
			project.add(mobile);
			var android = new Android();
			android.setName("Android");
			mobile.add(android);
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override public Project getOriginalProjectByName(String name) { return project; }
			};
			var parameters = Map.of("project", project.getName(), "platform", android.getName(), "uuid", "test");
			var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) ->
						"getParameter".equals(method.getName()) ? parameters.get(args[0]) : null);
			var response = new JSONObject();
			long started = System.nanoTime();
			long bridgeCalls = com.twinsoft.convertigo.engine.flow.FlowEngineBridge.syncInputsAttempts();
			long runtimeCalls = com.twinsoft.convertigo.engine.flow.FlowBridgeProbe.calls();
			new GetResources().getServiceResult(request, response);
			assertEquals(bridgeCalls, com.twinsoft.convertigo.engine.flow.FlowEngineBridge.syncInputsAttempts());
			assertEquals(runtimeCalls, com.twinsoft.convertigo.engine.flow.FlowBridgeProbe.calls());
			assertTrue("GetResources took over one second", System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1));
			assertFalse(response.getBoolean("flashUpdateEnabled"));
			assertFalse(com.twinsoft.convertigo.engine.flow.FlowGenericView.active());
		} finally {
			Engine.PROJECTS_PATH = previousProjects;
			Engine.theApp = previousApp;
		}
	}
}
