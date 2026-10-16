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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.catalina.startup.Tomcat;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.api.Session;
import com.twinsoft.api.SessionManager;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.SessionKey;
import com.twinsoft.convertigo.engine.BillingManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ContextManager;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EventManager;
import com.twinsoft.convertigo.engine.PluginsManager;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.requesters.XmlServletRequester;
import com.twinsoft.convertigo.engine.sessions.RequestScopedHttpSession;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.tas.KeyManager;

/** Real Tomcat, servlet/requester plumbing and session accounting; requestable execution is a fixture. */
public class GenericServletSessionModeTest {
	private Engine previousEngine;
	private AuthenticatedSessionManager previousAuthentication;
	private int[] previousLimits;
	private Tomcat tomcat;
	private org.apache.catalina.Context webapp;
	private Path directory;
	private String base;
	private final HttpClient client = HttpClient.newHttpClient();
	private volatile CountDownLatch parallel;

	@Before
	public void start() throws Exception {
		var logger = Logger.getLogger("requester-session-test");
		Engine.logEngine = Engine.logContext = Engine.logContextManager = Engine.logUsageMonitor
				= Engine.logStatistics = Engine.logBeans = Engine.logAdmin = logger;
		EnginePropertiesManager.initProperties();
		previousEngine = Engine.theApp;
		previousAuthentication = Engine.authenticatedSessionManager;
		Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
		Engine.theApp = new Engine();
		Engine.theApp.eventManager = new EventManager();
		Engine.theApp.contextManager = new ContextManager();
		Engine.theApp.contextManager.init();
		Engine.theApp.billingManager = new BillingManager();
		Engine.theApp.sessionManager = new SessionManager();
		Engine.theApp.pluginsManager = new PluginsManager();
		var project = new Project();
		project.setName("SessionTest");
		for (String name : new String[] { "Echo", "Child" }) {
			var sequence = new GenericSequence();
			sequence.setName(name);
			project.add(sequence);
		}
		var hidden = new GenericSequence();
		hidden.setName("Hidden");
		hidden.setAccessibility(Accessibility.Private);
		project.add(hidden);
		project.setJsonOutput(JsonOutput.useType);
		project.setJsonRoot(JsonRoot.docNode);
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override
			public Project getOriginalProjectByName(String name, boolean checkOpenable) {
				return project.getName().equals(name) ? project : null;
			}
		};
		// Isolated fixture: a burst must work with only two concurrent session slots.
		var field = KeyManager.class.getDeclaredField("cvsMax");
		field.setAccessible(true);
		previousLimits = (int[]) field.get(null);
		int[] limits = previousLimits == null ? new int[(int) Session.EmulIDSE + 1] : previousLimits.clone();
		limits[(int) Session.EmulIDSE] = 2;
		field.set(null, limits);
		directory = Files.createTempDirectory("requester-session-test");
		tomcat = new Tomcat();
		tomcat.setBaseDir(directory.toString());
		tomcat.setPort(0);
		tomcat.getConnector().setProperty("address", "127.0.0.1");
		webapp = tomcat.addContext("", directory.toString());
		Tomcat.addServlet(webapp, "requester", new FixtureServlet());
		webapp.addServletMappingDecoded("/projects/*", "requester");
		Tomcat.addServlet(webapp, "reject-xml", new XmlServlet());
		webapp.addServletMappingDecoded("/projects/SessionTest/.reject.xml", "reject-xml");
		Tomcat.addServlet(webapp, "reject-json", new JsonServlet());
		webapp.addServletMappingDecoded("/projects/SessionTest/.reject.json", "reject-json");
		tomcat.start();
		base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/projects/SessionTest/";
	}

	@After
	public void stop() throws Exception {
		try {
			if (tomcat != null) {
				for (var session : webapp.getManager().findSessions()) {
					session.expire();
				}
				tomcat.stop();
				tomcat.destroy();
			}
		} finally {
			Engine.theApp = previousEngine;
			Engine.authenticatedSessionManager = previousAuthentication;
			var field = KeyManager.class.getDeclaredField("cvsMax");
			field.setAccessible(true);
			field.set(null, previousLimits);
			if (directory != null) {
				FileUtils.deleteDirectory(directory.toFile());
			}
		}
	}

	@Test
	public void unavailableTargetsHaveTheSameHttpErrorsWithoutCreatingSessions() throws Exception {
		for (String extension : new String[] { "xml", "json" }) {
			var hidden = send("reject." + extension + "?__sequence=Hidden", null);
			assertTrue(hidden.body(), hidden.body().contains("Unable to execute the requested requestable."));
			assertNoSession(hidden);
			for (String query : new String[] { "__sequence=Missing", "__project=Missing&__sequence=Hidden" }) {
				var missing = send("reject." + extension + "?" + query, null);
				assertEquals(hidden.statusCode(), missing.statusCode());
				assertEquals(hidden.headers().firstValue("Content-Type"), missing.headers().firstValue("Content-Type"));
				String expected = hidden.body();
				String actual = missing.body().replace("Missing", query.startsWith("__project") ? "SessionTest" : "Hidden");
				assertEquals(expected, actual);
				assertNoSession(missing);
			}
		}
	}

	@Test
	public void rejectedHttpRequestsPreserveInheritedAuthenticationAndBusinessState() throws Exception {
		var first = send("xml?__sequence=Echo", null);
		String cookie = first.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
		var session = webapp.getManager().findSessions()[0].getSession();
		session.setMaxInactiveInterval(1800);
		session.setAttribute("businessState", "keep");
		var contexts = Engine.theApp.contextManager.getContexts(session);
		assertEquals(1, contexts.size());
		var context = contexts.get(0);
		context.set("businessState", "keep");
		for (String extension : new String[] { "xml", "json" }) {
			var hidden = send("reject." + extension + "?__sequence=Hidden", cookie);
			var missing = send("reject." + extension + "?__sequence=Missing", cookie);
			assertEquals(hidden.body(), missing.body().replace("Missing", "Hidden"));
			assertFalse(hidden.body(), hidden.body().contains("businessState"));
			assertFalse(hidden.headers().firstValue("Set-Cookie").isPresent());
			assertFalse(missing.headers().firstValue("Set-Cookie").isPresent());
			assertEquals(1, webapp.getManager().getActiveSessions());
			assertEquals(1800, session.getMaxInactiveInterval());
			assertEquals("request-user", SessionAttribute.authenticatedUser.get(session));
			assertEquals("keep", session.getAttribute("businessState"));
			assertSame(context, Engine.theApp.contextManager.getContexts(session).get(0));
			assertEquals("keep", context.get("businessState"));
		}
		session.setAttribute(SessionKey.ADMIN_ROLES.toString(), new Role[] { Role.TEST_PLATFORM_PRIVATE });
		var admin = send("reject.xml?__sequence=Missing", cookie);
		assertTrue(admin.body(), admin.body().contains("Missing"));
		assertFalse(admin.body(), admin.body().contains("Unable to execute the requested requestable."));
		assertEquals(1800, session.getMaxInactiveInterval());
	}

	@Test
	public void statelessBurstsLeaveNoCookieSessionOrContext() throws Exception {
		for (String extension : new String[] {"json", "xml", "pxml", "cxml"}) {
			for (String value : new String[] {"", "true", "1", "30", "arbitrary", "False"}) {
				assertStateless(send(extension + "?__sequence=Echo&__removeSession=" + value
						+ "&__removeContext=false", null));
			}
		}
	}

	@Test
	public void expiredOrUnknownCookieIsStateless() throws Exception {
		var response = send("xml?__sequence=Echo", null);
		String cookie = response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
		for (var session : webapp.getManager().findSessions()) {
			session.expire();
		}
		assertStateless(send("xml?__sequence=Echo&__removeSession", cookie));
		assertStateless(send("xml?__sequence=Echo&__removeSession", "JSESSIONID=unknown"));
	}

	@Test
	public void absentOrFalseParameterKeepsContainerSessions() throws Exception {
		for (String query : new String[] {"", "&__removeSession=false"}) {
			var response = send("xml?__sequence=Echo" + query, null);
			assertEquals(200, response.statusCode());
			assertTrue(response.headers().firstValue("Set-Cookie").isPresent());
			assertFalse(response.body().contains("request-"));
			assertEquals(1, webapp.getManager().getActiveSessions());
			for (var session : webapp.getManager().findSessions()) {
				session.expire();
			}
		}
	}

	@Test
	public void existingSessionKeepsTheRequestedTimeout() throws Exception {
		var first = send("xml?__sequence=Echo", null);
		String cookie = first.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
		for (String value : new String[] {"", "30"}) {
			var response = send("xml?__sequence=Echo&__removeContext=true&__removeSession=" + value, cookie);
			int expectedInterval = value.isEmpty() ? 1 : 30;
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (webapp.getManager().findSessions()[0].getMaxInactiveInterval() != expectedInterval && System.nanoTime() < deadline) {
				Thread.sleep(10);
			}
			assertEquals(first.body(), response.body());
			assertEquals(1, webapp.getManager().getActiveSessions());
			assertEquals(expectedInterval, webapp.getManager().findSessions()[0].getMaxInactiveInterval());
		}
	}

	@Test
	public void formPutAndMultipartParametersSelectStatelessExecution() throws Exception {
		for (String method : new String[] {"POST", "PUT"}) {
			assertStateless(client.send(HttpRequest.newBuilder(URI.create(base + ".xml"))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.method(method, HttpRequest.BodyPublishers.ofString("__sequence=Echo&__removeSession=true"))
					.build(), HttpResponse.BodyHandlers.ofString()));
		}
		String body = "--boundary\r\nContent-Disposition: form-data; name=\"__sequence\"\r\n\r\nEcho\r\n"
				+ "--boundary\r\nContent-Disposition: form-data; name=\"__removeSession\"\r\n\r\ntrue\r\n--boundary--\r\n";
		assertStateless(client.send(HttpRequest.newBuilder(URI.create(base + ".xml"))
				.header("Content-Type", "multipart/form-data; boundary=boundary")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()));
	}

	@Test
	public void simultaneousStatelessRequestsHaveIndependentSessions() throws Exception {
		parallel = new CountDownLatch(2);
		var request = HttpRequest.newBuilder(URI.create(base + ".xml?__sequence=Echo&__removeSession&parallel=true"))
				.timeout(Duration.ofSeconds(10)).build();
		var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
		var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
		var a = first.get(15, TimeUnit.SECONDS);
		var b = second.get(15, TimeUnit.SECONDS);
		assertNotEquals(a.body(), b.body());
		assertStateless(a);
		assertStateless(b);
	}

	@Test
	public void errorsAndEndHookFailuresStillCloseTheRequestSession() throws Exception {
		var error = send("xml?__sequence=Echo&__removeSession&fail=true", null);
		assertTrue(error.body(), error.body().contains("fixture failure"));
		assertNoSession(error);
		Engine.theApp.pluginsManager = new PluginsManager() {
			@Override
			public synchronized void fireHttpServletRequestEnd(HttpServletRequest request, Long start, Long end) {
				throw new IllegalStateException("end hook failure");
			}
		};
		assertNoSession(send("xml?__sequence=Echo&__removeSession", null));
	}

	private HttpResponse<String> send(String path, String cookie) throws Exception {
		var builder = HttpRequest.newBuilder(URI.create(base + "." + path)).timeout(Duration.ofSeconds(10));
		if (cookie != null) {
			builder.header("Cookie", cookie);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private void assertStateless(HttpResponse<String> response) throws Exception {
		assertEquals(response.body(), 200, response.statusCode());
		assertTrue(response.body(), response.body().contains("request-"));
		assertNoSession(response);
	}

	private void assertNoSession(HttpResponse<String> response) throws Exception {
		assertFalse(response.headers().map().toString(), response.headers().firstValue("Set-Cookie").isPresent());
		// The servlet writes the body before its final cleanup; wait for that cleanup only.
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while ((HttpSessionListener.countSessions() != 0 || Engine.theApp.contextManager.getNumberOfContexts() != 0)
				&& System.nanoTime() < deadline) {
			Thread.sleep(10);
		}
		assertEquals(0, webapp.getManager().getActiveSessions());
		assertEquals(0, HttpSessionListener.countSessions());
		assertEquals(0, Engine.theApp.contextManager.getNumberOfContexts());
	}

	private class FixtureServlet extends GenericServlet {
		private static final long serialVersionUID = 1L;

		@Override public String getDocumentExtension() { return ".xml"; }

		@Override
		public Requester getRequester() {
			return new XmlServletRequester() {
				@Override
				public void initContext(Context context) throws Exception {
					var request = (HttpServletRequest) inputData;
					context.setRequest(request);
					SessionAttribute.contexts.set(context.httpSession, new ArrayList<>(java.util.List.of(context)));
					context.project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName("SessionTest");
					HttpSessionListener.checkSession(request);
				}

				@Override
				protected Object coreProcessRequest() throws Exception {
					var request = (HttpServletRequest) inputData;
					var session = request.getSession();
					if (RequestScopedHttpSession.isRequestScopedId(session.getId())) {
						assertTrue("ephemeral sessions must remain recognizable to the session-store code",
								session instanceof RequestScopedHttpSession);
					}
					assertSame(session, request.getSession(false));
					assertNotNull(HttpSessionListener.getHttpSession(session.getId()));
					assertTrue(HttpSessionListener.countSessions() > 0);
					SessionAttribute.authenticatedUser.set(session, "request-user");
					if (request.getParameter("parallel") != null) {
						parallel.countDown();
						assertTrue("stateless calls were serialized", parallel.await(5, TimeUnit.SECONDS));
					}
					// Internal requesters inherit this same session, attributes and id for their contexts.
					var parameters = new HashMap<String, Object>(Map.of("__project", "SessionTest", "__sequence", "Child", "__context", "child"));
					var internal = new InternalRequester(parameters, request);
					assertSame(session, internal.getHttpServletRequest().getSession());
					var child = internal.getContext();
					assertTrue(child.contextID.startsWith(session.getId()));
					assertEquals("request-user", SessionAttribute.authenticatedUser.string(internal.getHttpServletRequest().getSession()));
					if (!(session instanceof RequestScopedHttpSession)) {
						Engine.theApp.contextManager.remove(child);
					}
					if (request.getParameter("fail") != null) {
						throw new IllegalStateException("fixture failure");
					}
					var document = XMLUtils.createDom();
					document.appendChild(document.createElement("session")).setTextContent(session.getId());
					return document;
				}
			};
		}
	}
}
