/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.sessions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.servlets.SessionSerializationFilter;

/**
 * With the Redis session store, what a request changed in its session is in the store before its response can reach
 * the client, and the end of the request writes only what changed after: a request run through
 * {@link SessionSerializationFilter}, on a session store kept in memory, against a response that records what the
 * store holds each time some of it is sent.
 */
public class RedisSessionWrittenBeforeResponseTest {
	private static final SessionValueCodec CODEC = new SessionValueCodec();
	private static final String COOKIE = "JSESSIONID";
	private static RedisSessionConfiguration configuration;

	private MemoryStore store;

	@BeforeClass public static void configure() throws Exception {
		Engine.logRedis = org.apache.log4j.Logger.getLogger("redis-session-before-response-test");
		Engine.logEngine = Engine.logRedis;
		EnginePropertiesManager.initProperties();
		configuration = RedisSessionConfiguration.fromProperties();
	}

	@Before public void redisStore() throws Exception {
		store = new MemoryStore();
		use(SessionStoreMode.redis, new RedisSessionProvider(store, configuration));
	}

	@After public void reset() throws Exception {
		use(null, null);
	}

	private static void use(SessionStoreMode mode, SessionProvider provider) throws Exception {
		var manager = ConvertigoHttpSessionManager.getInstance();
		var storeMode = ConvertigoHttpSessionManager.class.getDeclaredField("storeMode");
		storeMode.setAccessible(true);
		storeMode.set(manager, mode);
		var providerField = ConvertigoHttpSessionManager.class.getDeclaredField("provider");
		providerField.setAccessible(true);
		providerField.set(manager, provider);
	}

	/** One session hash; each write is recorded. */
	private static final class MemoryStore implements SessionStore {
		final Map<String, String> fields = new HashMap<>();
		final List<Map<String, String>> writes = new ArrayList<>();

		@Override public SessionStoreMeta readMeta(String sessionId) {
			if (!fields.containsKey(SessionStoreKeys.META_CREATION)) {
				return null;
			}
			var attributes = new HashMap<String, String>();
			for (var name : RedisSessionStore.PREFETCHED_ATTRIBUTES) {
				attributes.put(name, fields.get(name));
			}
			var now = System.currentTimeMillis();
			return new SessionStoreMeta(now, now, 1800, attributes);
		}
		@Override public String readAttribute(String sessionId, String name) { return fields.get(name); }
		@Override public Set<String> readAttributeNames(String sessionId) { return new HashSet<>(fields.keySet()); }
		@Override public void writeDelta(String sessionId, Map<String, String> hset, Set<String> hdel, long ttlMillis) {
			writes.add(new HashMap<>(hset));
			fields.putAll(hset);
			hdel.forEach(fields::remove);
		}
		@Override public void delete(String sessionId) { fields.clear(); }
		@Override public void shutdown() {}

		void stored(String name, Object value) throws Exception {
			var now = Long.toString(System.currentTimeMillis());
			fields.put(SessionStoreKeys.META_CREATION, now);
			fields.put(SessionStoreKeys.META_LAST_ACCESS, now);
			fields.put(SessionStoreKeys.META_MAX_INACTIVE, "1800");
			if (name != null) {
				fields.put(name, CODEC.serialize(name, value));
			}
		}
	}

	/** The client side of a response: what the store held each time bytes were sent, and the cookies received. */
	private final class Client {
		final List<Map<String, String>> storeWhenSent = new ArrayList<>();
		final List<String> cookies = new ArrayList<>();
		final StringBuilder body = new StringBuilder();
		final ServletOutputStream stream = new ServletOutputStream() {
			@Override public void write(int b) { body.append((char) b); }
			@Override public void flush() { sent(); }
			@Override public void close() { sent(); }
			@Override public boolean isReady() { return true; }
			@Override public void setWriteListener(WriteListener writeListener) {}
		};
		final PrintWriter writer = new PrintWriter(new StringWriter() {
			@Override public void write(String str, int off, int len) { body.append(str, off, off + len); }
			@Override public void write(char[] cbuf, int off, int len) { body.append(cbuf, off, len); }
			@Override public void flush() { sent(); }
			@Override public void close() { sent(); }
		});

		void sent() {
			storeWhenSent.add(new HashMap<>(store.fields));
		}

		HttpServletResponse response() {
			return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletResponse.class },
					(proxy, method, args) -> switch (method.getName()) {
						case "getOutputStream" -> stream;
						case "getWriter" -> writer;
						case "flushBuffer" -> { sent(); yield null; }
						case "sendRedirect", "sendError" -> { sent(); yield null; }
						case "addCookie" -> { cookies.add(((Cookie) args[0]).getValue()); yield null; }
						case "getCharacterEncoding" -> "UTF-8";
						case "hashCode" -> System.identityHashCode(proxy);
						case "equals" -> proxy == args[0];
						case "toString" -> "test response";
						default -> defaultValue(method.getReturnType());
					});
		}
	}

	private static HttpServletRequest request(String sessionId) {
		var attributes = new HashMap<String, Object>();
		Cookie[] cookies = sessionId == null ? null : new Cookie[] { new Cookie(COOKIE, sessionId) };
		return (HttpServletRequest) Proxy.newProxyInstance(RedisSessionWrittenBeforeResponseTest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
					case "getAttribute" -> attributes.get((String) args[0]);
					case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
					case "removeAttribute" -> { attributes.remove((String) args[0]); yield null; }
					case "getAttributeNames" -> Collections.enumeration(new ArrayList<>(attributes.keySet()));
					case "getCookies" -> cookies;
					case "getContextPath" -> "/convertigo";
					case "getRequestURI" -> "/convertigo/projects/test/.json";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					case "toString" -> "test request";
					default -> defaultValue(method.getReturnType());
				});
	}

	private static Object defaultValue(Class<?> type) {
		if (type == boolean.class) return false;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		return null;
	}

	/** What a servlet does with the request and its response, as GenericServlet sets the response for the session cookie. */
	private interface Servlet {
		void service(HttpServletRequest request, HttpServletResponse response) throws Exception;
	}

	private static void run(HttpServletRequest request, Client client, Servlet servlet) throws Exception {
		FilterChain chain = (req, resp) -> {
			req.setAttribute("response", resp);
			try {
				servlet.service((HttpServletRequest) req, (HttpServletResponse) resp);
			} catch (IOException | RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		new SessionSerializationFilter().doFilter(request, client.response(), chain);
	}

	private static void write(ServletOutputStream out, String text) throws IOException {
		out.write(text.getBytes(StandardCharsets.UTF_8));
	}

	@Test public void aNewSessionIsStoredBeforeTheResponseReachesTheClient() throws Exception {
		var client = new Client();
		run(request(null), client, (request, response) -> {
			var session = request.getSession(true);
			SessionAttribute.authenticatedUser.set(session, "someone");
			var out = response.getOutputStream();
			write(out, "{\"user\":\"someone\"}");
			out.flush();
		});
		assertEquals("{\"user\":\"someone\"}", client.body.toString());
		assertEquals(1, client.cookies.size());
		var seen = client.storeWhenSent.get(0);
		assertTrue("the session is stored when the client gets the response", seen.containsKey(SessionStoreKeys.META_CREATION));
		assertEquals(CODEC.serialize(SessionAttribute.authenticatedUser.value(), "someone"),
				seen.get(SessionAttribute.authenticatedUser.value()));
		assertEquals("one write for the request", 1, store.writes.size());
	}

	@Test public void aResponseClosedInTheChainIsSentAfterTheWrite() throws Exception {
		// as the compression filter does: the end of the response is sent before the request is over
		store.stored("n", 1);
		var client = new Client();
		var sessionId = "closed-in-chain";
		run(request(sessionId), client, (request, response) -> {
			var session = request.getSession(false);
			assertNotNull(session);
			session.setAttribute("n", ((Number) session.getAttribute("n")).intValue() + 1);
			var writer = response.getWriter();
			writer.write("2");
			writer.close();
		});
		assertEquals(CODEC.serialize("n", 2), client.storeWhenSent.get(0).get("n"));
		assertEquals(1, store.writes.size());
		assertTrue("the request carried the session cookie", client.cookies.isEmpty());
	}

	@Test public void whatChangesAfterTheResponseIsWrittenAtTheEnd() throws Exception {
		store.stored(null, null);
		var client = new Client();
		run(request("after-response"), client, (request, response) -> {
			var session = request.getSession(false);
			session.setAttribute("a", "1");
			var out = response.getOutputStream();
			write(out, "ok");
			out.flush();
			session.setAttribute("b", "2");
			session.setMaxInactiveInterval(1);
		});
		var seen = client.storeWhenSent.get(0);
		assertEquals(CODEC.serialize("a", "1"), seen.get("a"));
		assertFalse(seen.containsKey("b"));
		assertEquals(CODEC.serialize("b", "2"), store.fields.get("b"));
		assertEquals("1", store.fields.get(SessionStoreKeys.META_MAX_INACTIVE));
		assertEquals(2, store.writes.size());
		assertEquals(Set.of("b", SessionStoreKeys.META_MAX_INACTIVE, SessionStoreKeys.META_LAST_ACCESS), store.writes.get(1).keySet());
	}

	@Test public void nothingChangedAfterTheResponseSendsNothingMore() throws Exception {
		store.stored("token", "eyJ.token");
		var client = new Client();
		run(request("nothing-after"), client, (request, response) -> {
			var session = request.getSession(false);
			SessionAttribute.markCounted(session);
			session.setAttribute("token", "eyJ.other");
			response.flushBuffer();
			var writer = response.getWriter();
			writer.write("ok");
			writer.flush();
			// read only, or never stored: nothing to send
			session.getAttribute("token");
			SessionAttribute.isNew.set(session, true);
			writer.close();
		});
		assertEquals("a single write for the request, before the response", 1, store.writes.size());
		assertTrue(store.writes.get(0).containsKey(SessionAttribute.countedSession.value()));
		assertEquals(CODEC.serialize("token", "eyJ.other"), client.storeWhenSent.get(0).get("token"));
	}

	@SuppressWarnings("unchecked")
	@Test public void anAttributeModifiedInPlaceAfterTheResponseIsWrittenAtTheEnd() throws Exception {
		store.stored(null, null);
		var client = new Client();
		var profile = new LinkedHashMap<String, Object>();
		profile.put("visits", 1);
		run(request("in-place"), client, (request, response) -> {
			var session = request.getSession(false);
			session.setAttribute("profile", profile);
			response.getOutputStream().flush();
			profile.put("visits", 2);
		});
		var early = (Map<String, Object>) CODEC.deserialize("profile", client.storeWhenSent.get(0).get("profile"));
		assertEquals(1, early.get("visits"));
		var last = (Map<String, Object>) CODEC.deserialize("profile", store.fields.get("profile"));
		assertEquals("the end of the request writes the value as it is then, as before", 2, last.get("visits"));
		assertEquals(Set.of("profile"), store.writes.get(1).keySet());
	}

	@Test public void theRequestKeepsItsViewOfTheSessionAfterTheEarlyWrite() throws Exception {
		var client = new Client();
		HttpSession[] session = new HttpSession[1];
		run(request(null), client, (request, response) -> {
			session[0] = request.getSession(true);
			SessionAttribute.isNew.set(session[0], true);
			response.getOutputStream().flush();
			assertTrue("still the first request of the session", session[0].isNew());
			assertTrue("an attribute never stored is kept for the request", SessionAttribute.isNew.has(session[0]));
		});
		assertFalse(store.fields.containsKey(SessionAttribute.isNew.value()));
		assertEquals(1, store.writes.size());
	}

	@Test public void aRequestThatGoesOnLongAfterItsResponseRefreshesTheTtlAtItsEnd() throws Exception {
		store.stored(null, null);
		var client = new Client();
		run(request("long-request"), client, (request, response) -> {
			request.getSession(false);
			response.getOutputStream().flush();
			Thread.sleep(1100);
		});
		assertEquals("the session expires counting from the end of the request, as before", 2, store.writes.size());
	}

	@Test public void aRequestWithoutSessionWritesNothing() throws Exception {
		var client = new Client();
		run(request(null), client, (request, response) -> {
			assertNull(request.getSession(false));
			var out = response.getOutputStream();
			write(out, "static");
			out.close();
		});
		assertTrue(store.writes.isEmpty());
		assertTrue(client.cookies.isEmpty());
	}

	@Test public void aRedirectIsSentAfterTheWrite() throws Exception {
		var client = new Client();
		run(request(null), client, (request, response) -> {
			SessionAttribute.authenticatedUser.set(request.getSession(true), "someone");
			response.sendRedirect("/convertigo/next");
		});
		assertTrue(client.storeWhenSent.get(0).containsKey(SessionAttribute.authenticatedUser.value()));
		assertEquals(1, store.writes.size());
	}

	@Test public void theTomcatStoreKeepsTheResponseAsItIs() throws Exception {
		use(SessionStoreMode.tomcat, new LegacySessionProvider());
		var client = new Client();
		run(request(null), client, (request, response) -> {
			assertSame("no wrapped output stream", client.stream, response.getOutputStream());
		});
		var writerClient = new Client();
		run(request(null), writerClient, (request, response) -> {
			assertSame("no wrapped writer", writerClient.writer, response.getWriter());
		});
		assertTrue(store.writes.isEmpty());
	}
}
