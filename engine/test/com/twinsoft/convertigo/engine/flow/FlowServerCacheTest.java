/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class FlowServerCacheTest {
    @Test public void twentyRuntimesCompileOnceAndKeepIndependentExports() throws Exception {
        var cache = new FlowCompiledScriptCache(8);
        var workers = Executors.newFixedThreadPool(20);
        var gate = new CountDownLatch(1);
        try {
            var results = new ArrayList<Future<Object>>();
            for (int i=0; i<20; i++) {
                final int value=i;
                results.add(workers.submit(() -> {
                    gate.await();
                    try (var cx=Context.enter()) {
                        cx.setLanguageVersion(Context.VERSION_ES6); cx.setInterpretedMode(false);
                        var scope=cx.initStandardObjects();
                        ScriptableObject.putProperty(scope,"value",value);
                        var script=cache.get(cx,"({value:value, nested:{}})","module.js");
                        var exports=(org.mozilla.javascript.Scriptable)script.exec(cx,scope);
                        assertEquals(value, ((Number)ScriptableObject.getProperty(exports,"value")).intValue());
                        return ScriptableObject.getProperty(exports,"nested");
                    }
                }));
            }
            gate.countDown();
            var identities=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());
            for(var result:results) assertTrue(identities.add(result.get(15,TimeUnit.SECONDS)));
            assertEquals(1,cache.compilations());
        } finally {workers.shutdownNow();}
    }
    @Test public void revisionsFailuresAndBoundsDoNotReuseOldCode() {
        var cache=new FlowCompiledScriptCache(2);
        try(var cx=Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6);
            var first=cache.get(cx,"1","a");
            assertSame(first,cache.get(cx,"1","a"));
            assertNotSame(first,cache.get(cx,"2","a"));
            cache.get(cx,"3","b"); assertEquals(2,cache.size());
            try {cache.get(cx,"function (","bad"); fail();} catch(org.mozilla.javascript.EvaluatorException expected) { }
            cache.get(cx,"4","bad");
            cache.clear(); assertEquals(0,cache.size());
            assertNotSame(first,cache.get(cx,"1","a"));
        }
    }
    @Test public void bridgeRestoresInterpretedModeAndDoesNotShareClosures() {
        try(var cx=Context.enter()) {
            cx.setLanguageVersion(Context.VERSION_ES6); cx.setInterpretedMode(true);
            var a=cx.initStandardObjects(); var b=cx.initStandardObjects();
            ScriptableObject.putProperty(a,"value",1); ScriptableObject.putProperty(b,"value",2);
            String code="(function(){return value;}())";
            assertEquals(1,((Number)FlowEngineBridge.evalSharedModule(cx,a,code,"fixture-module")).intValue());
            assertEquals(2,((Number)FlowEngineBridge.evalSharedModule(cx,b,code,"fixture-module")).intValue());
            assertTrue(cx.isInterpretedMode());
        }
    }
    @Test public void notifiedEditsAndPeriodicFallbackRefreshSources() throws Exception {
        var dir=Files.createTempDirectory("source-cache"); var file=dir.resolve("module.js");
        try {
            Files.writeString(file,"old"); var cache=new FlowSourceCache(8,60_000);
            String stamp=cache.fingerprint(file.toFile()); assertEquals("old",cache.text(file.toFile()));
            Files.writeString(file,"changed");
            assertEquals(stamp,cache.fingerprint(file.toFile())); assertEquals("old",cache.text(file.toFile()));
            cache.clear(); assertNotEquals(stamp,cache.fingerprint(file.toFile())); assertEquals("changed",cache.text(file.toFile()));
            var periodic=new FlowSourceCache(8,0); assertEquals("changed",periodic.text(file.toFile()));
            Files.writeString(file,"new"); assertEquals("new",periodic.text(file.toFile()));
            Files.delete(file); assertTrue(periodic.fingerprint(file.toFile()).startsWith("missing:"));
        } finally {Files.deleteIfExists(file);Files.delete(dir);}
    }
    @Test public void directoryScanIsSharedUntilInvalidationAndEvictedAtBound() {
        var cache=new FlowSourceCache(2,60_000); var calls=new AtomicInteger();
        var a=new java.io.File("a");
        assertEquals("1",cache.directory(a,()->""+calls.incrementAndGet()));
        assertEquals("1",cache.directory(a,()->""+calls.incrementAndGet()));
        cache.directory(new java.io.File("b"),()->"b"); cache.directory(new java.io.File("c"),()->"c");
        assertEquals("2",cache.directory(a,()->""+calls.incrementAndGet()));
        cache.clear(); assertEquals("3",cache.directory(a,()->""+calls.incrementAndGet()));
    }
}
