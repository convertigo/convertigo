/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.ParallelStep;
import com.twinsoft.convertigo.beans.steps.ReturnStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.WhileStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.cache.FileCacheManager;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SequenceRequestStatisticsTest {
	private static final int CALL_DELAY_MS = 550;

	private final AtomicReference<Exception> callFailure = new AtomicReference<>();
	private final CyclicBarrier parallelCalls = new CyclicBarrier(2);
	private final MultiThreadedHttpConnectionManager connections = new MultiThreadedHttpConnectionManager();
	private HttpServer server;
	private ExecutorService handlers;
	private Project project;
	private Engine previousEngine;
	private String previousUrl;

	@Before
	public void setUp() throws Exception {
		Logger logger = Logger.getLogger(getClass());
		for (var field : Engine.class.getFields()) {
			if (field.getType() == Logger.class && field.get(null) == null) {
				field.set(null, logger);
			}
		}
		EnginePropertiesManager.initProperties();
		previousUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);

		previousEngine = Engine.theApp;
		Engine.theApp = new Engine();
		Engine.theApp.cacheManager = new FileCacheManager();
		Engine.theApp.eventManager = new EventManager();
		Engine.theApp.contextManager = new ContextManager();
		Engine.theApp.contextManager.init();
		Engine.theApp.pluginsManager = new PluginsManager();
		Engine.theApp.httpClient = new HttpClient(connections);

		project = new Project() {
			@Override public ClassLoader getProjectClassLoader() {
				return SequenceRequestStatisticsTest.class.getClassLoader();
			}
		};
		project.setName("StatisticsTest");
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override public Project getOriginalProjectByName(String name, boolean checkOpenable) {
				return project.getName().equals(name) ? project : null;
			}
		};

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		handlers = Executors.newFixedThreadPool(2);
		server.setExecutor(handlers);
		server.createContext("/projects/StatisticsTest/.xml", this::runParallelCall);
		server.start();
		EnginePropertiesManager.setProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL,
				"http://127.0.0.1:" + server.getAddress().getPort());
	}

	@After
	public void tearDown() throws EngineException {
		if (server != null) {
			server.stop(0);
			handlers.shutdownNow();
		}
		connections.shutdown();
		Engine.theApp.contextManager.destroy();
		Engine.theApp = previousEngine;
		EnginePropertiesManager.setProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL, previousUrl);
	}

	@Test(timeout = 15000)
	public void parallelTransactionAndSubsequenceAreCountedOnceInTheRequest() throws Exception {
		var connector = new HttpConnector();
		connector.setName("test");
		project.add(connector);
		var transaction = new XmlHttpTransaction() {
			@Override public void runCore() {
				context.outputDocument.getDocumentElement().appendChild(context.outputDocument.createElement("transactionRan"));
			}
		};
		transaction.setName("Read");
		connector.add(transaction);
		sequence("Sub").add(marker("subsequenceRan"));

		var main = sequence("Main");
		main.add(new SimpleStep("var iteration = 0;"));
		var loop = new WhileStep();
		loop.setCondition("iteration++ < 2");
		main.add(loop);
		var parallel = new ParallelStep();
		loop.addStep(parallel);
		var callTransaction = new TransactionStep();
		callTransaction.setSourceTransaction("StatisticsTest.test.Read");
		callTransaction.setInternalInvoke(false);
		callTransaction.setOutput(true);
		parallel.addStep(callTransaction);
		var callSequence = new SequenceStep();
		callSequence.setSourceSequence("StatisticsTest.Sub");
		callSequence.setInternalInvoke(false);
		callSequence.setOutput(true);
		parallel.addStep(callSequence);

		Document response = request(Map.of("__sequence", "Main"));
		assertNull("a loopback call failed", callFailure.get());
		assertEquals(0, response.getElementsByTagName("error").getLength());
		assertEquals(2, response.getElementsByTagName("transactionRan").getLength());
		assertEquals(2, response.getElementsByTagName("subsequenceRan").getLength());
		assertStepTotals(response);
		long calls = current(response, "executesequencecalls");
		long steps = current(response, "executesequencesteps");
		assertTrue("the calls include the loopback wait", calls >= CALL_DELAY_MS);
		assertTrue("overlapping calls " + calls + " must fit inside steps " + steps, calls <= steps);
		assertEquals(calls, current(response, "host"));
		assertTrue(current(response, "convertigo") >= 0);
		assertEquals(current(response, "totalstat"), calls + current(response, "convertigo"));
	}

	@Test(timeout = 10000)
	public void returnStopsTheSequenceAndFinishesTheStepMeasurement() throws Exception {
		var sequence = sequence("EarlyReturn");
		sequence.add(marker("beforeReturn"));
		sequence.add(new ReturnStep());
		sequence.add(marker("afterReturn"));
		Document response = request(Map.of("__sequence", sequence.getName()));
		assertEquals(1, response.getElementsByTagName("beforeReturn").getLength());
		assertEquals(0, response.getElementsByTagName("afterReturn").getLength());
		assertStepTotals(response);
		assertEquals(0, current(response, "host"));
		assertEquals(current(response, "totalstat"), current(response, "convertigo"));
	}

	private GenericSequence sequence(String name) throws EngineException {
		var sequence = new GenericSequence();
		sequence.setName(name);
		sequence.setAddStatistics(true);
		project.add(sequence);
		return sequence;
	}

	private static XMLElementStep marker(String name) {
		var step = new XMLElementStep();
		step.setNodeName(name);
		step.setOutput(true);
		return step;
	}

	private Document request(Map<String, Object> parameters) throws Exception {
		var input = new HashMap<>(parameters);
		input.put("__project", project.getName());
		return (Document) new InternalRequester(input).processRequest();
	}

	private void runParallelCall(HttpExchange exchange) throws IOException {
		try (exchange) {
			byte[] response;
			try {
				var input = new HashMap<String, Object>();
				String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				for (String pair : form.split("&")) {
					String[] entry = pair.split("=", 2);
					if (entry.length == 2) {
						input.put(URLDecoder.decode(entry[0], StandardCharsets.UTF_8),
								URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
					}
				}
				parallelCalls.await(3, TimeUnit.SECONDS);
				Thread.sleep(CALL_DELAY_MS);
				response = XMLUtils.prettyPrintDOM(request(input)).getBytes(StandardCharsets.UTF_8);
			} catch (Exception e) {
				callFailure.compareAndSet(null, e);
				exchange.sendResponseHeaders(500, -1);
				return;
			}
			exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=UTF-8");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
		}
	}

	private static void assertStepTotals(Document response) {
		long steps = current(response, "executesequencesteps");
		long document = current(response, "getdocument");
		assertTrue("steps must fit inside GetDocument", steps >= 0 && steps <= document);
		long others = current(response, "others");
		assertTrue("(others) must be non-negative", others >= 0);
		assertEquals(document, steps + others + current(response, "workerthreadstart"));
	}

	private static long current(Document response, String name) {
		// Only the outer request's statistics count, never those nested in a call's response.
		var statisticsNodes = response.getElementsByTagName("statistics");
		assertTrue("response must include statistics", statisticsNodes.getLength() > 0);
		Element statistics = (Element) statisticsNodes.item(statisticsNodes.getLength() - 1);
		Element measure = (Element) statistics.getElementsByTagName(name).item(0);
		assertNotNull("missing request measure " + name, measure);
		return Long.parseLong(measure.getAttribute("current"));
	}
}
