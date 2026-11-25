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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.Engine;

/**
 * The branches of a ParallelStep convert to JSON, at the same time, the answers their transaction steps imported in
 * the same sequence document (JsonSourceStep). Each conversion must give the JSON of its own elements and end, and an
 * element too deep for the stack must fail at once instead of being converted again at each level.
 */
public class XMLUtilsParallelJsonTest {
	private static final int BRANCHES = 8;
	private static final int ROWS = 60;
	private static final int ROUNDS = 300;

	private static Logger previousLogger;

	@BeforeClass
	public static void setUpClass() {
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
	public void anElementTooDeepForTheStackFailsAtOnce() throws Exception {
		Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element top = doc.createElement("object");
		top.setAttribute("type", "object");
		doc.appendChild(top);
		Element parent = top;
		for (int i = 0; i < 20000; i++) {
			Element child = doc.createElement("object");
			child.setAttribute("type", "object");
			parent.appendChild(child);
			parent = child;
		}
		AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		Thread thread = new Thread(null, () -> {
			try {
				XMLUtils.XmlToJson(top, true, true, null);
			} catch (Throwable t) {
				failure.set(t);
			}
		}, "deep", 256 * 1024);
		thread.setDaemon(true);
		long start = System.currentTimeMillis();
		thread.start();
		thread.join(20000);
		assertFalse("the conversion of a too deep element is still running after 20 s", thread.isAlive());
		assertTrue("a StackOverflowError is expected, got " + failure.get(), failure.get() instanceof StackOverflowError);
		assertTrue("failed after " + (System.currentTimeMillis() - start) + " ms", System.currentTimeMillis() - start < 20000);
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
