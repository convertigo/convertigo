/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.mozilla.javascript.*;
import org.codehaus.jettison.json.*;

public class JavascriptDebugInspectionBudgetTest {
    @org.junit.Before public void editingEnabledFixture() throws Exception {
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.WEB_STUDIO,"enabled");
    }

    private long paused(JavascriptDebugSession session) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end) {var s=session.state();if(s.getBoolean("stopped"))return s.getLong("pauseId");Thread.sleep(5);}
        throw new AssertionError("No pause");
    }
    private JSONObject find(JSONArray rows,String name) throws Exception {
        for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getString("name").equals(name))return rows.getJSONObject(i);
        throw new AssertionError("Missing variable");
    }
    public static class Hostile extends HashMap<String,Object> {
        public Hostile(){super();for(int i=0;i<1_000_000;i++)super.put("entry"+i,i);}
        @Override public Set<String> keySet(){throw new AssertionError("No host enumeration");}
        @Override public Set<Map.Entry<String,Object>> entrySet(){throw new AssertionError("No host enumeration");}
        @Override public int size(){throw new AssertionError("No host size");}
        @Override public String toString(){throw new AssertionError("No host toString");}
        public Object getDanger(){throw new AssertionError("No host getter");}
    }
    public static class HostileWrapper extends NativeJavaObject {
        public HostileWrapper(Scriptable scope) { super(scope, new Object(), org.mozilla.javascript.lc.type.TypeInfo.OBJECT); }
        @Override public Object unwrap() { throw new AssertionError("No unknown wrapper callback"); }
        @Override public String toString() { throw new AssertionError("No unknown wrapper callback"); }
    }
    @Test public void unknownRhinoWrapperIsOpaqueWithoutCallingItsUnwrap() throws Exception {
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("bounded",2,true);
            var task=pool.submit(()->{try(var lease=session.enter()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();scope.put("unknown",scope,new HostileWrapper(scope));
                return cx.evaluateString(scope,"var value=unknown;\n42;","bounded",1,null);
            }});
            long id=paused(session);var row=find(session.variables(0,id).getJSONArray("scope"),"unknown");
            assertFalse(row.getBoolean("expandable"));assertEquals("[Host object]",row.getString("value"));
            session.close();task.get(5,TimeUnit.SECONDS);
        }
    }
    @Test public void millionEntryHostMapIsOpaqueAndNeverEnumeratedOrStringified() throws Exception {
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("bounded",2,true);
            var task=pool.submit(()->{try(var lease=session.enter()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                scope.put("map",scope,Context.javaToJS(new Hostile(),scope));
                scope.put("nothing",scope,null);scope.put("missing",scope,Undefined.instance);
                var longNames=new NativeObject();longNames.put("k".repeat(1000000),longNames,42);scope.put("longNames",scope,longNames);
                return cx.evaluateString(scope,"var nested={value:map};\n42;","bounded",1,null);
            }});
            long id=paused(session);var rows=session.variables(0,id).getJSONArray("scope");
            assertFalse(find(rows,"nothing").getBoolean("limited"));assertFalse(find(rows,"missing").getBoolean("limited"));
            var map=find(rows,"map");assertFalse(map.getBoolean("expandable"));assertTrue(map.getBoolean("limited"));
            assertTrue(session.evaluate(0,id,"map").getString("result").startsWith("[Java "));
            var namePage=session.properties(id,find(rows,"longNames").getString("ref"),0);
            assertEquals("<long property name: inspection limited>",namePage.getJSONArray("variables").getJSONObject(0).getString("name"));
            session.resume("go",id);task.get(5,TimeUnit.SECONDS);
        }
    }
    @Test public void evaluationRunTimeIsBoundedByItsOwnDeadlineNotByTheInspectionQuota() throws Exception {
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("bounded",2,true);
            var task=pool.submit(()->{try(var lease=session.enter()){
                var cx=Context.getCurrentContext();return cx.evaluateString(cx.initStandardObjects(),"var value=41;\n42;","bounded",1,null);
            }});
            long id=paused(session);
            var slow=session.evaluate(0,id,"var t=java.lang.System.nanoTime(); while(java.lang.System.nanoTime()-t<120000000){} value+1");
            assertEquals(slow.toString(),"42",slow.getString("result"));assertFalse(slow.optBoolean("refused",false));
            var endless=session.evaluate(0,id,"while(true){}");
            assertEquals("Evaluation timed out.",endless.getString("result"));assertTrue(endless.getBoolean("evaluationError"));
            session.resume("go",id);assertEquals(42,((Number)task.get(5,TimeUnit.SECONDS)).intValue());
        }
    }
    @Test public void participantEvaluationRunTimeIsNotCountedInTheInspectionQuota() throws Exception {
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("fake:slow",1,true);
            var task=pool.submit(()->{try(var lease=session.enter();var suspended=JavascriptDebugger.suspendForFlow()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                // The participant's own evaluation outlasts the 50 ms inspection quota, as a cold or loaded JVM can.
                var inspect=(Function)cx.evaluateString(scope,"(function(id,action,expression){if(action!=='variables'){var t=java.lang.System.nanoTime();"
                    +"while(java.lang.System.nanoTime()-t<120000000){}}return JSON.stringify(action==='variables'?{scopes:[]}:{result:'42'});})","slow-inspect",1,null);
                var plugin=JavascriptDebugger.participant("fake",inspect);
                plugin.enter("x","{\"url\":\"fake:slow\",\"line\":1}");plugin.at("x","before");plugin.leave("x");return 42;
            }});
            long id=paused(session);
            var slow=session.evaluate(0,id,"input");
            assertEquals(slow.toString(),"42",slow.getString("result"));assertFalse(slow.optBoolean("refused",false));
            session.resume("go",id);assertEquals(42,((Number)task.get(5,TimeUnit.SECONDS)).intValue());
        }
    }
    @Test public void hugeRhinoObjectUsesContinuationAndSkipsSlowGetters() throws Exception {
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("bounded",3,true);
            var task=pool.submit(()->{try(var lease=session.enter()){
                var cx=Context.getCurrentContext();return cx.evaluateString(cx.initStandardObjects(),
                    "var object={}; for(var i=0;i<100000;i++)object['n'+i]=i;\nObject.defineProperty(object,'slow',{enumerable:true,get:function(){while(true){} }});\n42;","bounded",1,null);
            }});
            long id=paused(session);var root=find(session.variables(0,id).getJSONArray("scope"),"object");
            var first=session.properties(id,root.getString("ref"),0);
            assertEquals(100,first.getJSONArray("variables").length());assertTrue(first.getBoolean("partial"));
            assertTrue(first.getJSONObject("inspectionBudget").getInt("candidates")<=1000);
            assertFalse(first.getString("cursor").isEmpty());assertEquals("non-atomic",first.getString("view"));
            assertEquals("expired-cursor",session.properties(id,root.getString("ref"),900000).getString("reason"));
            var next=session.properties(id,root.getString("ref"),first.getInt("nextOffset"));
            assertEquals("n100",next.getJSONArray("variables").getJSONObject(0).getString("name"));
            session.close();task.get(5,TimeUnit.SECONDS);
            assertThrows(IllegalStateException.class,()->session.properties(id,root.getString("ref"),200));
        }
    }
    @Test public void formattingDeadlineReplaysUndeliveredRowsBeforeTheDelayedCandidate() throws Exception {
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("bounded",2,true);
            var task=pool.submit(()->{try(var lease=session.enter()){
                var cx=Context.getCurrentContext();return cx.evaluateString(cx.initStandardObjects(),
                    "var object=Array.from({length:250},function(_,i){return i;});\n42;","bounded",1,null);
            }});
            long id=paused(session);String ref=find(session.variables(0,id).getJSONArray("scope"),"object").getString("ref");
            var first=session.properties(id,ref,0);int offset=first.getInt("nextOffset");assertTrue(offset > 0);
            // Controlled test adapter: emulate a cooperative unit delayed by scheduling/GC, not a business iterator.
            var registryField=JavascriptDebugSession.class.getDeclaredField("inspection");registryField.setAccessible(true);
            Object registry=registryField.get(session);var cursorsField=registry.getClass().getDeclaredField("cursors");cursorsField.setAccessible(true);
            Object cursor=((Map<?,?>)cursorsField.get(registry)).get(offset);var iteratorField=cursor.getClass().getDeclaredField("iterator");iteratorField.setAccessible(true);
            var calls=new java.util.concurrent.atomic.AtomicInteger();
            iteratorField.set(cursor,(JavascriptDebugProperties.Cursor)()->{
                int n=calls.getAndIncrement(); if(n==4)return null;
                if(n==3)Thread.sleep(60);
                return new JavascriptDebugProperties.Item("queued"+n,40+n,false,true);
            });
            var partial=session.properties(id,ref,offset);assertEquals("time",partial.getString("reason"));assertEquals(0,partial.getJSONArray("variables").length());
            assertTrue(partial.getInt("nextOffset") > 0);assertNotEquals(offset,partial.getInt("nextOffset"));
            var next=session.properties(id,ref,partial.getInt("nextOffset"));
            assertEquals(4,next.getJSONArray("variables").length());
            for(int n=0;n<4;n++) {
                assertEquals("queued"+n,next.getJSONArray("variables").getJSONObject(n).getString("name"));
                assertEquals(Integer.toString(40+n),next.getJSONArray("variables").getJSONObject(n).getString("value"));
            }
            assertEquals(5,calls.get());

            session.close();task.get(5,TimeUnit.SECONDS);
        }
    }
    @Test public void concurrentMutationInvalidatesOrAnnouncesNonAtomicView() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2);var session=new JavascriptDebugSession()) {
            var object=new NativeObject();for(int i=0;i<2500;i++)object.put("n"+i,object,i);
            session.breakpoint("bounded",2,true);
            var task=pool.submit(()->{try(var lease=session.enter()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();scope.put("object",scope,object);
                return cx.evaluateString(scope,"var value=object;\n42;","bounded",1,null);
            }});
            long id=paused(session);var ref=find(session.variables(0,id).getJSONArray("scope"),"object").getString("ref");
            var first=session.properties(id,ref,0);
            pool.submit(()->{object.put("added",object,42);}).get();
            var second=session.properties(id,ref,first.getInt("nextOffset"));
            assertEquals("non-atomic",second.getString("view"));
            session.close();task.get(5,TimeUnit.SECONDS);
        }
    }
    @Test public void oneQuotaIncludesIgnoredSlotsAcrossAllParticipantScopes() throws Exception {
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("fake:bounded",1,true,false);
            var task=pool.submit(()->{try(var lease=session.enter();var suspended=JavascriptDebugger.suspendForFlow()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                cx.evaluateString(scope,"var object={};for(var i=0;i<3000;i++)Object.defineProperty(object,'hidden'+i,{value:i});","data",1,null);
                var callback=(org.mozilla.javascript.Function)cx.evaluateString(scope,
                    "(function(){var pages=[];for(var n=0;n<6;n++)pages.push(JSON.parse(String(plugin.scope('group'+n,object))));return JSON.stringify({scopes:pages});})","callback",1,null);
                var plugin=JavascriptDebugger.participant("fake",callback);scope.put("plugin",scope,Context.javaToJS(plugin,scope));
                plugin.enter("x","{\"url\":\"fake:bounded\",\"line\":1}");plugin.at("x","before");plugin.leave("x");return true;
            }});
            long id=paused(session);var result=session.variables(0,id);var quota=result.getJSONObject("inspectionBudget");
            assertTrue(quota.getInt("candidates")<=1000);assertTrue(Set.of("candidates","time").contains(quota.getString("reason")));
            assertTrue(quota.isNull("cpuNanos"));session.close();task.get(5,TimeUnit.SECONDS);
        }
    }
    @Test public void clientTimeoutAndEvaluationMarkerDoNotInterruptCompiledCallbackButStopIsImmediate() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2);var session=new JavascriptDebugSession()) {
            session.breakpoint("fake:blocking",1,true,false);
            var task=pool.submit(()->{try(var lease=session.enter();var suspended=JavascriptDebugger.suspendForFlow()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                cx.setInterpretedMode(false);
                var callback=new BaseFunction(){@Override public Object call(Context context,Scriptable s,Scriptable self,Object[] args){
                    entered.countDown();try{release.await(6,TimeUnit.SECONDS);}catch(InterruptedException e){throw new AssertionError(e);}return "{\"result\":\"42\"}";
                }};
                callback.setParentScope(scope);
                var plugin=JavascriptDebugger.participant("fake",callback);plugin.enter("x","{\"url\":\"fake:blocking\",\"line\":1}");
                plugin.at("x","before");plugin.leave("x");return true;
            }});
            long id=paused(session);var command=pool.submit(()->session.evaluate(0,id,"local.value"));
            assertTrue(entered.await(2,TimeUnit.SECONDS));Thread.sleep(600);assertFalse(command.isDone()); // marker is not a preemption mechanism
            try{command.get(4,TimeUnit.SECONDS);fail("client wait should time out");}catch(ExecutionException expected){assertTrue(expected.getCause() instanceof TimeoutException);}
            assertFalse(task.isDone()); // 3 seconds only bounded the caller, not worker work
            long start=System.nanoTime();session.close();assertTrue(System.nanoTime()-start<TimeUnit.MILLISECONDS.toNanos(250));
            assertFalse(task.isDone());release.countDown();assertTrue(task.get(3,TimeUnit.SECONDS));
        } finally {release.countDown();}
    }
    @Test public void allOutputPathsMaskKnownSecretCopiesAndErrors() throws Exception {
        String sentinel=UUID.randomUUID().toString();JavascriptDebugRedaction.testValues=()->List.of(sentinel);
        double numeric=1e100+new java.security.SecureRandom().nextDouble()*1e99;
        String numericSecret=org.mozilla.javascript.ScriptRuntime.numberToString(numeric,10);
        JavascriptDebugRedaction.testValues=()->List.of(sentinel,numericSecret);
        assertTrue("Numeric preview must mask a known protected value","[redacted]".equals(JavascriptDebugSession.preview(numeric)));
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            session.breakpoint("bounded",2,true);
            var task=pool.submit(()->{try(var lease=session.enter()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();scope.put("privateValue",scope,sentinel);
                return cx.evaluateString(scope,"var copied={value:privateValue};\n42;","bounded",1,null);
            }});
            long id=paused(session);var vars=session.variables(0,id);assertFalse("Raw secret in scopes",vars.toString().contains(sentinel));
            var ref=find(vars.getJSONArray("scope"),"copied").getString("ref");
            assertFalse("Raw secret in page",session.properties(id,ref,0).toString().contains(sentinel));
            assertEquals("[redacted]",session.evaluate(0,id,"privateValue").getString("result"));
            assertFalse("Raw secret in evaluation error",session.evaluate(0,id,"throw new Error(privateValue)").toString().contains(sentinel));
            assertFalse("Raw secret in events",session.state().toString().contains(sentinel));
            session.close();task.get(5,TimeUnit.SECONDS);
        } finally {JavascriptDebugRedaction.testValues=null;}
    }
    @Test public void identityInvalidationsAndChildFrameMetadataAreMaskedWithoutMutatingTheRegistry() throws Exception {
        String sentinel=UUID.randomUUID().toString();JavascriptDebugRedaction.testValues=()->List.of(sentinel);
        try(var pool=Executors.newSingleThreadExecutor();var session=new JavascriptDebugSession()) {
            String oldUrl="opaque:"+sentinel+":old";
            session.participantSources("test",new JSONObject().put("id","source").put("revision","old")
                .put("targets",new JSONArray().put(new JSONObject().put("url",oldUrl).put("line",1).put("key","same"))).toString());
            session.breakpoint(oldUrl,1,true,false);
            assertTrue("Protocol address must remain usable",session.state().getJSONObject("breakpoints").has(oldUrl));
            // Address keys are protocol handles; the invalidation display string is masked below.
            assertEquals(1,session.breakpoints(oldUrl).length()); // output masking never renames the internal key
            session.participantSources("test",new JSONObject().put("id","source").put("revision","new")
                .put("targets",new JSONArray()).toString());
            var changed=session.state();assertTrue(changed.getJSONArray("breakpointChanges").length()>0);
            assertFalse("Secret in invalidation notice",changed.toString().contains(sentinel));
            session.breakpoint("fake:metadata",1,true,false);
            var task=pool.submit(()->{try(var lease=session.enter();var suspended=JavascriptDebugger.suspendForFlow()){
                var cx=Context.getCurrentContext();var scope=cx.initStandardObjects();
                var callback=new BaseFunction(){@Override public Object call(Context c,Scriptable s,Scriptable self,Object[] args){
                    return JavascriptDebugSession.json("result",sentinel).toString();
                }};callback.setParentScope(scope);
                var plugin=JavascriptDebugger.participant("test",callback);
                plugin.enter("x",new JSONObject().put("url","fake:metadata").put("line",1).put("label",sentinel).put("source",sentinel).put("location",new JSONObject().put("display",sentinel).put("identity",new JSONObject().put("id","opaque-source"))).toString());
                plugin.at("x","before");plugin.leave("x");return true;
            }});
            long id=paused(session);
            assertFalse("Secret in activation metadata",session.state().toString().contains(sentinel));
            assertFalse("Secret in source response",session.source("fake:metadata").toString().contains(sentinel));
            assertFalse("Secret in participant evaluation",session.evaluate(0,id,"path").toString().contains(sentinel));
            session.close();assertTrue(task.get(5,TimeUnit.SECONDS));
        } finally {JavascriptDebugRedaction.testValues=null;}
    }

    @Test public void serviceParameterErrorsMaskKnownValuesWithoutLoggingThePayload() throws Exception {
        String sentinel=UUID.randomUUID().toString();JavascriptDebugRedaction.testValues=()->List.of(sentinel);
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        var property=com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.JAVASCRIPT_DEBUGGER;
        var previous=com.twinsoft.convertigo.engine.EnginePropertiesManager.getProperty(property);
        com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(property,"true");
        var writer=new java.io.StringWriter();var status=new java.util.concurrent.atomic.AtomicInteger();
        var oldManager=com.twinsoft.convertigo.engine.Engine.authenticatedSessionManager;
        var oldLog=com.twinsoft.convertigo.engine.Engine.logAdmin;
        com.twinsoft.convertigo.engine.Engine.authenticatedSessionManager=new com.twinsoft.convertigo.engine.AuthenticatedSessionManager();
        com.twinsoft.convertigo.engine.Engine.logAdmin=org.apache.log4j.Logger.getLogger("inspection-qualification");
        try {
            var session=(jakarta.servlet.http.HttpSession)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{jakarta.servlet.http.HttpSession.class},(proxy,method,args)->switch(method.getName()){
                case "getId"->"service-mask";
                case "getAttribute"->new com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role[]{com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role.WEB_ADMIN};
                default->null;
            });
            var parameters=Map.of("action","properties","pauseId","1","ref","reference","offset",sentinel);
            var request=(jakarta.servlet.http.HttpServletRequest)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{jakarta.servlet.http.HttpServletRequest.class},(proxy,method,args)->switch(method.getName()){
                case "getSession"->session;case "getMethod"->"POST";case "getParameter"->parameters.get(args[0]);default->null;
            });
            var response=(jakarta.servlet.http.HttpServletResponse)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{jakarta.servlet.http.HttpServletResponse.class},(proxy,method,args)->{
                if(method.getName().equals("getWriter"))return new java.io.PrintWriter(writer);
                if(method.getName().equals("setStatus"))status.set((Integer)args[0]);return null;
            });
            JavascriptDebugger.session("service-mask",true);
            new com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger().run("studio.debug.Debugger",request,response);
            assertEquals(400,status.get());assertFalse("Raw protected value in service error",writer.toString().contains(sentinel));
            assertTrue("Service error should carry the mask",writer.toString().contains("[redacted]"));
        } finally {
            JavascriptDebugger.stop("service-mask");JavascriptDebugRedaction.testValues=null;
            com.twinsoft.convertigo.engine.Engine.authenticatedSessionManager=oldManager;
            com.twinsoft.convertigo.engine.Engine.logAdmin=oldLog;
            com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(property,previous);
        }
    }

}
