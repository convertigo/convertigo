/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.GenericRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;
import jakarta.servlet.http.HttpServletRequest;

/** Conservative native boundary. XML-specific options and structured DOM inputs keep the established path. */
public final class NativeJsonSupport {
	private NativeJsonSupport() { }

	public static boolean prepare(Requester requester, Context context) throws Exception {
		if (!(requester instanceof GenericRequester generic) || !generic.acceptNativeJson()
				|| !EnginePropertiesManager.getPropertyAsBoolean(PropertyName.REQUESTABLE_NATIVE_JSON)
				|| !(context.requestedObject instanceof Flow flow) || context.isAsync || context.isStubRequested
				|| context.isXsltRequest || context.removeNamespaces || flow.isBillable()
				|| !flow.getResponseExpiryDate().isEmpty()
				|| context.httpSession != null && context.httpSession.getAttribute("customController") != null) return false;
		Map<?, ?> parameters = requester.inputData instanceof HttpServletRequest request ? request.getParameterMap()
				: requester.inputData instanceof Map<?, ?> map ? map : null;
		if (parameters == null || parameters.containsKey(Parameter.Testcase.getName())) return false;
		var input = new LinkedHashMap<String, Object>();
		for (var entry : parameters.entrySet()) {
			if (!(entry.getKey() instanceof String name)) return false;
			// Controls were processed by initContext/handleParameter; only __body contributes to Flow input.
			if (name.startsWith("__") && !name.equals(Parameter.HttpBody.getName())) continue;
			var value = entry.getValue();
			String[] values;
			if (value instanceof String text) values = new String[] {text};
			else if (value instanceof String[] texts) values = texts;
			else return false;
			// Empty multivalues are represented by XML NodeLists on the legacy path.
			if (values.length == 0) return false;
			var variable = flow.getVariable(name);
			var multi = variable != null && variable.isMultiValued();
			var items = new ArrayList<String>();
			for (var text : values) {
				if ("_empty_array_".equals(text)) return false;
				if (text != null) { if (multi) items.add(text); else input.put(name, text); }
			}
			if (multi && !items.isEmpty()) input.put(name, items);
		}
		context.nativeInput = input;
		context.nativeJson = true;
		return true;
	}

	/** Project typed docChildNodes without synthesizing XML text for an empty native result. */
	public static Map<String, Object> projectResult(Object value, String arrayItem) throws Exception {
		if (hasAttachmentProjection(value)) return null;
		if (value instanceof Map<?, ?> object && !object.isEmpty()) {
			if (!object.containsKey("attr")) return com.twinsoft.convertigo.engine.util.JsonValues.map(object);
			var projected = new LinkedHashMap<String, Object>();
			for (var entry : object.entrySet()) if (!"attr".equals(entry.getKey())) projected.put((String) entry.getKey(), entry.getValue());
			return projected;
		}
		var projected = new LinkedHashMap<String, Object>();
		if (value instanceof java.util.List<?> array) {
			for (var item : array) {
				if (!projected.containsKey(arrayItem)) projected.put(arrayItem, item instanceof java.util.List<?> list ? new ArrayList<>(list) : item);
				else if (projected.get(arrayItem) instanceof java.util.List<?> list) com.twinsoft.convertigo.engine.util.JsonValues.list(list).add(item);
				else { var values = new ArrayList<Object>(); values.add(projected.get(arrayItem)); values.add(item); projected.put(arrayItem, values); }
			}
		} else if (!(value instanceof Map<?, ?>) && value != null) projected.put("value", value);
		return projected;
	}
	private static boolean hasAttachmentProjection(Object value) {
		if (value instanceof Map<?, ?> object) {
			if (object.containsKey("_attachments")) return true;
			for (var item : object.values()) if (hasAttachmentProjection(item)) return true;
		} else if (value instanceof java.util.List<?> array) {
			for (var item : array) if (hasAttachmentProjection(item)) return true;
		}
		return false;
	}
}
