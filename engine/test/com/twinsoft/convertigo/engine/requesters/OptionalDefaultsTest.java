/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.requesters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.RequestableUnavailableException;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * The default connector of a project and the default transaction of a connector are optional (#1225): a
 * request naming a sequence never looks up a connector, a request naming no requestable runs the default
 * or the only connector and transaction, else answers an error that does not tell what the project holds.
 */
public class OptionalDefaultsTest {
	private static final String NO_REQUESTABLE = "The request names no requestable.";
	private static final Map<String, Project> projects = new HashMap<String, Project>();
	private Engine previousEngine;
	private AuthenticatedSessionManager previousSessionManager;

	@BeforeClass
	public static void initializeLogging() {
		Engine.logBeans = Logger.getLogger("optional-defaults-test");
		Engine.logEngine = Engine.logContext = Engine.logContextManager = Engine.logDatabaseObjectManager = Engine.logAdmin = Engine.logBeans;
	}

	@Before
	public void installEngine() throws Exception {
		EnginePropertiesManager.initProperties();
		previousEngine = Engine.theApp;
		previousSessionManager = Engine.authenticatedSessionManager;
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override
			public Project getOriginalProjectByName(String name, boolean checkOpenable) throws EngineException {
				var project = projects.get(name);
				if (project == null) {
					throw new EngineException("No project " + name);
				}
				return project;
			}
		};
	}

	@After
	public void restoreEngine() {
		Engine.theApp = previousEngine;
		Engine.authenticatedSessionManager = previousSessionManager;
		projects.clear();
	}

	@Test
	public void theServerModeToleratesMissingDefaults() throws Exception {
		assertTrue("the lookups threw in the server mode only", Engine.isEngineMode());
		var project = project("Sequences", "First", "Second");
		assertNull(project.getDefaultConnector());
		assertNull(project.getImplicitConnector());

		var connector = connector(project, "Several", false, "one", "two");
		assertNull(connector.getDefaultTransaction());
		assertNull(connector.getImplicitTransaction());
	}

	@Test
	public void servletRequestsOfSequencesInTheSameSessionNeverLookUpAConnector() throws Exception {
		var project = lookupForbidden("Sequences", "First", "Second");
		var context = sessionContext(project);
		var requester = servletRequester(project);
		for (var sequence : new String[] {"First", "Second", "First"}) {
			requester.inputData = request(Map.of("__sequence", sequence));
			requester.initContext(context);
			assertEquals(project.getName(), context.projectName);
			assertEquals(sequence, context.sequenceName);
			assertNull(context.connectorName);
			context.loadSequence();
			assertEquals(sequence, context.requestedObject.getName());
		}
	}

	@Test
	public void internalRequestsOfSequencesInTheSameSessionNeverLookUpAConnector() throws Exception {
		var project = lookupForbidden("Sequences", "First", "Second");
		install(project);
		var context = sessionContext(project);
		for (var sequence : new String[] {"First", "Second"}) {
			var requester = internalRequester(project, parameters("__project", "Sequences", "__sequence", sequence));
			requester.initContext(context);
			assertEquals(project.getName(), context.projectName);
			assertEquals(sequence, context.sequenceName);
			assertNull(context.connectorName);
		}
	}

	@Test
	public void aRequestNamingNoRequestableOfAProjectWithoutConnectorAnswersAClearError() throws Exception {
		var project = project("Sequences", "First", "Second");
		var context = sessionContext(project);
		var requester = servletRequester(project);
		requester.inputData = request(Map.of());
		requester.initContext(context);
		assertNull(context.connectorName);
		// the message does not tell the caller what the project holds
		assertEquals(NO_REQUESTABLE, error(context));
		assertEquals(NO_REQUESTABLE, error(sessionContext(project("Empty"))));
	}

	@Test
	public void aRequestWithoutParametersStillRunsTheDefaultConnectorAndTransaction() throws Exception {
		var project = project("Defaults", "Sequence");
		var main = connector(project, "Main", false, "first", "second");
		var other = connector(project, "Other", false, "third");
		project.setDefaultConnector(main);
		main.setDefaultTransaction(main.getTransactionByName("second"));
		assertSame(main, project.getDefaultConnector());
		assertSame(main, project.getImplicitConnector());
		assertTrue(main.isDefault);
		assertFalse(other.isDefault);

		var context = sessionContext(project);
		var requester = servletRequester(project);
		requester.inputData = request(Map.of());
		requester.initContext(context);
		assertEquals("Main", context.connectorName);
		context.loadTransaction();
		assertSame(main, context.getConnector());
		assertEquals("second", context.transactionName);
		assertSame(main.getTransactionByName("second"), context.requestedObject);

		// a named transaction keeps the name of the request, as before
		requester.inputData = request(Map.of("__transaction", "FIRST"));
		requester.initContext(context);
		context.loadTransaction();
		assertEquals("FIRST", context.transactionName);
		assertSame(main.getTransactionByName("first"), context.transaction);

		requester.inputData = request(Map.of("__connector", "Other"));
		requester.initContext(context);
		context.loadTransaction();
		assertSame(other.getTransactionByName("third"), context.transaction);

		requester.inputData = request(Map.of("__transaction", "unknown"));
		requester.initContext(context);
		assertEquals("Unknown transaction \"unknown\"", error(context));
	}

	@Test
	public void aSingleConnectorAndTransactionNotMarkedDefaultRunWithoutParameters() throws Exception {
		var project = project("Single", "Sequence");
		var connector = connector(project, "Only", false, "alone");
		assertNull(project.getDefaultConnector());
		assertNull(connector.getDefaultTransaction());

		var context = sessionContext(project);
		var requester = servletRequester(project);
		requester.inputData = request(Map.of());
		requester.initContext(context);
		assertEquals("Only", context.connectorName);
		context.loadTransaction();
		assertSame(connector.getTransactionByName("alone"), context.requestedObject);
		assertEquals("alone", context.transactionName);

		// the first call of a session, before the context knows the project
		var first = new Context("first");
		first.project = project;
		first.projectName = project.getName();
		first.loadTransaction();
		assertSame(connector.getTransactionByName("alone"), first.transaction);

		// the internal requester applies the same rule
		install(project);
		var internal = internalRequester(project, parameters("__project", "Single"));
		internal.initContext(context);
		assertEquals("Only", context.connectorName);
	}

	@Test
	public void nothingToChooseAnswersThePlainErrorWithoutTheRequestables() throws Exception {
		var project = project("Several", "Sequence");
		connector(project, "A", false, "a1", "a2");
		connector(project, "B", false, "b1");
		var context = sessionContext(project);
		assertEquals(NO_REQUESTABLE, error(context));

		context.connectorName = "A";
		assertEquals(NO_REQUESTABLE, error(context));
	}

	@Test
	public void theTransactionStepUsesTheSameRuleForAnEmptySource() throws Exception {
		var single = project("Single");
		var connector = connector(single, "Only", false, "alone");
		assertSame(connector.getTransactionByName("alone"), step(single, "").getTargetTransaction());
		assertSame(connector, single.getRequestedConnector(""));
		assertSame(connector.getTransactionByName("alone"), connector.getRequestedTransaction(""));

		var sequences = project("Sequences", "First");
		assertNull(step(sequences, "").getTargetTransaction());
		try {
			sequences.getRequestedConnector("");
			fail("no connector can be chosen");
		} catch (EngineException e) {
			assertEquals(NO_REQUESTABLE, e.getMessage());
		}

		var several = project("Several");
		var many = connector(several, "Many", false, "one", "two");
		assertNull(step(several, "Many").getTargetTransaction());
		assertSame(many.getTransactionByName("two"), step(several, "Many", "two").getTargetTransaction());
	}

	@Test
	public void theDefaultsCanBeRemoved() throws Exception {
		var project = project("Removal");
		var first = connector(project, "First", true, "one", "two");
		var second = connector(project, "Second", false, "three");
		first.setDefaultTransaction(first.getTransactionByName("two"));
		assertSame(first, project.getDefaultConnector());

		project.setDefaultConnector(null);
		assertNull(project.getDefaultConnector());
		assertFalse(first.isDefault);
		project.setDefaultConnector(second);
		assertSame(second, project.getDefaultConnector());
		project.remove(second);
		assertNull(project.getDefaultConnector());
		assertSame("the only connector left", first, project.getImplicitConnector());

		var two = first.getTransactionByName("two");
		assertSame(two, first.getDefaultTransaction());
		first.remove(two);
		assertNull(first.getDefaultTransaction());
		assertSame("the only transaction left", first.getTransactionByName("one"), first.getImplicitTransaction());
		first.setDefaultTransaction(first.getTransactionByName("one"));
		first.setDefaultTransaction(null);
		assertNull(first.getDefaultTransaction());
		assertFalse(first.getTransactionByName("one").isDefault);
	}

	@Test
	public void aTestCaseOfAProjectWithoutConnectorIsNotFound() throws Exception {
		var project = project("Sequences", "First");
		install(project);
		assertNull(TestCase.getTestCase(parameters("__project", "Sequences", "__testcase", "missing"), "Sequences"));
		assertNull(TestCase.getTestCase(request(Map.of("__testcase", "missing")), "Sequences"));
	}

	@Test
	public void aPrivateRequestableAnswersASoberErrorToAnAnonymousCaller() throws Exception {
		Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
		var project = project("Secrets", "Hidden");
		var sequence = project.getSequenceByName("Hidden");
		sequence.setAccessibility(Accessibility.Private);
		var connector = connector(project, "Only", false, "alone");
		var transaction = connector.getTransactionByName("alone");
		transaction.setAccessibility(Accessibility.Private);

		for (var requestable : List.<RequestableObject> of(sequence, transaction)) {
			var context = sessionContext(project);
			context.httpServletRequest = anonymousRequest();
			context.requestedObject = requestable;
			var requester = servletRequester(project);
			requester.inputData = context.httpServletRequest;
			requester.context = context;
			// the precise reason stays on the server, the caller gets the plain answer
			var refusal = refusalOf(requester::checkAccessibility);
			assertTrue(refusal instanceof RequestableUnavailableException);
			assertTrue(refusal.getMessage(), refusal.getMessage().contains("'" + requestable.getName() + "' because it is private"));
			var message = requester.getCallerError(refusal).getMessage();
			assertEquals("Unable to execute the requested requestable.", message);

			requestable.setAccessibility(Accessibility.Public);
			requestable.setSecureConnectionRequired(true);
			message = refusal(requester::checkSecuredConnection);
			assertEquals("Unable to execute the requested requestable: a secured connection is needed.", message);
			assertFalse(message, message.contains(requestable.getName()));
		}
	}

	@Test
	public void theErrorDocumentDoesNotTellTheConnectorAndTransactionTheEngineChose() throws Exception {
		var project = project("Single", "Sequence");
		connector(project, "Only", false, "alone");
		var requester = servletRequester(project);

		// the second request of a session: the requester names the connector, the context the transaction
		var context = sessionContext(project);
		requester.inputData = request(Map.of());
		requester.initContext(context);
		context.loadTransaction();
		var error = errorElement(requester, context);
		assertEquals("Single", error.getAttribute("project"));
		assertFalse(error.hasAttribute("connector"));
		assertFalse(error.hasAttribute("transaction"));

		// the first request of a session, before the context knows the project
		var first = new Context("first");
		first.project = project;
		first.projectName = project.getName();
		first.loadTransaction();
		error = errorElement(requester, first);
		assertFalse(error.hasAttribute("connector"));
		assertFalse(error.hasAttribute("transaction"));

		// the names the caller gives are kept
		requester.inputData = request(Map.of("__connector", "Only", "__transaction", "alone"));
		requester.initContext(context);
		context.loadTransaction();
		error = errorElement(requester, context);
		assertEquals("Only", error.getAttribute("connector"));
		assertEquals("alone", error.getAttribute("transaction"));

		requester.inputData = request(Map.of("__connector", "Only"));
		requester.initContext(context);
		context.loadTransaction();
		error = errorElement(requester, context);
		assertEquals("Only", error.getAttribute("connector"));
		assertFalse(error.hasAttribute("transaction"));

		requester.inputData = request(Map.of("__sequence", "Sequence"));
		requester.initContext(context);
		error = errorElement(requester, context);
		assertEquals("Sequence", error.getAttribute("sequence"));
	}

	@Test
	public void aCallerWhoIsNotAnAdministratorGetsTheSameAnswerForAnUnknownOrARefusedRequestable() throws Exception {
		Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
		var project = project("Secrets", "Hidden");
		project.getSequenceByName("Hidden").setAccessibility(Accessibility.Private);
		connector(project, "Only", false, "alone").getTransactionByName("alone").setAccessibility(Accessibility.Private);

		var refused = answer(project, "Secrets", Map.of("__sequence", "Hidden"), false);
		assertTrue(refused, refused.contains("<message>Unable to execute the requested requestable.</message>"));
		assertFalse(refused, refused.contains("private"));
		assertEquals(refused, answer(project, "Secrets", Map.of("__sequence", "Nope"), false).replace("Nope", "Hidden"));

		// the names the caller gave, as it gave them, even when the engine found them in another case
		refused = answer(project, "Secrets", Map.of("__connector", "ONLY", "__transaction", "ALONE"), false);
		assertTrue(refused, refused.contains("connector=\"ONLY\"") && refused.contains("transaction=\"ALONE\""));
		assertEquals(refused, answer(project, "Secrets", Map.of("__connector", "ONLY", "__transaction", "NONE"), false)
				.replace("NONE", "ALONE"));
		assertEquals(refused, answer(project, "Secrets", Map.of("__connector", "NOPE", "__transaction", "ALONE"), false)
				.replace("NOPE", "ONLY"));

		// a request naming nothing: a private implicit transaction, nothing to choose, an unknown project
		refused = answer(project, "Secrets", Map.of(), false);
		assertFalse(refused, refused.contains("connector=") || refused.contains("transaction="));
		assertEquals(refused, answer(project("Empty"), "Empty", Map.of(), false).replace("Empty", "Secrets"));
		assertEquals(refused, answer(null, "Missing", Map.of(), false).replace("Missing", "Secrets"));
	}

	@Test
	public void theAdministratorsAndTheInternalRequestsKeepThePreciseErrors() throws Exception {
		Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
		var project = project("Secrets", "Hidden");
		project.getSequenceByName("Hidden").setAccessibility(Accessibility.Private);

		var unknown = answer(project, "Secrets", Map.of("__sequence", "Nope"), true);
		assertTrue(unknown, unknown.contains("There is no sequence named \"Nope\" found into the project \"Secrets\"."));
		assertTrue(answer(null, "Missing", Map.of(), true).contains("There is no project named \"Missing\"."));

		// an administrator executes the private requestables
		var context = sessionContext(project);
		var requester = servletRequester(project);
		requester.inputData = callerRequest(Map.of("__sequence", "Hidden"), true);
		requester.initContext(context);
		requester.context = context;
		context.loadSequence();
		requester.checkAccessibility();

		// a sequence step or a Flow calls through the internal requester, on the server
		install(project);
		var failure = new RequestableUnavailableException("There is no sequence named \"Nope\" found into the project \"Secrets\".");
		assertSame(failure, internalRequester(project, parameters("__project", "Secrets", "__sequence", "Nope")).getCallerError(failure));
	}

	/**
	 * @return the error document answered to the request, which the engine refuses, and its JSON as the
	 *         .json requester answers it
	 */
	private static String answer(Project project, String projectName, Map<String, String> parameters, boolean administrator)
			throws Exception {
		var context = new Context("session_default");
		context.project = project;
		context.projectName = projectName;
		var requester = new JsonServletRequester();
		requester.projectName = projectName;
		requester.inputData = callerRequest(parameters, administrator);
		requester.initContext(context);
		requester.context = context;
		var failure = refusalOf(() -> {
			if (context.project == null) {
				// as Engine.getDocument for an unknown project
				throw new RequestableUnavailableException("There is no project named \"" + projectName + "\".");
			}
			if (context.sequenceName != null) {
				context.loadSequence();
			} else {
				context.loadTransaction();
			}
			requester.checkAccessibility();
		});
		var document = ConvertigoError.get(requester.getCallerError(failure)).buildErrorDocument(requester, context);
		// the stack trace is the same in the engine, where the answers come from the same place
		var stackTrace = document.getElementsByTagName("stacktrace").item(0);
		assertFalse(stackTrace.getTextContent().contains("private"));
		stackTrace.setTextContent("");
		var xml = XMLUtils.prettyPrintDOM(document);
		return xml + "\n" + requester.postGetDocument(document);
	}

	private interface Check {
		void run() throws EngineException;
	}

	private static EngineException refusalOf(Check check) {
		try {
			check.run();
		} catch (EngineException e) {
			return e;
		}
		throw new AssertionError("the requestable is executed");
	}

	private static String refusal(Check check) {
		return refusalOf(check).getMessage();
	}

	/**
	 * @return an HTTP request carrying the parameters, of an administrator or of a caller without role
	 */
	private static HttpServletRequest callerRequest(Map<String, String> parameters, boolean administrator) {
		var roles = administrator ? new Role[] {Role.WEB_ADMIN, Role.TEST_PLATFORM_PRIVATE} : null;
		var session = (HttpSession) Proxy.newProxyInstance(OptionalDefaultsTest.class.getClassLoader(),
				new Class<?>[] {HttpSession.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getId" -> administrator ? "administrator" : "anonymous";
					case "getAttribute" -> SessionKey.ADMIN_ROLES.toString().equals(args[0]) ? roles : null;
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> defaultValue(method.getReturnType());
				});
		return (HttpServletRequest) Proxy.newProxyInstance(OptionalDefaultsTest.class.getClassLoader(),
				new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getSession" -> session;
					case "getParameterNames" -> Collections.enumeration(parameters.keySet());
					case "getParameter" -> parameters.get(args[0]);
					case "getParameterValues" -> parameters.containsKey(args[0]) ? new String[] {parameters.get(args[0])} : null;
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> defaultValue(method.getReturnType());
				});
	}

	private static Element errorElement(Requester requester, Context context) throws Exception {
		requester.context = context;
		var document = ConvertigoError.get(new EngineException("failure")).buildErrorDocument(requester, context);
		return (Element) document.getElementsByTagName("error").item(0);
	}

	/**
	 * @return an HTTP request of a caller without role, over a connection that is not secured
	 */
	private static HttpServletRequest anonymousRequest() {
		var session = (HttpSession) Proxy.newProxyInstance(OptionalDefaultsTest.class.getClassLoader(),
				new Class<?>[] {HttpSession.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getId" -> "anonymous";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> defaultValue(method.getReturnType());
				});
		return (HttpServletRequest) Proxy.newProxyInstance(OptionalDefaultsTest.class.getClassLoader(),
				new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getSession" -> session;
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> defaultValue(method.getReturnType());
				});
	}

	/**
	 * @return a servlet requester for the project, as after the lookup of its context
	 */
	private static JsonServletRequester servletRequester(Project project) {
		var requester = new JsonServletRequester();
		requester.projectName = project.getName();
		return requester;
	}

	/**
	 * @return an internal requester for the project, as after the lookup of its context
	 */
	private static InternalRequester internalRequester(Project project, Map<String, Object> parameters) throws EngineException {
		var requester = new InternalRequester(parameters);
		requester.projectName = project.getName();
		return requester;
	}

	private static Project project(String name, String... sequences) throws EngineException {
		var project = new Project();
		project.setName(name);
		for (var sequenceName : sequences) {
			var sequence = new GenericSequence();
			sequence.setName(sequenceName);
			project.add(sequence);
		}
		return project;
	}

	/**
	 * @return a project without connector whose connector lookups fail the test
	 */
	private static Project lookupForbidden(String name, String... sequences) throws EngineException {
		var project = new Project() {
			@Override
			public Connector getDefaultConnector() {
				throw new AssertionError("the default connector is looked up");
			}

			@Override
			public Connector getImplicitConnector() {
				throw new AssertionError("the implicit connector is looked up");
			}
		};
		project.setName(name);
		for (var sequenceName : sequences) {
			var sequence = new GenericSequence();
			sequence.setName(sequenceName);
			project.add(sequence);
		}
		return project;
	}

	private static Connector connector(Project project, String name, boolean byDefault, String... transactions)
			throws EngineException {
		var connector = new SqlConnector();
		connector.setName(name);
		project.add(connector);
		if (byDefault) {
			project.setDefaultConnector(connector);
		}
		for (var transactionName : transactions) {
			var transaction = new SqlTransaction();
			transaction.setName(transactionName);
			connector.add(transaction);
		}
		return connector;
	}

	private static TransactionStep step(Project project, String connectorName) {
		return step(project, connectorName, "");
	}

	private static TransactionStep step(Project project, String connectorName, String transactionName) {
		var step = new TransactionStep() {
			private static final long serialVersionUID = 1L;

			@Override
			protected Project getTargetProject(String projectName) {
				return project;
			}
		};
		step.setSourceTransaction(project.getName() + TransactionStep.SOURCE_SEPARATOR + connectorName
				+ TransactionStep.SOURCE_SEPARATOR + transactionName);
		return step;
	}

	/**
	 * @return a context whose project is already loaded, as for the second request of an HTTP session
	 */
	private static Context sessionContext(Project project) {
		var context = new Context("session_default");
		context.project = project;
		context.projectName = project.getName();
		return context;
	}

	private static String error(Context context) {
		try {
			context.loadTransaction();
		} catch (EngineException e) {
			return e.getMessage();
		}
		throw new AssertionError("the request runs " + context.requestedObject);
	}

	/**
	 * Makes the project available to the lookups of the requesters and the test cases.
	 */
	private static void install(Project project) {
		projects.put(project.getName(), project);
	}

	private static Map<String, Object> parameters(String... namesAndValues) {
		var parameters = new LinkedHashMap<String, Object>();
		for (int i = 0; i < namesAndValues.length; i += 2) {
			parameters.put(namesAndValues[i], new String[] {namesAndValues[i + 1]});
		}
		return parameters;
	}

	/**
	 * @return an HTTP request without session carrying the parameters
	 */
	private static HttpServletRequest request(Map<String, String> parameters) {
		return (HttpServletRequest) Proxy.newProxyInstance(OptionalDefaultsTest.class.getClassLoader(),
				new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getParameterNames" -> Collections.enumeration(parameters.keySet());
					case "getParameter" -> parameters.get(args[0]);
					case "getParameterValues" -> parameters.containsKey(args[0]) ? new String[] {parameters.get(args[0])} : null;
					case "getQueryString" -> null;
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "request " + parameters;
					default -> defaultValue(method.getReturnType());
				});
	}

	private static Object defaultValue(Class<?> type) {
		if (type == boolean.class) {
			return false;
		}
		if (type == int.class) {
			return 0;
		}
		if (type == long.class) {
			return 0L;
		}
		return null;
	}
}
