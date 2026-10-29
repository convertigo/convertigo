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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The persistent keys without walking the files at each call: computed once from the files, then from memory without
 * reading any file, each project walked once for every key showing it and for concurrent callers; a change made
 * through the engine changes them at once; a change made outside it is seen on demand, the first call after the delay
 * answered from memory while one walk of its scope is done in the background for all callers, nothing being read while
 * nobody asks, and a state older than the maximum never served. The key computed from memory is the key computed from
 * the files.
 */
public class FlowAuthoringKeyReuseTest {
	private Path workspace;
	private File provider, engineFile, app, library, base, other, builder;

	@Before
	public void create() throws Exception {
		com.twinsoft.convertigo.engine.Engine.logEngine = org.apache.log4j.Logger.getLogger("authoring-key-reuse-test");
		AuthoringKeyClock.awaitVerifications();
		workspace = Files.createTempDirectory("authoring-key-reuse-");
		provider = project("lib_flow_engine", "", "version: 1\n");
		Files.writeString(new File(provider, "_flow/Engine.js").toPath(), "({})");
		Files.createDirectories(new File(provider, "_flow/modules").toPath());
		Files.writeString(new File(provider, "_flow/modules/tree.js").toPath(), "1");
		engineFile = new File(provider, "_flow/Engine.js");
		base = project("lib_base", "", "version: 1\n");
		library = project("lib_mid", "  ↓ref [references.ProjectSchemaReference]: \n    projectName: lib_base\n", "version: 1\n");
		app = project("app", "  ↓ref [references.ProjectSchemaReference]: \n    projectName: lib_mid\n",
				"version: 1\nconfig:\n  frontbuilder:\n    svelte:\n      target: svelte5\n");
		Files.writeString(new File(app, "_flow/blocks/page.block.js").toPath(), "page");
		other = project("unrelated", "", "version: 1\n");
		builder = project("lib_flow_frontbuilder_svelte", "", "version: 1\n");
		Files.createDirectories(new File(builder, "_flow/frontbuilder/svelte/node_modules").toPath());
		Files.writeString(new File(builder, "_flow/frontbuilder/svelte/node_modules/.package-lock.json").toPath(), "{\"v\":1}");
		age(workspace.toFile());
		FlowEngineBridge.clearCaches();
	}

	@After
	public void remove() throws Exception {
		AuthoringKeyClock.awaitVerifications();
		FlowEngineBridge.clearCaches();
		FileUtils.deleteDirectory(workspace.toFile());
	}

	private File project(String name, String references, String engine) throws Exception {
		var root = workspace.resolve(name).toFile();
		Files.createDirectories(new File(root, "_flow/blocks").toPath());
		Files.createDirectories(new File(root, "_c8oProject").toPath());
		Files.writeString(new File(root, "c8oProject.yaml").toPath(), "↓" + name + " [core.Project]: \n" + references);
		Files.writeString(new File(root, "_flow/engine.yaml").toPath(), engine);
		Files.writeString(new File(root, "_c8oProject/flowEngine.yaml").toPath(), "comment: x\n");
		return root;
	}

	/** Dates the files of a folder an hour ago, as files written before the walks, even those moved back by the test. */
	private static void age(File folder) throws Exception {
		var then = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
		try (var files = Files.walk(folder.toPath())) {
			for (var file : (Iterable<Path>) files::iterator) {
				Files.setLastModifiedTime(file, then);
			}
		}
	}

	/** Changes a file as an editor of an earlier hour: its date says it is not recent. */
	private static void write(File file, String text) throws Exception {
		Files.createDirectories(file.getParentFile().toPath());
		Files.writeString(file.toPath(), text);
		Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));
	}

	private JSONObject request(File project) throws Exception {
		var engine = Files.readString(new File(project, "_flow/engine.yaml").toPath());
		return new JSONObject().put("engineQName", "lib_flow_engine.Engine").put("target", "engine")
				.put("projectDir", project.getPath()).put("engineSource", engine)
				.put("frontendSourceDrafts", new JSONObject()).put("sourceRemovals", new JSONArray())
				.put("tagContext", new JSONObject().put("project", project.getName()).put("revision", "abc:3").put("tags", new JSONObject()));
	}

	private JSONObject flowRequest(File project) throws Exception {
		return request(project).put("target", "flow").put("sourceFile", new File(project, "_flow/blocks/page.block.js").getPath());
	}

	private String key(String method, JSONObject request) {
		return FlowEngineBridge.persistentResponseKey("lib_flow_engine.Engine", engineFile, method, request);
	}

	/** The key computed from the files themselves, as the key was computed before it was kept in memory. */
	private String reference(String method, JSONObject request) throws Exception {
		var text = new StringBuilder().append(FlowEngineBridge.PERSISTENT_KEY_FORMAT).append('\n').append(FlowEngineBridge.engineStamp())
				.append('\n').append(FlowEngineBridge.environmentStamp()).append('\n').append("lib_flow_engine.Engine\n").append(method).append('\n');
		for (var root : FlowEngineBridge.persistentDependencyRoots(engineFile, request)) {
			if (!new File(root, "c8oProject.yaml").isFile()) {
				text.append("missing:").append(root.getCanonicalPath()).append('\n');
				continue;
			}
			var walk = new FlowSourceFingerprint(root).summarizing("node_modules", ".package-lock.json").header("authoring")
					.entry("c8oProject.yaml").entry("_c8oProject").entry(FlowSourceLayout.current().root()).entry("libs");
			text.append(walk.text());
		}
		for (var label : List.of("sourceFile", "sourcePath")) {
			var path = request.optString(label, "");
			if (!path.isBlank()) {
				var file = new File(path);
				text.append(label).append(":f:").append(file.getCanonicalPath())
						.append(file.isFile() ? ":" + file.lastModified() + ":" + file.length() : ":missing").append('\n');
			}
		}
		text.append(FlowEngineBridge.canonicalJson(FlowEngineBridge.persistentRequest(request)));
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));
	}

	private static long walks() {
		return FlowAuthoringKeys.statistics.walks.sum();
	}

	private static long resolutions() {
		return FlowAuthoringKeys.statistics.resolutions.sum();
	}

	@Test
	public void aKeyIsComputedOnceFromTheFilesThenFromMemoryWithoutReadingAnyFile() throws Exception {
		var walks = walks();
		var tree = key("describeTree", request(app));
		assertNotNull(tree);
		assertEquals("the key of the files", reference("describeTree", request(app)), tree);
		assertEquals("the provider, the project, its references and the frontend builder: each walked once", walks + 5, walks());

		var resolutions = resolutions();
		var reuses = FlowAuthoringKeys.statistics.reuses.sum();
		var palette = key("authoringPalette", flowRequest(app));
		var editor = key("propertyEditor", flowRequest(app));
		assertEquals(reference("authoringPalette", flowRequest(app)), palette);
		assertEquals(reference("propertyEditor", flowRequest(app)), editor);
		for (int i = 0; i < 20; i++) {
			assertEquals(tree, key("describeTree", request(app)));
			assertEquals(palette, key("authoringPalette", flowRequest(app)));
		}
		assertEquals("the tree, the palette and the property editor: no other walk", walks + 5, walks());
		assertEquals("served from memory", resolutions, resolutions());
		assertEquals(reuses + 42, FlowAuthoringKeys.statistics.reuses.sum());

		// nothing is read: the folders moved away, the keys are those of the state verified a moment ago
		var treeRequest = request(app);
		var flowRequest = flowRequest(app);
		var moved = workspace.resolveSibling(workspace.getFileName() + "-moved");
		Files.move(workspace, moved);
		try {
			assertEquals(tree, key("describeTree", new JSONObject(treeRequest.toString())));
			assertEquals(palette, key("authoringPalette", new JSONObject(flowRequest.toString())));
			assertEquals(editor, key("propertyEditor", new JSONObject(flowRequest.toString())));
			// but never from a state older than the maximum
			AuthoringKeyClock.elapse(FlowAuthoringKeys.maxAge());
			assertNotEquals("the projects are missing", tree, key("describeTree", new JSONObject(treeRequest.toString())));
		} finally {
			Files.move(moved, workspace);
		}
		FlowEngineBridge.clearCaches();
		assertEquals("the same files: the same key", tree, key("describeTree", request(app)));
	}

	@Test
	public void aChangeMadeThroughTheEngineChangesTheKeyAtOnce() throws Exception {
		var before = key("describeTree", request(app));
		var keys = new java.util.HashSet<String>(List.of(before));
		// a source saved through the engine (a mutation of the provider, a source of the Studio)
		write(new File(app, "_flow/blocks/new.block.js"), "x");
		FlowEngineBridge.invalidateProjectDataCaches(app.getPath());
		var after = key("describeTree", request(app));
		assertTrue("a file of the project", keys.add(after));
		assertEquals(reference("describeTree", request(app)), after);

		// a referenced project, through another one
		write(new File(base, "_flow/types/t.type.yaml"), "x");
		FlowEngineBridge.invalidateProjectDataCaches(base.getPath());
		assertTrue("a project referenced through another one", keys.add(key("describeTree", request(app))));

		// the beans of the project saved, the files of the project written by the Studio
		write(new File(app, "_c8oProject/flowEngine.yaml"), "comment: y\n");
		FlowEngineBridge.projectFilesWritten(app);
		assertTrue("its beans", keys.add(key("describeTree", request(app))));
		write(new File(provider, "_flow/modules/tree.js"), "22");
		FlowEngineBridge.projectFilesWritten(provider);
		assertTrue("the provider", keys.add(key("describeTree", request(app))));

		// every data cache
		write(new File(builder, "_flow/frontbuilder/svelte/node_modules/.package-lock.json"), "{\"v\":2}");
		FlowEngineBridge.invalidateDataCaches();
		var last = key("describeTree", request(app));
		assertTrue("the packages of the frontend builder", keys.add(last));
		assertEquals(reference("describeTree", request(app)), last);

		// the file a request names
		var flow = key("authoringPalette", flowRequest(app));
		write(new File(app, "_flow/blocks/page.block.js"), "page 2");
		FlowEngineBridge.invalidateProjectDataCaches(app.getPath());
		assertNotEquals("the file of the request", flow, key("authoringPalette", flowRequest(app)));
		assertEquals(reference("authoringPalette", flowRequest(app)), key("authoringPalette", flowRequest(app)));
	}

	@Test
	public void aChangeMadeOutsideTheEngineIsSeenOnDemandWithoutWaiting() throws Exception {
		var before = key("describeTree", request(app));
		write(new File(library, "_flow/blocks/mid.block.js"), "x");
		assertEquals("within the delay: the state verified a moment ago", before, key("describeTree", request(app)));

		// past the delay: answered from memory, one walk of its scope in the background, which the next calls use
		AuthoringKeyClock.elapse(FlowAuthoringKeys.recheckAge());
		var resolutions = resolutions();
		var walks = walks();
		assertEquals(before, key("describeTree", request(app)));
		assertEquals("not waiting for the walks", resolutions, resolutions());
		AuthoringKeyClock.awaitVerifications();
		var after = key("describeTree", request(app));
		assertNotEquals("the change seen", before, after);
		assertEquals(reference("describeTree", request(app)), after);
		assertEquals("from memory", resolutions, resolutions());
		assertEquals("each project walked once again", walks + 5, walks());

		// never served once older than the maximum: its callers wait for the walks
		write(new File(base, "_flow/blocks/base.block.js"), "x");
		AuthoringKeyClock.elapse(FlowAuthoringKeys.maxAge());
		var waited = key("describeTree", request(app));
		assertNotEquals(after, waited);
		assertEquals(reference("describeTree", request(app)), waited);
		assertEquals(resolutions + 1, resolutions());

		// a referenced project appearing outside the engine
		write(new File(base, "c8oProject.yaml"), "↓lib_base [core.Project]: \n  ↓ref [references.ProjectSchemaReference]: \n    projectName: lib_late\n");
		AuthoringKeyClock.elapse(FlowAuthoringKeys.maxAge());
		var referencing = key("describeTree", request(app));
		assertEquals(reference("describeTree", request(app)), referencing);
		var late = project("lib_late", "", "version: 1\n");
		age(late);
		AuthoringKeyClock.elapse(FlowAuthoringKeys.maxAge());
		var appeared = key("describeTree", request(app));
		assertNotEquals("the missing project appeared", referencing, appeared);
		assertEquals(reference("describeTree", request(app)), appeared);
	}

	@Test
	public void nothingIsReadWhileNobodyAsksAndOneWalkVerifiesForEveryCaller() throws Exception {
		var before = key("describeTree", request(app));
		var file = key("authoringPalette", flowRequest(app));
		write(new File(provider, "_flow/modules/tree.js"), "33");
		write(new File(app, "_flow/blocks/page.block.js"), "page 3");

		// past the delay, nobody asking: nothing walked
		AuthoringKeyClock.elapse(FlowAuthoringKeys.recheckAge());
		var walks = walks();
		var refreshes = FlowAuthoringKeys.statistics.refreshes.sum();
		Thread.sleep(300);
		assertEquals(walks, walks());
		assertEquals(refreshes, FlowAuthoringKeys.statistics.refreshes.sum());

		// callers at the same time: answered from memory, one verification of the scope for all of them
		var resolutions = resolutions();
		var callers = 12;
		var barrier = new CyclicBarrier(callers);
		var pool = Executors.newFixedThreadPool(callers);
		try {
			var tasks = new ArrayList<Future<String>>();
			for (int i = 0; i < callers; i++) {
				tasks.add(pool.submit(() -> {
					barrier.await(20, TimeUnit.SECONDS);
					return key("describeTree", request(app));
				}));
			}
			var keys = new java.util.HashSet<String>();
			for (var task : tasks) {
				keys.add(task.get(30, TimeUnit.SECONDS));
			}
			AuthoringKeyClock.awaitVerifications();
			keys.remove(before);
			keys.remove(reference("describeTree", request(app)));
			assertTrue("the state it had, or the one verified meanwhile: " + keys, keys.isEmpty());
		} finally {
			pool.shutdownNow();
		}
		assertEquals("none waited", resolutions, resolutions());
		assertEquals("one verification of the scope", refreshes + 1, FlowAuthoringKeys.statistics.refreshes.sum());
		assertEquals("each of its projects walked once", walks + 5, walks());
		var after = key("describeTree", request(app));
		assertNotEquals(before, after);
		assertEquals(reference("describeTree", request(app)), after);

		// the file of a request, verified the same way: its line of before, then read again in the background
		var stale = key("authoringPalette", flowRequest(app));
		assertNotEquals(file, stale);
		assertNotEquals("the file as read before", reference("authoringPalette", flowRequest(app)), stale);
		AuthoringKeyClock.awaitVerifications();
		assertEquals(reference("authoringPalette", flowRequest(app)), key("authoringPalette", flowRequest(app)));
		assertEquals(resolutions, resolutions());

		// no thread left once the verifications are done
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
		while (AuthoringKeyClock.verificationThreads() > 0 && System.nanoTime() < deadline) {
			Thread.sleep(100);
		}
		assertEquals(0, AuthoringKeyClock.verificationThreads());
	}

	@Test
	public void aRootIsWalkedOnceForConcurrentKeys() throws Exception {
		// a provider long enough to walk for the callers to meet
		for (int i = 0; i < 1500; i++) {
			Files.writeString(new File(provider, "_flow/modules/m" + i + ".js").toPath(), "var m = " + i + ";");
		}
		age(provider);
		var callers = 24;
		var pool = Executors.newFixedThreadPool(callers);
		try {
			for (int round = 0; round < 8; round++) {
				walkedOnceByConcurrentKeys(pool, callers);
			}
		} finally {
			pool.shutdownNow();
		}
	}

	/** A restart, then keys asked at the same time by several callers, for projects sharing projects. */
	private void walkedOnceByConcurrentKeys(java.util.concurrent.ExecutorService pool, int callers) throws Exception {
		FlowEngineBridge.clearCaches();
		var walks = walks();
		var barrier = new CyclicBarrier(callers);
		{
			var tasks = new ArrayList<Future<String[]>>();
			for (int i = 0; i < callers; i++) {
				var which = i % 4;
				tasks.add(pool.submit((Callable<String[]>) () -> {
					barrier.await(20, TimeUnit.SECONDS);
					return switch (which) {
					case 0 -> new String[] { "app", key("describeTree", request(app)) };
					case 1 -> new String[] { "palette", key("authoringPalette", flowRequest(app)) };
					case 2 -> new String[] { "mid", key("describeTree", request(library)) };
					default -> new String[] { "other", key("describeTree", request(other)) };
					};
				}));
			}
			var seen = new java.util.HashMap<String, String>();
			for (var task : tasks) {
				var result = task.get(60, TimeUnit.SECONDS);
				assertNotNull(result[1]);
				var known = seen.putIfAbsent(result[0], result[1]);
				assertTrue("the same key for the same request", known == null || known.equals(result[1]));
			}
			assertEquals("the provider, app, lib_mid, lib_base, the frontend builder, unrelated: once each", walks + 6, walks());
			assertEquals(reference("describeTree", request(app)), seen.get("app"));
			assertEquals(reference("describeTree", request(other)), seen.get("other"));
		}
	}

	@Test
	public void aRecentFileGivesNoKeyUntilItsProjectIsWalkedAgainInTheBackground() throws Exception {
		var block = new File(library, "_flow/blocks/recent.block.js");
		Files.writeString(block.toPath(), "just written");
		var settles = System.currentTimeMillis() + 1000;
		Files.setLastModifiedTime(block.toPath(), FileTime.fromMillis(settles - FlowEngineBridge.RECENT_FILE_MS));
		FlowEngineBridge.invalidateProjectDataCaches(library.getPath());
		assertNull("a file written just now may change again within the resolution of its date", key("describeTree", request(app)));
		while (System.currentTimeMillis() <= settles) {
			Thread.sleep(50);
		}
		assertNull("still the walk that saw it recent, walked again in the background", key("describeTree", request(app)));
		AuthoringKeyClock.awaitVerifications();
		var settled = key("describeTree", request(app));
		assertNotNull(settled);
		assertEquals(reference("describeTree", request(app)), settled);
	}
}
