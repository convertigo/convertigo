package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class FlowProfileSummaryTest {
	@Test public void aggregatesWithoutModifyingDetailedRecords() throws Exception {
		var profile = new JSONObject("{blocks:[{kind:'node',name:'set',durationMs:1.5,allocatedBytes:10},{kind:'node',name:'set',durationMs:2.5,allocatedBytes:20},{kind:'call',name:'set',durationMs:3,allocatedBytes:30}],executeNodesMs:4}");
		var summary = FlowProfileSummary.summarize(profile);
		assertEquals(3, profile.getJSONArray("blocks").length());
		assertEquals(2, summary.getJSONArray("blocks").length());
		var set = summary.getJSONArray("blocks").getJSONObject(0);
		assertEquals(2, set.getInt("count"));
		assertEquals(4, set.getDouble("durationMs"), 0);
		assertEquals(30, set.getLong("allocatedBytes"));
		assertEquals(4, summary.getInt("executeNodesMs"));
	}
}
