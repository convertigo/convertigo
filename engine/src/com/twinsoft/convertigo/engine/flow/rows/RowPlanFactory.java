/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.flow.RowProducerRuntime;
import com.twinsoft.convertigo.engine.util.HttpUtils;

/** Constructed only by the registered result boundary, from an owned detached plan. */
public final class RowPlanFactory {
	private RowPlanFactory() { }
	public static ResultDocument create(JSONObject plan, ClassLoader generation) throws Exception {
		var limit = plan.getJSONObject("limits");
		var output = new ResultDocument.Limits(limit.getLong("rows"), limit.getLong("bytes"),
				limit.getInt("rowBytes"), limit.getInt("precommitRows"), limit.getInt("precommitBytes"));
		var bounds = new PagedJsonRows.Bounds(limit.getInt("pages"), limit.getLong("inputRows"), limit.getLong("inputBytes"),
				limit.getInt("rowNodes"), limit.getInt("stringCharacters"), limit.getInt("depth"), limit.getLong("millis"));
		var http = plan.getJSONObject("http");
		var headers = new HashMap<String,String>();
		var headerObject = http.getJSONObject("headers");
		for (var keys = headerObject.keys(); keys.hasNext();) { var key = (String) keys.next(); headers.put(key, headerObject.getString(key)); }
		var redactions = new ArrayList<String>(); var redact = http.getJSONArray("redactions");
		for (int i=0;i<redact.length();i++) redactions.add(redact.getString(i));
		var dropKeys = new HashSet<String>(); var drop = http.getJSONArray("dropKeys");
		for (int i=0;i<drop.length();i++) dropKeys.add(drop.getString(i));
		var transform = plan.getJSONObject("transform");
		var sources = new HashMap<String,String>(); var blocks = transform.getJSONObject("blocks"); int chars = 0;
		for (var keys = blocks.keys(); keys.hasNext();) {
			var name = (String) keys.next(); var source = blocks.getString(name); chars += source.length(); sources.put(name,source);
		}
		if (sources.isEmpty() || sources.size()>32 || chars>2_000_000 || transform.getString("bootstrap").length()>65_536)
			throw new RowStreamException("ROW_STREAM_PLAN_LIMIT",413);
		ResultDocument.encodedBytes(transform.getJSONObject("input"), 1024 * 1024);
		// Validate before acquiring a client. Neither the URL nor headers enter exceptions.
		URI uri;
		try { uri = URI.create(http.getString("url")); } catch (Exception error) { throw new RowStreamException("ROW_STREAM_URL",400); }
		var ownClient = Engine.theApp == null || Engine.theApp.httpClient4 == null;
		var client = ownClient ? HttpUtils.makeHttpClient(false) : Engine.theApp.httpClient4;
		RowSource source = null;
		try {
			source = new PagedJsonRows(client, ownClient, uri, headers, http.getString("items"), http.getString("next"), bounds,
					plan.optLong("take",0), http.getInt("connectTimeout"), http.getInt("readTimeout"), redactions, dropKeys, http.optString("pageParameter",""));
			source = new RowProducerRuntime(source, new RowProducerRuntime.Plan(transform.getString("bootstrap"), sources,
					transform.getJSONObject("input"), transform.getString("entry"), transform.optString("finish",""),
					plan.optBoolean("collect",false),output.rows(),(int)output.bytes()), generation);
			return new ResultDocument(source, plan.getString("field"), plan.getJSONObject("fields"), output);
		} catch (Exception error) {
			if (source != null) { try { source.close(); } catch (Exception ignored) { } }
			else if (ownClient) client.close();
			throw RowStreamException.safe(error);
		}
	}
}
