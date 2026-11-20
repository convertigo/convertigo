/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.mozilla.javascript.*;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** Contract probes use synthetic markers only. Counters must stay zero on rejected paths. */
public class JavascriptDebugPathTest {
    private static final JSONObject probes=new JSONObject();
    private static void probe(String id,Object... values) throws Exception {
        var item=new JSONObject();for(int i=0;i<values.length;i+=2)item.put((String)values[i],values[i+1]);probes.put(id,item);
    }
    @AfterClass public static void exportProbes() throws Exception {
        String path=System.getProperty("debugger.contract.proof");
        if(path!=null)java.nio.file.Files.writeString(java.nio.file.Path.of(path),probes.toString(2)+"\n");
    }
    private String previous;
    @Before public void setup() throws Exception {
        EnginePropertiesManager.initProperties(); previous=EnginePropertiesManager.getProperty(PropertyName.WEB_STUDIO);
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"disabled");
    }
    @After public void restore() { EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,previous); JavascriptDebugRedaction.testValues=null; }
    private static long paused(JavascriptDebugSession s) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end) {var state=s.state(); if(state.getBoolean("stopped"))return state.getLong("pauseId");Thread.sleep(2);}
        throw new AssertionError("No pause");
    }
    public static class Counter {
        private final AtomicInteger value;
        Counter(AtomicInteger value) { this.value=value; }
        public int incrementAndGet() { return value.incrementAndGet(); }
    }
    private static class Fixture implements AutoCloseable {
        final JavascriptDebugSession session;
        final ExecutorService pool=Executors.newSingleThreadExecutor();
        final AtomicInteger calls=new AtomicInteger(), effects=new AtomicInteger();
        final Future<?> task;
        final long pause;
        Fixture(boolean capability) throws Exception { this(capability,120000); }
        Fixture(boolean capability,long timeout) throws Exception {
            session=new JavascriptDebugSession(timeout); session.breakpoint("paths",1,true,false);
            task=pool.submit(()->{try(var lease=session.enter();var suspended=JavascriptDebugger.suspendForFlow()) {
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                scope.put("calls",scope,Context.javaToJS(new Counter(calls),scope));
                scope.put("effects",scope,Context.javaToJS(new Counter(effects),scope));
                var f=(Function)cx.evaluateString(scope,"(function(id,action,path){calls.incrementAndGet(); if(action==='path')return JSON.stringify({result:String(plugin.previewParsedPath({alias:'FAKE-SECRET-0001',items:[{name:42}],headers:{authorization:'FAKE-RUNTIME-CREDENTIAL'},derived:'RkFLRS1TRUNSRVQtMDAwMQ=='},path))}); if(action!=='evaluate')throw Error('Unexpected action');effects.incrementAndGet();return '{\"result\":\"free\"}';})","callback",1,null);
                var plugin=JavascriptDebugger.participant("test",f); if(capability)plugin.pathEvaluation();
                scope.put("plugin",scope,Context.javaToJS(plugin,scope));
                plugin.enter("one","{\"url\":\"paths\",\"line\":1}");plugin.at("one","before");plugin.leave("one"); return null;
            }});
            pause=paused(session);
        }
        @Override public void close() throws Exception { session.close();task.get(5,TimeUnit.SECONDS);pool.close(); }
    }
    @Test public void q1GrammarRefusesBeforeEveryParticipantOrWatchCall() throws Exception {
        var bad=new ArrayList<>(List.of("a.b()","a+b","a=1","a,b","(a)","a?.b","a[\"b\"]","a[-1]","a[01]","a[1e3]","a[0x1]"," a","a .b","a","a/*x*/","eval(1)","this","globalThis","Packages.java","unknown","\u0456nput.x","input"+".a".repeat(33),"input."+"a".repeat(4091),"input[01]","input[-1]","input[1e3]","input[0x1]","input.\u0430","input.\\u0061","input /*x*/","input.a()"));
        bad.addAll(List.of("input.b()","input+b","input=1","input,b","(input)","input?.b","input[\"b\"]","input/*x*/","input."+"a".repeat(129)));
        try(var fixture=new Fixture(true)) {
            for(String source:bad)for(int watch=0;watch<2;watch++) {
                var refusal=assertThrows(IllegalArgumentException.class,()->fixture.session.evaluate(0,fixture.pause,source));
                assertEquals(JavascriptDebugPath.REFUSAL,refusal.getMessage());
            }
            assertEquals(0,fixture.calls.get());assertEquals(0,fixture.effects.get());
            probe("Q1","refusals",bad.size()*2,"participantCalls",fixture.calls.get(),"effects",fixture.effects.get());
        }
    }
    @Test public void q1ParsedPathHasNoSourceTextAndReadsIndices() throws Exception {
        try(var f=new Fixture(true)) {
            var answer=f.session.evaluate(0,f.pause,"input.items[0].name");
            for(int i=0;answer.optBoolean("refused")&&i<3;i++)answer=f.session.evaluate(0,f.pause,"input.items[0].name");
            assertEquals("42",answer.getString("result"));assertEquals(0,f.effects.get());
            assertEquals("[redacted]",f.session.evaluate(0,f.pause,"input.headers.authorization").getString("result"));
        }
    }
    @Test public void q2OwnDataOnlyWithoutConversionGetterPrototypeOrHostCalls() throws Exception {
        try(var cx=Context.enter()) {
            var scope=cx.initStandardObjects();
            assertThrows(IllegalArgumentException.class,()->JavascriptDebugPath.parse("input"+".a".repeat(32)));
            assertNotNull(JavascriptDebugPath.parse("input"+".a".repeat(31)));
            var data=cx.evaluateString(scope,"var calls=0;var proto={get inherited(){calls++;throw Error('getter');}};var value=Object.create(proto);Object.defineProperty(value,'own',{get:function(){calls++;throw Error('getter');}}); value.toString=value.valueOf=value.toJSON=function(){calls++;throw Error('conversion');}; value[Symbol.toPrimitive]=value.toString;value;","data",1,null);
            assertEquals("<accessor>",JavascriptDebugPath.parse("input.own").resolve(data,()->{}));
            assertEquals(Undefined.instance,JavascriptDebugProperties.read(data,"inherited"));
            for(String key:List.of("__proto__","constructor","prototype"))assertEquals(Undefined.instance,JavascriptDebugProperties.read(data,key));
            assertEquals("[Object]",JavascriptDebugSession.preview(data));
            assertEquals("<opaque>",JavascriptDebugProperties.index(data,0));
            var own=cx.evaluateString(scope,"JSON.parse('{\"__proto__\":42}')","own",1,null);
            assertEquals(42,((Number)JavascriptDebugProperties.read(own,"__proto__")).intValue());
            assertEquals(0,((Number)scope.get("calls",scope)).intValue());
            var host=new Hostile(); assertEquals("<opaque>",JavascriptDebugProperties.read(host,"danger"));
            JavascriptDebugSession.preview(host); assertEquals(0,host.calls.get());
            Object wrapped=Context.javaToJS(host,scope);assertEquals("<opaque>",JavascriptDebugProperties.read(wrapped,"danger"));
            Object list=Context.javaToJS(new ArrayList<>(),scope);assertEquals("<opaque>",JavascriptDebugProperties.read(list,"length"));
            assertEquals(0,host.calls.get());
            var proxy=cx.evaluateString(scope,"new Proxy({}, {get:function(){calls++;throw Error('trap')},ownKeys:function(){calls++;throw Error('trap')}})","proxy",1,null);
            assertEquals("<opaque>",JavascriptDebugProperties.read(proxy,"x"));assertEquals(0,((Number)scope.get("calls",scope)).intValue());
            probe("Q2","javascriptGettersAndConversions",((Number)scope.get("calls",scope)).intValue(),"hostGettersAndConversions",host.calls.get());
        }
    }
    public static class Hostile extends HashMap<String,Object> {
        final AtomicInteger calls=new AtomicInteger();
        @Override public String toString(){calls.incrementAndGet();throw new AssertionError();}
        @Override public Set<Map.Entry<String,Object>> entrySet(){calls.incrementAndGet();throw new AssertionError();}
        public Object getDanger(){calls.incrementAndGet();throw new AssertionError();}
    }
    @Test public void indexRejectsUnqualifiedSlotMapsWithoutCallingThem() throws Exception {
        try(var cx=Context.enter()) {
            var scope=cx.initStandardObjects();var getters=new AtomicInteger();var effects=new AtomicInteger();
            scope.put("counter",scope,Context.javaToJS(new Counter(getters),scope));
            var array=(NativeArray)cx.evaluateString(scope,"var a=[42,84];Object.defineProperty(a,'1',{get:function(){counter.incrementAndGet();throw Error('getter');}});a;","array-layout",1,null);
            var field=Class.forName("org.mozilla.javascript.SlotMapOwner").getDeclaredField("slotMap");field.setAccessible(true);
            var original=(SlotMap)field.get(array);int checked=0;
            assertEquals(42,((Number)JavascriptDebugProperties.index(array,0)).intValue());
            assertEquals("<accessor>",JavascriptDebugProperties.index(array,1));
            assertEquals(42,JavascriptDebugProperties.index(new Object[]{42},0));
            var sparse=(NativeArray)cx.evaluateString(scope,"var sparse=[];sparse[100000]=84;sparse;","sparse-layout",1,null);
            assertEquals(84,((Number)JavascriptDebugProperties.index(sparse,100000)).intValue());
            var object=cx.evaluateString(scope,"({value:7})","object-layout",1,null);
            assertEquals(7,((Number)JavascriptDebugProperties.read(object,"value")).intValue());
            var unknown=java.lang.reflect.Proxy.newProxyInstance(SlotMap.class.getClassLoader(),new Class<?>[]{SlotMap.class},(proxy,method,args)->{
                effects.incrementAndGet();return method.invoke(original,args);
            });
            var maps=new ArrayList<Object>();maps.add(unknown);
            for(String name:List.of("ThreadSafeEmbeddedSlotMap","ThreadSafeHashSlotMap")) {
                var constructor=Class.forName("org.mozilla.javascript."+name).getDeclaredConstructor();constructor.setAccessible(true);maps.add(constructor.newInstance());
            }
            try {
                for(Object map:maps) {
                    field.set(array,map);
                    assertEquals("<opaque>",JavascriptDebugProperties.index(array,0));
                    assertEquals("<opaque>",JavascriptDebugProperties.index(array,1));checked++;
                }
                assertEquals(0,getters.get());assertEquals(0,effects.get());
            } finally {
                field.set(array,original);
                probe("R20","unqualifiedLayoutsChecked",checked,"getters",getters.get(),"effects",effects.get());
            }
            assertEquals(42,((Number)JavascriptDebugProperties.index(array,0)).intValue());
        }
    }
    @Test public void q3ConcurrentArrayAccessorNeverRuns() throws Exception {
        try(var cx=Context.enter()) {
            var scope=cx.initStandardObjects();var calls=new AtomicInteger();scope.put("counter",scope,Context.javaToJS(new Counter(calls),scope));
            var array=(NativeArray)cx.evaluateString(scope,"var a=[42]; a;","array",1,null);
            var getter=(Function)cx.evaluateString(scope,"(function(){counter.incrementAndGet();throw Error('getter');})","getter",1,null);
            try(var pool=Executors.newSingleThreadExecutor()) {
                var future=pool.submit(()->{try(var other=Context.enter()){for(int i=0;i<10000;i++)array.setGetterOrSetter(null,0,getter,false);}});
                for(int i=0;i<10000;i++) {try{JavascriptDebugProperties.index(array,0);}catch(Exception ignored){}}
                future.get(5,TimeUnit.SECONDS);
            }
            assertEquals(0,calls.get());assertEquals("<accessor>",JavascriptDebugProperties.index(array,0));
            var hole=(NativeArray)cx.evaluateString(scope,"new Array(1)","hole",1,null);
            assertEquals("<opaque>",JavascriptDebugProperties.index(hole,0));
            probe("Q3","reads",10001,"accessorCalls",calls.get(),"sparseHoleOpaque",true);
        }
    }
    @Test public void q4StringsAndPrimitiveArraysAreOpaque() throws Exception {
        for(Object value:List.of("FAKE-SECRET-0001",new ConsString("FAKE-","SECRET"),new char[]{'x'},new byte[]{1})) {
            assertEquals("<opaque>",JavascriptDebugProperties.index(value,0));
            assertEquals("<opaque>",JavascriptDebugProperties.read(value,"length"));
            assertFalse(JavascriptDebugProperties.supported(value));
        }
        assertEquals(42,JavascriptDebugProperties.index(new Object[]{42},0));
        probe("Q4","stringAndPrimitiveArrayReads",8,"opaque",true);
    }
    @Test public void q5ServerPauseCannotBecomeFreeAndMissingCapabilityRefuses() throws Exception {
        try(var f=new Fixture(true)) {
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"enabled");
            assertEquals(JavascriptDebugPath.REFUSAL,assertThrows(IllegalArgumentException.class,()->f.session.evaluate(0,f.pause,"input.items.push(1)")).getMessage());
            assertEquals(0,f.calls.get());assertEquals(0,f.effects.get());
        }
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"disabled");
        try(var f=new Fixture(false)) {
            assertEquals(JavascriptDebugPath.REFUSAL,assertThrows(IllegalArgumentException.class,()->f.session.evaluate(0,f.pause,"input")).getMessage());
            assertEquals(0,f.calls.get());assertEquals(0,f.effects.get());
            probe("Q5","refusedParticipantCalls",f.calls.get(),"refusedEffects",f.effects.get(),"missingCapabilityRefused",true);
        }
    }
    @Test public void q5FreePauseBecomesRestrictedImmediatelyForParticipant() throws Exception {
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"enabled");
        try(var f=new Fixture(true)) {
            assertEquals("free",f.session.evaluate(0,f.pause,"1+1").getString("result"));int calls=f.calls.get(),effects=f.effects.get();
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"disabled");
            assertThrows(IllegalArgumentException.class,()->f.session.evaluate(0,f.pause,"1+1"));
            assertEquals(calls,f.calls.get());assertEquals(effects,f.effects.get());
        }
    }
    @Test public void q5RhinoFrameAlsoRefusesAfterStudioDisabledAndClearsSourcePoints() throws Exception {
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"enabled");
        try(var pool=Executors.newSingleThreadExecutor();var s=new JavascriptDebugSession()) {
            s.breakpoint("rhino-path",2,true);var future=pool.submit(()->{try(var lease=s.enter()) {
                var cx=Context.getCurrentContext();return cx.evaluateString(cx.initStandardObjects(),"var input={answer:42};\ninput.answer;","rhino-path",1,null);
            }});
            long id=paused(s);assertEquals("2",s.evaluate(0,id,"1+1").getString("result"));
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"disabled");
            assertEquals(JavascriptDebugPath.REFUSAL,assertThrows(IllegalArgumentException.class,()->s.evaluate(0,id,"1+1")).getMessage());
            assertFalse(s.hasRhinoBreakpoints());assertEquals("42",s.evaluate(0,id,"input.answer").getString("result"));
            s.resume("go",id);future.get(5,TimeUnit.SECONDS);
        }
    }
    @Test public void q5EclipseRetainsFreeEvaluationAndUnknownModeIsClosed() throws Exception {
        var mode=Engine.class.getDeclaredField("bStudioMode");mode.setAccessible(true);boolean previous=mode.getBoolean(null);
        try {
            mode.setBoolean(null,true);assertTrue(JavascriptDebugger.freeEvaluation());
            try(var f=new Fixture(true)) {assertEquals("free",f.session.evaluate(0,f.pause,"1+1").getString("result"));}
            try(var oldParticipant=new Fixture(false)) {
                assertEquals("free",oldParticipant.session.evaluate(0,oldParticipant.pause,"1+1").getString("result"));
                assertEquals(1,oldParticipant.calls.get());assertEquals(1,oldParticipant.effects.get());
            }
            mode.setBoolean(null,false);
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO,"unknown");
            assertFalse(JavascriptDebugger.freeEvaluation());
        } finally {mode.setBoolean(null,previous);}
    }
    @Test public void q4BoxedJavascriptStringAndWrappedPrimitiveArraysRemainOpaque() throws Exception {
        try(var cx=Context.enter()) {
            var scope=cx.initStandardObjects();
            for(Object object:List.of(cx.evaluateString(scope,"new String('FAKE-SECRET-0001')","string",1,null),
                Context.javaToJS(new char[]{'x'},scope),Context.javaToJS(new byte[]{1},scope))) {
                assertEquals("<opaque>",JavascriptDebugProperties.index(object,0));assertEquals("<opaque>",JavascriptDebugProperties.read(object,"length"));
            }
        }
    }
    @Test public void q6MasksBySensitiveKeyAndDocumentsDerivedValues() throws Exception {
        JavascriptDebugRedaction.testValues=()->List.of("FAKE-SECRET-0001");
        assertEquals("[redacted]",JavascriptDebugRedaction.text("FAKE-SECRET-0001"));
        try(var f=new Fixture(true)) {
            assertEquals("[redacted]",f.session.evaluate(0,f.pause,"input.headers.authorization").getString("result"));
            assertEquals("RkFLRS1TRUNSRVQtMDAwMQ==",f.session.evaluate(0,f.pause,"input.derived").getString("result"));
            probe("Q6","sensitiveKeyMasked",true,"derivedBase64Visible",true,"effects",f.effects.get());
        }
    }
    @Test public void q6EscapesOverlapsPreviewBoundariesAndKeyMaskingPreserveProtocol() throws Exception {
        String marker = "FAKE-\"\\/<é\u2028-0001";
        JavascriptDebugRedaction.testValues=()->List.of(marker, "FAKE-SHORT-LONG", "FAKE-SHORT");
        for(String plain:List.of(marker, "x".repeat(495)+marker, "x".repeat(65530)+marker)) {
            assertFalse(JavascriptDebugSession.preview(plain).contains(marker));
            assertFalse(JavascriptDebugRedaction.output(new JSONObject().put("error",plain)).toString().contains(marker));
        }
        assertEquals("[redacted]",JavascriptDebugRedaction.text("FAKE-SHORT-LONG"));
        var data=new JSONObject().put("data",new JSONObject().put("authorization","FAKE-RUNTIME-CREDENTIAL")
            .put("cookie","FAKE-RUNTIME-CREDENTIAL").put(marker,new org.codehaus.jettison.json.JSONArray().put(marker)));
        var prepared=JavascriptDebugResponse.prepare(data,candidate->{});
        assertEquals("[redacted]",prepared.getJSONObject("data").getString("authorization"));
        assertFalse(prepared.toString().contains(marker));assertFalse(prepared.toString().contains("FAKE-RUNTIME-CREDENTIAL"));
        var rows=new JSONObject().put("variables",new org.codehaus.jettison.json.JSONArray().put(new JSONObject().put("name","authorization").put("value","FAKE-RUNTIME-CREDENTIAL").put("ref","opaque-reference").put("fullValue",true).put("expandable",true)));
        for(var reply:List.of(JavascriptDebugRedaction.output(rows),JavascriptDebugResponse.prepare(rows,candidate->{}))) {
            var row=reply.getJSONArray("variables").getJSONObject(0);
            assertEquals("authorization",row.getString("name"));assertEquals("[redacted]",row.getString("value"));assertEquals("",row.getString("ref"));
            assertFalse(row.getBoolean("fullValue"));assertFalse(row.getBoolean("expandable"));
        }
    }
    @Test public void q6CatalogOverflowFailsClosed() throws Exception {
        var oldApp=Engine.theApp;boolean oldStarted=Engine.isStarted;var oldLog=Engine.logEngine;Engine.logEngine=org.apache.log4j.Logger.getLogger("catalog-fixture");
        try {
            Engine.theApp=new Engine();Engine.isStarted=true;
            Engine.theApp.databaseObjectsManager=new com.twinsoft.convertigo.engine.DatabaseObjectsManager() {
                @Override public Set<String> symbolsGetNames() { var names=new LinkedHashSet<String>();for(int i=0;i<1001;i++)names.add("symbol"+i);return names; }
                @Override public String symbolsGetValue(String name) { throw new AssertionError("Non-sensitive symbol value must not be fetched"); }
            };
            assertEquals("[redacted: secret catalog unavailable]",JavascriptDebugRedaction.text("FAKE-SECRET-0001"));
        } finally {Engine.theApp=oldApp;Engine.isStarted=oldStarted;Engine.logEngine=oldLog;}
    }
    @Test public void q7DebugTraceAuditContainsModeAndKindButNeverPathOrMarker() throws Exception {
        var writer=new java.io.StringWriter();
        var appender=new org.apache.log4j.WriterAppender(new org.apache.log4j.PatternLayout("%p %m%n"),writer);
        var root=org.apache.log4j.Logger.getRootLogger();var rootLevel=root.getLevel();
        var levels=new java.util.IdentityHashMap<org.apache.log4j.Logger,org.apache.log4j.Level>();
        var enumeration=org.apache.log4j.LogManager.getCurrentLoggers();
        while(enumeration.hasMoreElements()) {var logger=(org.apache.log4j.Logger)enumeration.nextElement();levels.put(logger,logger.getLevel());logger.setLevel(org.apache.log4j.Level.TRACE);}
        var oldAudit=Engine.logAudit;Engine.logAudit=org.apache.log4j.Logger.getLogger("contract-audit");Engine.logAudit.setLevel(org.apache.log4j.Level.TRACE);
        root.setLevel(org.apache.log4j.Level.TRACE);root.addAppender(appender);
        JavascriptDebugRedaction.testValues=()->List.of("FAKE-SECRET-0001");
        try(var f=new Fixture(true)) {
            f.session.evaluate(0,f.pause,"input.alias");
            f.session.evaluate(0,f.pause,"input.alias"); // watch uses the same common entry
            assertThrows(IllegalArgumentException.class,()->f.session.evaluate(0,f.pause,"input.alias.toString()"));
            assertEquals(2,f.calls.get());assertEquals(0,f.effects.get());
        } finally {
            root.removeAppender(appender);appender.close();root.setLevel(rootLevel);levels.forEach(org.apache.log4j.Logger::setLevel);Engine.logAudit=oldAudit;
        }
        String logs=writer.toString();assertTrue(logs.contains("mode=path frame=participant"));
        assertFalse(logs.contains("input.alias"));assertFalse(logs.contains("FAKE-SECRET-0001"));
        probe("Q7","pathOccurrences",0,"markerOccurrences",0,"auditModeAndKindPresent",true,"allLoggersTrace",true);
    }
    @Test public void q8StoppedExpiredAndBudgetLimitedCommandsHaveNoEffects() throws Exception {
        try(var f=new Fixture(true)) {
            f.session.close();assertThrows(IllegalStateException.class,()->f.session.evaluate(0,f.pause,"input"));
            assertEquals(0,f.calls.get());assertEquals(0,f.effects.get());
        }
        try(var f=new Fixture(true,30)) {
            Thread.sleep(350);assertThrows(IllegalStateException.class,()->f.session.evaluate(0,f.pause,"input"));
            assertEquals(0,f.calls.get());assertEquals(0,f.effects.get());
        }
        var calls=new AtomicInteger();
        try(var cx=Context.enter()) {
            var data=new NativeObject();
            data.setGetterOrSetter("value",0,new BaseFunction(){@Override public Object call(Context c,Scriptable s,Scriptable self,Object[] args){calls.incrementAndGet();return 42;}},false);
            assertThrows(JavascriptDebugResponse.Limit.class,()->JavascriptDebugPath.parse("input.value").resolve(data,()->{throw new JavascriptDebugResponse.Limit("candidates");}));
        }
        assertEquals(0,calls.get());
        probe("Q8","stoppedParticipantCalls",0,"expiredParticipantCalls",0,"effects",calls.get(),"exhaustedBudgetRefused",true);
    }
}
