/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Proxy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.requesters.GenericRequester;
import com.twinsoft.convertigo.engine.admin.services.logs.Add;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.servlets.MdcFilter;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;

/** Uses the production Log4j bridge, layout, suppression filter and task entry points. */
public class MdcLifecycleTest {
	private static final String KEY = "ContextualParameters";
	private static Logger logger;
	private static RecordingAppender appender;

	@BeforeClass
	public static void initialize() throws Exception {
		// Initialize Engine first, as in production (including MDC inheritance).
		Engine.isEngineMode();
		logger = Logger.getLogger("mdc-regression");
		Engine.logEngine = Engine.logContext = Engine.logStatistics = Engine.logDevices = logger;
		for (String name : List.of("properties", "system_properties")) {
			var field = EnginePropertiesManager.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(null, new Properties());
		}
		Engine.theApp = new Engine();
		Engine.theApp.contextManager = new ContextManager();
		var field = EnginePropertiesManager.class.getDeclaredField("filterLog4J");
		field.setAccessible(true);
		appender = new RecordingAppender((Filter) field.get(null));
		appender.start();
		var coreLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger("mdc-regression");
		coreLogger.addAppender(appender);
		coreLogger.setAdditive(false);
		coreLogger.setLevel(org.apache.logging.log4j.Level.INFO);
	}

	@Before
	public void before() {
		Log4jHelper.mdcClear();
		appender.lines.clear();
	}

	@After
	public void after() {
		Log4jHelper.mdcClear();
	}

	@Test
	public void clearRemovesBothMdcRepresentationsAndSuppression() {
		request("CheckSite");
		MDC.put("nolog", true);
		ThreadContext.put("core-only", "value");
		Log4jHelper.mdcClear();
		assertTrue(MDC.getContext().isEmpty());
		assertTrue(ThreadContext.getContext().isEmpty());
		assertEquals("visible", log("visible"));
	}

	@Test
	public void updatesReachTheRenderedLogAndNullSetRemovesOnlyParameters() {
		request("Old");
		Log4jHelper.mdcPut(mdcKeys.Project, "New");
		Log4jHelper.mdcPut(mdcKeys.ClientIP, "192.0.2.1");
		String line = log("updated");
		assertTrue(line.contains("$project=New"));
		assertTrue(line.contains("$clientip=192.0.2.1"));
		MDC.put("nolog", true);
		Log4jHelper.mdcSet(null);
		assertNull(MDC.get(KEY));
		assertNull(ThreadContext.get(KEY));
		assertEquals(Boolean.TRUE, MDC.get("nolog"));
	}

	@Test
	public void snapshotsAndContextClonesAreIndependentOfLaterRequests() throws Exception {
		Context context = request("Original");
		Context clone = context.clone();
		var snapshot = Log4jHelper.mdcSnapshot();
		Log4jHelper.mdcInit(context);
		Log4jHelper.mdcPut(mdcKeys.Project, "NextRequest");
		assertTrue(clone.logParameters.toString().contains("$project=Original |"));
		snapshot.install();
		assertTrue(log("async-original").contains("$project=Original"));
		Log4jHelper.mdcPut(mdcKeys.Project, "ChangedWorker");
		snapshot.install();
		assertTrue(log("second-worker").contains("$project=Original"));
	}

	@Test
	public void nestedScopesRestoreParametersFlagsAndCoreOnlyValuesAfterFailure() {
		request("Parent");
		MDC.put("nolog", true);
		ThreadContext.put("core-only", "parent");
		assertThrows(IllegalStateException.class, () -> {
			try (var scope = Log4jHelper.mdcScope()) {
				request("Child");
				MDC.remove("nolog");
				ThreadContext.put("core-only", "child");
				throw new IllegalStateException("child failure");
			}
		});
		assertEquals(Boolean.TRUE, MDC.get("nolog"));
		assertEquals("parent", ThreadContext.get("core-only"));
		MDC.remove("nolog");
		assertTrue(log("parent-again").contains("$project=Parent"));
	}

	@Test
	public void sameWorkerUsesSubmittingRequestAndIsEmptyEvenAfterAnException() throws Exception {
		request("CheckSite");
		var pool = Executors.newSingleThreadExecutor();
		try {
			long thread = pool.submit(() -> {
				// Deliberately contaminate a reusable worker before submitting the real task.
				request("CheckSite");
				MDC.put("nolog", true);
				return Thread.currentThread().getId();
			}).get(5, TimeUnit.SECONDS);
			request("SendMail");
			Runnable task = Log4jHelper.withMdc(() -> {
				assertEquals(thread, Thread.currentThread().getId());
				assertTrue(log("SMTP").contains("$project=SendMail"));
				MDC.put("nolog", true);
				throw new IllegalArgumentException("SMTP failure");
			});
			request("NextRequest");
			var failure = assertThrows(java.util.concurrent.ExecutionException.class,
					() -> pool.submit(task).get(5, TimeUnit.SECONDS));
			assertTrue(failure.getCause() instanceof IllegalArgumentException);
			assertEquals("SMTP failure", failure.getCause().getMessage());
			pool.submit(() -> {
				assertTrue(MDC.getContext().isEmpty());
				assertTrue(ThreadContext.getContext().isEmpty());
				assertEquals("background", log("background"));
			}).get(5, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	public void engineExecutorCapturesBeforeTheCallerMovesOn() throws Exception {
		Context context = request("MailProject");
		var release = new CountDownLatch(1);
		var result = new CompletableFuture<String>();
		Engine.execute(() -> {
			try {
				assertTrue(release.await(5, TimeUnit.SECONDS));
				result.complete(log("SMTP"));
			} catch (Throwable t) { result.completeExceptionally(t); }
		});
		Log4jHelper.mdcInit(context);
		Log4jHelper.mdcPut(mdcKeys.Project, "CheckSite");
		release.countDown();
		assertTrue(result.get(5, TimeUnit.SECONDS).contains("$project=MailProject"));
	}

	@Test
	public void delayedExecutorAlsoCapturesBeforeDelay() throws Exception {
		request("DeferredProject");
		var release = new CountDownLatch(1);
		var result = new CompletableFuture<String>();
		Engine.execute(() -> {
			try {
				assertTrue(release.await(5, TimeUnit.SECONDS));
				result.complete(log("deferred"));
			} catch (Throwable t) { result.completeExceptionally(t); }
		}, 20);
		request("NextRequest");
		release.countDown();
		assertTrue(result.get(5, TimeUnit.SECONDS).contains("$project=DeferredProject"));
	}

	@Test
	public void requestableThreadCapturesAtConstructionAndOwnsItsUpdates() throws Exception {
		request("Parent");
		var result = new CompletableFuture<String>();
		var cleaned = new CompletableFuture<Boolean>();
		var sequence = new GenericSequence() {
			@Override public void prepareForRequestable(Context context,
					org.mozilla.javascript.Context jsContext, org.mozilla.javascript.Scriptable scope) {
				result.complete(log("requestable"));
				Log4jHelper.mdcPut(mdcKeys.User, "child-user");
				runningThread.bContinue = false;
			}
		};
		sequence.context = new Context("thread-context");
		sequence.context.project = new com.twinsoft.convertigo.beans.core.Project() {
			@Override public ClassLoader getProjectClassLoader() { return getClass().getClassLoader(); }
		};
		var thread = sequence.new RequestableThread() {
			@Override public void run() {
				try {
					super.run();
					cleaned.complete(MDC.getContext().isEmpty() && ThreadContext.getContext().isEmpty());
				} catch (Throwable t) { cleaned.completeExceptionally(t); }
			}
		};
		sequence.runningThread = thread;
		Log4jHelper.mdcPut(mdcKeys.Project, "ChangedParent");
		thread.start();
		assertTrue(result.get(5, TimeUnit.SECONDS).contains("$project=Parent"));
		assertTrue(cleaned.get(5, TimeUnit.SECONDS));
		assertFalse(log("parent").contains("child-user"));
	}

	@Test
	public void requesterRestoresParentOnSuccessFailureAndCleanupFailure() throws Exception {
		String previous = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.DOCUMENT_INCLUDE_STATISTICS);
		EnginePropertiesManager.setProperty(EnginePropertiesManager.PropertyName.DOCUMENT_INCLUDE_STATISTICS, "true");
		try {
			for (int failure : new int[] {0, 1, 2}) {
				request("Parent");
				MDC.put("nolog", true);
				var requester = new TestRequester(failure);
				if (failure == 0) {
					assertEquals("result", requester.processRequest(Map.of()));
				} else {
					assertThrows(IllegalStateException.class, () -> requester.processRequest(Map.of()));
				}
				assertEquals(Boolean.TRUE, MDC.get("nolog"));
				MDC.remove("nolog");
				assertTrue(log("returned-to-parent").contains("$project=Parent"));
			}
		} finally {
			EnginePropertiesManager.setProperty(EnginePropertiesManager.PropertyName.DOCUMENT_INCLUDE_STATISTICS, previous);
		}
	}

	@Test
	public void httpBoundaryClearsIncomingAndOutgoingStateOnSuccessAndFailure() throws Exception {
		var filter = new MdcFilter();
		for (boolean fail : new boolean[] {false, true}) {
			request("PreviousRequest");
			MDC.put("nolog", true);
			try {
				filter.doFilter(null, null, (request, response) -> {
					assertTrue(MDC.getContext().isEmpty());
					assertTrue(ThreadContext.getContext().isEmpty());
					request("CurrentRequest");
					MDC.put("nolog", true);
					logger.info("hidden");
					if (fail) { throw new ServletException("request failure"); }
				});
				assertFalse(fail);
			} catch (ServletException e) { assertTrue(fail); }
			assertTrue(MDC.getContext().isEmpty());
			assertTrue(ThreadContext.getContext().isEmpty());
			assertFalse(appender.lines.stream().anyMatch(line -> line.contains("hidden")));
			assertEquals("next-request-visible", log("next-request-visible"));
		}
	}

	@Test
	public void httpBoundarySetsNoLogBeforeAliasForwarding() throws Exception {
		for (String source : new String[] {"query", "header", "system", "normal"}) {
			var httpRequest = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
						case "getQueryString" -> source.equals("query") ? "__sequence=Probe&__nolog=true" : null;
						case "getHeader" -> source.equals("header") ? "true" : null;
						case "getRequestURI" -> "/convertigo/" + (source.equals("system") ? "system/" : "")
								+ "projects/Test/.json";
						default -> null;
					});
			new MdcFilter().doFilter(httpRequest, null, (request, response) -> {
				assertEquals(source.equals("normal") ? null : Boolean.TRUE, MDC.get("nolog"));
				logger.info("alias-request-" + source);
			});
			assertTrue(MDC.getContext().isEmpty());
			assertTrue(ThreadContext.getContext().isEmpty());
		}
		assertEquals(List.of("alias-request-normal"), appender.lines);
	}

	@Test
	public void deviceLogsPublishTheCurrentEnvironmentAndDoNotKeepThePreviousUser() throws Exception {
		request("Caller");
		Map<String, Object> attributes = new java.util.HashMap<>();
		HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {HttpSession.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getId" -> "device-session";
					case "getAttribute" -> attributes.get(args[0]);
					case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
					case "removeAttribute" -> { attributes.remove(args[0]); yield null; }
					default -> null;
				});
		SessionAttribute.authenticatedUser.set(session, "alice");
		var service = new Add() {
			void emit(String project) throws Exception {
				var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
							case "getSession" -> session;
							case "getRemoteAddr" -> "192.0.2.1";
							case "getRemoteHost" -> "device.example";
							case "getParameter" -> "logs".equals(args[0])
									? "[{\"level\":\"INFO\",\"msg\":\"device\",\"time\":\"now\"}]"
									: new JSONObject().put("uid", "same-device").put("project", project).toString();
							default -> null;
						});
				getServiceResult(request, new JSONObject());
			}
		};
		service.emit("FirstProject");
		String first = appender.lines.get(appender.lines.size() - 1);
		assertTrue(first.contains("$project=FirstProject"));
		assertTrue(first.contains("$user=alice"));
		assertTrue(first.contains("$clientip=192.0.2.1"));
		var saved = (Map<?, ?>) session.getAttribute(Add.class.getCanonicalName());
		var previousParameters = (LogParameters) saved.get("same-device");
		SessionAttribute.authenticatedUser.remove(session);
		service.emit("SecondProject");
		String second = appender.lines.get(appender.lines.size() - 1);
		assertTrue(second.contains("$project=SecondProject"));
		assertTrue(second.contains("$user=(anonymous)"));
		assertTrue(previousParameters.toString().contains("$project=FirstProject |"));
		assertTrue(log("caller-again").contains("$project=Caller"));
	}

	private static Context request(String project) {
		var context = new Context("test-context");
		Log4jHelper.mdcInit(context);
		Log4jHelper.mdcPut(mdcKeys.Project, project);
		return context;
	}

	private static String log(String message) {
		synchronized (appender.lines) {
			int size = appender.lines.size();
			logger.info(message);
			assertEquals("Expected a rendered log", size + 1, appender.lines.size());
			return appender.lines.get(size);
		}
	}

	private static class RecordingAppender extends AbstractAppender {
		final List<String> lines = java.util.Collections.synchronizedList(new ArrayList<>());
		RecordingAppender(Filter filter) {
			super("mdc-regression", filter, PatternLayout.newBuilder()
					.withPattern("%X{ContextualParameters}%m").build(), false, null);
		}
		@Override public void append(LogEvent event) { lines.add(getLayout().toSerializable(event).toString()); }
	}

	/** Keeps GenericRequester's real lifecycle while replacing project execution. */
	private static class TestRequester extends GenericRequester {
		private final int failure;
		TestRequester(int failure) { this.failure = failure; }
		@Override public String getName() { return "mdc-test"; }
		@Override public Context getContext() {
			var child = new Context("child");
			child.projectName = "Child";
			child.sequenceName = "ChildSequence";
			return child;
		}
		@Override public void initContext(Context context) {}
		@Override protected void initInternalVariables() {}
		@Override public void preGetDocument() {}
		@Override public void setStyleSheet(Document document) {}
		@Override protected Object addStatisticsAsText(String stats, Object result) {
			if (failure == 2) { throw new IllegalStateException("cleanup failure"); }
			return result;
		}
		@Override protected Object addStatisticsAsData(Object result) { return result; }
		@Override protected Object coreProcessRequest() {
			assertTrue(ThreadContext.get(KEY).contains("$project=Child"));
			assertEquals(Boolean.TRUE, MDC.get("nolog"));
			if (failure == 1) { throw new IllegalStateException("execution failure"); }
			return "result";
		}
		@Override public Translator getTranslator() {
			return new Translator() {
				public void buildInputDocument(Context context, Object input) {}
				public Object buildOutputData(Context context, Object output) { return output; }
				public String getContextName(byte[] data) { return "child"; }
				public String getProjectName(byte[] data) { return "Child"; }
			};
		}
	}
}
