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
	/**
	 * The key that tells, beside the answer (result or document), whether the requestable failed: true when it answered
	 * an error instead of its result (a Flow whose run ended in error, an error document), false otherwise. A result may
	 * hold an error property: the caller reads this key rather than the shape of the answer.
	 */
	public static final String FAILED = "failed";
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
	/** true when the request answered an error instead of the result of its requestable */
	private boolean failed() { return context.runFailed || context.isErrorDocument; }
	@Override public Object postGetNativeJson() throws Exception {
		if (rawResult) {
			var value = new java.util.LinkedHashMap<String, Object>(context.nativeJsonResponse);
			value.put(FAILED, failed());
			return value;
		}
		var value = new java.util.LinkedHashMap<String, Object>(context.nativeJsonResponse);
		value.put("attr", JsonValues.fromLegacy(context.nativeJsonAttributes));
		return JsonValues.object("document", value, FAILED, failed());
	}
	@Override public Object postGetDocument(Document document) throws Exception {
		// Legacy sequences and connectors still own their XML model. Convert only at this boundary.
		super.postGetDocument(document);
		var value = JsonValues.parse(XMLUtils.XmlToJson(document.getDocumentElement(), true, true, JsonRoot.docNode));
		if (value instanceof Map<?, ?>) JsonValues.map(value).put(FAILED, failed());
		return value;
	}
}
