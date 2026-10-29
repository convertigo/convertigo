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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

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
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * The warm-up plans of the actual provider (FLOW_ENGINE_RESOURCE_ROOT, a copy of the _flow folder of lib_flow_engine
 * 0.1.120 or later): the requests they prepare are those the Studio sends, whose responses it then finds in memory, or
 * on disk after a restart.
 */
public class FlowAuthoringWarmupProviderTest {
	private static Engine previous;
	private static volatile Project providerProject, loaded;
	private Path folder, project;
	private FlowEngine owner;
	private Flow flow;

	@BeforeClass
	public static void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = org.apache.log4j.Logger.getLogger("authoring-warmup-provider-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			{
				symbolsProperties = new Properties();
			}

			@Override
			public Project getOriginalProjectByName(String name, boolean checkOpenable) {
				return "lib_flow_engine".equals(name) ? providerProject : loaded != null && loaded.getName().equals(name) ? loaded : null;
			}

			@Override
			public Project getLoadedProjectByName(String name) {
				return loaded != null && loaded.getName().equals(name) ? loaded : null;
			}

			@Override
			public List<Project> getLoadedProjects() {
				return loaded == null ? List.of() : List.of(loaded);
			}
		};
	}

	@AfterClass
	public static void restore() {
		Engine.theApp = previous;
	}

	@Before
	public void create() throws Exception {
		var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		Assume.assumeNotNull(root);
		var directory = new File(root).getAbsoluteFile().getParentFile();
		var provider = new Project() {
			@Override public File getDirFile() { return directory; }
			@Override public String getDirPath() { return directory.getAbsolutePath(); }
		};
		provider.setName("lib_flow_engine");
		providerProject = provider;

		folder = Files.createTempDirectory("authoring-warmup-provider-");
		FlowAuthoringDiskCache.use(new FlowAuthoringDiskCache(folder.resolve("cache"), FlowAuthoringDiskCache.DEFAULT_MAX_BYTES,
				"warmup-provider-installation-32b".getBytes(StandardCharsets.UTF_8)));
		project = folder.resolve("WarmProof");
		Files.createDirectories(project.resolve("_flow/flows"));
		Files.writeString(project.resolve("c8oProject.yaml"), "↓WarmProof [core.Project]: \n");
		Files.writeString(project.resolve("_flow/engine.yaml"), "version: 1\nengineQName: lib_flow_engine.Engine\nconfig:\n  greeting: hello\n");
		Files.writeString(project.resolve("_flow/flows/Hello.flow.js"),
				"const _flow = { sourceVersion: 2 }\nfunction Hello({ input, config, result }) {\n  result.greeting = config.greeting\n}\n");
		var then = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
		try (var files = Files.walk(project)) {
			for (var file : (Iterable<Path>) files::iterator) {
				Files.setLastModifiedTime(file, then);
			}
		}
		var dir = project.toFile();
		var model = new Project() {
			@Override public File getDirFile() { return dir; }
			@Override public String getDirPath() { return dir.getAbsolutePath(); }
			@Override public FlowEngine getFlowEngine() { return owner; }
			@Override public List<Sequence> getSequencesList() { return List.of(flow); }
		};
		model.setName("WarmProof");
		owner = new FlowEngine() { @Override public Project getProject() { return model; } };
		owner.setName("FlowEngine");
		flow = new Flow() { @Override public Project getProject() { return model; } };
		flow.setName("Hello");
		loaded = model;
		FlowEngineBridge.clearCaches();
	}

	@After
	public void remove() throws Exception {
		loaded = null;
		if (folder != null) {
			FlowAuthoringDiskCache.use(null);
			FlowEngineBridge.clearCaches();
			Flow.projectUnloaded(owner.getProject());
			FileUtils.deleteDirectory(folder.toFile());
		}
	}

	private static long memoryHits() throws Exception {
		var field = FlowEngineBridge.class.getDeclaredField("methodResponseCacheHits");
		field.setAccessible(true);
		return ((LongAdder) field.get(null)).sum();
	}

	@Test
	public void thePlansPrepareTheRequestsTheStudioSends() throws Exception {
		var studio = FlowAuthoringWarmup.PLANNER.plan("WarmProof", FlowAuthoringWarmup.STUDIO);
		assertEquals("the plan of the provider, not the plan without plan: " + studio, 1, studio.size());
		var tree = studio.get(0);
		assertEquals("describeTree", tree.method());
		assertNotNull("options of the provider", tree.request());
		var projectStage = FlowAuthoringWarmup.PLANNER.plan("WarmProof", FlowAuthoringWarmup.PROJECT);
		var palette = projectStage.stream().filter(item -> "Hello".equals(item.flow())).findFirst().orElse(null);
		assertNotNull("the palette of the Flow: " + projectStage, palette);
		assertEquals("authoringPalette", palette.method());
		assertEquals("no frontend, nothing else", 1, projectStage.size());

		var items = new ArrayList<>(studio);
		items.addAll(projectStage);
		for (var item : FlowAuthoringWarmup.ordered(items)) {
			FlowEngineBridge.inBackground(() -> FlowAuthoringWarmup.PREPARATION.prepare(item));
		}

		// the Studio then asks the same requests: answered from memory
		var hits = memoryHits();
		var expanded = new FlowEngineBridge().describeTree(owner);
		assertTrue(expanded.toString(), expanded.optBoolean("ok"));
		assertEquals("the tree of the first expansion is the one prepared", hits + 1, memoryHits());
		FlowStudioSupport.paletteCategories(flow);
		assertEquals("the palette of the Flow is the one prepared", hits + 2, memoryHits());

		// a restart: what was prepared is on disk, even computed in a few ms in a runtime ready for it
		FlowAuthoringDiskCache.flush();
		FlowEngineBridge.clearCaches();
		Assume.assumeNotNull("files of the provider modified in the last seconds: not kept on disk",
				FlowEngineBridge.persistentResponseKey("lib_flow_engine.Engine", new File(providerProject.getDirPath(), "_flow/Engine.js"),
						"describeTree", new JSONObject().put("projectDir", project.toString())));
		for (var item : items) {
			assertFalse("found on disk, not computed: " + item.id(),
					FlowEngineBridge.inBackground(() -> FlowAuthoringWarmup.PREPARATION.prepare(item)));
		}
		FlowEngineBridge.probing("describeTree", () -> new FlowEngineBridge().describeTree(owner));
		assertTrue("the Studio finds the tree on disk", FlowEngineBridge.probeAnswered());
		FlowEngineBridge.probing("authoringPalette", () -> FlowStudioSupport.paletteCategories(flow));
		assertTrue("the Studio finds the palette of the Flow on disk", FlowEngineBridge.probeAnswered());
	}

	@Test
	public void aPlanItemFailingThrows() throws Exception {
		var unknown = new FlowAuthoringWarmup.Item("WarmProof", "palette", "authoringPalette", "Missing", "{}", 0, List.of());
		try {
			FlowAuthoringWarmup.PREPARATION.prepare(unknown);
			throw new AssertionError("a Flow that does not exist");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("Missing"));
		}
		var focus = new FlowAuthoringWarmup.Item("WarmProof", "palette", "authoringPalette", "",
				new JSONObject().put("surface", "virtual").put("focusPath", "nowhere.at.all").toString(), 0, List.of());
		try {
			FlowEngineBridge.inBackground(() -> FlowAuthoringWarmup.PREPARATION.prepare(focus));
			throw new AssertionError("a node that does not exist");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("FOCUS_NOT_FOUND"));
		}
	}
}
