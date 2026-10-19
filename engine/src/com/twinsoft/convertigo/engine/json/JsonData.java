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


package com.twinsoft.convertigo.engine.json;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import com.twinsoft.convertigo.engine.util.RhinoUtils;

/**
 * Reads the JSON data of a sequence, whatever holds it: JavaScript objects and arrays (JSON.parse, steps written in
 * JavaScript) or Java maps and lists (native JSON steps). Values keep the JavaScript semantics of the code they
 * replace: a missing key is undefined, conversions and comparisons are the ones of the language.
 */
public final class JsonData {
	public static final Object UNDEFINED = Undefined.instance;

	private JsonData() {
	}

	public static Object unwrap(Object value) {
		return value instanceof NativeJavaObject ? ((Wrapper) value).unwrap() : value;
	}

	/** obj[key] for an object, undefined when the key is missing or obj is not an object */
	public static Object get(Object obj, String key) {
		if (obj instanceof Scriptable) {
			Scriptable s = (Scriptable) obj;
			Object value = s.get(key, s);
			return value == Scriptable.NOT_FOUND ? UNDEFINED : value;
		}
		if (obj instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) obj;
			Object value = map.get(key);
			return value == null && !map.containsKey(key) ? UNDEFINED : value;
		}
		return UNDEFINED;
	}

	/** obj.hasOwnProperty(key) */
	public static boolean has(Object obj, String key) {
		if (obj instanceof Scriptable) {
			Scriptable s = (Scriptable) obj;
			return s.has(key, s);
		}
		if (obj instanceof Map) {
			return ((Map<?, ?>) obj).containsKey(key);
		}
		return false;
	}

	/** the keys of an object in the order of for (key in obj) */
	public static List<String> keys(Object obj) {
		if (obj instanceof Scriptable) {
			Object[] ids = ((Scriptable) obj).getIds();
			List<String> keys = new ArrayList<String>(ids.length);
			for (Object id : ids) {
				keys.add(id.toString());
			}
			return keys;
		}
		if (obj instanceof Map) {
			List<String> keys = new ArrayList<String>(((Map<?, ?>) obj).size());
			for (Object key : ((Map<?, ?>) obj).keySet()) {
				keys.add(key.toString());
			}
			return keys;
		}
		return Collections.emptyList();
	}

	public static boolean isArray(Object value) {
		return value instanceof NativeArray || (value instanceof List && !(value instanceof Scriptable));
	}

	/** an array as a list, a single value as a list of one, null and undefined as null */
	@SuppressWarnings("unchecked")
	public static List<Object> list(Object value) {
		value = unwrap(value);
		if (isNullish(value)) {
			return null;
		}
		if (value instanceof NativeArray) {
			NativeArray array = (NativeArray) value;
			int length = (int) array.getLength();
			List<Object> list = new ArrayList<Object>(length);
			for (int i = 0; i < length; i++) {
				Object item = array.get(i, array);
				list.add(item == Scriptable.NOT_FOUND ? UNDEFINED : item);
			}
			return list;
		}
		if (value instanceof List) {
			return (List<Object>) value;
		}
		List<Object> list = new ArrayList<Object>(1);
		list.add(value);
		return list;
	}

	/** a list of texts (column names), null for null or undefined */
	public static List<String> strings(Object value) {
		List<Object> list = list(value);
		if (list == null) {
			return null;
		}
		List<String> strings = new ArrayList<String>(list.size());
		for (Object item : list) {
			strings.add(toStr(item));
		}
		return strings;
	}

	public static boolean isNullish(Object value) {
		return value == null || value == UNDEFINED || value == Scriptable.NOT_FOUND;
	}

	/** typeof value == "object" (null included) */
	public static boolean isObject(Object value) {
		return value == null || value instanceof Scriptable && !(value instanceof org.mozilla.javascript.Callable)
				|| value instanceof Map || value instanceof List;
	}

	public static boolean isNumber(Object value) {
		return value instanceof Number;
	}

	public static boolean isString(Object value) {
		return value instanceof CharSequence;
	}

	/** "" + value */
	public static String toStr(Object value) {
		if (value instanceof String) {
			return (String) value;
		}
		if (value instanceof Map || value instanceof List && !(value instanceof Scriptable)) {
			return value instanceof List ? joinList((List<?>) value) : "[object Object]";
		}
		return ScriptRuntime.toString(value);
	}

	private static String joinList(List<?> list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			Object item = list.get(i);
			if (!isNullish(item)) {
				sb.append(toStr(item));
			}
		}
		return sb.toString();
	}

	/** a == b */
	public static boolean looseEq(Object a, Object b) {
		if (a instanceof Map || a instanceof List && !(a instanceof Scriptable)) {
			return b instanceof CharSequence || b instanceof Number || b instanceof Boolean ? looseEq(toStr(a), b) : a == b;
		}
		return ScriptRuntime.eq(a, b);
	}

	/** JavaScript number to text */
	public static String numberToString(double d) {
		return ScriptRuntime.numberToString(d, 10);
	}

	/** value.toFixed(digits): the exact decimal value of the double, rounded half up, as the language defines it */
	public static String toFixed(double value, int digits) {
		if (Double.isNaN(value)) {
			return "NaN";
		}
		if (Math.abs(value) >= 1e21 || Double.isInfinite(value)) {
			return numberToString(value);
		}
		String text = new BigDecimal(Math.abs(value)).setScale(digits, RoundingMode.HALF_UP).toPlainString();
		return value < 0 ? "-" + text : text;
	}

	/** Math.round(value) */
	public static double round(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return value;
		}
		double floor = Math.floor(value);
		double rounded = value - floor >= 0.5 ? floor + 1 : floor;
		return rounded == 0 && value < 0 ? -0.0 : rounded;
	}

	/** Number(text) */
	public static double toNumber(String text) {
		return ScriptRuntime.toNumber(text);
	}

	private static boolean isJsSpace(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000B' || c == '\f' || c == ' ' || c == '﻿'
				|| c == ' ' || c == ' ' || c == ' ' || (c >= ' ' && c <= ' ') || c == ' '
				|| c == ' ' || c == '　';
	}

	/** text.trim() and text.replace(/^\s+|\s+$/g, "") */
	public static String trim(String text) {
		int start = 0, end = text.length();
		while (start < end && isJsSpace(text.charAt(start))) {
			start++;
		}
		while (end > start && isJsSpace(text.charAt(end - 1))) {
			end--;
		}
		return start == 0 && end == text.length() ? text : text.substring(start, end);
	}

	/** parseInt(text, 10) */
	public static double parseInt(String text) {
		int i = 0, length = text.length();
		while (i < length && isJsSpace(text.charAt(i))) {
			i++;
		}
		boolean negative = false;
		if (i < length && (text.charAt(i) == '+' || text.charAt(i) == '-')) {
			negative = text.charAt(i) == '-';
			i++;
		}
		int start = i;
		double result = 0;
		while (i < length && text.charAt(i) >= '0' && text.charAt(i) <= '9') {
			result = result * 10 + (text.charAt(i) - '0');
			i++;
		}
		if (i == start) {
			return Double.NaN;
		}
		if (i - start > 15) {
			result = Double.parseDouble(text.substring(start, i));
		}
		return negative ? -result : result;
	}

	/** parseFloat(text) */
	public static double parseFloat(String text) {
		int i = 0, length = text.length();
		while (i < length && isJsSpace(text.charAt(i))) {
			i++;
		}
		int start = i;
		if (i < length && (text.charAt(i) == '+' || text.charAt(i) == '-')) {
			i++;
		}
		if (text.startsWith("Infinity", i)) {
			return text.charAt(start) == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		}
		int intDigits = 0;
		while (i < length && text.charAt(i) >= '0' && text.charAt(i) <= '9') {
			i++;
			intDigits++;
		}
		int fractionDigits = 0;
		if (i < length && text.charAt(i) == '.') {
			int j = i + 1;
			while (j < length && text.charAt(j) >= '0' && text.charAt(j) <= '9') {
				j++;
				fractionDigits++;
			}
			if (intDigits > 0 || fractionDigits > 0) {
				i = j;
			}
		}
		if (intDigits == 0 && fractionDigits == 0) {
			return Double.NaN;
		}
		if (i < length && (text.charAt(i) == 'e' || text.charAt(i) == 'E')) {
			int j = i + 1;
			if (j < length && (text.charAt(j) == '+' || text.charAt(j) == '-')) {
				j++;
			}
			int exponentStart = j;
			while (j < length && text.charAt(j) >= '0' && text.charAt(j) <= '9') {
				j++;
			}
			if (j > exponentStart) {
				i = j;
			}
		}
		return Double.parseDouble(text.substring(start, i));
	}

	/** JSON.stringify(value), "undefined" for undefined (as concatenated in a text) */
	public static String stringify(Object value) {
		if (value == UNDEFINED || value == Scriptable.NOT_FOUND) {
			return "undefined";
		}
		return RhinoUtils.jsonStringifyData(value);
	}
}
