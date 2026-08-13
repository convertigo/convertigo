/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import com.twinsoft.convertigo.engine.util.XMLUtils;

public class FlowInputValueTest {
	@Test
	public void preservesTypedObjectsInsideVariableWrappers() throws Exception {
		var document = XMLUtils.parseDOMFromString("""
				<variable>
					<item type="object">
						<user type="string">aaaa</user>
						<active type="boolean">true</active>
						<count type="integer">2</count>
					</item>
					<item type="object">
						<user type="string">bbbb</user>
						<active type="boolean">false</active>
						<count type="integer">3</count>
					</item>
				</variable>
				""");

		var value = Flow.toJsonValue(document.getDocumentElement());

		assertTrue(value instanceof JSONArray);
		var items = (JSONArray) value;
		assertEquals(2, items.length());
		assertEquals("aaaa", items.getJSONObject(0).getString("user"));
		assertTrue(items.getJSONObject(0).getBoolean("active"));
		assertEquals(2, items.getJSONObject(0).getInt("count"));
		assertEquals("bbbb", items.getJSONObject(1).getString("user"));
	}
}
