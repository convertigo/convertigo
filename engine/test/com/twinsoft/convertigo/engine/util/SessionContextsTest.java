package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import jakarta.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.junit.Test;

public class SessionContextsTest {
	private Engine previousEngine;
	@org.junit.Before public void setUp() { previousEngine = Engine.theApp; Engine.logEngine = Logger.getLogger("test"); Engine.theApp = new Engine(); }
	@org.junit.After public void tearDown() { Engine.theApp = previousEngine; }
	private HttpSession facade(Map<String, Object> attributes) {
		return (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpSession.class},
			(proxy, method, args) -> switch (method.getName()) {
				case "getId" -> "shared-id";
				case "getAttribute" -> attributes.get(args[0]);
				case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
				default -> null;
			});
	}

	@Test public void concurrentFacadesPublishOneListAndReturnStableSnapshots() throws Exception {
		Engine.logContext = Logger.getLogger("test");
		var attributes = new ConcurrentHashMap<String, Object>();
		var first = facade(attributes);
		var second = facade(attributes);
		var executor = Executors.newFixedThreadPool(4);
		try {
			for (int i = 0; i < 200; i++) {
				var context = new Context("shared-id_" + i);
				var session = i % 2 == 0 ? first : second;
				executor.submit(() -> SessionContexts.add(session, context));
			}
		} finally { executor.shutdown(); }
		assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
		var snapshot = SessionContexts.snapshot(first);
		assertEquals(200, snapshot.size());
		SessionContexts.remove(second, "shared-id_1");
		assertEquals(200, snapshot.size());
		assertEquals(199, SessionContexts.snapshot(first).size());
	}

	@Test public void retainedSessionSurvivesClearRequestAndNullEntriesAreRemoved() {
		Engine.logContext = Logger.getLogger("test");
		var session = facade(new ConcurrentHashMap<>());
		var context = new Context("shared-id_retained");
		var list = new ArrayList<Context>();
		list.add(null);
		SessionAttribute.contexts.set(session, list);
		SessionContexts.add(session, context);
		context.httpSession = session;
		context.clearRequest();
		assertNull(context.httpSession);
		assertSame(session, context.contextListSession);
		SessionContexts.remove(context.contextListSession, context.contextID);
		assertTrue(SessionContexts.snapshot(session).isEmpty());
	}
}
