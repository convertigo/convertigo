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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.SchemaManager;
import com.twinsoft.convertigo.engine.sync.SharedWorkspaceSyncManager;

/**
 * The persistent keys of an instance of a shared workspace when another instance changes a project: its marker of a
 * reload (a save of the project, a deployment) unloads the project, its marker of changed files (sources written
 * without a save) invalidates the data caches of the project without reloading it, and either way the next authoring
 * call of this instance misses, without waiting for the delay of the changes made outside the engines. The markers of
 * this instance are not applied to itself; the sources the provider writes without a working copy are told once,
 * those of a working copy not at all.
 */
public class SharedWorkspaceAuthoringKeyTest {
	private static Engine previous;
	private static String previousConfiguration;
	private static final Map<String, Project> models = new ConcurrentHashMap<>();
	private static final AtomicInteger reloads = new AtomicInteger();
	private Path workspace, configuration;
	private File provider, engineFile, app;
	private Project model;
	private FlowEngine owner;
	private Object manager;

	@BeforeClass
	public static void initialize() throws Exception {
		previous = Engine.theApp;
		previousConfiguration = Engine.CONFIGURATION_PATH;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = org.apache.log4j.Logger.getLogger("shared-workspace-key-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.schemaManager = new SchemaManager();
		var objects = new DatabaseObjectsManager() {
			{
				symbolsProperties = new Properties();
			}

			@Override
			public Project getOriginalProjectByName(String name, boolean checkOpenable) {
				return models.get(name);
			}

			@Override
			public Project getOriginalProjectByName(String name) {
				return models.get(name);
			}

			@Override
			public Project getProjectByName(String name) {
				// loaded again from its files, as the import of the project does
				reloads.incrementAndGet();
				var project = models.get(name);
				loaded(this).put(name, project);
				return project;
			}
		};
		setField(DatabaseObjectsManager.class, objects, "projects", new HashMap<String, Project>());
		setField(DatabaseObjectsManager.class, objects, "importLocks", new ConcurrentHashMap<>());
		Engine.theApp.databaseObjectsManager = objects;
	}

	@AfterClass
	public static void restore() {
		EnginePropertiesManager.setProperty(PropertyName.SESSION_SHARED_WORKSPACE_SYNC_ENABLED, "false");
		Engine.theApp = previous;
		Engine.CONFIGURATION_PATH = previousConfiguration;
	}

	@Before
	public void create() throws Exception {
		AuthoringKeyClock.awaitVerifications();
		workspace = Files.createTempDirectory("shared-workspace-key-");
		configuration = Files.createDirectories(workspace.resolve("configuration"));
		Engine.CONFIGURATION_PATH = configuration.toString();
		EnginePropertiesManager.setProperty(PropertyName.SESSION_SHARED_WORKSPACE_SYNC_ENABLED, "true");
		provider = folder("lib_flow_engine");
		Files.writeString(new File(provider, "_flow/Engine.js").toPath(), "({})");
		engineFile = new File(provider, "_flow/Engine.js");
		app = folder("app");
		Files.writeString(new File(app, "_flow/blocks/page.block.js").toPath(), "page");
		age(workspace.toFile());
		var directory = app;
		model = new Project() {
			@Override public File getDirFile() { return directory; }
			@Override public String getDirPath() { return directory.getAbsolutePath(); }
			@Override public FlowEngine getFlowEngine() { return owner; }
			@Override public List<Sequence> getSequencesList() { return List.of(); }
		};
		model.setName("app");
		owner = new FlowEngine() { @Override public Project getProject() { return model; } };
		owner.setName("FlowEngine");
		models.put("app", model);
		loaded(Engine.theApp.databaseObjectsManager).put("app", model);
		reloads.set(0);
		FlowEngineBridge.clearCaches();
		manager = new SharedWorkspaceSyncManager();
		call("initializeMarkersState");
	}

	@After
	public void remove() throws Exception {
		AuthoringKeyClock.awaitVerifications();
		owner.discardSource(new File(app, "_flow/blocks/draft.block.js").getPath());
		loaded(Engine.theApp.databaseObjectsManager).clear();
		models.clear();
		FlowEngineBridge.clearCaches();
		FileUtils.deleteDirectory(workspace.toFile());
	}

	private File folder(String name) throws Exception {
		var root = workspace.resolve(name).toFile();
		Files.createDirectories(new File(root, "_flow/blocks").toPath());
		Files.createDirectories(new File(root, "_c8oProject").toPath());
		Files.writeString(new File(root, "c8oProject.yaml").toPath(), "↓" + name + " [core.Project]: \n");
		Files.writeString(new File(root, "_flow/engine.yaml").toPath(), "version: 1\n");
		return root;
	}

	private static void age(File folder) throws Exception {
		var then = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
		try (var files = Files.walk(folder.toPath())) {
			for (var file : (Iterable<Path>) files::iterator) {
				Files.setLastModifiedTime(file, then);
			}
		}
	}

	/** A file of the project changed by another instance, dated an hour ago: not recent. */
	private void changedElsewhere(String text) throws Exception {
		var file = new File(app, "_flow/blocks/page.block.js").toPath();
		Files.writeString(file, text);
		Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));
	}

	private String key() throws Exception {
		var request = new JSONObject().put("engineQName", "lib_flow_engine.Engine").put("target", "engine")
				.put("projectDir", app.getPath()).put("engineSource", "version: 1\n")
				.put("frontendSourceDrafts", new JSONObject()).put("sourceRemovals", new JSONArray());
		return FlowEngineBridge.persistentResponseKey("lib_flow_engine.Engine", engineFile, "describeTree", request);
	}

	/** The key computed from the files, nothing kept in memory. */
	private String keyOfTheFiles() throws Exception {
		FlowEngineBridge.clearCaches();
		return key();
	}

	private Path markers() {
		return configuration.resolve("cluster-sync/projects");
	}

	/** A marker as another instance writes it. */
	private void markerOfAnotherInstance(String file, String kind, String operation) throws Exception {
		var properties = new Properties();
		properties.setProperty("eventId", UUID.randomUUID().toString());
		properties.setProperty("updatedAt", Long.toString(System.currentTimeMillis()));
		properties.setProperty("instanceId", "another-instance");
		properties.setProperty("kind", kind);
		properties.setProperty("target", "app");
		if (operation != null) {
			properties.setProperty("operation", operation);
		}
		Files.createDirectories(markers());
		try (var out = Files.newOutputStream(markers().resolve(file))) {
			properties.store(out, null);
		}
	}

	private void call(String method) throws Exception {
		var poll = SharedWorkspaceSyncManager.class.getDeclaredMethod(method);
		poll.setAccessible(true);
		poll.invoke(manager);
	}

	private Properties awaitMarker(String file, long notBefore) throws Exception {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			var path = markers().resolve(file);
			if (Files.isRegularFile(path)) {
				var properties = new Properties();
				try (var in = Files.newInputStream(path)) {
					properties.load(in);
				}
				if (Long.parseLong(properties.getProperty("updatedAt", "0")) >= notBefore) {
					return properties;
				}
			}
			Thread.sleep(20);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Project> loaded(DatabaseObjectsManager manager) {
		try {
			var field = DatabaseObjectsManager.class.getDeclaredField("projects");
			field.setAccessible(true);
			return (Map<String, Project>) field.get(manager);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
		var field = type.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	public void aProjectReloadedByAnotherInstanceMissesAtItsNextCall() throws Exception {
		var before = key();
		assertNotNull(before);
		changedElsewhere("saved elsewhere");
		assertEquals("not seen yet: within the delay of the changes made outside the engines", before, key());

		markerOfAnotherInstance("app.marker", "project", "reload");
		call("pollMarkers");
		assertEquals("loaded again", 1, reloads.get());
		var walks = FlowAuthoringKeys.statistics.walks.sum();
		var after = key();
		assertNotEquals("unloaded: its walk forgotten", before, after);
		assertEquals("walked again", walks + 1, FlowAuthoringKeys.statistics.walks.sum());
		assertEquals(keyOfTheFiles(), after);
	}

	@Test
	public void filesChangedByAnotherInstanceMissAtTheNextCallWithoutAReload() throws Exception {
		var before = key();
		changedElsewhere("written elsewhere");
		assertEquals(before, key());

		markerOfAnotherInstance("app.files.marker", "project-files", null);
		call("pollMarkers");
		assertEquals("not loaded again", 0, reloads.get());
		assertSame("still the loaded project", model, Engine.theApp.databaseObjectsManager.getLoadedProjectByName("app"));
		var after = key();
		assertNotEquals("its data caches invalidated", before, after);
		assertEquals(keyOfTheFiles(), after);

		// applied once: the same marker polled again changes nothing
		var revision = FlowEngineBridge.metadataSourceRevision(app.getPath());
		call("pollMarkers");
		assertEquals(revision, FlowEngineBridge.metadataSourceRevision(app.getPath()));
	}

	@Test
	public void theFilesWrittenHereAreToldOnceAfterTheirChangesAndNotAppliedHere() throws Exception {
		var before = key();
		var started = System.currentTimeMillis();
		for (int i = 0; i < 20; i++) {
			SharedWorkspaceSyncManager.markProjectFilesChanged("app");
		}
		var told = awaitMarker("app.files.marker", started);
		assertNotNull("told", told);
		assertEquals("project-files", told.getProperty("kind"));
		assertEquals("app", told.getProperty("target"));
		assertTrue("written after the changes it tells", Long.parseLong(told.getProperty("updatedAt")) >= started);
		var eventId = told.getProperty("eventId");
		Thread.sleep(300);
		assertEquals("the changes of a moment told by one marker", eventId, awaitMarker("app.files.marker", started).getProperty("eventId"));

		// a marker of this instance: not applied to itself
		call("pollMarkers");
		assertEquals(before, key());

		// the same marker read by another instance
		changedElsewhere("written here");
		var other = new Properties();
		other.putAll(told);
		other.setProperty("instanceId", "another-instance");
		try (var out = Files.newOutputStream(markers().resolve("app.files.marker"))) {
			other.store(out, null);
		}
		call("pollMarkers");
		assertNotEquals(before, key());
	}

	@Test
	public void aSourceTheProviderWritesIsToldButNotAWorkingCopy() throws Exception {
		var draft = new File(app, "_flow/blocks/draft.block.js");
		owner.setSource(draft.getPath(), "draft");
		var started = System.currentTimeMillis();
		FlowEngineBridge.notifySourceMutation(app.getPath(), draft.getPath());
		Thread.sleep(400);
		assertFalse("a working copy: of this instance only", Files.exists(markers().resolve("app.files.marker")));

		var written = new File(app, "_flow/blocks/written.block.js");
		Files.writeString(written.toPath(), "written by the provider");
		FlowEngineBridge.notifySourceMutation(app.getPath(), written.getPath());
		assertNotNull("a file written: told", awaitMarker("app.files.marker", started));
	}

	@Test
	public void theSourcesOfAProjectWrittenFromTheStudioAreToldWhenTheyAreRead() throws Exception {
		var started = System.currentTimeMillis();
		FlowEngineBridge.sourceFilesWritten(model, List.of(new File(app, "DisplayObjects/image.png")));
		Thread.sleep(400);
		assertFalse("a file neither the keys nor the runtimes read", Files.exists(markers().resolve("app.files.marker")));
		var before = key();
		changedElsewhere("uploaded");
		FlowEngineBridge.sourceFilesWritten(model, List.of(new File(app, "_flow/blocks/page.block.js")));
		assertNotNull(awaitMarker("app.files.marker", started));
		assertNotEquals("seen here at once", before, key());
	}
}
