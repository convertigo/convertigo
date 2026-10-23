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
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.Engine;

public class RequestableStepCloneTest {
	@BeforeClass
	public static void initializeLogging() {
		Engine.logBeans = Logger.getLogger("requestable-step-clone-test");
		Engine.logEngine = Engine.logBeans;
	}

	private static StepVariable variable(String name) throws Exception {
		var variable = new StepVariable();
		variable.setName(name);
		return variable;
	}

	@Test
	public void aCloneOrdersItsOwnVariables() throws Exception {
		var original = new SequenceStep();
		var declared = variable("declared");
		original.addVariable(declared);
		var clone = original.clone();
		assertNotSame(original.getOrderedVariables(), clone.getOrderedVariables());
		assertEquals("the clone starts with the order of the original", List.of(declared.priority), clone.getOrderedVariables().get(0));

		clone.addVariable(variable("added"));
		assertEquals("a variable added to the clone stays out of the original", List.of(declared.priority), original.getOrderedVariables().get(0));
	}

	@Test
	public void concurrentRunsAddVariablesToTheirOwnClone() throws Exception {
		var original = new SequenceStep();
		var failures = new AtomicInteger();
		var threads = new Thread[8];
		for (int t = 0; t < threads.length; t++) {
			threads[t] = new Thread(() -> {
				for (int i = 0; i < 500; i++) {
					try {
						var clone = original.clone();
						for (var name : List.of("target", "depth", "__nolog", "__sequence")) {
							clone.addVariable(variable(name));
						}
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
		assertEquals("runs that failed", 0, failures.get());
		assertEquals("entries left in the order of the original", 0, original.getOrderedVariables().get(0).size());
	}
}
