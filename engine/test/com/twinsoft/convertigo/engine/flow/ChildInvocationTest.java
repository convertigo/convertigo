/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.*;
import org.mozilla.javascript.ScriptableObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.engine.*;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.enums.*;
import com.twinsoft.convertigo.engine.requesters.*;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/** Real internal policy and real ticket construction/queue; no external billing sink. */
public class ChildInvocationTest {
    Engine previous;
    Project project;
    java.nio.file.Path directory;
    Flow child;
    Context parent;
    List<Ticket> tickets;
    final Map<String,String> properties = new HashMap<>();
    static void field(Object target, String name, Object value) throws Exception {
        var f = BillingManager.class.getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    @Before public void setup() throws Exception {
        for (var key : List.of("convertigo.javascript.stateless", "convertigo.javascript.direct_calls")) properties.put(key, System.getProperty(key));
        System.setProperty("convertigo.javascript.stateless", "true"); System.clearProperty("convertigo.javascript.direct_calls");
        EnginePropertiesManager.initProperties();
        for (var f : Engine.class.getFields()) if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == org.apache.log4j.Logger.class)
            f.set(null, org.apache.log4j.Logger.getLogger("child-invocation-test"));
        previous = Engine.theApp; Engine.theApp = new Engine();
        Engine.theApp.pluginsManager = new PluginsManager();
        Engine.theApp.billingManager = new BillingManager();
        tickets = new CopyOnWriteArrayList<>();
        field(Engine.theApp.billingManager, "isDestroying", false);
        field(Engine.theApp.billingManager, "tickets", tickets);
        field(Engine.theApp.billingManager, "managers", Collections.singletonList(null));
        directory = java.nio.file.Files.createTempDirectory("child-invocation-");
        project = new Project() { @Override public String getDirPath() { return directory.toString(); } @Override public java.io.File getDirFile() { return directory.toFile(); } @Override public ClassLoader getProjectClassLoader() { return getClass().getClassLoader(); } };
        project.setName("Fixture");
        Engine.theApp.databaseObjectsManager=new DatabaseObjectsManager(){
            {symbolsProperties=new java.util.Properties();}
            @Override public Project getOriginalProjectByName(String name,boolean openable) {return project;}
            @Override public Project getLoadedProjectByName(String name) {return project;}
        };
        child = new Flow(); child.setName("Child"); child.setFlowSource("function Child(){return {};}" ); child.setResponseTimeout(60); project.add(child);
        parent = new Context("root"); parent.project = project; parent.projectName = project.getName();
        parent.httpServletRequest = new InternalHttpServletRequest(); parent.httpSession = parent.httpServletRequest.getSession();
        parent.remoteAddr = parent.httpServletRequest.getRemoteAddr(); parent.userAgent = parent.httpServletRequest.getHeader("User-Agent");
        EnginePropertiesManager.setProperty(EnginePropertiesManager.PropertyName.NATIVE_INLINE_CALLS,"true");
        EnginePropertiesManager.setProperty(EnginePropertiesManager.PropertyName.DOCUMENT_THREADING_USE_STOP_METHOD,"false");
    }
    @After public void cleanup() throws Exception { FlowEngineBridge.clearCaches(); org.apache.commons.io.FileUtils.deleteDirectory(directory.toFile()); Engine.theApp = previous; properties.forEach((k,v)->{if(v==null)System.clearProperty(k);else System.setProperty(k,v);}); Log4jHelper.mdcClear(); }
    @FunctionalInterface interface Action { void run(Flow owner) throws Exception; }
    void root(int timeout, Action action) throws Exception {
        var failure = new AtomicReference<Throwable>();
        var owner = new Flow() {
            @Override public void fireRequestableEvent(String event) { }
            @Override public void runCore() throws EngineException { try { action.run(this); } catch(Throwable e) { failure.set(e); } }
        };
        owner.setName("Root"); owner.setResponseTimeout(timeout); owner.setParent(project);
        parent.nativeJson = true; parent.nativeInput = Map.of(); parent.requestedObject = owner;
        var requester = new NativeJsonRequester(Map.of("__project","Fixture"), parent.httpServletRequest); requester.context = parent;
        Log4jHelper.mdcInit(parent);
        owner.run(requester, parent);
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    NativeJsonRequester legacy() throws Exception {
        var ctx = new Context("legacy"); ctx.project = project; ctx.projectName = project.getName(); ctx.sequenceName = child.getName();
        ctx.requestedObject = child; ctx.httpSession = parent.httpSession; ctx.httpServletRequest = parent.httpServletRequest;
        ctx.remoteAddr = parent.remoteAddr; ctx.userAgent = parent.userAgent;
        var requester = new NativeJsonRequester(Map.of("__project","Fixture"), parent.httpServletRequest); requester.context = ctx; return requester;
    }
    @Test public void samePolicyOrderAndShortCircuit() throws Exception {
        var calls = new ArrayList<Integer>();
        for(int reject=0;reject<=4;reject++) {
            calls.clear(); final int failAt=reject;
            RequestableInvocation.Check[] checks = new RequestableInvocation.Check[4];
            for(int i=0;i<4;i++){final int n=i+1;checks[i]=()->{calls.add(n);if(n==failAt)throw new EngineException("refused");};}
            try { RequestableInvocation.authorize(checks[0],checks[1],checks[2],checks[3]); assertEquals(0,reject); }
            catch(EngineException e){assertEquals("refused",e.getMessage());}
            assertEquals(reject==0?4:reject,calls.size());
        }
    }
    @Test public void internalPrivateTlsAndAuthenticationMatchAndBillEvenRefusals() throws Exception {
        child.setAccessibility(Accessibility.Private); child.setSecureConnectionRequired(true); child.setAuthenticatedContextRequired(true);
        root(60, owner -> {
            var requester = legacy();
            try { assertThrows(EngineException.class,()->RequestableInvocation.authorize(requester)); }
            finally { RequestableInvocation.bill(requester.context); }
            try(var invocation = new ChildInvocation(child,parent,Flow.currentInvocationGuard())) {
                var error=assertThrows(EngineException.class,invocation::authorize); assertEquals("Authentication required",error.getMessage());
            }
            SessionAttribute.authenticatedUser.set(parent.httpSession,"fixture-user");
            RequestableInvocation.authorize(requester);
            try(var invocation = new ChildInvocation(child,parent,Flow.currentInvocationGuard())) {
                invocation.authorize(); assertEquals("fixture-user",invocation.getAuthenticatedUser());
            }
        });
        assertEquals(3,tickets.size()); assertEquals("user",tickets.get(0).getUserName()); assertEquals("user",tickets.get(1).getUserName());
        assertEquals("fixture-user",tickets.get(2).getUserName());
    }
    @Test public void identitySessionAttributesTicketsAndThreadStateArePreserved() throws Exception {
        root(60, owner -> {
            org.apache.log4j.MDC.put("marker","parent");
            var loader=Thread.currentThread().getContextClassLoader();
            var requester=legacy();
            parent.httpSession.setAttribute(Parameter.UIid.name(),"fixture-ui");
            parent.httpSession.setAttribute(Parameter.DeviceUUID.name(),"fixture-device");
            requester.context.set("username","fallback-user");
            RequestableInvocation.bill(requester.context);
            ChildInvocation invocation;
            try(var c=invocation=new ChildInvocation(child,parent,Flow.currentInvocationGuard())) {
                c.authorize(); assertSame(parent.httpSession,c.httpSession); assertSame(parent.httpServletRequest,c.httpServletRequest);
                assertEquals("Fixture",c.projectName);assertEquals("Child",c.sequenceName);assertTrue(c.name.startsWith("flow-"));
                assertEquals(c.httpSession.getId()+"_"+c.name,c.contextID);assertNull(c.parentContext);
                c.set("username","fallback-user"); c.set("local",7); assertNull(parent.get("local")); c.remove("local"); assertNull(c.get("local"));
                c.setAuthenticatedUser("new-user");assertEquals("new-user",parent.getAuthenticatedUser());c.removeAuthenticatedUser();
                c.setResponseHeader("X-Fixture","yes");c.setResponseStatus(202,"fixture");
                assertEquals("yes",((Map<?,?>)RequestAttribute.responseHeader.get(parent.httpServletRequest)).get("X-Fixture"));
                assertEquals("fixture",((Map<?,?>)RequestAttribute.responseStatus.get(parent.httpServletRequest)).get(202));
                org.apache.log4j.MDC.put("marker","child");Thread.currentThread().setContextClassLoader(null);
            }
            invocation.close();assertEquals("parent",org.apache.log4j.MDC.get("marker"));assertSame(loader,Thread.currentThread().getContextClassLoader());
        });
        assertEquals(2,tickets.size());
        for (var method : Ticket.class.getMethods()) if(method.getName().startsWith("get") && method.getParameterCount()==0
                && !Set.of("getCreationDate","getResponseTime","getClass").contains(method.getName()))
            assertEquals(method.getName(),method.invoke(tickets.get(0)),method.invoke(tickets.get(1)));
    }
    @Test public void framesAreNestedAndErrorsCloseAndBillOnce() throws Exception {
        root(60, owner -> {
            try(var cx=org.mozilla.javascript.Context.enter()) {
                cx.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                var api=new FlowRuntimeApi(cx);
                try(var frame=api.enter(cx)) {
                    frame.scope.put("context",frame.scope,org.mozilla.javascript.Context.javaToJS(parent,frame.scope));
                    var input=cx.newObject(frame.scope);
                    assertNull(api.childEligibility(child,input));
                    var nested=api.openChild(child,input,false,System.nanoTime());
                    try(nested) {
                        assertTrue(nested.isRunning());assertSame(child,nested.requestedObject);assertNull(child.context);
                        try(var deeper=api.openChild(child,input,false,System.nanoTime())) {assertNotEquals(nested.contextID,deeper.contextID);deeper.abortRequestable();assertFalse(deeper.isRunning());}
                        assertTrue(nested.isRunning());throw new IllegalStateException("business");
                    } catch(IllegalStateException expected){assertEquals("business",expected.getMessage());}
                    nested.close();assertSame(parent,((org.mozilla.javascript.Wrapper)ScriptableObject.getProperty(api.sharedScope(),"context")).unwrap());
                }
                assertFalse(api.sharedScope().has("context",api.sharedScope()));
            }
        }); assertEquals(2,tickets.size());
    }
    @Test public void reservedInputsAndUncoveredModesFallBackBeforeExecution() throws Exception {
        root(60, owner -> {
            try(var cx=org.mozilla.javascript.Context.enter()) {
                var api=new FlowRuntimeApi(cx);try(var frame=api.enter(cx)){
                    frame.scope.put("context",frame.scope,org.mozilla.javascript.Context.javaToJS(parent,frame.scope));var input=cx.newObject(frame.scope);
                    input.put("__body",input,"{}");assertEquals("reserved-input",api.childEligibility(child,input));input.delete("__body");
                    child.setResponseTimeout(1);assertEquals("independent-deadline",api.childEligibility(child,input));child.setResponseTimeout(60);
                    parent.httpSession.setAttribute("customController",new Object());assertEquals("custom-controller",api.childEligibility(child,input));parent.httpSession.removeAttribute("customController");
                    var other=new Project();other.setName("Other");child.setParent(other);assertEquals("project-generation",api.childEligibility(child,input));child.setParent(project);
                    child.setIncludeTrace(true);assertEquals("trace",api.childEligibility(child,input));child.setIncludeTrace(false);
                    parent.isStubRequested=true;assertEquals("parent-mode",api.childEligibility(child,input));parent.isStubRequested=false;
                    Engine.theApp.pluginsManager=new PluginsManager(){@Override public synchronized boolean hasPlugins(){return true;}};
                    assertEquals("lifecycle-plugins",api.childEligibility(child,input));Engine.theApp.pluginsManager=new PluginsManager();
                    assertNull(api.childEligibility(child,input));
                    FlowEngineBridge.clearCaches();assertEquals("revision",api.childEligibility(child,input));
                }
            }
        }); assertTrue(tickets.isEmpty());
    }
    @Test public void cancellationAndDeadlineAreInherited() throws Exception {
        root(60, owner -> {try(var c=new ChildInvocation(child,parent,Flow.currentInvocationGuard())){assertTrue(c.isRunning());owner.runningThread.bContinue=false;assertFalse(c.isRunning());assertThrows(EngineException.class,c::authorize);owner.runningThread.bContinue=true;}});
        var ended=new CountDownLatch(1);
        try { root(1, owner -> {try(var c=new ChildInvocation(child,parent,Flow.currentInvocationGuard())){assertTrue(c.isRunning());while(System.nanoTime()<Flow.currentInvocationGuard().deadlineNanos())Thread.sleep(2);assertFalse(c.isRunning());}finally{ended.countDown();}}); }
        catch(com.twinsoft.convertigo.beans.core.TransactionTimeoutException expected) { }
        assertTrue(ended.await(3,TimeUnit.SECONDS));
    }
    @Test public void billingFailureCannotReplaceBusinessFailure() throws Exception {
        Engine.theApp.billingManager=new BillingManager(){@Override public void insertBilling(RequestableInvocation.Billing value)throws EngineException{throw new EngineException("fixture billing failure");}};
        root(60, owner -> {try(var c=new ChildInvocation(child,parent,Flow.currentInvocationGuard())){throw new IllegalArgumentException("business");}catch(IllegalArgumentException e){assertEquals("business",e.getMessage());}});
    }
    static String cacheInfo() {try {var m=FlowEngineBridge.class.getDeclaredMethod("bridgeCacheInfo",String.class);m.setAccessible(true);return m.invoke(null,"lib_flow_engine.Engine").toString();}catch(Exception e){return e.getClass().getName();}}
    void provider() throws Exception {
        var resource=System.getenv("FLOW_ENGINE_RESOURCE_ROOT");Assume.assumeNotNull(resource);
        var library=new Project(){@Override public String getDirPath(){return java.nio.file.Path.of(resource).getParent().toString();}};library.setName("lib_flow_engine");
        Engine.theApp.databaseObjectsManager=new DatabaseObjectsManager(){
            {symbolsProperties=new java.util.Properties();}
            @Override public Project getOriginalProjectByName(String name,boolean openable){return "lib_flow_engine".equals(name)?library:project;}
            @Override public Project getLoadedProjectByName(String name){return "lib_flow_engine".equals(name)?library:project;}
            @Override public com.twinsoft.convertigo.beans.core.DatabaseObject getDatabaseObjectByQName(String name){return "Fixture.Child".equals(name)?child:null;}
        };
        Engine.theApp.contextManager=new ContextManager(){@Override public Context get(Requester r,String n,String id,String pool,String p,String c,String s){throw new AssertionError("Unexpected managed child: "+s+" "+RequestableInvocation.statistics()+" shared="+cacheInfo());}};
    }
    @Test public void actualProviderRequestableBlockUsesDirectContractAndNoHistoricalRuntime() throws Exception {
        provider();
        child.setFlowSource("const _flow={sourceVersion:2,invocation:'direct-v1',inputs:{payload:{type:'object'}},authenticatedContextRequired:true};\nfunction Child({input}){return input.payload;}");
        SessionAttribute.authenticatedUser.set(parent.httpSession,"fixture-user");
        root(60, owner -> {
            owner.setFlowSource("function Root({result}){result.child=requestable.call({requestable:'.Child',input:{payload:{n:3,values:[true,null]}}});return result;}");
            var response=new FlowEngineBridge().run(owner,parent,null,null);
            assertEquals(response.toString(),Boolean.TRUE,response.get("ok"));
            var result=(Map<?,?>)response.get("result");var value=(Map<?,?>)result.get("child");
            assertNotNull(result.toString(),value);assertFalse(value.toString(),value.containsKey("error"));assertEquals(3,((Number)value.get("n")).intValue());
        });
        assertEquals(1,tickets.size());assertEquals("Child",tickets.get(0).getRequestableName());
        var method=FlowEngineBridge.class.getDeclaredMethod("bridgeCacheInfo",String.class);method.setAccessible(true);
        var info=new org.codehaus.jettison.json.JSONObject(method.invoke(null,"lib_flow_engine.Engine").toString());
        assertEquals(info.toString(),0,info.optInt("runtimeCacheSize",-1));
    }

    public static final AtomicInteger handlesClosed = new AtomicInteger();
    void proofBlock(String name,String body) throws Exception {
        var file=directory.resolve("_flow/blocks/proof/"+name+".block.js");java.nio.file.Files.createDirectories(file.getParent());
        java.nio.file.Files.writeString(file,"const _meta={sourceVersion:2,runtime:'rhino',properties:{},outputs:{out:{type:'object'}}};\n(function(){return {run:function(ctx){"+body+"}};})()");
    }
    Map<String,Object> execute(String source) throws Exception {
        var answer=new AtomicReference<Map<String,Object>>();
        root(60,owner->{owner.setFlowSource(source);answer.set(new FlowEngineBridge().run(owner,parent,null,null));});
        return answer.get();
    }
    void allowManagedCalls() {
        Engine.theApp.cacheManager=new com.twinsoft.convertigo.engine.cache.FileCacheManager();
        Engine.theApp.contextManager=new ContextManager(){
            @Override public Context get(Requester r,String name,String id,String pool,String p,String c,String s)throws Exception {
                var copy=child.clone();copy.setParent(project);
                var context=new Context(id+"_"+name){@Override public void loadSequence(){requestedObject=copy;}};
                context.name=name;context.project=project;context.projectName=project.getName();context.sequenceName=s;return context;
            }
            @Override public void remove(Context context){context.isDestroying=true;}
            @Override public void remove(Context context,jakarta.servlet.http.HttpSession session){context.isDestroying=true;}
        };
    }
    @Test public void actualBlockPassesTypedObjectsByReferenceAndExcludesUndeclaredContracts() throws Exception {
        provider();proofBlock("identity","var value={n:7};var target=Packages.com.twinsoft.convertigo.engine.Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName('Fixture.Child');var direct=ctx.tryDirectRequestable(target,{payload:value});var reply=direct&&direct.handled?direct.value:ctx.callBlock('requestable.call',{requestable:'.Child',input:{payload:value}});return {same:reply===value,value:reply};");
        child.setFlowSource("const _flow={sourceVersion:2,invocation:'direct-v1',inputs:{payload:{type:'object'}}};\nfunction Child({input}){return input.payload;}");
        var response=execute("function Root(){local.value=proof.identity({});return local.value;}");
        assertEquals(response.toString(),Boolean.TRUE,response.get("ok"));var result=(Map<?,?>)response.get("result");assertEquals(result.toString(),Boolean.TRUE,result.get("same"));
        assertEquals(1,tickets.size());
        allowManagedCalls();System.setProperty("convertigo.javascript.direct_calls","false");
        var managed=execute("function Root(){local.value=proof.identity({});return local.value;}");
        assertEquals(managed.toString(),Boolean.TRUE,managed.get("ok"));var managedResult=(Map<?,?>)managed.get("result");assertEquals(managedResult.toString(),Boolean.FALSE,managedResult.get("same"));
        assertEquals(result.get("value"),managedResult.get("value"));assertEquals(2,tickets.size());
    }
    @Test public void actualChildBusinessFailuresCloseHandlesAndBillInBothPaths() throws Exception {
        provider();allowManagedCalls();handlesClosed.set(0);
        proofBlock("failure","ctx.createHandle('fixture',{}, {close:function(){Packages.com.twinsoft.convertigo.engine.flow.ChildInvocationTest.handlesClosed.incrementAndGet();}});ctx.raise('FIXTURE_FAILURE','fixture error');");
        child.setFlowSource("const _flow={sourceVersion:2,invocation:'direct-v1'};\nfunction Child(){local.value=proof.failure({});return local.value;}");
        for(var direct:List.of("true","false")) {
            System.setProperty("convertigo.javascript.direct_calls",direct);
            var response=execute("function Root(){local.value=requestable.call({requestable:'.Child',input:{}});return local.value;}");
            assertEquals(response.toString(),Boolean.TRUE,response.get("ok"));
            var result=(Map<?,?>)response.get("result");assertEquals(result.toString(),"FIXTURE_FAILURE",((Map<?,?>)result.get("error")).get("code"));
        }
        assertEquals(2,handlesClosed.get());assertEquals(2,tickets.size());
    }
    @Test public void actualAuthenticationRefusalsBillInBothPathsBeforeAnyBlock() throws Exception {
        provider();allowManagedCalls();handlesClosed.set(0);
        proofBlock("never","Packages.com.twinsoft.convertigo.engine.flow.ChildInvocationTest.handlesClosed.incrementAndGet();return {};");
        child.setFlowSource("const _flow={sourceVersion:2,invocation:'direct-v1',authenticatedContextRequired:true};\nfunction Child(){local.value=proof.never({});return local.value;}");
        for(var direct:List.of("true","false")) {
            System.setProperty("convertigo.javascript.direct_calls",direct);
            var response=execute("function Root(){local.value=requestable.call({requestable:'.Child',input:{}});return local.value;}");
            // Managed requesters adapt authorization exceptions into an error envelope.
            assertTrue(response.toString(),Boolean.FALSE.equals(response.get("ok")) || ((Map<?,?>)response.get("result")).containsKey("error"));
        }
        assertEquals(0,handlesClosed.get());assertEquals(2,tickets.size());
    }

    @Test public void scalarCoercionLargeIntegersDefaultsAndUndeclaredInputsMatchRequester() throws Exception {
        provider();allowManagedCalls();
        child.setFlowSource("const _flow={sourceVersion:2,invocation:'direct-v1',inputs:{huge:{type:'integer'},flag:{type:'boolean'},count:{type:'integer',default:4},nil:{type:'object',default:null}}};\nfunction Child({input}){return input;}");
        Map<?,?> previous=null;
        for(var direct:List.of("true","false")) {
            System.setProperty("convertigo.javascript.direct_calls",direct);
            var response=execute("function Root(){local.value=requestable.call({requestable:'.Child',input:{huge:1000000000000000100,flag:true,count:null,extra:{n:3}}});return local.value;}");
            assertEquals(response.toString(),Boolean.TRUE,response.get("ok"));
            var result=(Map<?,?>)response.get("result");assertFalse(result.toString(),result.containsKey("error"));
            assertTrue(result.toString(),result.get("huge") instanceof String);assertEquals(4,((Number)result.get("count")).intValue());
            assertEquals("{\"n\":3}",result.get("extra"));assertNull(result.get("nil"));
            if(previous!=null)assertEquals(previous,result);previous=result;
        }
        assertEquals(2,tickets.size());
    }

    @Test public void parentStoppingImmediatelyBeforeOpenCannotStartAFallbackWorker() throws Exception {
        root(60,owner->{
            try(var cx=org.mozilla.javascript.Context.enter()) {
                var api=new FlowRuntimeApi(cx);try(var frame=api.enter(cx)){
                    frame.scope.put("context",frame.scope,org.mozilla.javascript.Context.javaToJS(parent,frame.scope));
                    var input=cx.newObject(frame.scope);assertNull(api.childEligibility(child,input));
                    owner.runningThread.bContinue=false;
                    try {
                        var error=assertThrows(EngineException.class,()->api.openChild(child,input,false,System.nanoTime()));
                        assertEquals("Parent context stopped",error.getMessage());
                        assertSame(parent,((org.mozilla.javascript.Wrapper)ScriptableObject.getProperty(api.sharedScope(),"context")).unwrap());
                        assertEquals(1,tickets.size());
                    } finally {owner.runningThread.bContinue=true;}
                }
            }
        });
    }

}
