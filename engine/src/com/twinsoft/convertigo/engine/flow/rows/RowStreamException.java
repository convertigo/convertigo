/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow.rows;

import java.io.IOException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** Public errors carry only stable codes, never an upstream URL, body or credential. */
public final class RowStreamException extends IOException {
	private static final long serialVersionUID = 1L;
	private final String code;
	private final int status;
	private JSONObject response;
	public RowStreamException(String code, int status) {
		super("The streamed result could not complete."); this.code = code; this.status = status;
	}
	public static RowStreamException response(JSONObject response, int status) throws IOException {
		if (response == null || response.optJSONObject("error") == null || status < 200 || status > 599)
			throw new RowStreamException("ROW_STREAM_ERROR_CONTRACT",502);
		ResultDocument.encodedBytes(response,65536);
		var error=new RowStreamException("ROW_STREAM_BUSINESS_ERROR",status);error.response=response;return error;
	}
	public JSONObject response() {
		if (response != null) return response;
		try { return new JSONObject().put("error",error()); } catch (JSONException impossible) { throw new IllegalStateException(impossible); }
	}
	public int status() { return status; }
	public String code() { return code; }
	public JSONObject error() {
		if (response != null) return response.optJSONObject("error");
		try { return new JSONObject().put("code", code).put("message", getMessage()).put("details", "").put("status", status); }
		catch (JSONException impossible) { throw new IllegalStateException(impossible); }
	}
	public static RowStreamException safe(Exception error) {
		return error instanceof RowStreamException stream ? stream : new RowStreamException("ROW_STREAM_FAILED", 502);
	}
}
