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

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ContextFactory.Listener;
import org.mozilla.javascript.NativeArray;
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
			script = cx.compileString(source, sourceName, lineno, securityDomain);
			compiledScript.put(source, script);
		}
		Object result = script.exec(cx, scope, scope);
		return result;
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

	static public Object jsonParse(String string) {
		return ScriptableObject.callMethod(json, "parse", new Object[]{string});
	}

	static public String jsonStringify(Object object) {
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
			return jsonStringify(object);
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

	private static void writeJsonData(StringBuilder sb, Object value, int depth) {
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
					writeJsonData(sb, item, depth + 1);
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
				writeJsonData(sb, item, depth + 1);
			}
			sb.append('}');
		} else {
			throw NotJsonData.INSTANCE;
		}
	}

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private static void writeJsonString(StringBuilder sb, String s) {
		sb.append('"');
		int length = s.length();
		for (int i = 0; i < length; i++) {
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
				boolean loneSurrogate = Character.isSurrogate(c) && !(Character.isHighSurrogate(c) && i + 1 < length
						&& Character.isLowSurrogate(s.charAt(i + 1))) && !(Character.isLowSurrogate(c) && i > 0
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
