/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class RequestableObjectCloneTest {
	private static Engine previous;

	@BeforeClass
	public static void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logContext = Logger.getLogger("requestable-object-clone-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
	}

	@AfterClass
	public static void restore() {
		Engine.theApp = previous;
	}

	private interface Run {
		void run() throws Exception;
	}

	private static int failedConcurrentRuns(Run run) throws Exception {
		var failures = new AtomicInteger();
		var threads = new Thread[8];
		for (int t = 0; t < threads.length; t++) {
			threads[t] = new Thread(() -> {
				for (int i = 0; i < 500; i++) {
					try {
						run.run();
					} catch (Throwable e) {
						failures.incrementAndGet();
					}
				}
			});
			threads[t].start();
		}
		for (var thread : threads) {
			thread.join();
		}
		return failures.get();
	}

	private static RequestableVariable variable(String name) throws Exception {
		var variable = new RequestableVariable();
		variable.setName(name);
		return variable;
	}

	private static SimpleStep step(String name) throws Exception {
		var step = new SimpleStep();
		step.setName(name);
		return step;
	}

	@Test
	public void aSequenceCloneOrdersItsOwnStepsAndVariables() throws Exception {
		var original = new GenericSequence();
		var declaredStep = step("declared");
		original.addStep(declaredStep);
		var declaredVariable = variable("declared");
		original.addVariable(declaredVariable);
		var clone = original.clone();
		assertNotSame(original.getOrderedSteps(), clone.getOrderedSteps());
		assertNotSame(original.getOrderedVariables(), clone.getOrderedVariables());
		assertEquals("the clone starts with the step order of the original", List.of(declaredStep.priority), clone.getOrderedSteps().get(0));
		assertEquals("the clone starts with the variable order of the original", List.of(declaredVariable.priority), clone.getOrderedVariables().get(0));

		clone.addStep(step("added"));
		clone.addVariable(variable("added"));
		assertEquals("a step added to the clone stays out of the original", List.of(declaredStep.priority), original.getOrderedSteps().get(0));
		assertEquals("a variable added to the clone stays out of the original", List.of(declaredVariable.priority), original.getOrderedVariables().get(0));

		var empty = new GenericSequence();
		var failures = failedConcurrentRuns(() -> {
			var run = empty.clone();
			for (var name : List.of("target", "depth", "__nolog", "__sequence")) {
				run.addVariable(variable(name));
			}
		});
		assertEquals("runs that failed", 0, failures);
		assertEquals("entries left in the variable order of the original", 0, empty.getOrderedVariables().get(0).size());
	}

	@Test
	public void dynamicVariablesStayInTheTransactionOfTheirRun() throws Exception {
		var project = new Project();
		project.setName("clone_test");
		var connector = new HttpConnector();
		connector.setName("http");
		project.add(connector);
		var transaction = new HttpTransaction();
		transaction.setName("call");
		connector.add(transaction);
		var declared = new RequestableHttpVariable();
		declared.setName("declared");
		transaction.addVariable(declared);

		// A context runs on its own clone of the project, and __POST_ and __GET_ parameters add variables to its transaction.
		Run run = () -> {
			var clone = (HttpTransaction) project.clone().getConnectorByName("http").getTransactionByName("call");
			assertEquals("the clone starts with the order of the original", List.of(declared.priority), clone.getOrderedVariables().get(0));
			var input = XMLUtils.getDefaultDocumentBuilder().newDocument();
			input.appendChild(input.createElement("input"));
			for (var name : List.of("__POST_a", "__GET_b")) {
				var variable = input.createElement("variable");
				variable.setAttribute("name", name);
				variable.setAttribute("value", "1");
				input.getDocumentElement().appendChild(variable);
			}
			var context = new Context("clone-test");
			context.inputDocument = input;
			clone.parseInputDocument(context);
		};
		run.run();
		assertEquals("dynamic variables stay out of the original", List.of(declared.priority), transaction.getOrderedVariables().get(0));

		assertEquals("runs that failed", 0, failedConcurrentRuns(run));
		assertEquals("order of the original after concurrent runs", List.of(declared.priority), transaction.getOrderedVariables().get(0));
	}
}
