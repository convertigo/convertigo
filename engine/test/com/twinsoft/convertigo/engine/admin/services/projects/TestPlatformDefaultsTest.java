/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.admin.services.projects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * The test platform of the dashboard tells the default connector of a project and the default transaction of a
 * connector (#1225): only the explicit ones, as a project or a connector may have none, and a default transaction
 * the caller may not see stays unlisted.
 */
public class TestPlatformDefaultsTest {
	private static final Map<String, Project> projects = new HashMap<String, Project>();
	private Engine previousEngine;
	private AuthenticatedSessionManager previousSessionManager;

	@BeforeClass
	public static void initializeLogging() {
		Engine.logBeans = Logger.getLogger("test-platform-defaults-test");
		Engine.logEngine = Engine.logContext = Engine.logDatabaseObjectManager = Engine.logAdmin = Engine.logBeans;
	}

	@Before
	public void installEngine() throws Exception {
		EnginePropertiesManager.initProperties();
		previousEngine = Engine.theApp;
		previousSessionManager = Engine.authenticatedSessionManager;
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override
			public Project getOriginalProjectByName(String name) throws EngineException {
				var project = projects.get(name);
				if (project == null) {
					throw new EngineException("No project " + name);
				}
				return project;
			}
		};
		Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
	}

	@After
	public void restoreEngine() {
		Engine.theApp = previousEngine;
		Engine.authenticatedSessionManager = previousSessionManager;
		projects.clear();
	}

	@Test
	public void theExplicitDefaultsAreMarked() throws Exception {
		var project = project("Defaults");
		var main = connector(project, "Main", "first", "chosen");
		project.setDefaultConnector(main);
		main.setDefaultTransaction(main.getTransactionByName("chosen"));
		connector(project, "Other", "first");

		var platform = testPlatform("Defaults", false);
		assertEquals("true", connector(platform, "Main").getAttribute("default"));
		assertEquals("true", transaction(platform, "Main", "chosen").getAttribute("default"));
		assertNull(defaultOf(transaction(platform, "Main", "first")));
		assertNull(defaultOf(connector(platform, "Other")));
		assertNull("a connector without default transaction marks none",
				defaultOf(transaction(platform, "Other", "first")));
	}

	@Test
	public void theImplicitChoicesAreNotMarked() throws Exception {
		var project = project("Implicit");
		connector(project, "Only", "alone");

		var platform = testPlatform("Implicit", false);
		assertNull("the only connector is not the default one", defaultOf(connector(platform, "Only")));
		assertNull("the only transaction is not the default one", defaultOf(transaction(platform, "Only", "alone")));
	}

	@Test
	public void aDefaultTransactionTheCallerMayNotSeeStaysUnlisted() throws Exception {
		var project = project("Private");
		var connector = connector(project, "Main", "open", "secret");
		project.setDefaultConnector(connector);
		var secret = (SqlTransaction) connector.getTransactionByName("secret");
		secret.setAccessibility(Accessibility.Private);
		connector.setDefaultTransaction(secret);

		var anonymous = testPlatform("Private", false);
		assertEquals("true", connector(anonymous, "Main").getAttribute("default"));
		assertEquals(1, connector(anonymous, "Main").getElementsByTagName("transaction").getLength());
		assertNull(defaultOf(transaction(anonymous, "Main", "open")));

		var administrator = testPlatform("Private", true);
		assertEquals("true", transaction(administrator, "Main", "secret").getAttribute("default"));
	}

	private static Element testPlatform(String projectName, boolean privateRole) throws Exception {
		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		document.appendChild(document.createElement("admin"));
		new GetTestPlatform().getServiceResult(request(projectName, privateRole), document);
		return (Element) document.getElementsByTagName("project").item(0);
	}

	private static Element connector(Element project, String name) {
		return child(project.getElementsByTagName("connector"), name);
	}

	private static Element transaction(Element project, String connectorName, String name) {
		return child(connector(project, connectorName).getElementsByTagName("transaction"), name);
	}

	private static Element child(NodeList elements, String name) {
		for (int i = 0; i < elements.getLength(); i++) {
			var element = (Element) elements.item(i);
			if (name.equals(element.getAttribute("name"))) {
				return element;
			}
		}
		throw new AssertionError("no element named " + name);
	}

	private static String defaultOf(Element element) {
		return element.hasAttribute("default") ? element.getAttribute("default") : null;
	}

	private static Project project(String name) throws EngineException {
		var project = new Project();
		project.setName(name);
		projects.put(name, project);
		return project;
	}

	private static Connector connector(Project project, String name, String... transactions) throws EngineException {
		var connector = new SqlConnector();
		connector.setName(name);
		project.add(connector);
		for (var transactionName : transactions) {
			var transaction = new SqlTransaction();
			transaction.setName(transactionName);
			// a new transaction is private
			transaction.setAccessibility(Accessibility.Public);
			connector.add(transaction);
		}
		return connector;
	}

	private static HttpServletRequest request(String projectName, boolean privateRole) {
		var roles = privateRole ? new Role[] {Role.WEB_ADMIN, Role.TEST_PLATFORM_PRIVATE} : null;
		var session = (HttpSession) Proxy.newProxyInstance(TestPlatformDefaultsTest.class.getClassLoader(),
				new Class<?>[] {HttpSession.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getId" -> privateRole ? "administrator" : "anonymous";
					case "getAttribute" -> SessionKey.ADMIN_ROLES.toString().equals(args[0]) ? roles : null;
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> null;
				});
		return (HttpServletRequest) Proxy.newProxyInstance(TestPlatformDefaultsTest.class.getClassLoader(),
				new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getSession" -> session;
					case "getParameter" -> "projectName".equals(args[0]) ? projectName : "lang".equals(args[0]) ? "en" : null;
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> null;
				});
	}
}
