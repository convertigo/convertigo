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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.twinsoft.convertigo.engine.Engine;

@SuppressWarnings("deprecation")
final class RedisHttpSession implements HttpSession, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Object NULL = new Object();
	private static final LocalAttributeStore LOCAL_STORE = new LocalAttributeStore();

	private final transient SessionStore store;
	private final transient RedisSessionConfiguration configuration;
	private final transient ServletContext servletContext;
	private final transient SessionValueCodec codec = new SessionValueCodec();
	private final transient AtomicBoolean invalidated = new AtomicBoolean(false);
	private final transient Object mutex = new Object();

	private final String id;
	private final long creationTime;
	private long lastAccessedTime;
	private int maxInactiveInterval;
	private boolean isNew;

	private boolean dirtyCreation;
	private boolean dirtyLastAccess;
	private boolean dirtyMaxInactive;

	private final Map<String, Object> cache = new HashMap<>();
	private final Set<String> dirtyAttributes = new HashSet<>();
	private final Set<String> removedAttributes = new HashSet<>();

	private RedisHttpSession(SessionStore store, RedisSessionConfiguration configuration, ServletContext servletContext, String id,
			long creationTime, long lastAccessedTime, int maxInactiveInterval, boolean isNew) {
		this.store = store;
		this.configuration = configuration;
		this.servletContext = servletContext;
		this.id = id;
		this.creationTime = creationTime;
		this.lastAccessedTime = lastAccessedTime;
		this.maxInactiveInterval = maxInactiveInterval;
		this.isNew = isNew;
	}

	static RedisHttpSession newSession(SessionStore store, ServletContext servletContext,
			RedisSessionConfiguration configuration, String id) {
		var now = System.currentTimeMillis();
		var session = new RedisHttpSession(store, configuration, servletContext, id, now, now, configuration.getDefaultTtlSeconds(),
				true);
		session.dirtyCreation = true;
		session.dirtyLastAccess = true;
		session.dirtyMaxInactive = true;
		return session;
	}

	static RedisHttpSession fromMeta(SessionStore store, ServletContext servletContext, RedisSessionConfiguration configuration,
			String id, SessionStoreMeta meta) {
		return new RedisHttpSession(store, configuration, servletContext, id, meta.creationTime(), meta.lastAccessedTime(),
				meta.maxInactiveInterval(), false);
	}

	void markAccessed() {
		synchronized (mutex) {
			ensureValid();
			lastAccessedTime = System.currentTimeMillis();
			dirtyLastAccess = true;
			LOCAL_STORE.touchSession(id, resolveLocalTtlSeconds());
		}
	}

	void flush() {
		synchronized (mutex) {
			if (invalidated.get()) {
				return;
			}
			var hset = new HashMap<String, String>();
			var hdel = new HashSet<String>();

			if (dirtyCreation) {
				hset.put(SessionStoreKeys.META_CREATION, Long.toString(creationTime));
			}
			if (dirtyLastAccess) {
				hset.put(SessionStoreKeys.META_LAST_ACCESS, Long.toString(lastAccessedTime));
			}
			if (dirtyMaxInactive) {
				hset.put(SessionStoreKeys.META_MAX_INACTIVE, Integer.toString(maxInactiveInterval));
			}

			for (var name : removedAttributes) {
				if (name != null) {
					hdel.add(name);
				}
			}

			for (var name : dirtyAttributes) {
				if (name == null) {
					continue;
				}
				if (removedAttributes.contains(name)) {
					continue;
				}
				var value = cache.get(name);
				value = value == NULL ? null : value;
				value = SessionAttributeFilter.sanitizeValue(this, name, value);
				if (value == null) {
					hdel.add(name);
					cache.put(name, NULL);
					continue;
				}
				try {
					hset.put(name, codec.serialize(name, value));
				} catch (Exception e) {
					log("(RedisHttpSession) Skip attribute '" + name + "' serialization failure", e);
					hdel.add(name);
					cache.put(name, NULL);
				}
			}

			hdel.remove(SessionStoreKeys.META_CREATION);
			hdel.remove(SessionStoreKeys.META_LAST_ACCESS);
			hdel.remove(SessionStoreKeys.META_MAX_INACTIVE);

			var ttlSeconds = maxInactiveInterval > 0 ? maxInactiveInterval : configuration.getDefaultTtlSeconds();
			var ttlMillis = ttlSeconds > 0 ? ttlSeconds * 1000L : 0L;
			if (!hset.isEmpty() || !hdel.isEmpty() || ttlMillis > 0) {
				store.writeDelta(id, hset, hdel, ttlMillis);
			}

			dirtyCreation = false;
			dirtyLastAccess = false;
			dirtyMaxInactive = false;
			dirtyAttributes.clear();
			removedAttributes.clear();
			isNew = false;
		}
	}

	@Override
	public long getCreationTime() {
		synchronized (mutex) {
			ensureValid();
			return creationTime;
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public long getLastAccessedTime() {
		synchronized (mutex) {
			ensureValid();
			return lastAccessedTime;
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
			maxInactiveInterval = interval;
			dirtyMaxInactive = true;
			LOCAL_STORE.touchSession(id, resolveLocalTtlSeconds());
		}
	}

	@Override
	public int getMaxInactiveInterval() {
		synchronized (mutex) {
			ensureValid();
			return maxInactiveInterval;
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
			if (name == null) {
				return null;
			}
			var localValue = LOCAL_STORE.getAttribute(id, name, resolveLocalTtlSeconds());
			if (localValue != null) {
				return localValue;
			}
			if (removedAttributes.contains(name)) {
				return null;
			}
			if (cache.containsKey(name)) {
				var v = cache.get(name);
				return v == NULL ? null : v;
			}
			var raw = store.readAttribute(id, name);
			if (raw == null) {
				cache.put(name, NULL);
				return null;
			}
			try {
				var value = codec.deserialize(name, raw);
				cache.put(name, value != null ? value : NULL);
				return value;
			} catch (Exception e) {
				log("(RedisHttpSession) Failed to deserialize attribute '" + name + "'", e);
				removedAttributes.add(name);
				dirtyAttributes.remove(name);
				cache.put(name, NULL);
				return null;
			}
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
			var names = new HashSet<String>();
			try {
				names.addAll(store.readAttributeNames(id));
			} catch (Exception ignore) {
				// ignore
			}
			names.addAll(LOCAL_STORE.getAttributeNames(id, resolveLocalTtlSeconds()));
			for (var entry : cache.entrySet()) {
				if (entry.getKey() != null && entry.getValue() != NULL) {
					names.add(entry.getKey());
				}
			}
			names.removeAll(removedAttributes);
			names.removeIf(name -> name == null || name.startsWith(SessionStoreKeys.META_PREFIX));
			return Collections.enumeration(names);
		}
	}

	@Override
	public String[] getValueNames() {
		return Collections.list(getAttributeNames()).toArray(String[]::new);
	}

	@Override
	public void setAttribute(String name, Object value) {
		synchronized (mutex) {
			ensureValid();
			if (name == null) {
				return;
			}
			LOCAL_STORE.removeAttribute(id, name, resolveLocalTtlSeconds());
			cache.put(name, value != null ? value : NULL);
			dirtyAttributes.add(name);
			removedAttributes.remove(name);
			lastAccessedTime = System.currentTimeMillis();
			dirtyLastAccess = true;
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
			if (name == null) {
				return;
			}
			LOCAL_STORE.removeAttribute(id, name, resolveLocalTtlSeconds());
			cache.put(name, NULL);
			removedAttributes.add(name);
			dirtyAttributes.remove(name);
			lastAccessedTime = System.currentTimeMillis();
			dirtyLastAccess = true;
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
				store.delete(id);
			} catch (Exception e) {
				log("(RedisHttpSession) Failed to invalidate session", e);
			} finally {
				LOCAL_STORE.invalidate(id);
				synchronized (mutex) {
					cache.clear();
					dirtyAttributes.clear();
					removedAttributes.clear();
				}
			}
		}
	}

	@Override
	public boolean isNew() {
		synchronized (mutex) {
			ensureValid();
			return isNew;
		}
	}

	boolean isInvalidatedInternal() {
		return invalidated.get();
	}

	private int resolveLocalTtlSeconds() {
		return maxInactiveInterval > 0 ? maxInactiveInterval : configuration.getDefaultTtlSeconds();
	}

	Object getStatefulAttribute(String name) {
		synchronized (mutex) {
			ensureValid();
			if (name == null) {
				return null;
			}
			return LOCAL_STORE.getAttribute(id, name, resolveLocalTtlSeconds());
		}
	}

	void setStatefulAttribute(String name, Object value) {
		synchronized (mutex) {
			ensureValid();
			if (name == null) {
				return;
			}
			if (value == null) {
				LOCAL_STORE.removeAttribute(id, name, resolveLocalTtlSeconds());
			} else {
				LOCAL_STORE.setAttribute(id, name, value, resolveLocalTtlSeconds());
			}
			cache.put(name, NULL);
			removedAttributes.add(name);
			dirtyAttributes.remove(name);
			lastAccessedTime = System.currentTimeMillis();
			dirtyLastAccess = true;
		}
	}

	void removeStatefulAttribute(String name) {
		synchronized (mutex) {
			ensureValid();
			if (name == null) {
				return;
			}
			LOCAL_STORE.removeAttribute(id, name, resolveLocalTtlSeconds());
			cache.put(name, NULL);
			removedAttributes.add(name);
			dirtyAttributes.remove(name);
			lastAccessedTime = System.currentTimeMillis();
			dirtyLastAccess = true;
		}
	}

	private void ensureValid() {
		if (invalidated.get()) {
			throw new IllegalStateException("Session has been invalidated");
		}
	}

	private void log(String message, Exception e) {
		try {
			if (Engine.logEngine != null) {
				if (e == null) {
					Engine.logEngine.warn(message);
				} else {
					Engine.logEngine.warn(message, e);
				}
			}
		} catch (Exception ignore) {
			// ignore
		}
	}

	private static final class LocalAttributeStore {
		private static final long CLEANUP_INTERVAL_MILLIS = 10000L;
		private final ConcurrentHashMap<String, LocalSessionData> sessions = new ConcurrentHashMap<>();

		LocalAttributeStore() {
			var cleaner = new Thread(this::cleanupLoop);
			cleaner.setDaemon(true);
			cleaner.setName("RedisSession LocalAttributeStore Cleaner");
			cleaner.start();
		}

		void touchSession(String sessionId, int ttlSeconds) {
			if (sessionId == null) {
				return;
			}
			getSessionIfPresent(sessionId, ttlSeconds);
		}

		Object getAttribute(String sessionId, String name, int ttlSeconds) {
			if (sessionId == null || name == null) {
				return null;
			}
			var data = getSessionIfPresent(sessionId, ttlSeconds);
			if (data == null) {
				return null;
			}
			return data.attributes.get(name);
		}

		void setAttribute(String sessionId, String name, Object value, int ttlSeconds) {
			if (sessionId == null || name == null) {
				return;
			}
			if (value == null) {
				removeAttribute(sessionId, name, ttlSeconds);
				return;
			}
			var data = getOrCreateSession(sessionId, ttlSeconds);
			var previous = data.attributes.put(name, value);
			if (previous != null && previous != value) {
				closeValue(previous);
			}
		}

		void removeAttribute(String sessionId, String name, int ttlSeconds) {
			if (sessionId == null || name == null) {
				return;
			}
			var data = getSessionIfPresent(sessionId, ttlSeconds);
			if (data == null) {
				return;
			}
			var previous = data.attributes.remove(name);
			if (previous != null) {
				closeValue(previous);
			}
		}

		Set<String> getAttributeNames(String sessionId, int ttlSeconds) {
			if (sessionId == null) {
				return Collections.emptySet();
			}
			var data = getSessionIfPresent(sessionId, ttlSeconds);
			if (data == null) {
				return Collections.emptySet();
			}
			return new HashSet<>(data.attributes.keySet());
		}

		void invalidate(String sessionId) {
			if (sessionId == null) {
				return;
			}
			var data = sessions.remove(sessionId);
			if (data != null) {
				closeSession(data);
			}
		}

		private LocalSessionData getSessionIfPresent(String sessionId, int ttlSeconds) {
			var data = sessions.get(sessionId);
			if (data == null) {
				return null;
			}
			var now = System.currentTimeMillis();
			if (data.isExpired(now)) {
				if (sessions.remove(sessionId, data)) {
					closeSession(data);
				}
				return null;
			}
			data.touch(now, ttlSeconds);
			return data;
		}

		private LocalSessionData getOrCreateSession(String sessionId, int ttlSeconds) {
			var now = System.currentTimeMillis();
			while (true) {
				var data = sessions.get(sessionId);
				if (data == null) {
					var created = new LocalSessionData(now, ttlSeconds);
					var previous = sessions.putIfAbsent(sessionId, created);
					if (previous == null) {
						return created;
					}
					data = previous;
				}
				if (data.isExpired(now)) {
					if (sessions.remove(sessionId, data)) {
						closeSession(data);
					}
					continue;
				}
				data.touch(now, ttlSeconds);
				return data;
			}
		}

		private void cleanupLoop() {
			while (true) {
				try {
					Thread.sleep(CLEANUP_INTERVAL_MILLIS);
					cleanupExpired();
				} catch (InterruptedException ignore) {
					Thread.currentThread().interrupt();
					return;
				} catch (Exception ignore) {
					// ignore
				}
			}
		}

		private void cleanupExpired() {
			var now = System.currentTimeMillis();
			for (var entry : sessions.entrySet()) {
				var data = entry.getValue();
				if (data == null || !data.isExpired(now)) {
					continue;
				}
				if (sessions.remove(entry.getKey(), data)) {
					closeSession(data);
				}
			}
		}

		private void closeSession(LocalSessionData data) {
			for (var value : data.attributes.values()) {
				closeValue(value);
			}
			data.attributes.clear();
		}

		private void closeValue(Object value) {
			if (value instanceof AutoCloseable closeable) {
				try {
					closeable.close();
				} catch (Exception e) {
					if (Engine.logEngine != null) {
						Engine.logEngine.warn("(RedisHttpSession) Failed to close local session attribute", e);
					}
				}
			}
		}
	}

	private static final class LocalSessionData {
		private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
		private volatile long lastAccessTime;
		private volatile int ttlSeconds;

		private LocalSessionData(long now, int ttlSeconds) {
			this.lastAccessTime = now;
			this.ttlSeconds = ttlSeconds;
		}

		private void touch(long now, int ttlSeconds) {
			this.lastAccessTime = now;
			this.ttlSeconds = ttlSeconds;
		}

		private boolean isExpired(long now) {
			return ttlSeconds > 0 && (now - lastAccessTime) > ttlSeconds * 1000L;
		}
	}
}
