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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.twinsoft.convertigo.engine.util.RequestScopedHttpServletRequest;
import com.twinsoft.convertigo.engine.util.HttpServletRequestTwsWrapper;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

public class RequestScopedHttpSessionTest {
	@Test
	public void keepsAttributesForTheRequestAndNotifiesBindingListeners() {
		var listener = new RecordingBindingListener();
		var session = new RequestScopedHttpSession(servletContext(), "request-test");

		session.setAttribute("listener", listener);
		assertSame(listener, session.getAttribute("listener"));
		assertEquals("listener", listener.boundName);

		session.removeAttribute("listener");
		assertEquals("listener", listener.unboundName);
		assertNull(session.getAttribute("listener"));
	}

	@Test
	public void invalidationClearsStateExactlyOnce() {
		var listener = new RecordingBindingListener();
		var session = new RequestScopedHttpSession(servletContext());
		session.setAttribute("listener", listener);

		session.invalidate();

		assertFalse(session.isValid());
		assertEquals("listener", listener.unboundName);
		assertThrows(IllegalStateException.class, () -> session.getAttribute("listener"));
		assertThrows(IllegalStateException.class, session::invalidate);
	}

	@Test
	public void requestWrapperNeverDelegatesSessionCreationToTheContainer() {
		var containerSessionCalls = new AtomicInteger();
		var containerSession = new RequestScopedHttpSession(servletContext(), "container-session");
		var request = requestProxy(containerSessionCalls, containerSession);

		try (var scopedRequest = new RequestScopedHttpServletRequest(request)) {
			assertNull(scopedRequest.getSession(false));
			var session = scopedRequest.getSession(true);

			assertNotNull(session);
			assertTrue(session instanceof RequestScopedHttpSession);
			assertEquals(0, containerSessionCalls.get());
			assertNull(scopedRequest.getRequestedSessionId());
			assertFalse(scopedRequest.isRequestedSessionIdValid());
			assertFalse(scopedRequest.isRequestedSessionIdFromCookie());
		}

		assertEquals(0, containerSessionCalls.get());
	}

	@Test
	public void requestScopedWrapperCanOverlayTheTwsParameterRequest() {
		var containerSessionCalls = new AtomicInteger();
		var request = requestProxy(containerSessionCalls,
				new RequestScopedHttpSession(servletContext(), "container-session"));
		var twsRequest = new HttpServletRequestTwsWrapper(request);

		try (var scopedRequest = new RequestScopedHttpServletRequest(twsRequest)) {
			var session = scopedRequest.getSession();

			assertTrue(session instanceof RequestScopedHttpSession);
			assertSame(session, scopedRequest.getSession(false));
			assertEquals(0, containerSessionCalls.get());
		}
	}

	private static ServletContext servletContext() {
		return (ServletContext) Proxy.newProxyInstance(RequestScopedHttpSessionTest.class.getClassLoader(),
				new Class<?>[] { ServletContext.class }, (proxy, method, args) -> defaultValue(method.getReturnType()));
	}

	private static HttpServletRequest requestProxy(AtomicInteger getSessionCalls, HttpSession containerSession) {
		var servletContext = servletContext();
		return (HttpServletRequest) Proxy.newProxyInstance(RequestScopedHttpSessionTest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
					if (method.getName().equals("getSession")) {
						getSessionCalls.incrementAndGet();
						return containerSession;
					}
					if (method.getName().equals("getServletContext")) {
						return servletContext;
					}
					if (method.getName().equals("getMethod")) {
						return "GET";
					}
					if (method.getName().equals("getCharacterEncoding")) {
						return "UTF-8";
					}
					if (method.getName().equals("getParameterMap")) {
						return Map.of();
					}
					if (method.getName().equals("getHeaderNames")) {
						return Collections.emptyEnumeration();
					}
					return defaultValue(method.getReturnType());
				});
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == char.class) {
			return '\0';
		}
		return 0;
	}

	private static final class RecordingBindingListener implements HttpSessionBindingListener {
		private String boundName;
		private String unboundName;

		@Override
		public void valueBound(HttpSessionBindingEvent event) {
			boundName = event.getName();
		}

		@Override
		public void valueUnbound(HttpSessionBindingEvent event) {
			unboundName = event.getName();
		}
	}
}
