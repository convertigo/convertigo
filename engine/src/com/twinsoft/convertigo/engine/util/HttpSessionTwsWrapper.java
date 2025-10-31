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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.DomSerializationSupport.SerializedDom;

@SuppressWarnings("deprecation")
public class HttpSessionTwsWrapper implements HttpSession {
	private static final Map<String, Map<String, Object>> transientAttributes = new ConcurrentHashMap<>();
	private static final Map<String, Set<String>> reportedSerializationFailures = new ConcurrentHashMap<>();
	private static final String DEBUG_DIRECTORY_NAME = "session-debug";
private static final int DEFAULT_DEBUG_DEPTH = 2;
private static final int MAX_DEBUG_DEPTH = 6;
	private static final int DEBUG_ARRAY_PREVIEW = 20;

	private final HttpSession session;
	private String id = "sessionTerminated";

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
		} finally {
			writeDebugSnapshot();
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
					clearSerializationFailure(name, restored);
				} else {
					removeTransientAttribute(name);
					clearSerializationFailure(name, null);
				}
				session.setAttribute(name, dom);
				return;
			}

			var serializedDom = DomSerializationSupport.serialize(value);
			if (serializedDom != null) {
				setTransientAttribute(name, value);
				session.setAttribute(name, serializedDom);
				clearSerializationFailure(name, value);
				return;
			}

			if (canSerialize(name, value)) {
				removeTransientAttribute(name);
				session.setAttribute(name, value);
				clearSerializationFailure(name, value);
			} else {
				setTransientAttribute(name, value);
				session.removeAttribute(name);
			}
		} catch (Exception e) {
			onException(e);
		} finally {
			writeDebugSnapshot();
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
		recordSerializationFailure(attributeName, value, cause);
	}

	private void recordSerializationFailure(String attributeName, Object value, Exception cause) {
		if (!isFailureReportEnabled() || attributeName == null || attributeName.isEmpty()) {
			return;
		}
		var sessionId = getId();
		if (sessionId == null || sessionId.isEmpty()) {
			sessionId = "sessionTerminated";
		}
		var type = value == null ? "null" : value.getClass().getName();
		var key = attributeName + '|' + type;
		if (!failureKeysForSession(sessionId).add(key)) {
			return;
		}
		try {
			var path = failureLogPath(sessionId);
			Files.createDirectories(path.getParent());
			var sb = new StringBuilder();
			sb.append(Instant.now()).append('\t').append(sessionId).append('\t').append(attributeName).append('\t').append(type);
			if (cause != null) {
				sb.append('\t').append(cause.getClass().getName());
				var message = cause.getMessage();
				if (message != null && !message.isEmpty()) {
					sb.append(": ").append(message.replace('\n', ' ').replace('\r', ' '));
				}
			}
			sb.append(System.lineSeparator());
			Files.writeString(path, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Exception e) {
			onException(e);
		}
	}

	private void clearSerializationFailure(String attributeName, Object value) {
		if (attributeName == null || attributeName.isEmpty()) {
			return;
		}
		var sessionId = getId();
		if (sessionId == null || sessionId.isEmpty()) {
			sessionId = "sessionTerminated";
		}
		var set = reportedSerializationFailures.get(sessionId);
		if (set == null || set.isEmpty()) {
			return;
		}
		var type = value == null ? "null" : value.getClass().getName();
		set.remove(attributeName + '|' + type);
		if (set.isEmpty()) {
			reportedSerializationFailures.remove(sessionId);
		}
	}

	private static boolean isFailureReportEnabled() {
		try {
			return EnginePropertiesManager.getPropertyAsBoolean(PropertyName.SESSION_SERIALIZATION_REPORT);
		} catch (Exception e) {
			return false;
		}
	}

	private static Set<String> failureKeysForSession(String sessionId) {
		return reportedSerializationFailures.computeIfAbsent(sessionId,
				key -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
	}

	private static Path failureLogPath(String sessionId) {
		return debugFilePath(sessionId).resolveSibling(sanitizeSessionId(sessionId) + ".failures.log");
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

	private void writeDebugSnapshot() {
		var sessionId = getId();
		synchronized (session) {
			if (sessionId == null || sessionId.isEmpty()) {
				return;
			}
			var debugDepth = resolveDebugDepth();
			if (debugDepth <= 0) {
				deleteDebugSnapshot(sessionId);
				return;
			}
			debugDepth = Math.min(debugDepth, MAX_DEBUG_DEPTH);
			if ("sessionTerminated".equals(sessionId)) {
				return;
			}
			try {
				var attributes = new JSONObject();
				for (var enumeration = session.getAttributeNames(); enumeration.hasMoreElements();) {
					var name = enumeration.nextElement();
					var value = session.getAttribute(name);
					if (value instanceof SerializedDom dom) {
						var local = getTransientAttribute(name);
						value = local != TransientAttributeHolder.NOT_FOUND ? local : DomSerializationSupport.deserialize(dom);
					}
					put(attributes, name, toDebugEntry(value, debugDepth));
				}
				var root = new JSONObject();
				put(root, "sessionId", sessionId);
				put(root, "timestamp", Instant.now().toString());
				put(root, "depth", debugDepth);
				put(root, "attributes", attributes);

				var path = debugFilePath(sessionId);
				Files.createDirectories(path.getParent());
				Files.writeString(path, root.toString(2), StandardCharsets.UTF_8);
			} catch (IllegalStateException e) {
				deleteDebugSnapshot(sessionId);
			} catch (Exception e) {
				onException(e);
			}
		}
	}

	private JSONObject toDebugEntry(Object value, int depth) {
		var entry = new JSONObject();
		if (value == null) {
			put(entry, "class", JSONObject.NULL);
			put(entry, "value", JSONObject.NULL);
			return entry;
		}
		put(entry, "class", value.getClass().getName());
		try {
			var visited = Collections.newSetFromMap(new IdentityHashMap<>());
			put(entry, "value", debugValue(value, depth, visited));
		} catch (Exception e) {
			put(entry, "value", "[error:" + e.getClass().getSimpleName() + "]");
		}
		return entry;
	}

	private Object debugValue(Object value, int depth, Set<Object> visited) {
		if (value == null) {
			return JSONObject.NULL;
		}
		if (value instanceof SerializedDom dom) {
			var restored = DomSerializationSupport.deserialize(dom);
			return restored != null ? debugValue(restored, depth, visited) : "[dom:null]";
		}
		if (value instanceof byte[] bytes) {
			return "[blob:" + bytes.length + "]";
		}
		if (value instanceof ByteBuffer buffer) {
			return "[blob:" + buffer.remaining() + "]";
		}
		if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
			return value;
		}
		if (value instanceof char[] chars) {
			return new String(chars);
		}
		if (depth <= 0) {
			return "[" + value.getClass().getSimpleName() + "]";
		}
		if (value instanceof Map<?, ?> map) {
			if (!visited.add(value)) {
				return "[circular:" + value.getClass().getName() + "]";
			}
			try {
				var jsonObject = new JSONObject();
				for (var entry : map.entrySet()) {
					put(jsonObject, String.valueOf(entry.getKey()), debugValue(entry.getValue(), depth - 1, visited));
				}
				return jsonObject;
			} finally {
				visited.remove(value);
			}
		}
		if (value instanceof Collection<?> collection) {
			if (!visited.add(value)) {
				return "[circular:" + value.getClass().getName() + "]";
			}
			try {
				var array = new JSONArray();
				var count = 0;
				for (var item : collection) {
					if (count++ >= DEBUG_ARRAY_PREVIEW) {
						add(array, "[...]");
						break;
					}
					add(array, debugValue(item, depth - 1, visited));
				}
				return array;
			} finally {
				visited.remove(value);
			}
		}
		if (value.getClass().isArray()) {
			if (!visited.add(value)) {
				return "[circular:" + value.getClass().getName() + "]";
			}
			try {
				if (value instanceof byte[] bytes) {
					return "[blob:" + bytes.length + "]";
				}
				if (value instanceof char[] chars) {
					return new String(chars);
				}
				if (value instanceof Object[] array) {
					var jsonArray = new JSONArray();
					for (int i = 0; i < Math.min(array.length, DEBUG_ARRAY_PREVIEW); i++) {
						add(jsonArray, debugValue(array[i], depth - 1, visited));
					}
					if (array.length > DEBUG_ARRAY_PREVIEW) {
						add(jsonArray, "[...]");
					}
					return jsonArray;
				}
				var jsonArray = new JSONArray();
				var length = Array.getLength(value);
				for (int i = 0; i < Math.min(length, DEBUG_ARRAY_PREVIEW); i++) {
					add(jsonArray, Array.get(value, i));
				}
				if (length > DEBUG_ARRAY_PREVIEW) {
					add(jsonArray, "[...]");
		}
		return jsonArray;
	} finally {
		visited.remove(value);
	}
}
		if (value instanceof Context context) {
			return describeContext(context, depth, visited);
		}
		if (value instanceof HttpState state) {
			return describeHttpState(state, depth, visited);
		}
		if (value instanceof Cookie cookie) {
			return describeCookie(cookie);
		}
		if (shouldIntrospect(value)) {
			return describeObject(value, depth, visited);
		}
		return value.toString();
	}

	private Object describeContext(Context context, int depth, Set<Object> visited) {
		if (context == null) {
			return JSONObject.NULL;
		}
		if (depth <= 0) {
			return "[Context]";
		}
		if (!visited.add(context)) {
			return "[circular:" + Context.class.getName() + "]";
		}
		try {
			var json = new JSONObject();
			put(json, "contextId", context.contextID);
			put(json, "name", context.name);
			put(json, "project", context.projectName);
			put(json, "sequence", context.sequenceName);
			put(json, "connector", context.connectorName);
			put(json, "isDestroying", context.isDestroying);
			put(json, "isErrorDocument", context.isErrorDocument);
			put(json, "isNewSession", context.isNewSession);
			put(json, "httpState", describeHttpState(context.httpState, depth - 1, visited));
			if (depth > 1) {
				var keys = context.keys();
				if (keys != null && !keys.isEmpty()) {
					var attributes = new JSONObject();
					int index = 0;
					for (String key : keys) {
						if (index++ >= DEBUG_ARRAY_PREVIEW) {
							put(attributes, "[...]", keys.size() - DEBUG_ARRAY_PREVIEW);
							break;
						}
						var attributeValue = context.get(key);
						put(attributes, key, debugValue(attributeValue, depth - 2, visited));
					}
					put(json, "attributes", attributes);
				}
			}
			return json;
		} catch (Exception e) {
			return "[Context:" + context.contextID + "]";
		} finally {
			visited.remove(context);
		}
	}

	private Object describeHttpState(HttpState state, int depth, Set<Object> visited) {
		if (state == null) {
			return JSONObject.NULL;
		}
		if (depth <= 0) {
			return "[HttpState]";
		}
		if (!visited.add(state)) {
			return "[circular:" + HttpState.class.getName() + "]";
		}
		try {
			var json = new JSONObject();
			try {
				put(json, "cookiePolicy", state.getCookiePolicy());
			} catch (Exception e) {
				// ignore
			}
			var cookies = state.getCookies();
			if (cookies != null) {
				if (depth > 1) {
					var array = new JSONArray();
					int limit = Math.min(cookies.length, DEBUG_ARRAY_PREVIEW);
					for (int i = 0; i < limit; i++) {
						add(array, describeCookie(cookies[i]));
					}
					if (cookies.length > limit) {
						add(array, "[...]");
					}
					put(json, "cookies", array);
				} else {
					put(json, "cookies", cookies.length);
				}
			}
			return json;
		} catch (Exception e) {
			return "[HttpState]";
		} finally {
			visited.remove(state);
		}
	}

	private Object describeCookie(Cookie cookie) {
		if (cookie == null) {
			return JSONObject.NULL;
		}
		var json = new JSONObject();
		put(json, "name", cookie.getName());
		put(json, "value", cookie.getValue());
		if (cookie.getDomain() != null) {
			put(json, "domain", cookie.getDomain());
		}
		if (cookie.getPath() != null) {
			put(json, "path", cookie.getPath());
		}
		var expiry = cookie.getExpiryDate();
		if (expiry != null) {
			put(json, "expires", expiry.toInstant().toString());
		}
		if (cookie.getSecure()) {
			put(json, "secure", true);
		}
		put(json, "version", cookie.getVersion());
		return json;
	}

	private Object describeObject(Object value, int depth, Set<Object> visited) {
		if (value == null) {
			return JSONObject.NULL;
		}
		if (depth <= 0) {
			return "[" + value.getClass().getSimpleName() + "]";
		}
		if (!visited.add(value)) {
			return "[circular:" + value.getClass().getName() + "]";
		}
		try {
			var json = new JSONObject();
			Class<?> type = value.getClass();
			int inspected = 0;
			while (type != null && type != Object.class) {
				for (Field field : type.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()) || field.isSynthetic()) {
						continue;
					}
					var fieldName = field.getName();
					if (json.has(fieldName)) {
						continue;
					}
					field.setAccessible(true);
					var fieldValue = field.get(value);
					put(json, fieldName, debugValue(fieldValue, depth - 1, visited));
					if (++inspected >= DEBUG_ARRAY_PREVIEW) {
						put(json, "[...]", "fields truncated");
						return json;
					}
				}
				type = type.getSuperclass();
			}
			return json;
		} catch (Exception e) {
			return value.toString();
		} finally {
			visited.remove(value);
		}
	}

	private boolean shouldIntrospect(Object value) {
		if (value == null) {
			return false;
		}
		var className = value.getClass().getName();
		return className.startsWith("com.twinsoft.convertigo.");
	}

	private static int resolveDebugDepth() {
		try {
			var raw = EnginePropertiesManager.getProperty(PropertyName.SESSION_SERIALIZATION_DEBUG);
			if (raw == null) {
				return 0;
			}
			raw = raw.trim();
			if (raw.isEmpty()) {
				return 0;
			}
			if ("true".equalsIgnoreCase(raw)) {
				return DEFAULT_DEBUG_DEPTH;
			}
			if ("false".equalsIgnoreCase(raw)) {
				return 0;
			}
			return Math.max(0, Integer.parseInt(raw));
		} catch (NumberFormatException e) {
			return 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private static Path debugFilePath(String sessionId) {
		var base = Engine.CACHE_PATH;
		if (base == null || base.isEmpty()) {
			var fallback = Engine.USER_WORKSPACE_PATH != null && !Engine.USER_WORKSPACE_PATH.isEmpty()
					? Engine.USER_WORKSPACE_PATH + "/cache"
					: System.getProperty("java.io.tmpdir");
			base = fallback;
		}
		return Path.of(base, DEBUG_DIRECTORY_NAME, sanitizeSessionId(sessionId) + ".json");
	}

	private static String sanitizeSessionId(String sessionId) {
		var sanitized = sessionId.replaceAll("[^A-Za-z0-9\\-_.]", "_");
		if (sanitized.length() > 120) {
			sanitized = sanitized.substring(0, 120);
		}
		return sanitized.isEmpty() ? "session" : sanitized;
	}

	private static void deleteDebugSnapshot(String sessionId) {
		if (sessionId == null || sessionId.isEmpty()) {
			return;
		}
		try {
			Files.deleteIfExists(debugFilePath(sessionId));
		} catch (Exception e) {
			try {
				if (Engine.logEngine.isTraceEnabled()) {
					Engine.logEngine.trace("(HttpSessionTwsWrapper) Failed to delete session debug snapshot [" + sessionId + "]", e);
				} else {
					Engine.logEngine.debug("(HttpSessionTwsWrapper) Failed to delete session debug snapshot [" + sessionId + "]: " + e.getMessage());
				}
			} catch (Exception ignore) {
				// ignore logging failures
			}
		}
	}

	private static void put(JSONObject object, String key, Object value) {
		try {
			object.put(key, value);
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void add(JSONArray array, Object value) {
		array.put(value);
	}

	public static void cleanTransientAttributes(String sessionId) {
		if (sessionId != null) {
			transientAttributes.remove(sessionId);
			transientAttributes.remove("sessionTerminated");
			reportedSerializationFailures.remove(sessionId);
			reportedSerializationFailures.remove("sessionTerminated");
			deleteDebugSnapshot(sessionId);
		}
	}

	public static HttpSession wrap(HttpSession session) {
		if (session == null || session instanceof HttpSessionTwsWrapper) {
			return session;
		}
		var wrapper = new HttpSessionTwsWrapper(session);
		wrapper.writeDebugSnapshot();
		return wrapper;
	}

	private enum TransientAttributeHolder {
		NOT_FOUND;
	}
}
