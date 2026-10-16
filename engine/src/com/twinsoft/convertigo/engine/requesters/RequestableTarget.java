/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.requesters;

import java.util.HashMap;
import java.util.function.Function;

import com.twinsoft.convertigo.beans.core.ITestCaseContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;

/** Resolve an invocation without creating a session or changing an execution context. */
final class RequestableTarget {

	static void validate(Project project, String projectName, Function<String, String> parameters) throws EngineException {
		if (project == null) {
			throw new EngineException("Unknown project \"" + projectName + "\".");
		}
		RequestableObject target = resolve(project, parameters);
		String testCaseName = parameters.apply("__testcase");
		if (testCaseName != null && target instanceof ITestCaseContainer container) {
			var testCase = container.getTestCaseByName(testCaseName);
			if (testCase != null) {
				var defaults = new HashMap<String, String>();
				for (var variable : testCase.getVariables()) {
					String name = variable.getName();
					if ((Parameter.Sequence.getName().equals(name) || Parameter.Connector.getName().equals(name)
							|| Parameter.Transaction.getName().equals(name)) && parameters.apply(name) == null) {
						String value = InternalRequester.getParameterValue(variable.getValueOrNull());
						if (value != null) {
							defaults.put(variable.getName(), value);
						}
					}
				}
				resolve(project, name -> {
					String value = parameters.apply(name);
					return value == null ? defaults.get(name) : value;
				});
			}
		}
	}

	private static RequestableObject resolve(Project project, Function<String, String> parameters) throws EngineException {
		String sequence = parameters.apply(Parameter.Sequence.getName());
		if (sequence != null && !sequence.isEmpty()) {
			return project.getSequenceByName(sequence);
		}
		String connectorName = parameters.apply(Parameter.Connector.getName());
		var connector = connectorName == null || connectorName.isEmpty()
				? project.getDefaultConnector() : project.getConnectorByName(connectorName);
		if (connector == null) {
			throw new EngineException("No default connector in project \"" + project.getName() + "\".");
		}
		String transactionName = parameters.apply(Parameter.Transaction.getName());
		var transaction = transactionName == null || transactionName.isEmpty()
				? connector.getDefaultTransaction() : connector.getTransactionByName(transactionName);
		if (transaction == null) {
			throw new EngineException("Unknown transaction \"" + transactionName + "\" in connector \""
					+ connector.getName() + "\" of project \"" + project.getName() + "\".");
		}
		return transaction;
	}
}
