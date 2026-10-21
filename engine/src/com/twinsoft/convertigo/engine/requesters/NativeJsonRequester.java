/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.requesters;

import java.util.Map;
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
		return toScriptable(processRequest(), ScriptableObject.getTopLevelScope(scope));
	}
	private static Object toScriptable(Object value, Scriptable scope) throws Exception {
		var javascript = org.mozilla.javascript.Context.getCurrentContext();
		if (value instanceof JSONObject object) {
			var result = javascript.newObject(scope);
			for (var keys = object.keys(); keys.hasNext();) {
				var key = (String) keys.next();
				// Define an own data property: __proto__ is JSON data, never a prototype setter.
				ScriptableObject.defineProperty(result, key, toScriptable(object.get(key), scope), ScriptableObject.EMPTY);
			}
			return result;
		}
		if (value instanceof JSONArray array) {
			var items = new Object[array.length()];
			for (int i = 0; i < items.length; i++) items[i] = toScriptable(array.get(i), scope);
			return javascript.newArray(scope, items);
		}
		if (value == null || value == JSONObject.NULL || value == JSONObject.EXPLICIT_NULL) return null;
		if (value instanceof Number number) return number.doubleValue();
		if (value instanceof String || value instanceof Boolean) return value;
		throw new IllegalArgumentException("Expected a JSON result, received " + value.getClass().getName());
	}
	@Override public boolean acceptNativeJson() { return true; }
	@Override public Object postGetNativeJson() throws Exception {
		if (rawResult) return context.nativeJsonResponse;
		var value = new JSONObject();
		for (var keys = context.nativeJsonResponse.keys(); keys.hasNext();) {
			var key = (String) keys.next(); value.put(key, context.nativeJsonResponse.get(key));
		}
		value.put("attr", context.nativeJsonAttributes);
		return new JSONObject().put("document", value);
	}
	@Override public Object postGetDocument(Document document) throws Exception {
		// Legacy sequences and connectors still own their XML model. Convert only at this boundary.
		super.postGetDocument(document);
		return new JSONObject(XMLUtils.XmlToJson(document.getDocumentElement(), true, true, JsonRoot.docNode));
	}
}
