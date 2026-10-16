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
	private final String json;
	private final String nodeName;
	private final String arrayChildrenTag;

	public JsonNativeOutput(String key, String json, String nodeName, String arrayChildrenTag) {
		this.key = key;
		this.json = json;
		this.nodeName = nodeName;
		this.arrayChildrenTag = arrayChildrenTag;
	}

	public String getKey() {
		return key;
	}

	@Override
	public String toJSONString() {
		return json;
	}

	/** The element JsonToXmlStep.createStepNode builds for this JSON */
	public Element toElement(Document doc) throws JSONException {
		Element elt = doc.createElement("e");
		Object o = json.startsWith("{") ? new JSONObject(json) : new JSONArray(json);
		XMLUtils.jsonToXml(o, nodeName, elt, true, false, arrayChildrenTag);
		elt = (Element) elt.getFirstChild();
		elt.setAttribute("originalKeyName", key);
		return elt;
	}
}
