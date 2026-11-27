/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/** Opaque plugin identities: no provider syntax, labels or source values in the transport. */
public class JavascriptDebugIdentityTest {
    @org.junit.Rule public final FrozenInspectionClock inspectionClock = new FrozenInspectionClock();
    @org.junit.Before public void editingEnabledFixture() throws Exception {
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.WEB_STUDIO,"enabled");
    }

    private String manifest(String id, String revision, String... keys) throws Exception {
        var rows = new JSONArray();
        for (int i=0;i<keys.length;i++) rows.put(new JSONObject().put("url",id+":"+revision+":"+i).put("line",i+2).put("key",keys[i]));
        return new JSONObject().put("id",id).put("revision",revision).put("targets",rows).toString();
    }
    @Test public void onlyUniqueUnchangedContentPreservesAPointAcrossAnEdit() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            session.participantSources("plugin",manifest("source","old","keep","edited","gone"));
            for (int i=0;i<3;i++) session.breakpoint("source:old:"+i,i+2,true,false);
            session.participantSources("plugin",manifest("source","new","inserted","keep","different"));
            assertEquals("[3]",session.breakpoints("source:new:1").toString());
            for (int i=0;i<3;i++) assertEquals(0,session.breakpoints("source:old:"+i).length());
            assertEquals(0,session.breakpoints("source:new:2").length());
            var changes=session.state().getJSONArray("breakpointChanges");
            assertEquals(3,changes.length());
            assertEquals("preserved",changes.getJSONObject(0).getString("status"));
            assertEquals("invalidated",changes.getJSONObject(1).getString("status"));
            assertTrue(session.state().getString("notice").contains("invalidated"));
        }
    }
    @Test public void duplicatesInEitherRevisionAreNeverGuessed() throws Exception {
        for (boolean before : new boolean[]{true,false}) try (var session = new JavascriptDebugSession()) {
            session.participantSources("plugin",manifest("source","old",before ? new String[]{"same","same"} : new String[]{"same"}));
            session.breakpoint("source:old:0",2,true,false);
            session.participantSources("plugin",manifest("source","new",before ? new String[]{"same"} : new String[]{"same","same"}));
            assertEquals(0,session.breakpoints("source:new:0").length());
            var change=session.state().getJSONArray("breakpointChanges").getJSONObject(0);
            assertEquals("invalidated",change.getString("status"));
            assertTrue(change.getString("reason").contains("ambiguous"));
        }
    }
    @Test public void unchangedDraftKeepsItsIdentityAndPromotionUsesTheSameReconciliation() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            var draft=manifest("draft-uri","agent-text","same");
            session.participantSources("plugin",draft);
            session.breakpoint("draft-uri:agent-text:0",2,true,false);
            session.participantSources("plugin",draft);
            assertEquals("[2]",session.breakpoints("draft-uri:agent-text:0").toString());
            assertEquals(0,session.state().getJSONArray("breakpointChanges").length());
            session.participantSources("plugin",manifest("draft-uri","formatted","same"));
            assertEquals("[2]",session.breakpoints("draft-uri:formatted:0").toString());
            assertEquals(0,session.breakpoints("draft-uri:agent-text:0").length());
        }
    }
    @Test public void otherSourcesAndKindsDoNotInvalidateEachOtherAndRemovalClearsThePoint() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            session.participantSources("plugin",manifest("source","old","same"));
            session.breakpoint("source:old:0",2,true,false);
            session.participantSources("other",manifest("source","new","same"));
            session.participantSources("plugin",manifest("unrelated","new","same"));
            assertEquals("[2]",session.breakpoints("source:old:0").toString());
            session.participantSources("plugin",manifest("source","removed"));
            assertEquals(0,session.breakpoints("source:old:0").length());
        }
    }
    @Test public void malformedManifestsAreRejectedBeforeMutatingTheBaseline() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            session.participantSources("plugin",manifest("source","old","same"));
            session.breakpoint("source:old:0",2,true,false);
            var invalid=new JSONObject(manifest("source","new","same"));
            invalid.getJSONArray("targets").getJSONObject(0).put("line",0);
            assertThrows(IllegalArgumentException.class,()->session.participantSources("plugin",invalid.toString()));
            assertEquals("[2]",session.breakpoints("source:old:0").toString());
        }
    }
    @Test public void cachedProjectionSetsAPointBeforeTheFirstInvocationAndStaleClicksAreRefused() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            var identity = new JSONObject().put("kind","plugin").put("sourceId","source").put("revision","old").put("key","same").put("unique",true).toString();
            assertTrue(session.participantBreakpoint("source:old:0",2,true,identity));
            session.participantSources("plugin",manifest("source","new","same"));
            assertEquals("[2]",session.breakpoints("source:new:0").toString());
            assertFalse(session.participantBreakpoint("source:old:0",2,true,identity));
            assertEquals(0,session.breakpoints("source:old:0").length());
            assertTrue(session.state().getString("notice").contains("older source revision"));
        }
    }
    @Test public void runningSavedTextDoesNotMoveADraftPointBeforePromotion() throws Exception {
        try (var session = new JavascriptDebugSession()) {
            session.participantSources("plugin",manifest("working","agent-text","same"));
            session.breakpoint("working:agent-text:0",2,true,false);
            session.participantSources("plugin",manifest("saved","disk-text","same"));
            assertEquals("[2]",session.breakpoints("working:agent-text:0").toString());
            assertEquals(0,session.breakpoints("saved:disk-text:0").length());
            var promote=new JSONObject(manifest("saved","formatted","same")).put("previousId","working");
            session.participantSources("plugin",promote.toString());
            assertEquals("[2]",session.breakpoints("saved:formatted:0").toString());
            assertEquals(0,session.breakpoints("working:agent-text:0").length());
            // A later edit to the saved file now uses the promoted baseline.
            session.participantSources("plugin",manifest("saved","edited","different"));
            assertEquals(0,session.breakpoints("saved:formatted:0").length());
        }
    }

}
