package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class JavascriptDebuggerTest {
    @Test public void editorPropertyNamesMatchEngineSourcesIncludingSmartTypes() throws Exception {
        assertEquals("expression", com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger.scriptName(
                new com.twinsoft.convertigo.beans.steps.SimpleStep(), "expression"));
        assertEquals("LogStep", com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger.scriptName(
                new com.twinsoft.convertigo.beans.steps.LogStep(), "expression"));
        assertEquals("message", com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger.scriptName(
                new com.twinsoft.convertigo.beans.steps.ExceptionStep(), "expression"));
        assertEquals("ctxName", com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger.scriptName(
                new com.twinsoft.convertigo.beans.steps.SequenceStep(), "contextName"));
        assertEquals("value-smartType", com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger.scriptName(
                new com.twinsoft.convertigo.beans.steps.JsonFieldStep(), "value"));
        var transaction = new com.twinsoft.convertigo.beans.transactions.SqlTransaction(); transaction.setName("Handlers");
        assertEquals("Handlers", com.twinsoft.convertigo.engine.admin.services.studio.debug.Debugger.scriptName(transaction, "handlers"));
    }

    @Test public void onlyExplicitEclipseLaunchesJoinInStudioAndBusyRootsAreRefused() throws Exception {
        EnginePropertiesManager.initProperties();
        Engine.logAdmin = Logger.getLogger("debugger-test");
        String webStudio = EnginePropertiesManager.getProperty(PropertyName.WEB_STUDIO);
        String debuggerEnabled = EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_DEBUGGER);
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, "enabled");
        EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "true");
        var mode = Engine.class.getDeclaredField("bStudioMode"); mode.setAccessible(true);
        boolean previous = mode.getBoolean(null);
        var request = new com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest();
        try {
            mode.setBoolean(null, false);
            assertThrows(IllegalStateException.class, () -> JavascriptDebugger.studioRequests("eclipse", true));
            mode.setBoolean(null, true);
            var session = JavascriptDebugger.session("eclipse", true);
            session.breakpoint("eclipse", 1, true);
            JavascriptDebugger.selectStudioRequest(request);
            assertFalse(JavascriptDebugger.selects(request));
            JavascriptDebugger.studioRequests("eclipse", true);
            var other = JavascriptDebugger.session("other-eclipse", true);
            assertThrows(IllegalStateException.class, () -> JavascriptDebugger.studioRequests("other-eclipse", true));
            assertFalse(JavascriptDebugger.selects(request("eclipse", null, true)));
            assertFalse(JavascriptDebugger.selects(request));
            JavascriptDebugger.selectStudioRequest(request);
            assertTrue(JavascriptDebugger.selects(request));
            assertTrue(session.state().getBoolean("studioRequests"));
            try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
                 var binding = JavascriptDebugger.bind(request)) {
                assertSame(session, Context.getCurrentContext().getDebugger());
                assertNull(JavascriptDebugger.bind(request));
                var rejected = pool.submit(() -> {
                    var next = new com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest();
                    JavascriptDebugger.selectStudioRequest(next);
                    return assertThrows(IllegalStateException.class, () -> JavascriptDebugger.bind(next)).getMessage();
                });
                assertTrue(rejected.get(2, java.util.concurrent.TimeUnit.SECONDS).contains("already has a running request"));
                assertTrue(session.state().getString("notice").contains("refused"));
            }
            JavascriptDebugger.studioRequests("eclipse", false);
            assertFalse(JavascriptDebugger.selects(request));
            JavascriptDebugger.studioRequests("eclipse", true);
            mode.setBoolean(null, false);
            assertNull(JavascriptDebugger.studioSession());
            assertFalse(JavascriptDebugger.selects(request));
            mode.setBoolean(null, true);
            JavascriptDebugger.stop("eclipse");
            assertNull(JavascriptDebugger.studioSession());
            assertFalse(JavascriptDebugger.selects(request));
            assertNotNull(other);
        } finally {
            JavascriptDebugger.stop("eclipse"); JavascriptDebugger.stop("other-eclipse");
            mode.setBoolean(null, previous);
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, webStudio);
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, debuggerEnabled);
        }
    }
    HttpServletRequest request(String owner, String marker, boolean admin) {
        Map<String, Object> sessionAttributes = new HashMap<>(), attributes = new HashMap<>();
        if (admin) sessionAttributes.put(SessionKey.ADMIN_ROLES.toString(), new Role[] { Role.WEB_ADMIN });
        var session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ HttpSession.class }, (p,m,a) -> switch (m.getName()) {
            case "getId" -> owner;
            case "getAttribute" -> sessionAttributes.get(a[0]);
            case "removeAttribute" -> sessionAttributes.remove(a[0]);
            default -> null;
        });
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ HttpServletRequest.class }, (p,m,a) -> switch (m.getName()) {
            case "getSession" -> session;
            case "getParameter" -> JavascriptDebugger.PARAMETER.equals(a[0]) ? marker : null;
            case "getAttribute" -> attributes.get(a[0]);
            case "setAttribute" -> attributes.put((String)a[0], a[1]);
            case "removeAttribute" -> attributes.remove(a[0]);
            default -> null;
        });
    }
    @Test public void serverGateOwnershipAndUnmarkedRequests() throws Exception {
        EnginePropertiesManager.initProperties();
        Engine.logAdmin = Logger.getLogger("debugger-test");
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, "enabled");
        try {
            assertFalse(JavascriptDebugger.enabled());
            assertThrows(IllegalStateException.class, () -> JavascriptDebugger.session("one", true));
            assertNull(JavascriptDebugger.bind(request("one", "unused", true)));
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "true");
            var first = JavascriptDebugger.session("one", true);
            first.breakpoint("marked", 1, true);
            var second = JavascriptDebugger.session("two", true);
            assertNotEquals(first.id(), second.id()); assertFalse(RhinoUtils.debugMode);
            try (var registration = JavascriptDebugger.bind(request("one", null, true))) { assertNotNull(registration); assertNull(Context.getCurrentContext()); }
            assertNull(JavascriptDebugger.bind(request("one", first.id(), false)));
            assertThrows(IllegalArgumentException.class, () -> JavascriptDebugger.bind(request("two", first.id(), true)));
            var marked = request("one", first.id(), true);
            try (var binding = JavascriptDebugger.bind(marked)) {
                assertSame(first, Context.getCurrentContext().getDebugger());
                assertTrue(Context.getCurrentContext().isInterpretedMode());
                assertNull(JavascriptDebugger.bind(marked));
                JavascriptDebugger.stop("one");
                assertEquals(42, ((Number)RhinoUtils.evalCachedJavascript(Context.getCurrentContext(), Context.getCurrentContext().initStandardObjects(), "40 + 2", "after-detach", 1, null)).intValue());
            }
            assertNull(Context.getCurrentContext());
            try (var cx = Context.enter()) {
                assertNull(cx.getDebugger());
                RhinoUtils.evalCachedJavascript(cx, cx.initStandardObjects(), "40 + 3", "unmarked", 1, null);
                assertFalse(cx.isInterpretedMode());
            }
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "false");
            assertNull(JavascriptDebugger.session("two", false)); assertFalse(JavascriptDebugger.active());
        } finally {
            JavascriptDebugger.stop("one"); JavascriptDebugger.stop("two");
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, PropertyName.WEB_STUDIO.getDefaultValue());
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "false");
        }
    }
    @Test public void compiledCacheSurvivesDebugging() throws Exception {
        EnginePropertiesManager.initProperties();
        final String source = "var cacheProof = 6; cacheProof * 7;";
        var field = RhinoUtils.class.getDeclaredField("compiledScript"); field.setAccessible(true);
        @SuppressWarnings("unchecked") var cache = (Map<String, Script>)field.get(null);
        try (var cx = Context.enter()) { RhinoUtils.evalCachedJavascript(cx, cx.initStandardObjects(), source, "cache", 1, null); }
        var compiled = cache.get(source); assertNotNull(compiled);
        try (var session = new JavascriptDebugSession(); var lease = session.enter()) {
            var cx = Context.getCurrentContext();
            RhinoUtils.evalCachedJavascript(cx, cx.initStandardObjects(), source, "cache", 1, null);
            assertEquals(1, session.state().getJSONArray("sources").length());
        }
        assertSame(compiled, cache.get(source));
    }
    @Test public void logoutRemovesOwnershipAndReleasesTheRequest() throws Exception {
        EnginePropertiesManager.initProperties(); Engine.logAdmin = Logger.getLogger("debugger-test");
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, "enabled");
        EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "true");
        try (var pool = java.util.concurrent.Executors.newCachedThreadPool()) {
            var session = JavascriptDebugger.session("logout", true);
            session.breakpoint("logout", 1, true);
            var marked = request("logout", session.id(), true);
            session.requestPause();
            var task = pool.submit(() -> { try (var lease = JavascriptDebugger.bind(marked)) {
                var cx = Context.getCurrentContext(); return cx.evaluateString(cx.initStandardObjects(), "42;", "logout", 1, null);
            }});
            long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
            while (!session.state().getBoolean("stopped") && System.nanoTime() < until) Thread.sleep(5);
            assertTrue(session.state().getBoolean("stopped"));
            new com.twinsoft.convertigo.engine.AuthenticatedSessionManager().removeAuthenticatedSession(marked.getSession());
            assertEquals(42, ((Number) task.get(2, java.util.concurrent.TimeUnit.SECONDS)).intValue());
            assertNull(JavascriptDebugger.session("logout", false));
            assertFalse(session.state().getBoolean("attached"));
            assertNull(session.enterChild());
        } finally {
            JavascriptDebugger.stop("logout");
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, PropertyName.WEB_STUDIO.getDefaultValue());
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "false");
        }
    }
    @Test public void useFunctionsArePrivateToDebugRequestsWithoutChangingTheOrdinaryCache() throws Exception {
        EnginePropertiesManager.initProperties();
        var project = new com.twinsoft.convertigo.beans.core.Project() {
            private final java.util.Map<String, Object> shared = new java.util.HashMap<>();
            @Override public Object get(String key) { return shared.get(key); }
            @Override public void set(String key, Object value) { shared.put(key, value); }
        }; project.setName("UseProbe");

        String source = "(function(value) { return value + 3; })";
        Object cached;
        try (var cx = Context.enter()) {
            var scope = cx.initStandardObjects(); var context = cx.newObject(scope); context.put("project", context, Context.javaToJS(project, scope)); scope.put("context", scope, context);
            cached = com.twinsoft.convertigo.beans.core.RequestableObject.useInScope(cx, scope, new Object[]{source}, null);
            assertNotNull(cached);
        }
        try (var session = new JavascriptDebugSession(); var lease = session.enter()) {
            var cx = Context.getCurrentContext(); var scope = cx.initStandardObjects();
            var context = cx.newObject(scope); context.put("project", context, Context.javaToJS(project, scope)); scope.put("context", scope, context);
            var debug = com.twinsoft.convertigo.beans.core.RequestableObject.useInScope(cx, scope, new Object[]{source}, null);
            assertNotSame(cached, debug);
            assertTrue(session.state().getJSONArray("sources").getString(0).startsWith("UseProbe/use/"));
        }
        try (var cx = Context.enter()) {
            var scope = cx.initStandardObjects(); var context = cx.newObject(scope); context.put("project", context, Context.javaToJS(project, scope)); scope.put("context", scope, context);
            assertSame(cached, com.twinsoft.convertigo.beans.core.RequestableObject.useInScope(cx, scope, new Object[]{source}, null));
        }
    }

    @Test public void sourceLocationsExposeProjectSequenceStepAndProperty() throws Exception {
        EnginePropertiesManager.initProperties();
        var project = new com.twinsoft.convertigo.beans.core.Project(); project.setName("ReadableProject");
        var sequence = new com.twinsoft.convertigo.beans.sequences.GenericSequence(); sequence.setName("Probe"); project.add(sequence);
        var step = new com.twinsoft.convertigo.beans.steps.SimpleStep(); step.setName("Script"); sequence.add(step);
        try (var session = new JavascriptDebugSession(); var lease = session.enter()) {
            var cx = Context.getCurrentContext();
            RhinoUtils.evalCachedJavascript(step, cx, cx.initStandardObjects(), "42;", "expression", 1, null);
            String url = step.getShortQName() + "-expression";
            var location = session.source(url).getJSONObject("location");
            assertEquals("ReadableProject › Probe › Script › expression", location.getString("label"));
            assertEquals(step.getFullQName(), location.getString("id"));
            assertEquals("expression", location.getString("property"));
            assertEquals(location.toString(), session.state().getJSONObject("sourceLocations").getJSONObject(url).toString());
        }
    }
    @Test public void legacyAndWebDebuggerAttachmentsAreMutuallyExclusiveAndRecover() throws Exception {
        EnginePropertiesManager.initProperties(); Engine.logAdmin = Logger.getLogger("debugger-test");
        EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, "enabled");
        EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "true");
        boolean previous = RhinoUtils.debugMode;
        try {
            var web = JavascriptDebugger.session("exclusion", true);
            assertThrows(IllegalStateException.class, JavascriptDebugger::attachLegacyDebugger);
            assertFalse(RhinoUtils.debugMode); assertTrue(web.state().getBoolean("attached"));
            JavascriptDebugger.stop("exclusion");
            JavascriptDebugger.attachLegacyDebugger(); assertTrue(RhinoUtils.debugMode);
            assertThrows(IllegalStateException.class, () -> JavascriptDebugger.session("exclusion", true));
            try (var cx = Context.enter()) {
                assertEquals(42, ((Number)RhinoUtils.evalCachedJavascript(cx, cx.initStandardObjects(), "40+2", "legacy", 1, null)).intValue());
            }
            RhinoUtils.debugMode = false;
            assertNotNull(JavascriptDebugger.session("exclusion", true));
        } finally {
            JavascriptDebugger.stop("exclusion"); RhinoUtils.debugMode = previous;
            EnginePropertiesManager.setProperty(PropertyName.WEB_STUDIO, PropertyName.WEB_STUDIO.getDefaultValue());
            EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_DEBUGGER, "false");
        }
    }
    @Test public void eclipsePauseRejectsSecondLaunchAndDetachLetsTheFirstFinish() throws Exception {
        EnginePropertiesManager.initProperties(); Engine.logAdmin = Logger.getLogger("debugger-test");
        var mode = Engine.class.getDeclaredField("bStudioMode"); mode.setAccessible(true);
        boolean previous = mode.getBoolean(null); mode.setBoolean(null, true);
        try (var pool = java.util.concurrent.Executors.newCachedThreadPool()) {
            var session = JavascriptDebugger.session("paused-eclipse", true);
            session.breakpoint("eclipse", 1, true);
            JavascriptDebugger.studioRequests("paused-eclipse", true);
            session.requestPause();
            var first = pool.submit(() -> {
                var request = new com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest();
                JavascriptDebugger.selectStudioRequest(request);
                try (var lease = JavascriptDebugger.bind(request)) {
                    var cx = Context.getCurrentContext(); return cx.evaluateString(cx.initStandardObjects(), "42;", "eclipse", 1, null);
                }
            });
            long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
            while (!session.state().getBoolean("stopped") && System.nanoTime() < until) Thread.sleep(5);
            assertTrue(session.state().getBoolean("stopped"));
            var next = new com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest(); JavascriptDebugger.selectStudioRequest(next);
            assertTrue(assertThrows(IllegalStateException.class, () -> JavascriptDebugger.bind(next)).getMessage().contains("already has a running request"));
            assertTrue(session.state().getString("notice").contains("refused")); assertFalse(first.isDone());
            JavascriptDebugger.studioRequests("paused-eclipse", false);
            var ordinary = new com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest(); JavascriptDebugger.selectStudioRequest(ordinary);
            assertNull(JavascriptDebugger.bind(ordinary)); assertNull(Context.getCurrentContext());
            JavascriptDebugger.stop("paused-eclipse");
            assertEquals(42, ((Number)first.get(2, java.util.concurrent.TimeUnit.SECONDS)).intValue());
            assertFalse(session.state().getBoolean("attached")); assertNull(JavascriptDebugger.studioSession());
        } finally { JavascriptDebugger.stop("paused-eclipse"); mode.setBoolean(null, previous); }
    }

    @Test(timeout = 5000) public void legacyDriverStillPausesInspectsEvaluatesStepsAndContinues() throws Exception {
        EnginePropertiesManager.initProperties();
        var dim = new org.mozilla.javascript.tools.debugger.Dim();
        var lines = new java.util.ArrayList<Integer>();
        boolean previous = RhinoUtils.debugMode;
        try {
            JavascriptDebugger.attachLegacyDebugger();
            dim.setGuiCallback(new org.mozilla.javascript.tools.debugger.GuiCallback() {
                @Override public boolean isGuiEventThread() { return true; }
                @Override public void dispatchNextGuiEvent() { throw new AssertionError("No GUI event pump is required by this probe"); }
                @Override public void updateSourceText(org.mozilla.javascript.tools.debugger.Dim.SourceInfo source) { source.breakpoint(2, true); }
                @Override public void enterInterrupt(org.mozilla.javascript.tools.debugger.Dim.StackFrame frame, String thread, String alert) {
                    lines.add(frame.getLineNumber());
                    assertEquals(1, frame.contextData().frameCount());
                    dim.contextSwitch(0);
                    assertEquals(lines.size() == 1 ? "41" : "42", dim.eval("legacyValue"));
                    assertEquals(lines.size() == 1 ? "42" : "43", dim.eval("legacyValue + 1"));
                    assertTrue(java.util.Arrays.asList(dim.getObjectIds(frame.scope())).contains("legacyValue"));
                    dim.setReturnValue(lines.size() == 1 ? org.mozilla.javascript.tools.debugger.Dim.STEP_OVER : org.mozilla.javascript.tools.debugger.Dim.GO);
                }
            });
            dim.attachTo(org.mozilla.javascript.ContextFactory.getGlobal());
            try (var cx = Context.enter()) {
                assertEquals(42, ((Number)RhinoUtils.evalCachedJavascript(cx, cx.initStandardObjects(), "var legacyValue=41;\nlegacyValue++;\nlegacyValue;", "legacy-driver", 1, null)).intValue());
            }
            assertEquals(java.util.List.of(2, 3), lines);
        } finally { dim.dispose(); RhinoUtils.debugMode = previous; }
    }

}
