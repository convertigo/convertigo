/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import com.twinsoft.convertigo.engine.util.JsonValues;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.flow.RowProducerRuntime;
import com.twinsoft.convertigo.engine.util.HttpUtils;

/** Constructed only by the registered result boundary, from an owned detached plan. */
public final class RowPlanFactory {
	private RowPlanFactory() { }

	private static final class EmptyRows implements RowSource {
		@Override public Map<String, Object> next() { return null; }
		@Override public void close() { }
	}

	/** The pure row kernels of the Flow engine library, in Java, when its helpers are available to the caller. */
	@SuppressWarnings("unchecked")
	private static java.util.function.Function<Object, Object> javaPipeline(Object java, ClassLoader generation) {
		if (!(java instanceof Map<?, ?> spec) || generation == null) return null;
		try {
			var rows = Class.forName("com.convertigo.libflowengine.Rows", true, generation);
			if (!Boolean.TRUE.equals(rows.getMethod("available").invoke(null))) return null;
			return (java.util.function.Function<Object, Object>) rows.getMethod("javaPipeline", Object.class).invoke(null, spec.get("steps"));
		} catch (ReflectiveOperationException | LinkageError | ClassCastException unavailable) {
			return null;
		}
	}
	public static ResultDocument create(Map<String, Object> plan, ClassLoader generation) throws Exception {
		var limit = JsonValues.map(plan.get("limits"));
		var output = new ResultDocument.Limits(((Number) limit.get("rows")).longValue(), ((Number) limit.get("bytes")).longValue(),
				((Number) limit.get("rowBytes")).intValue(), ((Number) limit.get("precommitRows")).intValue(), ((Number) limit.get("precommitBytes")).intValue());
		var bounds = new PagedJsonRows.Bounds(((Number) limit.get("pages")).intValue(), ((Number) limit.get("inputRows")).longValue(), ((Number) limit.get("inputBytes")).longValue(),
				((Number) limit.get("rowNodes")).intValue(), ((Number) limit.get("stringCharacters")).intValue(), ((Number) limit.get("depth")).intValue(), ((Number) limit.get("millis")).longValue());
		var http = JsonValues.map(plan.get("http"));
		var headers = new HashMap<String,String>();
		var headerObject = JsonValues.map(http.get("headers"));
		for (var keys = headerObject.keySet().iterator(); keys.hasNext();) { var key = (String) keys.next(); headers.put(key, ((String) headerObject.get(key))); }
		var redactions = new ArrayList<String>(); var redact = JsonValues.list(http.get("redactions"));
		for (int i=0;i<redact.size();i++) redactions.add(((String) redact.get(i)));
		var dropKeys = new HashSet<String>(); var drop = JsonValues.list(http.get("dropKeys"));
		for (int i=0;i<drop.size();i++) dropKeys.add(((String) drop.get(i)));
		var transform = JsonValues.map(plan.get("transform"));
		var sources = new HashMap<String,String>(); var blocks = JsonValues.map(transform.get("blocks")); int chars = 0;
		for (var keys = blocks.keySet().iterator(); keys.hasNext();) {
			var name = (String) keys.next(); var source = ((String) blocks.get(name)); chars += source.length(); sources.put(name,source);
		}
		if (sources.isEmpty() || sources.size()>32 || chars>2_000_000 || ((String) transform.get("bootstrap")).length()>65_536)
			throw new RowStreamException("ROW_STREAM_PLAN_LIMIT",413);
		ResultDocument.encodedBytes(JsonValues.map(transform.get("input")), 1024 * 1024);
		// Validate before acquiring a client. Neither the URL nor headers enter exceptions.
		URI uri;
		try { uri = URI.create(((String) http.get("url"))); } catch (Exception error) { throw new RowStreamException("ROW_STREAM_URL",400); }
		var ownClient = Engine.theApp == null || Engine.theApp.httpClient4 == null;
		var client = ownClient ? HttpUtils.makeHttpClient(false) : Engine.theApp.httpClient4;
		RowSource source = null;
		try {
			source = new PagedJsonRows(client, ownClient, uri, headers, ((String) http.get("items")), ((String) http.get("next")), bounds,
					JsonValues.number(plan, "take",0), ((Number) http.get("connectTimeout")).intValue(), ((Number) http.get("readTimeout")).intValue(), redactions, dropKeys, JsonValues.text(http, "pageParameter",""));
			var producerPlan = new RowProducerRuntime.Plan(((String) transform.get("bootstrap")), sources,
					JsonValues.map(transform.get("input")), ((String) transform.get("entry")), JsonValues.text(transform, "finish",""),
					JsonValues.bool(plan, "collect",false),output.rows(),(int)output.bytes());
			var java = javaPipeline(transform.get("java"), generation);
			if (java != null) {
				// the JavaScript producer only gives the metadata of an empty result
				source = new JavaRowTransform(source, java, JsonValues.map(JsonValues.map(transform.get("java")).get("metadata")),
						() -> new RowProducerRuntime(new EmptyRows(), producerPlan, generation));
			} else {
				source = new RowProducerRuntime(source, producerPlan, generation);
			}
			return new ResultDocument(source, ((String) plan.get("field")), JsonValues.map(plan.get("fields")), output);
		} catch (Exception error) {
			if (source != null) { try { source.close(); } catch (Exception ignored) { } }
			else if (ownClient) client.close();
			throw RowStreamException.safe(error);
		}
	}
}
