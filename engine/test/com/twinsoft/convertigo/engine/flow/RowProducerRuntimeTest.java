/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import com.twinsoft.convertigo.engine.util.JsonValues;

import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.apache.log4j.MDC;
import com.twinsoft.convertigo.engine.flow.rows.RowSource;
import com.twinsoft.convertigo.engine.flow.rows.RowStreamException;

public class RowProducerRuntimeTest {
	@org.junit.Before public void logs() {
		com.twinsoft.convertigo.engine.Engine.logEngine = org.apache.log4j.Logger.getLogger("row-test");
		com.twinsoft.convertigo.engine.Engine.logBeans = com.twinsoft.convertigo.engine.Engine.logEngine;
	}
	private String bootstrap() throws Exception {
		var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		return Files.readString((root == null ? Path.of("../../lib_flow_engine/_flow") : Path.of(root)).resolve("modules/row-producer-runtime.js"));
	}
	private RowProducerRuntime runtime(String code, Map<String, Object> input) throws Exception {
		return runtime(code,input,new ResultDocumentTest.Source());
	}
	private RowProducerRuntime runtime(String code, Map<String, Object> input, RowSource source) throws Exception {
		return new RowProducerRuntime(source, new RowProducerRuntime.Plan(bootstrap(), Map.of("transform", code), input,"transform",""), null);
	}
	@Test public void lazyOwnershipNeverBorrowsForAbandonedResponses() throws Exception {
		var source=new ResultDocumentTest.Source();
		var runtime=runtime("({run:function(c,n){return n.props.row;}})",JsonValues.object(),source);
		assertEquals(0,RowProducerRuntime.activeLeases()); runtime.close(); runtime.close();
		assertEquals(0,source.pulls); assertEquals(1,source.closed); assertEquals(0,RowProducerRuntime.activeLeases());
	}
	@Test public void independentRealmsKeepLiteralInputAndRestoreThreadState() throws Exception {
		var loader=Thread.currentThread().getContextClassLoader(); MDC.put("row-test","caller");
		try(var a=runtime("({run:function(c,n){c.rowState.n=(c.rowState.n||0)+1;return {n:c.rowState.n,text:n.props.input.text,context:typeof context,clean:org.apache.log4j.MDC.get('row-test')===null};}})",JsonValues.object("text","{{ java.lang.System.exit(9) }}"));
				var b=runtime("({run:function(c,n){return {n:c.rowState.n||0,dirty:Object.prototype.hasOwnProperty('leak')};}})",JsonValues.object())) {
			var value=a.next(); assertEquals(1,((Number) value.get("n")).intValue()); assertTrue(Boolean.TRUE.equals(value.get("clean")));
			assertEquals("undefined",((String) value.get("context"))); assertTrue(((String) value.get("text")).startsWith("{{"));
			assertEquals(0,((Number) b.next().get("n")).intValue()); assertEquals(2,((Number) a.next().get("n")).intValue());
			assertEquals("caller",MDC.get("row-test")); assertSame(loader,Thread.currentThread().getContextClassLoader());
			assertEquals(2,RowProducerRuntime.activeLeases());
		} finally { MDC.remove("row-test"); }
		assertEquals(0,RowProducerRuntime.activeLeases());
	}
	@Test public void failedTransformAndBindingReleaseEveryLease() throws Exception {
		for(var code:new String[]{"throw new Error('synthetic')", "({run:function(){throw new Error('synthetic');}})",
				"({run:function(){return {bad:new java.io.File('synthetic')};}})",
				"({run:function(){return {__flowHandle:true};}})",
				"({run:function(){return {get x(){throw new Error('accessor');}};}})"}) {
			var source=new ResultDocumentTest.Source();
			try(var runtime=runtime(code,JsonValues.object(),source)) {
				try { runtime.next(); fail("invalid row"); } catch(RowStreamException expected) { assertFalse(expected.getMessage().contains("synthetic")); }
			}
			assertEquals(1,source.closed); assertEquals(0,RowProducerRuntime.activeLeases());
		}
	}
	@Test public void prototypeMutationIsDiscardedWithItsRealm() throws Exception {
		try(var a=runtime("({run:function(){Object.prototype.leak=9;return {ok:true};}})",JsonValues.object())) { a.next(); }
		try(var b=runtime("({run:function(){return {clean:!Object.prototype.hasOwnProperty('leak')};}})",JsonValues.object())) { assertTrue(Boolean.TRUE.equals(b.next().get("clean"))); }
		assertEquals(0,RowProducerRuntime.activeLeases());
	}
	@Test public void boundedPoolRejectsOverflowAndRecoversAfterClose() throws Exception {
		var open=new ArrayList<RowProducerRuntime>();
		try {
			for(int i=0;i<32;i++) { var r=runtime("({run:function(c,n){return n.props.row;}})",JsonValues.object()); open.add(r); r.next(); }
			try(var excess=runtime("({run:function(c,n){return n.props.row;}})",JsonValues.object())) {
				try { excess.next(); fail("pool capacity"); } catch(RowStreamException expected) { assertEquals(503,expected.status()); }
			}
		} finally { for(var r:open) r.close(); }
		assertEquals(0,RowProducerRuntime.activeLeases());
	}
	@Test public void oneSourceCannotBeResumedOnAnotherThread() throws Exception {
		try(var r=runtime("({run:function(c,n){return n.props.row;}})",JsonValues.object())) {
			r.next(); var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
			var thread=new Thread(()->{try { r.next(); } catch(Throwable e) { failure.set(e); }});thread.start();thread.join();
			assertTrue(failure.get() instanceof RowStreamException); assertEquals(0,RowProducerRuntime.activeLeases());
		}
	}
	@Test public void globalOperatorsCollectUnderExplicitBoundsBeforeEmitting() throws Exception {
		for(int limit:new int[]{2,30}) {
			var source=new ResultDocumentTest.Source();
			var plan=new RowProducerRuntime.Plan(bootstrap(),Map.of("transform","({run:function(c,n){return {rows:n.props.rows.reverse(),metadata:{colDef:null}};}})"),JsonValues.object(),"transform","",true,limit,100_000);
			try(var runtime=new RowProducerRuntime(source,plan,null)) {
				if(limit==2) { try {runtime.next();fail("collect bound");}catch(RowStreamException expected){assertEquals(413,expected.status());} }
				else { assertEquals(20,((Number) runtime.next().get("id")).intValue());assertEquals(21,source.pulls);assertTrue(runtime.metadata().containsKey("colDef"));assertEquals(0,RowProducerRuntime.activeLeases()); }
			}
			assertEquals(1,source.closed);assertEquals(0,RowProducerRuntime.activeLeases());
		}
	}
	@Test public void immutableBlockSnapshotAndGenerationSurviveCallerReuse() throws Exception {
		var root=Files.createTempDirectory("row-generation");
		try {
			var file=root.resolve("probe/Version.java");Files.createDirectories(file.getParent());
			Files.writeString(file,"package probe; public class Version { public static String value(){return \"captured\";} }");
			assertEquals(0,javax.tools.ToolProvider.getSystemJavaCompiler().run(null,null,null,"-d",root.toString(),file.toString()));
			try(var loader=new java.net.URLClassLoader(new java.net.URL[]{root.toUri().toURL()},getClass().getClassLoader())) {
				var sources=new java.util.HashMap<String,String>();sources.put("transform","({run:function(c,n){return {original:n.props.input.original,generation:String(Packages.probe.Version.value()),nolog:String(org.apache.log4j.MDC.get('nolog'))==='true'};}})");
				var config=JsonValues.object("original",true);
				var plan=new RowProducerRuntime.Plan(bootstrap(),sources,config,"transform","");
				config.put("original",false);
				sources.put("transform","throw new Error('changed source')");
				try(var runtime=new RowProducerRuntime(new ResultDocumentTest.Source(),plan,loader)) {
					var previous=Thread.currentThread().getContextClassLoader();
					var value=runtime.next(); assertTrue(Boolean.TRUE.equals(value.get("original"))); assertTrue(Boolean.TRUE.equals(value.get("nolog")));
					assertEquals("captured",((String) value.get("generation")));assertSame(previous,Thread.currentThread().getContextClassLoader());
				}
			}
		} finally { com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(root.toFile()); }
		assertEquals(0,RowProducerRuntime.activeLeases());
	}

	@Test public void privateProducerKeepsPackagesWithoutLeakingThreadOrRealmState() throws Exception {
		var previous=Thread.currentThread().getContextClassLoader();
		try(var loader=new java.net.URLClassLoader(new java.net.URL[0],getClass().getClassLoader());
			var runtime=new RowProducerRuntime(new ResultDocumentTest.Source(),new RowProducerRuntime.Plan(bootstrap(),
				Map.of("transform","({run:function(c,n){var old=c.rowState.packages;c.rowState.packages=Packages;return {same:!old||old===Packages,loader:java.lang.Thread.currentThread().getContextClassLoader()===Packages.java.lang.Thread.currentThread().getContextClassLoader()};}})"),
				JsonValues.object(),"transform",""),loader)) {
			MDC.put("row-test","outer");
			var cx=org.mozilla.javascript.Context.enter();
			cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_1_8);
			try {
				for(int i=0;i<20;i++) {
					assertTrue(Boolean.TRUE.equals(runtime.next().get("same")));
					assertSame(previous,Thread.currentThread().getContextClassLoader());
					assertEquals("outer",MDC.get("row-test"));
					assertSame(cx,org.mozilla.javascript.Context.getCurrentContext());
					assertEquals(org.mozilla.javascript.Context.VERSION_1_8,cx.getLanguageVersion());
				}
			} finally { org.mozilla.javascript.Context.exit(); MDC.remove("row-test"); }
		}
		assertEquals(0,RowProducerRuntime.activeLeases());
	}

	@Test public void failedPackageRestorationCannotRetainProducerLease() throws Exception {
		var source = new ResultDocumentTest.Source();
		var runtime = runtime("({run:function(c,n){return n.props.row;}})", JsonValues.object(), source);
		runtime.next();
		var field = RowProducerRuntime.class.getDeclaredField("scope"); field.setAccessible(true);
		var original = (org.mozilla.javascript.Scriptable) field.get(runtime);
		var reject = new boolean[] {false};
		var failing = new org.mozilla.javascript.NativeObject() {
			@Override public void put(String name, org.mozilla.javascript.Scriptable start, Object value) {
				if (reject[0]) throw new IllegalStateException("synthetic restore failure");
				super.put(name, start, value);
			}
		};
		failing.defineProperty("__flowEnginePackages", org.mozilla.javascript.ScriptableObject.getProperty(original, "__flowEnginePackages"), 0);
		reject[0] = true; field.set(runtime, failing);
		try { runtime.close(); fail("restore failure"); }
		catch (IllegalStateException expected) { }
		assertNull(field.get(runtime));
		assertEquals(0, RowProducerRuntime.activeLeases());
		assertEquals(1, source.closed);
		runtime.close(); // Idempotent even after a failing restoration.
		assertEquals(1, source.closed);
	}

}
