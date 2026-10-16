/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.JobManager;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.requesters.XmlServletRequester;
import com.twinsoft.convertigo.engine.sessions.RequestScopedHttpSession;

public class InvalidRequestSessionTest {
	private Project project;
	private GenericSequence sequence;
	private final AtomicInteger removed = new AtomicInteger();
	private final XmlServlet servlet = new XmlServlet();

	@Before public void setup() throws Exception {
		Engine.isEngineMode();
		Engine.logEngine = Engine.logContext = Engine.logContextManager = Engine.logBeans
				= Engine.logStatistics = Engine.logAdmin = Logger.getLogger("invalid-request-session-test");
		EnginePropertiesManager.initProperties();
		project = new Project();
		project.setName("SessionTest");
		sequence = new GenericSequence();
		sequence.setName("Check");
		project.add(sequence);
		var connector = new SqlConnector();
		connector.setName("sql");
		connector.isDefault = true;
		project.add(connector);
		var transaction = new SqlTransaction();
		transaction.setName("Select");
		transaction.isDefault = true;
		connector.add(transaction);
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override public Project getOriginalProjectByName(String name) {
				return project.getName().equals(name) ? project : null;
			}
		};
		Engine.theApp.contextManager = new ContextManager() {
			@Override public void remove(Context context) {
				removed.incrementAndGet();
				context.isDestroying = true;
			}
			@Override public void remove(Context context, HttpSession session) {
				remove(context);
				if (session != null) {
					java.util.List<Context> contexts = SessionAttribute.contexts.get(session);
					if (contexts != null) contexts.removeIf(ctx -> context.contextID.equals(ctx.contextID));
				}
			}
		};
		Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
	}

	@Test public void missingTargetsNeverCreateASession() throws Exception {
		for (var parameters : java.util.List.of(
				Map.of("__project", "Missing", "__sequence", "Check"),
				Map.of("__sequence", "Missing"),
				Map.of("__connector", "Missing"),
				Map.of("__connector", "sql", "__transaction", "Missing"),
				Map.of("__transaction", "Missing"))) {
			var request = new Request(parameters, null);
			var requester = new TestRequester(false);
			requester.inputData = request.proxy();
			assertThrows(EngineException.class, requester::getContext);
			assertEquals(0, request.creations);
			assertNull(requester.context);
		}
	}

	@Test public void uniqueTargetsWithoutDefaultsRemainValid() throws Exception {
		var connector = project.getDefaultConnector();
		connector.setDefaultTransaction(null);
		project.setDefaultConnector(null);
		var request = new Request(Map.of(), null);
		new TestRequester(false).validate(request.proxy());
		assertEquals(0, request.creations);
	}

	@Test public void privateTargetsAreRejectedBeforeSessionCreationButAdminsCanResolveThem() throws Exception {
		sequence.setAccessibility(Accessibility.Private);
		var request = new Request(Map.of("__sequence", "Check"), null);
		var requester = new TestRequester(false);
		requester.inputData = request.proxy();
		assertThrows(EngineException.class, requester::getContext);
		assertEquals(0, request.creations);
		var session = session(true);
		session.setAttribute(SessionKey.ADMIN_ROLES.toString(), new Role[] { Role.TEST_PLATFORM_PRIVATE });
		var expected = new Context("test_default");
		Engine.theApp.contextManager = new ContextManager() {
			@Override public Context get(com.twinsoft.convertigo.engine.requesters.Requester requester,
					String name, String prefix, String pool, String project, String connector, String sequence) {
				return expected;
			}
		};
		request = new Request(Map.of("__sequence", "Check"), session);
		requester.inputData = request.proxy();
		assertSame(expected, requester.getContext());
		assertEquals(0, request.creations);
	}

	@Test public void missingTargetLeavesAnAuthenticatedOrAnonymousContextUntouched() throws Exception {
		for (boolean authenticated : new boolean[] { false, true }) {
			var session = session(authenticated);
			var context = new Context(session.getId() + "_default");
			context.project = project;
			context.projectName = project.getName();
			context.set("businessState", "keep");
			SessionAttribute.contexts.set(session, new ArrayList<>(java.util.List.of(context)));
			var request = new Request(Map.of("__project", "Missing", "__sequence", "Check"), session);
			var requester = new TestRequester(false);
			requester.inputData = request.proxy();
			assertThrows(EngineException.class, requester::getContext);
			servlet.onFinally(request.proxy());
			assertEquals(0, removed.get());
			assertSame(project, context.project);
			assertEquals("SessionTest", context.projectName);
			assertEquals("keep", context.get("businessState"));
			assertEquals(1800, session.getMaxInactiveInterval());
			assertEquals(authenticated ? "alice" : null, SessionAttribute.authenticatedUser.get(session));
		}
	}

	@Test public void errorOnAnInheritedSessionDoesNotScheduleCleanupEvenWithoutTheLegacyMarker() {
		for (boolean authenticated : new boolean[] { false, true }) {
			var session = session(authenticated);
			var request = new Request(Map.of(), session);
			var requester = failedRequester(false);
			servlet.processRequestEnd(request.proxy(), requester);
			servlet.onFinally(request.proxy());
			assertFalse(requester.context.isDestroying);
			assertEquals(1800, session.getMaxInactiveInterval());
		}
	}

	@Test public void genuinelyNewFailedSessionStillGetsCleanedUp() {
		var session = session(false);
		var request = new Request(Map.of(), session);
		var requester = failedRequester(true);
		servlet.processRequestEnd(request.proxy(), requester);
		servlet.onFinally(request.proxy());
		assertTrue(requester.context.isDestroying);
		assertEquals(1, session.getMaxInactiveInterval());
	}

	@Test public void newSessionAuthenticatedByTheRequestIsNotDiscarded() {
		var session = session(true);
		var request = new Request(Map.of(), session);
		var requester = failedRequester(true);
		servlet.processRequestEnd(request.proxy(), requester);
		servlet.onFinally(request.proxy());
		assertFalse(requester.context.isDestroying);
		assertEquals(1800, session.getMaxInactiveInterval());
	}

	@Test public void authenticationBetweenErrorProcessingAndCleanupIsPreserved() {
		var session = session(false);
		var request = new Request(Map.of(), session);
		var requester = failedRequester(true);
		servlet.processRequestEnd(request.proxy(), requester);
		SessionAttribute.authenticatedUser.set(session, "alice");
		servlet.onFinally(request.proxy());
		assertFalse(requester.context.isDestroying);
		assertEquals(1800, session.getMaxInactiveInterval());
	}

	@Test public void pollingAnExistingAsyncJobDoesNotResolveTheTargetAgain() throws Exception {
		var jobsField = JobManager.class.getDeclaredField("jobs");
		jobsField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var jobs = (Map<String, Object>) jobsField.get(null);
		var jobClass = Class.forName("com.twinsoft.convertigo.engine.Job");
		var constructor = jobClass.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		var context = new Context("test_default");
		jobs.put(context.contextID, constructor.newInstance(null, null, null, context));
		try {
			var request = new Request(Map.of("__project", "Missing", "__sequence", "Missing"), session(false));
			new TestRequester(false).validate(request.proxy());
			assertEquals(0, request.creations);
		} finally {
			jobs.remove(context.contextID);
		}
	}

	@Test public void explicitSessionAndContextTerminationKeepTheirBehavior() {
		var session = session(false);
		var request = new Request(Map.of("__removeSession", "5", "__removeContext", "true"), session);
		var requester = failedRequester(false);
		servlet.processRequestEnd(request.proxy(), requester);
		servlet.onFinally(request.proxy());
		assertTrue(requester.context.isDestroying);
		assertEquals(5, session.getMaxInactiveInterval());
	}

	@Test public void cleanupNeverCreatesASession() {
		var request = new Request(Map.of("__removeSession", "true"), null);
		servlet.onFinally(request.proxy());
		assertEquals(0, request.creations);
	}

	@Test public void rejectedTargetCleansUpANewSessionAcquiredByTheProtocolEarlierInTheRequest() throws Exception {
		var session = session(false);
		SessionAttribute.contexts.remove(session);
		var request = new Request(Map.of("__project", "Missing", "__sequence", "Check"), session);
		request.attributes.put("convertigo.session.initiallyPresent", false);
		var requester = new TestRequester(false);
		requester.inputData = request.proxy();
		assertThrows(EngineException.class, requester::getContext);
		servlet.onFinally(request.proxy());
		assertEquals(1, session.getMaxInactiveInterval());
		assertEquals(0, removed.get());
	}

	@Test public void removalAlsoUpdatesTheCurrentRequestsSessionFacade() {
		var session = session(false);
		var requester = failedRequester(false);
		// A stored context may have been reconstructed as a different Java object.
		SessionAttribute.contexts.set(session, new ArrayList<>(java.util.List.of(new Context(requester.context.contextID))));
		var request = new Request(Map.of("__removeContext", "true", "__removeSession", "5"), session);
		servlet.processRequestEnd(request.proxy(), requester);
		servlet.onFinally(request.proxy());
		assertTrue(Engine.theApp.contextManager.getContexts(session).isEmpty());
		assertEquals(5, session.getMaxInactiveInterval());
	}

	@Test public void testcaseTargetDefaultsAreValidatedBeforeCreatingASession() throws Exception {
		var testCase = new TestCase();
		testCase.setName("Broken");
		var variable = new TestCaseVariable();
		variable.setName("__sequence");
		variable.setValueOrNull("Missing");
		testCase.add(variable);
		var transaction = (SqlTransaction) project.getDefaultConnector().getDefaultTransaction();
		transaction.add(testCase);
		var request = new Request(Map.of("__testcase", "Broken"), null);
		var requester = new TestRequester(false);
		requester.inputData = request.proxy();
		assertThrows(EngineException.class, requester::getContext);
		assertEquals(0, request.creations);
		variable.setName("__transaction");
		requester.inputData = request.proxy();
		assertThrows(EngineException.class, requester::getContext);
		// An explicit target overrides testcase defaults.
		request = new Request(Map.of("__transaction", "Select", "__testcase", "Broken"), null);
		requester.validate(request.proxy());
	}

	@Test public void internalRequesterRejectsMissingProjectWithoutAnNpeOrSession() {
		var request = new Request(Map.of(), null);
		var error = assertThrows(EngineException.class, () -> new InternalRequester(
				new HashMap<>(Map.of("__project", "Missing")), request.proxy()));
		assertTrue(error.getMessage().contains("Unknown project"));
		assertEquals(0, request.creations);
	}

	private TestRequester failedRequester(boolean created) {
		var requester = new TestRequester(created);
		requester.context = new Context("test_default");
		requester.context.isErrorDocument = true;
		return requester;
	}

	private HttpSession session(boolean authenticated) {
		var session = new RequestScopedHttpSession(null, "test");
		session.setMaxInactiveInterval(1800);
		SessionAttribute.contexts.set(session, new ArrayList<Context>());
		if (authenticated) SessionAttribute.authenticatedUser.set(session, "alice");
		return session;
	}

	private static class TestRequester extends XmlServletRequester {
		TestRequester(boolean created) { newHttpSession = created; }
		void validate(HttpServletRequest request) throws Exception {
			inputData = request;
			initInternalVariables();
			var session = request.getSession(false);
			validateRequestTarget(request, request::getParameter, "default", session == null ? null : session.getId());
		}
	}

	private static class Request {
		final Map<String, String> parameters;
		final Map<String, Object> attributes = new HashMap<>();
		HttpSession session;
		int creations;
		Request(Map<String, String> parameters, HttpSession session) {
			this.parameters = parameters;
			this.session = session;
		}
		HttpServletRequest proxy() {
			return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getSession" -> {
					if (session == null && (args == null || Boolean.TRUE.equals(args[0]))) {
						creations++;
						session = new RequestScopedHttpSession(null, "created");
					}
					yield session;
				}
				case "getParameter" -> parameters.get(args[0]);
				case "getParameterValues" -> parameters.containsKey(args[0]) ? new String[] { parameters.get(args[0]) } : null;
				case "getParameterNames" -> Collections.enumeration(parameters.keySet());
				case "getAttribute" -> attributes.get(args[0]);
				case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
				case "removeAttribute" -> attributes.remove(args[0]);
				case "getRequestURI" -> "/convertigo/projects/SessionTest/.xml";
				default -> null;
			});
		}
	}
}
