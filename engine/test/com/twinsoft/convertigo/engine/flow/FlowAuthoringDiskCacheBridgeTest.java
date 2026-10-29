/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * The bridge with its disk cache: a tree computed once is served after a restart (memory and runtimes cleared) as it
 * was computed, without preparing a runtime; a change of the project computes it again; a probe tells it is on disk
 * without reading it; the shared definitions a response references travel with it. The provider calls need the actual
 * provider (FLOW_ENGINE_RESOURCE_ROOT, a copy of the _flow folder of lib_flow_engine).
 */
public class FlowAuthoringDiskCacheBridgeTest {
	private static Engine previous;
	private static volatile Project providerProject;
	private Path folder, project;
	private FlowAuthoringDiskCache cache;
	private FlowEngine owner;

	@BeforeClass
	public static void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = org.apache.log4j.Logger.getLogger("authoring-disk-cache-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			{
				symbolsProperties = new Properties();
			}

			@Override
			public Project getOriginalProjectByName(String name, boolean checkOpenable) {
				return "lib_flow_engine".equals(name) ? providerProject : null;
			}

			@Override
			public Project getLoadedProjectByName(String name) {
				return null;
			}
		};
	}

	@AfterClass
	public static void restore() {
		Engine.theApp = previous;
	}

	@Before
	public void create() throws Exception {
		folder = Files.createTempDirectory("authoring-disk-cache-bridge-");
		cache = new FlowAuthoringDiskCache(folder.resolve("cache"), FlowAuthoringDiskCache.DEFAULT_MAX_BYTES,
				"bridge-test-installation-key-32b".getBytes(StandardCharsets.UTF_8));
		FlowAuthoringDiskCache.use(cache);
		project = folder.resolve("DiskProof");
		Files.createDirectories(project.resolve("_flow/blocks/proof"));
		Files.writeString(project.resolve("c8oProject.yaml"), "↓DiskProof [core.Project]: \n");
		Files.writeString(project.resolve("_flow/engine.yaml"), "version: 1\nengineQName: lib_flow_engine.Engine\nconfig:\n  greeting: hello\n");
		model();
		age(project);
		FlowEngineBridge.clearCaches();
	}

	/** The beans of the project, loaded again from its files. */
	private void model() throws Exception {
		if (owner != null) {
			com.twinsoft.convertigo.beans.flow.Flow.projectUnloaded(owner.getProject());
		}
		var directory = project.toFile();
		var model = new Project() {
			@Override public File getDirFile() { return directory; }
			@Override public String getDirPath() { return directory.getAbsolutePath(); }
			@Override public FlowEngine getFlowEngine() { return owner; }
			@Override public List<Sequence> getSequencesList() { return List.of(); }
		};
		model.setName("DiskProof");
		owner = new FlowEngine() { @Override public Project getProject() { return model; } };
		owner.setName("FlowEngine");
	}

	@After
	public void remove() throws Exception {
		FlowAuthoringDiskCache.use(null);
		FlowEngineBridge.clearCaches();
		com.twinsoft.convertigo.beans.flow.Flow.projectUnloaded(owner.getProject());
		FileUtils.deleteDirectory(folder.toFile());
	}

	private static void age(Path root) throws Exception {
		var then = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
		try (var files = Files.walk(root)) {
			for (var file : (Iterable<Path>) files::iterator) {
				Files.setLastModifiedTime(file, then);
			}
		}
	}

	/** The provider of FLOW_ENGINE_RESOURCE_ROOT, as the project lib_flow_engine. */
	private static void provider() throws Exception {
		var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		Assume.assumeNotNull(root);
		var directory = new File(root).getAbsoluteFile().getParentFile();
		var model = new Project() {
			@Override public File getDirFile() { return directory; }
			@Override public String getDirPath() { return directory.getAbsolutePath(); }
		};
		model.setName("lib_flow_engine");
		providerProject = model;
	}

	private static int runtimes() throws Exception {
		var field = FlowEngineBridge.class.getDeclaredField("engineRuntimeCache");
		field.setAccessible(true);
		return ((Map<?, ?>) field.get(null)).size();
	}

	private String key() throws Exception {
		var request = new JSONObject().put("projectDir", project.toString());
		return FlowEngineBridge.persistentResponseKey("lib_flow_engine.Engine",
				new File(providerProject.getDirPath(), "_flow/Engine.js"), "describeTree", request);
	}

	@Test
	public void aTreeComputedOnceIsServedAfterARestartWithoutARuntime() throws Exception {
		provider();
		Assume.assumeNotNull("files of the provider modified in the last seconds: not kept on disk", key());
		var bridge = new FlowEngineBridge();
		var writes = FlowAuthoringDiskCache.statistics.writes.get();
		var computed = bridge.describeTree(owner);
		assertTrue(computed.toString(), computed.optBoolean("ok"));
		FlowAuthoringDiskCache.flush();
		assertEquals("written once", writes + 1, FlowAuthoringDiskCache.statistics.writes.get());

		// a restart: nothing in memory, no runtime
		FlowEngineBridge.clearCaches();
		var hits = FlowAuthoringDiskCache.statistics.hits.get();
		var served = bridge.describeTree(owner);
		assertEquals(hits + 1, FlowAuthoringDiskCache.statistics.hits.get());
		assertEquals("as computed", FlowEngineBridge.serializedResponse(computed), FlowEngineBridge.serializedResponse(served));
		assertEquals("no runtime prepared", 0, runtimes());
		// then from memory
		bridge.describeTree(owner);
		assertEquals(hits + 1, FlowAuthoringDiskCache.statistics.hits.get());

		// a probe tells it is there, without reading it
		FlowEngineBridge.clearCaches();
		var probe = FlowEngineBridge.probing("describeTree", () -> bridge.describeTree(owner));
		assertTrue(FlowEngineBridge.probeAnswered());
		assertTrue(probe.optBoolean("persisted"));
		assertEquals(hits + 1, FlowAuthoringDiskCache.statistics.hits.get());

		// a change of the project: computed again, the change shown
		Files.writeString(project.resolve("_flow/engine.yaml"), "version: 1\nengineQName: lib_flow_engine.Engine\nconfig:\n  greeting: changed\n");
		Files.setLastModifiedTime(project.resolve("_flow/engine.yaml"), FileTime.fromMillis(System.currentTimeMillis() - 30_000));
		model();
		FlowEngineBridge.clearCaches();
		var changed = bridge.describeTree(owner);
		assertEquals("not served from disk", hits + 1, FlowAuthoringDiskCache.statistics.hits.get());
		assertNotEquals(FlowEngineBridge.serializedResponse(computed), FlowEngineBridge.serializedResponse(changed));
		assertTrue(FlowEngineBridge.serializedResponse(changed).contains("changed"));
		FlowAuthoringDiskCache.flush();
		assertEquals(writes + 2, FlowAuthoringDiskCache.statistics.writes.get());
	}

	@Test
	public void aCallOfTheProviderThatMayWriteFilesWalksItsProjectAgain() throws Exception {
		provider();
		var before = key();
		Assume.assumeNotNull("files of the provider modified in the last seconds: not kept on disk", before);
		// written by the provider itself (block code, schemas, resources), not through a working copy
		var block = project.resolve("_flow/blocks/proof/written.block.js");
		Files.writeString(block, "written by the provider");
		Files.setLastModifiedTime(block, FileTime.fromMillis(System.currentTimeMillis() - 30_000));
		assertEquals("not seen yet: within the delay of the changes made outside the engine", before, key());
		try {
			new FlowEngineBridge().invoke("lib_flow_engine.Engine", "proofWrite", new JSONObject().put("projectDir", project.toString()),
					null, null, null);
		} catch (com.twinsoft.convertigo.engine.EngineException unknown) {
			// a method the provider does not know: a call that may write all the same
		}
		assertNotEquals("its project walked again", before, key());
	}

	@Test
	public void theSharedDefinitionsOfAResponseTravelWithIt() throws Exception {
		var shared = Engine.theApp.getShareServerMap();
		var registry = new java.util.concurrent.ConcurrentHashMap<Object, Object>();
		shared.set("flow.virtualDefinitions", registry);
		var ref = "vd-0123456789abcdef0123456789abcdef";
		var entry = "{\"iconify\":\"mdi:application-braces-outline\"}";
		registry.put(ref, entry);
		var text = "{\"ok\":true,\"children\":[{\"info\":\"{\\\"definitionRef\\\":\\\"" + ref + "\\\"}\"}]}";
		var parts = FlowEngineBridge.persistedParts(text);
		assertNotNull(parts);
		assertEquals(text, parts.get("response"));
		assertTrue(parts.get("virtualDefinitions").contains(ref));
		assertNull("a definition no longer published: not kept",
				FlowEngineBridge.persistedParts(text.replace(ref, "vd-ffffffffffffffffffffffffffffffff")));
		assertEquals("nothing referenced", Map.of("response", "{\"ok\":true}"), FlowEngineBridge.persistedParts("{\"ok\":true}"));

		var key = "a".repeat(64);
		cache.writeNow(key, parts);
		// a restart: the server map is empty
		shared.set("flow.virtualDefinitions", null);
		var response = FlowEngineBridge.persistedResponse(cache, key, "describeTree");
		assertEquals(text, FlowEngineBridge.serializedResponse(response));
		var published = (Map<?, ?>) shared.get("flow.virtualDefinitions");
		assertEquals(entry, published.get(ref));
		var info = new JSONObject().put("definitionRef", ref);
		assertEquals("mdi:application-braces-outline",
				com.twinsoft.convertigo.beans.flow.FlowVirtualObject.resolveSharedDefinitions(info).getString("iconify"));
	}

	@Test
	public void aResponseShowingIconFilesRemovedSinceIsComputedAgain() throws Exception {
		var icon = folder.resolve("icons/mdi/x_16x16.png");
		Files.createDirectories(icon.getParent());
		Files.writeString(icon, "png");
		var escaped = "{\"ok\":true,\"children\":[{\"info\":\"{\\\"iconFile16\\\":\\\"" + icon + "\\\"}\"}]}";
		var plain = "{\"ok\":true,\"iconFile\":\"" + icon + "\"}";
		cache.writeNow("b".repeat(64), Map.of("response", escaped));
		cache.writeNow("c".repeat(64), Map.of("response", plain));
		assertNotNull(FlowEngineBridge.persistedResponse(cache, "b".repeat(64), "describeTree"));
		assertNotNull(FlowEngineBridge.persistedResponse(cache, "c".repeat(64), "describeTree"));
		Files.delete(icon);
		assertFalse(FlowEngineBridge.iconFilesExist(escaped));
		assertNull(FlowEngineBridge.persistedResponse(cache, "b".repeat(64), "describeTree"));
		assertNull(FlowEngineBridge.persistedResponse(cache, "c".repeat(64), "describeTree"));
	}

	@Test
	public void onlyTheExpensiveAuthoringMethodsAreKeptOnDisk() throws Exception {
		var request = new JSONObject();
		for (var method : List.of("describeTree", "authoringTree", "authoringPalette", "catalog", "propertyEditor")) {
			assertTrue(method, FlowEngineBridge.isPersistedMethod(method, request));
		}
		for (var method : List.of("syncInputs", "contextMenu", "icons", "run", "preload", "applyMutation", "context")) {
			assertFalse(method, FlowEngineBridge.isPersistedMethod(method, request));
		}
	}
}
