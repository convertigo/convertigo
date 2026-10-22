/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

/** A Java-only capability. No JSONObject marker can forge ownership. */
public final class StreamResult extends JSONObject {
	private ResultDocument document;
	public StreamResult(JSONObject envelope, ResultDocument document) throws JSONException {
		for (var keys = envelope.keys(); keys.hasNext();) { var key = (String) keys.next(); put(key, envelope.get(key)); }
		this.document = document;
	}
	public ResultDocument take() {
		if (document == null) throw new IllegalStateException("Row result already transferred");
		var value = document; document = null; return value;
	}
}
