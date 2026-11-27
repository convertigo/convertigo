/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.debug;

import static org.junit.Assert.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.codehaus.jettison.json.*;
import org.junit.Test;

public class JavascriptDebugResponseTest {
    @org.junit.Rule public final FrozenInspectionClock inspectionClock = new FrozenInspectionClock();
    @org.junit.Before public void editingEnabledFixture() throws Exception {
        com.twinsoft.convertigo.engine.EnginePropertiesManager.initProperties();
        com.twinsoft.convertigo.engine.EnginePropertiesManager.setProperty(com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName.WEB_STUDIO,"enabled");
    }

    @Test public void oneCharacterMarkerPreservesProtocolAndOpaqueHandlesButMasksDataKeys() throws Exception {
        JavascriptDebugRedaction.testValues = () -> List.of("e");
        try {
            var input = new JSONObject().put("attached", true).put("sessionId", "eeee")
                .put("frames", new JSONArray().put(new JSONObject().put("ref", "eeee").put("name", "secret")))
                .put("breakpoints", new JSONObject().put("source:e", new JSONArray().put(1)))
                .put("bindings",new JSONObject().put("e","secret"));
            var output = JavascriptDebugRedaction.output(input);
            assertTrue(output.has("attached")); assertTrue(output.has("frames"));
            assertEquals("eeee", output.getString("sessionId"));
            assertEquals("eeee", output.getJSONArray("frames").getJSONObject(0).getString("ref"));
            assertEquals("s[redacted]cr[redacted]t", output.getJSONArray("frames").getJSONObject(0).getString("name"));
            assertTrue(output.getJSONObject("breakpoints").has("source:e"));
            assertTrue(JavascriptDebugResponse.prepare(input,candidate->{}).getJSONObject("breakpoints").has("source:e"));
            assertTrue(input.getJSONObject("breakpoints").has("source:e"));
            assertTrue(output.getJSONObject("bindings").has("[redacted]"));
            assertTrue(JavascriptDebugResponse.prepare(input,candidate->{}).getJSONObject("bindings").has("[redacted]"));
        } finally { JavascriptDebugRedaction.testValues = null; }
    }
    @Test public void businessIdsAndScalarsAreMaskedWithoutRenamingMetadataFields() throws Exception {
        String sentinel=java.util.UUID.randomUUID().toString();
        JavascriptDebugRedaction.testValues=()->List.of(sentinel,"42");
        try {
            var input=new JSONObject().put("sessionId",sentinel).put("currentLevel",42)
                .put("data",new JSONObject().put("id",sentinel).put("ref",sentinel).put("number",42));
            var output=JavascriptDebugRedaction.output(input);
            assertTrue(sentinel.equals(output.getString("sessionId"))); assertEquals(42,output.getInt("currentLevel"));
            assertFalse(output.getJSONObject("data").toString().contains(sentinel));
            assertTrue("[redacted]".equals(output.getJSONObject("data").getString("number")));
            var decoded=JavascriptDebugResponse.decode(input.toString(),candidate->{});
            var prepared=JavascriptDebugResponse.prepare(decoded,candidate->{});
            assertTrue(sentinel.equals(prepared.getString("sessionId")));
            assertFalse(prepared.getJSONObject("data").toString().contains(sentinel));
            assertTrue("[redacted]".equals(prepared.getJSONObject("data").getString("number")));
        } finally {JavascriptDebugRedaction.testValues=null;}
    }
    @Test public void dynamicSourceMapKeysDoNotMakeTheirMetadataKeysBusinessData() throws Exception {
        JavascriptDebugRedaction.testValues=()->List.of("e");
        try {
            for(String key:List.of("e","data")) {
                var input=new JSONObject().put("sourceLocations",new JSONObject().put(key,new JSONObject()
                    .put("identity",new JSONObject().put("id","eeee")).put("reference",true)));
                var output=JavascriptDebugRedaction.output(input);String displayed=key;
                var metadata=output.getJSONObject("sourceLocations").getJSONObject(displayed);
                assertTrue(metadata.has("reference"));assertTrue(metadata.has("identity"));
                assertEquals("eeee",metadata.getJSONObject("identity").getString("id"));
                var prepared=JavascriptDebugResponse.prepare(input,candidate->{});
                assertTrue(prepared.getJSONObject("sourceLocations").getJSONObject(displayed).has("identity"));
            }
        } finally {JavascriptDebugRedaction.testValues=null;}
    }
    @Test public void integerMetadataKeepsItsExactValueAndOverflowIsRefused() throws Exception {
        String text="{\"serial\":9007199254740993}";
        var decoded=JavascriptDebugResponse.decode(text,candidate->{});
        assertEquals(9007199254740993L,decoded.getLong("serial"));
        assertTrue(JavascriptDebugResponse.prepare(decoded,candidate->{}).toString().contains("9007199254740993"));
        assertThrows(JavascriptDebugResponse.Limit.class,()->JavascriptDebugResponse.decode("{\"serial\":999999999999999999999999}",candidate->{}));
    }
    @Test public void thousandsOfVariablesAreRefusedBeforeFullDecode() throws Exception {
        var rows = new JSONArray(); for (int i = 0; i < 2500; i++) rows.put(new JSONObject().put("name", "v" + i).put("value", 42));
        var count = new AtomicInteger();
        JavascriptDebugResponse.Work work = candidate -> { if (candidate && count.incrementAndGet() > 1000) throw new JavascriptDebugResponse.Limit("candidates"); };
        var failure = assertThrows(JavascriptDebugResponse.Limit.class,
            () -> JavascriptDebugResponse.decode(new JSONObject().put("variables", rows).toString(), work));
        assertEquals("candidates", failure.reason); assertEquals(1001, count.get());
    }
    @Test public void decodingMaskingAndFormattingShareOneQuotaAndCachedWire() throws Exception {
        JavascriptDebugRedaction.testValues = () -> List.of("fixture-value");
        try {
            var count = new AtomicInteger(); JavascriptDebugResponse.Work work = candidate -> { if (candidate) count.incrementAndGet(); };
            var decoded = JavascriptDebugResponse.decode("{\"variables\":[{\"name\":\"alias\",\"value\":\"fixture-value\"}],\"cursor\":\"1200000\"}", work);
            int afterDecode = count.get();
            var result = JavascriptDebugResponse.prepare(decoded, work);
            assertTrue(count.get() > afterDecode);
            assertFalse(result.toString().contains("fixture-value")); assertEquals("1200000", result.getString("cursor"));
            result.put("inspectionBudget", new JSONObject().put("candidates", count.get()));
            ((JavascriptDebugResponse.Prepared)result).seal();
            assertEquals(result.getJSONObject("inspectionBudget").getInt("candidates"), new JSONObject(result.toString()).getJSONObject("inspectionBudget").getInt("candidates"));
            int after = count.get(); result.toString(); result.toString(2); assertEquals(after, count.get());
        } finally { JavascriptDebugRedaction.testValues = null; }
    }
    @Test public void malformedRepliesAndDeepObjectsAreRefused() {
        for (String text : List.of("{\"x\":1,\"x\":2}", "{\"x\":NaN}", "[]", "{\"x\":\"bad\n\"}", "{\"x\":01}"))
            assertThrows(JavascriptDebugResponse.Limit.class, () -> JavascriptDebugResponse.decode(text, candidate -> {}));
    }
    @Test public void stopDuringMaskingAndFormattingStopsCooperatively() throws Exception {
        var object = new JSONObject().put("value", "x".repeat(60000)); var calls = new AtomicInteger();
        var failure = assertThrows(JavascriptDebugResponse.Limit.class, () -> JavascriptDebugResponse.prepare(object,
            candidate -> { if (calls.incrementAndGet() > 500) throw new JavascriptDebugResponse.Limit("expired"); }));
        assertEquals("expired", failure.reason); assertTrue(calls.get() < 510);
    }
}
