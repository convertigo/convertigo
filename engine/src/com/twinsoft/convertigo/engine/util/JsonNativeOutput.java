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

package com.twinsoft.convertigo.engine.util;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The JSON of an output JSON to XML step, kept as text instead of being built as XML
 * (Sequence.jsonNativeOutput). It is written as is in the JSON response, or turned into the
 * element the step would have produced when the response is built from the XML after all.
 */
public class JsonNativeOutput implements JSONString {
	private final String key;
	private String json;
	private final String nodeName;
	private final String arrayChildrenTag;

	/** for an output whose JSON is written later (JsonStreamOutput) */
	protected JsonNativeOutput(String key, String nodeName, String arrayChildrenTag) {
		this(key, null, nodeName, arrayChildrenTag);
	}

	public JsonNativeOutput(String key, String json, String nodeName, String arrayChildrenTag) {
		this.key = key;
		this.json = json;
		this.nodeName = nodeName;
		this.arrayChildrenTag = arrayChildrenTag;
	}

	private boolean legacy;
	private Object value = NO_VALUE;
	private static final Object NO_VALUE = new Object();
	/** the data the JSON is written from, when its text is not built yet (a native sequence) */
	private Object data = NO_VALUE;

	/** an output of a native sequence: its JSON is written from the data, its text built only when asked */
	public static JsonNativeOutput ofData(String key, Object data, String nodeName, String arrayChildrenTag) {
		JsonNativeOutput output = new JsonNativeOutput(key, null, nodeName, arrayChildrenTag);
		output.data = data;
		return output;
	}

	/** writes the JSON of the output; the data of a native sequence is written directly, by chunks */
	public void writeTo(java.io.Writer writer) throws java.io.IOException {
		if (json == null && data != NO_VALUE) {
			RhinoUtils.writeJsonData(writer, data);
		} else {
			writer.write(toJSONString());
		}
	}

	/** the value of the output for a native call: the data the JSON was written from, or its JSON parsed */
	public JsonNativeOutput value(Object value) {
		this.value = value;
		return this;
	}

	public Object getValue() {
		if (value == NO_VALUE) {
			value = RhinoUtils.jsonParse(toJSONString());
		}
		return value;
	}

	public String getKey() {
		return key;
	}

	/**
	 * an output of a native sequence that was XML before (not directly under the sequence, or a value that is not an
	 * object nor an array): written first, in the form XmlToJson gives the XML part of the answer
	 */
	public JsonNativeOutput legacy(boolean legacy) {
		this.legacy = legacy;
		return this;
	}

	public boolean isLegacy() {
		return legacy;
	}

	@Override
	public String toJSONString() {
		if (json == null && data != NO_VALUE) {
			json = RhinoUtils.jsonStringifyData(data);
		}
		return json;
	}

	/** The element JsonToXmlStep.createStepNode builds for this JSON */
	public Element toElement(Document doc) throws JSONException {
		Element elt = doc.createElement("e");
		String json = toJSONString().trim();
		Object o = json.startsWith("{") ? new JSONObject(json) : json.startsWith("[") ? new JSONArray(json)
				: new org.codehaus.jettison.json.JSONTokener(json).nextValue();
		XMLUtils.jsonToXml(o, nodeName, elt, true, false, arrayChildrenTag);
		elt = (Element) elt.getFirstChild();
		elt.setAttribute("originalKeyName", key);
		return elt;
	}
}
