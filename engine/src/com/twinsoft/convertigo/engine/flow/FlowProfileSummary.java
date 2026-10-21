/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** Aggregate repetitive block records for logs; leave the detailed MCP profile intact. */
public final class FlowProfileSummary {
	private FlowProfileSummary() { }

	public static JSONObject summarize(JSONObject profile) throws JSONException {
		var summary = new JSONObject();
		for (var keys = profile.keys(); keys.hasNext();) {
			var key = (String) keys.next();
			if (!"blocks".equals(key)) summary.put(key, profile.get(key));
		}
		var blocks = profile.optJSONArray("blocks");
		var grouped = new java.util.LinkedHashMap<String, JSONObject>();
		if (blocks != null) for (int i = 0; i < blocks.length(); i++) {
			var block = blocks.getJSONObject(i);
			var key = block.optString("kind") + ":" + block.optString("name");
			var total = grouped.get(key);
			if (total == null) {
				total = new JSONObject().put("kind", block.optString("kind")).put("name", block.optString("name"));
				grouped.put(key, total);
			}
			total.put("count", total.optLong("count", 0) + 1);
			total.put("durationMs", total.optDouble("durationMs", 0) + block.optDouble("durationMs", 0));
			total.put("allocatedBytes", total.optLong("allocatedBytes", 0) + block.optLong("allocatedBytes", 0));
		}
		return summary.put("blocks", new JSONArray(grouped.values())).put("blocksAreInclusive", true);
	}
}
