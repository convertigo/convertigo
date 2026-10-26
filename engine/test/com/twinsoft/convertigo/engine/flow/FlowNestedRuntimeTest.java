/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.mozilla.javascript.Scriptable;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.*;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** Re-entry crosses the public bridge and returns detached values, as a requestable does. */
public class FlowNestedRuntimeTest {
    private static volatile Fixture fixture;
    private static volatile CyclicBarrier barrier;

    public static void awaitRoots() throws Exception { barrier.await(10, TimeUnit.SECONDS); }
    public static String child(String owner, boolean fail, boolean otherGeneration, boolean otherThread, boolean invalidate) throws Exception {
        if (invalidate) FlowEngineBridge.clearCaches();
        Callable<String> action = () -> {
            try { return fixture.call(owner + "-child", false, fail, otherGeneration, false, false, false).toString(); }
            catch (EngineException expected) { return "{\"failed\":true}"; }
        };
        if (!otherThread) return action.call();
        var worker = Executors.newSingleThreadExecutor();
        try { return worker.submit(action).get(10, TimeUnit.SECONDS); }
        finally { worker.shutdownNow(); }
    }

    private static final class Fixture implements AutoCloseable {
        final Engine previous = Engine.theApp;
        final Path directory;
        final ClassLoader first = new ClassLoader(getClass().getClassLoader()) {};
        final ClassLoader second = new ClassLoader(getClass().getClassLoader()) {};
        final Project project, alternate;
        final FlowEngineBridge bridge = new FlowEngineBridge();
        Fixture(boolean compatible) throws Exception {
            Engine.logEngine = Engine.logBeans = org.apache.log4j.Logger.getLogger("nested-test");
            EnginePropertiesManager.initProperties();
            directory = Files.createTempDirectory("nested-runtime-");
            Files.createDirectories(directory.resolve("_flow"));
            Files.writeString(directory.resolve("_flow/Engine.js"), """
                (function(){var id=String(java.util.UUID.randomUUID());return {
                  supportsNestedExecution: COMPATIBLE,
                  cacheClear:function(){return '{}';},
                  runObject:function(text,detach){var r=JSON.parse(text);
                    var owner=String(context.get('fixture'));var dir=String(__flowProjectDir);
                    if(r.parallel)Packages.com.twinsoft.convertigo.engine.flow.FlowNestedRuntimeTest.awaitRoots();
                    if(r.fail)throw new Error('fixture failure');
                    var child=r.nested?JSON.parse(String(Packages.com.twinsoft.convertigo.engine.flow.FlowNestedRuntimeTest.child(owner,!!r.childFail,!!r.otherGeneration,!!r.otherThread,!!r.invalidate))):null;
                    return detach({ok:true,id:id,owner:owner,input:r.owner,child:child,
                      after:String(context.get('fixture')),directoryRestored:dir===String(__flowProjectDir)});
                  }
                };})()
                """.replace("COMPATIBLE", Boolean.toString(compatible)));
            project = project(first); alternate = project(second);
            Engine.theApp = new Engine();
            Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
                @Override public Project getLoadedProjectByName(String name) { return null; }
                @Override public Project getOriginalProjectByName(String name, boolean openable) { return project; }
            };
            FlowEngineBridge.clearCaches(); fixture = this;
        }
        private Project project(ClassLoader generation) throws EngineException {
            var p = new Project() {
                @Override public String getDirPath() { return directory.toString(); }
                @Override public ClassLoader getProjectClassLoader() { return generation; }
            }; p.setName("NestedFixture"); return p;
        }
        JSONObject call(String owner, boolean nested, boolean fail, boolean otherGeneration, boolean otherThread,
                boolean invalidate, boolean parallel) throws Exception {
            var context = new com.twinsoft.convertigo.engine.Context(owner);
            context.project = otherGeneration ? alternate : project; context.set("fixture", owner);
            var request = new JSONObject().put("owner", owner).put("nested", nested).put("fail", !nested && fail)
                .put("childFail", nested && fail).put("otherGeneration", otherGeneration).put("otherThread", otherThread)
                .put("invalidate", invalidate).put("parallel", parallel)
                .put("projectDir", directory.resolve(owner).toString());
            return bridge.invoke("NestedFixture.Engine", "run", request, context, null, null);
        }
        @Override public void close() throws Exception {
            FlowEngineBridge.clearCaches(); Engine.theApp = previous; fixture = null;
            org.apache.commons.io.FileUtils.deleteDirectory(directory.toFile());
        }
    }
    private static void isolated(JSONObject value, String owner) throws Exception {
        assertEquals(owner, value.getString("owner")); assertEquals(owner, value.getString("after"));
        assertEquals(owner, value.getString("input")); assertTrue(value.getBoolean("directoryRestored"));
    }
    @Test public void nestedRunRestoresContextAfterSuccessAndFailureAndRetainsNoRequest() throws Exception {
        try (var f = new Fixture(true)) {
            var info = FlowEngineBridge.class.getDeclaredMethod("bridgeCacheInfo", String.class); info.setAccessible(true);
            var before = ((JSONObject)info.invoke(null,"NestedFixture.Engine")).getJSONObject("methods").optJSONObject("run");
            int reusedBefore = before == null ? 0 : before.getInt("runtimeNestedReuses");
            var value = f.call("parent", true, false, false, false, false, false);
            isolated(value, "parent"); isolated(value.getJSONObject("child"), "parent-child");
            assertEquals(value.getString("id"), value.getJSONObject("child").getString("id"));
            var failed = f.call("next", true, true, false, false, false, false);
            isolated(failed, "next"); assertTrue(failed.getJSONObject("child").getBoolean("failed"));
            assertEquals(reusedBefore + 2, ((JSONObject)info.invoke(null,"NestedFixture.Engine")).getJSONObject("methods").getJSONObject("run").getInt("runtimeNestedReuses"));
            var field = FlowEngineBridge.class.getDeclaredField("currentRuntime"); field.setAccessible(true);
            assertNull(((ThreadLocal<?>)field.get(null)).get());
            field = FlowEngineBridge.class.getDeclaredField("engineRuntimeCache"); field.setAccessible(true);
            for (var pool : ((Map<?,?>)field.get(null)).values()) {
                var available = pool.getClass().getDeclaredField("available"); available.setAccessible(true);
                for (var idle : (Iterable<?>)available.get(pool)) {
                    var method = idle.getClass().getDeclaredMethod("runtime"); method.setAccessible(true); var runtime = method.invoke(idle);
                    method = runtime.getClass().getDeclaredMethod("scope"); method.setAccessible(true); var scope = (Scriptable)method.invoke(runtime);
                    assertSame(Scriptable.NOT_FOUND, scope.get("context", scope));
                    assertSame(Scriptable.NOT_FOUND, scope.get("__flowProjectDir", scope));
                }
            }
        }
    }
    @Test public void anotherThreadGenerationOrInvalidationDoesNotBorrowTheActiveScope() throws Exception {
        try (var f = new Fixture(true)) {
            for (int mode = 0; mode < 4; mode++) {
                EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_REENTRANT, mode == 3 ? "false" : "true");
                var value = f.call("parent", true, false, false, mode == 1, mode == 2, false);
                if (mode == 0) {
                    // Explicitly invoke a parent whose child requests a different generation.
                    var request = new JSONObject().put("nested", true).put("owner", "parent").put("otherGeneration", true);
                    var context = new com.twinsoft.convertigo.engine.Context("parent"); context.project=f.project; context.set("fixture","parent");
                    value = f.bridge.invoke("NestedFixture.Engine", "run", request, context, null, null);
                }
                isolated(value, "parent"); assertNotEquals(value.getString("id"), value.getJSONObject("child").getString("id"));
            }
        }
    }
    @Test public void providersMustExplicitlyAcceptReentry() throws Exception {
        try (var f = new Fixture(false)) {
            var value = f.call("parent", true, false, false, false, false, false);
            assertNotEquals(value.getString("id"), value.getJSONObject("child").getString("id"));
        }
    }
    @Test public void concurrentRootsKeepTheirOwnRuntimeAndChildIdentity() throws Exception {
        try (var f = new Fixture(true)) {
            barrier = new CyclicBarrier(5); var workers = Executors.newFixedThreadPool(5);
            try {
                var futures = new ArrayList<Future<JSONObject>>();
                for (int i=0;i<5;i++) {var owner="owner-"+i; futures.add(workers.submit(()->f.call(owner,true,false,false,false,false,true)));}
                var ids = new HashSet<String>();
                for (int i=0;i<5;i++) {var value=futures.get(i).get(15,TimeUnit.SECONDS); isolated(value,"owner-"+i);
                    assertTrue(ids.add(value.getString("id"))); assertEquals(value.getString("id"),value.getJSONObject("child").getString("id"));}
            } finally {workers.shutdownNow();}
        }
    }
}
