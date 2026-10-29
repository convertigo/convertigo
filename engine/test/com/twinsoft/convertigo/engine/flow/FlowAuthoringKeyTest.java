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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The key of an authoring response on disk: stable across restarts while nothing it depends on changes, another key
 * for any change of the files of the provider, of the project, of the projects it references directly or not, of the
 * frontend builder it resolves by its name, of the request; the same key for the changes of what it does not depend on
 * (another project, installed packages other than their lock file, the tag generation of this run). No key for
 * working copies, nor for files too recent for their dates to tell their next change.
 */
public class FlowAuthoringKeyTest {
	private Path workspace;
	private File provider, engineFile, app, library, base, other, builder;

	@Before
	public void create() throws Exception {
		com.twinsoft.convertigo.engine.Engine.logEngine = org.apache.log4j.Logger.getLogger("authoring-key-test");
		workspace = Files.createTempDirectory("authoring-key-");
		provider = project("lib_flow_engine", "", "version: 1\n");
		Files.writeString(new File(provider, "_flow/Engine.js").toPath(), "({})");
		Files.createDirectories(new File(provider, "_flow/modules").toPath());
		Files.writeString(new File(provider, "_flow/modules/tree.js").toPath(), "1");
		engineFile = new File(provider, "_flow/Engine.js");
		base = project("lib_base", "", "version: 1\n");
		library = project("lib_mid", "  ↓ref [references.ProjectSchemaReference]: \n    projectName: lib_base\n", "version: 1\n");
		app = project("app", "  ↓ref [references.ProjectSchemaReference]: \n    projectName: lib_mid=https://example.invalid/lib_mid.zip\n",
				"version: 1\nconfig:\n  frontbuilder:\n    svelte:\n      target: svelte5\n");
		other = project("unrelated", "", "version: 1\n");
		builder = project("lib_flow_frontbuilder_svelte", "", "version: 1\n");
		Files.createDirectories(new File(builder, "_flow/frontbuilder/svelte/node_modules/svelte").toPath());
		Files.writeString(new File(builder, "_flow/frontbuilder/svelte/node_modules/.package-lock.json").toPath(), "{\"v\":1}");
		Files.writeString(new File(builder, "_flow/frontbuilder/svelte/node_modules/svelte/index.js").toPath(), "1");
		age(workspace.toFile());
		FlowEngineBridge.clearCaches();
	}

	@After
	public void remove() throws Exception {
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

	/** Dates the files of a folder a minute ago, as files written before the walk. */
	private static void age(File folder) throws Exception {
		var then = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
		try (var files = Files.walk(folder.toPath())) {
			for (var file : (Iterable<Path>) files::iterator) {
				Files.setLastModifiedTime(file, then);
			}
		}
	}

	/** Changes a file as an editor of an earlier minute: its date says it is not recent. */
	private static void write(File file, String text) throws Exception {
		Files.createDirectories(file.getParentFile().toPath());
		Files.writeString(file.toPath(), text);
		Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30)));
	}

	private JSONObject request() throws Exception {
		return new JSONObject().put("engineQName", "lib_flow_engine.Engine").put("target", "engine")
				.put("projectDir", app.getPath()).put("engineSource", "version: 1\n")
				.put("frontendSourceDrafts", new JSONObject()).put("sourceRemovals", new JSONArray())
				.put("tagContext", new JSONObject().put("project", "app").put("revision", "abc:3").put("tags", new JSONObject()));
	}

	/** The key after a restart: nothing kept in memory. */
	private String key(JSONObject request) {
		FlowEngineBridge.clearCaches();
		return FlowEngineBridge.persistentResponseKey("lib_flow_engine.Engine", engineFile, "describeTree", request);
	}

	@Test
	public void theDependenciesAreTheProviderTheReferencesAndTheFrontendBuilder() throws Exception {
		var roots = FlowEngineBridge.persistentDependencyRoots(engineFile, request());
		var names = roots.stream().map(File::getName).sorted().toList();
		assertEquals(java.util.List.of("app", "lib_base", "lib_flow_engine", "lib_flow_frontbuilder_svelte", "lib_mid"), names);
	}

	@Test
	public void theKeyIsStableAcrossRestarts() throws Exception {
		var first = key(request());
		assertNotNull(first);
		assertEquals(first, key(request()));
		// the generation of the tags of this run is not their content
		assertEquals(first, key(request().put("tagContext", request().getJSONObject("tagContext").put("revision", "abc:9"))));
		// another order of the same request
		var reordered = new JSONObject();
		var original = request();
		var names = new java.util.ArrayList<String>();
		original.keys().forEachRemaining(name -> names.add(String.valueOf(name)));
		java.util.Collections.reverse(names);
		for (var name : names) {
			reordered.put(name, original.get(name));
		}
		assertEquals(first, key(reordered));
		// what the bridge attaches after computing the key
		assertEquals(first, key(request().put("__flowBridgeDescribeTreeSnapshot", new JSONObject().put("ok", true))));
	}

	@Test
	public void anyChangeOfWhatItDependsOnIsAnotherKey() throws Exception {
		var keys = new java.util.HashSet<String>();
		assertTrue(keys.add(key(request())));
		write(new File(app, "_flow/blocks/new.block.js"), "x");
		assertTrue("a file of the project", keys.add(key(request())));
		write(new File(app, "_flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte"), "<div/>");
		assertTrue("a frontend source of the project", keys.add(key(request())));
		write(new File(app, "_c8oProject/flowEngine.yaml"), "comment: y\n");
		assertTrue("a bean of the project", keys.add(key(request())));
		write(new File(app, "c8oProject.yaml"), Files.readString(new File(app, "c8oProject.yaml").toPath()) + "  ↓x [core.Comment]: \n");
		assertTrue("the descriptor of the project", keys.add(key(request())));
		write(new File(library, "_flow/blocks/mid.block.js"), "x");
		assertTrue("a referenced project", keys.add(key(request())));
		write(new File(base, "_flow/types/t.type.yaml"), "x");
		assertTrue("a project referenced through another one", keys.add(key(request())));
		write(new File(provider, "_flow/modules/tree.js"), "2");
		assertTrue("the provider", keys.add(key(request())));
		write(new File(provider, "libs/build/src.sha256"), "1");
		assertTrue("the Java libraries of the provider", keys.add(key(request())));
		write(new File(builder, "_flow/frontbuilder/svelte/node_modules/.package-lock.json"), "{\"v\":2}");
		assertTrue("the packages installed for the frontend builder", keys.add(key(request())));
		assertTrue("the request", keys.add(key(request().put("engineSource", "version: 2\n"))));
		assertTrue("the request", keys.add(key(request().put("tagContext", request().getJSONObject("tagContext").put("revision", "abd:3")))));
		var now = key(request());
		write(new File(other, "_flow/blocks/other.block.js"), "x");
		assertEquals("another project", now, key(request()));
		write(new File(builder, "_flow/frontbuilder/svelte/node_modules/svelte/index.js"), "22");
		assertEquals("an installed package, its lock file unchanged", now, key(request()));
		Files.delete(new File(library, "c8oProject.yaml").toPath());
		assertNotEquals("a referenced project removed", now, key(request()));
	}

	@Test
	public void noKeyForWorkingCopiesNorForRecentFiles() throws Exception {
		assertNull(key(request().put("frontendSourceDrafts", new JSONObject().put("/a/b.flow.svelte", "<div/>"))));
		assertNull(key(request().put("sourceRemovals", new JSONArray().put("/a/b.flow.svelte"))));
		var block = new File(library, "_flow/blocks/recent.block.js");
		Files.writeString(block.toPath(), "just written");
		assertNull("a file written just now may change again within the resolution of its date", key(request()));
		Files.setLastModifiedTime(block.toPath(), FileTime.fromMillis(System.currentTimeMillis() - FlowEngineBridge.RECENT_FILE_MS - 1000));
		assertNotNull(key(request()));
	}

	@Test
	public void theCanonicalJsonSortsTheKeysOfObjects() throws Exception {
		var a = new JSONObject().put("b", 1).put("a", new JSONObject().put("y", "é\"").put("x", new JSONArray().put(true).put(JSONObject.NULL)));
		var b = new JSONObject().put("a", new JSONObject().put("x", new JSONArray().put(true).put(JSONObject.NULL)).put("y", "é\"")).put("b", 1);
		assertEquals(FlowEngineBridge.canonicalJson(a), FlowEngineBridge.canonicalJson(b));
		assertEquals("{\"a\":{\"x\":[true,null],\"y\":\"é\\\"\"},\"b\":1}", FlowEngineBridge.canonicalJson(a));
	}

	@Test
	public void aWalkSummarizesInstalledPackagesAndTellsItsNewestFile() throws Exception {
		var root = new File(builder, "_flow");
		var walk = new FlowSourceFingerprint(builder).summarizing("node_modules", ".package-lock.json");
		walk.entry("_flow");
		assertTrue(walk.text().contains("f:_flow/frontbuilder/svelte/node_modules/.package-lock.json:"));
		assertTrue("the packages themselves are not walked", !walk.text().contains("svelte/index.js"));
		assertEquals(new File(root, "engine.yaml").lastModified(), walk.newest());
		write(new File(root, "blocks/newer.block.js"), "x");
		var again = new FlowSourceFingerprint(builder).summarizing("node_modules", ".package-lock.json");
		again.entry("_flow");
		assertEquals(new File(root, "blocks/newer.block.js").lastModified(), again.newest());
	}
}
