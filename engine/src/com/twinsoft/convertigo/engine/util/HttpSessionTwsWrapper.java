/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
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

package com.twinsoft.convertigo.engine.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.DomSerializationSupport.SerializedDom;

@SuppressWarnings("deprecation")
public class HttpSessionTwsWrapper implements HttpSession {
	private static final Map<String, Map<String, Object>> transientAttributes = new ConcurrentHashMap<>();

	private final HttpSession session;
	private String id = "sessionTerminated";
	private final AtomicBoolean invalidated = new AtomicBoolean(false);
	private volatile Runnable invalidateCallback;

	private HttpSessionTwsWrapper(HttpSession session) {
		this.session = session;
	}

	@Override
	public Object getAttribute(String name) {
		try {
			var stored = session.getAttribute(name);
			if (stored instanceof SerializedDom dom) {
				var restored = DomSerializationSupport.deserialize(dom);
				if (restored != null) {
					setTransientAttribute(name, restored);
				} else {
					removeTransientAttribute(name);
				}
				return restored;
			}
			if (stored != null) {
				return stored;
			}
			var value = getTransientAttribute(name);
			return value != TransientAttributeHolder.NOT_FOUND ? value : null;
		} catch (Exception e) {
			onException(e);
			var value = getTransientAttribute(name);
			return value != TransientAttributeHolder.NOT_FOUND ? value : null;
		}
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		try {
			var names = new LinkedHashSet<String>();
			names.addAll(getTransientAttributeNames());
			for (var enumeration = session.getAttributeNames(); enumeration.hasMoreElements();) {
				names.add(enumeration.nextElement());
			}
			return Collections.enumeration(names);
		} catch (Exception e) {
			onException(e);
			var names = getTransientAttributeNames();
			return names.isEmpty() ? Collections.emptyEnumeration() : Collections.enumeration(names);
		}
	}

	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}

	@Override
	public String getId() {
		try {
			return id = session.getId();
		} catch (Exception e) {
			onException(e);
			return id;
		}
	}

	@Override
	public long getLastAccessedTime() {
		try {
			return session.getLastAccessedTime();
		} catch (Exception e) {
			onException(e);
			return 1;
		}
	}

	@Override
	public int getMaxInactiveInterval() {
		try {
			return session.getMaxInactiveInterval();
		} catch (Exception e) {
			onException(e);
			return 1;
		}
	}

	@Override
	public ServletContext getServletContext() {
		try {
			return session.getServletContext();
		} catch (Exception e) {
			onException(e);
			return null;
		}
	}

	@Override
	public HttpSessionContext getSessionContext() {
		try {
			return session.getSessionContext();
		} catch (Exception e) {
			onException(e);
			return null;
		}
	}

	@Override
	public Object getValue(String name) {
		return getAttribute(name);
	}

	@Override
	public String[] getValueNames() {
		try {
			var names = new LinkedHashSet<String>();
			names.addAll(getTransientAttributeNames());
			for (var entry : session.getValueNames()) {
				names.add(entry);
			}
			return names.toArray(String[]::new);
		} catch (Exception e) {
			onException(e);
			var names = getTransientAttributeNames();
			return names.isEmpty() ? new String[0] : names.toArray(String[]::new);
		}
	}

	@Override
	public void invalidate() {
		var sessionId = getId();
		try {
			session.invalidate();
		} catch (Exception e) {
			onException(e);
		} finally {
			cleanTransientAttributes(sessionId);
			markInvalidated();
		}
	}

	@Override
	public boolean isNew() {
		try {
			return session.isNew();
		} catch (Exception e) {
			onException(e);
			return false;
		}
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		removeTransientAttribute(name);
		try {
			session.removeAttribute(name);
		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public void removeValue(String name) {
		removeAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (value == null) {
			removeAttribute(name);
			return;
		}

		try {
			if (value instanceof SerializedDom dom) {
				var restored = DomSerializationSupport.deserialize(dom);
				if (restored != null) {
					setTransientAttribute(name, restored);
				} else {
					removeTransientAttribute(name);
				}
				session.setAttribute(name, dom);
				return;
			}

			var serializedDom = DomSerializationSupport.serialize(value);
			if (serializedDom != null) {
				setTransientAttribute(name, value);
				session.setAttribute(name, serializedDom);
				return;
			}

			if (canSerialize(name, value)) {
				removeTransientAttribute(name);
				session.setAttribute(name, value);
			} else {
				setTransientAttribute(name, value);
				session.removeAttribute(name);
			}
		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		try {
			session.setMaxInactiveInterval(interval);
		} catch (Exception e) {
			onException(e);
		}
	}

	private void onException(Exception e) {
		try {
			if (Engine.logEngine.isTraceEnabled()) {
				Engine.logEngine.trace("(HttpSessionTwsWrapper) onException [" + id + "]", e);
			} else {
				Engine.logEngine.debug("(HttpSessionTwsWrapper) onException [" + id + "]: " + e.getMessage());
			}
		} catch (Exception ex) {
			// ignore if log fail
		} finally {
			if (e instanceof IllegalStateException) {
				markInvalidated();
			}
		}
	}

	private boolean canSerialize(String attributeName, Object value) {
		if (!(value instanceof Serializable)) {
			logSerializationSkip(attributeName, value, null);
			return false;
		}

		try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
			oos.writeObject(value);
			return true;
		} catch (IOException e) {
			onException(e);
			logSerializationSkip(attributeName, value, e);
			return false;
		}
	}

	private void logSerializationSkip(String attributeName, Object value, Exception cause) {
		try {
			if (Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(HttpSessionTwsWrapper) Attribute skipped from serialization [" + getId() + "]: "
						+ value.getClass().getName(), cause);
			}
		} catch (Exception ignore) {
			// ignore logging issues
		}
	}

	private Object getTransientAttribute(String name) {
		var attributes = getTransientAttributes(false);
		if (attributes != null && attributes.containsKey(name)) {
			return attributes.get(name);
		}
		return TransientAttributeHolder.NOT_FOUND;
	}

	private void setTransientAttribute(String name, Object value) {
		var attributes = getTransientAttributes(true);
		attributes.put(name, value);
	}

	private void removeTransientAttribute(String name) {
		var sessionId = getId();
		var attributes = getTransientAttributes(false);
		if (attributes != null) {
			attributes.remove(name);
			if (attributes.isEmpty() && sessionId != null) {
				transientAttributes.remove(sessionId, attributes);
			}
		}
	}

	private Map<String, Object> getTransientAttributes(boolean create) {
		var sessionId = getId();
		if (sessionId == null) {
			sessionId = "sessionTerminated";
		}
		return create ? transientAttributes.computeIfAbsent(sessionId, key -> new ConcurrentHashMap<>()) : transientAttributes.get(sessionId);
	}

	private Set<String> getTransientAttributeNames() {
		var attributes = getTransientAttributes(false);
		return attributes == null ? Collections.emptySet() : attributes.keySet();
	}

	public static void cleanTransientAttributes(String sessionId) {
		if (sessionId != null) {
			transientAttributes.remove(sessionId);
			transientAttributes.remove("sessionTerminated");
		}
	}

	public static HttpSession wrap(HttpSession session) {
		if (session == null || session instanceof HttpSessionTwsWrapper) {
			return session;
		}
		return new HttpSessionTwsWrapper(session);
	}

	void onInvalidate(Runnable callback) {
		this.invalidateCallback = callback;
		if (invalidated.get() && callback != null) {
			try {
				callback.run();
			} catch (Exception ignore) {
				// nothing else to do
			}
		}
	}

	boolean isInvalidatedInternal() {
		return invalidated.get();
	}

	private void markInvalidated() {
		if (invalidated.compareAndSet(false, true)) {
			var callback = invalidateCallback;
			if (callback != null) {
				try {
					callback.run();
				} catch (Exception ignore) {
					// no-op
				}
			}
		}
	}

	private enum TransientAttributeHolder {
		NOT_FOUND;
	}
}
