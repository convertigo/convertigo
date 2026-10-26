/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.util.Map;
import java.util.List;
import com.twinsoft.convertigo.engine.util.JsonValues;
import org.codehaus.jettison.json.JSONException;

/** A Java-only capability. No JSON marker can forge ownership. */
public final class StreamResult extends java.util.LinkedHashMap<String, Object> {
	private static final long serialVersionUID = 1L;
	private ResultDocument document;
	public StreamResult(Map<String, Object> envelope, ResultDocument document) throws JSONException {
		for (var keys = envelope.keySet().iterator(); keys.hasNext();) { var key = (String) keys.next(); put(key, envelope.get(key)); }
		this.document = document;
	}
	public ResultDocument take() {
		if (document == null) throw new IllegalStateException("Row result already transferred");
		var value = document; document = null; return value;
	}
}
