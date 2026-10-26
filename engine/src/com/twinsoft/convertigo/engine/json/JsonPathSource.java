/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.JsonNativeOutput;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * A source whose path is a JSON path ($...): it reads the JSON value of its step (a native step, a JSON to XML step)
 * without XML. The steps that take a value (JsonSourceStep, SimpleSourceStep, a SmartType) get it as is; the XML
 * steps get the XML of the value selected only, built like a JSON to XML step builds it.
 */
public final class JsonPathSource {

	private JsonPathSource() {
	}

	/** the value at the JSON path of the JSON of the step: MISSING, a value, or a list with a wildcard */
	public static Object select(Step step, String path) throws EngineException {
		try {
			return step.selectJson(JsonPath.parse(path));
		} catch (IllegalArgumentException e) {
			throw new EngineException("Invalid JSON path '" + path + "' on the step '" + step.getName() + "'", e);
		}
	}

	/** the values selected as XML nodes, for a step that reads its source by XPath */
	public static NodeList nodes(Step step, String path, Document doc) throws EngineException {
		Object value = select(step, path);
		Element holder = doc.createElement("jsonpath");
		JsonPath jsonPath = JsonPath.parse(path);
		String name = nodeName(step, jsonPath);
		List<?> values = value == JsonPath.MISSING ? Collections.emptyList()
				: jsonPath.hasWildcard() ? (List<?>) value : Collections.singletonList(value);
		for (Object v : values) {
			holder.appendChild(toElement(v, jsonPath.hasWildcard() ? "item" : name, doc));
		}
		if (Engine.logBeans.isDebugEnabled()) {
			Engine.logBeans.debug("(JsonPathSource) XML of " + values.size() + " value(s) at '" + path + "' of '" + step.getName() + "'");
		}
		return holder.getChildNodes();
	}

	private static String nodeName(Step step, JsonPath path) {
		String last = path.getLastKey();
		if (path.isRoot()) {
			return step.getStepNodeName();
		}
		return last != null && XMLUtils.checkName(last) ? last : "item";
	}

	private static Element toElement(Object value, String name, Document doc) throws EngineException {
		value = JsonData.unwrap(value);
		if (JsonData.isArray(value) || JsonData.isObject(value)) {
			try {
				return new JsonNativeOutput(name, RhinoUtils.jsonStringifyData(value), name, "item").toElement(doc);
			} catch (Exception e) {
				throw new EngineException("Unable to build the XML of a JSON value", e);
			}
		}
		Element element = doc.createElement(name);
		if (!JsonData.isNullish(value)) {
			element.setTextContent(JsonData.toStr(value));
		}
		return element;
	}

	/** the text of a value: a string, a number or a boolean as JavaScript writes them, an object or array in JSON */
	public static String text(Object value) {
		value = JsonData.unwrap(value);
		if (value == JsonPath.MISSING || value == null) {
			return null;
		}
		if (JsonData.isArray(value) || JsonData.isObject(value)) {
			return RhinoUtils.jsonStringifyData(value);
		}
		return JsonData.toStr(value);
	}

	/** a value for the scope, as JSON.parse would give it: the Java maps and lists become JavaScript data */
	public static Object toJavaScript(Object value) {
		value = JsonData.unwrap(value);
		if (value == JsonPath.MISSING) {
			return null;
		}
		if (value instanceof Map || value instanceof List) {
			return value instanceof Scriptable ? value : RhinoUtils.jsonParse(RhinoUtils.jsonStringifyData(value));
		}
		return value;
	}
}
