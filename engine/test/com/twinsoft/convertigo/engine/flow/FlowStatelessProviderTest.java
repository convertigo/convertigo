/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/** Actual provider code: independent closure instances, simultaneous frames and source revisions. */
public class FlowStatelessProviderTest {
    public static final java.util.concurrent.atomic.AtomicInteger preparations = new java.util.concurrent.atomic.AtomicInteger();
    private static Method method(Class<?> c, String name, Class<?>... types) throws Exception {
        var method = c.getDeclaredMethod(name, types); method.setAccessible(true); return method;
    }
    private static Object field(Object object, String name) throws Exception { return method(object.getClass(), name).invoke(object); }
    private static final class Harness implements AutoCloseable {
        final Path project = Files.createTempDirectory("stateless-provider-");
        final File file;
        final Object reference, source;
        final Method acquire, invoke;
        Harness() throws Exception {
            var root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
            org.junit.Assume.assumeNotNull(root);
            Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("stateless-provider-test");
            EnginePropertiesManager.initProperties();
            file = new File(root, "Engine.js");
            var bridge = FlowEngineBridge.class;
            var ref = Class.forName(bridge.getName()+"$EngineRef");
            var src = Class.forName(bridge.getName()+"$CachedEngineSource");
            var lookup = Class.forName(bridge.getName()+"$CachedEngineRuntimeLookup");
            reference = method(ref,"parse",String.class).invoke(null,"lib_flow_engine.Engine");
            var constructor = src.getDeclaredConstructor(File.class,String.class,long.class,long.class);constructor.setAccessible(true);
            source = constructor.newInstance(file,Files.readString(file.toPath()),file.lastModified(),file.length());
            acquire = method(bridge,"sharedEngineRuntime",ref,File.class,src,Context.class,JSONObject.class,com.twinsoft.convertigo.engine.Context.class);
            invoke = method(bridge,"invokePrepared",ref,File.class,src,String.class,JSONObject.class,com.twinsoft.convertigo.engine.Context.class,Context.class,Scriptable.class,Scriptable.class,lookup);
            var block = project.resolve("_flow/blocks/proof/count.block.js");Files.createDirectories(block.getParent());
            Files.writeString(block,"const _meta={sourceVersion:2,runtime:'rhino',properties:{},outputs:{out:{type:'object'}}};\n"+
                    "(function(){var count=0;return {run:function(ctx){count++;"+
                    "var prototype=Object.getPrototypeOf(ctx);var original=prototype.canContinue;"+
                    "try{prototype.canContinue=function(){return false;};}catch(ignore){}"+
                    "return {count:count,libraryCount:ctx.lib('proof').next(),owner:ctx.scopes.input.owner,protected:prototype.canContinue===original,"+
                    "frozen:Object.isFrozen(ctx.blocks)&&Object.isFrozen(ctx.definition)&&Object.isFrozen(ctx.preparedNodes)};}};})()");
            Files.createDirectories(project.resolve("_flow/lib"));
            Files.writeString(project.resolve("_flow/lib/proof.js"),
                    "(function(){var count=0;return {next:function(){return ++count;}};})()");
            Files.writeString(project.resolve("_flow/blocks/proof/optional.block.js"),
                    "const _meta={sourceVersion:2,runtime:'flow',properties:{},outputs:{out:{type:'object'}}};\n"+
                    "function optional({input,result}) { result.value = unavailable.provider({}); return result; }");
            Files.writeString(project.resolve("_flow/blocks/proof/shared.block.js"),
                    "const _meta={sourceVersion:2,runtime:'rhino',properties:{},outputs:{out:{type:'object'}}};\n"+
                    "(function(){return {stateless:true,prepareNode:function(){"+
                    "Packages.com.twinsoft.convertigo.engine.flow.FlowStatelessProviderTest.preparations.incrementAndGet();"+
                    "return function(ctx){return {owner:ctx.scopes.input.owner};};},run:function(){throw Error('unprepared');}};})()");
        }
        Map<?,?> run(JSONObject request) throws Exception {
            request.put("projectDir",project.toString()).put("includeTrace",false);
            try (var cx = Context.enter()) {
                cx.setLanguageVersion(Context.VERSION_ES6);
                var lookup = acquire.invoke(null,reference,file,source,cx,request,null);
                var runtime = field(lookup,"runtime");
                return (Map<?,?>)invoke.invoke(new FlowEngineBridge(),reference,file,source,"run",request,null,cx,
                        field(runtime,"scope"),field(runtime,"engineObject"),lookup);
            }
        }
        @Override public void close() throws Exception { FlowEngineBridge.clearCaches();org.apache.commons.io.FileUtils.deleteDirectory(project.toFile()); }
    }
    @Test public void concurrentProviderCallsDoNotShareBlockClosureCountersOrInput() throws Exception {
        try (var h = new Harness()) {
            var pool = Executors.newFixedThreadPool(5);
            var jobs = new ArrayList<java.util.concurrent.Future<?>>();
            try {
                for(int i=0;i<5;i++) {
                    var owner="owner-"+i;
                    jobs.add(pool.submit(() -> {
                        try {
                            for(int pass=0;pass<4;pass++) {
                                var request=new JSONObject().put("flowQName","Fixture.Proof")
                                        .put("definition",new JSONObject("{version:1,nodes:[{block:'proof.count',out:'result.proof'}]}"))
                                        .put("input",new JSONObject().put("owner",owner));
                                var response=h.run(request);
                                assertEquals(Boolean.TRUE,response.get("ok"));
                                var proof=(Map<?,?>)((Map<?,?>)response.get("result")).get("proof");
                                assertEquals(owner,proof.get("owner"));assertEquals(1,((Number)proof.get("count")).intValue());
                                assertEquals(1,((Number)proof.get("libraryCount")).intValue());
                                assertEquals(Boolean.TRUE,proof.get("protected"));assertEquals(Boolean.TRUE,proof.get("frozen"));
                            }
                        } catch(Exception e) { throw new AssertionError(e); }
                    }));
                }
                for(var job:jobs)job.get(90,TimeUnit.SECONDS);
            } finally { pool.shutdownNow(); }
        }
    }
    @Test public void differentDefinitionsWithSameNameAndInvalidationCannotReuseAnOldPlan() throws Exception {
        try(var h=new Harness()) {
            for(int value=1;value<=3;value++) {
                var request=new JSONObject().put("flowQName","Fixture.Same")
                        .put("flowSource","function Same() { return "+value+"; }");
                var response=h.run(request);assertEquals(Boolean.TRUE,response.get("ok"));assertEquals(value,((Number)response.get("result")).intValue());
                if(value==2)FlowEngineBridge.invalidateProjectDataCaches(h.project.toString());
            }
        }
    }

    @Test public void declaredStatelessImplementationPreparesItsBindingsOncePerPlan() throws Exception {
        preparations.set(0);
        try(var h=new Harness()) {
            for(int i=0;i<8;i++) {
                var request=new JSONObject().put("flowQName","Fixture.Prepared")
                        .put("definition",new JSONObject("{version:1,nodes:[{block:'proof.shared',out:'result.proof'}]}"))
                        .put("input",new JSONObject().put("owner","call-"+i));
                var result=h.run(request);assertEquals(Boolean.TRUE,result.get("ok"));
                assertEquals("call-"+i,((Map<?,?>)((Map<?,?>)result.get("result")).get("proof")).get("owner"));
            }
            assertEquals(1,preparations.get());
        }
    }
}
