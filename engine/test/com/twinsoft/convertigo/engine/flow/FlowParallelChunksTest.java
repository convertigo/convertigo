/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * Parallel chunk loops on real threads: the provider code of FLOW_ENGINE_RESOURCE_ROOT in the shared runtime. A probe
 * block records which threads see which objects: the run context, scopes and local of a chunk must stay on one thread,
 * while parent, shared by design, must be seen by several (the probe would see a shared object if there were one).
 */
public class FlowParallelChunksTest {
	static {
		System.setProperty("convertigo.flow.parallel_grace_ms", "500");
	}

	// probe state, read and written by the probe block from the lanes
	private static final Map<Object, Set<String>> seen = Collections.synchronizedMap(new IdentityHashMap<>());
	private static final Map<Object, String> roles = Collections.synchronizedMap(new IdentityHashMap<>());
	private static final AtomicInteger inside = new AtomicInteger(), overlap = new AtomicInteger();
	private static volatile CountDownLatch rendezvous = new CountDownLatch(2);

	public static void touch(String role, Object value) {
		seen.computeIfAbsent(value, key -> Collections.synchronizedSet(new HashSet<>())).add(Thread.currentThread().getName());
		roles.put(value, role);
	}

	private static final Map<Integer, Set<Object>> views = new java.util.concurrent.ConcurrentHashMap<>();

	/** A row view read by a lane: every lane must read the same view of a row. */
	public static void view(int index, Object view) {
		views.computeIfAbsent(index, key -> Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()))).add(view);
	}

	public static void enter() {
		overlap.accumulateAndGet(inside.incrementAndGet(), Math::max);
	}

	public static void exit() {
		inside.decrementAndGet();
	}

	/** The first two probes wait for each other: proves two lanes run at the same time, never more than 5 s. */
	public static void meet() throws InterruptedException {
		var latch = rendezvous;
		if (latch.getCount() > 0) {
			latch.countDown();
			latch.await(5, TimeUnit.SECONDS);
		}
	}

	private static Method method(Class<?> c, String name, Class<?>... types) throws Exception {
		var method = c.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method;
	}

	private static Object field(Object object, String name) throws Exception {
		return method(object.getClass(), name).invoke(object);
	}

	private static final class Harness implements AutoCloseable {
		final Path project = Files.createTempDirectory("parallel-chunks-");
		final File file;
		final Object reference, source;
		final Method acquire, invoke;

		Harness() throws Exception {
			var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
			org.junit.Assume.assumeNotNull(root);
			Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("parallel-chunks-test");
			EnginePropertiesManager.initProperties();
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
			var probe = project.resolve("_flow/blocks/proof/probe.block.js");
			Files.createDirectories(probe.getParent());
			Files.writeString(probe, "const _meta={sourceVersion:2,runtime:'rhino',properties:{index:{kind:'expression',type:'integer'}},outputs:{out:{type:'any'}}};\n"
					+ "(function(){return {run:function(ctx,node){var P=Packages.com.twinsoft.convertigo.engine.flow.FlowParallelChunksTest;"
					+ "P.enter();try{P.touch('ctx',ctx);P.touch('scopes',ctx.scopes);P.touch('local',ctx.scopes.local);P.touch('parent',ctx.scopes.parent);"
					+ "ctx.scopes.local.mark=ctx.expr(ctx.props(node).index);P.meet();"
					+ "return String(Packages.java.lang.Thread.currentThread().getName());}finally{P.exit();}}};})()");
		}

		Map<?, ?> run(String flowSource, JSONObject input) throws Exception {
			var request = new JSONObject().put("flowQName", "Fixture.Parallel").put("flowSource", flowSource)
					.put("input", input).put("projectDir", project.toString()).put("includeTrace", false);
			try (var cx = Context.enter()) {
				cx.setLanguageVersion(Context.VERSION_ES6);
				var lookup = acquire.invoke(null, reference, file, source, cx, request, null);
				var runtime = field(lookup, "runtime");
				return (Map<?, ?>) invoke.invoke(new FlowEngineBridge(), reference, file, source, "run", request, null, cx,
						field(runtime, "scope"), field(runtime, "engineObject"), lookup);
			}
		}

		@Override
		public void close() throws Exception {
			FlowEngineBridge.clearCaches();
			org.apache.commons.io.FileUtils.deleteDirectory(project.toFile());
		}
	}

	private static JSONObject items(int count) throws Exception {
		var items = new JSONArray();
		for (int i = 1; i <= count; i++) items.put(new JSONObject().put("id", i).put("score", i * 7 % 11));
		return new JSONObject().put("items", items);
	}

	private static String folded(int parallel) {
		return "const _flow={sourceVersion:2,inputs:{items:{type:\"array\"}}}\nfunction Fold({input,result}){\nlocal.factor=3\n"
				+ "local.text=forEachChunk({source:input.items,size:5,parallel:" + parallel + ",into:\"data\",$$each:function({chunk}){\n"
				+ "local.thread=proof.probe({index:chunk.index})\nlocal.ids=rows.select({rows:chunk.rows,columns:[\"id\"]})\nemit({rows:local.ids})\n"
				+ "local.i=\"{{ chunk.index }}:{{ parent.factor }}\"\nreturn local.i\n"
				+ "},$$reduce:function({acc,value}){\nif({condition:acc===null,$$then:function(){return value}})\nlocal.t=\"{{ acc }},{{ value }}\"\nreturn local.t\n}})\n"
				+ "result.text=local.text\n}";
	}

	private static Map<?, ?> result(Map<?, ?> response) {
		assertEquals("The Flow runs: " + response.get("error"), Boolean.TRUE, response.get("ok"));
		return (Map<?, ?>) response.get("result");
	}

	private static String errorCode(Map<?, ?> response) {
		assertEquals(Boolean.FALSE, response.get("ok"));
		var code = ((Map<?, ?>) response.get("error")).get("code");
		return code == null ? null : code.toString();
	}

	@Before
	public void reset() {
		seen.clear();
		roles.clear();
		views.clear();
		inside.set(0);
		overlap.set(0);
		rendezvous = new CountDownLatch(2);
	}

	@Test
	public void chunksRunOnSeveralThreadsWithTheSequentialResultAndNoSharedRunState() throws Exception {
		try (var h = new Harness()) {
			rendezvous = new CountDownLatch(0);
			var expected = result(h.run(folded(1), items(53)));
			assertEquals("0:3,1:3,2:3,3:3,4:3,5:3,6:3,7:3,8:3,9:3,10:3", expected.get("text"));
			reset();
			var threads = new HashSet<String>();
			for (int pass = 0; pass < 40; pass++) {
				var parallel = result(h.run(folded(4), items(53)));
				assertEquals("pass " + pass, expected.get("text"), parallel.get("text"));
				assertEquals("pass " + pass, expected.get("data").toString(), parallel.get("data").toString());
			}
			System.out.println("lanes at the same time: " + overlap.get() + ", objects probed: " + seen.size());
			assertTrue("Two lanes ran at the same time: " + overlap.get(), overlap.get() >= 2);
			int sharedParents = 0;
			synchronized (seen) {
				for (var entry : seen.entrySet()) {
					var role = roles.get(entry.getKey());
					threads.addAll(entry.getValue());
					if ("parent".equals(role)) {
						if (entry.getValue().size() > 1) sharedParents++;
					} else {
						assertEquals("A " + role + " of a chunk stays on one thread: " + entry.getValue(), 1, entry.getValue().size());
					}
				}
			}
			assertTrue("The probe sees parent, shared by design, on several threads", sharedParents > 0);
			assertTrue("The chunks run on lane threads: " + threads, threads.stream().filter(name -> name.startsWith("chunk-lane-")).count() >= 2);
			assertFalse("No chunk runs on the calling thread when lanes are free: " + threads, threads.contains(Thread.currentThread().getName()));
		}
	}

	@Test
	public void parentAndResultAreReadOnlyOnTheLanes() throws Exception {
		try (var h = new Harness()) {
			rendezvous = new CountDownLatch(0);
			for (var statement : List.of("parent.total=parent.total+1", "result.seen=chunk.index")) {
				var response = h.run("const _flow={sourceVersion:2,inputs:{items:{type:\"array\"}}}\nfunction W({input,result}){\nlocal.total=0\n"
						+ "forEachChunk({source:input.items,size:3,parallel:4,$$each:function({chunk}){\n" + statement + "\n}})\nresult.total=local.total\n}", items(30));
				assertEquals(statement, "PARALLEL_SCOPE_READ_ONLY", errorCode(response));
			}
		}
	}

	@Test
	public void theFirstFailedChunkWinsAndStopsTheLanesAtOnce() throws Exception {
		try (var h = new Harness()) {
			var flow = "const _flow={sourceVersion:2,inputs:{items:{type:\"array\"}}}\nfunction F({input,result}){\n"
					+ "local.out=forEachChunk({source:input.items,size:1,parallel:4%s,$$each:function({chunk}){\n"
					+ "if({condition:chunk.index===1,$$then:function(){local.w=flow.wait({delayMs:150})\nthrow({code:\"FIRST\",message:\"first\"})}})\n"
					+ "if({condition:chunk.index===3,$$then:function(){local.w=flow.wait({delayMs:300})\nthrow({code:\"LATER\",message:\"later\"})}})\n"
					+ "if({condition:chunk.index>3,$$then:function(){local.w=flow.wait({delayMs:%d})}})\n"
					+ "return 1\n},$$reduce:function({acc,value}){\nreturn acc===null?value:acc+value\n}})\nresult.out=local.out\n}";
			// also warms the runtime up: the timing below is the one of the loop only
			var tolerant = result(h.run(String.format(flow, ",onError:\"continue\"", 10), items(12)));
			assertEquals(10, ((Number) tolerant.get("out")).intValue());
			var started = System.nanoTime();
			var stopped = h.run(String.format(flow, "", 3000), items(40));
			var elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
			assertEquals("FIRST", errorCode(stopped));
			System.out.println("first failed chunk: " + elapsed + " ms for chunks waiting 3000 ms");
			assertTrue("The running chunks are cancelled at their next block: " + elapsed + " ms", elapsed < 1500);
		}
	}

	@Test
	public void aSaturatedPoolFallsBackToTheCallingThread() throws Exception {
		var poolField = ParallelLanes.class.getDeclaredField("POOL");
		poolField.setAccessible(true);
		var pool = (ThreadPoolExecutor) poolField.get(null);
		var maximum = pool.getMaximumPoolSize();
		var release = new CountDownLatch(1);
		var occupied = new CountDownLatch(1);
		try (var h = new Harness()) {
			rendezvous = new CountDownLatch(0);
			var expected = result(h.run(folded(1), items(23)));
			pool.setMaximumPoolSize(1);
			for (int i = 0; i < 100 && pool.getPoolSize() > 1; i++) Thread.sleep(20);
			pool.execute(() -> {
				occupied.countDown();
				try {
					release.await(30, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			assertTrue(occupied.await(5, TimeUnit.SECONDS));
			reset();
			rendezvous = new CountDownLatch(0);
			var fallback = result(h.run(folded(4), items(23)));
			assertEquals(expected.get("text"), fallback.get("text"));
			assertEquals(expected.get("data").toString(), fallback.get("data").toString());
			synchronized (seen) {
				for (var threads : seen.values()) assertEquals(Set.of(Thread.currentThread().getName()), threads);
			}
		} finally {
			release.countDown();
			pool.setMaximumPoolSize(maximum);
		}
	}

	@Test
	public void cursorChunksRunOnTheLanesWhileThisThreadReadsThePages() throws Exception {
		var total = 450;
		var failingPage = new AtomicInteger(-1);
		var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		var port = server.getAddress().getPort();
		server.createContext("/rows/", exchange -> {
			var query = String.valueOf(exchange.getRequestURI().getRawQuery());
			var matcher = java.util.regex.Pattern.compile("(?:^|&)page=(\\d+)").matcher(query);
			var page = matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
			byte[] body;
			int status = 200;
			try {
				if (page == failingPage.get()) {
					status = 500;
					body = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
				} else {
					var results = new JSONArray();
					for (int id = (page - 1) * 100 + 1; id <= Math.min(total, page * 100); id++) {
						results.put(new JSONObject().put("id", id).put("Amount", id + ".5"));
					}
					var next = page * 100 < total ? "http://127.0.0.1:" + port + "/rows/?page=" + (page + 1) : JSONObject.NULL;
					body = new JSONObject().put("count", total).put("next", next).put("results", results).toString()
							.getBytes(java.nio.charset.StandardCharsets.UTF_8);
				}
			} catch (org.codehaus.jettison.json.JSONException e) {
				throw new java.io.IOException(e);
			}
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, body.length);
			try (var out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		try (var h = new Harness()) {
			Files.writeString(h.project.resolve("_flow/blocks/proof/pages.block.js"),
					"const _meta={sourceVersion:2,runtime:'rhino',properties:{url:{kind:'value',type:'string'}},outputs:{out:{type:'any'}}};\n"
					+ "(function(){return {run:function(ctx,node){return ctx.createRowsCursor({http:{url:ctx.template(ctx.props(node).url),headers:{},redactions:[],"
					+ "dropKeys:['__proto__'],items:'/results',next:'/next',pageParameter:'page',connectTimeout:2000,readTimeout:5000},"
					+ "limits:{pages:100,inputRows:100000,inputBytes:67108864,rowNodes:100000,stringCharacters:262144,depth:64,millis:60000},take:0});}};}())");
			// reads every row of a chunk, from a lane
			Files.writeString(h.project.resolve("_flow/blocks/proof/views.block.js"),
					"const _meta={sourceVersion:2,runtime:'rhino',properties:{rows:{kind:'expression',type:'any'}},outputs:{out:{type:'any'}}};\n"
					+ "(function(){return {run:function(ctx,node){var P=Packages.com.twinsoft.convertigo.engine.flow.FlowParallelChunksTest;"
					+ "var rows=ctx.expr(ctx.props(node).rows);P.meet();for(var i=0;i<rows.length;i++)P.view(i,rows[i]);return rows.length;}};})()");
			var flow = "const _flow={sourceVersion:2,inputs:{url:{type:\"string\"}}}\nfunction Pages({input,result}){\n"
					+ "local.schema=[{name:\"Amount\",type:\"number\",number_decimal_places:2}]\nlocal.source=proof.pages({url:input.url})\n"
					+ "local.summary=forEachChunk({source:local.source,size:64,parallel:%d,into:\"data\",$$each:function({chunk}){\n"
					+ "local.thread=proof.probe({index:chunk.index})\n"
					+ "local.cells=rows.formatCells({rows:chunk.rows,profile:\"table\",schema:parent.schema})\n"
					+ "local.selected=rows.select({rows:local.cells,firstColumns:[\"id\"],columns:[{key:\"Amount\",path:\"$[\\\"Amount\\\"]\"}]})\n"
					+ "emit({rows:local.selected})\n}})\nresult.chunks=local.summary.chunks\nresult.rows=local.summary.rows\n}";
			var input = new JSONObject().put("url", "http://127.0.0.1:" + port + "/rows/");
			rendezvous = new CountDownLatch(0);
			var sequential = result(h.run(String.format(flow, 1), input));
			reset();
			var parallel = result(h.run(String.format(flow, 4), input));
			assertTrue("The cursor chunks run on several lanes at the same time", overlap.get() >= 2);
			rendezvous = new CountDownLatch(0);
			assertEquals(total, ((Number) parallel.get("rows")).intValue());
			assertEquals(8, ((Number) parallel.get("chunks")).intValue());
			assertEquals(total, ((List<?>) parallel.get("data")).size());
			assertEquals("The parallel rows are the sequential ones, in order", sequential.get("data").toString(), parallel.get("data").toString());
			// a chunk kept in parent is read by every lane at the same time: one view per row, published once
			var keep = "const _flow={sourceVersion:2,inputs:{url:{type:\"string\"},items:{type:\"array\"}}}\nfunction Keep({input,result}){\n"
					+ "local.keep=null\nlocal.source=proof.pages({url:input.url})\n"
					+ "forEachChunk({source:local.source,size:100,$$each:function({chunk}){\n"
					+ "if({condition:chunk.index===0,$$then:function(){parent.keep=chunk.rows}})\n}})\n"
					+ "local.n=forEachChunk({source:input.items,size:1,parallel:4,$$each:function({chunk}){\nlocal.c=proof.views({rows:parent.keep})\nreturn local.c\n"
					+ "},$$reduce:function({acc,value}){\nreturn acc===null?value:acc+value\n}})\nresult.n=local.n\n}";
			reset();
			var kept = result(h.run(keep, new JSONObject(input.toString()).put("items", items(16).get("items"))));
			assertEquals(1600, ((Number) kept.get("n")).intValue());
			assertEquals(100, views.size());
			for (var entry : views.entrySet()) assertEquals("Row " + entry.getKey() + " has one view", 1, entry.getValue().size());
			failingPage.set(3);
			var failedSequential = h.run(String.format(flow, 1), input);
			var failedParallel = h.run(String.format(flow, 4), input);
			assertEquals(errorCode(failedSequential), errorCode(failedParallel));
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void cancelledLanesStopAndCloseWaitsForThem() throws Exception {
		var ended = new AtomicInteger();
		var lanes = ParallelLanes.open(3, (self, index) -> {
			try {
				while (self.canContinue(0)) Thread.sleep(5);
			} finally {
				ended.incrementAndGet();
			}
		});
		assertNotNull(lanes);
		var count = lanes.count();
		assertTrue(count >= 1);
		lanes.cancel("test");
		assertEquals("test", lanes.cancelled());
		assertNull("A cancelled lane gets no task", lanes.next());
		lanes.close();
		assertEquals(count, ended.get());
		// a lane that ignores the cancellation is interrupted after the grace delay
		var stuck = ParallelLanes.open(1, (self, index) -> Thread.sleep(60_000));
		var started = System.nanoTime();
		stuck.cancel("test");
		stuck.close();
		assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 5_000);
	}
}
