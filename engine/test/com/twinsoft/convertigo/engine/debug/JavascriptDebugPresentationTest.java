package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import org.junit.Test;

/** UI metadata reflects the running worker and the server's existing breakpoint map. */
public class JavascriptDebugPresentationTest {
    @org.junit.Rule public final FrozenInspectionClock inspectionClock = new FrozenInspectionClock();
    @org.junit.Before public void editingEnabledFixture() throws Exception {
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.WEB_STUDIO,"enabled");
    }

    @Test public void participantLevelStaysCapturedWhenSourceBreakpointsChange() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            assertTrue(session.state().isNull("currentLevel"));
            session.joinParticipantWorker();
            try {
                assertEquals(1, session.state().getInt("currentLevel"));
                assertTrue(session.breakpoint("project/source.js", 2, true, true));
                assertTrue(session.hasRhinoBreakpoints());
                assertEquals(1, session.state().getInt("currentLevel"));
                assertTrue(session.breakpoint("project/source.js", 2, false, true));
                assertEquals(1, session.state().getInt("currentLevel"));
            } finally { session.finishParticipantWorker(); }
            assertTrue(session.state().isNull("currentLevel"));
        }
    }
    @Test public void rhinoLevelStaysCapturedAfterRemovingLastSourceBreakpoint() throws Exception {
        try (var session = new JavascriptDebugSession(); var lease = session.enter()) {
            assertEquals(2, session.state().getInt("currentLevel"));
            session.breakpoint("project/source.js", 2, true, true);
            session.breakpoint("project/source.js", 2, false, true);
            assertFalse(session.hasRhinoBreakpoints());
            assertEquals(2, session.state().getInt("currentLevel"));
            session.close();
            assertTrue(session.state().isNull("currentLevel"));
        }
    }
    @Test public void breakpointSnapshotUsesExactRevisionAndPointerAndIsDetached() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            String target = "flow://project/Test?revision=one#/nodes/0";
            String revised = "flow://project/Test?revision=two#/nodes/0";
            session.breakpoint(target, 1, true, false);
            var snapshot = session.state().getJSONObject("breakpoints");
            assertEquals(1, snapshot.getJSONArray(target).getInt(0));
            assertFalse(snapshot.has(revised));
            snapshot.getJSONArray(target).put(9);
            assertEquals(1, session.breakpoints(target).length());
            session.breakpoint(target, 1, false, false);
            assertEquals(0, session.state().getJSONObject("breakpoints").length());
            session.breakpoint(revised, 1, true, false);
            session.close();
            assertEquals(0, session.state().getJSONObject("breakpoints").length());
        }
    }
}
