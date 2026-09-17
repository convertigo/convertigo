/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.twinsoft.convertigo.beans.rest.GetOperation;

public class RestApiServletSessionModeTest {
	@Test
	public void terminatingOperationsUseRequestScopedSessionsWithoutANewDboProperty() {
		var operation = new GetOperation();

		assertTrue(operation.isTerminateSession());
		assertTrue(RestApiServlet.usesRequestScopedSession(operation));
	}

	@Test
	public void nonTerminatingOperationsKeepTheInheritedHttpSession() {
		var operation = new GetOperation();
		operation.setTerminateSession(false);

		assertFalse(RestApiServlet.usesRequestScopedSession(operation));
	}
}
