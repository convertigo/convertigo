/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.YamlConverter;

/**
 * A project made with a newer engine, with a step class this engine does not know: the project loads without that
 * step and its children, a warning names the project, the sequence and the class, and that sequence answers an error.
 */
public class UnknownBeanClassTest {
	private static final String UNKNOWN = "com.twinsoft.convertigo.beans.steps.FutureUnknownStep";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void theProjectLoadsWithoutTheUnknownBeanAndItsSequenceAnswersAnError() throws Exception {
		Logger logger = Logger.getLogger("unknown-bean-test");
		Engine.logBeans = logger;
		Engine.logEngine = logger;

		File project = folder.newFolder("Demo");
		File yaml = new File(project, "c8oProject.yaml");
		Files.write(yaml.toPath(), String.join("\n",
				"↑convertigo: 8.0.0.m006",
				"↓Demo [core.Project]: ",
				"  ↓Broken [sequences.GenericSequence]: ",
				"    ↓Before [steps.SimpleStep-1001]: ",
				"      expression: before = 1;",
				"    ↓Future [steps.FutureUnknownStep-1002]: ",
				"      futureProperty: x",
				"      ↓Inside [steps.SimpleStep-1003]: ",
				"        expression: inside = 1;",
				"    ↓After [steps.SimpleStep-1004]: ",
				"      expression: after = 1;",
				"  ↓Fine [sequences.GenericSequence]: ",
				"    ↓Only [steps.SimpleStep-1005]: ",
				"      expression: only = 1;",
				"").getBytes(StandardCharsets.UTF_8));

		Document document = BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml));

		List<String> classes = new ArrayList<String>();
		for (Element bean : beans(document)) {
			classes.add(bean.getAttribute("classname"));
		}
		assertTrue("the unknown bean is left out: " + classes, !classes.contains(UNKNOWN));
		assertEquals("the project, its two sequences and the known steps, without the child of the unknown one", 6, classes.size());

		Element broken = sequence(document, "Broken");
		Element fine = sequence(document, "Fine");
		assertEquals(UNKNOWN, broken.getAttribute(BeansDefaultValues.SKIPPED_BEANS));
		assertEquals("", fine.getAttribute(BeansDefaultValues.SKIPPED_BEANS));

		// the warning logged on cems.Beans, also kept on the document
		@SuppressWarnings("unchecked")
		List<String> warnings = (List<String>) document.getUserData(BeansDefaultValues.SKIPPED_BEANS);
		assertEquals(1, warnings.size());
		String warning = warnings.get(0);
		assertTrue(warning, warning.contains(UNKNOWN));
		assertTrue(warning, warning.contains("project \"Demo\", sequence \"Broken\""));
		assertTrue(warning, warning.contains("bean \"Future\""));

		// what DatabaseObject.read does with the attribute when the engine imports the sequence
		Sequence brokenSequence = new GenericSequence();
		brokenSequence.setName("Broken");
		brokenSequence.preconfigure(broken);
		Sequence fineSequence = new GenericSequence();
		fineSequence.setName("Fine");
		fineSequence.preconfigure(fine);
		assertEquals(UNKNOWN, brokenSequence.skippedBeans());
		assertNull(fineSequence.skippedBeans());
		// each request runs a copy of the sequence
		assertEquals(UNKNOWN, brokenSequence.clone().skippedBeans());
		try {
			brokenSequence.prepareForRequestable(null, null, null);
			fail("the sequence must not run without its step");
		} catch (EngineException e) {
			assertNotNull(e.getMessage());
			assertTrue(e.getMessage(), e.getMessage().contains("\"Broken\"") && e.getMessage().contains(UNKNOWN));
		}
	}

	private static Element sequence(Document document, String name) {
		for (Element bean : beans(document)) {
			if (bean.getAttribute("classname").startsWith("com.twinsoft.convertigo.beans.sequences.") && name.equals(nameOf(bean))) {
				return bean;
			}
		}
		throw new AssertionError("no sequence " + name);
	}

	/** the beans of an unshrunk project: the elements with a class and a priority (project, sequence, step...) */
	private static List<Element> beans(Document document) {
		List<Element> beans = new ArrayList<Element>();
		NodeList elements = document.getElementsByTagName("*");
		for (int i = 0; i < elements.getLength(); i++) {
			Element element = (Element) elements.item(i);
			if (element.hasAttribute("classname") && element.hasAttribute("priority")) {
				beans.add(element);
			}
		}
		return beans;
	}

	/** the value of the name property of a bean: <property name="name"><java.lang.String value="..."/></property> */
	private static String nameOf(Element bean) {
		for (org.w3c.dom.Node child = bean.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && "property".equals(child.getNodeName()) && "name".equals(((Element) child).getAttribute("name"))) {
				NodeList values = ((Element) child).getElementsByTagName("*");
				return values.getLength() > 0 ? ((Element) values.item(0)).getAttribute("value") : null;
			}
		}
		return null;
	}
}
