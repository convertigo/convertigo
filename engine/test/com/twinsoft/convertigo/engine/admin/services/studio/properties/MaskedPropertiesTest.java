/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.steps.SmtpStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class MaskedPropertiesTest {
	private static final String PASSWORD = "smtpPassword";

	@BeforeClass
	public static void initializeEngineProperties() throws Exception {
		Engine.logBeans = Logger.getLogger(MaskedPropertiesTest.class);
		EnginePropertiesManager.initProperties();
	}

	@Test
	public void jsonPropertyOnlyShowsWhetherAValueIsSet() throws Exception {
		var step = new SmtpStep();
		assertFalse(jsonProperty(step).getBoolean("hasMaskedValue"));

		step.setSmtpPassword(UUID.randomUUID().toString());
		var property = jsonProperty(step);
		assertTrue(property.getBoolean("isMasked"));
		assertTrue(property.getBoolean("hasMaskedValue"));
		assertFalse(property.has("value"));
		assertFalse(property.has("ciphered"));
		assertFalse(property.has("compiledValue"));
	}

	@Test
	public void unchangedCiphertextMatchesSymbolSourceBeforeCompilation() throws Exception {
		var step = new SmtpStep();
		var source = "${mail.fixture.secret}";
		step.setSmtpPassword(UUID.randomUUID().toString());
		step.setCompilablePropertySourceValue(PASSWORD, source);

		var xmlProperty = xmlProperty(step);
		var value = (Element) xmlProperty.getFirstChild();
		assertTrue(xmlProperty.getAttribute("ciphered").equals("true"));
		assertFalse(value.hasAttribute("compiledValue"));
		assertFalse(value.hasAttribute("compiledValueClass"));
		assertTrue(step.matchesCipheredPropertyValue(PASSWORD, value.getAttribute("value"), step.getSmtpPassword()));
		assertFalse(step.matchesCipheredPropertyValue(PASSWORD, "", step.getSmtpPassword()));
		assertTrue(jsonProperty(step).getBoolean("hasMaskedValue"));
	}

	private static JSONObject jsonProperty(SmtpStep step) throws Exception {
		var xml = xmlProperty(step);
		var properties = new JSONObject();
		new Get().addDboProperties(step, properties, xml);
		return properties.getJSONObject(xml.getAttribute("displayName"));
	}

	private static Element xmlProperty(SmtpStep step) throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var object = com.twinsoft.convertigo.engine.admin.services.database_objects.Get
				.getProperties(step, document, step.getQName());
		for (var node = object.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element property && PASSWORD.equals(property.getAttribute("name"))) {
				return property;
			}
		}
		throw new AssertionError("SMTP password property is absent");
	}
}
