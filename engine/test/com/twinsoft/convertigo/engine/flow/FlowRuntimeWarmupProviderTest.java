/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
 * The preload of run plans with the actual provider (FLOW_ENGINE_RESOURCE_ROOT, lib_flow_engine 0.1.119 or later), in
 * the shared runtime of a stateless server: the first run finds the plan prepared, a run arriving during the preload
 * waits for its preparation instead of preparing it again, a Flow that cannot be prepared does not stop the others, and
 * nothing runs.
 */
public class FlowRuntimeWarmupProviderTest {
	private static final AtomicInteger preparations = new AtomicInteger(), runs = new AtomicInteger();
	private static volatile CountDownLatch gate;
	private static final AtomicInteger gateReached = new AtomicInteger();

	/** Called by the preparation of the proof block (its prepareNode), held while a gate is closed. */
	public static void prepared() throws InterruptedException {
		preparations.incrementAndGet();
		var current = gate;
		if (current != null) {
			gateReached.incrementAndGet();
			assertTrue("The gate of the preparation stayed closed", current.await(30, TimeUnit.SECONDS));
		}
	}

	/** Called by each run of the proof block. */
	public static int ran() {
		return runs.incrementAndGet();
	}

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
	private Method acquire, invoke;

	private static final String WARM = "function Warm({ input, result }) {\n  result.proof = proof.prepared({})\n  return result\n}\n";
	private static final String OTHER = "function Other({ input, result }) {\n  result.proof = proof.prepared({})\n  result.other = true\n  return result\n}\n";
	private static final String BROKEN = "function Broken({ input, result }) {\n  result.value = (\n}\n";

	@Before
	public void create() throws Exception {
		var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		org.junit.Assume.assumeNotNull(root);
		Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("runtime-warmup-provider-test");
		EnginePropertiesManager.initProperties();
		preparations.set(0);
		runs.set(0);
		gate = null;
		gateReached.set(0);
		project = Files.createTempDirectory("runtime-warmup-provider-");
		file = new File(root, "Engine.js");
		var bridge = FlowEngineBridge.class;
		var ref = Class.forName(bridge.getName() + "$EngineRef");
		var src = Class.forName(bridge.getName() + "$CachedEngineSource");
		var lookup = Class.forName(bridge.getName() + "$CachedEngineRuntimeLookup");
		reference = method(ref, "parse", String.class).invoke(null, "lib_flow_engine.Engine");
		var constructor = src.getDeclaredConstructor(File.class, String.class, long.class, long.class);
		constructor.setAccessible(true);
		source = constructor.newInstance(file, Files.readString(file.toPath()), file.lastModified(), file.length());
		acquire = method(bridge, "sharedEngineRuntime", ref, File.class, src, Context.class, JSONObject.class,
				com.twinsoft.convertigo.engine.Context.class);
		invoke = method(bridge, "invokePrepared", ref, File.class, src, String.class, JSONObject.class,
				com.twinsoft.convertigo.engine.Context.class, Context.class, Scriptable.class, Scriptable.class, lookup);
		var block = project.resolve("_flow/blocks/proof/prepared.block.js");
		Files.createDirectories(block.getParent());
		var probe = "Packages." + FlowRuntimeWarmupProviderTest.class.getName();
		Files.writeString(block, "const _meta={sourceVersion:2,runtime:'rhino',properties:{},outputs:{out:{type:'object'}}};\n"
				+ "(function(){return {stateless:true,prepareNode:function(){" + probe + ".prepared();"
				+ "return function(ctx){return {runs:" + probe + ".ran()};};},"
				+ "run:function(){throw Error('unprepared');}};})()");
	}

	@After
	public void release() throws Exception {
		var current = gate;
		gate = null;
		if (current != null) {
			current.countDown();
		}
		if (project != null) {
			FlowEngineBridge.clearCaches();
			org.apache.commons.io.FileUtils.deleteDirectory(project.toFile());
		}
	}

	/** A call in the shared runtime of the project, as on a stateless server. */
	private Object call(String method, JSONObject request) throws Exception {
		request.put("projectDir", project.toString());
		try (var cx = Context.enter()) {
			cx.setLanguageVersion(Context.VERSION_ES6);
			var lookup = acquire.invoke(null, reference, file, source, cx, request, null);
			var runtime = field(lookup, "runtime");
			return invoke.invoke(new FlowEngineBridge(), reference, file, source, method, request, null, cx,
					field(runtime, "scope"), field(runtime, "engineObject"), lookup);
		}
	}

	private FlowRuntimeApi api() throws Exception {
		try (var cx = Context.enter()) {
			var lookup = acquire.invoke(null, reference, file, source, cx, new JSONObject().put("projectDir", project.toString()), null);
			return (FlowRuntimeApi) field(field(lookup, "runtime"), "api");
		}
	}

	private static JSONObject flow(String name, String flowSource) throws Exception {
		return new JSONObject().put("flowQName", "Fixture." + name).put("flowName", name).put("flowSource", flowSource);
	}

	/** The request of FlowEngineBridge.preloadPlan. */
	private JSONObject preload(JSONObject... flows) throws Exception {
		var list = new JSONArray();
		for (var flow : flows) {
			list.put(flow);
		}
		var answer = (JSONObject) call("preload", new JSONObject().put("engineQName", "lib_flow_engine.Engine")
				.put("flowQName", flows[0].getString("flowQName")).put("flowSource", "").put("project", "Fixture")
				.put("flows", list));
		assertTrue("The provider of FLOW_ENGINE_RESOURCE_ROOT prepares no plan in advance (lib_flow_engine 0.1.119 or later): "
				+ answer, answer.optBoolean("ok") && answer.has("plans"));
		return answer;
	}

	/** The request of FlowEngineBridge.run, without its context. */
	private Map<?, ?> run(String name, String flowSource) throws Exception {
		var answer = (Map<?, ?>) call("run", new JSONObject().put("engineQName", "lib_flow_engine.Engine")
				.put("flowQName", "Fixture." + name).put("flowSource", flowSource).put("flowName", name)
				.put("includeTrace", false).put("input", new JSONObject()));
		assertEquals(answer.toString(), Boolean.TRUE, answer.get("ok"));
		return (Map<?, ?>) answer.get("result");
	}

	private static long statistic(FlowRuntimeApi api, String name) {
		return ((Number) api.statistics().get(name)).longValue();
	}

	@Test
	public void theFirstRunFindsThePlanPreparedAndNothingRanBefore() throws Exception {
		var answer = preload(flow("Warm", WARM));
		var plan = answer.getJSONArray("plans").getJSONObject(0);
		assertTrue(plan.toString(), plan.getBoolean("ok"));
		assertEquals("Fixture.Warm", plan.getString("flowQName"));
		assertEquals("The plan prepared its block", 1, preparations.get());
		assertEquals("Nothing runs during a preload", 0, runs.get());
		var api = api();
		var builds = statistic(api, "builds");
		var proof = (Map<?, ?>) run("Warm", WARM).get("proof");
		assertEquals(1, ((Number) proof.get("runs")).intValue());
		assertEquals("The run prepared its plan again", 1, preparations.get());
		assertEquals("The run built a plan, block or module again", builds, statistic(api, "builds"));
		// another Flow of the project, not preloaded, prepares its own plan
		run("Other", OTHER);
		assertEquals(2, preparations.get());
	}

	@Test
	public void aRunArrivingDuringThePreloadWaitsForItsPreparation() throws Exception {
		var pool = Executors.newFixedThreadPool(2);
		try {
			gate = new CountDownLatch(1);
			var preloading = pool.submit(() -> preload(flow("Warm", WARM)));
			for (long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30); gateReached.get() == 0;) {
				assertTrue("The preload did not reach the preparation of its plan", System.nanoTime() < deadline);
				Thread.sleep(5);
			}
			var running = pool.submit(() -> run("Warm", WARM));
			var api = api();
			for (long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30); statistic(api, "joins") == 0 && !waiting(api);) {
				assertTrue("The run did not wait for the preparation in progress", System.nanoTime() < deadline);
				Thread.sleep(5);
			}
			assertFalse("The run did not wait for the preparation in progress", running.isDone());
			gate.countDown();
			var plan = preloading.get(60, TimeUnit.SECONDS).getJSONArray("plans").getJSONObject(0);
			assertTrue(plan.toString(), plan.getBoolean("ok"));
			var proof = (Map<?, ?>) running.get(60, TimeUnit.SECONDS).get("proof");
			assertEquals(1, ((Number) proof.get("runs")).intValue());
			assertEquals("The plan was prepared twice", 1, preparations.get());
			assertEquals(1, gateReached.get());
			assertTrue(statistic(api, "joins") >= 1);
		} finally {
			pool.shutdownNow();
		}
	}

	private static boolean waiting(FlowRuntimeApi api) throws Exception {
		var field = FlowRuntimeApi.class.getDeclaredField("waiting");
		field.setAccessible(true);
		var waiting = (Map<?, ?>) field.get(api);
		synchronized (waiting) {
			return !waiting.isEmpty();
		}
	}

	@Test
	public void aFlowThatCannotBePreparedIsToldAndTheOthersArePrepared() throws Exception {
		var answer = preload(flow("Broken", BROKEN), flow("Warm", WARM));
		var plans = answer.getJSONArray("plans");
		var broken = plans.getJSONObject(0);
		assertFalse(broken.toString(), broken.getBoolean("ok"));
		assertEquals("Fixture.Broken", broken.getString("flowQName"));
		assertFalse(FlowRuntimeWarmup.planOutcome(new JSONObject().put("ok", true).put("plans", new JSONArray().put(broken))).isEmpty());
		assertTrue(plans.getJSONObject(1).toString(), plans.getJSONObject(1).getBoolean("ok"));
		assertEquals(1, answer.getInt("planCount"));
		assertEquals(1, answer.getInt("failedPlanCount"));
		assertEquals(1, preparations.get());
		run("Warm", WARM);
		assertEquals(1, preparations.get());
		assertEquals(1, runs.get());
	}
}
