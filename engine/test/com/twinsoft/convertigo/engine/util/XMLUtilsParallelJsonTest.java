/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.JsonSourceStep;
import com.twinsoft.convertigo.beans.steps.SourceStep;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.XMLUtils.XmlToJsonDepthException;

/**
 * The branches of a ParallelStep convert to JSON, at the same time, the answers their transaction steps imported in
 * the same sequence document (JsonSourceStep). Each conversion must give the JSON of its own elements and end, and an
 * deep element must fail with a conversion error.
 */
public class XMLUtilsParallelJsonTest {
	private static final int BRANCHES = 8;
	private static final int ROWS = 60;
	private static final int ROUNDS = 300;

	private static Logger previousLogger;

	@BeforeClass
	public static void setUpClass() throws Exception {
		try {
			EnginePropertiesManager.getProperty(PropertyName.STACK_LIMIT_PROTECTION);
		} catch (IllegalStateException uninitialized) {
			EnginePropertiesManager.initProperties();
		}
		previousLogger = Engine.logEngine;
		Engine.logEngine = Logger.getLogger("xml-parallel-json-test");
	}

	@AfterClass
	public static void tearDownClass() {
		Engine.logEngine = previousLogger;
	}

	@Test
	public void branchesConvertTheirOwnRowsOfTheSameDocument() throws Exception {
		String expected = rowsToJson(results(answer().getDocumentElement()));
		for (int round = 0; round < ROUNDS; round++) {
			Document sequence = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element root = sequence.createElement("sequence");
			sequence.appendChild(root);
			List<Element> results = new ArrayList<Element>();
			for (int branch = 0; branch < BRANCHES; branch++) {
				Element step = sequence.createElement("Call_Transaction");
				root.appendChild(step);
				Element imported = (Element) sequence.importNode(answer().getDocumentElement(), true);
				step.appendChild(imported);
				results.add(results(imported));
			}

			CyclicBarrier barrier = new CyclicBarrier(BRANCHES);
			List<AtomicReference<Object>> outcomes = new ArrayList<AtomicReference<Object>>();
			List<Thread> threads = new ArrayList<Thread>();
			for (int branch = 0; branch < BRANCHES; branch++) {
				Element branchResults = results.get(branch);
				AtomicReference<Object> outcome = new AtomicReference<Object>();
				outcomes.add(outcome);
				Thread thread = new Thread(() -> {
					try {
						barrier.await();
						outcome.set(rowsToJson(branchResults));
					} catch (Throwable t) {
						outcome.set(t);
					}
				}, "branch-" + branch);
				thread.setDaemon(true);
				threads.add(thread);
				thread.start();
			}
			for (int branch = 0; branch < BRANCHES; branch++) {
				threads.get(branch).join(10000);
				assertFalse("round " + round + ": branch " + branch + " is still converting after 10 s", threads.get(branch).isAlive());
				assertEquals("round " + round + ": JSON of branch " + branch, expected, outcomes.get(branch).get());
			}
		}
	}

	@Test
	public void jsonSourcesLeaveTheSharedXmlSourceIntact() throws Exception {
		Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element root = doc.createElement("source");
		doc.appendChild(root);
		Element amount = typed(doc, root, "amount", "number", "42");
		amount.setAttribute("number", "business attribute");
		NodeList nodes = root.getChildNodes();

		Engine previousEngine = Engine.theApp;
		Logger previousBeans = Engine.logBeans;
		Engine.theApp = new Engine();
		Engine.logBeans = Engine.logEngine;
		var javascript = org.mozilla.javascript.Context.enter();
		try {
			var sequence = new GenericSequence() {
				@Override public boolean isRunning() { return true; }
				@Override public void addCopy(String id, Step step) {}
				@Override public void appendStepNode(Step step) {}
			};
			sequence.context = new Context("xml-json-test");
			Scriptable scope = javascript.initStandardObjects();
			var first = jsonSource(sequence, nodes, "first");
			var second = jsonSource(sequence, nodes, "second");
			assertTrue(first.execute(javascript, scope));
			assertTrue(second.execute(javascript, scope));
			assertEquals(42, ((Number) scope.get("first", scope)).intValue());
			assertEquals(42, ((Number) scope.get("second", scope)).intValue());
			var readXml = new SourceStep() {
				@Override protected StepSource getSource() { return sharedSource(this, nodes); }
			};
			readXml.setParent(sequence);
			assertTrue(readXml.copy().execute(javascript, scope));
			var xml = (Element) ((NodeList) scope.get(readXml.getVariableName(), scope)).item(0);
			assertSame(amount, xml);
			assertEquals("number", xml.getAttribute("type"));
			assertEquals("business attribute", xml.getAttribute("number"));

			Element parent = amount;
			for (int i = 0; i < 1000; i++) {
				parent = typed(doc, parent, "level", "object", null);
			}
			var tooDeep = jsonSource(sequence, nodes, "tooDeep");
			EngineException error = assertThrows(EngineException.class, () -> tooDeep.execute(javascript, scope));
			Throwable cause = error.getCause().getCause();
			assertTrue(cause instanceof XmlToJsonDepthException);
			assertTrue(cause.getMessage(), cause.getMessage().contains("maximum depth of 1000"));
		} finally {
			org.mozilla.javascript.Context.exit();
			Engine.logBeans = previousBeans;
			Engine.theApp = previousEngine;
		}
	}

	private static JsonSourceStep jsonSource(GenericSequence sequence, NodeList nodes, String name)
			throws CloneNotSupportedException {
		var step = new JsonSourceStep() {
			@Override protected StepSource getSource() { return sharedSource(this, nodes); }
		};
		step.setParent(sequence);
		step.setVariableName(name);
		return (JsonSourceStep) step.copy();
	}

	private static StepSource sharedSource(Step step, NodeList nodes) {
		return new StepSource(step, new XMLVector<String>()) {
			@Override public NodeList getContextValues() { return nodes; }
		};
	}

	@Test
	public void limitsXmlDepthByDefault() throws Exception {
		assertNotNull(XMLUtils.XmlToJson(nestedXml(999), true, true, null));
		assertNotNull(XMLUtils.XmlToJson(nestedXml(1000), true, false));

		Element top = nestedXml(1001);
		assertDepthError(1000, () -> XMLUtils.XmlToJson(top, true, true, null));
		assertDepthError(1000, () -> XMLUtils.XmlToJson(top, true, false));
		assertDepthError(1000, () -> XMLUtils.XmlToJsonResponse(top, true, List.of(), "UTF-8"));
		assertDepthError(1000, () -> XMLUtils.handleElement(top, new JSONObject(), true, true));
		top.setAttribute("type", "string");
		assertDepthError(1000, () -> XMLUtils.XmlToJson(top, true, true, null));
	}

	@Test
	public void nonPositiveDepthDisablesTheLimit() throws Exception {
		var property = PropertyName.STACK_LIMIT_PROTECTION;
		String previous = EnginePropertiesManager.getOriginalProperty(property);
		try {
			Element top = nestedXml(1001);
			EnginePropertiesManager.setProperty(property, "0");
			assertNotNull(XMLUtils.XmlToJson(top, true, true, null));
			EnginePropertiesManager.setProperty(property, "-1");
			assertNotNull(XMLUtils.XmlToJson(top, true, true, null));
		} finally {
			EnginePropertiesManager.setProperty(property, previous);
		}
	}

	@Test
	public void appliesACustomDepthLimit() throws Exception {
		var property = PropertyName.STACK_LIMIT_PROTECTION;
		String previous = EnginePropertiesManager.getOriginalProperty(property);
		try {
			EnginePropertiesManager.setProperty(property, "12");
			assertNotNull(XMLUtils.XmlToJson(nestedXml(12), true, true, null));
			assertDepthError(12, () -> XMLUtils.XmlToJson(nestedXml(13), true, true, null));
		} finally {
			EnginePropertiesManager.setProperty(property, previous);
		}
	}

	private static Element nestedXml(int depth) throws Exception {
		Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element top = doc.createElement("level");
		top.setAttribute("type", "object");
		doc.appendChild(top);
		Element parent = top;
		for (int i = 1; i < depth; i++) {
			parent = typed(doc, parent, "level", "object", null);
		}
		parent.setAttribute("type", "string");
		parent.setTextContent("nested text");
		return top;
	}

	private static void assertDepthError(int limit, ThrowingRunnable conversion) {
		XmlToJsonDepthException error = assertThrows(XmlToJsonDepthException.class, conversion);
		assertTrue(error.getMessage(), error.getMessage().contains("maximum depth of " + limit));
		assertTrue(error.getMessage(), error.getMessage().contains(PropertyName.STACK_LIMIT_PROTECTION.getKey()));
	}

	/** the rows a JsonSourceStep on ./document/object/results/object reads, joined as an array */
	private static String rowsToJson(Element results) throws Exception {
		StringBuilder sb = new StringBuilder("[");
		for (Node node = results.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element) {
				if (sb.length() > 1) {
					sb.append(", ");
				}
				sb.append(XMLUtils.XmlToJson((Element) node, true, true, null));
			}
		}
		return sb.append("]").toString();
	}

	private static Element results(Element document) {
		return child(child(document, "object"), "results");
	}

	private static Element child(Element parent, String name) {
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element && name.equals(node.getNodeName())) {
				return (Element) node;
			}
		}
		throw new IllegalStateException("no " + name + " in " + parent.getNodeName());
	}

	/** the XML of a JSON HTTP transaction answering a page of rows */
	private static Document answer() throws Exception {
		Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element document = doc.createElement("document");
		doc.appendChild(document);
		Element object = typed(doc, document, "object", "object", null);
		typed(doc, object, "count", "number", Integer.toString(ROWS));
		typed(doc, object, "previous", "null", null);
		Element results = typed(doc, object, "results", "array", null);
		for (int r = 0; r < ROWS; r++) {
			Element row = typed(doc, results, "object", "object", null);
			typed(doc, row, "id", "number", Integer.toString(r));
			for (int f = 0; f < 8; f++) {
				typed(doc, row, "field" + f, "string", "value " + r + "-" + f);
			}
			Element link = typed(doc, row, "link", "array", null);
			for (int l = 0; l < 2; l++) {
				Element item = typed(doc, link, "object", "object", null);
				typed(doc, item, "id", "number", Integer.toString(l));
				typed(doc, item, "value", "string", "linked " + l);
			}
			Element status = typed(doc, row, "status", "object", null);
			typed(doc, status, "value", "string", "open");
			typed(doc, row, "active", "boolean", "true");
		}
		return doc;
	}

	private static Element typed(Document doc, Element parent, String name, String type, String text) {
		Element element = doc.createElement(name);
		element.setAttribute("type", type);
		if (text != null) {
			element.appendChild(doc.createTextNode(text));
		}
		parent.appendChild(element);
		return element;
	}
}
