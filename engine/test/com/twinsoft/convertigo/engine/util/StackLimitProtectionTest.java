/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class StackLimitProtectionTest {
	private static final PropertyName LIMIT = PropertyName.STACK_LIMIT_PROTECTION;
	private String previousLimit;
	private Logger previousLogger;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		try {
			previousLimit = EnginePropertiesManager.getOriginalProperty(LIMIT);
		} catch (IllegalStateException uninitialized) {
			EnginePropertiesManager.initProperties();
			previousLimit = EnginePropertiesManager.getOriginalProperty(LIMIT);
		}
		previousLogger = Engine.logBeans;
		Engine.logBeans = Logger.getLogger("stack-limit-protection-test");
		EnginePropertiesManager.setProperty(LIMIT, LIMIT.getDefaultValue());
	}

	@After
	public void tearDown() {
		EnginePropertiesManager.setProperty(LIMIT, previousLimit);
		Engine.logBeans = previousLogger;
	}

	@Test
	public void limitsJsonToXmlBeforeChangingTheDocument() throws Exception {
		for (int depth : new int[] {999, 1000}) {
			var target = XMLUtils.createDom("java");
			target.appendChild(target.createElement("result"));
			XMLUtils.jsonToXml(nestedJson(depth), "value", target.getDocumentElement(), true, false, "item");
			assertEquals(depth + 2, elementDepth(target.getDocumentElement()));
		}

		var target = XMLUtils.createDom("java");
		var attachments = target.createElement("_attachments");
		target.appendChild(attachments);
		var input = (JSONObject) nestedJson(1001);
		assertLimit(1000, assertThrows(JSONException.class,
				() -> XMLUtils.jsonToXml(input, "value", attachments, true, false, "item")));
		assertFalse(attachments.hasChildNodes());
		assertFalse(attachments.hasAttribute("type"));
		assertFalse(input.has("name"));

		EnginePropertiesManager.setProperty(LIMIT, "0");
		XMLUtils.jsonToXml(input, "value", attachments, true, false, "item");
		assertEquals(1003, elementDepth(attachments));
	}

	@Test
	public void limitsCompactArraysEvenWhenTheOutputIsShallow() throws Exception {
		var target = XMLUtils.createDom("java");
		var root = target.createElement("result");
		target.appendChild(root);

		EnginePropertiesManager.setProperty(LIMIT, "4");
		XMLUtils.jsonToXml(nestedJsonArray(3), "item", root, true, true, "item");
		XMLUtils.jsonToXml(nestedJsonArray(4), "item", root, true, true, "item");
		assertLimit(4, assertThrows(JSONException.class,
				() -> XMLUtils.jsonToXml(nestedJsonArray(5), "item", root, true, true, "item")));
		assertEquals(2, root.getChildNodes().getLength());

		EnginePropertiesManager.setProperty(LIMIT, "0");
		XMLUtils.jsonToXml(nestedJsonArray(5), "item", root, true, true, "item");
		assertEquals(3, root.getChildNodes().getLength());
		assertEquals(2, elementDepth(root));
	}

	@Test
	public void limitsDeepDocumentImportsBeforeChangingTheDestination() throws Exception {
		for (int depth : new int[] {999, 1000}) {
			var target = XMLUtils.createDom("java");
			target.appendChild(target.createElement("result"));
			XMLUtils.copyDocument(nestedXml(depth), target);
			assertEquals(depth + 1, elementDepth(target.getDocumentElement()));
			assertEquals("urn:test", target.getDocumentElement().getFirstChild().getNamespaceURI());
		}
		var target = XMLUtils.createDom("java");
		target.appendChild(target.createElement("result"));
		var input = nestedXml(1001);
		assertLimit(1000, assertThrows(IllegalArgumentException.class, () -> XMLUtils.copyDocument(input, target)));
		assertFalse(target.getDocumentElement().hasChildNodes());

		EnginePropertiesManager.setProperty(LIMIT, "0");
		XMLUtils.copyDocument(input, target);
		assertEquals(1002, elementDepth(target.getDocumentElement()));
	}

	@Test
	public void limitsDocumentCopiesWithoutNamespaces() throws Exception {
		for (int depth : new int[] {999, 1000}) {
			var copy = XMLUtils.copyDocumentWithoutNamespace(nestedXml(depth));
			assertEquals(depth, elementDepth(copy.getDocumentElement()));
			assertEquals("level", copy.getDocumentElement().getNodeName());
		}
		var input = nestedXml(1001);
		assertLimit(1000, assertThrows(IllegalArgumentException.class, () -> XMLUtils.copyDocumentWithoutNamespace(input)));

		EnginePropertiesManager.setProperty(LIMIT, "0");
		assertEquals(1001, elementDepth(XMLUtils.copyDocumentWithoutNamespace(input).getDocumentElement()));
	}

	@Test
	public void limitsNormalizedTextExtractionAndKeepsSkippingScripts() throws Exception {
		assertEquals("leaf", XMLUtils.getNormalizedText(nestedXml(999)));
		assertEquals("leaf", XMLUtils.getNormalizedText(nestedXml(1000)));
		var input = nestedXml(1001);
		assertLimit(1000, assertThrows(IllegalArgumentException.class, () -> XMLUtils.getNormalizedText(input)));
		var script = input.createElement("script");
		var root = input.getDocumentElement();
		script.appendChild(root.getFirstChild());
		root.appendChild(script);
		assertEquals("", XMLUtils.getNormalizedText(input));
		root.appendChild(script.getFirstChild());

		EnginePropertiesManager.setProperty(LIMIT, "0");
		assertEquals("leaf", XMLUtils.getNormalizedText(input));
	}

	@Test
	public void limitsProjectYamlElements() throws Exception {
		var source = folder.newFile("c8oProject.yaml");
		for (int depth : new int[] {999, 1000}) {
			Files.writeString(source.toPath(), nestedYaml(depth));
			assertEquals(depth + 1, elementDepth(YamlConverter.readYaml(source).getDocumentElement()));
		}
		Files.writeString(source.toPath(), nestedYaml(1001));
		assertLimit(1000, assertThrows(IllegalArgumentException.class, () -> YamlConverter.readYaml(source)));

		EnginePropertiesManager.setProperty(LIMIT, "0");
		assertEquals(1002, elementDepth(YamlConverter.readYaml(source).getDocumentElement()));
	}

	@Test
	public void limitsYamlArraysAndKeepsDepthAcrossIncludedFiles() throws Exception {
		var source = folder.newFile("c8oProject.yaml");
		var included = folder.newFolder("_c8oProject").toPath().resolve("included.yaml");
		Files.writeString(source.toPath(), "↓Example [core.Project]: 🗏 included.yaml\n");
		Files.writeString(included, "value: \n  ↓children: \n  - item: \n      - item: leaf\n");
		int depth = 4;
		EnginePropertiesManager.setProperty(LIMIT, "5");
		assertEquals(depth + 1, elementDepth(YamlConverter.readYaml(source).getDocumentElement()));
		EnginePropertiesManager.setProperty(LIMIT, "4");
		assertEquals(depth + 1, elementDepth(YamlConverter.readYaml(source).getDocumentElement()));
		EnginePropertiesManager.setProperty(LIMIT, "3");
		assertLimit(3, assertThrows(IllegalArgumentException.class, () -> YamlConverter.readYaml(source)));
		EnginePropertiesManager.setProperty(LIMIT, "0");
		assertEquals(depth + 1, elementDepth(YamlConverter.readYaml(source).getDocumentElement()));
	}

	private static void assertLimit(int limit, Exception error) {
		assertTrue(error.getMessage(), error.getMessage().contains("maximum depth of " + limit));
		assertTrue(error.getMessage(), error.getMessage().contains(LIMIT.getKey()));
	}

	private static Object nestedJson(int depth) throws JSONException {
		Object value = "leaf";
		for (int i = depth; i > 0; i--) {
			value = i % 2 == 0 ? new JSONArray().put(value) : new JSONObject().put("item", value);
		}
		return value;
	}

	private static Object nestedJsonArray(int depth) {
		Object value = "leaf";
		for (int i = 0; i < depth; i++) {
			value = new JSONArray().put(value);
		}
		return value;
	}

	private static Document nestedXml(int depth) throws Exception {
		var doc = XMLUtils.createDom("java");
		Node current = doc;
		for (int i = 0; i < depth; i++) {
			current = current.appendChild(doc.createElementNS("urn:test", "p:level"));
		}
		current.appendChild(doc.createTextNode("leaf"));
		return doc;
	}

	private static int elementDepth(Node root) {
		int depth = 0;
		for (Node node = root; node != null && node.getNodeType() == Node.ELEMENT_NODE; node = node.getFirstChild()) {
			depth++;
		}
		return depth;
	}

	private static String nestedYaml(int depth) {
		var yaml = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			yaml.append("  ".repeat(i)).append("level: ").append(i == depth - 1 ? "leaf" : "").append('\n');
		}
		return yaml.toString();
	}
}
