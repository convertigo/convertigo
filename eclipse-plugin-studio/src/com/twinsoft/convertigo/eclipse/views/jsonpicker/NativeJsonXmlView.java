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

package com.twinsoft.convertigo.eclipse.views.jsonpicker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.twinsoft.convertigo.beans.steps.JsonTransformStep;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;
import com.twinsoft.convertigo.engine.util.JsonNativeOutput;

/**
 * The XML view a native JSON step gives to the XPath sources of the next steps (JsonTransformStep.getContextNode),
 * built in the Studio from its last JSON, for the source picker.
 */
public class NativeJsonXmlView {
	private static final ObjectMapper mapper = new ObjectMapper();
	/** the items of an array kept in the view of the picker: its structure, not its volume */
	private static final int MAX_ITEMS = 50;

	private NativeJsonXmlView() {
	}

	public static Element build(JsonTransformStep step, Document doc) {
		NativeStepOutput sample = NativeJson.getSample(step);
		String nodeName = step.getStepNodeName();
		if (sample == null || sample.getJson().isEmpty()) {
			Element element = doc.createElement(nodeName);
			element.appendChild(doc.createComment(" no sample: execute the sequence in the Studio to see the XML view of '" + step.getName() + "' "));
			return element;
		}
		String json = sample.getJson();
		boolean cut = false;
		try {
			JsonNode node = mapper.readTree(json);
			if (node.isArray() && node.size() > MAX_ITEMS) {
				ArrayNode first = mapper.createArrayNode();
				for (int i = 0; i < MAX_ITEMS; i++) {
					first.add(node.get(i));
				}
				json = mapper.writeValueAsString(first);
				cut = true;
			}
			Element element = new JsonNativeOutput(step.getVariableName(), json, nodeName, "item").toElement(doc);
			if (cut) {
				element.insertBefore(doc.createComment(" the first " + MAX_ITEMS + " items of " + node.size() + " "), element.getFirstChild());
			}
			return element;
		} catch (Exception e) {
			Element element = doc.createElement(nodeName);
			element.appendChild(doc.createComment(" " + e.getMessage() + " "));
			return element;
		}
	}
}
