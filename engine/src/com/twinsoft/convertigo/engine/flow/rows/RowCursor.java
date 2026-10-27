/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.DetachedJson;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.JsonValues;

/**
 * A source read chunk by chunk by a script: the paged JSON rows of an HTTP source, pulled only when the next chunk is
 * asked, never all retained. Its chunks are detached rows; the script owns the cursor and closes it.
 */
public final class RowCursor implements AutoCloseable {
	private final RowSource source;
	private boolean ended;

	private RowCursor(RowSource source) {
		this.source = source;
	}

	/** A cursor on paged JSON rows; spec is the JSON text of {http, limits, take} as a row plan describes them. */
	public static RowCursor open(String spec) throws Exception {
		var plan = JsonValues.map(JsonValues.parse(spec));
		var limit = JsonValues.map(plan.get("limits"));
		var bounds = new PagedJsonRows.Bounds(((Number) limit.get("pages")).intValue(), ((Number) limit.get("inputRows")).longValue(), ((Number) limit.get("inputBytes")).longValue(),
				((Number) limit.get("rowNodes")).intValue(), ((Number) limit.get("stringCharacters")).intValue(), ((Number) limit.get("depth")).intValue(), ((Number) limit.get("millis")).longValue());
		var http = JsonValues.map(plan.get("http"));
		var headers = new HashMap<String, String>();
		var headerObject = JsonValues.map(http.get("headers"));
		for (var key : headerObject.keySet()) headers.put(key, (String) headerObject.get(key));
		var redactions = new ArrayList<String>();
		for (var item : JsonValues.list(http.get("redactions"))) redactions.add((String) item);
		var dropKeys = new HashSet<String>();
		for (var item : JsonValues.list(http.get("dropKeys"))) dropKeys.add((String) item);
		URI uri;
		try { uri = URI.create((String) http.get("url")); } catch (Exception error) { throw new RowStreamException("ROW_STREAM_URL", 400); }
		var ownClient = Engine.theApp == null || Engine.theApp.httpClient4 == null;
		var client = ownClient ? HttpUtils.makeHttpClient(false) : Engine.theApp.httpClient4;
		try {
			return new RowCursor(new PagedJsonRows(client, ownClient, uri, headers, (String) http.get("items"), (String) http.get("next"), bounds,
					JsonValues.number(plan, "take", 0), ((Number) http.get("connectTimeout")).intValue(), ((Number) http.get("readTimeout")).intValue(),
					redactions, dropKeys, JsonValues.text(http, "pageParameter", "")));
		} catch (Exception error) {
			if (ownClient) client.close();
			throw RowStreamException.safe(error);
		}
	}

	/** The next rows, at most size, as a script array; null after the last row. */
	public Object next(int size, Scriptable scope) throws Exception {
		var rows = nextRows(size);
		return rows == null ? null : DetachedJson.toScriptable(rows, scope);
	}

	/** The next rows, at most size, as detached Java rows; null after the last row. */
	public List<Object> nextRows(int size) throws IOException {
		if (ended) return null;
		var rows = new ArrayList<Object>(Math.min(size, 1024));
		while (rows.size() < size) {
			var row = source.next();
			if (row == null) { ended = true; break; }
			rows.add(row);
		}
		return rows.isEmpty() ? null : rows;
	}

	@Override public void close() throws IOException {
		ended = true;
		source.close();
	}
}
