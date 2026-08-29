/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.sessions;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

/**
 * An in-memory {@link HttpSession} whose lifecycle is bounded by one request.
 *
 * <p>The session is deliberately independent from the servlet container and
 * from the configured persistent session store. It is useful for internal or
 * stateless executions that need the regular {@code HttpSession} contract
 * without creating a cookie or durable session record.</p>
 */
public final class RequestScopedHttpSession implements HttpSession, AutoCloseable {
	private final ServletContext servletContext;
	private final String id;
	private final long creationTime = System.currentTimeMillis();
	private final Map<String, Object> attributes = new LinkedHashMap<>();

	private long lastAccessedTime = creationTime;
	private int maxInactiveInterval = -1;
	private boolean invalidated;

	public RequestScopedHttpSession(ServletContext servletContext) {
		this(servletContext, "request-" + UUID.randomUUID());
	}

	public RequestScopedHttpSession(ServletContext servletContext, String id) {
		this.servletContext = servletContext;
		this.id = id == null || id.isBlank() ? "request-" + UUID.randomUUID() : id;
	}

	public synchronized boolean isValid() {
		return !invalidated;
	}

	@Override
	public synchronized long getCreationTime() {
		ensureValid();
		return creationTime;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public synchronized long getLastAccessedTime() {
		ensureValid();
		return lastAccessedTime;
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public synchronized void setMaxInactiveInterval(int interval) {
		ensureValid();
		maxInactiveInterval = interval;
	}

	@Override
	public synchronized int getMaxInactiveInterval() {
		ensureValid();
		return maxInactiveInterval;
	}

	@Override
	public synchronized Object getAttribute(String name) {
		ensureValid();
		touch();
		return attributes.get(name);
	}

	@Override
	public synchronized Enumeration<String> getAttributeNames() {
		ensureValid();
		touch();
		return Collections.enumeration(java.util.List.copyOf(attributes.keySet()));
	}

	@Override
	public synchronized void setAttribute(String name, Object value) {
		ensureValid();
		if (name == null) {
			throw new IllegalArgumentException("Session attribute name must not be null");
		}
		if (value == null) {
			removeAttribute(name);
			return;
		}

		var previous = attributes.put(name, value);
		if (previous != value) {
			notifyUnbound(name, previous);
			notifyBound(name, value);
		}
		touch();
	}

	@Override
	public synchronized void removeAttribute(String name) {
		ensureValid();
		if (name == null) {
			return;
		}
		var previous = attributes.remove(name);
		notifyUnbound(name, previous);
		touch();
	}

	@Override
	public synchronized void invalidate() {
		if (invalidated) {
			throw new IllegalStateException("Session " + id + " is already invalidated");
		}
		invalidated = true;
		var snapshot = new LinkedHashMap<>(attributes);
		attributes.clear();
		for (var entry : snapshot.entrySet()) {
			notifyUnbound(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public synchronized boolean isNew() {
		ensureValid();
		return true;
	}

	@Override
	public synchronized void close() {
		if (!invalidated) {
			invalidate();
		}
	}

	private void touch() {
		lastAccessedTime = System.currentTimeMillis();
	}

	private void ensureValid() {
		if (invalidated) {
			throw new IllegalStateException("Session " + id + " is invalidated");
		}
	}

	private void notifyBound(String name, Object value) {
		if (value instanceof HttpSessionBindingListener listener) {
			listener.valueBound(new HttpSessionBindingEvent(this, name, value));
		}
	}

	private void notifyUnbound(String name, Object value) {
		if (value instanceof HttpSessionBindingListener listener) {
			listener.valueUnbound(new HttpSessionBindingEvent(this, name, value));
		}
	}
}
