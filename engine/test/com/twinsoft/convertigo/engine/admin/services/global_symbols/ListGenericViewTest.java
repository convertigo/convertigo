/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ListGenericViewTest {
	@Test public void symbolsServiceWalksALongFlowWithoutRunningItsSource() throws Exception {
		var previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("symbols-generic-view-test");
		var flow = new Flow();
		flow.setName("Slow");
		flow.setFlowSource("const _flow = { accessibility: 'Private' };\n" + "x += 1;\n".repeat(100_000));
		var project = new Project() {
			@Override public java.util.List<Sequence> getSequencesList() { return java.util.List.of(flow); }
		};
		project.setName("GenericSymbols");
		var manager = new DatabaseObjectsManager() {
			{ symbolsProperties = new Properties(); }
			@Override public java.util.List<String> getAllProjectNamesList() { return java.util.List.of(project.getName()); }
			@Override public Project getOriginalProjectByName(String name) { return project; }
		};
		var projects = DatabaseObjectsManager.class.getDeclaredField("projects");
		projects.setAccessible(true);
		projects.set(manager, new HashMap<>(java.util.Map.of(project.getName(), project)));
		try {
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = manager;
			var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
			document.appendChild(document.createElement("admin"));
			long started = System.nanoTime();
			long bridgeCalls = com.twinsoft.convertigo.engine.flow.FlowEngineBridge.syncInputsAttempts();
			long runtimeCalls = com.twinsoft.convertigo.engine.flow.FlowBridgeProbe.calls();
			new com.twinsoft.convertigo.engine.admin.services.global_symbols.List().getServiceResult(null, document);
			assertEquals(bridgeCalls, com.twinsoft.convertigo.engine.flow.FlowEngineBridge.syncInputsAttempts());
			assertEquals(runtimeCalls, com.twinsoft.convertigo.engine.flow.FlowBridgeProbe.calls());
			assertTrue("global_symbols.List took over one second", System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1));
			assertEquals(1, document.getElementsByTagName("symbols").getLength());
			assertFalse(com.twinsoft.convertigo.engine.flow.FlowGenericView.active());
		} finally { Engine.theApp = previous; }
	}
}
