/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.requesters;

import java.util.Map;
import com.twinsoft.convertigo.engine.util.JsonValues;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import jakarta.servlet.http.HttpServletRequest;

/** Internal JSON boundary retaining all regular requestable checks, context cleanup and billing. */
public class NativeJsonRequester extends InternalRequester {
	private boolean rawResult;
	public void setRawResult(boolean rawResult) { this.rawResult = rawResult; }
	public boolean isRawResult() { return rawResult; }
	public NativeJsonRequester(Map<String, Object> request, HttpServletRequest parent) throws EngineException {
		super(request, parent);
	}
	@Override public String getName() { return "NativeJsonRequester"; }
	/** Copy JSON values into the caller's realm without serializing and parsing them again. */
	public Object processRequestAsObject(Scriptable scope) throws Exception {
		Object result;
		try (var allocation = com.twinsoft.convertigo.engine.util.AllocationTrace.open("boundary", "internal-call", "")) {
			result = processRequest();
		}
		try (var allocation = com.twinsoft.convertigo.engine.util.AllocationTrace.open("boundary", "caller-conversion", "")) {
			return com.twinsoft.convertigo.engine.util.DetachedJson.toScriptable(result, ScriptableObject.getTopLevelScope(scope));
		}
	}
	@Override public boolean acceptNativeJson() { return true; }
	@Override public Object postGetNativeJson() throws Exception {
		if (rawResult) return context.nativeJsonResponse;
		var value = new java.util.LinkedHashMap<String, Object>(context.nativeJsonResponse);
		value.put("attr", JsonValues.fromLegacy(context.nativeJsonAttributes));
		return JsonValues.object("document", value);
	}
	@Override public Object postGetDocument(Document document) throws Exception {
		// Legacy sequences and connectors still own their XML model. Convert only at this boundary.
		super.postGetDocument(document);
		return JsonValues.parse(XMLUtils.XmlToJson(document.getDocumentElement(), true, true, JsonRoot.docNode));
	}
}
