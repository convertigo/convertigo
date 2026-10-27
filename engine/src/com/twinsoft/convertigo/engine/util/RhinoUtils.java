/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import java.io.File;
import java.io.Writer;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ContextFactory.Listener;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.Undefined;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class RhinoUtils {
	static public boolean debugMode = false;
	static final public Object[] EMPTY_ARGS = new Object[0];
	static final private Map<String, Script> compiledScript = new ConcurrentHashMap<String, Script>();
	static final private NativeJSON json;
	static {
		ScriptableObject scope = Context.enter().initStandardObjects();
		json = (NativeJSON) scope.get("JSON", scope);
		Context.exit();
	}

	static public Scriptable copyScope(Context context, Scriptable scope) {
		Scriptable scopeCopy = context.initStandardObjects();
		for (Object id : scope.getIds()) {
			scopeCopy.put(id.toString(), scopeCopy, scope.get(id.toString(), scope));
		}
		return scopeCopy;
	}
	static public Object evalCachedJavascript(Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		return evalCachedJavascript(null, cx, scope, source, sourceName, lineno, securityDomain);
	}

	static public Object evalCachedJavascript(DatabaseObject dbo, Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		return evalCachedJavascript(dbo, cx, scope, source, sourceName, lineno, securityDomain, null);
	}

	/**
	 * As {@link #evalCachedJavascript(Context, Scriptable, String, String, int, Object)}, for the code of a file of a
	 * project: also kept compiled on disk across restarts, by {@link RhinoBytecodeCache}.
	 *
	 * @param origin the file of the source
	 */
	static public Object evalCachedJavascript(Context cx, Scriptable scope, String source, String sourceName, File origin) {
		return evalCachedJavascript(null, cx, scope, source, sourceName, 1, null, origin);
	}

	static private Object evalCachedJavascript(DatabaseObject dbo, Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain, File origin) {
		source = CopilotHelper.addInstruction(dbo, source);
		if (debugMode) {
			if (dbo != null) {
				sourceName = dbo.getShortQName() + "-" + sourceName;
			}
			return evalInterpretedJavascript(cx, scope, source, sourceName, lineno, securityDomain);
		}
		Script script = compiledScript.get(source);
		if (script == null) {
			cx.setInterpretedMode(false);
			script = origin != null && lineno == 1 && securityDomain == null
					? RhinoBytecodeCache.compile(cx, source, sourceName, origin)
					: cx.compileString(source, sourceName, lineno, securityDomain);
			compiledScript.put(source, script);
		}
		Object result = script.exec(cx, scope, scope);
		return result;
	}

	static public void clearCachedJavascript() {
		compiledScript.clear();
	}

	static public Object evalInterpretedJavascript(Context cx, Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		boolean interpretedMode = cx.isInterpretedMode();
		cx.setInterpretedMode(true);
		try {
			return cx.evaluateString(scope, source, sourceName, lineno, securityDomain);
		} finally {
			cx.setInterpretedMode(interpretedMode);
		}
	}

	/** the scope of the objects that jsonParse builds (the one of JSON.parse here) */
	static public Scriptable jsonScope() {
		return ScriptableObject.getTopLevelScope(json);
	}

	static public Object jsonParse(String string) {
		return ScriptableObject.callMethod(json, "parse", new Object[]{string});
	}

	static public String jsonStringify(Object object) {
		if (object instanceof NativeJavaObject) {
			object = ((NativeJavaObject) object).unwrap();
		}
		// the Java maps and lists of the native JSON steps
		if (!(object instanceof Scriptable) && (object instanceof Map || object instanceof List)) {
			StringBuilder sb = new StringBuilder(4096);
			try {
				writeJsonData(sb, object, 0);
				return sb.toString();
			} catch (NotJsonData e) {
				return rhinoStringify(object);
			}
		}
		return rhinoStringify(object);
	}

	/**
	 * JSON.stringify of Rhino, never our writer: a value our writer refuses comes here. A Java map or list is wrapped
	 * for JavaScript first (Rhino writes a raw one as undefined).
	 */
	private static String rhinoStringify(Object object) {
		if (object instanceof NativeJavaObject) {
			object = ((NativeJavaObject) object).unwrap();
		}
		if (!(object instanceof Scriptable) && (object instanceof Map || object instanceof List)) {
			object = Context.javaToJS(object, jsonScope());
		}
		return ScriptableObject.callMethod(json, "stringify", new Object[]{object}).toString();
	}

	/**
	 * JSON.stringify(object) for plain JavaScript data (objects, arrays, strings, numbers, booleans, null), written
	 * in one buffer. Anything else (toJSON, Java objects, dates, wrappers, BigInt, very deep or cyclic data) goes
	 * through JSON.stringify.
	 */
	static public String jsonStringifyData(Object object) {
		StringBuilder sb = new StringBuilder(4096);
		try {
			writeJsonData(sb, object, 0);
			return sb.toString();
		} catch (NotJsonData e) {
			return rhinoStringify(object);
		}
	}

	private static class NotJsonData extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private static final NotJsonData INSTANCE = new NotJsonData();

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}

	private static boolean isSkipped(Object value) {
		return value == Undefined.instance || value == Scriptable.NOT_FOUND || value instanceof Callable || value instanceof Symbol;
	}

	/** writes the JSON of plain JavaScript data, Java maps and lists, as jsonStringifyData */
	public static void writeJsonData(StringBuilder sb, Object value) {
		int start = sb.length();
		try {
			writeJsonData(sb, value, 0);
		} catch (NotJsonData e) {
			// the text written before the refused value is dropped
			sb.setLength(start);
			sb.append(rhinoStringify(value));
		}
	}

	private static final int CHUNK = 8192;

	/** the text written so far goes to the writer by chunks, so the JSON is never held whole */
	private static final class ChunkFlusher {
		final Writer writer;
		final char[] chunk = new char[CHUNK];

		ChunkFlusher(Writer writer) {
			this.writer = writer;
		}

		void flush(StringBuilder sb) {
			try {
				int length = sb.length();
				for (int start = 0; start < length; start += chunk.length) {
					int end = Math.min(length, start + chunk.length);
					sb.getChars(start, end, chunk, 0);
					writer.write(chunk, 0, end - start);
				}
				sb.setLength(0);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	/**
	 * writes the JSON of a value to a writer, as jsonStringifyData would give it, without holding the whole text:
	 * plain data is checked first (a value JSON.stringify must write is written from its text)
	 */
	public static void writeJsonData(Writer writer, Object value) throws IOException {
		if (!isJsonData(value)) {
			writer.write(rhinoStringify(value));
			return;
		}
		StringBuilder sb = new StringBuilder(CHUNK + 1024);
		ChunkFlusher flusher = new ChunkFlusher(writer);
		try {
			writeJsonData(sb, value, 0, flusher);
			flusher.flush(sb);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	/** true when jsonStringifyData writes the value itself (plain JavaScript data, Java maps and lists) */
	public static boolean isJsonData(Object value) {
		try {
			checkJsonData(value, 0);
			return true;
		} catch (NotJsonData e) {
			return false;
		}
	}

	private static void checkJsonData(Object value, int depth) {
		if (depth > 256) {
			throw NotJsonData.INSTANCE;
		}
		if (value == null || value instanceof CharSequence || value instanceof Boolean
				|| (value instanceof Number && !(value instanceof BigInteger))) {
			return;
		}
		if (value.getClass() == NativeArray.class) {
			NativeArray array = (NativeArray) value;
			long length = array.getLength();
			for (int i = 0; i < length; i++) {
				Object item = array.get(i, array);
				if (!isSkipped(item)) {
					checkJsonData(item, depth + 1);
				}
			}
		} else if (value.getClass() == NativeObject.class) {
			NativeObject object = (NativeObject) value;
			if (ScriptableObject.getProperty(object, "toJSON") instanceof Callable) {
				throw NotJsonData.INSTANCE;
			}
			for (Object id : object.getIds()) {
				Object item = id instanceof Integer ? object.get((Integer) id, object) : id instanceof String ? object.get((String) id, object) : null;
				if (item != null && !isSkipped(item)) {
					checkJsonData(item, depth + 1);
				}
			}
		} else if (value instanceof NativeJavaObject) {
			checkJsonData(((NativeJavaObject) value).unwrap(), depth);
		} else if (value instanceof Map && !(value instanceof Scriptable)) {
			for (Object item : ((Map<?, ?>) value).values()) {
				if (!isSkipped(item)) {
					checkJsonData(item, depth + 1);
				}
			}
		} else if (value instanceof List && !(value instanceof Scriptable)) {
			for (Object item : (List<?>) value) {
				if (!isSkipped(item)) {
					checkJsonData(item, depth + 1);
				}
			}
		} else {
			throw NotJsonData.INSTANCE;
		}
	}

	private static void writeJsonData(StringBuilder sb, Object value, int depth) {
		writeJsonData(sb, value, depth, null);
	}

	private static void writeJsonData(StringBuilder sb, Object value, int depth, ChunkFlusher flusher) {
		if (flusher != null && sb.length() >= CHUNK) {
			flusher.flush(sb);
		}
		if (depth > 256) {
			throw NotJsonData.INSTANCE;
		}
		if (value == null) {
			sb.append("null");
		} else if (value instanceof CharSequence) {
			writeJsonString(sb, value.toString());
		} else if (value instanceof Boolean) {
			sb.append(((Boolean) value).booleanValue() ? "true" : "false");
		} else if (value instanceof Number && !(value instanceof BigInteger)) {
			double d = ((Number) value).doubleValue();
			sb.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : ScriptRuntime.numberToString(d, 10));
		} else if (value.getClass() == NativeArray.class) {
			NativeArray array = (NativeArray) value;
			long length = array.getLength();
			sb.append('[');
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					sb.append(',');
				}
				Object item = array.get(i, array);
				if (isSkipped(item)) {
					sb.append("null");
				} else {
					writeJsonData(sb, item, depth + 1, flusher);
				}
			}
			sb.append(']');
		} else if (value.getClass() == NativeObject.class) {
			NativeObject object = (NativeObject) value;
			if (ScriptableObject.getProperty(object, "toJSON") instanceof Callable) {
				throw NotJsonData.INSTANCE;
			}
			sb.append('{');
			boolean first = true;
			for (Object id : object.getIds()) {
				Object item;
				String key;
				if (id instanceof Integer) {
					item = object.get((Integer) id, object);
					key = id.toString();
				} else if (id instanceof String) {
					item = object.get((String) id, object);
					key = (String) id;
				} else {
					continue;
				}
				if (isSkipped(item)) {
					continue;
				}
				if (!first) {
					sb.append(',');
				}
				first = false;
				writeJsonString(sb, key);
				sb.append(':');
				writeJsonData(sb, item, depth + 1, flusher);
			}
			sb.append('}');
		} else if (value instanceof NativeJavaObject) {
			writeJsonData(sb, ((NativeJavaObject) value).unwrap(), depth, flusher);
		} else if (value instanceof Map && !(value instanceof Scriptable)) {
			// a map of the native JSON steps: an undefined value is skipped like in a JavaScript object
			sb.append('{');
			boolean first = true;
			Map<?, ?> map = (Map<?, ?>) value;
			for (Object key : keysInJsOrder(map)) {
				Object item = map.get(key);
				if (isSkipped(item)) {
					continue;
				}
				if (!first) {
					sb.append(',');
				}
				first = false;
				writeJsonString(sb, String.valueOf(key));
				sb.append(':');
				writeJsonData(sb, item, depth + 1, flusher);
			}
			sb.append('}');
		} else if (value instanceof List && !(value instanceof Scriptable)) {
			sb.append('[');
			boolean first = true;
			for (Object item : (List<?>) value) {
				if (!first) {
					sb.append(',');
				}
				first = false;
				if (isSkipped(item)) {
					sb.append("null");
				} else {
					writeJsonData(sb, item, depth + 1, flusher);
				}
			}
			sb.append(']');
		} else {
			throw NotJsonData.INSTANCE;
		}
	}

	/** the keys of a map in the order a JavaScript object would give them (index keys first) */
	private static Iterable<?> keysInJsOrder(Map<?, ?> map) {
		for (Object key : map.keySet()) {
			if (com.twinsoft.convertigo.engine.json.JsonData.isIndexKey(String.valueOf(key))) {
				return com.twinsoft.convertigo.engine.json.JsonData.keys(map);
			}
		}
		return map.keySet();
	}

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	public static void writeJsonString(StringBuilder sb, String s) {
		writeJsonString(sb, s, 0, s.length());
	}

	/** the JSON string of s[start, end) */
	public static void writeJsonString(StringBuilder sb, CharSequence s, int start, int end) {
		sb.append('"');
		for (int i = start; i < end; i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"': sb.append("\\\""); break;
			case '\\': sb.append("\\\\"); break;
			case '\b': sb.append("\\b"); break;
			case '\f': sb.append("\\f"); break;
			case '\n': sb.append("\\n"); break;
			case '\r': sb.append("\\r"); break;
			case '\t': sb.append("\\t"); break;
			default:
				boolean loneSurrogate = Character.isSurrogate(c) && !(Character.isHighSurrogate(c) && i + 1 < end
						&& Character.isLowSurrogate(s.charAt(i + 1))) && !(Character.isLowSurrogate(c) && i > start
						&& Character.isHighSurrogate(s.charAt(i - 1)));
				if (c < 0x20 || loneSurrogate) {
					sb.append("\\u").append(HEX[c >> 12 & 15]).append(HEX[c >> 8 & 15]).append(HEX[c >> 4 & 15]).append(HEX[c & 15]);
				} else {
					sb.append(c);
				}
			}
		}
		sb.append('"');
	}

	static public void init() {
		ContextFactory.getGlobal().addListener(new Listener() {

			@Override
			public void contextReleased(Context cx) {
			}

			@Override
			public void contextCreated(Context cx) {
				cx.setLanguageVersion(Context.VERSION_ES6);
			}
		});
	}
}
