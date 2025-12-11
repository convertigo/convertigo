/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the  Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.sessions;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.twinsoft.convertigo.engine.Engine;

@SuppressWarnings("deprecation")
final class RedisHttpSession implements HttpSession, Serializable {
	private static final long serialVersionUID = 1L;

	private final SessionStore store;
	private final ServletContext servletContext;
	@SuppressWarnings("unused")
	private final RedisSessionConfiguration configuration;
	private final AtomicBoolean invalidated = new AtomicBoolean(false);
	private final Object mutex = new Object();
	private SessionData sessionData;

	RedisHttpSession(SessionStore store, SessionData sessionData, ServletContext servletContext,
			RedisSessionConfiguration configuration) {
		this.store = store;
		this.sessionData = sessionData;
		this.servletContext = servletContext;
		this.configuration = configuration;
	}

	void markAccessed() {
		synchronized (mutex) {
			ensureValid();
			sessionData.touch();
			store.save(sessionData);
		}
	}

	@Override
	public long getCreationTime() {
		synchronized (mutex) {
			ensureValid();
			return sessionData.getCreationTime();
		}
	}

	@Override
	public String getId() {
		synchronized (mutex) {
			ensureValid();
			return sessionData.getId();
		}
	}

	@Override
	public long getLastAccessedTime() {
		synchronized (mutex) {
			ensureValid();
			return sessionData.getLastAccessedTime();
		}
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		synchronized (mutex) {
			ensureValid();
			sessionData.setMaxInactiveInterval(interval);
			store.save(sessionData);
		}
	}

	@Override
	public int getMaxInactiveInterval() {
		synchronized (mutex) {
			ensureValid();
			return sessionData.getMaxInactiveInterval();
		}
	}

	@Override
	public HttpSessionContext getSessionContext() {
		return null;
	}

	@Override
	public Object getAttribute(String name) {
		synchronized (mutex) {
			ensureValid();
			return sessionData.getAttribute(name);
		}
	}

	@Override
	public Object getValue(String name) {
		return getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		synchronized (mutex) {
			ensureValid();
			return sessionData.getAttributeNames();
		}
	}

	@Override
	public String[] getValueNames() {
		synchronized (mutex) {
			ensureValid();
			var enumeration = sessionData.getAttributeNames();
			return Collections.list(enumeration).toArray(String[]::new);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		synchronized (mutex) {
			ensureValid();
			sessionData.setAttribute(name, value);
			sessionData.touch();
			store.save(sessionData);
		}
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		synchronized (mutex) {
			ensureValid();
			sessionData.removeAttribute(name);
			sessionData.touch();
			store.save(sessionData);
		}
	}

	@Override
	public void removeValue(String name) {
		removeAttribute(name);
	}

	@Override
	public void invalidate() {
		if (invalidated.compareAndSet(false, true)) {
			try {
				store.delete(sessionData.getId());
				sessionData = null;
			} catch (Exception e) {
				log("(RedisHttpSession) Failed to invalidate session", e);
			}
		}
	}

	@Override
	public boolean isNew() {
		synchronized (mutex) {
			ensureValid();
			return sessionData.isNew();
		}
	}

	private void ensureValid() {
		if (invalidated.get()) {
			throw new IllegalStateException("Session has been invalidated");
		}
	}

	boolean isInvalidatedInternal() {
		return invalidated.get();
	}

	private void log(String message, Exception e) {
		try {
			if (Engine.logEngine != null) {
				Engine.logEngine.warn(message, e);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}
}
