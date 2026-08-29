/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.sessions.RequestScopedHttpSession;

/**
 * Preserves an HTTP request while replacing its container session with an
 * in-memory request-scoped session. Calling {@code getSession()} never reaches
 * Tomcat or the configured persistent session provider and therefore cannot
 * emit a session cookie.
 */
public final class RequestScopedHttpServletRequest extends HttpServletRequestWrapper implements AutoCloseable {
	private RequestScopedHttpSession session;

	public RequestScopedHttpServletRequest(HttpServletRequest request) {
		super(request);
	}

	@Override
	public synchronized HttpSession getSession() {
		return getSession(true);
	}

	@Override
	public synchronized HttpSession getSession(boolean create) {
		if (session != null && !session.isValid()) {
			session = null;
		}
		if (session == null && create) {
			session = new RequestScopedHttpSession(getServletContext());
		}
		return session;
	}

	public synchronized RequestScopedHttpSession getRequestScopedSession() {
		return session;
	}

	@Override
	public String getRequestedSessionId() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public synchronized String changeSessionId() {
		var current = getSession(false);
		if (current == null) {
			throw new IllegalStateException("No request-scoped session exists");
		}
		return current.getId();
	}

	@Override
	public synchronized void close() {
		if (session != null) {
			session.close();
			session = null;
		}
	}
}
