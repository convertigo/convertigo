/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * What the actual provider (FLOW_ENGINE_RESOURCE_ROOT, lib_flow_engine 0.1.120 or later) computes from the content of
 * the sources of a project, with runtimes of their own scopes as the bridge creates them: another runtime, and the
 * shared runtime of the project, find it computed by the first, and answer what the first answered.
 */
public class FlowSourceMemoProviderTest {
	private static Method method(Class<?> c, String name, Class<?>... types) throws Exception {
		var method = c.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method;
	}

	private static Object field(Object object, String name) throws Exception {
		return method(object.getClass(), name).invoke(object);
	}

	private Path project;
	private File file;
	private Object reference, source;
	private Method create, acquireShared, invoke;

	private static final String FLOW = "function Greet({ input, result }) {\n  result.text = proof.wrap({ name: input.name })\n  return result\n}\n";

	@Before
	public void create() throws Exception {
		var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		org.junit.Assume.assumeNotNull(root);
		Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("source-memo-provider-test");
		EnginePropertiesManager.initProperties();
		FlowEngineBridge.clearCaches();
		project = Files.createTempDirectory("source-memo-provider-");
		file = new File(root, "Engine.js");
		var bridge = FlowEngineBridge.class;
		var ref = Class.forName(bridge.getName() + "$EngineRef");
		var src = Class.forName(bridge.getName() + "$CachedEngineSource");
		var lookup = Class.forName(bridge.getName() + "$CachedEngineRuntimeLookup");
		reference = method(ref, "parse", String.class).invoke(null, "lib_flow_engine.Engine");
		var constructor = src.getDeclaredConstructor(File.class, String.class, long.class, long.class);
		constructor.setAccessible(true);
		source = constructor.newInstance(file, Files.readString(file.toPath()), file.lastModified(), file.length());
		create = method(bridge, "createEngineRuntime", ref, File.class, src, Context.class, long.class);
		acquireShared = method(bridge, "sharedEngineRuntime", ref, File.class, src, Context.class, JSONObject.class,
				com.twinsoft.convertigo.engine.Context.class);
		invoke = method(bridge, "invokePrepared", ref, File.class, src, String.class, JSONObject.class,
				com.twinsoft.convertigo.engine.Context.class, Context.class, Scriptable.class, Scriptable.class, lookup);
		var blocks = project.resolve("_flow/blocks/proof");
		Files.createDirectories(blocks);
		Files.writeString(blocks.resolve("hello.block.js"), "const _meta={sourceVersion:2,runtime:'rhino',properties:{name:{type:'string',kind:'value'}},"
				+ "outputs:{out:{type:'string'}}};\n(function(){return {run:function(ctx,node){return 'Hello '+ctx.input({value:ctx.props(node).name});}};})()");
		Files.writeString(blocks.resolve("wrap.block.js"), "const _meta={sourceVersion:2,properties:{name:{type:'string',kind:'value'}},outputs:{out:{type:'string'}}}\n"
				+ "function proof_wrap({ input, result }) {\n  local.text = proof.hello({ name: input.name })\n  return local.text\n}\n");
		Files.createDirectories(project.resolve("_flow/flows"));
		Files.writeString(project.resolve("_flow/flows/Greet.flow.js"), FLOW);
	}

	@After
	public void release() throws Exception {
		if (project != null) {
			FlowEngineBridge.clearCaches();
			org.apache.commons.io.FileUtils.deleteDirectory(project.toFile());
		}
	}

	/** A runtime of its own scope, as the pool of the bridge creates them. */
	private Object[] runtime() throws Exception {
		try (var cx = Context.enter()) {
			cx.setLanguageVersion(Context.VERSION_ES6);
			var created = create.invoke(null, reference, file, source, cx, FlowEngineBridge.cacheGeneration());
			return new Object[] { field(created, "scope"), field(created, "engineObject") };
		}
	}

	private Object call(Object[] runtime, String name, JSONObject request) throws Exception {
		request.put("engineQName", "lib_flow_engine.Engine").put("projectDir", project.toString());
		try (var cx = Context.enter()) {
			cx.setLanguageVersion(Context.VERSION_ES6);
			if (runtime == null) {
				var lookup = acquireShared.invoke(null, reference, file, source, cx, request, null);
				assertNotNull("No shared runtime for the provider", lookup);
				var shared = field(lookup, "runtime");
				return invoke.invoke(new FlowEngineBridge(), reference, file, source, name, request, null, cx,
						field(shared, "scope"), field(shared, "engineObject"), lookup);
			}
			return invoke.invoke(new FlowEngineBridge(), reference, file, source, name, request, null, cx, runtime[0], runtime[1], null);
		}
	}

	private String tree(Object[] runtime) throws Exception {
		var answer = (JSONObject) call(runtime, "describeTree", new JSONObject().put("flowQName", "Proof.FlowEngine").put("flowSource", "")
				.put("frontendSourceDrafts", new JSONObject()).put("sourceRemovals", new JSONArray()).put("target", "engine")
				.put("engineSource", "").put("includeFlowCatalog", true).put("flowCatalogOrigin", "project")
				.put("includeCatalogLibraries", false).put("includeBindings", false));
		assertTrue(answer.toString(), answer.optBoolean("ok", true) && !answer.has("error"));
		answer.remove("profile");
		return answer.toString();
	}

	/** @return the builds of a kind of the source memo by a runtime */
	private int builds(Object[] runtime, String kind) throws Exception {
		var answer = call(runtime, "cacheInfo", new JSONObject());
		var info = answer instanceof JSONObject json ? json : new JSONObject(String.valueOf(answer));
		var memo = info.getJSONObject("caches").optJSONObject("sourceMemo");
		assertTrue("The provider of FLOW_ENGINE_RESOURCE_ROOT keeps no source memo for all its runtimes (lib_flow_engine 0.1.120 or later): "
				+ memo, memo != null && memo.optBoolean("shared"));
		var stats = memo.getJSONObject("kinds").optJSONObject(kind);
		return stats == null ? 0 : stats.getInt("builds");
	}

	private static long memoBuilds() throws Exception {
		var field = FlowEngineBridge.class.getDeclaredField("sourceMemo");
		field.setAccessible(true);
		return ((FlowMetadataCache) field.get(null)).builds();
	}

	@Test
	public void anotherRuntimeAndTheSharedRuntimeFindTheSourcesComputed() throws Exception {
		var first = runtime();
		var expected = tree(first);
		assertTrue("The first runtime compiled no block", builds(first, "block-descriptor") >= 1);
		assertTrue("The first runtime read no block header", builds(first, "block-meta") >= 2);
		var built = memoBuilds();
		var second = runtime();
		assertEquals("Another runtime answered otherwise", expected, tree(second));
		assertEquals("Another runtime compiled a block again", 0, builds(second, "block-descriptor"));
		assertEquals("Another runtime read a block header again", 0, builds(second, "block-meta"));
		assertEquals(built, memoBuilds());
		var answer = (java.util.Map<?, ?>) call(null, "run", new JSONObject().put("flowQName", "Proof.Greet").put("flowName", "Greet")
				.put("flowSource", FLOW).put("includeTrace", false).put("input", new JSONObject().put("name", "Ada")));
		assertEquals(answer.toString(), Boolean.TRUE, answer.get("ok"));
		assertEquals("Hello Ada", ((java.util.Map<?, ?>) answer.get("result")).get("text"));
		var plans = (JSONObject) call(null, "preload", new JSONObject().put("flowQName", "Proof.Greet").put("flowSource", "").put("project", "Proof")
				.put("flows", new JSONArray().put(new JSONObject().put("flowQName", "Proof.Greet").put("flowName", "Greet").put("flowSource", FLOW))));
		assertTrue(plans.toString(), plans.optBoolean("ok"));
		// the shared runtime parses the contract of every block of its catalog, read for the first time but the two above
		assertEquals("The shared runtime compiled a block again", 0, builds(null, "block-descriptor"));
		var third = runtime();
		assertEquals("A third runtime answered otherwise", expected, tree(third));
		assertEquals(0, builds(third, "block-descriptor"));
		assertEquals(0, builds(third, "block-meta"));
	}
}
