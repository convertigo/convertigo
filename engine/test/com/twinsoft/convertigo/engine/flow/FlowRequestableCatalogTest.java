/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.*;

public class FlowRequestableCatalogTest {
	@Test public void serverCatalogVisitsOnlyTransitiveReferencesAndHandlesCyclesAndReloads() throws Exception {
		var previous = Engine.theApp;
		Engine.logEngine = org.apache.log4j.Logger.getLogger("catalog-test");
		Engine.logBeans = Engine.logEngine;
		EnginePropertiesManager.initProperties();
		assertFalse("This regression must run outside Studio", Engine.isStudioMode());
		var projects = new HashMap<String, Project>();
		var visits = new HashSet<String>();
		for (int i = 0; i < 50; i++) projects.put("Unrelated" + i, project("Unrelated" + i));
		projects.put("Consumer", project("Consumer", "Provider", "Missing"));
		projects.put("Provider", project("Provider", "Shared"));
		projects.put("Shared", project("Shared", "Consumer"));
		try {
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override public List<String> getAllProjectNamesList(boolean checkOpenable) {
					throw new AssertionError("A scoped catalogue must not enumerate the workspace");
				}
				@Override public Project getOriginalProjectByName(String name, boolean checkOpenable) throws EngineException {
					visits.add(name);
					if (!projects.containsKey(name)) throw new EngineException("Missing fixture");
					return projects.get(name);
				}
			};
			var owner = new FlowEngine() { @Override public Project getProject() { return projects.get("Consumer"); } };
			var bridge = new FlowEngineBridge();
			var catalog = bridge.requestables(owner).getJSONArray("projects");
			assertEquals(3, catalog.length());
			assertEquals("Consumer", catalog.getJSONObject(0).getString("name"));
			assertEquals(Set.of("Consumer", "Provider", "Shared", "Missing"), visits);
			// A replacement project/reference graph must be observed immediately.
			projects.put("Consumer", project("Consumer", "Shared"));
			projects.put("Shared", project("Shared"));
			visits.clear();
			assertEquals(2, bridge.requestables(owner).getJSONArray("projects").length());
			assertEquals(Set.of("Consumer", "Shared"), visits);
			assertEquals(0, bridge.requestables((FlowEngine) null).getJSONArray("projects").length());
		} finally { Engine.theApp = previous; }
	}

	@Test public void coldReadersShareMetadataButNotMutableResponsesAndObserveRenames() throws Exception {
		var previous = Engine.theApp;
		Engine.logEngine = org.apache.log4j.Logger.getLogger("catalog-test"); Engine.logBeans = Engine.logEngine;
		EnginePropertiesManager.initProperties();
		var sequence = new com.twinsoft.convertigo.beans.sequences.GenericSequence(); sequence.setName("Before");
		var project = new Project() {
			@Override public List<com.twinsoft.convertigo.beans.core.Sequence> getSequencesList() { return List.of(sequence); }
		}; project.setName("MetadataProof");
		var workers = java.util.concurrent.Executors.newFixedThreadPool(20);
		try {
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override public Project getOriginalProjectByName(String name, boolean openable) { return project; }
			};
			FlowEngineBridge.clearSharedMetadata();
			var field = FlowEngineBridge.class.getDeclaredField("sharedMetadata"); field.setAccessible(true);
			var cache = (FlowMetadataCache) field.get(null); long before = cache.builds();
			var start = new java.util.concurrent.CountDownLatch(1);
			var replies = new ArrayList<java.util.concurrent.Future<String>>();
			for (int i = 0; i < 20; i++) replies.add(workers.submit(() -> { start.await(); return FlowEngineBridge.requestableProjectCatalog("MetadataProof"); }));
			start.countDown();
			for (var reply : replies) assertEquals("Before", new org.codehaus.jettison.json.JSONObject(reply.get(5, java.util.concurrent.TimeUnit.SECONDS))
					.getJSONArray("children").getJSONObject(0).getString("name"));
			assertEquals(1, cache.builds() - before);
			var owner = new FlowEngine() { @Override public Project getProject() { return project; } };
			var bridge = new FlowEngineBridge();
			bridge.requestables(owner).getJSONArray("projects").getJSONObject(0).put("name", "ChangedByCaller");
			assertEquals("MetadataProof", bridge.requestables(owner).getJSONArray("projects").getJSONObject(0).getString("name"));
			sequence.setName("After");
			assertEquals("After", new org.codehaus.jettison.json.JSONObject(FlowEngineBridge.requestableProjectCatalog("MetadataProof"))
					.getJSONArray("children").getJSONObject(0).getString("name"));
			assertEquals(2, cache.builds() - before);
		} finally { workers.shutdownNow(); FlowEngineBridge.clearSharedMetadata(); Engine.theApp = previous; }
	}

	private static Project project(String name, String... names) throws Exception {
		var references = new ArrayList<Reference>();
		for (var target : names) { var reference = new ProjectSchemaReference(); reference.setProjectName(target); references.add(reference); }
		var project = new Project() { @Override public List<Reference> getReferenceList() { return references; } };
		project.setName(name);
		return project;
	}
}
